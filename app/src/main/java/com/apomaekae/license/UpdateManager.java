package com.apomaekae.license;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdateManager {

    private static final String GITHUB_API =
            "https://api.github.com/repos/rpoling123/ApoMaeKae/releases/latest";

    private UpdateManager() {}

    public static void check(Activity activity) {

        new Thread(() -> {

            try {

                String currentVersion =
                        LicenseClient.getAppVersion(activity);

                String latestVersion =
                        LicenseClient.getLatestVersion(activity);

                /*
                 * ใช้ latestVersion จากระบบ KEY เป็นหลัก
                 */
                if (latestVersion == null ||
                        latestVersion.trim().isEmpty()) {

                    latestVersion = currentVersion;
                }

                latestVersion = cleanVersion(latestVersion);
                currentVersion = cleanVersion(currentVersion);

                /*
                 * ⭐ สำคัญ
                 *
                 * ถ้าเป็น Version ล่าสุดแล้ว
                 * ไม่ต้องแสดง Download
                 */
                if (compareVersion(
                        currentVersion,
                        latestVersion
                ) >= 0) {

                    return;
                }

                /*
                 * มี Version ใหม่
                 * หา APK จาก GitHub Release
                 */
                String apkUrl =
                        getLatestApkUrl();

                if (apkUrl == null ||
                        apkUrl.isEmpty()) {

                    apkUrl =
                            "https://github.com/rpoling123/ApoMaeKae/releases/latest";
                }

                String finalLatestVersion =
                        latestVersion;

                String finalApkUrl =
                        apkUrl;

                activity.runOnUiThread(() -> {

                    new AlertDialog.Builder(activity)

                            .setTitle("📦 มีเวอร์ชันใหม่")

                            .setMessage(
                                    "เวอร์ชันปัจจุบัน : " +
                                    currentVersion +
                                    "\n\n" +
                                    "เวอร์ชันล่าสุด : " +
                                    finalLatestVersion +
                                    "\n\n" +
                                    "กรุณาอัปเดตเพื่อใช้งานต่อ"
                            )

                            .setCancelable(false)

                            .setNegativeButton(
                                    "ภายหลัง",
                                    null
                            )

                            .setPositiveButton(
                                    "ดาวน์โหลด",
                                    (dialog, which) -> {

                                        try {

                                            Intent intent =
                                                    new Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(finalApkUrl)
                                                    );

                                            activity.startActivity(
                                                    intent
                                            );

                                        } catch (Exception e) {

                                            Toast.makeText(
                                                    activity,
                                                    "เปิดหน้าดาวน์โหลดไม่สำเร็จ",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                            )

                            .show();
                });

            } catch (Exception ignored) {
            }

        }).start();
    }

    private static String getLatestApkUrl() {

        HttpURLConnection connection = null;

        try {

            URL url =
                    new URL(GITHUB_API);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "APO-MAE-KAE"
            );

            int code =
                    connection.getResponseCode();

            if (code != 200) {
                return "";
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream(),
                                    "UTF-8"
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();

            JSONObject release =
                    new JSONObject(result.toString());

            JSONArray assets =
                    release.optJSONArray("assets");

            if (assets == null) {
                return "";
            }

            for (int i = 0;
                 i < assets.length();
                 i++) {

                JSONObject asset =
                        assets.getJSONObject(i);

                String name =
                        asset.optString(
                                "name",
                                ""
                        );

                String download =
                        asset.optString(
                                "browser_download_url",
                                ""
                        );

                if (name.toLowerCase()
                        .endsWith(".apk")
                        &&
                        !download.isEmpty()) {

                    return download;
                }
            }

        } catch (Exception ignored) {

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

        return "";
    }

    private static String cleanVersion(
            String version
    ) {

        if (version == null) {
            return "";
        }

        version =
                version.trim()
                        .replace("v", "")
                        .replace("V", "");

        return version;
    }

    private static int compareVersion(
            String a,
            String b
    ) {

        try {

            String[] aa =
                    cleanVersion(a).split("\\.");

            String[] bb =
                    cleanVersion(b).split("\\.");

            int length =
                    Math.max(
                            aa.length,
                            bb.length
                    );

            for (int i = 0;
                 i < length;
                 i++) {

                int x =
                        i < aa.length
                                ? Integer.parseInt(aa[i])
                                : 0;

                int y =
                        i < bb.length
                                ? Integer.parseInt(bb[i])
                                : 0;

                if (x < y) {
                    return -1;
                }

                if (x > y) {
                    return 1;
                }
            }

        } catch (Exception ignored) {
        }

        return a.equals(b) ? 0 : -1;
    }
}
