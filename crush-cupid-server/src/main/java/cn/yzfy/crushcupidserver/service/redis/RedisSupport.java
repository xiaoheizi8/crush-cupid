package cn.yzfy.crushcupidserver.service.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 能力薄封装（验证码缓存/频控、登录失败锁定、通用限流）。
 * <p>所有读写在调用前必须校验 {@link #isEnabled()}；关闭时调用方回退到原有 PostgreSQL/内存实现，
 * 本组件不抛异常、不连接 Redis（Lettuce 惰性连接），因此未启用时对本地无 Redis 环境零副作用。
 */
@Component
public class RedisSupport {

    private static final Logger log = LoggerFactory.getLogger(RedisSupport.class);

    private final StringRedisTemplate redis;
    private final boolean enabled;

    public RedisSupport(StringRedisTemplate redis,
                        @Value("${crush.redis.enabled:false}") boolean enabled) {
        this.redis = redis;
        this.enabled = enabled;
    }

    /** Redis 总开关 */
    public boolean isEnabled() {
        return enabled;
    }

    // ============ 邮箱验证码 ============

    private String codeKey(String email, String purpose) {
        return "auth:code:" + email + ":" + purpose;
    }

    private String codeSentKey(String email, String purpose) {
        return "auth:code:sent:" + email + ":" + purpose;
    }

    private String codeAttemptKey(String email, String purpose) {
        return "auth:code:attempt:" + email + ":" + purpose;
    }

    /** 存验证码 hash，ttlSeconds 秒后自动过期 */
    public void emailStoreCode(String email, String purpose, String codeHash, long ttlSeconds) {
        redis.opsForValue().set(codeKey(email, purpose), codeHash, Duration.ofSeconds(ttlSeconds));
    }

    /** 读当前验证码 hash（无/已过期返回 null） */
    public String emailGetCodeHash(String email, String purpose) {
        return redis.opsForValue().get(codeKey(email, purpose));
    }

    /** 校验失败累计次数 +1，并顺延过期时间 */
    public int emailIncrementAttempt(String email, String purpose, long ttlSeconds) {
        Long c = redis.opsForValue().increment(codeAttemptKey(email, purpose));
        if (c != null && c == 1L) {
            redis.expire(codeAttemptKey(email, purpose), Duration.ofSeconds(ttlSeconds));
        }
        return c == null ? 0 : c.intValue();
    }

    /** 作废/清除该邮箱+用途的验证码与尝试计数 */
    public void emailClear(String email, String purpose) {
        redis.delete(codeKey(email, purpose));
        redis.delete(codeAttemptKey(email, purpose));
    }

    /** 记录“刚发过一次码”，resendSeconds 内禁止重发 */
    public void emailMarkSent(String email, String purpose, long resendSeconds) {
        redis.opsForValue().set(codeSentKey(email, purpose), "1", Duration.ofSeconds(resendSeconds));
    }

    /** resendSeconds 窗口内是否已发过验证码 */
    public boolean emailRecentSent(String email, String purpose) {
        return Boolean.TRUE.equals(redis.hasKey(codeSentKey(email, purpose)));
    }

    // ============ 登录失败 / 锁定 ============

    private String loginFailedKey(String email) {
        return "auth:login:failed:" + email;
    }

    private String loginLockKey(String email) {
        return "auth:login:lock:" + email;
    }

    /** 登录失败计数 +1，返回最新失败次数（首个失败时设置窗口过期） */
    public int loginIncrementFailed(String email, long windowSeconds) {
        Long c = redis.opsForValue().increment(loginFailedKey(email));
        if (c != null && c == 1L) {
            redis.expire(loginFailedKey(email), Duration.ofSeconds(windowSeconds));
        }
        return c == null ? 0 : c.intValue();
    }

    /** 达到阈值后写锁定标志，lockSeconds 后自动解锁（期间登录一律拒绝） */
    public void loginLock(String email, long lockSeconds) {
        redis.opsForValue().set(loginLockKey(email), "1", Duration.ofSeconds(lockSeconds));
    }

    /** 是否处于锁定中 */
    public boolean loginIsLocked(String email) {
        return Boolean.TRUE.equals(redis.hasKey(loginLockKey(email)));
    }

    /** 登录成功：清除失败计数与锁定 */
    public void loginClear(String email) {
        redis.delete(loginFailedKey(email));
        redis.delete(loginLockKey(email));
    }

    // ============ 通用限流 ============

    /** 固定窗口限流：窗口 windowSeconds 秒内最多 limit 次，超限返回 false */
    public boolean tryAcquire(String key, long windowSeconds, long limit) {
        String k = "rate:" + key;
        Long c = redis.opsForValue().increment(k);
        if (c != null && c == 1L) {
            redis.expire(k, Duration.ofSeconds(windowSeconds));
        }
        boolean allowed = c != null && c <= limit;
        if (!allowed) {
            log.debug("[Redis] 限流命中 key={} count={} limit={}", key, c, limit);
        }
        return allowed;
    }
}
