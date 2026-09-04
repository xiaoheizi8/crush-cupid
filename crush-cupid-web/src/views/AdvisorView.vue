
<template>
  <PageContainer icon="🧠" title="军师" subtitle="独立对话 · 帮你分析怎么追 ta，不走模拟话术">
    <div class="advisor-page">
      <a-row :gutter="20" class="advisor-row">
        <!-- 左侧：crush 选择 + 报告入口 -->
        <a-col :span="6">
          <div class="side-card">
            <div class="side-card__title">选择暗恋对象</div>
            <a-select
              v-model:value="currentSlug"
              placeholder="选择 crush"
              size="large"
              style="width: 100%"
              :loading="loading"
              :options="crushOptions"
            />
            <a-button
              size="large"
              block
              class="side-card__btn"
              :disabled="!currentSlug || reportBusy"
              @click="openReports"
            >
              <span>📚</span>&nbsp;关系报告
            </a-button>
            <div class="side-card__hint">
              <span>💡</span> 军师对话使用独立会话记忆，不会出现在「对话」页的聊天记录里。
            </div>
          </div>
        </a-col>

        <!-- 右侧：军师对话区 -->
        <a-col :span="18" class="advisor-col">
          <div class="chat-card">
            <div class="chat-card__head">
              <div class="chat-card__title">
                {{ currentSlug ? `🧠 军师 · ${currentName}` : '请先选择 crush' }}
              </div>
              <div class="chat-card__sub" v-if="currentCrush">
                {{ currentCrush.mbti || '—' }} · {{ currentCrush.zodiac || '—' }}
              </div>
            </div>

            <!-- 快捷指令 chips -->
            <div v-if="commands.length" class="quick-chips">
              <a-tag
                v-for="cmd in commands"
                :key="cmd.trigger"
                class="quick-chip"
                :color="cmd.requiresCrush ? 'purple' : 'geekblue'"
                :disabled="streaming"
                @click="sendQuick(cmd)"
              >
                {{ cmd.title }}
              </a-tag>
            </div>

            <div ref="msgBox" class="messages">
              <div v-if="messages.length === 0 && !streaming" class="empty">
                <div class="empty__icon">🧠</div>
                <div class="empty__text">说说你的情况，军师帮你出主意～</div>
              </div>
              <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
                <div class="avatar">{{ m.role === 'user' ? '🧑' : '🧠' }}</div>
                <div class="bubble">
                  <span>{{ m.content }}</span>
                  <span v-if="streaming && i === messages.length - 1" class="cursor">▋</span>
                </div>
              </div>
              <div v-if="streaming && !messages.length" class="msg assistant">
                <div class="avatar">🧠</div>
                <div class="bubble">
                  <span class="typing">军师思考中…</span>
                  <span class="cursor">▋</span>
                </div>
              </div>
            </div>

            <div class="input-row">
              <textarea
                ref="inputRef"
                v-model="input"
                :rows="2"
                placeholder="描述你的情况 / 粘贴聊天记录…（Enter 发送，Shift+Enter 换行）"
                :disabled="streaming"
                class="input-area native-textarea"
                @keydown="onKeydown"
              ></textarea>
              <a-button
                type="primary"
                size="large"
                class="send-btn"
                :loading="streaming"
                :disabled="!currentSlug || streaming || !input.trim()"
                @click="send"
              >
                问军师
              </a-button>
            </div>
          </div>
        </a-col>
      </a-row>

      <!-- 报告历史弹窗 -->
      <a-modal
        v-model:open="reportOpen"
        :title="`📚 关系报告 · ${currentName}`"
        width="760"
        :footer="null"
      >
        <div class="report-body">
          <div class="report-row report-row--actions">
            <a-button type="primary" :loading="reportBusy" @click="generateNow">生成新报告</a-button>
            <a-button
              :disabled="!reportMd"
              @click="downloadCurrent"
            >导出 Word (.docx)</a-button>
          </div>
          <a-spin :spinning="reportBusy">
            <pre v-if="reportMd" class="report-pre">{{ reportMd }}</pre>
            <div v-else class="report-placeholder">点击「生成新报告」由军师综合 ta 的资料生成一份关系分析报告。</div>
          </a-spin>
          <div class="report-history" v-if="reportHistory.length">
            <div class="report-history__title">📚 历史报告（{{ reportHistory.length }}）</div>
            <div v-for="r in reportHistory" :key="r.id" class="report-history__item">
              <div class="report-history__info">
                <div class="report-history__name">{{ r.title || '关系报告' }}</div>
                <div class="report-history__meta">
                  {{ r.reportDate || '—' }}
                  <a-tag v-if="r.source === 'scheduled'" color="blue">定时</a-tag>
                  <a-tag v-else color="green">手动</a-tag>
                </div>
              </div>
              <div class="report-history__ops" @click.stop>
                <a-button size="small" @click="downloadSaved(r)">下载</a-button>
                <a-button size="small" @click="loadDetail(r)">详情</a-button>
                <a-popconfirm title="删除该报告？" @confirm="removeReport(r)">
                  <a-button size="small" danger>删除</a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>
        </div>
      </a-modal>

      <!-- 报告详情弹窗 -->
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
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
/**
 * 军师页面：独立于模拟对话的咨询界面。
 * 使用 /api/chat/advisor（军师人设 + 独立内存记忆），配合军师子命令快捷入口与关系报告。
 */
