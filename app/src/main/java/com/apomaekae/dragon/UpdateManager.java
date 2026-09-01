package com.apomaekae.dragon;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdateManager {

    private static final String UPDATE_JSON =
            "https://raw.githubusercontent.com/rpoling123/ApoMaeKae/main/update.json";

    private UpdateManager() {}

    public static void check(Activity activity) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                connection =
                        (HttpURLConnection)
                                new URL(UPDATE_JSON).openConnection();

                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestMethod("GET");

                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream(),
                                        "UTF-8"
                                )
                        );

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                br.close();

                JSONObject json =
                        new JSONObject(sb.toString());

                final int latestCode =
                        json.optInt("versionCode", 0);

                final String latestName =
                        json.optString("versionName", "");

                final String apkUrl =
                        json.optString("apkUrl", "");

                final boolean force =
                        json.optBoolean("forceUpdate", false);

                final String note =
                        json.optString(
                                "note",
                                "มีเวอร์ชันใหม่"
                        );

                final int currentCode =
                        getCurrentVersionCode(activity);

                final String currentName =
                        getCurrentVersionName(activity);

                /*
                 * สำคัญมาก
                 *
                 * ถ้าเวอร์ชันปัจจุบันเท่ากับ
                 * หรือใหม่กว่าเวอร์ชันบน Server
                 *
                 * จะไม่แสดงหน้าดาวน์โหลด
                 */
                if (latestCode <= 0 ||
                        latestCode <= currentCode) {

                    return;
                }

                /*
                 * ทำเป็น final ก่อนเข้า Lambda
                 * ป้องกัน error:
                 *
                 * local variables referenced from
                 * a lambda expression must be final
                 * or effectively final
                 */
                final String displayCurrent =
                        currentName.isEmpty()
                                ? String.valueOf(currentCode)
                                : currentName;

                activity.runOnUiThread(() ->
                        showUpdate(
                                activity,
                                displayCurrent,
                                latestName,
                                apkUrl,
                                force,
                                note
                        )
                );

            } catch (Exception ignored) {

                // ไม่มี Internet / Server ไม่ตอบ
                // ไม่รบกวนการใช้งานแอป

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private static int getCurrentVersionCode(
            Activity activity
    ) {

        try {

            PackageInfo info =
                    activity
                            .getPackageManager()
                            .getPackageInfo(
                                    activity.getPackageName(),
                                    0
                            );

            if (Build.VERSION.SDK_INT >= 28) {

                return (int)
                        info.getLongVersionCode();
            }

            return info.versionCode;

        } catch (Exception e) {

            return 0;
        }
    }

    private static String getCurrentVersionName(
            Activity activity
    ) {

        try {

            PackageInfo info =
                    activity
                            .getPackageManager()
                            .getPackageInfo(
                                    activity.getPackageName(),
                                    0
                            );

            return info.versionName == null
                    ? ""
                    : info.versionName;

        } catch (Exception e) {

            return "";
        }
    }

    private static void showUpdate(
            Activity activity,
            String currentVersion,
            String latestVersion,
            String apkUrl,
            boolean force,
            String note
    ) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(activity);

        builder.setTitle(
                "📲 มีเวอร์ชันใหม่"
        );

        builder.setMessage(
                "เวอร์ชันปัจจุบัน: "
                        + currentVersion
                        + "\n"
                        + "เวอร์ชันล่าสุด: "
                        + latestVersion
                        + "\n\n"
                        + note
                        + "\n\n"
                        + "กรุณาอัปเดตเป็นเวอร์ชันล่าสุด"
        );

        builder.setPositiveButton(
                "⬇️ ดาวน์โหลด APK",
                (dialog, which) ->
                        openApk(
                                activity,
                                apkUrl
                        )
        );

        if (!force) {

            builder.setNegativeButton(
                    "ไว้ก่อน",
                    null
            );

        } else {

            builder.setCancelable(false);
        }

        builder.show();
    }

    private static void openApk(
            Activity activity,
            String apkUrl
    ) {

        if (apkUrl == null ||
                apkUrl.trim().isEmpty()) {

            Toast.makeText(
                    activity,
                    "ยังไม่มีลิงก์ APK เวอร์ชันล่าสุด",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(apkUrl)
                    );

            activity.startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    activity,
                    "เปิดลิงก์ APK ไม่ได้",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
