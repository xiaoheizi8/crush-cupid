package cn.yzfy.crushcupidserver.service;

import cn.yzfy.crushcupidserver.model.entity.SysEmailCode;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 邮箱验证码数据访问 Service（MyBatis-Plus 薄封装，不写业务逻辑）。
 */
public interface SysEmailCodeService extends IService<SysEmailCode> {

    /**
     * 查询该邮箱+用途最近一条未使用验证码
     */
    SysEmailCode getLatestUnused(String email, String purpose);

    /**
     * 该邮箱+用途在过去 seconds 秒内是否已发过验证码（用于重发频控）
     */
    boolean recentSentWithin(String email, String purpose, int seconds);
}
