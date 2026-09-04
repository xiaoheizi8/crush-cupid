package cn.yzfy.crushcupidserver.logic;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.NumberUtil;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.converter.UserConverter;
import cn.yzfy.crushcupidserver.model.dto.ChangePasswordDTO;
import cn.yzfy.crushcupidserver.model.dto.EmailCodeDTO;
import cn.yzfy.crushcupidserver.model.dto.LoginDTO;
import cn.yzfy.crushcupidserver.model.dto.RegisterDTO;
import cn.yzfy.crushcupidserver.model.dto.ResetPasswordDTO;
import cn.yzfy.crushcupidserver.model.entity.SysEmailCode;
import cn.yzfy.crushcupidserver.model.entity.SysUser;
import cn.yzfy.crushcupidserver.model.enums.EmailCodePurpose;
import cn.yzfy.crushcupidserver.model.vo.LoginVO;
import cn.yzfy.crushcupidserver.model.vo.UserVO;
import cn.yzfy.crushcupidserver.security.CryptoHelper;
import cn.yzfy.crushcupidserver.security.EmailSender;
import cn.yzfy.crushcupidserver.service.SysEmailCodeService;
import cn.yzfy.crushcupidserver.service.SysRbacService;
import cn.yzfy.crushcupidserver.service.SysUserService;
import cn.yzfy.crushcupidserver.service.redis.RedisSupport;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * 认证与用户业务逻辑层：邮箱验证码、注册、登录、登出、改密、重置密码、当前用户。
 * 数据访问委托 MP 薄 Service；Sa-Token 会话由 {@link StpUtil} 管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLogic {

    /** 校验码最多失败次数 */
    private static final int CODE_MAX_ATTEMPT = 5;
    /** 登录失败锁定次数 */
    private static final int LOGIN_MAX_FAILED = 5;
    /** 登录锁定时长（毫秒） */
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000L;
    /** 密码强度：8~64 位，含大小写字母与数字 */
    private static final Pattern PASSWORD_REGEX =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$");
    /** 邮箱格式 */
    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    /** 验证码有效期（毫秒） */
    private static final long CODE_TTL_MS = 10 * 60 * 1000L;
    /** Redis 启用时：单邮箱每小时发码上限（防短信/邮件轰炸） */
    private static final long EMAIL_CODE_PER_HOUR = 10;
    /** Redis 启用时：单邮箱+IP 每分钟登录尝试上限（防暴破） */
    private static final long LOGIN_PER_MINUTE = 10;
    /** Redis 启用时：登录失败计数窗口 = 锁定窗口 */
    private static final long LOGIN_FAIL_WINDOW_SEC = LOCK_DURATION_MS / 1000;

    private final SysUserService sysUserService;
    private final SysEmailCodeService sysEmailCodeService;
    private final SysRbacService sysRbacService;
    private final QuotaLogic quotaLogic;
    private final AuditLogic auditLogic;
    private final CryptoHelper cryptoHelper;
    private final EmailSender emailSender;
    private final RedisSupport redisSupport;

    /** 验证码重发间隔（秒），取自 crush.email-code.resend-interval-seconds */
    @Value("${crush.email-code.resend-interval-seconds:60}")
    private int resendIntervalSeconds;

    /** 发送邮箱验证码 */
    public void sendEmailCode(EmailCodeDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        String purpose = normalizePurpose(dto.getPurpose());

        if (EmailCodePurpose.REGISTER.name().equals(purpose) && sysUserService.getByEmail(email) != null) {
            throw BizException.authError(2, "邮箱已注册");
        }

        // Redis 启用：验证码与频控全部走 Redis（纯内存、带过期、防轰炸）
        if (redisSupport.isEnabled()) {
            String rateKey = "emailcode:" + email + ":" + purpose;
            if (redisSupport.emailRecentSent(email, purpose)) {
                throw BizException.authError(1, "操作太频繁，请 " + resendIntervalSeconds + " 秒后再试");
            }
            if (!redisSupport.tryAcquire(rateKey, 3600L, EMAIL_CODE_PER_HOUR)) {
                throw BizException.authError(1, "该邮箱发送过于频繁，请稍后再试");
            }
            String code = RandomUtil.randomNumbers(6);
            String hash = cryptoHelper.sha256(code);
            redisSupport.emailStoreCode(email, purpose, hash, CODE_TTL_MS / 1000);
            redisSupport.emailMarkSent(email, purpose, resendIntervalSeconds);
            emailSender.send(email, code, purpose);
            return;
        }

        // 未启用 Redis：走原 PostgreSQL 流程（DB 级 60s 重发节流）
        if (sysEmailCodeService.recentSentWithin(email, purpose, resendIntervalSeconds)) {
            throw BizException.authError(1, "操作太频繁，请 " + resendIntervalSeconds + " 秒后再试");
        }
        invalidateOldCodes(email, purpose);
        String code = RandomUtil.randomNumbers(6);
        SysEmailCode record = new SysEmailCode();
        record.setEmail(email);
        record.setPurpose(purpose);
        record.setCodeHash(cryptoHelper.sha256(code));
        record.setExpireAt(new Date(System.currentTimeMillis() + CODE_TTL_MS));
        record.setUsed(false);
        record.setAttempt(0);
        record.setCreatedAt(new Date());
        sysEmailCodeService.save(record);
        emailSender.send(email, code, purpose);
    }

    /** 注册：校验验证码 → 创建用户 → 自动登录 */
    public LoginVO register(RegisterDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        if (sysUserService.getByEmail(email) != null) {
            throw BizException.authError(2, "邮箱已注册");
        }
        validatePassword(dto.getPassword());
        verifyCode(email, EmailCodePurpose.REGISTER.name(), dto.getCode());

        this.invalidateOldCodes(email, EmailCodePurpose.REGISTER.name());
        SysUser user = new SysUser();
        user.setEmail(email);
        user.setUsername(email.substring(0, email.indexOf('@')));
        user.setPasswordHash(cryptoHelper.encodePassword(dto.getPassword()));
        user.setStatus(1);
        user.setEmailVerified(true);
        user.setFailedAttempt(0);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        try {
            sysUserService.save(user);
        } catch (DataIntegrityViolationException e) {
            // 并发注册撞唯一邮箱：视为已注册，避免 500
            throw BizException.authError(2, "邮箱已注册");
        }
        // 默认授予 ROLE_USER
        sysRbacService.assignRole(user.getId(),"ROLE_USER");
        // 建默认配额
        try {
            quotaLogic.getOrCreateQuota(user.getId());
        } catch (Exception e) {
            log.warn("注册创建默认配额失败: {}", e.getMessage());
        }
        // 登记登录成功后清除锁定
        user.setLastLoginAt(new Date());
        sysUserService.updateById(user);
        LoginVO loginVO = loginUser(user);
        auditLogic.success("auth", "REGISTER", "User", String.valueOf(user.getId()), null, 0);
        return loginVO;
    }

    /** 邮箱登录 */
    public LoginVO login(LoginDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        // Redis 启用：单邮箱+IP 每分钟限流，防暴破
        if (redisSupport.isEnabled()
                && !redisSupport.tryAcquire("login:" + email + ":" + resolveClientIp(), 60L, LOGIN_PER_MINUTE)) {
            throw BizException.authError(5, "登录过于频繁，请 1 分钟后再试");
        }
        SysUser user = sysUserService.getByEmail(email);
        if (user == null) {
            // 统一提示避免撞库
            throw BizException.authError(3, "邮箱或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw BizException.authError(6, "账号已被禁用");
        }
        checkLocked(user);
        if (!cryptoHelper.matchesPassword(dto.getPassword(), user.getPasswordHash())) {
            recordFailedAttempt(user);
            throw BizException.authError(3, "邮箱或密码错误");
        }
        // 验证码校验（若前端传入了验证码）
        if (StrUtil.isNotBlank(dto.getCode())) {
            verifyCode(email, EmailCodePurpose.LOGIN.name(), dto.getCode());
        }
        // 登录成功：清除失败计数与锁定
        if (redisSupport.isEnabled()) {
            redisSupport.loginClear(email);
        }
        user.setFailedAttempt(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(new Date());
        sysUserService.updateById(user);
        LoginVO loginVO = loginUser(user);
        auditLogic.success("auth", "LOGIN", "User", String.valueOf(user.getId()), null, 0);
        return loginVO;
    }

    /** 登出 */
    public void logout() {
        // 先留痕再登出：StpUtil.logout() 会清空登录态，导致审计拿不到 user_id/email
        if (StpUtil.isLogin()) {
            Long uid = StpUtil.getLoginIdAsLong();
            auditLogic.success("auth", "LOGOUT", "User", String.valueOf(uid), null, 0);
        }
        StpUtil.logout();
    }

    /** 当前登录用户信息 */
    public UserVO me() {
        long userId = requireLoginId();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        return UserConverter.toVO(user);
    }

    /** 修改密码（登录后） */
    public void changePassword(ChangePasswordDTO dto) {
        long userId = requireLoginId();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        if (StrUtil.isBlank(dto.getOldPassword())
                || !cryptoHelper.matchesPassword(dto.getOldPassword(), user.getPasswordHash())) {
            throw BizException.authError(3, "原密码错误");
        }
        validatePassword(dto.getNewPassword());
        user.setPasswordHash(cryptoHelper.encodePassword(dto.getNewPassword()));
        user.setUpdatedAt(new Date());
        sysUserService.updateById(user);
    }

    /** 重置密码（验证码方式，未登录可用） */
    public void resetPassword(ResetPasswordDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        SysUser user = sysUserService.getByEmail(email);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        verifyCode(email, EmailCodePurpose.RESET_PWD.name(), dto.getCode());
        this.invalidateOldCodes(email, EmailCodePurpose.RESET_PWD.name());
        validatePassword(dto.getNewPassword());
        user.setPasswordHash(cryptoHelper.encodePassword(dto.getNewPassword()));
        user.setFailedAttempt(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(new Date());
        sysUserService.updateById(user);
    }

    // ---------------- 内部辅助 ----------------

    /** 登录并构造 LoginVO */
    private LoginVO loginUser(SysUser user) {
        StpUtil.login(user.getId());
        LoginVO vo = new LoginVO();
        vo.setTokenName(StpUtil.getTokenName());
        vo.setTokenValue(StpUtil.getTokenValue());
        vo.setExpiresIn(StpUtil.getTokenTimeout());
        vo.setUser(UserConverter.toVO(user));
        return vo;
    }

    private long requireLoginId() {
        if (!StpUtil.isLogin()) {
            throw BizException.unauthorized("请先登录");
        }
        return StpUtil.getLoginIdAsLong();
    }

    /** 使用并校验验证码（成功则整条记录标记已用，失败累计） */
    private void verifyCode(String email, String purpose, String code) {
        if (StrUtil.isBlank(code) || !NumberUtil.isInteger(code) || code.length() != 6) {
            throw BizException.authError(3, "验证码错误");
        }
        // Redis 启用：从 Redis 取验证码校验
        if (redisSupport.isEnabled()) {
            String hash = redisSupport.emailGetCodeHash(email, purpose);
            if (StrUtil.isBlank(hash)) {
                throw BizException.authError(3, "验证码已过期，请重新获取");
            }
            if (!cryptoHelper.sha256(code).equals(hash)) {
                int attempt = redisSupport.emailIncrementAttempt(email, purpose, CODE_TTL_MS / 1000);
                if (attempt >= CODE_MAX_ATTEMPT) {
                    redisSupport.emailClear(email, purpose);
                }
                throw BizException.authError(3, "验证码错误");
            }
            redisSupport.emailClear(email, purpose);
            return;
        }
        SysEmailCode record = sysEmailCodeService.getLatestUnused(email, purpose);
        if (record == null || record.getExpireAt().before(new Date())) {
            throw BizException.authError(3, "验证码已过期，请重新获取");
        }
        if (!cryptoHelper.sha256(code).equals(record.getCodeHash())) {
            int attempt = (record.getAttempt() == null ? 0 : record.getAttempt()) + 1;
            record.setAttempt(attempt);
            sysEmailCodeService.updateById(record);
            if (attempt >= CODE_MAX_ATTEMPT) {
                record.setUsed(true);
                sysEmailCodeService.updateById(record);
            }
            throw BizException.authError(3, "验证码错误");
        }
        record.setUsed(true);
        sysEmailCodeService.updateById(record);
    }

    /** 作废该邮箱+用途的旧验证码 */
    private void invalidateOldCodes(String email, String purpose) {
        sysEmailCodeService.lambdaUpdate()
                .eq(SysEmailCode::getEmail, email)
                .eq(SysEmailCode::getPurpose, purpose)
                .eq(SysEmailCode::getUsed, false)
                .set(SysEmailCode::getUsed, true)
                .update();
    }

    /** 校验密码强度 */
    private void validatePassword(String password) {
        if (StrUtil.isBlank(password) || !PASSWORD_REGEX.matcher(password).matches()) {
            throw BizException.authError(4, "密码需 8~64 位，且包含大小写字母和数字");
        }
    }

    /** 规范化邮箱：trim + 小写 + 格式校验 */
    private String normalizeEmail(String email) {
        if (StrUtil.isBlank(email)) {
            throw BizException.authError(1, "邮箱不能为空");
        }
        String normalized = email.trim().toLowerCase();
        if (!EMAIL_REGEX.matcher(normalized).matches()) {
            throw BizException.authError(1, "邮箱格式不正确");
        }
        return normalized;
    }

    /** 规范化用途，仅允许 REGISTER / LOGIN / RESET_PWD */
    private String normalizePurpose(String purpose) {
        String p = StrUtil.blankToDefault(purpose, "").trim().toUpperCase();
        if (!EmailCodePurpose.REGISTER.name().equals(p)
                && !EmailCodePurpose.LOGIN.name().equals(p)
                && !EmailCodePurpose.RESET_PWD.name().equals(p)) {
            throw BizException.badRequest("用途只能是 REGISTER、LOGIN 或 RESET_PWD");
        }
        return p;
    }

    /** 当前请求客户端 IP（优先取 X-Forwarded-For 首段，用于登录限流） */
    private String resolveClientIp() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                String xff = req.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isBlank()) {
                    return xff.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    /** 登录前检查是否锁定 */
    private void checkLocked(SysUser user) {
        // Redis 启用：锁定状态存 Redis（自动过期解锁）
        if (redisSupport.isEnabled()) {
            if (redisSupport.loginIsLocked(user.getEmail())) {
                throw BizException.authError(5, "账号已锁定，请 " + (LOCK_DURATION_MS / 60000) + " 分钟后重试");
            }
            return;
        }
        Date lockedUntil = user.getLockedUntil();
        if (lockedUntil != null && lockedUntil.after(new Date())) {
            long minutes = (lockedUntil.getTime() - System.currentTimeMillis()) / 60000;
            throw BizException.authError(5, "账号已锁定，请 " + Math.max(1, minutes) + " 分钟后重试");
        }
        // 锁定已过期：重置计数
        if (lockedUntil != null && user.getFailedAttempt() != null && user.getFailedAttempt() > 0) {
            user.setFailedAttempt(0);
            user.setLockedUntil(null);
            sysUserService.updateById(user);
        }
    }

    /** 记录登录失败并达到阈值时锁定 */
    private void recordFailedAttempt(SysUser user) {
        // Redis 启用：失败计数与锁定存 Redis
        if (redisSupport.isEnabled()) {
            int attempt = redisSupport.loginIncrementFailed(user.getEmail(), LOGIN_FAIL_WINDOW_SEC);
            if (attempt >= LOGIN_MAX_FAILED) {
                redisSupport.loginLock(user.getEmail(), LOGIN_FAIL_WINDOW_SEC);
                log.warn("账号登录失败 {} 次被锁定（Redis），email={}", attempt, user.getEmail());
            }
            return;
        }
        int attempt = (user.getFailedAttempt() == null ? 0 : user.getFailedAttempt()) + 1;
        user.setFailedAttempt(attempt);
        if (attempt >= LOGIN_MAX_FAILED) {
            user.setLockedUntil(new Date(System.currentTimeMillis() + LOCK_DURATION_MS));
            log.warn("账号登录失败 {} 次被锁定，email={}", attempt, user.getEmail());
        }
        sysUserService.updateById(user);
    }
}