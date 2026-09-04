
<template>
  <PageContainer
    icon="💬"
    title="对话"
    :subtitle="currentSlug ? `正在和 ${currentName} 聊天` : '选择一个暗恋对象开始对话'"
  >
    <div v-if="!isLoggedIn" class="login-overlay" @click.self="closeLoginOverlay">
      <div class="login-overlay-card" @click.stop>
        <div class="login-overlay-header">
          <div class="login-overlay-logo">💘</div>
          <div class="login-overlay-title">Cupid</div>
          <div class="login-overlay-sub">暗恋模拟器 · 请先登录</div>
        </div>
        <a-tabs v-model:activeKey="loginTab" class="login-overlay-tabs">
          <a-tab-pane key="login" tab="登录">
            <a-form :model="loginForm" layout="vertical" @finish="handleLogin">
              <a-form-item label="邮箱" name="email">
                <a-input v-model:value="loginForm.email" placeholder="请输入邮箱">
                  <template #suffix>
                    <a-button size="small" type="primary" @click="sendOverlayCode" :disabled="!loginForm.email || overlaySendingCode || overlayCountdown > 0" style="margin-left: 4px; font-size: 11px;">
                      {{ overlayCountdown > 0 ? `${overlayCountdown}s` : (overlaySendingCode ? '发送中' : '验证码') }}
                    </a-button>
                  </template>
                </a-input>
              </a-form-item>
              <a-form-item v-if="overlayShowCode" label="验证码" name="code">
                <a-input v-model:value="loginForm.code" placeholder="请输入验证码" />
              </a-form-item>
              <a-form-item label="密码" name="password">
                <a-input-password v-model:value="loginForm.password" placeholder="请输入密码" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" html-type="submit" block :loading="loginLoading">
                  登录
                </a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="register" tab="注册">
            <a-form :model="registerForm" layout="vertical" @finish="handleRegister">
              <a-form-item label="用户名" name="username">
                <a-input v-model:value="registerForm.username" placeholder="请输入用户名" />
              </a-form-item>
              <a-form-item label="邮箱" name="email">
                <a-input v-model:value="registerForm.email" placeholder="请输入邮箱">
                  <template #suffix>
                    <a-button size="small" type="primary" @click="sendOverlayRegCode" :disabled="!registerForm.email || overlaySendingRegCode || overlayRegCountdown > 0" style="margin-left: 4px; font-size: 11px;">
                      {{ overlayRegCountdown > 0 ? `${overlayRegCountdown}s` : (overlaySendingRegCode ? '发送中' : '验证码') }}
                    </a-button>
                  </template>
                </a-input>
              </a-form-item>
              <a-form-item v-if="overlayShowRegCode" label="验证码" name="code">
                <a-input v-model:value="registerForm.code" placeholder="请输入验证码" />
              </a-form-item>
              <a-form-item label="密码" name="password">
                <a-input-password v-model:value="registerForm.password" placeholder="请输入密码" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" html-type="submit" block :loading="loginLoading">
                  注册
                </a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
    <div class="chat-page">
      <a-row :gutter="20" class="chat-row">
        <!-- 左侧：crush 选择 -->
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
              :disabled="!currentCrush"
              @click="importOpen = true"
            >
              <span>📥</span>&nbsp;补充原材料
            </a-button>
            <a-button
              size="large"
              block
              class="side-card__btn side-card__btn--nudge"
              :loading="streaming"
              :disabled="!currentCrush || streaming"
              @click="nudge"
            >
              <span>💌</span>&nbsp;等 ta 主动找我
            </a-button>
            <div class="side-card__hint">
              <span>💡</span> 还没有暗恋对象？去「暗恋对象」页新建一个。
            </div>
          </div>
        </a-col>

        <!-- 右侧：聊天区 -->
        <a-col :span="18" class="chat-col">
          <div class="chat-card">
            <div class="chat-card__head">
              <div class="chat-card__title">
                {{ currentSlug ? `和 ${currentName} 聊天` : '请先选择 crush' }}
              </div>
              <div class="chat-card__sub" v-if="currentCrush">
                {{ currentCrush.mbti || '—' }} · {{ currentCrush.zodiac || '—' }}
              </div>
            </div>

            <div ref="msgBox" class="messages">
              <div v-if="messages.length === 0 && !streaming" class="empty">
                <div class="empty__icon">💌</div>
                <div class="empty__text">开始你们的对话吧～</div>
              </div>
              <div
                v-for="(m, i) in messages"
                :key="i"
                :class="['msg', m.role]"
              >
                <div class="avatar">{{ m.role === 'user' ? '🧑' : '💗' }}</div>
                <!-- 表情包气泡：纯图片，无语音按钮；支持本地 /api/stickers 和远端 raw URL -->
                <div v-if="m.kind === 'sticker'" class="bubble bubble--sticker">
                  <img
                    class="sticker-img"
                    :src="m.content"
                    alt="表情包"
                    @error="(e) => (e.target as HTMLImageElement).style.opacity = '0.3'"
                  />
                </div>
                <!-- 用户发送的图片气泡：base64/URL 直接渲染 -->
                <div v-else-if="m.kind === 'image'" class="bubble bubble--image">
                  <img class="chat-img" :src="m.content" alt="图片" />
                </div>
                <div v-else class="bubble">
                  <span>{{ m.content }}</span>
                  <span v-if="streaming && i === streamingBubbleIdx" class="cursor">▋</span>
                  <div
                    v-if="m.role === 'assistant' && m.content && !streaming"
                    class="bubble__voice"
                  >
                    <button
                      class="voice-btn"
                      :disabled="m.synthesizing"
                      :title="m.synthesizing ? '合成中…' : (m.audioUrl ? '播放' : '合成并播放')"
                      @click="playVoice(m)"
                    >
                      <span v-if="m.synthesizing">⏳</span>
                      <span v-else>🎤</span>
                    </button>
                    <audio
                      v-if="m.audioUrl"
                      class="bubble-audio"
                      :src="m.audioUrl"
                      controls
                      preload="none"
                    />
                  </div>
                </div>
              </div>
              <div v-if="streaming && streamingBubbleIdx < 0" class="msg assistant">
                <div class="avatar">💗</div>
                <div class="bubble">
                  <span class="typing">正在输入…</span>
                  <span class="cursor">▋</span>
                </div>
              </div>
            </div>

            <div class="input-row">
              <!-- 待发送附件预览 -->
              <div v-if="pendingMedia.length" class="attach-chips">
                <span v-for="(m, i) in pendingMedia" :key="i" class="chip">
                  {{ describeMedia(m) }}
                  <a class="chip__del" @click="pendingMedia.splice(i, 1)">×</a>
                </span>
              </div>
              <button
                class="attach-btn"
                title="发图片 / 附件"
                :disabled="streaming"
                @click="fileInput?.click()"
              >
                📎
              </button>
              <input
                ref="fileInput"
                type="file"
                multiple
                hidden
                accept="image/*,.txt,.md,.csv,.json,.log,.pdf,.docx"
                @change="onFilePick"
              />
              <textarea
                ref="inputRef"
                v-model="input"
                :rows="2"
                placeholder="说点什么…（Enter 发送，Shift+Enter 换行）"
                :disabled="streaming"
                class="input-area native-textarea"
                @keydown="onKeydown"
              ></textarea>
              <a-button
                type="primary"
                size="large"
                class="send-btn"
                :loading="streaming"
                :disabled="!currentSlug || streaming || (!input.trim() && !pendingMedia.length)"
                @click="send"
              >
                发送
              </a-button>
            </div>
          </div>
        </a-col>
      </a-row>

      <SourceImportModal v-model:open="importOpen" :crush-id="currentCrush?.id ?? 0" />
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
/**
 * 对话页面：加载 crush 列表、维护消息历史、流式发送。
 * 支持一次连发多条短消息（按 chunk.index 切气泡）、crush 主动发起对话、
 * 以及把 crush 的文本回复一键转 CosyVoice 语音播放。
 */
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { login, register, sendEmailCode } from '@/api'
import {
  getChatHistory,
  listCrushes,
  listenProactive,
  proactiveChat,
  streamChat,
  synthesizeVoice,
} from '@/api'
import type { ChatMedia, Crush, MultiChunk } from '@/types'
import SourceImportModal from '@/components/SourceImportModal.vue'
import PageContainer from '@/components/PageContainer.vue'

