package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.UploadLogic;
import cn.yzfy.crushcupidserver.model.vo.UploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件上传（需登录态）。
 * <p>
 * 图片经第三方对象存储（阿里云 OSS）持久化，未启用/失败自动回退本地磁盘，
 * 返回可访问 URL；头像、素材等场景复用同一通道。
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadLogic uploadLogic;

    /** 上传单张图片，返回 { url } */
    @PostMapping("/image")
    public Result<UploadVO> image(@RequestParam("file") MultipartFile file) {
        return Result.ok(uploadLogic.uploadImage(file, "upload"));
    }
}
