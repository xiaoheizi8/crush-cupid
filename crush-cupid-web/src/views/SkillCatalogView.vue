<!--
  @className SkillCatalogView.vue
  @description Skill 目录页：使用 PageContainer，卡片化展示远端 Skill 信息与可用 Prompts
  @author cupid
  @code view
  @createTime 2026-08-26
-->
<template>
  <PageContainer
    icon="📚"
    title="Skill 目录"
    subtitle="查看远端 Skill 信息与可用 Prompts"
  >
    <a-spin :spinning="loading" class="skill-spin">
      <div class="skill-page cupid-fade-in">
        <!-- Skill 基本信息 -->
        <div class="info-card" v-if="catalog">
          <div class="info-card__head">
            <div class="info-card__icon">📦</div>
            <div class="info-card__title">远端 Skill 信息</div>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <div class="info-item__label">name</div>
              <div class="info-item__value">{{ catalog.skill.name }}</div>
            </div>
            <div class="info-item">
              <div class="info-item__label">version</div>
              <div class="info-item__value">
                <a-tag color="pink" class="version-tag">{{ catalog.skill.version }}</a-tag>
              </div>
            </div>
            <div class="info-item info-item--full">
              <div class="info-item__label">description</div>
              <div class="info-item__value">{{ catalog.skill.description }}</div>
            </div>
            <div class="info-item info-item--full">
              <div class="info-item__label">argument-hint</div>
              <div class="info-item__value info-item__value--mono">{{ catalog.skill.argumentHint }}</div>
            </div>
          </div>
        </div>

        <!-- 可用 Prompts -->
        <div class="prompts-card">
          <div class="prompts-card__head">
            <div class="prompts-card__title">
              <span>🗂️</span> 可用 Prompts
            </div>
            <div class="prompts-card__count" v-if="catalog">
              共 {{ catalog.prompts.length }} 个
            </div>
          </div>
          <div class="prompts-grid" v-if="catalog && catalog.prompts.length">
            <div
              v-for="item in catalog.prompts"
              :key="item"
              class="prompt-tile"
              @click="loadPrompt(item)"
            >
              <div class="prompt-tile__icon">📄</div>
              <div class="prompt-tile__name">{{ item }}</div>
              <div class="prompt-tile__action">查看 →</div>
            </div>
          </div>
          <a-empty v-else description="暂无 Prompts" />
        </div>

        <!-- 军师模式子命令 -->
        <div class="prompts-card" v-if="advisorCommands.length">
          <div class="prompts-card__head">
            <div class="prompts-card__title">
              <span>🧠</span> 军师模式子命令
            </div>
            <div class="prompts-card__count">共 {{ advisorCommands.length }} 个</div>
          </div>
          <div class="advisor-tip">
            军师已升级为独立页面：「军师」对话有专属会话记忆，不污染「对话」页的聊天记录。
          </div>
          <a-button class="advisor-jump" type="primary" @click="goAdvisor">
            🧠 去军师页对话
          </a-button>
          <div class="prompts-grid">
            <div
              v-for="cmd in advisorCommands"
              :key="cmd.name"
              class="prompt-tile"
              @click="openAdvisor(cmd)"
            >
              <div class="prompt-tile__icon">🎯</div>
              <div class="prompt-tile__name">{{ cmd.title }}</div>
              <div class="advisor-trigger">{{ cmd.trigger }}</div>
              <div class="prompt-tile__desc">{{ cmd.description }}</div>
              <div class="prompt-tile__action">咨询 →</div>
            </div>
          </div>
        </div>

        <!-- 关系报告 -->
        <div class="prompts-card" v-if="crushes.length || reportBusy">
          <div class="prompts-card__head">
            <div class="prompts-card__title">
              <span>📑</span> 关系报告
            </div>
          </div>
          <div class="report-body">
            <div class="report-row">
              <span class="report-label">暗恋对象</span>
              <a-select
                v-model:value="reportSlug"
                placeholder="选择暗恋对象"
                style="width: 240px"
                :options="crushOptions"
              />
            </div>
            <div class="report-row report-row--actions">
              <a-button type="primary" :loading="reportBusy" @click="doGenerate">生成报告</a-button>
              <a-button :disabled="!reportMd" @click="doDownload">导出 Word (.docx)</a-button>
            </div>
            <a-spin :spinning="reportBusy">
              <pre v-if="reportMd" class="report-pre">{{ reportMd }}</pre>
              <div v-else class="report-placeholder">
                选择暗恋对象后点击「生成报告」，AI 军师会综合画像与聊天记录产出关系进展报告。
              </div>
            </a-spin>

            <!-- 报告历史 -->
            <div class="report-history" v-if="reportHistory.length">
              <div class="report-history__title">📚 历史报告（{{ reportHistory.length }}）</div>
              <div
                v-for="r in reportHistory"
                :key="r.id"
                class="report-history__item"
                @click="openDetail(r)"
              >
                <div class="report-history__info">
                  <div class="report-history__name">{{ r.title || '关系报告' }}</div>
                  <div class="report-history__meta">
                    {{ r.reportDate || '—' }}
                    <a-tag v-if="r.source === 'scheduled'" color="blue" class="report-history__src">定时</a-tag>
                    <a-tag v-else color="green" class="report-history__src">手动</a-tag>
                  </div>
                </div>
                <div class="report-history__ops" @click.stop>
                  <a-button size="small" @click="downloadSaved(r)">下载</a-button>
                  <a-popconfirm title="删除该报告？" @confirm="removeReport(r)">
                    <a-button size="small" danger>删除</a-button>
                  </a-popconfirm>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-spin>

    <!-- Prompt 详情弹窗 -->
    <a-modal
      v-model:open="promptOpen"
      :title="`📄 prompt: ${currentPrompt}`"
      width="760"
      :footer="null"
    >
      <pre class="prompt-pre">{{ promptContent }}</pre>
    </a-modal>

    <!-- 军师咨询弹窗 -->
    <a-modal
      v-model:open="advisorOpen"
      :title="`🎯 军师 · ${currentAdvisor?.title || ''}`"
      width="680"
      :confirm-loading="advisorBusy"
      @ok="doAdvisorInvoke"
    >
      <div class="advisor-modal">
        <div class="report-row">
          <span class="report-label">触发命令</span>
          <a-tag color="purple">{{ currentAdvisor?.trigger }}</a-tag>
        </div>
        <a-textarea
          v-if="!currentAdvisor?.requiresCrush"
          v-model:value="advisorQuestion"
          :rows="4"
          placeholder="简单描述你的情况 / 或粘贴你想给军师看的聊天记录…"
        />
        <div v-else class="report-row">
          <span class="report-label">暗恋对象</span>
          <a-select
            v-model:value="reportSlug"
            placeholder="选择暗恋对象"
            style="width: 240px"
            :options="crushOptions"
          />
        </div>
        <pre v-if="advisorResult" class="report-pre">{{ advisorResult }}</pre>
      </div>
    </a-modal>

    <!-- 历史报告详情弹窗 -->
    <a-modal
      v-model:open="detailOpen"
      :title="'📑 报告详情'"
      width="780"
      :footer="null"
    >
      <a-spin :spinning="detailBusy">
        <pre class="report-pre">{{ detailContent }}</pre>
      </a-spin>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