/** 聊天消息结构 */
interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  /** 气泡类型：text 普通文本（默认）；sticker 表情包；image 用户发的图片（content 为可渲染 data URL / URL） */
  kind?: 'text' | 'sticker' | 'image'
  /** 语音 URL（合成后生成，便于气泡内 <audio> 播放）；无则未合成 */
  audioUrl?: string
  /** 是否正在合成语音 */
  synthesizing?: boolean
  /** 合成中的 promise，用于并发去重与等待 */
  synthPromise?: Promise<void>
}

const crushes = ref<Crush[]>([])
const loading = ref(false)
const currentSlug = ref<string>()
const messages = ref<ChatMessage[]>([])
const input = ref('')
/** 输入框原生 textarea 引用 */
const inputRef = ref<HTMLTextAreaElement>()
const streaming = ref(false)
const msgBox = ref<HTMLElement>()
/** 待发送的图片/附件（随消息一起发送，发送后清空） */
const pendingMedia = ref<ChatMedia[]>([])
const fileInput = ref<HTMLInputElement>()
/** 当前 crush 的主动消息监听关闭函数（切换 crush 时先关旧的） */
let closePush: (() => void) | null = null

/** 当前正在流式追加的气泡在 messages 中的索引（用于显示光标）；-1 表示无 */
const streamingBubbleIdx = ref(-1)

