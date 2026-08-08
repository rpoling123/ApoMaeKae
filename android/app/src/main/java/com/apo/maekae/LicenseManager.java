https://apomaekae-2.onrender.com/api/license/check

package com.apo.maekae;

import android.content.Context;
import android.provider.Settings;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class LicenseManager {
    // เปลี่ยนเป็น URL จริงของเซิร์ฟเวอร์ เช่น https://your-domain.com/api/license/check
    public static final String LICENSE_API_URL = "https://YOUR-SERVER.example.com/api/license/check";
    private static final String PREF = "license";
    private static final String KEY = "license_key";
    private static final String EXP = "expires_at";
    private static final String OFFSET = "server_offset";
    private final Context context;

    public LicenseManager(Context context) { this.context = context.getApplicationContext(); }

    public String getKey() { return context.getSharedPreferences(PREF, 0).getString(KEY, ""); }
    public void saveKey(String key) { context.getSharedPreferences(PREF, 0).edit().putString(KEY, key.trim()).apply(); }
    public long getExpiresAt() { return context.getSharedPreferences(PREF, 0).getLong(EXP, 0L); }
    public long nowServer() { return System.currentTimeMillis() + context.getSharedPreferences(PREF, 0).getLong(OFFSET, 0L); }

    public String deviceId() {
        String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return id == null ? "unknown" : id;
    }

    public Result check() throws Exception {
        String key = getKey();
        if (key.isEmpty()) return Result.invalid("ยังไม่ได้ใส่ Key");
        if (LICENSE_API_URL.contains("YOUR-SERVER")) return Result.invalid("ยังไม่ได้ตั้งค่า License Server");
        HttpURLConnection c = (HttpURLConnection) new URL(LICENSE_API_URL).openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(10000); c.setReadTimeout(10000);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8"); c.setDoOutput(true);
        JSONObject body = new JSONObject(); body.put("key", key); body.put("deviceId", deviceId());
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 400 ? c.getInputStream() : c.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (in != null) { try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { String s; while ((s=r.readLine())!=null) sb.append(s); } }
        JSONObject o = new JSONObject(sb.toString());
        long server = o.optLong("serverTime", System.currentTimeMillis());
        long expires = o.optLong("expiresAt", 0L);
        context.getSharedPreferences(PREF,0).edit().putLong(EXP, expires).putLong(OFFSET, server-System.currentTimeMillis()).apply();
        return new Result(o.optBoolean("active", false), o.optString("message", ""), expires, server);
    }

    public static final class Result {
        public final boolean active; public final String message; public final long expiresAt; public final long serverTime;
        Result(boolean a, String m, long e, long s) { active=a; message=m; expiresAt=e; serverTime=s; }
        static Result invalid(String m) { return new Result(false,m,0,0); }
    }
}
