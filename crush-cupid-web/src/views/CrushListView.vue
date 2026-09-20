
<template>
  <PageContainer
    icon="💞"
    title="暗恋对象"
    subtitle="管理你的 crush 列表，构建专属 AI 人格"
  >
    <template #extra>
      <a-button type="primary" @click="openCreate">
        <span>➕</span>&nbsp;新建
      </a-button>
      <a-button @click="load">
        <span>🔄</span>&nbsp;刷新
      </a-button>
    </template>

    <div class="crush-list cupid-fade-in">
      <!-- 移动端卡片列表（窄屏时替代表格） -->
      <div v-if="isMobile" class="crush-cards">
        <div v-for="record in crushes" :key="record.id" class="crush-card">
          <div class="crush-card__top">
            <div class="crush-card__avatar">{{ record.name?.charAt(0) || '?' }}</div>
            <div class="crush-card__info">
              <div class="crush-card__name">{{ record.name }}</div>
              <div class="crush-card__slug">{{ record.slug }}</div>
            </div>
            <a-tag :color="record.status === 'READY' ? 'green' : 'orange'" class="status-tag">
              {{ record.status || 'DRAFT' }}
            </a-tag>
          </div>
          <div class="crush-card__meta">
            <span v-if="record.mbti">{{ record.mbti }}</span>
            <span v-if="record.zodiac">{{ record.zodiac }}</span>
            <span v-if="!record.mbti && !record.zodiac" class="crush-card__id">#{{ record.id }}</span>
          </div>
          <div class="crush-card__ops">
            <a class="action-link" @click="openEdit(record)">编辑</a>
            <a class="action-link" @click="openImport(record)">导入</a>
            <a class="action-link action-link--primary" @click="build(record)">构建</a>
            <a-popconfirm title="确定删除？" @confirm="remove(record)">
              <a class="action-link action-link--danger">删除</a>
            </a-popconfirm>
          </div>
        </div>
        <div v-if="!loading && crushes.length === 0" class="crush-card__empty">还没有暗恋对象，点右上角「新建」开始～</div>
      </div>

      <!-- 桌面表格 -->
      <a-table
        v-else
        :data-source="crushes"
        :columns="columns"
        row-key="id"
        :loading="loading"
        size="middle"
        class="crush-table"
        :pagination="{ pageSize: 10, hideOnSinglePage: true }"
        :scroll="{ x: 760 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="name-cell">
              <div class="name-cell__avatar">{{ record.name?.charAt(0) || '?' }}</div>
              <div class="name-cell__text">
                <div class="name-cell__name">{{ record.name }}</div>
                <div class="name-cell__slug">{{ record.slug }}</div>
              </div>
            </div>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'READY' ? 'green' : 'orange'" class="status-tag">
              {{ record.status || 'DRAFT' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="middle">
              <a class="action-link" @click="openEdit(record)">编辑</a>
              <a-divider type="vertical" class="action-divider" />
              <a class="action-link" @click="openImport(record)">导入</a>
              <a-divider type="vertical" class="action-divider" />
              <a class="action-link action-link--primary" @click="build(record)">构建</a>
              <a-divider type="vertical" class="action-divider" />
              <a-popconfirm title="确定删除？" @confirm="remove(record)">
                <a class="action-link action-link--danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>

      <SourceImportModal v-model:open="importOpen" :crush-id="importCrushId" @imported="load" />

      <!-- 新建/编辑弹窗 -->
      <a-modal
        v-model:open="modalOpen"
        :title="editing ? '编辑暗恋对象' : '新建暗恋对象'"
        :confirm-loading="saving"
        width="min(560px, 96vw)"
        @ok="submit"
      >
        <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" class="crush-form">
          <a-form-item label="花名" required>
            <a-input v-model:value="form.name" placeholder="如：小美" />
          </a-form-item>
          <a-form-item label="slug" required>
            <a-input v-model:value="form.slug" :disabled="!!editing" placeholder="如：xiaomei" />
          </a-form-item>
          <a-form-item label="MBTI"><a-input v-model:value="form.mbti" placeholder="如：INFJ" /></a-form-item>
          <a-form-item label="星座"><a-input v-model:value="form.zodiac" placeholder="如：双鱼" /></a-form-item>
          <a-form-item label="职业"><a-input v-model:value="form.occupation" /></a-form-item>
          <a-form-item label="性别"><a-input v-model:value="form.gender" /></a-form-item>
          <a-form-item label="认识时长"><a-input v-model:value="form.knowDuration" /></a-form-item>
          <a-form-item label="关系状态"><a-input v-model:value="form.relationshipStatus" /></a-form-item>
          <a-form-item label="印象"><a-textarea v-model:value="form.impression" :rows="3" /></a-form-item>
          <a-form-item label="音色ID">
            <a-input v-model:value="form.voiceId" placeholder="CosyVoice voice_id，空则走默认音色" />
          </a-form-item>
        </a-form>
        <div class="form-voice-help">
          💡 各音色 voice_id 参考：<a href="https://help.aliyun.com/zh/model-studio/voices" target="_blank" rel="noopener noreferrer">CosyVoice 音色参考 ↗</a>
        </div>
      </a-modal>

      <!-- 构建结果弹窗 -->
      <a-modal
        v-model:open="buildOpen"
        :title="buildResult ? '✅ 构建完成' : '⏳ 构建中…'"
        width="min(640px, 96vw)"
        :footer="null"
      >
        <div class="build-log">
          <div v-for="(line, i) in buildLog" :key="i" class="log-line">{{ line }}</div>
        </div>
        <template v-if="buildResult">
          <a-descriptions :column="1" size="small" bordered class="build-result">
            <a-descriptions-item label="性格">{{ buildResult.personaSummary }}</a-descriptions-item>
            <a-descriptions-item label="记忆">{{ buildResult.memorySummary }}</a-descriptions-item>
            <a-descriptions-item label="版本">v{{ buildResult.version }} · {{ buildResult.status }}</a-descriptions-item>
          </a-descriptions>
          <a-button type="primary" block size="large" class="build-done-btn" @click="buildOpen = false">
            完成
          </a-button>
        </template>
      </a-modal>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
/**
 * 暗恋对象列表页：增删改查 + 构建
 */
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { buildCrush, createCrush, deleteCrush, listCrushes, updateCrush } from '@/api'
import type { BuildResult, Crush, CrushCreatePayload } from '@/types'
import SourceImportModal from '@/components/SourceImportModal.vue'
import PageContainer from '@/components/PageContainer.vue'

/** 窄屏时用卡片列表渲染（替代横向挤压的表格） */
const isMobile = ref(false)

/** 表格列定义 */
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '花名', key: 'name' },
  { title: 'MBTI', dataIndex: 'mbti', key: 'mbti', width: 90 },
  { title: '星座', dataIndex: 'zodiac', key: 'zodiac', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 260 },
]

