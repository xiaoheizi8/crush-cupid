package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.UserCenterLogic;
import cn.yzfy.crushcupidserver.model.dto.UpdateProfileDTO;
import cn.yzfy.crushcupidserver.model.vo.MyQuotaVO;
import cn.yzfy.crushcupidserver.model.vo.UserVO;
import cn.yzfy.crushcupidserver.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户中心（本人自助）：资料、配额、角色。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserCenterLogic userCenterLogic;

    /** 我的资料 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userCenterLogic.profile(SecurityUtils.currentUserId()));
    }

    /** 更新我的资料 */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody UpdateProfileDTO dto) {
        return Result.ok(userCenterLogic.updateProfile(SecurityUtils.currentUserId(), dto));
    }

    /** 我的配额与今日用量 */
    @GetMapping("/quota")
    public Result<MyQuotaVO> quota() {
        return Result.ok(userCenterLogic.myQuota(SecurityUtils.currentUserId()));
    }

    /** 我的角色码 */
    @GetMapping("/roles")
    public Result<List<String>> roles() {
        return Result.ok(userCenterLogic.myRoles(SecurityUtils.currentUserId()));
    }
}