/**
 * Skill 目录页：加载远端 Skill 元信息与 Prompts
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  deleteReport,
  downloadReportLive,
  downloadSavedReport,
  generateReport,
  getReportDetail,
  getSkillCatalog,
  getSkillPrompt,
  invokeAdvisor,
  listAdvisorCommands,
  listCrushes,
  listReports,
} from '@/api'
import { message } from 'ant-design-vue'
import type { AdvisorCommand, Crush, CrushReport, SkillCatalog } from '@/types'
import PageContainer from '@/components/PageContainer.vue'

const catalog = ref<SkillCatalog | null>(null)
const loading = ref(false)
const promptOpen = ref(false)
const currentPrompt = ref('')
const promptContent = ref('')

const router = useRouter()

const advisorCommands = ref<AdvisorCommand[]>([])
const advisorOpen = ref(false)
const advisorBusy = ref(false)
const currentAdvisor = ref<AdvisorCommand | null>(null)
const advisorQuestion = ref('')
const advisorResult = ref('')

const crushes = ref<Crush[]>([])
const reportSlug = ref('')
const reportBusy = ref(false)
const reportMd = ref('')
const reportHistory = ref<CrushReport[]>([])
const historyLoading = ref(false)
const detailOpen = ref(false)
const detailBusy = ref(false)
const detailContent = ref('')

const crushOptions = computed(() =>
  crushes.value.map((c) => ({ label: c.name, value: c.slug })),
)

/** 加载 Skill 目录 + 军师子命令 + 暗恋对象列表 */
async function load() {
  loading.value = true
  try {
    const [cat, adv, cs] = await Promise.all([
      getSkillCatalog(),
      listAdvisorCommands(),
      listCrushes(), // eslint-disable-line @typescript-eslint/no-unused-vars
    ])
    catalog.value = cat
    advisorCommands.value = adv
    crushes.value = cs
  } finally {
    loading.value = false
  }
}