const crushes = ref<Crush[]>([])
const loading = ref(false)
const modalOpen = ref(false)
const saving = ref(false)
const editing = ref<Crush | null>(null)

const importOpen = ref(false)
const importCrushId = ref(0)

const buildOpen = ref(false)
const buildLog = ref<string[]>([])
const buildResult = ref<BuildResult | null>(null)

/** 表单数据 */
const form = reactive<CrushCreatePayload>({
  name: '',
  slug: '',
  mbti: '',
  zodiac: '',
  occupation: '',
  gender: '',
  knowDuration: '',
  relationshipStatus: '',
  impression: '',
  voiceId: '',
})

/** 重置表单 */
function resetForm() {
  Object.assign(form, {
    name: '',
    slug: '',
    mbti: '',
    zodiac: '',
    occupation: '',
    gender: '',
    knowDuration: '',
    relationshipStatus: '',
    impression: '',
    voiceId: '',
  })
}

/** 加载列表 */
async function load() {
  loading.value = true
  try {
    crushes.value = await listCrushes()
  } finally {
    loading.value = false
  }
}

/** 打开新建弹窗 */
function openCreate() {
  editing.value = null
  resetForm()
  modalOpen.value = true
}

/** 打开编辑弹窗 */
function openEdit(record: Crush) {
  editing.value = record
  Object.assign(form, {
    name: record.name,
    slug: record.slug,
    mbti: record.mbti ?? '',
    zodiac: record.zodiac ?? '',
    occupation: record.occupation ?? '',
    gender: record.gender ?? '',
    knowDuration: record.knowDuration ?? '',
    relationshipStatus: record.relationshipStatus ?? '',
    impression: record.impression ?? '',
    voiceId: record.voiceId ?? '',
  })
  modalOpen.value = true
}

/** 打开导入弹窗 */
function openImport(record: Crush) {
  if (!record.id) return
  importCrushId.value = record.id
  importOpen.value = true
}

/** 构建 crush 人格 */
async function build(record: Crush) {
  if (!record.id) return
  buildOpen.value = true
  buildLog.value = []
  buildResult.value = null
  try {
    await buildCrush(record.id, (ev) => {
      if (ev.type === 'progress' && ev.message) buildLog.value.push(ev.message)
      if (ev.type === 'error') buildLog.value.push('[错误] ' + (ev.message || ''))
      if (ev.type === 'done' && ev.result) buildResult.value = ev.result
    })
  } catch (e) {
    buildLog.value.push('[错误] ' + (e instanceof Error ? e.message : '构建失败'))
  } finally {
    await load()
  }
}

