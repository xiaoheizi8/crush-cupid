package cn.yzfy.crushcupidserver.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link PgChatMemoryRepository#parse(String)} 的回归测试。
 * <p>
 * parse 为私有方法且只依赖类常量、不访问注入服务，故构造器直接传 null，
 * 通过反射调用验证 conversationId 解析出的 (userId, crushId)。
 * <p>
 * 针对线上 bug：conversationId {@code "u4:crush:7"} 曾因分隔符用 "crush:" 切出带冒号的
 * userId 段（"4:"），Long.parseLong 抛 NumberFormatException 返回 null，导致 saveAll 跳过、
 * findByConversationId 返回空，记忆不入库、刷新后历史消失。
 */
class PgChatMemoryRepositoryParseTest {

    private final PgChatMemoryRepository repo = new PgChatMemoryRepository(null, null);

    @Test
    void parseMainFormat() throws Exception {
        // 主格式 "u{userId}:crush:{crushId}"，CupidAgent 实际构造，修复前返回 null
        assertKey("u4:crush:7", 4L, 7L);
        assertKey("u123:crush:456", 123L, 456L);
        assertKey("u0:crush:1", 0L, 1L);
    }

    @Test
    void parseCompatColonFormat() throws Exception {
        // 兼容带冒号旧写法 "u:{userId}:crush:{crushId}"
        assertKey("u:4:crush:7", 4L, 7L);
    }

    @Test
    void parseLegacyCrushOnly() throws Exception {
        // 旧格式 "crush:{crushId}"：归属共享桶 userId=0
        assertKey("crush:7", 0L, 7L);
    }

    @Test
    void parseNullReturnsNull() throws Exception {
        assertNull(parse(null));
    }

    @Test
    void parseFallbackExtractsNumbers() throws Exception {
        // 兜底正则：任何含数字、但不以 u/crush: 开头的变体，最后一个数字=crushId，倒数第二个=userId
        assertKey("foo:crush:7", 0L, 7L);
    }

    private void assertKey(String conversationId, long expectedUserId, long expectedCrushId) throws Exception {
        Object key = parse(conversationId);
        assertNotNull(key, "无法解析: " + conversationId);
        assertEquals(expectedUserId, userId(key), "userId 解析错误: " + conversationId);
        assertEquals(expectedCrushId, crushId(key), "crushId 解析错误: " + conversationId);
    }

    private Object parse(String conversationId) throws Exception {
        Method m = PgChatMemoryRepository.class.getDeclaredMethod("parse", String.class);
        m.setAccessible(true);
        return m.invoke(repo, conversationId);
    }

    private long userId(Object key) throws Exception {
        return (long) key.getClass().getMethod("userId").invoke(key);
    }

    private long crushId(Object key) throws Exception {
        return (long) key.getClass().getMethod("crushId").invoke(key);
    }
}
