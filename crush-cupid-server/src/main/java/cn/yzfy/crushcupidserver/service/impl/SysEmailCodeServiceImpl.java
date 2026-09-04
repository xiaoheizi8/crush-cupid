package cn.yzfy.crushcupidserver.service.impl;

import cn.yzfy.crushcupidserver.mapper.SysEmailCodeMapper;
import cn.yzfy.crushcupidserver.model.entity.SysEmailCode;
import cn.yzfy.crushcupidserver.service.SysEmailCodeService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 邮箱验证码数据访问 Service 实现（MyBatis-Plus 薄封装，不写业务逻辑）。
 */
@Service
public class SysEmailCodeServiceImpl extends ServiceImpl<SysEmailCodeMapper, SysEmailCode>
        implements SysEmailCodeService {

    @Override
    public SysEmailCode getLatestUnused(String email, String purpose) {
        if (StrUtil.isBlank(email) || StrUtil.isBlank(purpose)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysEmailCode>()
                .eq(SysEmailCode::getEmail, email.trim().toLowerCase())
                .eq(SysEmailCode::getPurpose, purpose)
                .eq(SysEmailCode::getUsed, false)
                .orderByDesc(SysEmailCode::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    public boolean recentSentWithin(String email, String purpose, int seconds) {
        if (StrUtil.isBlank(email) || StrUtil.isBlank(purpose) || seconds <= 0) {
            return false;
        }
        Date since = new Date(System.currentTimeMillis() - seconds * 1000L);
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysEmailCode>()
                .eq(SysEmailCode::getEmail, email.trim().toLowerCase())
                .eq(SysEmailCode::getPurpose, purpose)
                .ge(SysEmailCode::getCreatedAt, since));
        return count != null && count > 0;
    }
}
