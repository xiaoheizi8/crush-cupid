package cn.yzfy.crushcupidserver.logic;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.converter.CrushConverter;
import cn.yzfy.crushcupidserver.model.dto.CrushCreateDTO;
import cn.yzfy.crushcupidserver.model.dto.CrushUpdateDTO;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import cn.yzfy.crushcupidserver.model.vo.CrushVO;
import cn.yzfy.crushcupidserver.security.OwnershipGuard;
import cn.yzfy.crushcupidserver.security.SecurityUtils;
import cn.yzfy.crushcupidserver.service.CrushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 暗恋对象业务逻辑层：校验、唯一性检查、实体/VO 组装。
 * 数据访问仅委托 {@link CrushService}（MyBatis-Plus 薄封装），本层不写 SQL。
 * <p>
 * 多租户隔离：列表仅返回当前用户自己创建的 crush；写操作全部归属当前用户；
 * 单查/改/删经 {@link OwnershipGuard} 校验归属（非本人返回 403）。
 */
@Service
@RequiredArgsConstructor
public class CrushLogic {

    private final CrushService crushService;
    private final OwnershipGuard ownershipGuard;
    private final QuotaLogic quotaLogic;
    private final AuditLogic auditLogic;

    public List<CrushVO> list() {
        long uid = SecurityUtils.currentUserId();
        return crushService.listOwnedBy(uid).stream().map(CrushConverter::toVO).toList();
    }

    public CrushVO get(Long id) {
        return CrushConverter.toVO(ownershipGuard.requireOwnership(id));
    }

    public CrushVO create(CrushCreateDTO dto) {
        if (StrUtil.isBlank(dto.getName())) {
            throw BizException.badRequest("name 不能为空");
        }
        if (StrUtil.isBlank(dto.getSlug())) {
            throw BizException.badRequest("slug 不能为空");
        }
        if (crushService.getBySlug(dto.getSlug()) != null) {
            throw BizException.badRequest("slug 已存在：" + dto.getSlug());
        }
        Crush crush = CrushConverter.toEntity(dto);
        long uid = SecurityUtils.currentUserId();
        crush.setUserId(uid);
        // 配额：crush 数量上限
        quotaLogic.checkCrushLimit(uid, crushService.countOwnedBy(uid));
        Date now = new Date();
        crush.setCreatedAt(now);
        crush.setUpdatedAt(now);
        crushService.save(crush);
        auditLogic.success("crush", "CREATE", "Crush", String.valueOf(crush.getId()), null, 0);
        return CrushConverter.toVO(crush);
    }

    public CrushVO update(Long id, CrushUpdateDTO dto) {
        Crush crush = ownershipGuard.requireOwnership(id);
        CrushConverter.update(crush, dto);
        crush.setUpdatedAt(new Date());
        crushService.updateById(crush);
        return CrushConverter.toVO(crush);
    }

    public void delete(Long id) {
        ownershipGuard.requireOwnership(id);
        crushService.removeById(id);
    }
}