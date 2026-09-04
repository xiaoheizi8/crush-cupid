package cn.yzfy.crushcupidserver.model.converter;

import cn.yzfy.crushcupidserver.model.entity.SysUser;
import cn.yzfy.crushcupidserver.model.vo.UserVO;
import org.springframework.beans.BeanUtils;

/**
 * 用户实体 / VO 映射。
 */
public final class UserConverter {

    private UserConverter() {
    }

    public static UserVO toVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
