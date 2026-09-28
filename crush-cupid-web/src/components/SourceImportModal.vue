<template>
  <a-modal v-model:open="open" title="导入原材料" :confirm-loading="submitting" @ok="submit">
    <!-- 素材分类（与安卓端 SourceImporter / 后端 SourceType 枚举对齐） -->
    <div class="type-row">
      <button
        v-for="t in TYPES"
        :key="t.value"
        type="button"
        class="type-chip"
        :class="{ active: type === t.value }"
        @click="type = t.value"
      >
        {{ t.label }}
      </button>
    </div>

    <a-tabs v-model:activeKey="mode">
      <a-tab-pane key="text" tab="粘贴文本">
        <a-textarea
          v-model:value="text"
          :rows="7"
          :placeholder="`粘贴${currentLabel}内容：聊天记录、回忆、口述…`"
        />
      </a-tab-pane>
      <a-tab-pane key="file" tab="上传文件">
        <a-upload
          :before-upload="() => false"
          :max-count="1"
          v-model:file-list="fileList"
        >
          <a-button>选择文件</a-button>
        </a-upload>
        <div class="hint">支持 txt / json / html / csv（微信/QQ 导出等），直接读取文本内容</div>
      </a-tab-pane>
      <a-tab-pane key="list" tab="已导入">
        <a-spin :spinning="listLoading">
          <template v-if="sources.length">
            <div v-for="s in sources" :key="s.id" class="source-row">
              <div class="source-row__main">
                <div class="source-row__head">
                  <a-tag :color="typeColor(s.type)" class="source-row__type">{{ typeLabel(s.type) }}</a-tag>
                  <span class="source-row__title">{{ s.fileName || brief(s.content) }}</span>
                </div>
                <div class="source-row__meta">
                  <span v-if="s.messageCount">{{ s.messageCount }} 条记录</span>
                  <span v-if="s.createdAt">{{ s.createdAt.slice(0, 10) }}</span>
                  <span v-if="!s.fileName && s.content">{{ s.content.length }} 字</span>
                </div>
                <a-collapse v-if="s.analysis" ghost class="source-row__analysis">
                  <a-collapse-panel header="🔍 AI 分析">
                    <div class="analysis-body">{{ prettyAnalysis(s.analysis) }}</div>
                  </a-collapse-panel>
                </a-collapse>
              </div>
              <a-popconfirm title="删除该素材？" @confirm="removeSource(s)">
                <a-button size="small" danger type="text">删除</a-button>
              </a-popconfirm>
            </div>
          </template>
          <div v-else class="hint">还没有素材 · 粘贴文本或上传文件后这里会出现列表</div>
        </a-spin>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { UploadFile } from 'ant-design-vue'
import { deleteSource, importSource, listSources, uploadSource } from '@/api'
import type { Source } from '@/types'

const props = defineProps<{ crushId: number }>()
const emit = defineEmits<{ (e: 'imported'): void }>()

const open = defineModel<boolean>('open')

/** 素材分类（与安卓 SourceImporter.TYPES 一致） */
const TYPES = [
  { value: 'TEXT', label: '📝 普通' },
  { value: 'WECHAT', label: '💬 微信' },
  { value: 'QQ', label: '🐧 QQ' },
  { value: 'SOCIAL', label: '🌐 社交' },
  { value: 'PHOTO', label: '📷 照片' },
] as const

const mode = ref<'text' | 'file' | 'list'>('text')
const type = ref<string>('TEXT')
const text = ref('')
const fileList = ref<UploadFile[]>([])
const submitting = ref(false)

const sources = ref<Source[]>([])
const listLoading = ref(false)

const currentLabel = computed(() => TYPES.find((t) => t.value === type.value)?.label ?? '')

watch(open, (v) => {
  if (v) {
    text.value = ''
    fileList.value = []
    mode.value = 'text'
    type.value = 'TEXT'
    loadSources()
  }
})

/** 已导入素材列表 */
async function loadSources() {
  if (!props.crushId) return
  listLoading.value = true
  try {
    sources.value = await listSources(props.crushId)
  } catch {
    sources.value = []
  } finally {
    listLoading.value = false
  }
}

