package cn.yzfy.crushcupidserver.agent;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.config.ChatModelRegistry;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.util.List;

/**
 * @className ImageUnderstandingService
 * @description 图片视觉理解：用声明了 vision 能力的供应商（qwen-vl / qwen-native 等）直传图片，
 * 让模型直接「看懂」图片并输出文字描述，替代 OCR 提取。
 * <p>
 * 上传图片后仍会交给 {@link SourceAnalysisService#analyze} 做结构化提炼（facts/portraitClues 等），
 * 故本服务只负责把图片翻译成一段可靠的文字原文。
 * <p>
 * 降级策略：无可用视觉供应商 / 调用失败 / 返回为空时返回 null，由调用方回退 OCR 或文本解析，
 * 绝不阻断上传主流程。
 * @author 一朝风月
 * @code service
 * @createTime 2026-09-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUnderstandingService {

    private final ChatModelRegistry chatModelRegistry;

    /**
     * 视觉理解图片为文字。
     *
     * @param imageBytes 图片字节
     * @param mime       图片 MIME（如 image/jpeg）
     * @param crush      所属暗恋对象（可选，用于提示语带上下文）
     * @return 理解出的文字描述；不可用时返回 null
     */
    public String understand(byte[] imageBytes, String mime, Crush crush) {
        String provider = chatModelRegistry.firstVision();
        if (provider == null) {
            log.warn("无可用视觉供应商，图片理解跳过（回退 OCR/文本解析）");
            return null;
        }
        ChatModel model = chatModelRegistry.get(provider);
        Media media = Media.builder()
                .mimeType(MimeType.valueOf(mime == null ? "image/png" : mime))
                .data((Object) imageBytes)
                .build();
        String text = "你是暗恋对象的资料理解助手。请详细描述这张图片的内容，"
                + "并尽可能逐字提取图片中的文字（如聊天记录、朋友圈、便签、文档截图等）。"
                + "重点突出与暗恋对象相关的事实线索（时间/事件/人物关系/性格喜好）。"
                + "不要编造图片里没有的信息。\n"
                + "暗恋对象：" + (crush != null && StrUtil.isNotBlank(crush.getName()) ? crush.getName() : "（未知）");
        UserMessage user = UserMessage.builder().text(text).media(List.of(media)).build();
        try {
            var response = model.call(new Prompt(List.of(user)));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("视觉模型返回为空，图片理解回退");
                return null;
            }
            String content = response.getResult().getOutput().getText();
            return StrUtil.isBlank(content) ? null : content.trim();
        } catch (Exception e) {
            log.warn("视觉模型理解图片失败（回退 OCR/文本解析）：{}", e.getMessage());
            return null;
        }
    }
}
