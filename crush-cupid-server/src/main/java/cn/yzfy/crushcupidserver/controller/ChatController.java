package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.agent.CupidAgent;
import cn.yzfy.crushcupidserver.config.ThreadPoolsConfig;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.logic.AuditLogic;
import cn.yzfy.crushcupidserver.logic.QuotaLogic;
import cn.yzfy.crushcupidserver.model.dto.ChatRequestDTO;
import cn.yzfy.crushcupidserver.model.dto.ProactiveRequestDTO;
import cn.yzfy.crushcupidserver.model.vo.MultiChunkVO;
import cn.yzfy.crushcupidserver.security.OwnershipGuard;
import cn.yzfy.crushcupidserver.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @className ChatController
 * @description 对话接口（SSE 流式）。每个 chunk 以 {@link MultiChunkVO} JSON 编码下发到 data 行；
 * 前端按 index 切气泡，支持 crush 一次连发多条短消息。
 * <p>
 * 安全治理：
 * <ul>
 *   <li><b>信号量限流</b>：并发 SSE 流上限 {@value ThreadPoolsConfig#SSE_MAX_CONCURRENT}，超过返回 503 快速失败</li>
 *   <li><b>Flux 超时</b>：LLM {@value ThreadPoolsConfig#LLM_TIMEOUT} 无响应自动取消，释放虚拟线程与连接</li>
 *   <li><b>订阅取消</b>：emitter 关闭（客户端断开/超时/完成）时立即 dispose Flux 订阅，防资源泄漏</li>
 *   <li><b>信号量防重复释放</b>：AtomicBoolean 保证 emitter 生命周期回调只释放一次</li>
 * </ul>
 *
 * @author crush-cupid
 * @code controller
 * @createTime 2026-08-26
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final CupidAgent cupidAgent;
    private final ObjectMapper objectMapper;
    private final ExecutorService sseExecutor;
    private final Semaphore sseLimiter;
    private final OwnershipGuard ownershipGuard;
    private final QuotaLogic quotaLogic;
    private final AuditLogic auditLogic;
    private final AtomicInteger activeStreams = new AtomicInteger(0);

    public ChatController(CupidAgent cupidAgent,
                          ObjectMapper objectMapper,
                          @Qualifier("sseExecutor") ExecutorService sseExecutor,
                          @Qualifier("sseLimiter") Semaphore sseLimiter,
                          OwnershipGuard ownershipGuard,
                          QuotaLogic quotaLogic,
                          AuditLogic auditLogic) {
        this.cupidAgent = cupidAgent;
        this.objectMapper = objectMapper;
        this.sseExecutor = sseExecutor;
        this.sseLimiter = sseLimiter;
        this.ownershipGuard = ownershipGuard;
        this.quotaLogic = quotaLogic;
        this.auditLogic = auditLogic;
    }

    @PreDestroy
    public void shutdown() {
        sseExecutor.shutdown();
    }

    /**
     * 用户主动对话（流式多条消息）。
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequestDTO dto) {
        ownershipGuard.requireWriteBySlug(dto.getCrushSlug());
        long uid = SecurityUtils.currentUserId();
        quotaLogic.checkDailyChatLimit(uid);
        return stream(afterDone(cupidAgent.chat(dto), uid, "chat", "CHAT"), "/api/chat " + safeSlug(dto.getCrushSlug()));
    }

    /**
     * 军师模式对话（流式多条消息）。独立于模拟对话，使用军师人设与独立内存记忆，
     * 不写入模拟对话历史。
     */
    @PostMapping(value = "/advisor", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter advisor(@RequestBody ChatRequestDTO dto) {
        ownershipGuard.requireWriteBySlug(dto.getCrushSlug());
        long uid = SecurityUtils.currentUserId();
        quotaLogic.checkDailyChatLimit(uid);
        return stream(afterDone(cupidAgent.advisorChat(dto), uid, "chat", "ADVISOR"), "/api/chat/advisor " + safeSlug(dto.getCrushSlug()));
    }

    /**
     * crush 主动发起对话（流式多条消息）。用户进入对话页或点击「等 ta 主动找我」时调用。
     */
    @PostMapping(value = "/proactive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter proactive(@RequestBody ProactiveRequestDTO dto) {
        ownershipGuard.requireWriteBySlug(dto.getCrushSlug());
        long uid = SecurityUtils.currentUserId();
        quotaLogic.checkDailyChatLimit(uid);
        return stream(afterDone(cupidAgent.proactive(dto), uid, "chat", "PROACTIVE"), "/api/chat/proactive " + safeSlug(dto.getCrushSlug()));
    }

    /**
     * 在流结束时记录用量与审计（不阻塞、失败静默）。
     */
    private Flux<MultiChunkVO> afterDone(Flux<MultiChunkVO> flux, long uid, String module, String action) {
        return flux.doFinally(signal -> {
            quotaLogic.consumeMessages(uid, 1, 1);
            auditLogic.success(module, action, "Chat", null, null, 0);
        });
    }

    /**
     * 把 MultiChunkVO 流接入 SseEmitter，全生命周期安全治理。
     * <p>
     * 请求线程：tryAcquire 信号量 → 创建 emitter → 注册回调 → 异步订阅 → 立即返回。
     * 订阅线程（虚拟）：消费 Flux → emitter.send → 完成/超时/异常时回调 → 释放信号量 + 取消订阅。
     */
    private SseEmitter stream(Flux<MultiChunkVO> flux, String tag) {
        // 1. 信号量限流：超过并发上限立即拒绝，不排队
        if (!sseLimiter.tryAcquire()) {
            log.warn("SSE [{}] 并发超限({})，拒绝请求", tag, ThreadPoolsConfig.SSE_MAX_CONCURRENT);
            throw BizException.badRequest("系统繁忙，请稍后重试");
        }

        long start = System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(ThreadPoolsConfig.SSE_TIMEOUT);
        AtomicBoolean firstSent = new AtomicBoolean(false);
        AtomicBoolean released = new AtomicBoolean(false);
        AtomicReference<Disposable> subRef = new AtomicReference<>();
        int concurrency = activeStreams.incrementAndGet();

        if (concurrency > 100) {
            log.warn("SSE 并发较高：{} 个活跃流", concurrency);
        }

        // 2. emitter 生命周期回调：任一回调触发时释放信号量 + 取消订阅（AtomicBoolean 防重复）
        Runnable cleanup = () -> {
            if (released.compareAndSet(false, true)) {
                activeStreams.decrementAndGet();
                sseLimiter.release();
                Disposable d = subRef.get();
                if (d != null && !d.isDisposed()) {
                    d.dispose();
                }
            }
        };
        emitter.onCompletion(cleanup::run);
        emitter.onTimeout(cleanup::run);
        emitter.onError(e -> {
            log.warn("SSE [{}] emitter onError: {}", tag, e.getMessage());
            cleanup.run();
        });

        // 3. 异步订阅：虚拟线程池执行，请求线程立即返回 emitter
        sseExecutor.execute(() -> {
            try {
                flux
                        // LLM 超时保护：5 分钟无响应自动 onError，释放虚拟线程与连接
                        .timeout(ThreadPoolsConfig.LLM_TIMEOUT)
                        .onBackpressureBuffer(ThreadPoolsConfig.BACKPRESSURE_BUFFER)
                        .doOnSubscribe(s -> log.info("SSE [{}] Flux 订阅开始，当前并发={}", tag, concurrency))
                        .doOnNext(c -> {
                            if (firstSent.compareAndSet(false, true)) {
                                log.info("SSE [{}] 首字节 TTFB={}ms", tag, System.currentTimeMillis() - start);
                            }
                        })
                        .doOnComplete(() -> {
                            log.info("SSE [{}] 完成，总耗时={}ms", tag, System.currentTimeMillis() - start);
                            emitter.complete();
                        })
                        .doOnError(e -> {
                            log.error("SSE [{}] 异常，总耗时={}ms err={}", tag, System.currentTimeMillis() - start, e.getMessage());
                            emitter.completeWithError(e);
                        })
                        .doFinally(sig -> {
                            log.info("SSE [{}] Flux 终止 signal={} 总耗时={}ms", tag, sig, System.currentTimeMillis() - start);
                        })
                        .subscribe(
                                chunk -> {
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .data(objectMapper.writeValueAsString(chunk)));
                                    } catch (Exception e) {
                                        log.error("SSE [{}] send 失败，总耗时={}ms err={}", tag, System.currentTimeMillis() - start, e.getMessage());
                                        emitter.completeWithError(e);
                                    }
                                },
                                emitter::completeWithError
                        );
            } catch (Exception e) {
                log.error("SSE [{}] 订阅编排异常，总耗时={}ms", tag, System.currentTimeMillis() - start, e);
                emitter.completeWithError(e);
                cleanup.run();
            }
        });

        return emitter;
    }

    /** slug 截断 + 防空，避免日志里出现 null 或超长 slug */
    private String safeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return "slug=?";
        }
        return "slug=" + (slug.length() > 32 ? slug.substring(0, 32) + "..." : slug);
    }
}