async function submit() {
  if (mode.value === 'text') {
    if (!text.value.trim()) {
      message.warning('请输入内容')
      return
    }
    submitting.value = true
    try {
      await importSource(props.crushId, { type: type.value, content: text.value.trim() })
      message.success('导入成功')
      open.value = false
      emit('imported')
    } finally {
      submitting.value = false
    }
  } else {
    const file = fileList.value[0]?.originFileObj as File | undefined
    if (!file) {
      message.warning('请选择文件')
      return
    }
    submitting.value = true
    try {
      await uploadSource(props.crushId, file, type.value)
      message.success('导入成功')
      open.value = false
      emit('imported')
    } finally {
      submitting.value = false
    }
  }
}

async function removeSource(s: Source) {
  try {
    await deleteSource(props.crushId, s.id)
    message.success('已删除')
    await loadSources()
    emit('imported')
  } catch { /* handled by interceptor */ }
}

function typeLabel(t: string): string {
  return TYPES.find((x) => x.value === t)?.label?.replace(/^\S+\s/, '') || t || '素材'
}

function typeColor(t: string): string {
  const map: Record<string, string> = {
    TEXT: 'geekblue',
    WECHAT: 'green',
    QQ: 'blue',
    SOCIAL: 'purple',
    PHOTO: 'orange',
  }
  return map[t] || 'default'
}

/** 内容摘要（无文件名时展示首行） */
function brief(content?: string): string {
  if (!content) return '素材'
  const line = content.trim().split('\n')[0] || ''
  return line.length > 24 ? line.slice(0, 24) + '…' : line || '素材'
}

/**
 * 解析 LLM 分析 JSON 为可读文本（对齐安卓 SourceImporter.readAnalysis）：
 * keyPoints / facts / portraitClues / emotionSignals / risks 各字段逐行展示，失败回落原文
 */
function prettyAnalysis(json: string): string {
  if (!json) return ''
  try {
    const start = json.indexOf('{')
    const end = json.lastIndexOf('}')
    const obj = JSON.parse(start >= 0 && end > start ? json.slice(start, end + 1) : json)
    if (obj && typeof obj === 'object') {
      const labels: Record<string, string> = {
        keyPoints: '关键要点',
        facts: '事实清单',
        portraitClues: '画像线索',
        emotionSignals: '情感信号',
        risks: '风险提示',
      }
      const lines: string[] = []
      for (const [key, label] of Object.entries(labels)) {
        const v = obj[key]
        if (typeof v === 'string' && v.trim()) {
          lines.push(`${label}：${v.trim()}`)
        } else if (Array.isArray(v) && v.length) {
          lines.push(`${label}：${v.join('；')}`)
        }
      }
      if (lines.length) return lines.join('\n')
      if (typeof obj.raw === 'string' && obj.raw.trim()) return '已记录（原文本）'
    }
  } catch { /* fallthrough */ }
  return json.replace(/\n/g, ' ')
}
</script>

<style scoped>
.type-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.type-chip {
  border: 1px solid var(--cupid-border);
  background: #fff;
  color: var(--cupid-text-secondary);
  border-radius: 999px;
  padding: 4px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: all var(--cupid-transition);
}

.type-chip:hover {
  border-color: var(--cupid-primary);
  color: var(--cupid-primary);
}

.type-chip.active {
  background: var(--cupid-gradient-soft);
  border-color: var(--cupid-primary);
  color: var(--cupid-primary);
  font-weight: 600;
}

.hint {
  margin-top: 10px;
  padding: 8px 12px;
  background: var(--cupid-gradient-soft);
  border-radius: var(--cupid-radius-sm);
  color: var(--cupid-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.source-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 4px;
  border-bottom: 1px dashed var(--cupid-border);
}

.source-row:last-child {
  border-bottom: none;
}

.source-row__main {
  flex: 1;
  min-width: 0;
}

.source-row__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.source-row__type {
  border-radius: 8px;
  font-size: 11px;
  line-height: 16px;
  flex-shrink: 0;
}

.source-row__title {
  font-size: 13px;
  color: var(--cupid-text);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-row__meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: var(--cupid-text-muted);
  margin-top: 4px;
}

.source-row__analysis :deep(.ant-collapse-header) {
  padding: 6px 0 !important;
  font-size: 12px;
  color: var(--cupid-text-secondary);
}

.analysis-body {
  font-size: 12px;
  line-height: 1.7;
  color: var(--cupid-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
