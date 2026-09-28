package cn.yzfy.crushApp.model;

/** 我的配额（GET /api/user/quota） */
public class MyQuota {
    public String plan;
    public Integer crushLimit;
    public Integer dailyChatLimit;
    public Integer todayMessageCount;
    public Integer crushCount;

    public int crushLimitOr(int def) {
        return crushLimit == null ? def : crushLimit;
    }

    public int dailyChatLimitOr(int def) {
        return dailyChatLimit == null ? def : dailyChatLimit;
    }

    public int todayCountOr() {
        return todayMessageCount == null ? 0 : todayMessageCount;
    }

    public int crushCountOr() {
        return crushCount == null ? 0 : crushCount;
    }

    public String planOr() {
        return plan == null || plan.isEmpty() ? "FREE" : plan;
    }
}