/** 提交新建/编辑 */
async function submit() {
  if (!form.name?.trim() || !form.slug?.trim()) {
    message.warning('花名和 slug 必填')
    return
  }
  saving.value = true
  try {
    if (editing.value?.id) {
      await updateCrush(editing.value.id, form)
    } else {
      await createCrush(form)
    }
    message.success('保存成功')
    modalOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

/** 删除 */
async function remove(record: Crush) {
  if (!record.id) return
  await deleteCrush(record.id)
  message.success('已删除')
  await load()
}

onMounted(() => {
  const mql = window.matchMedia('(max-width: 768px)')
  isMobile.value = mql.matches
  mql.addEventListener('change', (e) => {
    isMobile.value = e.matches
  })
  load()
})
</script>

<style scoped>
.crush-list {
  background: var(--cupid-bg-card);
  border: 1px solid var(--cupid-border);
  border-radius: var(--cupid-radius);
  box-shadow: var(--cupid-shadow-sm);
  overflow: hidden;
}

/* 移动端卡片列表 */
.crush-cards {
  display: flex;
  flex-direction: column;
}
.crush-card {
  padding: 14px 16px;
  border-bottom: 1px solid var(--cupid-border);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.crush-card:last-child {
  border-bottom: none;
}
.crush-card__top {
  display: flex;
  align-items: center;
  gap: 12px;
}
.crush-card__avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: var(--cupid-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 18px;
  flex-shrink: 0;
  box-shadow: var(--cupid-shadow-sm);
}
.crush-card__info {
  flex: 1;
  min-width: 0;
}
.crush-card__name {
  font-weight: 600;
  color: var(--cupid-text);
  font-size: 15px;
}
.crush-card__slug {
  font-size: 12px;
  color: var(--cupid-text-muted);
  margin-top: 2px;
}
.crush-card__meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: var(--cupid-text-secondary);
}
.crush-card__id {
  color: var(--cupid-text-muted);
}
.crush-card__ops {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-top: 2px;
}
.crush-card__empty {
  padding: 40px 16px;
  text-align: center;
  color: var(--cupid-text-muted);
  font-size: 14px;
}

/* 表格美化 */
.crush-table :deep(.ant-table-thead) {
  background: var(--cupid-gradient-soft) !important;
}

.crush-table :deep(.ant-table-thead > tr > th) {
  background: transparent !important;
  border-bottom: 1px solid var(--cupid-border) !important;
  font-weight: 600;
  color: var(--cupid-text);
  font-size: 13px;
}

.crush-table :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid var(--cupid-border) !important;
}

.crush-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--cupid-bg-hover) !important;
}

.crush-table :deep(.ant-table-tbody > tr) {
  transition: background var(--cupid-transition);
}

/* 名称单元格：头像 + 名称 */
.name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-cell__avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--cupid-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 15px;
  flex-shrink: 0;
  box-shadow: var(--cupid-shadow-sm);
}

.name-cell__name {
  font-weight: 600;
  color: var(--cupid-text);
}

.name-cell__slug {
  font-size: 12px;
  color: var(--cupid-text-muted);
  margin-top: 1px;
}

.status-tag {
  border-radius: 12px !important;
  font-size: 12px !important;
  padding: 2px 10px !important;
}

/* 操作链接 */
.action-link {
  color: var(--cupid-text-secondary);
  cursor: pointer;
  font-size: 13px;
  transition: color 0.2s;
}

.action-link:hover {
  color: var(--cupid-primary);
}

.action-link--primary {
  color: var(--cupid-primary);
  font-weight: 600;
}

.action-link--danger:hover {
  color: #ff4d4f !important;
}

.action-divider {
  margin: 0 !important;
  background: var(--cupid-border) !important;
}

/* 构建日志 */
.build-log {
  max-height: 220px;
  overflow-y: auto;
  background: #1f1722;
  border-radius: var(--cupid-radius-sm);
  padding: 12px 14px;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.log-line {
  color: #ffb3c0;
  font-size: 12.5px;
  line-height: 1.9;
}

.build-result {
  margin-top: 14px;
}

/* 音色参考链接 */
.form-voice-help {
  font-size: 12px;
  color: var(--cupid-text-secondary);
  padding: 0 2px 8px;
}
.form-voice-help a {
  color: var(--cupid-primary);
  font-weight: 600;
}

.build-done-btn {
  margin-top: 14px;
  border-radius: var(--cupid-radius-sm) !important;
}
</style>
