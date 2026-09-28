package cn.yzfy.crushcupidserver.logic;

import cn.yzfy.crushcupidserver.agent.ImageStorageService;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.vo.UploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

/**
 * 文件上传业务逻辑：校验图片类型与大小后，交给 {@link ImageStorageService} 持久化。
 * <p>
 * 存储优先第三方对象存储（阿里云 OSS），未启用/失败时回退服务端本地磁盘，
 * 返回可访问 URL 供前端直接渲染。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadLogic {

    /** 单张图片大小上限：5MB（与前端提示保持一致） */
    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024L;

    private final ImageStorageService imageStorageService;

    /**
     * 上传图片。
     *
     * @param file   上传的图片文件
     * @param module 业务模块前缀（如 avatar / upload），决定对象键与落盘子目录
     * @return 可访问 URL
     */
    public UploadVO uploadImage(MultipartFile file, String module) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw BizException.badRequest("仅支持图片文件");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw BizException.badRequest("图片不能超过 5MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.warn("读取上传文件失败：{}", e.getMessage());
            throw new BizException("读取文件失败，请稍后再试");
        }
        return new UploadVO(imageStorageService.storeImage(bytes, contentType, module));
    }
}
