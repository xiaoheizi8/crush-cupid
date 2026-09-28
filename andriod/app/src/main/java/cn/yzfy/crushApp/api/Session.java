package cn.yzfy.crushApp.api;

/** 登录态失效统一处理：清除 token 并通知界面跳回登录页。 */
public final class Session {

    /** 登录失效监听：由持有导航上下文的界面（MainActivity）注册。 */
    public interface Listener {
        void onAuthExpired();
    }

    private static Listener listener;

    private Session() {
    }

    public static void setListener(Listener l) {
        listener = l;
    }

    /** 登录态是否已失效（HTTP 状态码 401，或响应体 code=401）。 */
    public static boolean isUnauthorized(int code) {
        return code == 401;
    }

    /**
     * 处理登录失效：清除本地 token 并跳回登录页。
     * 以 token 是否存在作为防重入依据：首个 401 清掉 token 后，后续并发 401 直接跳过
     * （所有回调均在主线程串行执行，无竞态），避免重复导航。
     */
    public static void handleUnauthorized() {
        if (!AuthApi.isLoggedIn()) {
            return;
        }
        AuthApi.clearToken();
        if (listener != null) {
            listener.onAuthExpired();
        }
    }
}
