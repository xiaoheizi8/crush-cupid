package cn.yzfy.crushcupidserver.security;

import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import cn.yzfy.crushcupidserver.service.CrushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 多租户归属校验（OwnershipGuard）：确保当前登录用户只能访问本人名下的 crush 及子资源。
 * <p>
 * 规则（严格隔离）：
 * <ul>
 *   <li>crush 归属当前用户：正常访问</li>
 *   <li>其余（含历史系统共享/演示数据 user_id=0）：非本人资源 → 403（越权）</li>
 * </ul>
 * 每个用户只能看到、聊天、修改自己创建的 crush。
 *
 * @author 一朝风月
 * @code guard
 * @createTime 2026-08-28
 */
@Component
@RequiredArgsConstructor
public class OwnershipGuard {

    private final CrushService crushService;

    /**
     * 校验 crushId 归属当前登录用户；失败抛 403。返回该 crush（已加载）。
     */
    public Crush requireOwnership(Long crushId) {
        long uid = SecurityUtils.currentUserId();
        Crush crush = crushService.getById(crushId);
        if (crush == null) {
            throw BizException.notFound("未找到暗恋对象 id=" + crushId);
        }
        if (!belongsTo(crush, uid)) {
            throw BizException.forbidden("无权访问该暗恋对象");
        }
        return crush;
    }

    /**
     * 归属校验（返回 boolean，不抛异常）。用于需要先判断再走批量/内部流程的场景。
     */
    public boolean isOwner(Long crushId) {
        try {
            requireOwnership(crushId);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 「写」访问归属校验（按 slug）：仅允许本人名下 crush，返回该 crush。
     * <p>
     * 用于 chat/proactive/voice 等会写 conversation 的入口；
     * 这些入口在请求线程被调用（同步），Sa-Token 上下文可用，因此可在 reactive 管道之前完成归属校验。
     */
    public Crush requireWriteBySlug(String slug) {
        return requireSlug(slug);
    }

    /** 「读」访问归属校验（按 slug）：同样仅允许本人名下 crush，返回该 crush。 */
    public Crush requireReadBySlug(String slug) {
        return requireSlug(slug);
    }

    private Crush requireSlug(String slug) {
        long uid = SecurityUtils.currentUserId();
        Crush crush = crushService.getBySlug(slug);
        if (crush == null) {
            throw BizException.notFound("未找到暗恋对象：" + slug);
        }
        if (!belongsTo(crush, uid)) {
            throw BizException.forbidden("无权访问该暗恋对象");
        }
        return crush;
    }

    /** crush 是否归属指定用户（严格本人，user_id 为空或不匹配均视为无权） */
    private boolean belongsTo(Crush crush, long uid) {
        Long owner = crush.getUserId();
        return owner != null && owner == uid;
    }
}