/** crush 下拉选项 */
const crushOptions = computed(() =>
  crushes.value.map((c) => ({ label: `${c.name} (${c.slug})`, value: c.slug })),
)
/** 当前 crush 名称 */
const currentName = computed(
  () => crushes.value.find((c) => c.slug === currentSlug.value)?.name ?? '',
)
/** 当前 crush 对象 */
const currentCrush = computed(() =>
  crushes.value.find((c) => c.slug === currentSlug.value),
)
const importOpen = ref(false)

/** 登录覆盖层 */
const isLoggedIn = ref(!!localStorage.getItem('satoken'))
const loginTab = ref('login')
const loginLoading = ref(false)
const overlaySendingCode = ref(false)
const overlayCountdown = ref(0)
const overlayShowCode = ref(false)
const overlaySendingRegCode = ref(false)
const overlayRegCountdown = ref(0)
const overlayShowRegCode = ref(false)
let overlayCountdownTimer: ReturnType<typeof setInterval> | null = null
let overlayRegCountdownTimer: ReturnType<typeof setInterval> | null = null
const loginForm = reactive({ email: '', password: '', code: '' })
const registerForm = reactive({ username: '', email: '', password: '', code: '' })

async function sendOverlayCode() {
  if (!loginForm.email) {
    message.warning('请先输入邮箱')
    return
  }
  overlaySendingCode.value = true
  try {
    await sendEmailCode(loginForm.email, 'LOGIN')
    message.success('验证码已发送')
    overlayShowCode.value = true
    overlayCountdown.value = 60
    overlayCountdownTimer = setInterval(() => {
      overlayCountdown.value--
      if (overlayCountdown.value <= 0) {
        clearInterval(overlayCountdownTimer)
        overlayCountdownTimer = null
        overlaySendingCode.value = false
      }
    }, 1000)
  } catch (e: any) {
    message.error(e?.message || '发送失败')
  } finally {
    overlaySendingCode.value = false
  }
}

async function sendOverlayRegCode() {
  if (!registerForm.email) {
    message.warning('请先输入邮箱')
    return
  }
  overlaySendingRegCode.value = true
  try {
    await sendEmailCode(registerForm.email, 'REGISTER')
    message.success('验证码已发送')
    overlayShowRegCode.value = true
    overlayRegCountdown.value = 60
    overlayRegCountdownTimer = setInterval(() => {
      overlayRegCountdown.value--
      if (overlayRegCountdown.value <= 0) {
        clearInterval(overlayRegCountdownTimer)
        overlayRegCountdownTimer = null
        overlaySendingRegCode.value = false
      }
    }, 1000)
  } catch (e: any) {
    message.error(e?.message || '发送失败')
  } finally {
    overlaySendingRegCode.value = false
  }
}

