package cn.yzfy.crushcupidserver.agent.proactive;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.agent.CupidAgent;
import cn.yzfy.crushcupidserver.config.ChatClientProvider;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 主动发言决策者：让 LLM 结合整个人（性格、记忆、关系状态、对话历史、用户最近活跃情况）
 * 自主决定「此刻是否该主动找人、这轮说什么方向、下一次什么时候再找」。
 * <p>
 * 这是「主动消息」策略的核心 —— 调度器只负责按时唤醒探测（{@link ProactiveSchedulerService}），
 * 真正「发不发、发几次、发什么」交给 LLM 推理，而不是固定 cron 规则。
 *
 * @author 一朝风月
 * @code service
 * @createTime 2026-08-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProactiveDecisionService {

    private final CupidAgent cupidAgent;
    private final ChatClientProvider chatClientProvider;
    private final ObjectMapper objectMapper;

    /**
     * LLM 决策当前时间窗口是否主动发言。任何解析失败都回退到「暂不发言 + 默认下次窗口」，
     * 保证调度永不因单个 crush 决策异常而中断。
     */
    public ProactiveDecision decide(Crush crush) {
        String prompt = buildDecisionPrompt(crush);
        ProactiveDecision fallback = defaultNoSend();
        try {
            String raw = callWithTimeout(() -> chatClientProvider.getDefault()
                            .prompt(new Prompt(
                                    List.of(new SystemMessage(decisionSystem()), new UserMessage(prompt))))
                            .call()
                            .content(),
                    java.time.Duration.ofSeconds(DECISION_CALL_TIMEOUT_SECONDS));
            return parse(raw, fallback);
        } catch (Exception e) {
            log.warn("主动发言决策失败 crush={} err={}", crush.getSlug(), e.getMessage());
            return fallback;
        }
    }

    private static final long DECISION_CALL_TIMEOUT_SECONDS = 60;

    /** 带超时的阻塞调用：决策卡死时抛异常回退，避免永久占用主动消息信号量槽位。 */
    private String callWithTimeout(Supplier<String> supplier, java.time.Duration timeout) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new BizException("决策模型调用超时（" + timeout.toSeconds() + "s）");
        } catch (Exception e) {
            future.cancel(true);
            log.warn("决策模型调用失败：{}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            throw new BizException("主动消息决策失败，请稍后再试");
        }
    }

    /** 系统约束：告诉 LLM 它是个决策者，只输出 JSON。 */
    private String decisionSystem() {
        return "你是一位情感陪伴的「发言决策者」。判断一个拟人角色此刻是否应该主动给用户发消息。"
                + "你只能输出一个 JSON 对象，不要输出任何其他文字。格式如下：\n"
                + "{\"shouldSend\":true或false,\"reason\":\"理由\",\"messageDirection\":\"内容方向\",\"nextInMinutes\":整数}\n"
                + "字段说明：shouldSend=是否此刻主动发言；reason=简短理由；"
                + "messageDirection=若发言给内容生成的方向简述（如：关心 / 分享日常 / 撒娇 / 求关注 / 借故搭话 / 突然想起一件事）；"
                + "nextInMinutes=若本次不发言，多少分钟后（60~600）再考虑，或发言后希望隔多久再聊一嘴（300~1440）。";
    }

    /** 构建决策输入上下文：persona + 记忆 + 关系 + 时间 + 用户活跃度 + 当日次数。 */
    private String buildDecisionPrompt(Crush crush) {
        StringBuilder sb = new StringBuilder();
        sb.append("请判断此刻是否应该主动给用户发消息。\n\n");

        sb.append("## 角色画像（ta 是谁、怎么说话）\n")
                .append(StrUtil.blankToDefault(cupidAgent.buildPersona(crush), "（无）")).append("\n\n");

        sb.append("## 关系到目前的记忆与进展\n");
        sb.append("- 关系阶段(stage)：").append(crush.getCurrentStage() == null ? "?" : crush.getCurrentStage()).append("\n");
        sb.append("- 关系状态：").append(StrUtil.blankToDefault(crush.getRelationshipStatus(), "（无）")).append("\n");
        sb.append("- 最近一次聊天时间：").append(crush.getLastChatDate() == null
                ? "（尚不清楚）"
                : formatDateTime(crush.getLastChatDate())).append("\n");
        sb.append("- 记忆：\n").append(
                StrUtil.blankToDefault(cupidAgent.buildMemory(crush), "（无）")).append("\n\n");

        sb.append("## 当前时刻上下文\n");
        StringBuilder ctx = new StringBuilder();
        ctx.append("现在：").append(LocalDate.now()).append(" ").append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (crush.getLastChatDate() != null) {
            Duration away = Duration.between(crush.getLastChatDate().toInstant(), java.time.Instant.now());
            ctx.append("；用户已经约 ").append(away.toHours()).append(" 小时没互动了");
        } else {
            ctx.append("；用户刚认识 ta 不久，还没怎么聊过");
        }
        ctx.append("；今日已主动发言 ").append(countSafe(crush.getProactiveCount())).append(" 次");
        sb.append(ctx).append("\n");

        sb.append("\n请结合 ta 的性格与记忆，做出自然、不打扰、符合当下时段（早/午/晚/深夜）的判断。"
                + "注意：不要过度刷屏骚扰，深夜（23:00-7:00）除非关系很熟否则倾向不发言。");
        return sb.toString();
    }

    /** 解析 LLM JSON 文本为决策对象；失败回退 fallback。 */
    private ProactiveDecision parse(String raw, ProactiveDecision fallback) {
        if (StrUtil.isBlank(raw)) {
            return fallback;
        }
        String jsonStr = extractJsonObject(raw);
        try {
            JsonNode node = objectMapper.readTree(jsonStr);
            ProactiveDecision d = new ProactiveDecision();
            d.setShouldSend(node.path("shouldSend").asBoolean(false));
            d.setReason(node.path("reason").asText(null));
            d.setMessageDirection(node.path("messageDirection").asText(null));
            int next = node.path("nextInMinutes").asInt(120);
            if (next < 1) {
                next = 120;
            }
            d.setNextInMinutes(next);
            return d;
        } catch (Exception e) {
            log.warn("主动发言决策 JSON 解析失败 raw={} err={}", StrUtil.maxLength(jsonStr, 200), e.getMessage());
            return fallback;
        }
    }

    /** 从 LLM 回复里截取最外层 { ... } JSON 片段，容忍前后无关文字。 */
    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return raw;
        }
        return raw.substring(start, end + 1);
    }

    /** 默认决策：暂不发言，1 小时后再看。 */
    private ProactiveDecision defaultNoSend() {
        ProactiveDecision d = new ProactiveDecision();
        d.setShouldSend(false);
        d.setReason("决策不可用，回退延迟");
        d.setNextInMinutes(60);
        return d;
    }

    private int countSafe(Integer v) {
        return v == null ? 0 : v;
    }

    private String formatDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
