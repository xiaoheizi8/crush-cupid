package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.MetaLogic;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 元数据接口：当前用户的角色/权限枚举（供后台前端渲染菜单、按钮权限）。
 */
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaLogic metaLogic;

    /** 当前用户权限码 + 全量权限码 */
    @GetMapping("/permissions")
    public Result<Map<String, Object>> permissions() {
        return Result.ok(metaLogic.permissions());
    }

    /** 当前用户角色码 + 全量角色 */
    @GetMapping("/roles")
    public Result<Map<String, Object>> roles() {
        return Result.ok(metaLogic.roles());
    }
}
