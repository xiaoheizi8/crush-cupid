package cn.yzfy.crushApp.api;

/** 全局配置：后端口地址。模拟器用 10.0.2.2，真机改成电脑局域网 IP。 */
public final class Config {
    public static final String BASE_URL = "http://8.156.80.218:520";
    public static final int CONNECT_TIMEOUT_SECONDS = 15;
    public static final int READ_TIMEOUT_SECONDS = 120;

    private Config() {
    }
}