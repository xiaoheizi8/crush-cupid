package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.agent.proactive.ProactivePushService;
import cn.yzfy.crushcupidserver.security.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 主动消息推送接口。
 * <p>
 * 前端在对话页为当前查看的 crush 建立常驻 SSE（{@code GET /api/push/listen?crushSlug=xx}），
 * 后端调度器生成新的主动消息后，通过该连接把事件推给页面（前端收到后重新拉取历史渲染新气泡）。
 *
 * @author crush-cupid
 * @code controller
 * @createTime 2026-08-27
 */
@Slf4j
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class ProactiveController {

    private final ProactivePushService pushService;
    private final OwnershipGuard ownershipGuard;

    /**
     * 为指定 crush 建立常驻 SSE 监听连接。
     *
     * @param crushSlug 要接收主动消息的暗恋对象 slug
     */
    @GetMapping(value = "/listen", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter listen(@RequestParam String crushSlug) {
        ownershipGuard.requireReadBySlug(crushSlug);
        SseEmitter emitter = pushService.register(crushSlug);
        log.info("已注册主动消息监听 crush={}", crushSlug);
        return emitter;
    }
}
