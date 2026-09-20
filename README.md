# Cupid

> *"每一支射出的箭，都是一次未说出口的喜欢。"*

**把暗恋蒸馏成 AI 引擎 —— 通过 GitHub 远端的 Skill，生成一个真正像 ta 的智能 agent。**

[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-purple.svg)](https://spring.io/projects/spring-ai)
[![Spring AI Alibaba](https://img.shields.io/badge/Spring%20AI%20Alibaba-1.1.2.3-orange.svg)](https://java2.cn.alibaba.com/)
[![DeepSeek](https://img.shields.io/badge/DeepSeek-chat-orange.svg)](https://www.deepseek.com/)
[![通义千问](https://img.shields.io/badge/Qwen-DashScope-blue.svg)](https://dashscope.aliyun.com/)
[![Vue 3](https://img.shields.io/badge/Vue-3-brightgreen.svg)](https://vuejs.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[crush-skills](https://github.com/xiaoheizi8/crush-skills) 的 Java 服务端实现。它把「暗恋对象」蒸馏成可对话的 AI：从 GitHub 远端拉取 Skill 提示词，用 ta 的语气说话，记得你们之间的每一个心动瞬间。

⚠️ **本项目仅用于个人情感分析与回忆，不用于骚扰、跟踪或侵犯他人隐私。**

[它能做什么](#它能做什么) · [架构](#架构) · [设计模式](#设计模式) · [快速开始](#快速开始) · [配置](#配置) · [接口](#接口)

---

## 它能做什么

| 功能 | 说明 |
|------|------|
| Skill 远程调用 | 通过 GitHub raw URL 拉取 SKILL.md 元信息 + prompts 模板，本地 TTL 缓存 |
| 多供应商路由 | DeepSeek / 通义千问 / OpenAI / Qwen-Native（DashScope 原生）一键切换，按 `provider` 字段路由 |
| 视觉/音频对话 | 供应商能力可配：文本（默认）/ 视觉看图（vision）/ 音频听语音（audio）；带图自动切视觉模型（qwen-vl 默认），OCR 已弃用 |
| 多条消息 | LLM 用 `\|\|\|` 分隔连发短消息，像真人微信一样「在吗？」「哈哈哈」「你猜」 |
| 表情包 | LLM 按情绪/性格/情境自主决定何时发表情包，后端 prompt 标记 `[[sticker:情绪]]` 方案（绕开 Spring AI 流式 tool call 不稳定），从 ChineseBQB 素材库随机抽取，经 jsdelivr CDN 加速，独立气泡渲染 |
| 图片上传持久化 | 对话中上传图片自动落盘（`chat_media` 表独立存储 URL），刷新/重启后历史回显不丢失 |
| 主动消息 | crush 不依赖用户输入主动连发（「等 ta 主动找我」按钮），可带场景暗示；SSE 心跳保活 |
| 语音合成 | CosyVoice v2 模型（WebSocket 直联合成），全手动点击播放，支持声音设计生成专属声线 |
| Persona 建模 | 5 层人格模型（硬规则 → 身份 → 说话风格 → 情感模式 → 关系行为） |
| 对话记忆入库 | 对话历史落库 PostgreSQL（自封装 `PgChatMemoryRepository`），跨刷新/重启续上下文 |
| 历史加载 | 进入对话页自动拉取该 crush 的历史消息渲染气泡（文本 + 图片 + 表情包） |
| OCR 识别 | ~~阿里云百炼「通用 OCR」MCP~~（已弃用），识图统一走视觉大模型（qwen-vl / gpt-4o） |
| 工具调用 | 封装 `@Tool`（列出暗恋对象 / 查画像 / 拉远端 prompt） |
| 语音播放 | 聊天页 QQ 式语音按钮：assistant 文本气泡旁 ▶ 点击即播（CosyVoice 合成 base64 mp3），播放中变 ■ 可停止 |
| Advisor 封装 | 安全边界 / Persona / Memory 三个自定义 advisor 注入系统提示 |
| 线程池安全治理 | 虚拟线程池 + 信号量限流（SSE 并发上限 200）+ Flux 5 分钟超时 + 订阅取消防资源泄漏 |

---

## 演示
 ![img.png](img.png)
![img_1.png](img_1.png)![img_2.png](img_2.png)
**核心链路**：`SkillResourceClient`（Adapter）从 GitHub raw 拉取 `SKILL.md` + `prompts/*.md`，`CachingSkillResourceClient`（Decorator）加 TTL 缓存；`CupidAgent`（Facade）按 `provider` 路由到对应 `ChatClient`，编排 advisor + tool + memory，输出经 `MessageSeparator` 切成多条消息流式回传；`PgChatMemoryRepository` 把对话历史落库 PG；图片 URL 独立存 `chat_media` 表，表情包走 `[[sticker:情绪]]` prompt 标记方案绕开 Spring AI 流式 tool call 不稳定。

---

## 设计模式

项目用 Adapter / Decorator（Skill 资源取用 + 缓存）、Facade（Agent 编排）、Strategy（多 LLM 供应商路由）、Template Method（advisor 注入提示骨架）、Factory（AI 装配）、DTO/VO 分层等常见模式组织代码，不在此逐一展开。

---

## 技术栈

```
Java 17 · Spring Boot 3.5 · Spring AI 1.1.2 · Spring AI Alibaba 1.1.2.3
DeepSeek / 通义千问(DashScope) / OpenAI · CosyVoice 语音合成
MyBatis-Plus · PostgreSQL · Hutool
Vue 3 + Ant Design Vue · Vite
```

---

## 快速开始

```bash
# 1. 后端（依赖：JDK17 + Maven + PostgreSQL）
export DEEPSEEK_API_KEY=sk-xxx        # 默认供应商 DeepSeek
export DASHSCOPE_API_KEY=sk-xxx       # 通义千问 + 语音合成（可选，缺省跳过该供应商）
cd crush-cupid-server
mvn -DskipTests spring-boot:run        # 启动在 http://localhost:91

# 2. 前端
cd crush-cupid-web
npm install
npm run dev                            # 打开 http://localhost:5173
```

> 前端通过 Vite 代理把 `/api` 转发到 `localhost:91`；后端也配了 CORS，跨域直连同样可用。

---

## 配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/crushCupid
    username: postgres
    password: 123456
  ai:
    # Alibaba DashScope：通义千问原生 + CosyVoice 语音
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
      audio:
        speech:
          default-options:
            model: cosyvoice-v3-flash   # 模型名
            voice: longyingling_v3      # 音色（v3 系列用 _v3 音色）

# 多供应商路由（自封装，按 provider 字段切换）
crush:
  ai:
    default-provider: deepseek
    providers:
      deepseek:
        base-url: https://api.deepseek.com
        api-key: ${DEEPSEEK_API_KEY}
        model: deepseek-chat
      qwen:                            # OpenAI 兼容协议走通义
        base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
        api-key: ${DASHSCOPE_API_KEY}
        model: qwen-plus
      qwen-vl:                         # 视觉能力（图像理解）
        base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
        api-key: ${DASHSCOPE_API_KEY}
        model: qwen-vl-plus
        vision: true
      openai:                          # 视觉 + 音频能力
        base-url: https://api.openai.com
        api-key: ${OPENAI_API_KEY}
        model: gpt-4o
        vision: true
        audio: true
  skill:
    # GitHub raw 基础地址，兼容多种写法（raw 目录 / github.com/tree / 指向 SKILL.md 的完整地址）
    base-url: https://raw.githubusercontent.com/xiaoheizi8/crush-skills/main
    cache-ttl: 3600                    # 本地缓存过期秒数
  # 图片上传落盘（对话中发送的图片 base64 持久化到磁盘）
  upload:
    dir: D:/uploads                     # 绝对路径，与 WebMvcConfig 静态映射对齐；需确保目录存在（启动时自动创建）
    url-prefix: /api/uploads           # 对外访问 URL 前缀
  # 主动消息调度
  proactive:
    enabled: true
    scan-interval-ms: 60000
    cooldown-minutes: 90
    daily-limit: 3
    max-concurrent: 2                 # 并发 LLM 决策任务上限
  # 表情包配置
  sticker:
    enabled: true
    cache-ttl: 3600
    chinesebqb:
      repo: zhaoolee/ChineseBQB
      default-emotion: 开心
      topics:                         # 情绪 → ChineseBQB 目录映射
        开心: 001Cat
        可爱: 001Cat
        无语: 006Pikachu
        生气: 023Aubrey
        委屈: 023Aubrey
        吃瓜: 056Doraemon
        疑惑: 007Conan
        尴尬: 007Conan
        撒娇: 001Cat
        么么哒: 056Doraemon
        晚安: 056Doraemon
        早安: 056Doraemon
```

> `qwen-native` 供应商由 Spring AI Alibaba 自动注册（DashScope 原生协议），无需在此声明。缺 `api-key` 的供应商启动时自动跳过，不阻塞。

---

## 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat` | 对话（SSE 流式，`{crushSlug, message, provider?, media?}`） |
| POST | `/api/chat/proactive` | 主动消息（SSE 流式，`{crushSlug, provider?, contextHint?}`） |
| POST | `/api/chat/voice` | 语音合成（`{text, voice?}` → `Result<String>` base64 mp3） |
| POST | `/api/chat/voice/design` | 声音设计（`{voicePrompt, previewText?}` → 专属 voice_id） |
| GET | `/api/chat/history?crushSlug=xxx` | 加载历史对话（`Result<List<ChatHistoryVO>>`） |
| GET | `/api/crush` | 列出暗恋对象 |
| POST | `/api/crush` | 新建暗恋对象 |
| PUT | `/api/crush/{id}` | 更新暗恋对象 |
| DELETE | `/api/crush/{id}` | 删除暗恋对象 |
| GET | `/api/skill/catalog` | 远端 Skill 元信息 + 可用 prompts |
| GET | `/api/skill/prompt/{name}` | 拉取指定 prompt 模板 |

**对话请求示例**：

```jsonc
// 纯文本
{ "crushSlug": "xiaomei", "message": "今天好累" }

// 切到千问
{ "crushSlug": "xiaomei", "message": "今天好累", "provider": "qwen" }

// 视觉能力（自动校验供应商 vision: true，自动切换视觉模型 qwen-vl）
{
  "crushSlug": "xiaomei",
  "message": "这张图让我想到你",
  "provider": "qwen-vl",
  "media": [{ "type": "IMAGE_URL", "mimeType": "image/jpeg", "data": "https://..." }]
}
```

**SSE 多条消息协议**：后端按 `index` 切气泡，每条 `{index, content, type, done}`，前端按 index 跳变即新气泡。

| 字段 | 类型 | 说明 |
|------|------|------|
| index | int | 气泡序号，相同 index 的 chunk 追加内容，不同 index 开新气泡 |
| content | string | 文本片段或表情包 URL |
| type | string | `text` 文本气泡 / `sticker` 表情包气泡 |
| done | boolean | 当前 index 是否完成（流式切分完成后最后一个 chunk 为 true） |

**表情包协议**：`type=sticker` 的 chunk，content 为图片 URL（经 `[[sticker:情绪]]` 标记方案由后端替换为真实 URL），前端按图片渲染独立气泡，跳过语音合成。

---

## 项目结构

```
crush-cupid-server/                      crush-cupid-web/
├── src/main/java/cn/yzfy/crushcupidserver/
│   ├── agent/        # CupidAgent 门面 + Tools + Advisors + MessageSeparator + VoiceService
│   │                   + StickerService (表情包) + ImageStorageService (图片持久化) + StickerSanitizer
│   ├── agent/tool/   # StickerTools (表情包 tool) + CrushTools
│   ├── agent/proactive/ # ProactiveSchedulerService + ProactivePushService (主动消息 + SSE 心跳)
│   ├── skill/        # GitHub 远端 Skill 调用（Adapter + Decorator）
│   ├── model/        # entity / dto / vo / mapper / service / converter
│   │                   + ChatMedia (chat_media 表独立存储图片 URL)
│   ├── controller/   # REST + SSE 接口
│   ├── config/       # AiConfig / LlmProperties / ChatModelRegistry / ChatClientProvider
│   │                   + PgChatMemoryRepository + ThreadPoolsConfig + UploadProperties + WebMvcConfig
│   ├── common/       # 统一返回结构 Result<T>
│   └── exception/    # 全局异常处理
└── src/main/resources/
    └── application.yml
```

---

## 注意事项

- **Spring AI 流式 tool call 不稳定**：`stream()` + tool call round-trip 在 Spring AI 1.1.2 下可能卡住 SSE。表情包因此采用 **prompt 标记方案**（LLM 输出 `[[sticker:情绪]]`，后端替换为真实 URL），不走 tool call。
- **表情包历史回显**：`PgChatMemoryRepository` 写入侧原样存 `[[sticker:URL]]`（保留 URL），读取侧注入 prompt 前清洗为占位文本（防 LLM 模仿）。前端 `loadHistory` 解析标记提取 URL 渲染 sticker 气泡。
- **图片独立存储**：对话中上传的图片 URL 独立存 `chat_media` 表（不拼进消息文本），按 `[图片]` 标记顺序 FIFO 匹配回填。`WebMvcConfig` 注册 `/api/uploads/**` → `file:D:/.../uploads/` 静态映射，**必须确保目录存在且路径正确**。
- **SSE 并发上限**：虚拟线程池 + 信号量限流 200，超过返回 503。Flux 5 分钟超时自动释放资源。Emitter 关闭（断连/超时/完成）时 dispose Flux 订阅 + release 信号量，防止资源泄漏。
- **语音播放按钮**：回复生成不自动合成语音，需点文本气泡旁 ▶ 按钮逐条触发（播放中变 ■ 可停止）。sticker 气泡天然跳过语音合成。
- **CosyVoice v2 音色**：v2 模型配 `longxiaochun_v2` 音色；v3 系列配 `_v3` 系统音色。参考 [阿里云文档](https://www.alibabacloud.com/help/zh/model-studio/cosyvoice-voice-list)。
- **DeepSeek Function Calling 不稳定**：架构以 advisor 注入 skill prompt 为主链路，工具调用只作可选增强，happy path 不依赖模型调工具。
- **本机到 GitHub raw 仅 IPv6 可达**：`spring-boot-maven-plugin` 里已配 `-Djava.net.preferIPv6Addresses=true`，用别的启动方式时需自行带上。
- **Spring AI 版本对齐**：项目用 Spring AI 1.1.2（而非最新 1.1.8），是为了对齐 Spring AI Alibaba 1.1.2.3 的硬依赖；所用 API 均为 1.1 GA 起即有，降级安全。
- **OCR 已弃用**：识图统一走视觉大模型（`vision: true` 供应商 / 自动切 qwen-vl）。MCP OCR 连接配置与 `mcp-servers.json` 已移除，`OcrService`/`OcrTools` 保留为兼容空壳，`crush.ocr.enabled=false` 时完全不初始化。

---

## 相关项目

- [crush-skills](https://github.com/xiaoheizi8/crush-skills) — Skill 资源仓库（SKILL.md + Prompt 模板 + Python 工具）

## License

MIT
## Star History

[![RepoStars](https://repostars.dev/api/embed?repo=xiaoheizi8/crush-skills)](https://repostars.dev/?repos=xiaoheizi8/crush-skills)