import { computed, nextTick, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  advisorStreamChat,
  deleteReport,
  downloadReportLive,
  downloadSavedReport,
  generateReport,
  getReportDetail,
  listAdvisorCommands,
  listCrushes,
  listReports,
} from '@/api'
import type { AdvisorCommand, Crush, CrushReport } from '@/types'
import PageContainer from '@/components/PageContainer.vue'

interface AdvisorMessage {
  role: 'user' | 'assistant'
  content: string
}

const crushes = ref<Crush[]>([])
const loading = ref(false)
const currentSlug = ref<string>()
const messages = ref<AdvisorMessage[]>([])
const input = ref('')
const streaming = ref(false)
const commands = ref<AdvisorCommand[]>([])
const inputRef = ref<HTMLTextAreaElement>()
const msgBox = ref<HTMLElement>()

const reportOpen = ref(false)
const reportBusy = ref(false)
const reportMd = ref('')
const reportHistory = ref<CrushReport[]>([])
const detailOpen = ref(false)
const detailBusy = ref(false)
const detailContent = ref('')

const crushOptions = computed(() =>
  crushes.value.map((c) => ({ label: `${c.name} (${c.slug})`, value: c.slug })),
)
const currentName = computed(
  () => crushes.value.find((c) => c.slug === currentSlug.value)?.name ?? '',
)
const currentCrush = computed(() =>
  crushes.value.find((c) => c.slug === currentSlug.value),
)

async function loadCrushes() {
  loading.value = true
  try {
    crushes.value = await listCrushes()
    if (crushes.value.length && !currentSlug.value) {
      currentSlug.value = crushes.value[0].slug
    }
  } finally {
    loading.value = false
  }
}

async function loadCommands() {
  try {
    commands.value = await listAdvisorCommands()
  } catch {
    commands.value = []
  }
}

async function scrollToBottom() {
  await nextTick()
  if (msgBox.value) {
    msgBox.value.scrollTop = msgBox.value.scrollHeight
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.isComposing) return
  if (e.key !== 'Enter') return
  if (e.shiftKey || e.altKey || e.ctrlKey || e.metaKey) return
  e.preventDefault()
  void send()
}

/** 发送一条消息给军师并流式接收回复 */
async function sendUser(text: string) {
  if (!currentSlug.value || streaming.value) return
  const t = text.trim()
  if (!t) return
  messages.value.push({ role: 'user', content: t })
  input.value = ''
  streaming.value = true
  messages.value.push({ role: 'assistant', content: '' })
  const aiIdx = messages.value.length - 1
  await scrollToBottom()
  try {
    await advisorStreamChat(currentSlug.value, t, (chunk) => {
      if (!chunk.done && chunk.content) {
        messages.value[aiIdx].content += chunk.content
      }
      scrollToBottom()
    })
  } catch (e) {
    const msg = e instanceof Error ? e.message : '军师暂时掉线'
    messages.value[aiIdx].content = `[错误] ${msg}`
  } finally {
    streaming.value = false
    await scrollToBottom()
  }
}

function send() {
  void sendUser(input.value)
}

/** 点击快捷指令 chip：将指令语贴进输入框并触发（相当于替用户补全子命令） */
function sendQuick(cmd: AdvisorCommand) {
  if (streaming.value || !currentSlug.value) return
  void sendUser(`${cmd.title}：`)
}

/* ---------- 报告 ---------- */
async function openReports() {
  if (!currentSlug.value) return
  reportOpen.value = true
  reportMd.value = ''
  await loadReportHistory()
}

async function loadReportHistory() {
  if (!currentSlug.value) {
    reportHistory.value = []
    return
  }
  try {
    reportHistory.value = await listReports(currentSlug.value)
  } catch {
    reportHistory.value = []
  }
}

async function generateNow() {
  if (!currentSlug.value) return
  reportBusy.value = true
  reportMd.value = ''
  try {
    const report = await generateReport(currentSlug.value)
    reportMd.value = report.markdown || ''
    message.success('报告已生成并保存')
    await loadReportHistory()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '生成失败')
  } finally {
    reportBusy.value = false
  }
}

async function downloadCurrent() {
  if (!currentSlug.value) return
  try {
    await downloadReportLive(currentSlug.value, reportMd.value || undefined)
  } catch (e) {
    message.error(e instanceof Error ? e.message : '下载失败')
  }
}

async function downloadSaved(r: CrushReport) {
  try {
    await downloadSavedReport(r.id, currentName.value)
  } catch (e) {
    message.error(e instanceof Error ? e.message : '下载失败')
  }
}

async function loadDetail(r: CrushReport) {
  detailOpen.value = true
  detailBusy.value = true
  detailContent.value = r.markdown || '加载中…'
  try {
    if (!r.markdown) {
      const full = await getReportDetail(r.id)
      detailContent.value = full.markdown || '（无内容）'
    }
  } catch {
    detailContent.value = '加载失败'
  } finally {
    detailBusy.value = false
  }
}

