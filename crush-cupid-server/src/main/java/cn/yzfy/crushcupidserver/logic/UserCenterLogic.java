package cn.yzfy.crushcupidserver.logic;

import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.converter.UserConverter;
import cn.yzfy.crushcupidserver.model.dto.UpdateProfileDTO;
import cn.yzfy.crushcupidserver.model.entity.SysQuota;
import cn.yzfy.crushcupidserver.model.entity.SysUser;
import cn.yzfy.crushcupidserver.model.vo.MyQuotaVO;
import cn.yzfy.crushcupidserver.model.vo.UserVO;
import cn.yzfy.crushcupidserver.service.CrushService;
import cn.yzfy.crushcupidserver.service.SysRbacService;
import cn.yzfy.crushcupidserver.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 用户中心业务逻辑：个人资料、配额查看、角色等（本人自助）。
 */
@Service
@RequiredArgsConstructor
public class UserCenterLogic {

    private final SysUserService sysUserService;
    private final QuotaLogic quotaLogic;
    private final CrushService crushService;
    private final SysRbacService sysRbacService;

    /** 当前用户资料 */
    public UserVO profile(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return UserConverter.toVO(user);
    }

    /** 更新个人资料（username/avatarUrl），null 不修改 */
    public UserVO updateProfile(Long userId, UpdateProfileDTO dto) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (dto.getUsername() != null) {
            if (dto.getUsername().isBlank()) {
                throw BizException.badRequest("username 不能为空");
            }
            if (dto.getUsername().length() > 50) {
                throw BizException.badRequest("username 最长 50 字符");
            }
            user.setUsername(dto.getUsername().trim());
        }
        if (dto.getAvatarUrl() != null) {
            if (dto.getAvatarUrl().length() > 500) {
                throw BizException.badRequest("avatarUrl 过长");
            }
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        user.setUpdatedAt(new Date());
        sysUserService.updateById(user);
        return UserConverter.toVO(user);
    }

    /** 我的配额与今日用量 */
    public MyQuotaVO myQuota(Long userId) {
        SysQuota quota = quotaLogic.getOrCreateQuota(userId);
        MyQuotaVO vo = new MyQuotaVO();
        vo.setPlan(quota.getPlan());
        vo.setCrushLimit(quota.getCrushLimit());
        vo.setDailyChatLimit(quota.getDailyChatLimit());
        vo.setTodayMessageCount(quotaLogic.todayMessages(userId));
        vo.setCrushCount(crushService.countOwnedBy(userId));
        return vo;
    }

    /** 我的角色码 */
    public List<String> myRoles(Long userId) {
        return sysRbacService.getRoleCodes(userId);
    }
}
