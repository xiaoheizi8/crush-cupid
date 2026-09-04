export interface Crush {
  id?: number
  name: string
  slug: string
  mbti?: string
  zodiac?: string
  occupation?: string
  gender?: string
  knowDuration?: string
  relationshipStatus?: string
  impression?: string
  personaLayer0?: string
  personaLayer1?: string
  personaLayer2?: string
  personaLayer3?: string
  personaLayer4?: string
  memoryOverview?: string
  memoryTimeline?: string
  memorySweet?: string
  memoryInteraction?: string
  currentStage?: number
  status?: string
  totalMessages?: number
  lastChatDate?: string
  voiceId?: string
  version?: number
  createdAt?: string
  updatedAt?: string
}

export interface CrushCreatePayload {
  name: string
  slug: string
  mbti?: string
  zodiac?: string
  occupation?: string
  gender?: string
  knowDuration?: string
  relationshipStatus?: string
  impression?: string
  voiceId?: string
}

export interface SkillMeta {
  name: string
  description: string
  version: string
  argumentHint?: string
  userInvocable: boolean
}

export interface SkillCatalog {
  skill: SkillMeta
  prompts: string[]
}

export interface AdvisorCommand {
  name: string
  trigger: string
  title: string
  description: string
  promptName: string
  requiresCrush: boolean
}

export interface CrushReport {
  id: number
  crushId: number
  crushName?: string
  title?: string
  source?: string
  reportDate?: string
  markdown?: string
  createdAt?: string
}

export interface AiProvider {
  id: number
  name: string
  providerKey: string
  baseUrl: string
  apiKey?: string
  model: string
  temperature?: number
  topP?: number
  maxTokens?: number
  /** 能力列表：vision=视觉看图, audio=音频听语音（文本是所有 LLM 基本能力） */
  capabilities?: string[]
  isDefault?: boolean
}

export interface AiProviderPayload {
  name?: string
  providerKey?: string
  baseUrl?: string
  apiKey?: string
  model?: string
  temperature?: number
  topP?: number
  maxTokens?: number
  capabilities?: string[]
  isDefault?: boolean
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface Source {
  id: number
  crushId: number
  type: string
  fileName?: string
  content?: string
  messageCount?: number
  createdAt?: string
}

export interface Version {
  id: number
  crushId: number
  version: number
  reason?: string
  snapshot?: string
  createdAt?: string
}

export interface BuildResult {
  crushId: number
  version: number
  status: string
  memorySummary?: string
  personaSummary?: string
}

export interface BuildEvent {
  type: 'progress' | 'done' | 'error'
  message?: string
  result?: BuildResult
}

/**
 * 多条消息流式 chunk：后端按 index 切气泡，跳变即新气泡。
 * type=text 时 content 为增量文本；type=sticker 时 content 为表情包图片 URL（一次性下发）。
 */
export interface MultiChunk {
  index: number
  type?: 'text' | 'sticker'
  content: string
  done: boolean
}

/**
 * 聊天多模态输入片段：图片（URL/base64）、音频、文本附件（base64）。
 */
export interface ChatMedia {
  type: 'IMAGE_URL' | 'IMAGE_BASE64' | 'AUDIO_URL' | 'AUDIO_BASE64' | 'FILE_BASE64'
  mimeType?: string
  data: string
  fileName?: string
}

/**
 * 对话历史条目（后端 GET /api/chat/history 返回）。
 */
export interface ChatHistoryVO {
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  createdAt: string
  /** 关联的图片 URL（来自 chat_media 表）；无图片为 null/undefined */
  mediaUrl?: string
}

/** 用户信息 */
export interface UserVO {
  id: number
  email: string
  username: string
  avatarUrl: string
  emailVerified: boolean
  createdAt: string
}

/** 登录/注册成功响应（含 Sa-Token 会话） */
export interface LoginVO {
  tokenName: string
  tokenValue: string
  expiresIn: number
  user: UserVO
}

/** 登录参数 */
export interface LoginDTO {
  email: string
  password: string
}

/** 注册参数 */
export interface RegisterDTO {
  email: string
  password: string
  username?: string
}

/** 更新资料参数 */
export interface UpdateProfileDTO {
  username?: string
  avatarUrl?: string
}

/** 我的配额与用量 */
export interface MyQuotaVO {
  plan: string
  crushLimit: number
  dailyChatLimit: number
  todayMessageCount: number
  crushCount: number
}

/** 版本快照 */
export interface VersionVO {
  id: number
  crushId: number
  version: number
  reason?: string
  snapshot?: string
  createdAt: string
}
