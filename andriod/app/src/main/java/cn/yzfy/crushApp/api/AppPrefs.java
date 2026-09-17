package cn.yzfy.crushApp.api;

import android.content.Context;
import android.content.SharedPreferences;

/** 轻量 SharedPreferences 封装。 */
public final class AppPrefs {
    private static final String NAME = "crush_prefs";
    private static SharedPreferences prefs;

    private AppPrefs() {
    }

    public static void init(Context ctx) {
        prefs = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String getString(String key, String def) {
        return prefs.getString(key, def);
    }

    public static void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public static void remove(String key) {
        prefs.edit().remove(key).apply();
    }
}