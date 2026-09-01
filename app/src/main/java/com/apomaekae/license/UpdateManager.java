package com.apomaekae.license;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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
                        cleanVersion(LicenseClient.getAppVersion(activity));

                ReleaseInfo latest = getLatestRelease();

                if (latest == null) {
                    return;
                }

                /*
                 * สำคัญ:
                 * Version ล่าสุดต้องมาจาก GitHub Release เท่านั้น
                 * ห้ามสร้าง Version เอง
                 */
                if (compareVersion(
                        currentVersion,
                        latest.version
                ) >= 0) {

                    // เป็นเวอร์ชันล่าสุดแล้ว
                    return;
                }

                /*
                 * แสดงดาวน์โหลดเฉพาะเมื่อ GitHub มี Version ใหม่จริง
                 * และมี APK asset จริง
                 */
                if (latest.apkUrl.isEmpty()) {
                    return;
                }

                activity.runOnUiThread(() -> {

                    new AlertDialog.Builder(activity)
                            .setTitle("📦 มีเวอร์ชันใหม่")
                            .setMessage(
                                    "เวอร์ชันปัจจุบัน : "
                                            + currentVersion
                                            + "\n\n"
                                            + "เวอร์ชันล่าสุด : "
                                            + latest.version
                                            + "\n\n"
                                            + "พบ APK จาก GitHub Release"
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
                                                            Uri.parse(
                                                                    latest.apkUrl
                                                            )
                                                    );

                                            activity.startActivity(
                                                    intent
                                            );

                                        } catch (Exception e) {

                                            Toast.makeText(
                                                    activity,
                                                    "เปิดดาวน์โหลดไม่สำเร็จ",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                            )
                            .show();
                });

            } catch (Exception ignored) {
                // ระบบ Update ห้ามทำให้แอปหลักล้ม
            }
        }).start();
    }

    private static ReleaseInfo getLatestRelease() {

        HttpURLConnection connection = null;

        try {

            URL url = new URL(GITHUB_API);

            connection =
                    (HttpURLConnection) url.openConnection();

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

            if (connection.getResponseCode() != 200) {
                return null;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream(),
                                    "UTF-8"
                            )
                    );

            StringBuilder json =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            reader.close();

            JSONObject release =
                    new JSONObject(json.toString());

            /*
             * ใช้ Version จาก GitHub Release จริง
             * เช่น v9.2.0
             */
            String latestVersion =
                    cleanVersion(
                            release.optString(
                                    "tag_name",
                                    ""
                            )
                    );

            if (latestVersion.isEmpty()) {
                return null;
            }

            JSONArray assets =
                    release.optJSONArray("assets");

            if (assets == null) {
                return null;
            }

            String apkUrl = "";

            for (int i = 0;
                 i < assets.length();
                 i++) {

                JSONObject asset =
                        assets.optJSONObject(i);

                if (asset == null) {
                    continue;
                }

                String name =
                        asset.optString(
                                "name",
                                ""
                        );

                String downloadUrl =
                        asset.optString(
                                "browser_download_url",
                                ""
                        );

                if (
                        name.toLowerCase()
                                .endsWith(".apk")
                        &&
                        !downloadUrl.isEmpty()
                ) {

                    apkUrl = downloadUrl;
                    break;
                }
            }

            if (apkUrl.isEmpty()) {
                return null;
            }

            return new ReleaseInfo(
                    latestVersion,
                    apkUrl
            );

        } catch (Exception ignored) {

            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String cleanVersion(
            String version
    ) {

        if (version == null) {
            return "";
        }

        return version
                .trim()
                .replaceFirst(
                        "^[vV]",
                        ""
                );
    }

    private static int compareVersion(
            String current,
            String latest
    ) {

        try {

            String[] a =
                    cleanVersion(current)
                            .split("\\.");

            String[] b =
                    cleanVersion(latest)
                            .split("\\.");

            int length =
                    Math.max(
                            a.length,
                            b.length
                    );

            for (int i = 0;
                 i < length;
                 i++) {

                int x =
                        i < a.length
                                ? Integer.parseInt(a[i])
                                : 0;

                int y =
                        i < b.length
                                ? Integer.parseInt(b[i])
                                : 0;

                if (x < y) {
                    return -1;
                }

                if (x > y) {
                    return 1;
                }
            }

            return 0;

        } catch (Exception ignored) {

            // อ่าน Version ไม่ได้
            // ไม่แสดง Update
            return 0;
        }
    }

    private static final class ReleaseInfo {

        final String version;
        final String apkUrl;

        ReleaseInfo(
                String version,
                String apkUrl
        ) {

            this.version = version;
            this.apkUrl = apkUrl;
        }
    }
}