function closeLoginOverlay() {
  if (isLoggedIn.value) return
}

async function handleLogin() {
  if (!loginForm.email || !loginForm.password) {
    message.warning('请填写邮箱和密码')
    return
  }
  loginLoading.value = true
  try {
    const vo = await login({ email: loginForm.email, password: loginForm.password })
    localStorage.setItem('satoken', vo.tokenValue)
    isLoggedIn.value = true
    message.success('登录成功')
    loginForm.email = ''
    loginForm.password = ''
  } catch (e: any) {
    message.error(e?.message || '登录失败')
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.email || !registerForm.password) {
    message.warning('请填写邮箱和密码')
    return
  }
  loginLoading.value = true
  try {
    const vo = await register({
      email: registerForm.email,
      password: registerForm.password,
      username: registerForm.username,
    })
    localStorage.setItem('satoken', vo.tokenValue)
    isLoggedIn.value = true
    message.success('注册成功')
    registerForm.email = ''
    registerForm.password = ''
    registerForm.username = ''
  } catch (e: any) {
    message.error(e?.message || '注册失败')
  } finally {
    loginLoading.value = false
  }
}

/** 加载 crush 列表，默认选中第一个 */
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

/** 滚动到消息底部 */
async function scrollToBottom() {
  await nextTick()
  if (msgBox.value) {
    msgBox.value.scrollTop = msgBox.value.scrollHeight
  }
}

/** 历史消息分隔符，与后端 MessageSeparator.SEPARATOR 保持一致 */
const MSG_SEP = '|||'

/**
 * 清洗用户消息文本里的图片占位/内联残留，避免字面量泄漏到气泡：
 * - [图片] 占位（后端正常清，这里兜底）
 * - 历史内联 [[图片:URL]] 标记（后端已抽成 mediaUrl，这里是兜底防残留）
 */
function cleanMediaMarkers(s?: string): string {
  if (!s) return ''
  return s
    .replace(/\[\[图片:[^\]]*\]\]/g, '')
    .replace(/\[图片\]/g, '')
    .trim()
}

/** 解析历史文本中的 [[sticker:URL]] 标记，返回解析结果：
 * - { stickers: string[], text: string }
 *   stickers: 提取出的表情包 URL 列表
 *   text: 清理标记后的纯文本（用于文本气泡）
 * 同时清洗存量脏数据：[表情包]、(此处发表了一个表情包) 占位文本
 */
function parseStickerMark(s: string): { stickers: string[]; text: string } {
  const stickers: string[] = []
  let text = s.replace(/\[\[sticker:([^\]]*)\]\]/g, (_m, url: string) => {
    if (url && url.startsWith('http')) {
      stickers.push(url)
    }
    return ''
  })
  // 清洗存量占位文本
  text = text.replace(/\[表情包\]/g, '').replace(/\(此处发表了一个表情包\)/g, '')
  return { stickers, text: text.trim() }
}

/** 判断文本（trim 后）是否是独立的表情包图片地址：jsdelivr CDN / ChineseBQB raw 链接 / 本地 sticker 路径 / 图片扩展名 URL */
function isStickerUrl(s: string): boolean {
  const t = (s ?? '').trim()
  if (!t) return false
  if (t.startsWith('/api/stickers/')) return true
  // jsdelivr CDN（国内可直连）或 GitHub raw 链接
  if (/^https?:\/\/\S*(cdn\.jsdelivr\.net\/gh\/zhaoolee\/ChineseBQB|raw\.githubusercontent\.com\/zhaoolee\/ChineseBQB|github\.com\/zhaoolee\/ChineseBQB)\S*/i.test(t)) return true
  return /^https?:\/\/\S+\.(?:png|jpe?g|gif|webp)(?:\?\S*)?$/i.test(t)
}

