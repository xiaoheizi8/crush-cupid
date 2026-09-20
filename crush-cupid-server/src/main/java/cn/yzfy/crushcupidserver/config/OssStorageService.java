package cn.yzfy.crushcupidserver.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.config.OssProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * @className OssStorageService
 * @description 阿里云 OSS 图片存储组件：把对话图片字节上传到 OSS Bucket，返回完整公网 URL。
 * <p>
 * 上传失败或未配置时由调用方回退本地磁盘，本组件不以异常打断聊天流程。
 * URL 优先级：配置的 public-url（可挂 CDN/备案域名）> 自动拼 https://{bucket}.{endpoint}。
 * @author 一朝风月
 * @code config
 * @createTime 2026-09-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssStorageService {

    private final OssProperties ossProperties;

    private OSS ossClient;

    private boolean available = false;

    /** 启动时校验配置并初始化客户端（配置缺失时静默降级为不可用，不阻塞启动） */
    @PostConstruct
    public void init() {
        if (!ossProperties.isEnabled()) {
            log.info("阿里云 OSS 未启用（crush.oss.enabled=false），图片继续走本地磁盘");
            return;
        }
        if (StrUtil.hasBlank(ossProperties.getEndpoint(), ossProperties.getBucket(),
                ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret())) {
            log.warn("阿里云 OSS 配置不完整（endpoint/bucket/accessKeyId/accessKeySecret），图片回退本地磁盘");
            return;
        }
        try {
            this.ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(), ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
            this.available = true;
            log.info("阿里云 OSS 客户端初始化完成：bucket={} endpoint={} publicUrl={}",
                    ossProperties.getBucket(), ossProperties.getEndpoint(),
                    StrUtil.blankToDefault(ossProperties.getPublicUrl(), autoBaseUrl()));
        } catch (Exception e) {
            log.error("阿里云 OSS 客户端初始化失败，图片回退本地磁盘：{}", e.getMessage(), e);
        }
    }

    public boolean isAvailable() {
        return available && ossClient != null;
    }

    /**
     * 上传图片字节到 OSS。
     *
     * @return 完整公网 URL；上传失败返回 null（由调用方回退本地磁盘）
     */
    public String uploadImage(byte[] bytes, String objectKey, String contentType) {
        if (!isAvailable()) {
            return null;
        }
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(bytes.length);
            // 允许 long-running HTTP client 复用，bucket 需配置公共读
            ossClient.putObject(ossProperties.getBucket(), objectKey, new ByteArrayInputStream(bytes), metadata);
            log.info("图片已上传 OSS：key={}", objectKey);
            return buildUrl(objectKey);
        } catch (Exception e) {
            log.error("图片上传 OSS 失败，回退本地磁盘：key={} err={}", objectKey, e.getMessage(), e);
            return null;
        }
    }

    /** 拼接对外访问 URL：优先自定义域名，否则自动拼 https://{bucket}.{endpoint}/{key} */
    private String buildUrl(String objectKey) {
        String base = StrUtil.blankToDefault(ossProperties.getPublicUrl(), autoBaseUrl());
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return b + "/" + objectKey;
    }

    private String autoBaseUrl() {
        return "https://" + ossProperties.getBucket() + "." + ossProperties.getEndpoint();
    }
}