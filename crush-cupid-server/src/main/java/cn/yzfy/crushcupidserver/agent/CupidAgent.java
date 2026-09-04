package cn.yzfy.crushcupidserver.agent;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.agent.advisor.MemoryAdvisor;
import cn.yzfy.crushcupidserver.agent.advisor.PersonaAdvisor;
import cn.yzfy.crushcupidserver.common.DocumentTextExtractor;
import cn.yzfy.crushcupidserver.config.ChatClientProvider;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.dto.ChatRequestDTO;
import cn.yzfy.crushcupidserver.model.dto.ProactiveRequestDTO;
import cn.yzfy.crushcupidserver.model.entity.ChatMedia;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import cn.yzfy.crushcupidserver.service.ChatMediaService;
import cn.yzfy.crushcupidserver.service.CrushService;
import cn.yzfy.crushcupidserver.model.vo.MultiChunkVO;
import cn.yzfy.crushcupidserver.skill.SkillAdvisorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @className CupidAgent
 * @description 智能 agent 门面（Facade）：对外只暴露 chat，屏蔽工具注册、advisor、记忆、
 * 供应商路由与视觉/音频（vision/audio）能力分流细节。
 * <p>
 * 路由：按请求级 {@code provider} 选择 ChatClient；缺省走默认供应商（如 deepseek）。
 * 能力：若请求带 {@link cn.yzfy.crushcupidserver.model.dto.ChatMedia}，拼装为带 media 的 {@link UserMessage} 发送给模型；
 * 图片走视觉（vision）、音频走语音（audio）能力，分别校验目标供应商是否声明支持。
 * @author 一朝风月
 * @code facade
 * @createTime 2026-08-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CupidAgent {

    private final ChatClientProvider chatClientProvider;
    private final CrushService crushService;
    private final PersonaAdvisor personaAdvisor;
    private final MemoryAdvisor memoryAdvisor;
    private final org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor pgMemoryAdvisor;
    private final org.springframework.ai.chat.memory.MessageWindowChatMemory advisorChatMemory;
    private final StickerService stickerService;
    private final ImageStorageService imageStorageService;
    private final ChatMediaService chatMediaService;
    private final SkillAdvisorService skillAdvisorService;
    private final Scheduler aiScheduler;

    /**
     * 对话主入口，返回结构化多条消息流。LLM 用 {@value MessageSeparator#SEPARATOR} 分隔多条短消息，
     * 本方法用 {@link MessageSeparator} 流式切分成 {@link MultiChunkVO}，前端按 index 切气泡。
     * <p>
     * 同步阻塞处理：参数校验同步做（轻量，便于直接返回 HTTP 400）；DB 查询 + ChatClient 构造 +
     * UserMessage 拼装等重 IO 用 {@code Mono.zip + subscribeOn(aiScheduler)} 并行移到虚拟线程，
     * DB 查询和媒体处理并行执行，总耗时 = max(DB, 媒体处理) 而非相加，请求线程立即返回 Flux。
     */
    public Flux<MultiChunkVO> chat(ChatRequestDTO dto) {
        return doChat(dto, false);
    }

    /**
     * 军师模式对话（流式）：强制以「军师」身份回应，使用独立的军师记忆命名空间，
     * 与 {@link #chat}（模拟 crush）完全分离。skillPrompt 可携带具体子命令的任务要求。
     */
    public Flux<MultiChunkVO> advisorChat(ChatRequestDTO dto) {
        return doChat(dto, true);
    }

    private Flux<MultiChunkVO> doChat(ChatRequestDTO dto, boolean forcedAdvisorMode) {
        if (StrUtil.isBlank(dto.getCrushSlug())) {
            throw BizException.badRequest("缺少 crushSlug");
        }
        boolean hasText = StrUtil.isNotBlank(dto.getMessage());
        boolean hasMedia = dto.getMedia() != null && !dto.getMedia().isEmpty();
        if (!hasText && !hasMedia) {
            throw BizException.badRequest("消息与 media 至少有一个非空");
        }
        String provider = dto.getProvider();
        String crushSlug = dto.getCrushSlug();
        boolean advisorMode = forcedAdvisorMode || Boolean.TRUE.equals(dto.getAdvisorMode());
        // 同步入口（请求线程）捕获当前登录用户，作为会话记忆的归属维度（多用户共享 crush 时隔离历史）
        long ownerId = cn.yzfy.crushcupidserver.security.SecurityUtils.currentUserId();

        // 若请求带图片且当前供应商非视觉（vision），自动切到视觉模型（让模型真正"看懂"聊天图片）
        String effectiveProvider = chatClientProvider.resolveProvider(provider, hasImageMedia(dto));

        // 预处理并行化：DB 查询 ‖ 媒体处理，总耗时 = max(两者) 而非相加
        long preStart = System.currentTimeMillis();
        return Mono.zip(
                    Mono.fromCallable(() -> crushService.getBySlug(crushSlug))
                            .subscribeOn(aiScheduler),
                    Mono.fromCallable(() -> buildUserMessage(dto, effectiveProvider))
                            .subscribeOn(aiScheduler),
                    (crush, built) -> {
                        if (crush == null) {
                            throw BizException.notFound("未找到暗恋对象：" + crushSlug);
                        }
                        // 图片 URL 存入 chat_media 表（独立于 conversation，不受 saveAll 覆盖影响）
                        if (built.imageUrls() != null && !built.imageUrls().isEmpty()) {
                            saveChatMedia(crush.getId(), built.imageUrls());
                        }
                        ChatClient chatClient = chatClientProvider.get(effectiveProvider);
                        return new ChatContext(crush, chatClient, built.message(),
                                dto.getSkillPrompt(), advisorMode);
                    })
                .doOnSubscribe(s -> log.info("[chat] 预处理开始 crush={} advisorMode={}", crushSlug, advisorMode))
                .doOnNext(ctx -> log.info("[chat] 预处理完成 耗时={}ms crush={}",
                        System.currentTimeMillis() - preStart, crushSlug))
                .flatMapMany(ctx -> streamMulti(ctx.chatClient(), ctx.userMessage(), ctx.crush(), ctx.skillPrompt(), ctx.advisorMode(), ownerId));
    }

    /**
     * 主动消息入口：crush 主动发起连发多条短消息，模拟真人微信「不聊天时主动找你」。
     * 内部以元指令作为 user 消息触发模型主动发言，与 {@link #chat} 共用 persona/memory/分隔符协议。
     * <p>
     * 同样把预处理异步化到 boundedElastic，请求线程立即返回 Flux。
     */
    public Flux<MultiChunkVO> proactive(ProactiveRequestDTO dto) {
        if (StrUtil.isBlank(dto.getCrushSlug())) {
            throw BizException.badRequest("缺少 crushSlug");
        }
        String provider = dto.getProvider();
        String crushSlug = dto.getCrushSlug();
        String contextHint = dto.getContextHint();
        // 同步入口捕获当前登录用户（会话记忆归属维度）
        long ownerId = cn.yzfy.crushcupidserver.security.SecurityUtils.currentUserId();

        return Mono.fromCallable(() -> {
                    Crush crush = crushService.getBySlug(crushSlug);
                    if (crush == null) {
                        throw BizException.notFound("未找到暗恋对象：" + crushSlug);
                    }
                    ChatClient chatClient = chatClientProvider.get(provider);
                    UserMessage userMessage = new UserMessage(buildProactivePrompt(crush, contextHint));
                    return new ChatContext(crush, chatClient, userMessage, null, false);
                })
                .subscribeOn(aiScheduler)
                .flatMapMany(ctx -> streamMulti(ctx.chatClient(), ctx.userMessage(), ctx.crush(), ctx.skillPrompt(), ctx.advisorMode(), ownerId));
    }

    /**
     * 共用的流式调用 + 多条消息切分。绑定 persona/memory advisor 与会话记忆。
     * <p>
     * 切分阶段用 {@code publishOn(aiScheduler)} 让下游 chunk 投递到虚拟线程，
     * 避免 emitter.send 的同步 socket 写阻塞 LLM 流式响应线程。
     */
    private Flux<MultiChunkVO> streamMulti(ChatClient chatClient, UserMessage userMessage, Crush crush, String skillPrompt, boolean advisorMode, long ownerId) {
        // 用户维度会话记忆命名空间：u{ownerId}:crush:{crushId}（多用户共享同一 crush 时历史互不干扰）。
        // 军师对话使用独立命名空间与内存记忆，避免与「模拟 crush」对话历史互相污染。
        String conversationId = (advisorMode ? "u" + ownerId + ":advisor:" : "u" + ownerId + ":crush:") + crush.getId();
        String persona;
        if (advisorMode) {
            // 军师模式：用军师人设替代 crush 人格，注入 skill prompt 作为任务；记忆保留作背景上下文
            persona = skillAdvisorService.advisorSystemPrompt(skillPrompt);
        } else {
            persona = buildPersona(crush);
            if (StrUtil.isNotBlank(skillPrompt)) {
                persona = persona + "\n\n## 按需注入的 skill 提示\n" + skillPrompt.trim() + "\n";
            }
        }
        final String personaText = persona;
        String memory = buildMemory(crush);

        // 记忆 advisor 按模式选择：模拟对话用 PG 记忆；军师对话用独立内存记忆（不落库、不污染历史）
        org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor chatMemoryAdvisor = advisorMode
                ? org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor.builder(advisorChatMemory).build()
                : pgMemoryAdvisor;
        Flux<String> raw = chatClient.prompt()
                .messages(userMessage)
                .advisors(a -> a
                        .advisors(personaAdvisor, chatMemoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                        .param(PersonaAdvisor.CONTEXT_KEY, personaText)
                        .param(MemoryAdvisor.CONTEXT_KEY, memory))
                .stream()
                .content()
                // 日志：打印 LLM 原始输出，排查 ||| 分隔符和 [[sticker:...]] 标记是否存在
                .doOnNext(chunk -> {
                    if (chunk != null && !chunk.isEmpty()) {
                        // 只在包含关键标记时打印，避免刷屏
                        if (chunk.contains("|||") || chunk.contains("sticker") || chunk.contains("[")) {
                            log.info("[llm-raw] crush={} chunk={}", crush.getId(), chunk.length() > 200 ? chunk.substring(0, 200) + "..." : chunk);
                        }
                    }
                });

        // 用 Flux.defer 保证每次订阅都新建一个有状态的切分器
        // publishOn 把切分 + 后续 emitter.send 移到虚拟线程，不阻塞 LLM 流式响应线程
        return Flux.defer(() -> {
                    MessageSeparator splitter = new MessageSeparator();
                    return raw
                            .concatMapIterable(splitter::accept)
                            .concatWith(Flux.fromStream(splitter.finish().stream()));
                })
                .publishOn(aiScheduler)
                // 表情包标记 -> 实际图片 URL：
                // tool 路径：pickSticker 返回的标记 content 已是完整 URL（http/raw 或 /api/stickers），直接用；
                // prompt 标记路径：content 是情绪词，查 StickerService 随机抽图替换；
                // 两种路径都拿不到素材时丢弃该气泡，不影响文本流
                .map(vo -> {
                    if (MultiChunkVO.TYPE_STICKER.equals(vo.getType())) {
                        String content = vo.getContent();
                        if (isUrl(content)) {
                            // tool 产出的完整 URL，直接用
                        } else {
                            // 情绪词，查本地/远端素材
                            String url = stickerService.randomSticker(content);
                            log.info("[sticker-diag] emotion='{}' -> url={}", content, url);
                            if (url == null) {
                                vo.setType(MultiChunkVO.TYPE_TEXT);
                                vo.setContent("");
                                return vo;
                            }
                            vo.setContent(url);
                        }
                    }
                    return vo;
                });
    }

    /**
     * 异步预处理结果容器：把 crush / chatClient / userMessage 打包传给 streamMulti。
     */
    private record ChatContext(Crush crush, ChatClient chatClient, UserMessage userMessage,
                               String skillPrompt, boolean advisorMode) {
    }

    /**
     * 判断 sticker 标记 content 是否已是完整 URL（tool 路径产出）。
     * 本地路径 /api/stickers/... 或 http(s) 远端 URL 均算。
     */
    private boolean isUrl(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://") || s.startsWith(StickerService.URL_PREFIX));
    }

    /** 本次请求是否携带图片 media（图片需要视觉模型理解）。 */
    private boolean hasImageMedia(ChatRequestDTO dto) {
        if (dto.getMedia() == null) {
            return false;
        }
        return dto.getMedia().stream()
                .anyMatch(m -> cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_IMAGE_URL.equals(m.getType())
                        || cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_IMAGE_BASE64.equals(m.getType()));
    }

    /**
     * 静默生成主动消息并落库（供定时调度器使用）。
     * <p>
     * 复用 {@link #proactive(ProactiveRequestDTO)} 的 persona/memory/分隔符协议，但改用
     * 非流式 {@code .call()}：MessageChatMemoryAdvisor 会在 after 阶段把生成的 assistant
     * 消息写入 conversation 表，前端无需长连接即可于下次加载历史时看到。
     *
     * @param crush       目标暗恋对象
     * @param contextHint 场景暗示（可选，如「下雨天」「你刚发了条朋友圈」）
     * @return LLM 生成的原始回复文本（含 {@link MessageSeparator#SEPARATOR} 分隔的多条短消息）
     */
    public String proactiveSilent(Crush crush, String contextHint) {
        // 主动消息归属 crush 的所有者（私有 crush→其 owner；共享桶→0），写入 owner 的会话记忆空间
        long ownerId = crush.getUserId() == null ? 0L : crush.getUserId();
        String conversationId = "u" + ownerId + ":crush:" + crush.getId();
        UserMessage userMessage = new UserMessage(buildProactivePrompt(crush, contextHint, true));
        ChatClient chatClient = chatClientProvider.getDefault();
        return callWithTimeout(() ->
                        chatClient.prompt()
                                .messages(userMessage)
                                .advisors(a -> a
                                        .advisors(personaAdvisor, memoryAdvisor, pgMemoryAdvisor)
                                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                                        .param(PersonaAdvisor.CONTEXT_KEY, buildPersona(crush))
                                        .param(MemoryAdvisor.CONTEXT_KEY, buildMemory(crush)))
                                .call()
                                .content(),
                java.time.Duration.ofSeconds(PROACTIVE_CALL_TIMEOUT_SECONDS));
    }

    /** 非流式 LLM 调用超时兜底（秒）：守护线程上挂住会永久占用 proactive 信号量槽位与虚拟线程，必须限时。 */
    private static final long PROACTIVE_CALL_TIMEOUT_SECONDS = 90;

    /**
     * 带超时的阻塞调用：把 LLM 调用放到 CompletableFuture 上，超时抛 {@link BizException} 而非永久阻塞，
     * 保证调用方（主动消息调度持有信号量）异常路径能及时释放资源。
     */
    private <T> T callWithTimeout(Supplier<T> supplier, java.time.Duration timeout) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new BizException("模型调用超时（" + timeout.toSeconds() + "s）");
        } catch (Exception e) {
            future.cancel(true);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new BizException("模型调用失败：" + cause.getMessage());
        }
    }

    /**
     * 构造主动消息触发 prompt：注入时间、关系阶段、用户暗示，要求连发多条。
     */
    private String buildProactivePrompt(Crush crush, String contextHint) {
        return buildProactivePrompt(crush, contextHint, false);
    }

    /**
     * 构造主动消息触发 prompt。silent=true 时额外告知此刻无需用户在场（后台守护触发），
     * 让 LLM 说真实自然的话而不显得「被点醒」。
     */
    private String buildProactivePrompt(Crush crush, String contextHint, boolean silent) {
        StringBuilder sb = new StringBuilder();
        sb.append("【系统元指令】现在不是用户在和你说话，而是请你主动找用户聊天。\n");
        if (silent) {
            sb.append("此刻是自然生活的某个时刻，你想起 ta 了，主动开口说点什么。\n");
        }
        sb.append("当前时间：").append(LocalDate.now()).append(" ")
                .append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                .append("（请据此判断时段：早晨/午休/深夜等，调整说话的语气与内容）\n");
        if (StrUtil.isNotBlank(contextHint)) {
            sb.append("场景暗示：").append(contextHint).append("\n");
        }
        sb.append("根据你们的关系阶段、性格、记忆，自然地说点什么。\n");
        sb.append("可以选的方向：分享日常 / 撒娇 / 关心 / 抱怨 / 求关注 / 借故搭话 / 突然想起一件事。\n");
        sb.append("像真人微信一样连发多条短消息，用 ").append(MessageSeparator.SEPARATOR).append(" 分隔。\n");
        sb.append("不要解释、不要带括号动作描述、不要说\"我是 AI\"。\n");
        return sb.toString();
    }

    /**
     * 构造用户消息（按供应商能力分流媒体）：
     * <ul>
     *   <li>视觉（vision）供应商：图片以 {@link Media} 直传，模型图像理解（分享生活照的主路径，不走 OCR）</li>
     *   <li>音频（audio）供应商：音频以 {@link Media} 直传，模型语音理解</li>
     *   <li>非视觉供应商：图片走 OCR 兜底提取文字（截图/文档类仍可聊）；提取不到文字则告知模型「对方发了图但你看不见」；
     *       附件（FILE_BASE64）抽取文本内容拼进消息（与供应商能力无关）</li>
     * </ul>
     * <p>
     * 图片 URL 不再拼进消息文本，而是收集到 {@code imageUrls} 列表，由调用方存入 chat_media 表。
     * 消息文本中只插入 {@code [图片]} 占位标记，供 ChatHistoryController 顺序匹配回填。
     *
     * @return {@link BuiltMessage} 包含 UserMessage + 图片 URL 列表
     */
    private BuiltMessage buildUserMessage(ChatRequestDTO dto, String provider) {
        String text = StrUtil.blankToDefault(dto.getMessage(), "");
        List<cn.yzfy.crushcupidserver.model.dto.ChatMedia> mediaList = dto.getMedia();
        if (mediaList == null || mediaList.isEmpty()) {
            return new BuiltMessage(new UserMessage(text), List.of());
        }
        boolean vision = chatClientProvider.isVision(provider);
        boolean audio = chatClientProvider.isAudio(provider);
        StringBuilder enriched = new StringBuilder(text);
        List<Media> medias = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();

        for (cn.yzfy.crushcupidserver.model.dto.ChatMedia m : mediaList) {
            if (StrUtil.isBlank(m.getType()) || StrUtil.isBlank(m.getData())) {
                throw BizException.badRequest("ChatMedia 的 type/data 不能为空");
            }
            switch (m.getType()) {
                case cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_IMAGE_URL, cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_IMAGE_BASE64 -> {
                    String imageUrl;
                    try {
                        imageUrl = imageStorageService.storeImage(m);
                    } catch (Exception e) {
                        log.warn("图片持久化失败，跳过回显：{}", e.getMessage());
                        imageUrl = null;
                    }
                    if (vision) {
                        medias.add(toMedia(m));
                    } else {
                        enriched.append("\n[对方发来一张图片，你暂时无法查看图片内容，请不要编造图片细节]\n");
                    }
                    if (imageUrl != null) {
                        imageUrls.add(imageUrl);
                        // 占位标记：ChatHistoryController 按 [图片] 出现顺序匹配 chat_media 记录回填 URL
                        enriched.append("[图片]");
                    }
                }
                case cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_FILE_BASE64 -> {
                    String name = StrUtil.blankToDefault(m.getFileName(), "附件");
                    byte[] bytes = Base64.getDecoder().decode(m.getData());
                    String content;
                    try {
                        content = DocumentTextExtractor.extract(m.getFileName(), bytes);
                    } catch (Exception e) {
                        log.warn("对话附件解析失败 name={}：{}", name, e.getMessage());
                        content = "(附件解析失败)";
                    }
                    enriched.append("\n[对方发来附件「").append(name).append("」，内容如下]\n")
                            .append(StrUtil.blankToDefault(content, "(内容为空)")).append("\n");
                }
                case cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_AUDIO_URL, cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_AUDIO_BASE64 -> {
                    if (audio) {
                        medias.add(toMedia(m));
                    } else {
                        throw BizException.badRequest("当前供应商不支持语音输入，请切换支持音频的供应商");
                    }
                }
                default -> throw BizException.badRequest("不支持的 ChatMedia type：" + m.getType());
            }
        }
        UserMessage msg = medias.isEmpty()
                ? new UserMessage(enriched.toString())
                : UserMessage.builder().text(enriched.toString()).media(medias).build();
        return new BuiltMessage(msg, imageUrls);
    }

    /** buildUserMessage 的返回容器：UserMessage + 图片 URL 列表 */
    private record BuiltMessage(UserMessage message, List<String> imageUrls) {
    }

    /**
     * 把图片 URL 列表批量存入 chat_media 表，独立于 conversation，
     * 不受 PgChatMemoryRepository.saveAll 的覆盖语义影响。
     */
    private void saveChatMedia(Long crushId, List<String> imageUrls) {
        try {
            Date now = new Date();
            List<ChatMedia> entities = new ArrayList<>(imageUrls.size());
            for (String url : imageUrls) {
                ChatMedia e = new ChatMedia();
                e.setCrushId(crushId);
                e.setRole("user");
                e.setMediaUrl(url);
                e.setMediaType("image");
                e.setCreatedAt(now);
                entities.add(e);
            }
            chatMediaService.saveBatch(entities);
            log.info("[chat] 图片 URL 存入 chat_media：crushId={} count={}", crushId, entities.size());
        } catch (Exception e) {
            log.error("[chat] chat_media 存储失败 crushId={}：{}", crushId, e.getMessage());
        }
    }

    /**
     * 将 DTO 的 {@link cn.yzfy.crushcupidserver.model.dto.ChatMedia} 转为 Spring AI 的 {@link Media}。
     */
    private Media toMedia(cn.yzfy.crushcupidserver.model.dto.ChatMedia m) {
        if (StrUtil.isBlank(m.getType()) || StrUtil.isBlank(m.getData())) {
            throw BizException.badRequest("ChatMedia 的 type/data 不能为空");
        }
        MimeType mime = StrUtil.isNotBlank(m.getMimeType())
                ? MimeType.valueOf(m.getMimeType())
                : inferMimeType(m.getType());
        try {
            switch (m.getType()) {
                case cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_IMAGE_URL:
                case cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_AUDIO_URL:
                    // URL 形态：用公开构造器 new Media(MimeType, URI)
                    return new Media(mime, URI.create(m.getData()));
                case cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_IMAGE_BASE64:
                case cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_AUDIO_BASE64:
                    // base64 形态：用 Builder 接收解码后的 byte[]
                    return Media.builder()
                            .mimeType(mime)
                            .data((Object) Base64.getDecoder().decode(m.getData()))
                            .build();
                default:
                    throw BizException.badRequest("不支持的 ChatMedia type：" + m.getType());
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("解析多模态数据失败：" + e.getMessage());
        }
    }

    /**
     * 按 type 推断默认 MIME 类型。
     */
    private MimeType inferMimeType(String type) {
        return switch (type) {
            case cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_IMAGE_URL, cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_IMAGE_BASE64 -> MimeType.valueOf("image/png");
            case cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_AUDIO_URL, cn.yzfy.crushcupidserver.model.dto.ChatMedia.TYPE_AUDIO_BASE64 -> MimeType.valueOf("audio/wav");
            default -> MimeType.valueOf("application/octet-stream");
        };
    }

    public String buildPersona(Crush c) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是").append(c.getName()).append("，不是 AI 助手。用 ta 的方式说话、用 ta 的逻辑思考。\n");
        if (notBlank(c.getMbti())) sb.append("MBTI：").append(c.getMbti()).append("\n");
        if (notBlank(c.getZodiac())) sb.append("星座：").append(c.getZodiac()).append("\n");
        if (notBlank(c.getRelationshipStatus())) sb.append("与用户关系：").append(c.getRelationshipStatus()).append("\n");
        if (notBlank(c.getImpression())) sb.append("用户对你的印象：").append(c.getImpression()).append("\n");
        appendLayer(sb, "Layer 0 硬规则", c.getPersonaLayer0());
        appendLayer(sb, "Layer 1 身份", c.getPersonaLayer1());
        appendLayer(sb, "Layer 2 说话风格", c.getPersonaLayer2());
        appendLayer(sb, "Layer 3 情感模式", c.getPersonaLayer3());
        appendLayer(sb, "Layer 4 关系行为", c.getPersonaLayer4());
        appendMultiMessageGuide(sb);
        appendStickerGuide(sb);
        return sb.toString();
    }

    /**
     * 追加多条消息沟通风格指引：让模型像真人微信一样连发短消息，用 {@value MessageSeparator#SEPARATOR} 分隔。
     */
    private void appendMultiMessageGuide(StringBuilder sb) {
        sb.append("## 沟通风格\n");
        sb.append("像真人微信聊天一样连发多条短消息，每条都很短（几个字到一句话），不要写成一大段。\n");
        sb.append("必须用 ").append(MessageSeparator.SEPARATOR)
                .append(" 分隔每一条独立的消息。每个 ").append(MessageSeparator.SEPARATOR)
                .append(" 代表一次换行/发一条新消息。例如：\n");
        sb.append("  在吗？").append(MessageSeparator.SEPARATOR)
                .append("刚看到一个东西超像你").append(MessageSeparator.SEPARATOR)
                .append("哈哈哈哈你猜是啥\n");
        sb.append("上面三句话之间都用 ").append(MessageSeparator.SEPARATOR)
                .append(" 分开了，代表三条独立消息。你必须这样做，不要把所有内容写成一大段。\n");
        sb.append("不要带括号动作描述、不要解释、不要说\"我是 AI\"。\n");
    }

    /**
     * 追加表情包使用指引：prompt 标记方案（不走 tool call，避免 Spring AI 流式 tool round-trip 卡顿）。
     * <p>
     * LLM 根据消息内容 + crush 性格自主思考是否发表情包、发什么情绪，
     * 输出 {@code [[sticker:情绪词]]} 文本标记。后端 {@link MessageSeparator} 切成独立表情包气泡，
     * {@code streamMulti} 的 map 阶段把情绪词替换为真实图片 URL。
     * <p>
     * 表情包必须作为独立的一条消息（用 {@link MessageSeparator#SEPARATOR} 分隔），
     * 与文本分开——比如第一条是文本，第二条就是表情包。素材池为空时不注入。
     */
    private void appendStickerGuide(StringBuilder sb) {
        if (!stickerService.available()) {
            return;
        }
        sb.append("## 表情包\n");
        sb.append("你可以发表情包来让对话更生动。何时发：结合你的性格、当下心情、对方消息内容思考——")
          .append("聊天轻松时可以发开心/可爱的，被冷落时发委屈/吃瓜的，无语时发无语的，撒娇时发可爱的。")
          .append("不用每条都发，但在情绪该有表情包的时候一定要发。\n");
        sb.append("发法：输出 ").append(MessageSeparator.MARKER_PREFIX).append("情绪")
          .append(MessageSeparator.MARKER_SUFFIX).append(" 标记，系统自动换成对应图片。\n");
        sb.append("可选情绪：").append(String.join(" / ", stickerService.availableEmotions())).append("\n");
        sb.append("表情包必须单独作为一条消息，和文本分开。用 ").append(MessageSeparator.SEPARATOR)
          .append(" 分隔。例如：\n");
        sb.append("  在吗").append(MessageSeparator.SEPARATOR)
          .append(MessageSeparator.MARKER_PREFIX).append("开心").append(MessageSeparator.MARKER_SUFFIX)
          .append(MessageSeparator.SEPARATOR).append("刚看到一个东西超像你\n");
        sb.append("上面三条：第一条文本、第二条表情包、第三条文本。一条消息最多一个表情包标记。\n");
        sb.append("严禁：不要把标记附在文本消息末尾（必须用 ").append(MessageSeparator.SEPARATOR)
          .append(" 分隔成独立一条），不要直接输出图片链接/URL，不要发明其他表情包格式。\n");
    }

    public String buildMemory(Crush c) {
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        if (notBlank(c.getMemoryOverview())) { sb.append("## 关系记忆\n").append(c.getMemoryOverview()).append("\n"); any = true; }
        if (notBlank(c.getMemoryTimeline())) { sb.append("## 时间线\n").append(c.getMemoryTimeline()).append("\n"); any = true; }
        if (notBlank(c.getMemorySweet())) { sb.append("## 甜蜜瞬间\n").append(c.getMemorySweet()).append("\n"); any = true; }
        if (notBlank(c.getMemoryInteraction())) { sb.append("## 互动模式\n").append(c.getMemoryInteraction()).append("\n"); any = true; }
        return any ? sb.toString() : "";
    }

    private void appendLayer(StringBuilder sb, String title, String content) {
        if (notBlank(content)) {
            sb.append("## ").append(title).append("\n").append(content).append("\n");
        }
    }

    private boolean notBlank(String s) {
        return StrUtil.isNotBlank(s);
    }
}