/**
 * 兜底归一化：把 [fromIdx, end) 中「整条内容就是表情包 URL」的 assistant 文本气泡
 * 转成 sticker 气泡（按图片渲染、语音合成自动跳过）。
 * 防御 LLM 偶发裸输出 URL（未包裹 [[sticker:...]] 标记）导致 URL 被当文字展示 + 被语音读出。
 */
function normalizeStickerBubbles(fromIdx: number) {
  for (let i = Math.max(0, fromIdx); i < messages.value.length; i++) {
    const m = messages.value[i]
    if (m.role === 'assistant' && m.kind === 'text' && isStickerUrl(m.content)) {
      m.kind = 'sticker'
    }
  }
}

/** 加载某 crush 的历史对话（已落库 PG） */
async function loadHistory(slug: string) {
  if (!slug) {
    messages.value = []
    return
  }
  try {
    const rows = await getChatHistory(slug)
    const list: ChatMessage[] = []
    for (const r of rows) {
      if (r.role !== 'user' && r.role !== 'assistant') continue
      if (r.role === 'assistant' && r.content.includes(MSG_SEP)) {
        // assistant 消息按分隔符切分，与流式显示保持一致
        r.content.split(MSG_SEP).forEach((segment) => {
          const { stickers, text } = parseStickerMark(segment)
          // 先推表情包气泡（如果有）
          stickers.forEach((url) => list.push({ role: 'assistant', kind: 'sticker', content: url }))
          // 再推文本气泡
          if (text) {
            list.push({ role: 'assistant', content: text })
          }
        })
      } else if (r.role === 'assistant') {
        const { stickers, text } = parseStickerMark(r.content ?? '')
        stickers.forEach((url) => list.push({ role: 'assistant', kind: 'sticker', content: url }))
        if (text) {
          list.push({ role: 'assistant', content: text })
        }
      } else if (r.role === 'user') {
        // user 消息：如果有 mediaUrl（来自 chat_media 表），先推图片气泡
        if (r.mediaUrl) {
          list.push({ role: 'user', kind: 'image', content: r.mediaUrl })
        }
        // 再推文本气泡（[图片] 标记已在后端清除，这里兜底清洗历史内联 [[图片:URL]] 占位残留）
        const text = cleanMediaMarkers(r.content)
        if (text) {
          list.push({ role: 'user', content: text })
        }
      }
    }
    messages.value = list
    await scrollToBottom()
  } catch {
    messages.value = []
  }
}

// 切换 crush 时停止当前连播，并重新加载该 crush 的历史 + 建立主动推送监听
watch(currentSlug, (slug) => {
  stopPlayback()
  if (closePush) {
    closePush()
    closePush = null
  }
  if (slug) {
    loadHistory(slug)
    closePush = listenProactive(slug, onProactivePush)
  }
})

/**
 * 收到后端主动消息推送：重新拉取历史渲染新气泡，并给一个轻微提示音。
 * 若用户当前正看其它 crush 的历史，不强制切走，仅做轻提示。
 */
function onProactivePush(text: string) {
  if (currentSlug.value) {
    void loadHistory(currentSlug.value)
  }
  playNudgeTone()
}

/**
 * 本轮 assistant 多条气泡累积器：按 chunk.index 把 content 追加到对应气泡，
 * index 跳变即开新气泡；sticker chunk 是一次性完整图片 URL，直接建独立表情包气泡。
 * 以 index+type 复合键映射气泡：即便后端对同一 index 混发 text/sticker（历史/边界场景），
 * 也不会把文本气泡覆盖成图片，二者各自独立成气泡。
 */
class MultiBubbleAccumulator {
  /** chunk.index+type -> messages 数组位置 */
  private map = new Map<string, number>()

  push(chunk: MultiChunk) {
    if (chunk.done) return
    const key = `${chunk.index}:${chunk.type === 'sticker' ? 'sticker' : 'text'}`
    let pos = this.map.get(key)
    if (pos === undefined) {
      const isSticker = chunk.type === 'sticker'
      messages.value.push({ role: 'assistant', content: '', kind: isSticker ? 'sticker' : 'text' })
      pos = messages.value.length - 1
      this.map.set(key, pos)
    }
    if (chunk.content) {
      if (chunk.type === 'sticker') {
        messages.value[pos].content = chunk.content
      } else {
        messages.value[pos].content += chunk.content
      }
    }
    streamingBubbleIdx.value = chunk.type === 'sticker' ? -1 : pos
  }

