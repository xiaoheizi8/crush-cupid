package cn.yzfy.crushcupidserver.service;

import cn.yzfy.crushcupidserver.model.entity.Crush;
import com.baomidou.mybatisplus.spring.service.IService;

/**
* @author 27800
* @description 针对表【crush】的数据库操作Service
* @createDate 2026-08-25 22:03:03
*/
public interface CrushService extends IService<Crush> {

    /**
     * 根据 slug 查询暗恋对象
     */
    Crush getBySlug(String slug);

    /**
     * 查询某个用户可见的 crush 列表（本人私有 + 系统共享演示桶 0）。
     */
    java.util.List<Crush> listOwnedBy(long userId);

    /**
     * 某个用户私有 crush 的数量（配额校验用，不含系统共享桶）。
     */
    long countOwnedBy(long userId);
}
