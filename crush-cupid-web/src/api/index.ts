import http from './http'
import type {
  AdvisorCommand,
  AiProvider,
  AiProviderPayload,
  BuildEvent,
  ChatHistoryVO,
  ChatMedia,
  Crush,
  CrushCreatePayload,
  CrushReport,
  LoginDTO,
  LoginVO,
  MultiChunk,
  RegisterDTO,
  Result,
  SkillCatalog,
  Source,
  UpdateProfileDTO,
  UserVO,
  Version,
  VersionVO,
  MyQuotaVO,
} from '@/types'

async function unwrap<T>(p: Promise<{ data: Result<T> }>): Promise<T> {
  const { data } = await p
  if (data.code !== 0) {
    throw new Error(data.message)
  }
  return data.data
}

export async function listCrushes(): Promise<Crush[]> {
  return unwrap(http.get<Result<Crush[]>>('/crush'))
}

export async function getCrush(id: number): Promise<Crush> {
  return unwrap(http.get<Result<Crush>>(`/crush/${id}`))
}

export async function createCrush(payload: CrushCreatePayload): Promise<Crush> {
  return unwrap(http.post<Result<Crush>>('/crush', payload))
}

export async function updateCrush(id: number, payload: Partial<CrushCreatePayload>): Promise<Crush> {
  return unwrap(http.put<Result<Crush>>(`/crush/${id}`, payload))
}

export async function deleteCrush(id: number): Promise<void> {
  await unwrap(http.delete<Result<void>>(`/crush/${id}`))
}

export async function listAiProviders(): Promise<AiProvider[]> {
  return unwrap(http.get<Result<AiProvider[]>>('/ai-provider'))
}

export async function getAiProvider(id: number): Promise<AiProvider> {
  return unwrap(http.get<Result<AiProvider>>(`/ai-provider/${id}`))
}

export async function createAiProvider(payload: AiProviderPayload): Promise<AiProvider> {
  return unwrap(http.post<Result<AiProvider>>('/ai-provider', payload))
}

export async function updateAiProvider(id: number, payload: AiProviderPayload): Promise<AiProvider> {
  return unwrap(http.put<Result<AiProvider>>(`/ai-provider/${id}`, payload))
}

export async function deleteAiProvider(id: number): Promise<void> {
  await unwrap(http.delete<Result<void>>(`/ai-provider/${id}`))
}

export async function getSkillCatalog(): Promise<SkillCatalog> {
  return unwrap(http.get<Result<SkillCatalog>>('/skill/catalog'))
}

export async function getSkillPrompt(name: string): Promise<string> {
  return unwrap(http.get<Result<string>>(`/skill/prompt/${name}`))
}

/** 军师模式子命令列表 */
export async function listAdvisorCommands(): Promise<AdvisorCommand[]> {
  return unwrap(http.get<Result<AdvisorCommand[]>>('/skill/advisor'))
}

/** 调用军师子命令，返回 LLM 军师回复文本 */
export async function invokeAdvisor(
  body: { name: string; question?: string; crushSlug?: string },
): Promise<string> {
  return unwrap(http.post<Result<string>>('/skill/advisor/invoke', body))
}

/** 生成关系报告并落库，返回报告详情（含 markdown）；需 crushSlug */
export async function generateReport(crushSlug: string): Promise<CrushReport> {
  return unwrap(http.post<Result<CrushReport>>('/skill/advisor/report', { crushSlug }))
}

/** 某暗恋对象的关系报告历史（新→旧） */
export async function listReports(crushSlug: string): Promise<CrushReport[]> {
  return unwrap(http.get<Result<CrushReport[]>>('/skill/report/list', { params: { crushSlug } }))
}

/** 报告详情（含 markdown 全文） */
export async function getReportDetail(id: number): Promise<CrushReport> {
  return unwrap(http.get<Result<CrushReport>>(`/skill/report/${id}`))
}

/** 删除一条报告历史 */
export async function deleteReport(id: number): Promise<void> {
  await unwrap(http.delete<Result<void>>(`/skill/report/${id}`))
}

