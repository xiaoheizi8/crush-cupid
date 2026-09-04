package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.AuthLogic;
import cn.yzfy.crushcupidserver.model.dto.ChangePasswordDTO;
import cn.yzfy.crushcupidserver.model.dto.EmailCodeDTO;
import cn.yzfy.crushcupidserver.model.dto.LoginDTO;
import cn.yzfy.crushcupidserver.model.dto.RegisterDTO;
import cn.yzfy.crushcupidserver.model.dto.ResetPasswordDTO;
import cn.yzfy.crushcupidserver.model.vo.LoginVO;
import cn.yzfy.crushcupidserver.model.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（薄控制器：仅参数绑定，业务逻辑在 {@link AuthLogic}）。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthLogic authLogic;

    /** 发送邮箱验证码 */
    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@RequestBody EmailCodeDTO dto) {
        authLogic.sendEmailCode(dto);
        return Result.ok();
    }

    /** 邮箱注册（注册即登录） */
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody RegisterDTO dto) {
        return Result.ok(authLogic.register(dto));
    }

    /** 邮箱登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.ok(authLogic.login(dto));
    }

    /** 登出 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authLogic.logout();
        return Result.ok();
    }

    /** 当前登录用户 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authLogic.me());
    }

    /** 修改密码（登录后） */
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
        authLogic.changePassword(dto);
        return Result.ok();
    }

    /** 重置密码（验证码方式，未登录可用） */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordDTO dto) {
        authLogic.resetPassword(dto);
        return Result.ok();
    }
}