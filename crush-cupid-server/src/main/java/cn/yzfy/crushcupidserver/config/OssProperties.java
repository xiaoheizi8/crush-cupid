package cn.yzfy.crushcupidserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @className OssProperties
 * @description 阿里云 OSS 静态资源存储配置：启用后对话图片上传到 OSS 并返回完整公网 URL，
 * 未启用或配置不完整时回退本地磁盘落盘（见 {@code ImageStorageService}）。
 * 由 {@code CrushCupidServerApplication} 通过 {@code @EnableConfigurationProperties} 注册。
 * @author 一朝风月
 * @code config
 * @createTime 2026-09-17
 */
@Data
@ConfigurationProperties(prefix = "crush.oss")
public class OssProperties {

    /** 是否启用 OSS（默认 false：回退本地磁盘） */
    private boolean enabled = false;

    /** 地域节点，如 oss-cn-hangzhou.aliyuncs.com */
    private String endpoint;

    /** Bucket 名称（需配置公共读或使用自定义域名） */
    private String bucket;

    /** AccessKey ID（建议环境变量注入，勿提交真实值） */
    private String accessKeyId;

    /** AccessKey Secret（建议环境变量注入，勿提交真实值） */
    private String accessKeySecret;

    /** 自定义访问域名（如 CDN/备案域名），空则自动拼 https://{bucket}.{endpoint} */
    private String publicUrl;
}