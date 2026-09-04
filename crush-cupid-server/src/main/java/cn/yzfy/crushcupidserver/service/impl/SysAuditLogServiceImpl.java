package cn.yzfy.crushcupidserver.service.impl;

import cn.yzfy.crushcupidserver.mapper.SysAuditLogMapper;
import cn.yzfy.crushcupidserver.model.entity.SysAuditLog;
import cn.yzfy.crushcupidserver.service.SysAuditLogService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 审计日志 Service 实现
 */
@Service
public class SysAuditLogServiceImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog>
        implements SysAuditLogService {

}