/** 下载关系报告 .docx */
function triggerBlobDownload(resp: Blob, filename: string): void {
  const url = URL.createObjectURL(resp)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

/** 下载刚生成的报告 .docx（现场生成，不落库） */
export async function downloadReportLive(crushSlug: string, md?: string): Promise<void> {
  const resp = await http.get('/skill/advisor/report/download', {
    params: { crushSlug, md: md || undefined },
    responseType: 'blob',
  })
  const ct = (resp.headers['content-type'] as string) || ''
  if (ct.includes('application/json')) {
    const text = await (resp.data as Blob).text()
    throw new Error(text || '下载失败')
  }
  triggerBlobDownload(
    resp.data as Blob,
    `关系报告_${crushSlug}_${new Date().toISOString().slice(0, 10)}.docx`,
  )
}

/** 下载一条已保存的报告 .docx（读库，不重复调用 LLM） */
export async function downloadSavedReport(id: number, crushName?: string): Promise<void> {
  const resp = await http.get(`/skill/report/${id}/download`, { responseType: 'blob' })
  const ct = (resp.headers['content-type'] as string) || ''
  if (ct.includes('application/json')) {
    const text = await (resp.data as Blob).text()
    throw new Error(text || '下载失败')
  }
  triggerBlobDownload(resp.data as Blob, `关系报告_${crushName || id}_${new Date().toISOString().slice(0, 10)}.docx`)
}

/**
 * 流式对话（SSE，POST）。后端每个 chunk 以 {index,type,content,done} JSON 编码在 data 行；
 * 前端按 index 切气泡，支持 crush 一次连发多条短消息 + 表情包气泡。
 * media 可选：图片（IMAGE_BASE64，多模态供应商视觉理解 / 非多模态 OCR 兜底）或文本附件（FILE_BASE64）。
 * advisorMode 可选：true 时用军师人设回应（配合 skillPrompt 注入任务）；false 走普通 crush 对话。
 */
export async function streamChat(
  crushSlug: string,
  message: string,
  onChunk: (chunk: MultiChunk) => void,
  media?: ChatMedia[],
  options?: { advisorMode?: boolean; skillPrompt?: string },
): Promise<void> {
  await sseStream(
    '/api/chat',
    {
      crushSlug,
      message,
      media: media && media.length ? media : undefined,
      advisorMode: options?.advisorMode || undefined,
      skillPrompt: options?.skillPrompt || undefined,
    },
    onChunk,
  )
}

/**
 * 军师对话（SSE，POST）。独立于模拟对话：走 /api/chat/advisor，使用军师人设 + 独立内存记忆，
 * 不写入模拟对话历史。skillPrompt 可选，注入具体任务（如「帮我分析如何约 ta」「写个开场白」）。
 */
export async function advisorStreamChat(
  crushSlug: string,
  message: string,
  onChunk: (chunk: MultiChunk) => void,
  skillPrompt?: string,
): Promise<void> {
  await sseStream(
    '/api/chat/advisor',
    {
      crushSlug,
      message,
      advisorMode: true,
      skillPrompt: skillPrompt || undefined,
    },
    onChunk,
  )
}

/**
 * 主动消息（SSE，POST）。crush 不依赖用户输入而主动发起连发多条消息。
 * contextHint 可选，给 crush 提供场景暗示（如「凌晨三点」「下雨天」）。
 */
export async function proactiveChat(
  crushSlug: string,
  contextHint: string,
  onChunk: (chunk: MultiChunk) => void,
): Promise<void> {
  await sseStream(
    '/api/chat/proactive',
    { crushSlug, contextHint },
    onChunk,
  )
}

/**
 * 语音合成：把 crush 文本回复送 CosyVoice 合成。后端用 Result<String> 返回 base64 mp3，
 * 这里解码成 Blob 供 <audio> 播放。
 * 注：axios baseURL 已是 /api，此处不要再带 /api 前缀，否则拼成 /api/api/...
 */
export async function synthesizeVoice(text: string, voice?: string): Promise<Blob> {
  const base64 = await unwrap<string>(http.post('/chat/voice', { text, voice }))
  const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0))
  return new Blob([bytes], { type: 'audio/mpeg' })
}

/**
 * 加载某 crush 的历史对话（已落库 PG）。前端进入对话页时调用。
 */
export async function getChatHistory(crushSlug: string): Promise<ChatHistoryVO[]> {
  return unwrap<ChatHistoryVO[]>(http.get('/chat/history', { params: { crushSlug } }))
}

/**
 * 主动消息推送监听（SSE，GET）。为当前查看的 crush 建立常驻连接，
 * 后端调度器生成新的主动消息后通过该连接推送（事件名 proactive），
 * 前端收到后应重新拉取该 crush 的历史以渲染新气泡。
 *
 * @returns 关闭函数（切换 crush / 页面卸载时调用）
 */
export function listenProactive(crushSlug: string, onMessage: (text: string) => void): () => void {
  const token = localStorage.getItem('satoken')
  const qs = token ? `?crushSlug=${encodeURIComponent(crushSlug)}&satoken=${encodeURIComponent(token)}` : `?crushSlug=${encodeURIComponent(crushSlug)}`
  const es = new EventSource(`/api/push/listen${qs}`)
  es.addEventListener('proactive', (e) => {
    onMessage((e as MessageEvent).data)
  })
  return () => es.close()
}

