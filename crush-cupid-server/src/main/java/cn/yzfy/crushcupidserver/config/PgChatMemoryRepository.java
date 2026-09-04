package cn.yzfy.crushcupidserver.config;

import cn.yzfy.crushcupidserver.agent.StickerSanitizer;
import cn.yzfy.crushcupidserver.model.entity.Conversation;
import cn.yzfy.crushcupidserver.security.UserChatCipher;
import cn.yzfy.crushcupidserver.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @className PgChatMemoryRepository
 * @description 基于 PostgreSQL {@code conversation} 表实现的会话记忆仓库。
 * <p>
 * 表结构 {@code (id, crush_id, user_id, role, content, created_at)}：
 * conversationId 约定为 {@code "u{userId}:crush:{crushId}"}，由本类解析出 userId（会话归属）与
 * crushId 后按 {@code (user_id, crush_id)} 读写 conversation 表，实现「同一 crush 多用户共享」时
 * 会话记忆与加密按用户隔离（共享演示桶 user_id=0 亦互不干扰）。
 * <p>
 * 存储约定：
 * - role 存 MessageType 小写（user/assistant/system/tool）；
 * - user_id 存会话归属用户 id（即当前对话用户；0=系统共享/演示桶）；
 * - content 存 message.getText() 纯文本（多模态 Media 暂不入库，下次重发即可），并按归属用户点对点加密。
 * <p>
 * 兼容：旧数据以 {@code "crush:{crushId}"}（无 user 前缀）写入，解析时视作归属 {@code 0}/共享桶，
 * 解密回退全局 KEK 兜底明文，滚动迁移无感。
 * <p>
 * saveAll 采用「先按 (user_id, crush_id) 清空 + 批量插入」的覆盖语义，符合 {@link ChatMemoryRepository} 契约。
 * @author 一朝风月
 * @code repository
 * @createTime 2026-08-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PgChatMemoryRepository implements ChatMemoryRepository {

    /** conversationId 前缀（用户维度），与 CupidAgent 内 {@code "u{uid}:crush:{crushId}"} 对齐 */
    public static final String USER_PREFIX = "u";
    /** conversationId crush 段前缀 */
    public static final String CONV_PREFIX = "crush:";

    private final ConversationService conversationService;
    private final UserChatCipher userChatCipher;

    @Override
    public List<String> findConversationIds() {
        // 取所有 (user_id, crush_id) 有记录的会话对，拼成 "u{uid}:crush:{cid}" 作为 conversationId
        return conversationService.list().stream()
                .map(c -> USER_PREFIX + (c.getUserId() == null ? 0L : c.getUserId())
                        + ":" + CONV_PREFIX + c.getCrushId())
                .distinct()
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        ConvKey key = parse(conversationId);
        if (key == null) {
            return List.of();
        }
        // 按归属用户 + crush 过滤，created_at 升序还原对话顺序
        List<Conversation> rows = conversationService.lambdaQuery()
                .eq(Conversation::getCrushId, key.crushId())
                .eq(Conversation::getUserId, key.userId())
                .orderByAsc(Conversation::getCreatedAt)
                .list();
        List<Message> messages = new ArrayList<>(rows.size());
        for (Conversation row : rows) {
            // 读取时按归属用户解密：存量明文/历史全局密钥由 UserChatCipher 兜底
            String content = userChatCipher.decryptForUser(row.getContent(), key.userId());
            Message msg = toMessage(row.getRole(), content);
            if (msg != null) {
                messages.add(msg);
            }
        }
        // 历史注入 prompt 前清洗 assistant 侧表情包痕迹：
        // 把 [[sticker:URL]] / [表情包] / 裸 URL 替换为占位文本，防止 LLM 看到后模仿输出。
        // 写入侧不清洗——原样存 [[sticker:URL]]，保留 URL 供前端历史回显。
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (m instanceof AssistantMessage am) {
                String cleaned = StickerSanitizer.sanitize(am.getText());
                // 额外清洗存量脏数据里的 [表情包]（单括号，旧占位格式）和 (此处发表了一个表情包)
                if (cleaned != null) {
                    cleaned = cleaned.replace("[表情包]", StickerSanitizer.PLACEHOLDER);
                }
                if (cleaned != null && !cleaned.equals(am.getText())) {
                    messages.set(i, new AssistantMessage(cleaned));
                }
            }
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        ConvKey key = parse(conversationId);
        if (key == null) {
            log.warn("saveAll 跳过：无法解析 conversationId={}", conversationId);
            return;
        }
        // 覆盖语义：先清空该 (user_id, crush_id) 的历史，再批量插入新列表
        conversationService.lambdaUpdate()
                .eq(Conversation::getCrushId, key.crushId())
                .eq(Conversation::getUserId, key.userId())
                .remove();
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<Conversation> rows = new ArrayList<>(messages.size());
        Date now = new Date();
        for (Message msg : messages) {
            Conversation row = new Conversation();
            row.setCrushId(key.crushId());
            row.setUserId(key.userId());
            row.setRole(roleCode(msg.getMessageType()));
            // 写入侧不清洗：原样存 [[sticker:URL]]，保留 URL 供前端历史回显；并按归属用户点对点加密落库。
            // 读取侧（findByConversationId）解密后注入 prompt 前清洗，防止 LLM 模仿 URL。
            row.setContent(userChatCipher.encryptForUser(msg.getText(), key.userId()));
            row.setCreatedAt(now);
            rows.add(row);
        }
        conversationService.saveBatch(rows);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        ConvKey key = parse(conversationId);
        if (key == null) {
            return;
        }
        conversationService.lambdaUpdate()
                .eq(Conversation::getCrushId, key.crushId())
                .eq(Conversation::getUserId, key.userId())
                .remove();
    }

    /** conversationId -> ConvKey(userId, crushId)；无法解析返回 null。兼容旧 "crush:{cid}"（userId=0）。 */
    private ConvKey parse(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        if (conversationId.startsWith(USER_PREFIX + ":")) {
            int sep = conversationId.indexOf(":" + CONV_PREFIX);
            if (sep < 0) {
                return null;
            }
            try {
                long userId = Long.parseLong(conversationId.substring(USER_PREFIX.length() + 1, sep));
                long crushId = Long.parseLong(conversationId.substring(sep + CONV_PREFIX.length() + 1));
                return new ConvKey(userId, crushId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        // 旧格式 "crush:{crushId}"：归属视作共享桶 0
        if (conversationId.startsWith(CONV_PREFIX)) {
            try {
                return new ConvKey(0L, Long.parseLong(conversationId.substring(CONV_PREFIX.length())));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private record ConvKey(long userId, long crushId) {
    }

    /** MessageType -> 表中 role 字段小写值 */
    private String roleCode(MessageType type) {
        return switch (type) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

    /** 表中 role 字段 -> Message 实例。tool 消息无对应类，跳过返回 null（不渲染） */
    private Message toMessage(String role, String content) {
        if (role == null) {
            return null;
        }
        return switch (role.toLowerCase()) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            // TOOL 消息无独立 Message 子类，回读时跳过
            case "tool" -> null;
            default -> null;
        };
    }
}
