package cn.yzfy.crushcupidserver.agent;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.config.OcrProperties;
import cn.yzfy.crushcupidserver.exception.BizException;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @className OcrService
 * @description 阿里云百炼 MCP OCR 服务：调用 Spring AI MCP Client autoconfig
 * （spring.ai.mcp.client.streamable-http.connections.*）注册的 {@link McpSyncClient}，
 * 走「通用OCR文字识别」工具把图片（base64 或 URL）识别为文字。
 * <p>
 * 降级策略：未配置 MCP 连接（客户端列表为空）或 enabled=false 时 {@link #available()} 返回 false，
 * 文件上传流程自动回退默认文本解析方式。
 * @author 一朝风月
 * @code service
 * @createTime 2026-08-27
 */
@Slf4j
@Service
@EnableConfigurationProperties(OcrProperties.class)
public class OcrService {

    private final OcrProperties props;
    private final List<McpSyncClient> mcpClients;

    /** 已成功 initialize 的客户端（initialized=false 时由这里惰性连接，避免启动期连网失败阻塞应用） */
    private final Set<McpSyncClient> connected = ConcurrentHashMap.newKeySet();

    public OcrService(OcrProperties props, List<McpSyncClient> mcpClients) {
        this.props = props;
        this.mcpClients = mcpClients;
        if (!mcpClients.isEmpty()) {
            log.info("MCP OCR 注册：{} 个 MCP 客户端，工具={}（首调时惰性连接）", mcpClients.size(), props.getToolName());
        }
    }

    /**
     * OCR 能力是否可用：总开关开 + 存在已注册的 MCP 客户端。
     * 注意：网络连通性在 recognize 时惰性判定，失败仅影响当次上传，不影响应用其他功能。
     */
    public boolean available() {
        return props.isEnabled() && !mcpClients.isEmpty();
    }

    /**
     * 识别图片文字。base64 与公网 URL 二选一传入。
     *
     * @param imageRef 图片二进制 base64 编码或公网可访问 URL
     * @return 识别出的全部文字（多段 TextContent 拼接）
     */
    public String recognize(String imageRef) {
        if (!available()) {
            throw BizException.badRequest("OCR 未配置：请在 spring.ai.mcp.client.streamable-http.connections 配置百炼 MCP 端点");
        }
        BizException lastError = null;
        for (McpSyncClient client : mcpClients) {
            if (!ensureConnected(client)) {
                continue;
            }
            try {
                McpSchema.CallToolResult result = client.callTool(
                        new McpSchema.CallToolRequest(props.getToolName(), Map.of("image", imageRef)));
                if (Boolean.TRUE.equals(result.isError())) {
                    throw new BizException("OCR 未识别到文字内容");
                }
                String text = extractText(result);
                if (StrUtil.isNotBlank(text)) {
                    return text;
                }
                throw new BizException("OCR 未识别到文字内容");
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                // 单个客户端连接失效时继续尝试下一个
                lastError = new BizException("OCR 识别失败，请稍后再试");
                log.warn("MCP OCR 调用失败，尝试下一个客户端：{}", e.getMessage());
            }
        }
        throw lastError != null ? lastError : new BizException("OCR 识别失败，请稍后再试");
    }

    /**
     * 识别本地图片字节数组（文件上传场景）。
     */
    public String recognize(byte[] imageBytes) {
        return recognize(java.util.Base64.getEncoder().encodeToString(imageBytes));
    }

    /**
     * 惰性连接：首调时 initialize（MCP 协议握手），成功后缓存。
     * 连接失败只打 warn 不抛异常，保证 OCR 失败不影响应用其他功能。
     */
    private boolean ensureConnected(McpSyncClient client) {
        if (connected.contains(client)) {
            return true;
        }
        try {
            McpSchema.InitializeResult result = client.initialize();
            connected.add(client);
            log.info("MCP OCR 连接成功：server={}", result.serverInfo() != null ? result.serverInfo().name() : "unknown");
            return true;
        } catch (Exception e) {
            log.warn("MCP OCR 连接失败（OCR 暂不可用，不影响其他功能）：{}", e.getMessage());
            return false;
        }
    }

    /** 拼接结果里所有 TextContent */
    private String extractText(McpSchema.CallToolResult result) {
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content c : result.content()) {
            if (c instanceof McpSchema.TextContent tc && StrUtil.isNotBlank(tc.text())) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(tc.text());
            }
        }
        return sb.toString().trim();
    }
}
