package cn.yzfy.crushcupidserver.model.converter;

import cn.yzfy.crushcupidserver.model.dto.CrushCreateDTO;
import cn.yzfy.crushcupidserver.model.dto.CrushUpdateDTO;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import cn.yzfy.crushcupidserver.model.vo.CrushVO;
import org.springframework.beans.BeanUtils;

import java.util.Date;

/**
 * Crush 实体 / DTO / VO 单向映射。
 */
public final class CrushConverter {

    private CrushConverter() {
    }

    public static CrushVO toVO(Crush entity) {
        if (entity == null) {
            return null;
        }
        CrushVO vo = new CrushVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public static Crush toEntity(CrushCreateDTO dto) {
        Crush c = new Crush();
        BeanUtils.copyProperties(dto, c);
        c.setCurrentStage(1);
        c.setStatus("DRAFT");
        c.setTotalMessages(0);
        c.setVersion(1);
        c.setCreatedAt(new Date());
        c.setUpdatedAt(new Date());
        return c;
    }

    public static void update(Crush entity, CrushUpdateDTO dto) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getMbti() != null) entity.setMbti(dto.getMbti());
        if (dto.getZodiac() != null) entity.setZodiac(dto.getZodiac());
        if (dto.getOccupation() != null) entity.setOccupation(dto.getOccupation());
        if (dto.getGender() != null) entity.setGender(dto.getGender());
        if (dto.getKnowDuration() != null) entity.setKnowDuration(dto.getKnowDuration());
        if (dto.getRelationshipStatus() != null) entity.setRelationshipStatus(dto.getRelationshipStatus());
        if (dto.getImpression() != null) entity.setImpression(dto.getImpression());
        if (dto.getVoiceId() != null) entity.setVoiceId(dto.getVoiceId());
        if (dto.getProactiveEnabled() != null) entity.setProactiveEnabled(dto.getProactiveEnabled());
        entity.setUpdatedAt(new Date());
    }
}
