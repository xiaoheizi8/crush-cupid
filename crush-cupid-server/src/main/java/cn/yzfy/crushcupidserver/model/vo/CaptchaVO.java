package cn.yzfy.crushcupidserver.model.vo;

import lombok.Data;

/**
 * 图形验证码视图：captchaId 用于提交时携带，image 为 data URI（可直接用作 &lt;img src&gt;）。
 */
@Data
public class CaptchaVO {

    /** 本次验证码实例 ID（提交登录时回传） */
    private String captchaId;

    /** 验证码图片 data URI（"data:image/png;base64,..."） */
    private String image;
}