
<template>
  <PageContainer icon="🎙️" title="音色配置" subtitle="管理 TTS 语音合成参数与偏好音色">
    <div class="voice-config">
      <!-- 模型选择 -->
      <a-card class="config-card" title="模型选择" :bordered="false">
        <a-form layout="vertical">
          <a-form-item label="当前模型">
            <a-select v-model:value="config.currentModel" style="width: 100%" disabled>
              <a-select-option v-for="m in config.availableModels" :key="m.name" :value="m.name">
                {{ m.displayName || m.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- 音色选择 -->
      <a-card class="config-card" title="音色选择" :bordered="false">
        <a-form layout="vertical">
          <a-form-item label="偏好音色">
            <a-select
              v-model:value="config.currentVoice"
              style="width: 100%"
              placeholder="选择音色"
              @change="saveConfig"
            >
              <a-select-option v-for="v in allVoices" :key="v.voiceId" :value="v.voiceId">
                {{ v.name }}
                <span class="voice-tag" v-if="v.gender"> {{ v.gender }}</span>
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- 声音设计 -->
      <a-card class="config-card" title="创建专属音色" :bordered="false">
        <a-form layout="vertical" @finish="handleDesign">
          <a-form-item label="声音描述" name="voicePrompt">
            <a-textarea v-model:value="designForm.voicePrompt" placeholder="例如：温柔的年轻女性，语速轻快，说话带笑意" :rows="3" maxlength="500" show-count />
          </a-form-item>
          <a-form-item label="预览文本" name="previewText">
            <a-input v-model:value="designForm.previewText" placeholder="空则使用默认预览文本" :maxlength="200" show-count />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" :loading="designing">
              创建专属音色
            </a-button>
          </a-form-item>
        </a-form>
        <div v-if="designResult" class="design-result">
          <a-alert message="创建成功" :description="`Voice ID: ${designResult}`" type="success" show-icon />
        </div>
      </a-card>

      <!-- 试听 -->
      <a-card class="config-card" title="试听" :bordered="false">
        <a-form layout="vertical">
          <a-form-item label="输入文本">
            <a-textarea v-model:value="testText" placeholder="输入想合成的文本" :rows="3" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleTest" :loading="testing" :disabled="!testText.trim()">
              试听
            </a-button>
          </a-form-item>
        </a-form>
        <audio v-if="testAudio" :src="testAudio" controls class="test-audio" />
      </a-card>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listModels, listVoices, getVoiceConfig, saveVoiceConfig, designVoice, synthesize } from '@/api'
import type { VoiceConfigVO } from '@/types'

const config = reactive<VoiceConfigVO>({
  currentVoice: '',
  currentModel: '',
  availableModels: [],
  availableVoices: [],
})

const designing = ref(false)
const designForm = reactive({ voicePrompt: '', previewText: '' })
const designResult = ref('')
const testText = ref('')
const testAudio = ref('')
const testing = ref(false)

const allVoices = computed(() => {
  const voices: { voiceId: string; name: string; gender: string }[] = []
  for (const m of config.availableModels) {
    const vList = m.voices || []
    for (const v of vList) {
      voices.push({ voiceId: v.voiceId, name: v.name, gender: v.gender || '' })
    }
  }
  // 去重
  const seen = new Set<string>()
  return voices.filter(v => { if (seen.has(v.voiceId)) return false; seen.add(v.voiceId); return true })
})

async function loadConfig() {
  try {
    const vo = await getVoiceConfig()
    config.currentVoice = vo.currentVoice || ''
    config.currentModel = vo.currentModel || ''
    config.availableModels = vo.availableModels || []
    if (config.currentModel && config.availableModels.length > 0) {
      const model = config.availableModels.find(m => m.name === config.currentModel)
      if (model && (!model.voices || (model.voices as any[]).length === 0)) {
        await loadVoices(model.name)
      }
    }
  } catch { /* ignore */ }
}

async function loadVoices(model: string) {
  try {
    const voices = await listVoices(model)
    for (const m of config.availableModels) {
      if (m.name === model) {
        m.voices = voices
        break
      }
    }
  } catch { /* ignore */ }
}

function saveConfig() {
  saveVoiceConfig({ preferredVoice: config.currentVoice }).catch(() => {})
}

async function handleDesign() {
  if (!designForm.voicePrompt.trim()) {
    message.warning('请填写声音描述')
    return
  }
  designing.value = true
  try {
    const voiceId = await designVoice(designForm.voicePrompt, designForm.previewText || undefined)
    designResult.value = voiceId
    message.success('专属音色创建成功')
    await loadConfig()
  } catch (e: any) {
    message.error(e?.message || '创建失败')
  } finally {
    designing.value = false
  }
}

async function handleTest() {
  if (!testText.value.trim()) return
  testing.value = true
  try {
    const base64 = await synthesize({ text: testText.value, voice: config.currentVoice || undefined })
    testAudio.value = `data:audio/mp3;base64,${base64}`
  } catch (e: any) {
    message.error(e?.message || '合成失败')
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.voice-config {
  max-width: 720px;
}

.config-card {
  margin-bottom: 20px;
  border-radius: var(--cupid-radius-lg);
  box-shadow: var(--cupid-shadow-sm);
}

.config-card .ant-card-head {
  background: linear-gradient(135deg, rgba(196, 78, 200, 0.08), rgba(255, 107, 157, 0.05));
  border-bottom: 1px solid rgba(196, 78, 200, 0.12);
}

.voice-tag {
  color: var(--cupid-text-secondary);
  font-size: 11px;
  margin-left: 6px;
}

.design-result {
  margin-top: 16px;
}

.test-audio {
  width: 100%;
  margin-top: 12px;
  border-radius: var(--cupid-radius-sm);
}
</style>