/** 加载并预览某个 Prompt */
async function loadPrompt(name: string) {
  currentPrompt.value = name
  promptContent.value = '加载中…'
  promptOpen.value = true
  try {
    promptContent.value = await getSkillPrompt(name)
  } catch (e) {
    promptContent.value = e instanceof Error ? e.message : '加载失败'
  }
}

/** 打开军师咨询弹窗 */
function openAdvisor(cmd: AdvisorCommand) {
  currentAdvisor.value = cmd
  advisorQuestion.value = ''
  advisorResult.value = ''
  advisorOpen.value = true
}

/** 跳转到独立军师页 */
function goAdvisor() {
  router.push('/advisor')
}

/** 调用军师子命令 */
async function doAdvisorInvoke() {
  if (!currentAdvisor.value) return
  advisorBusy.value = true
  advisorResult.value = ''
  try {
    advisorResult.value = await invokeAdvisor({
      name: currentAdvisor.value.name,
      question: advisorQuestion.value || undefined,
      crushSlug: currentAdvisor.value.requiresCrush ? reportSlug.value : undefined,
    })
  } finally {
    advisorBusy.value = false
  }
}

/** 生成关系报告（落库），刷新历史 */
async function doGenerate() {
  if (!reportSlug.value) return
  reportBusy.value = true
  reportMd.value = ''
  try {
    const report = await generateReport(reportSlug.value)
    reportMd.value = report.markdown || ''
    message.success('报告已生成并保存')
    await loadHistory()
  } catch {
    /* handled by http interceptor */
  } finally {
    reportBusy.value = false
  }
}

/** 下载刚生成的报告 .docx（未落库也适用） */
async function doDownload() {
  if (!reportSlug.value) return
  try {
    await downloadReportLive(reportSlug.value, reportMd.value || undefined)
  } catch {
    /* handled by http interceptor */
  }
}

/** 加载当前 crush 的报告历史 */
async function loadHistory() {
  if (!reportSlug.value) {
    reportHistory.value = []
    return
  }
  historyLoading.value = true
  try {
    reportHistory.value = await listReports(reportSlug.value)
  } catch {
    reportHistory.value = []
  } finally {
    historyLoading.value = false
  }
}

/** 打开历史报告详情（取 markdown 全文） */
async function openDetail(report: CrushReport) {
  detailOpen.value = true
  detailContent.value = report.markdown || '加载中…'
  if (!report.markdown) {
    detailBusy.value = true
    try {
      const full = await getReportDetail(report.id)
      detailContent.value = full.markdown || ''
      report.markdown = full.markdown
    } catch {
      detailContent.value = '加载失败'
    } finally {
      detailBusy.value = false
    }
  }
}

/** 删除一条报告历史 */
async function removeReport(report: CrushReport) {
  try {
    await deleteReport(report.id)
    message.success('已删除')
    await loadHistory()
  } catch {
    /* handled */
  }
}

/** 下载某条历史报告 .docx */
async function downloadSaved(r: CrushReport) {
  try {
    await downloadSavedReport(r.id, r.crushName)
  } catch {
    /* handled */
  }
}

// 切换 crush 时刷新历史
watch(reportSlug, () => {
  loadHistory()
})

onMounted(load)
</script>

<style scoped>
.skill-spin {
  display: block;
}

.skill-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* Skill 信息卡片 */
.info-card {
  background: var(--cupid-bg-card);
  border: 1px solid var(--cupid-border);
  border-radius: var(--cupid-radius-lg);
  box-shadow: var(--cupid-shadow-sm);
  overflow: hidden;
}

.info-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  background: var(--cupid-gradient-soft);
  border-bottom: 1px solid var(--cupid-border);
}

.info-card__icon {
  font-size: 18px;
}

.info-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cupid-text);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
}