  reset() {
    this.map.clear()
    streamingBubbleIdx.value = -1
  }
}

/** 选择图片/附件：转 base64 放入待发送列表 */
function onFilePick(e: Event) {
  const el = e.target as HTMLInputElement
  if (!el.files) return
  for (const f of Array.from(el.files)) {
    const isImg = f.type.startsWith('image/')
    if (isImg && f.size > 4 * 1024 * 1024) {
      message.warning(`图片 ${f.name} 超过 4MB，请压缩后再发`)
      continue
    }
    if (!isImg && f.size > 8 * 1024 * 1024) {
      message.warning(`附件 ${f.name} 超过 8MB，请精简后再发`)
      continue
    }
    const reader = new FileReader()
    reader.onload = () => {
      const dataUrl = reader.result as string
      const base64 = dataUrl.slice(dataUrl.indexOf(',') + 1)
      if (isImg) {
        pendingMedia.value.push({ type: 'IMAGE_BASE64', mimeType: f.type, data: base64 })
      } else {
        pendingMedia.value.push({ type: 'FILE_BASE64', data: base64, fileName: f.name })
      }
    }
    reader.readAsDataURL(f)
  }
  el.value = ''
}

/** 附件在待发送预览/用户气泡里的展示文案 */
function describeMedia(m: ChatMedia): string {
  return m.type === 'IMAGE_BASE64' ? '[图片]' : `[附件] ${m.fileName ?? ''}`
}

/**
 * 原生 keydown 拦截 Enter 发送。
 * 用原生 textarea + @keydown 而非 a-textarea 的 @pressEnter：pressEnter 是 ant 派发的
 * 自定义事件，触发时机晚于 ant 内部 keydown 处理，preventDefault 可能赶不上——导致浏览器
 * 先往 DOM 插入换行、ant change 事件再把「原话+换行」回写 v-model，发送后输入框残留。
 * 原生 keydown 在浏览器派发时立即触发，preventDefault 在任何组件处理之前生效，彻底杜绝。
 * Shift/Alt/Ctrl/Cmd+Enter 保留换行；输入法组合中的 Enter（确认候选词）不触发发送。
 */
function onKeydown(e: KeyboardEvent) {
  if (e.isComposing) return
  if (e.key !== 'Enter') return
  if (e.shiftKey || e.altKey || e.ctrlKey || e.metaKey) return
  e.preventDefault()
  void send()
}

/** 发送消息并接收流式回复（可带图片/附件） */
async function send() {
  const text = input.value.trim()
  const media = pendingMedia.value.slice()
  if ((!text && media.length === 0) || !currentSlug.value || streaming.value) return

  // 用户气泡：图片（URL/base64）作为独立 image 气泡渲染真实图片；文本/附件作为文字气泡
  const imgs = media.filter((m) => m.type === 'IMAGE_BASE64' || m.type === 'IMAGE_URL')
  const others = media.filter((m) => m.type !== 'IMAGE_BASE64' && m.type !== 'IMAGE_URL')
  for (const img of imgs) {
    const src =
      img.type === 'IMAGE_BASE64'
        ? `data:${img.mimeType || 'image/png'};base64,${img.data}`
        : img.data
    messages.value.push({ role: 'user', kind: 'image', content: src })
  }
  if (text) {
    messages.value.push({ role: 'user', content: text })
  }
  // 附件（非图片）单独成气泡：文字 + 附件同时发时，附件也要有可见气泡，
  // 否则只看到文字、以为附件没发出去
  if (others.length) {
    messages.value.push({ role: 'user', content: others.map(describeMedia).join(' ') })
  }
  const firstAssistantIdx = messages.value.length
  input.value = ''
  pendingMedia.value = []
  // 原生 textarea：v-model 直接绑定，无第三方组件内部状态干扰。
  // nextTick 仍兜底强制同步 DOM（防极端事件时序下的边缘场景）
  void nextTick(() => {
    if (inputRef.value && !input.value && inputRef.value.value) {
      inputRef.value.value = ''
    }
  })
  streaming.value = true
  const acc = new MultiBubbleAccumulator()
  await scrollToBottom()

  try {
    await streamChat(
      currentSlug.value,
      text,
      (chunk) => {
        acc.push(chunk)
        scrollToBottom()
      },
      media,
    )
  } catch (e) {
    const msg = e instanceof Error ? e.message : '发送失败'
    messages.value.push({ role: 'assistant', content: `[错误] ${msg}` })
  } finally {
    acc.reset()
    streaming.value = false
    await scrollToBottom()
    normalizeStickerBubbles(firstAssistantIdx)
  }
}