/**
 * SSE POST 通用消费器：解析 data 行为 MultiChunk 并回调。
 */
async function sseStream(
  url: string,
  body: Record<string, unknown>,
  onChunk: (chunk: MultiChunk) => void,
): Promise<void> {
  const token = localStorage.getItem('satoken')
  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: token } : {}),
    },
    body: JSON.stringify(body),
  })

  const contentType = resp.headers.get('content-type') || ''
  if (!resp.ok || !contentType.includes('text/event-stream')) {
    const data = (await resp.json().catch(() => null)) as Result<unknown> | null
    throw new Error((data && data.message) || '请求失败')
  }

  const reader = resp.body!.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const consume = (line: string) => {
    if (!line.startsWith('data:')) return
    const payload = line.slice(5).trim()
    if (!payload) return
    try {
      onChunk(JSON.parse(payload) as MultiChunk)
    } catch {
      /* ignore malformed line */
    }
  }

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''
    for (const line of lines) consume(line)
  }
  if (buffer.trim()) consume(buffer)
}

export async function importSource(
  crushId: number,
  payload: { type?: string; fileName?: string; content: string },
): Promise<Source> {
  return unwrap(http.post<Result<Source>>(`/crush/${crushId}/sources`, payload))
}

export async function uploadSource(crushId: number, file: File, type?: string): Promise<Source> {
  const form = new FormData()
  form.append('file', file)
  if (type) form.append('type', type)
  return unwrap(
    http.post<Result<Source>>(`/crush/${crushId}/sources/upload`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  )
}

export async function listSources(crushId: number): Promise<Source[]> {
  return unwrap(http.get<Result<Source[]>>(`/crush/${crushId}/sources`))
}

export async function deleteSource(crushId: number, sourceId: number): Promise<void> {
  await unwrap(http.delete<Result<void>>(`/crush/${crushId}/sources/${sourceId}`))
}

export async function listVersions(crushId: number): Promise<Version[]> {
  return unwrap(http.get<Result<Version[]>>(`/crush/${crushId}/versions`))
}

/**
 * 构建 crush（SSE 进度流）。
 */
export async function buildCrush(crushId: number, onEvent: (ev: BuildEvent) => void): Promise<void> {
  const token = localStorage.getItem('satoken')
  const resp = await fetch(`/api/crush/${crushId}/build`, {
    method: 'POST',
    headers: { ...(token ? { Authorization: token } : {}) },
  })
  const ct = resp.headers.get('content-type') || ''
  if (!resp.ok || !ct.includes('text/event-stream')) {
    const data = (await resp.json().catch(() => null)) as Result<unknown> | null
    throw new Error((data && data.message) || '请求失败')
  }
  const reader = resp.body!.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  const consume = (line: string) => {
    if (!line.startsWith('data:')) return
    const payload = line.slice(5).trim()
    if (!payload) return
    try {
      onEvent(JSON.parse(payload) as BuildEvent)
    } catch {
      /* ignore */
    }
  }
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''
    for (const line of lines) consume(line)
  }
  if (buffer.trim()) consume(buffer)
}

/** 发送邮箱验证码 */
export async function sendEmailCode(email: string, purpose: string = 'LOGIN'): Promise<void> {
  await unwrap(http.post<Result<void>>('/auth/email-code', { email, purpose }))
}

/** 邮箱注册（注册即登录，返回 token） */
export async function register(payload: RegisterDTO): Promise<LoginVO> {
  return unwrap(http.post<Result<LoginVO>>('/auth/register', payload))
}

/** 邮箱登录（返回 token） */
export async function login(payload: LoginDTO): Promise<LoginVO> {
  return unwrap(http.post<Result<LoginVO>>('/auth/login', payload))
}

/** 登出 */
export async function logout(): Promise<void> {
  await unwrap(http.post<Result<void>>('/auth/logout'))
}

/** 当前登录用户 */
export async function me(): Promise<UserVO> {
  return unwrap(http.get<Result<UserVO>>('/auth/me'))
}

/** 修改密码 */
export async function changePassword(payload: UpdateProfileDTO): Promise<void> {
  await unwrap(http.put<Result<void>>('/auth/password', payload))
}

/** 更新资料 */
export async function updateProfile(payload: UpdateProfileDTO): Promise<UserVO> {
  return unwrap(http.put<Result<UserVO>>('/user/profile', payload))
}

/** 我的资料 */
export async function myProfile(): Promise<UserVO> {
  return unwrap(http.get<Result<UserVO>>('/user/me'))
}

/** 我的配额 */
export async function myQuota(): Promise<MyQuotaVO> {
  return unwrap(http.get<Result<MyQuotaVO>>('/user/quota'))
}
