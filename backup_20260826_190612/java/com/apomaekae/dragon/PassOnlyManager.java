package com.apomaekae.dragon;

import android.content.Context;

/**
 * APO-local pass/decline statistics.
 * Does not modify LINE MAN server-side data.
 */
public final class PassOnlyManager {
    private static final String PREF = "apo_pass_only";

    private PassOnlyManager() {}

    public static boolean enabled(Context c) {
        return c.getSharedPreferences(PREF, 0).getBoolean("enabled", false);
    }

    public static void setEnabled(Context c, boolean value) {
        c.getSharedPreferences(PREF, 0).edit().putBoolean("enabled", value).apply();
    }

    public static int count(Context c) {
        return c.getSharedPreferences(PREF, 0).getInt("count", 0);
    }

    public static void record(Context c) {
        android.content.SharedPreferences p = c.getSharedPreferences(PREF, 0);
        p.edit().putInt("count", count(c) + 1).apply();
    }
}
