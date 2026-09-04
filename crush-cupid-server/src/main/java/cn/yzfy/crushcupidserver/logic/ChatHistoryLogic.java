package cn.yzfy.crushcupidserver.logic;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.ChatMedia;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import cn.yzfy.crushcupidserver.model.entity.Conversation;
import cn.yzfy.crushcupidserver.model.vo.ChatHistoryVO;
import cn.yzfy.crushcupidserver.security.OwnershipGuard;
import cn.yzfy.crushcupidserver.security.SecurityUtils;
import cn.yzfy.crushcupidserver.security.UserChatCipher;
import cn.yzfy.crushcupidserver.service.ChatMediaService;
import cn.yzfy.crushcupidserver.service.ConversationService;
import cn.yzfy.crushcupidserver.service.CrushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对话历史业务逻辑层：按 crushSlug 加载本地 PG 已落库对话记录，并关联 chat_media 回填图片 URL。
 * 数据访问委托 MP 薄 Service，本层负责图片标记匹配与兜底清理规则。
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryLogic {

    private static final String IMG_MARKER = "[图片]";
    /** 兼容历史内联格式 [[图片:URL]]：图片 URL 自带在标记内，直接提取并移除标记 */
    private static final Pattern LEGACY_IMG = Pattern.compile("\\[\\[图片:([^\\]]*)\\]\\]");

    private final CrushService crushService;
    private final ConversationService conversationService;
    private final ChatMediaService chatMediaService;
    private final OwnershipGuard ownershipGuard;
    private final UserChatCipher userChatCipher;

    public List<ChatHistoryVO> history(String crushSlug) {
        if (StrUtil.isBlank(crushSlug)) {
            throw BizException.badRequest("crushSlug 不能为空");
        }
        Crush crush = crushService.getBySlug(crushSlug);
        if (crush == null) {
            throw BizException.notFound("未找到暗恋对象：" + crushSlug);
        }
        // 多租户归属校验：非本人 crush → 403（严格隔离，仅能看自己创建的）
        ownershipGuard.requireOwnership(crush.getId());
        long uid = SecurityUtils.currentUserId();

        // 1. 查对话文本记录（按归属用户隔离 + 解密，存量明文/历史全局密钥由 UserChatCipher 兜底）
        List<Conversation> rows = conversationService.lambdaQuery()
                .eq(Conversation::getCrushId, crush.getId())
                .eq(Conversation::getUserId, uid)
                .orderByAsc(Conversation::getCreatedAt)
                .list();
        List<ChatHistoryVO> list = rows.stream().map(row -> {
            ChatHistoryVO vo = ChatHistoryVO.of(row);
            vo.setContent(userChatCipher.decryptForUser(row.getContent(), uid));
            return vo;
        }).toList();

        // 2. 查图片 URL 记录（独立于 conversation，按 created_at 排序）
        List<ChatMedia> mediaList = chatMediaService.lambdaQuery()
                .eq(ChatMedia::getCrushId, crush.getId())
                .orderByAsc(ChatMedia::getCreatedAt)
                .list();
        Deque<String> mediaQueue = new ArrayDeque<>();
        mediaList.forEach(m -> mediaQueue.add(m.getMediaUrl()));

        // 3. 按 [图片] 标记顺序匹配回填 mediaUrl（content 中每个 [图片] 对应队列里最早的一张图，FIFO）
        for (ChatHistoryVO vo : list) {
            String c = vo.getContent();
            if (c == null) {
                continue;
            }
            // 4a. 历史内联 [[图片:URL]]：URL 内嵌，直接回填 mediaUrl 并清掉标记
            Matcher legacy = LEGACY_IMG.matcher(c);
            StringBuilder cleaned = new StringBuilder();
            int last = 0;
            while (legacy.find()) {
                cleaned.append(c, last, legacy.start());
                String url = legacy.group(1);
                if (StrUtil.isNotBlank(url) && vo.getMediaUrl() == null) {
                    vo.setMediaUrl(url);
                }
                last = legacy.end();
            }
            if (last > 0) {
                cleaned.append(c, last, c.length());
                c = cleaned.toString();
            }
            // 4b. 新占位标记 [图片]：按出现顺序消费 chat_media FIFO 队列
            if (c.contains(IMG_MARKER)) {
                while (c.contains(IMG_MARKER) && !mediaQueue.isEmpty()) {
                    String url = mediaQueue.poll();
                    c = c.replaceFirst(IMG_MARKER, "");
                    if (vo.getMediaUrl() == null) {
                        vo.setMediaUrl(url);
                    }
                }
                // 清理残留的未匹配 [图片] 标记
                c = c.replace(IMG_MARKER, "").trim();
            }
            vo.setContent(c);
        }

        return list;
    }
}