package cn.yzfy.crushcupidserver.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理员调整用户配额入参
 */
@Data
public class QuotaUpdateDTO {

    private String plan;
    private Integer crushLimit;
    private Integer dailyChatLimit;
    /** 允许的供应商 key 白名单（空=全部） */
    private List<String> modelAllowlist;
}
