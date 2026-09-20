
<template>
  <PageContainer
    icon="⚙️"
    title="大模型 API"
    subtitle="自定义 OpenAI 兼容供应商 · 增删改即生效，无需重启"
  >
    <template #extra>
      <a-button type="primary" @click="openCreate">
        <span>➕</span>&nbsp;新增供应商
      </a-button>
      <a-button @click="load">
        <span>🔄</span>&nbsp;刷新
      </a-button>
    </template>

    <div class="provider-list cupid-fade-in">
      <a-alert
        type="info"
        show-icon
        class="provider-alert"
        message="每位用户独立维护自己的大模型供应商，彼此隔离、互不可见；API Key 加密存储且脱敏展示。保存后立即生效（无需改配置文件 / 重启）。所有供应商统一走 OpenAI 兼容协议，仅用于文本对话；视觉与语音由系统 yml 配置的模型承担。"
      />

      <a-table
        :data-source="providers"
        :columns="columns"
        row-key="id"
        :loading="loading"
        size="middle"
        class="provider-table"
        :pagination="{ pageSize: 10, hideOnSinglePage: true }"
        :scroll="{ x: 820 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="name-cell">
              <div class="name-cell__avatar">🤖</div>
              <div class="name-cell__text">
                <div class="name-cell__name">
                  {{ record.name }}
                </div>
                <div class="name-cell__slug">{{ record.providerKey }}</div>
              </div>
            </div>
          </template>
          <template v-if="column.key === 'feature'">
            <a-tag>文本</a-tag>
          </template>
          <template v-if="column.key === 'temperature'">
            {{ record.temperature ?? '—' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="middle">
              <a class="action-link" @click="openEdit(record)">编辑</a>
              <a-divider type="vertical" class="action-divider" />
              <a-popconfirm title="确定删除该供应商？" @confirm="remove(record)">
                <a class="action-link action-link--danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- 新建/编辑弹窗 -->
      <a-modal
        v-model:open="modalOpen"
        :title="editing ? '编辑供应商' : '新增供应商'"
        :confirm-loading="saving"
        width="min(600px, 96vw)"
        :ok-text="editing ? '保存' : '创建'"
        @ok="submit"
      >
        <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }" class="provider-form">
          <a-form-item label="备注名" required>
            <a-input v-model:value="form.name" placeholder="如：自定义 OpenAI" />
          </a-form-item>
          <a-form-item label="供应商代号" required>
            <a-input v-model:value="form.providerKey" :disabled="!!editing" placeholder="路由 key，如 my-openai" />
          </a-form-item>
          <a-form-item label="Base URL" required>
            <a-input v-model:value="form.baseUrl" placeholder="https://api.deepseek.com 或 .../compatible-mode/v1" />
          </a-form-item>
          <a-form-item label="API Key">
            <a-input-password v-model:value="form.apiKey" :placeholder="editing ? '留空则不修改' : 'sk-...' " />
          </a-form-item>
          <a-form-item label="模型名" required>
            <a-input v-model:value="form.model" placeholder="如 deepseek-chat / gpt-4o / qwen-plus" />
          </a-form-item>
          <a-form-item label="温度">
            <a-slider v-model:value="formTemperature" :min="0" :max="2" :step="0.1" />
          </a-form-item>
          <a-form-item label="最大 token">
            <a-input-number v-model:value="form.maxTokens" :min="1" :max="128000" placeholder="可选" style="width: 100%" />
          </a-form-item>
          <a-form-item label="能力范围">
            <div class="capabilities-hint">此处供应商仅用于<b>文本对话</b>；视觉（看图）与语音能力由系统配置文件（yml）统一管理，无需在此勾选</div>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
/**
 * 大模型 API 管理页：运行时可增删改自定义 OpenAI 兼容供应商，即时生效。
 */
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  createAiProvider,
  deleteAiProvider,
  listAiProviders,
  updateAiProvider,
} from '@/api'
import type { AiProvider, AiProviderPayload } from '@/types'
import PageContainer from '@/components/PageContainer.vue'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '供应商', key: 'name', width: 240 },
  { title: 'Model', dataIndex: 'model', key: 'model', width: 200 },
  { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl' },
  { title: '能力', key: 'feature', width: 120 },
  { title: '温度', key: 'temperature', width: 70 },
  { title: '操作', key: 'action', width: 200 },
]

const providers = ref<AiProvider[]>([])
const loading = ref(false)
const modalOpen = ref(false)
const saving = ref(false)
const editing = ref<AiProvider | null>(null)

const form = reactive<AiProviderPayload>({
  name: '',
  providerKey: '',
  baseUrl: '',
  apiKey: '',
  model: '',
  temperature: 0.7,
  topP: undefined,
  maxTokens: undefined,
  capabilities: [],
})

const formTemperature = ref(0.7)

function resetForm() {
  Object.assign(form, {
    name: '',
    providerKey: '',
    baseUrl: '',
    apiKey: '',
    model: '',
    temperature: 0.7,
    topP: undefined,
    maxTokens: undefined,
    capabilities: [],
  })
  formTemperature.value = 0.7
}

async function load() {
  loading.value = true
  try {
    providers.value = await listAiProviders()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(p: AiProvider) {
  editing.value = p
  Object.assign(form, {
    name: p.name,
    providerKey: p.providerKey,
    baseUrl: p.baseUrl,
    apiKey: p.apiKey || undefined,
    model: p.model,
    temperature: p.temperature ?? 0.7,
    topP: p.topP,
    maxTokens: p.maxTokens,
    capabilities: p.capabilities ?? [],
  })
  formTemperature.value = p.temperature ?? 0.7
  modalOpen.value = true
}

async function submit() {
  if (!form.name || !form.providerKey || !form.baseUrl || !form.model) {
    message.warning('备注名 / 供应商代号 / Base URL / 模型名 均为必填')
    return
  }
  saving.value = true
  try {
    const payload: AiProviderPayload = {
      ...form,
      temperature: formTemperature.value,
    }
    if (editing.value) {
      await updateAiProvider(editing.value.id, payload)
      message.success('已保存并生效')
    } else {
      await createAiProvider(payload)
      message.success('已创建并生效')
    }
    modalOpen.value = false
    await load()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    saving.value = false
  }
}

async function remove(p: AiProvider) {
  try {
    await deleteAiProvider(p.id)
    message.success('已删除')
    await load()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.provider-alert {
  margin-bottom: 16px;
  border-radius: var(--cupid-radius-sm);
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-cell__avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  background: var(--cupid-gradient-soft);
  border: 1px solid var(--cupid-border);
  flex-shrink: 0;
}

.name-cell__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--cupid-text);
}

.default-tag {
  margin-left: 6px;
}

.name-cell__slug {
  font-size: 12px;
  color: var(--cupid-text-muted);
  margin-top: 2px;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.action-link {
  color: var(--cupid-primary);
  cursor: pointer;
}

.action-link--danger {
  color: #ff4d4f;
}

.action-divider {
  margin: 0 4px;
}

.capabilities-hint {
  font-size: 12px;
  color: var(--cupid-text-muted);
  margin-top: 4px;
}
</style>
