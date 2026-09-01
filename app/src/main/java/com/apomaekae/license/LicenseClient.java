package com.apomaekae.license;

import android.content.Context;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class LicenseClient {

    private static final String API =
            "https://apomaekae-2.onrender.com/api/license/check";

    private LicenseClient() {}

    public static String getKey(Context context) {
        return context.getSharedPreferences(
                "license",
                Context.MODE_PRIVATE
        ).getString("key", "");
    }

    public static void saveKey(Context context, String key) {
        context.getSharedPreferences(
                "license",
                Context.MODE_PRIVATE
        ).edit()
        .putString("key", key == null ? "" : key.trim())
        .apply();
    }

    public static String getDeviceId(Context context) {
        try {
            String id = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            if (id == null || id.trim().isEmpty()) {
                return "UNKNOWN-DEVICE";
            }

            return id.trim();

        } catch (Exception e) {
            return "UNKNOWN-DEVICE";
        }
    }

    public static String getAppVersion(Context context) {
        try {
            android.content.pm.PackageInfo info =
                    context.getPackageManager()
                            .getPackageInfo(
                                    context.getPackageName(),
                                    0
                            );

            if (info.versionName != null) {
                return info.versionName;
            }

        } catch (Exception ignored) {
        }

        return "";
    }

    public static Result verify(Context context, String key) {

        if (key == null || key.trim().isEmpty()) {
            return new Result(
                    false,
                    "กรุณาใส่ KEY",
                    "",
                    "inactive"
            );
        }

        HttpURLConnection connection = null;

        try {

            String appVersion = getAppVersion(context);

            URL url = new URL(API);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(true);

            connection.setRequestProperty(
                    "User-Agent",
                    "APO-MAEKAE-" + appVersion
            );

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            JSONObject body = new JSONObject();

            body.put("key", key.trim());
            body.put(
                    "deviceId",
                    getDeviceId(context)
            );
            body.put(
                    "appVersion",
                    appVersion
            );

            byte[] data =
                    body.toString()
                            .getBytes(StandardCharsets.UTF_8);

            OutputStream os =
                    connection.getOutputStream();

            os.write(data);
            os.flush();
            os.close();

            int responseCode =
                    connection.getResponseCode();

            BufferedReader reader;

            if (responseCode >= 200 &&
                    responseCode < 400) {

                reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        );

            } else {

                reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getErrorStream(),
                                        StandardCharsets.UTF_8
                                )
                        );
            }

            StringBuilder response =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            JSONObject json =
                    new JSONObject(
                            response.toString()
                    );

            boolean active =
                    json.optBoolean(
                            "active",
                            false
                    ) ||
                    "active".equalsIgnoreCase(
                            json.optString(
                                    "status",
                                    ""
                            )
                    );

            String message =
                    json.optString(
                            "message",
                            active
                                    ? "OK"
                                    : "KEY ไม่ผ่าน"
                    );

            String expires =
                    json.optString(
                            "expiresAt",
                            json.optString(
                                    "expireAt",
                                    ""
                            )
                    );

            String status =
                    json.optString(
                            "status",
                            active
                                    ? "active"
                                    : "inactive"
                    );

            String returnedKey =
                    json.optString(
                            "key",
                            key.trim()
                    );

            /*
             * VERSION LOCK
             *
             * KEY ต้องตรงกับ Version ล่าสุด
             */

            String keyVersion =
                    json.optString(
                            "keyVersion",
                            ""
                    );

            String latestVersion =
                    json.optString(
                            "latestVersion",
                            ""
                    );

            /*
             * ถ้า server ส่ง latestVersion มา
             * ต้องใช้ version ล่าสุดเท่านั้น
             */

            if (active &&
                    !latestVersion.isEmpty() &&
                    !appVersion.equals(
                            latestVersion
                    )) {

                return new Result(
                        false,
                        "KEY ใช้ได้เฉพาะ Version ล่าสุด " +
                                latestVersion +
                                " กรุณาอัปเดตแอป",
                        expires,
                        "version_locked"
                );
            }

            /*
             * KEY VERSION ต้องตรงกับ APP VERSION
             */

            if (active &&
                    !keyVersion.isEmpty() &&
                    !appVersion.equals(
                            keyVersion
                    )) {

                return new Result(
                        false,
                        "KEY นี้เป็นของ Version " +
                                keyVersion +
                                " ไม่ใช่ Version " +
                                appVersion,
                        expires,
                        "version_locked"
                );
            }

            if (active) {

                context.getSharedPreferences(
                        "license",
                        Context.MODE_PRIVATE
                ).edit()

                .putString(
                        "key",
                        returnedKey
                                .trim()
                                .toUpperCase()
                )

                .putString(
                        "expires",
                        expires
                )

                .putString(
                        "status",
                        status
                )

                .putString(
                        "keyVersion",
                        keyVersion
                )

                .putString(
                        "latestVersion",
                        latestVersion
                )

                .putLong(
                        "serverTime",
                        json.optLong(
                                "serverTime",
                                System.currentTimeMillis()
                        )
                )

                .apply();

                return new Result(
                        true,
                        "LICENSE ACTIVE",
                        expires,
                        status
                );
            }

            return new Result(
                    false,
                    message,
                    expires,
                    status
            );

        } catch (Exception e) {

            return new Result(
                    false,
                    "เชื่อมต่อระบบ KEY ไม่สำเร็จ",
                    "",
                    "error"
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String getExpires(Context context) {

        return context
                .getSharedPreferences(
                        "license",
                        Context.MODE_PRIVATE
                )
                .getString(
                        "expires",
                        ""
                );
    }

    public static String getKeyVersion(Context context) {

        return context
                .getSharedPreferences(
                        "license",
                        Context.MODE_PRIVATE
                )
                .getString(
                        "keyVersion",
                        ""
                );
    }

    public static String getLatestVersion(Context context) {

        return context
                .getSharedPreferences(
                        "license",
                        Context.MODE_PRIVATE
                )
                .getString(
                        "latestVersion",
                        getAppVersion(context)
                );
    }

    public static long getServerTime(Context context) {

        return context
                .getSharedPreferences(
                        "license",
                        Context.MODE_PRIVATE
                )
                .getLong(
                        "serverTime",
                        System.currentTimeMillis()
                );
    }

    public static final class Result {

        public final boolean ok;
        public final String message;
        public final String expires;
        public final String status;

        public Result(
                boolean ok,
                String message,
                String expires,
                String status
        ) {
            this.ok = ok;
            this.message = message;
            this.expires = expires;
            this.status = status;
        }
    }
}