/** 让 crush 主动找你（一次连发多条） */
async function nudge() {
  if (!currentSlug.value || streaming.value) return
  streaming.value = true
  const firstAssistantIdx = messages.value.length
  const acc = new MultiBubbleAccumulator()
  let receivedAny = false
  await scrollToBottom()

  try {
    await proactiveChat(currentSlug.value, '', (chunk) => {
      receivedAny = true
      acc.push(chunk)
      scrollToBottom()
    })
    // 后端返回了空 SSE 流（Flux 异常或 LLM 无输出），给用户可见反馈
    if (!receivedAny) {
      messages.value.push({ role: 'assistant', content: '…（ta 暂时没有回应，看下后端日志？）' })
    }
  } catch (e) {
    const msg = e instanceof Error ? e.message : '主动消息失败'
    messages.value.push({ role: 'assistant', content: `[错误] ${msg}` })
  } finally {
    acc.reset()
    streaming.value = false
    await scrollToBottom()
    normalizeStickerBubbles(firstAssistantIdx)
  }
}

/** 当前手动播放的音频（同一时间只播一条；切换 crush / 组件卸载时停止） */
let currentAudio: HTMLAudioElement | null = null

/** 停止当前手动播放 */
function stopPlayback() {
  if (currentAudio) {
    currentAudio.pause()
    currentAudio.src = ''
    currentAudio = null
  }
}

/** 合成单条消息语音（幂等：已合成/合成中直接返回同一 promise），不播放 */
function synthesizeBubble(msg: ChatMessage, voice?: string): Promise<void> {
  if (msg.audioUrl) return Promise.resolve()
  if (msg.synthPromise) return msg.synthPromise
  msg.synthesizing = true
  msg.synthPromise = (async () => {
    try {
      const blob = await synthesizeVoice(msg.content, voice)
      msg.audioUrl = URL.createObjectURL(blob)
    } catch {
      msg.audioUrl = undefined
    } finally {
      msg.synthesizing = false
    }
  })()
  return msg.synthPromise
}

/**
 * 点击 🎤：仅合成并播放当前这一条（全手动）。
 * 不做后台预合成、不做连播——每条消息都由用户点击逐条触发；已合成过的秒播。
 */
async function playVoice(msg: ChatMessage) {
  if (msg.role !== 'assistant' || msg.kind === 'sticker' || !msg.content) return
  stopPlayback()
  await synthesizeBubble(msg, currentCrush.value?.voiceId)
  if (!msg.audioUrl) return
  currentAudio = new Audio(msg.audioUrl)
  void currentAudio.play().catch(() => {})
}

/** 收到主动消息时的轻微提示音（Web Audio，无需音频文件） */
function playNudgeTone() {
  try {
    const Ctx = window.AudioContext || (window as any).webkitAudioContext
    const ctx = new Ctx()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'sine'
    osc.frequency.value = 880
    gain.gain.value = 0.04
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start()
    osc.frequency.exponentialRampToValueAtTime(1320, ctx.currentTime + 0.15)
    gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.35)
    osc.stop(ctx.currentTime + 0.35)
    osc.onended = () => ctx.close()
  } catch {
    /* 忽略音频不可用 */
  }
}

