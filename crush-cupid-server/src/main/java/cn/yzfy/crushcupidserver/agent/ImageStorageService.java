package cn.yzfy.crushcupidserver.agent;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.yzfy.crushcupidserver.config.OssStorageService;
import cn.yzfy.crushcupidserver.config.UploadProperties;
import cn.yzfy.crushcupidserver.model.dto.ChatMedia;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @className ImageStorageService
 * @description 对话图片持久化组件：把用户发送的图片（base64）持久化，生成可访问的 URL 返回。
 * <p>
 * 优先上传阿里云 OSS（返回完整公网 URL），OSS 未启用/未配置/上传失败时回退本地磁盘目录
 * （返回 /api/uploads/... 相对路径，由 WebMvcConfig 静态映射对外）。该 URL 标记会拼进用户
 * 消息文本入库，前端加载历史时据此回显图片——完整 URL 与相对路径两端（Web/安卓）均可直接渲染。
 * <p>
 * 仅处理 {@link ChatMedia#TYPE_IMAGE_BASE64}；{@link ChatMedia#TYPE_IMAGE_URL} 本身已是
 * 可访问 URL，原样返回即可。
 * @author 一朝风月
 * @code agent
 * @createTime 2026-08-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageStorageService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final UploadProperties uploadProperties;
    private final OssStorageService ossStorageService;

    /**
     * 启动时确保落盘目录存在，并打印实际绝对路径——便于排查「图片回显 404」类问题
     * （通常是 WebMvcConfig 映射的 file 路径与实际落盘路径不一致导致）。
     */
    @PostConstruct
    public void init() {
        File dir = new File(uploadProperties.getDir());
        if (!dir.exists()) {
            FileUtil.mkdir(dir);
        }
        log.info("图片上传落盘目录：{}（绝对路径={}）URL 前缀={}，OSS 可用={}",
                uploadProperties.getDir(),
                FileUtil.getAbsolutePath(dir),
                uploadProperties.getUrlPrefix(),
                ossStorageService.isAvailable());
    }

    /**
     * 持久化图片并返回可访问 URL。
     *
     * @return 可访问 URL（OSS 为完整公网 URL，本地为 /api/uploads/...，URL 形态图片原样返回）
     */
    public String storeImage(ChatMedia media) {
        if (ChatMedia.TYPE_IMAGE_URL.equals(media.getType())) {
            return media.getData();
        }
        if (!ChatMedia.TYPE_IMAGE_BASE64.equals(media.getType())) {
            throw new IllegalArgumentException("仅支持图片持久化，type=" + media.getType());
        }
        byte[] bytes = java.util.Base64.getDecoder().decode(media.getData());
        String ext = imageExt(media.getMimeType());
        String dateDir = LocalDate.now().format(DATE_DIR);
        String fileName = IdUtil.fastSimpleUUID() + ext;
        String relative = dateDir + "/" + fileName;
        String contentType = media.getMimeType() != null ? media.getMimeType() : "image/png";

        // 优先 OSS
        String ossUrl = ossStorageService.uploadImage(bytes, relative, contentType);
        if (ossUrl != null) {
            return ossUrl;
        }
        // 回退本地磁盘
        File target = FileUtil.file(uploadProperties.getDir(), relative);
        FileUtil.mkParentDirs(target);
        FileUtil.writeBytes(bytes, target);
        return slashUrl(uploadProperties.getUrlPrefix(), dateDir, fileName);
    }

    /** mimeType -> 文件扩展名（含点） */
    private String imageExt(String mimeType) {
        if (mimeType == null) {
            return ".png";
        }
        String lower = mimeType.toLowerCase();
        if (lower.contains("jpeg")) {
            return ".jpg";
        }
        if (lower.contains("gif")) {
            return ".gif";
        }
        if (lower.contains("webp")) {
            return ".webp";
        }
        if (lower.contains("bmp")) {
            return ".bmp";
        }
        return ".png";
    }

    /** 拼 URL，统一用正斜杠分隔 */
    private String slashUrl(String prefix, String dateDir, String fileName) {
        String p = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        return p + "/" + dateDir + "/" + fileName;
    }
}