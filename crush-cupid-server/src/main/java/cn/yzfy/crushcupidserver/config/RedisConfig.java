package cn.yzfy.crushcupidserver.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import jakarta.annotation.PostConstruct;

/**
 * Sa-Token 会话落 Redis。
 * <p>引入 sa-token-redis-jackson 后其自带自动装配会无条件接管 SaTokenDao，本地无 Redis 时会导致会话读写失败；
 * 故在 application.yml 中排除其自动装配，改由本类在 {@code crush.redis.enabled=true} 时才装配 Redis 版 SaTokenDao。
 * 这样关闭 Redis（默认）时保持原内存会话，打开时登录态跨实例共享。
 */
@Configuration
@ConditionalOnProperty(name = "crush.redis.enabled", havingValue = "true")
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    private final RedisConnectionFactory connectionFactory;

    public RedisConfig(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    public void initSaTokenRedisDao() {
        SaTokenDaoRedisJackson dao = new SaTokenDaoRedisJackson();
        dao.init(connectionFactory);
        SaManager.setSaTokenDao(dao);
        log.info("[Redis] Sa-Token 会话已切换到 Redis 存储");
    }
}