onMounted(() => {
  loadCrushes()
  window.addEventListener('storage', () => {
    isLoggedIn.value = !!localStorage.getItem('satoken')
  })
})
onUnmounted(() => {
  stopPlayback()
  if (closePush) {
    closePush()
    closePush = null
  }
  if (overlayCountdownTimer) { clearInterval(overlayCountdownTimer); overlayCountdownTimer = null }
  if (overlayRegCountdownTimer) { clearInterval(overlayRegCountdownTimer); overlayRegCountdownTimer = null }
})
</script>

<style scoped>
.chat-page {
  height: 100%;
}

.chat-row {
  height: 100%;
}

/* ant-col 默认无高度，需显式 100% 才能让 .chat-card 的 height:100% 生效，否则 .messages 不滚动 */
.chat-col {
  height: 100%;
}

/* 左侧选择卡片 */
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
  margin-bottom: 12px;
}

.side-card__btn {
  margin-top: auto;
  border-radius: var(--cupid-radius-sm) !important;
  height: 40px !important;
}

.side-card__btn--nudge {
  border: 1px dashed var(--cupid-primary) !important;
  color: var(--cupid-primary) !important;
  background: var(--cupid-gradient-soft) !important;
}

.side-card__btn--nudge:hover {
  background: #fff !important;
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

/* 右侧聊天卡片 */
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
  position: relative;
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

/* 军师模式开关 */
.chat-card__mode {
  position: absolute;
  top: 50%;
  right: 20px;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--cupid-bg-card);
  border: 1px solid var(--cupid-border);
  transition: all 0.25s ease;
}

.chat-card__mode.on {
  border-color: var(--cupid-primary);
  background: var(--cupid-gradient-soft);
}

.chat-card__mode-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--cupid-text-secondary);
}

.chat-card__mode.on .chat-card__mode-label {
  color: var(--cupid-primary);
}

/* 消息列表 */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 90, 122, 0.03), transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(255, 142, 83, 0.03), transparent 40%);
}

/* 消息条目 */
.msg {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-end;
  gap: 10px;
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
  padding: 10px 16px;
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

  .bubble__voice {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
    border-top: 1px dashed var(--cupid-border);
    padding-top: 8px;
  }

  .voice-btn {
    border: none;
    background: transparent;
    cursor: pointer;
    font-size: 16px;
    padding: 2px 6px;
    border-radius: 50%;
    transition: background 0.15s;
  }

  .voice-btn:hover:not(:disabled) {
    background: rgba(255, 105, 180, 0.12);
  }

  .voice-btn:disabled {
    cursor: progress;
    opacity: 0.6;
  }

  .bubble-audio {
    height: 32px;
    max-width: 240px;
  }

/* 表情包气泡 */
.bubble--sticker {
  padding: 4px !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.sticker-img {
  display: block;
  max-width: 140px;
  max-height: 140px;
  border-radius: 10px;
}

/* 用户发送的图片气泡 */
.bubble--image {
  padding: 4px !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.chat-img {
  display: block;
  max-width: 260px;
  max-height: 260px;
  border-radius: 12px;
  border: 1px solid var(--cupid-border);
  cursor: zoom-in;
}

/* 打字光标动画 */
.cursor {
  display: inline-block;
  color: var(--cupid-primary);
  animation: blink 1s steps(2) infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  50.01%, 100% { opacity: 0; }
}

/* 空状态 */
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

/* 输入区 */
.input-row {
  display: flex;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid var(--cupid-border);
  background: #fff;
  align-items: flex-end;
  flex-wrap: wrap;
}

/* 📎 附件按钮 */
.attach-btn {
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
  padding: 0 4px 14px;
  line-height: 60px;
  transition: transform 0.15s;
}

.attach-btn:hover:not(:disabled) {
  transform: scale(1.15);
}

.attach-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

/* 待发送附件预览条 */
.attach-chips {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  background: var(--cupid-gradient-soft);
  border: 1px solid var(--cupid-border);
  border-radius: 999px;
  font-size: 12px;
  color: var(--cupid-text);
}

.chip__del {
  cursor: pointer;
  color: var(--cupid-text-secondary);
  font-weight: 600;
}

.chip__del:hover {
  color: var(--cupid-primary);
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
  transition: border-color var(--cupid-transition);
  border-radius: var(--cupid-radius-sm);
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
</style>
