package cn.yzfy.crushcupidserver.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果视图：返回可访问 URL。
 * <p>
 * OSS 启用时为完整公网 URL（https://{bucket}.{endpoint}/... 或自定义域名），
 * 回退本地磁盘时为相对路径 /api/uploads/...（由 WebMvcConfig 静态映射对外）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadVO {

    /** 可访问 URL */
    private String url;
}