async function removeReport(r: CrushReport) {
  try {
    await deleteReport(r.id)
    message.success('已删除')
    await loadReportHistory()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(() => {
  void loadCrushes()
  void loadCommands()
})
</script>

<style scoped>
.advisor-page {
  height: 100%;
}

.advisor-row {
  height: 100%;
}

.advisor-col {
  height: 100%;
}

.side-card {
  background: var(--cupid-bg-card);
  border: 1px solid var(--cupid-border);
  border-radius: var(--cupid-radius);
  padding: 20px;
  box-shadow: var(--cupid-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-card__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--cupid-text);
}

.side-card__btn {
  margin-top: auto;
  border-radius: var(--cupid-radius-sm) !important;
  height: 40px !important;
}

.side-card__hint {
  margin-top: 4px;
  padding: 12px 14px;
  background: var(--cupid-gradient-soft);
  border-radius: var(--cupid-radius-sm);
  color: var(--cupid-text-secondary);
  font-size: 12px;
  line-height: 1.6;
  border-left: 3px solid rgba(255, 90, 122, 0.3);
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--cupid-bg-card);
  border: 1px solid var(--cupid-border);
  border-radius: var(--cupid-radius);
  box-shadow: var(--cupid-shadow-sm);
  overflow: hidden;
}

.chat-card__head {
  padding: 16px 20px;
  border-bottom: 1px solid var(--cupid-border);
  background: var(--cupid-gradient-soft);
}

.chat-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cupid-text);
}

.chat-card__sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--cupid-text-secondary);
}

.quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--cupid-border);
}

.quick-chip {
  cursor: pointer;
  margin: 0;
  padding: 4px 12px;
  border-radius: 999px;
}

.quick-chip:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px;
  background:
    radial-gradient(circle at 20% 20%, rgba(114, 86, 255, 0.04), transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(255, 142, 83, 0.03), transparent 40%);
}

.msg {
  display: flex;
  margin-bottom: 14px;
  align-items: flex-end;
  gap: 8px;
}

.msg.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  box-shadow: var(--cupid-shadow-sm);
  flex-shrink: 0;
}

.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 16px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
}

.msg.user .bubble {
  background: var(--cupid-gradient);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 12px rgba(255, 90, 122, 0.25);
}

.msg.assistant .bubble {
  background: #fff;
  color: var(--cupid-text);
  border: 1px solid var(--cupid-border);
  border-bottom-left-radius: 4px;
}

.cursor {
  display: inline-block;
  color: var(--cupid-primary);
  animation: blink 1s steps(2) infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  50.01%, 100% { opacity: 0; }
}

.empty {
  text-align: center;
  margin-top: 60px;
}

.empty__icon {
  font-size: 40px;
  opacity: 0.6;
}

.empty__text {
  margin-top: 12px;
  color: var(--cupid-text-muted);
  font-size: 14px;
}

.input-row {
  display: flex;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid var(--cupid-border);
  background: #fff;
  align-items: flex-end;
}

.input-area {
  flex: 1;
  border-radius: var(--cupid-radius-sm) !important;
}

.native-textarea {
  resize: none;
  border: 1px solid var(--ant-color-border, #d9d9d9);
  padding: 8px 12px;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.5;
  background: var(--ant-color-bg-container, #fff);
  color: var(--ant-color-text, rgba(0,0,0,0.88));
  outline: none;
  transition: border-color 0.2s;
}

.native-textarea:focus {
  border-color: var(--ant-color-primary, #69b1ff);
  box-shadow: 0 0 0 2px rgba(105,177,255,0.2);
}

.native-textarea:disabled {
  background: var(--ant-color-bg-container-disabled, #f5f5f5);
  cursor: not-allowed;
}

.send-btn {
  border-radius: var(--cupid-radius-sm) !important;
  min-width: 90px;
  height: 60px !important;
}

.report-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.report-row--actions {
  display: flex;
  gap: 10px;
}

.report-placeholder {
  padding: 20px;
  text-align: center;
  color: var(--cupid-text-muted);
  background: var(--cupid-gradient-soft);
  border-radius: 10px;
  font-size: 13px;
}

.report-pre {
  max-height: 360px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fafafa;
  border: 1px solid var(--cupid-border);
  border-radius: 10px;
  padding: 14px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--cupid-text);
}

.report-history {
  border-top: 1px dashed var(--cupid-border);
  padding-top: 8px;
}

.report-history__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--cupid-text-secondary);
  margin: 8px 0;
}

.report-history__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid var(--cupid-border);
  border-radius: 10px;
  margin-bottom: 8px;
  transition: all 0.2s;
}

.report-history__item:hover {
  border-color: var(--cupid-primary);
}

.report-history__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--cupid-text);
}

.report-history__meta {
  font-size: 12px;
  color: var(--cupid-text-muted);
  margin-top: 2px;
}

.report-history__ops {
  display: flex;
  gap: 6px;
}
</style>
