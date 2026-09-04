package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.ChatHistoryLogic;
import cn.yzfy.crushcupidserver.model.vo.ChatHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 对话历史查询接口（薄控制器：仅参数绑定，业务逻辑在 {@link ChatHistoryLogic}）。
 */
@RestController
@RequestMapping("/api/chat/history")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryLogic chatHistoryLogic;

    /**
     * 按 crushSlug 拉历史消息列表，同时关联 chat_media 回填图片 URL。
     */
    @GetMapping
    public Result<List<ChatHistoryVO>> history(@RequestParam String crushSlug) {
        return Result.ok(chatHistoryLogic.history(crushSlug));
    }
}