package cn.yzfy.crushcupidserver.model.vo;

import lombok.Data;

/**
 * 后台概览（dashboard）统计
 */
@Data
public class OverviewVO {

    private long userCount;
    private long crushCount;
    private long todayChats;
    private long providerCount;
}
