package cn.yzfy.crushcupidserver.model.vo;

import lombok.Data;

/**
 * @className AiProviderVO
 * @description 自定义大模型供应商响应 VO（api_key 永不下发明文，仅返回脱敏掩码）
 * @author crush-cupid
 * @code vo
 * @createTime 2026-08-31
 */
@Data
public class AiProviderVO {

    private Long id;
    /** 归属用户 id；null=系统共享 */
    private Long userId;
    private String name;
    private String providerKey;
    private String baseUrl;
    /** API Key 脱敏掩码（如 sk****abcd），非明文 */
    private String apiKeyMask;
    /** 是否已配置 API Key */
    private Boolean hasApiKey;
    private String model;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    /** 能力列表：vision=视觉看图, audio=音频听语音 */
    private java.util.List<String> capabilities;
    private Boolean isDefault;
    private Integer status;
}