.info-item {
  padding: 16px 20px;
  border-right: 1px solid var(--cupid-border);
  border-bottom: 1px solid var(--cupid-border);
  transition: background var(--cupid-transition);
}

.info-item:hover {
  background: var(--cupid-bg-hover);
}

.info-item:nth-child(2n) {
  border-right: none;
}

.info-item--full {
  grid-column: 1 / -1;
  border-right: none;
}

.info-item__label {
  font-size: 11px;
  color: var(--cupid-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.info-item__value {
  color: var(--cupid-text);
  font-size: 14px;
  line-height: 1.6;
}

.info-item__value--mono {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  background: #fafafa;
  padding: 4px 8px;
  border-radius: 6px;
  display: inline-block;
}

.version-tag {
  border-radius: 10px !important;
  font-weight: 600;
}

/* Prompts 卡片 */
.prompts-card {
  background: var(--cupid-bg-card);
  border: 1px solid var(--cupid-border);
  border-radius: var(--cupid-radius-lg);
  box-shadow: var(--cupid-shadow-sm);
  overflow: hidden;
}

.prompts-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--cupid-gradient-soft);
  border-bottom: 1px solid var(--cupid-border);
}

.prompts-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cupid-text);
}

.prompts-card__count {
  font-size: 12px;
  color: var(--cupid-text-secondary);
}

.prompts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  padding: 16px 20px;
}

.prompt-tile {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px;
  background: var(--cupid-gradient-soft);
  border: 1px solid var(--cupid-border);
  border-radius: var(--cupid-radius);
  cursor: pointer;
  transition: all var(--cupid-transition);
}

.prompt-tile:hover {
  background: #fff;
  border-color: var(--cupid-primary);
  box-shadow: var(--cupid-shadow);
  transform: translateY(-2px);
}

.prompt-tile__icon {
  font-size: 22px;
}

.prompt-tile__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--cupid-text);
  word-break: break-all;
}

.prompt-tile__action {
  font-size: 12px;
  color: var(--cupid-primary);
  font-weight: 600;
}

/* Prompt 预览 */
.prompt-pre {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 60vh;
  overflow-y: auto;
  background: #1f1722;
  color: #ffd6df;
  padding: 16px;
  border-radius: var(--cupid-radius-sm);
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
}

/* 军师 tip + 触发词 */
.advisor-tip {
  margin: 12px 20px 0;
  padding: 12px 16px;
  background: #f8f0ff;
  border: 1px solid var(--cupid-border);
  border-left: 3px solid #b37feb;
  border-radius: var(--cupid-radius-sm);
  font-size: 13px;
  line-height: 1.6;
}

.advisor-jump {
  margin: 12px 20px 0;
  border-radius: var(--cupid-radius-sm) !important;
}

.advisor-trigger {
  font-size: 12px;
  color: #7d3fbf;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.prompt-tile__desc {
  font-size: 12px;
  color: var(--cupid-text-secondary);
  line-height: 1.5;
}

/* 关系报告 / 军师弹窗 */
.report-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.report-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.report-row--actions {
  gap: 12px;
}

.report-label {
  font-size: 13px;
  color: var(--cupid-text-secondary);
  min-width: 60px;
}

.report-pre {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 52vh;
  overflow-y: auto;
  background: #fffdf7;
  color: var(--cupid-text);
  padding: 16px;
  border-radius: var(--cupid-radius-sm);
  border: 1px solid var(--cupid-border);
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
}

.report-placeholder {
  padding: 22px 16px;
  text-align: center;
  color: var(--cupid-text-muted);
  font-size: 13px;
  border: 1px dashed var(--cupid-border);
  border-radius: var(--cupid-radius-sm);
}

.advisor-modal {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 报告历史 */
.report-history {
  border-top: 1px dashed var(--cupid-border);
  padding-top: 12px;
}

.report-history__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--cupid-text);
  margin-bottom: 8px;
}

.report-history__item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--cupid-border);
  border-radius: var(--cupid-radius);
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color var(--cupid-transition), box-shadow var(--cupid-transition);
}

.report-history__item:hover {
  border-color: var(--cupid-primary);
  box-shadow: var(--cupid-shadow-sm);
}

.report-history__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.report-history__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--cupid-text);
}

.report-history__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--cupid-text-muted);
}

.report-history__src {
  line-height: 16px;
  font-size: 11px;
}

.report-history__ops {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
</style>
