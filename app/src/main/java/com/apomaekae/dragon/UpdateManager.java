package com.apomaekae.dragon;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;

public class UpdateManager {

    /*
     * แก้ URL 2 จุดนี้เป็น URL ของระบบอัปเดตจริง
     */
    public static final String UPDATE_JSON =
        "https://raw.githubusercontent.com/rpoling123/ApoMaeKae/main/update.json";

    public static void check(Activity activity) {
        new Thread(() -> {
            try {
                HttpURLConnection c =
                    (HttpURLConnection)new URL(UPDATE_JSON).openConnection();

                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestMethod("GET");

                BufferedReader br = new BufferedReader(
                    new InputStreamReader(c.getInputStream(), "UTF-8"));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null)
                    sb.append(line);

                br.close();
                c.disconnect();

                JSONObject j = new JSONObject(sb.toString());

                int latestCode = j.optInt("versionCode", 93);
                String latestName = j.optString("versionName", "9.1");
                String apkUrl = j.optString("apkUrl", "");
                String minVersion = j.optString("minVersion", "9.1");
                boolean force = j.optBoolean("forceUpdate", false);
                String note = j.optString("note", "มีเวอร์ชันใหม่");

                int currentCode = 93;

                if (latestCode > currentCode) {
                    activity.runOnUiThread(() ->
                        showUpdate(activity, latestName, apkUrl,
                                   force, note)
                    );
                }

            } catch (Exception ignored) {
                // ไม่มีอินเทอร์เน็ต / server ไม่ตอบ
                // ให้แอปทำงานต่อได้ตามปกติ
            }
        }).start();
    }

    private static void showUpdate(
        Activity a,
        String version,
        String apkUrl,
        boolean force,
        String note
    ) {

        AlertDialog.Builder b = new AlertDialog.Builder(a);

        b.setTitle("📲 อัปเดตเวอร์ชันใหม่");

        b.setMessage(
            "มีเวอร์ชันใหม่ " + version +
            "\n\n" + note +
            "\n\nกรุณาอัปเดตเพื่อใช้งาน APO MAEKAE ต่อ"
        );

        b.setPositiveButton(
            "☁ ดาวน์โหลดและติดตั้ง",
            (d, w) -> {
                if (!apkUrl.isEmpty()) {
                    try {
                        a.startActivity(
                            new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(apkUrl)
                            )
                        );
                    } catch (Exception e) {
                        Toast.makeText(
                            a,
                            "ไม่สามารถเปิดลิงก์ APK ได้",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                }
            }
        );

        b.setNegativeButton(
            "⬆ ดาวน์โหลดผ่านเบราว์เซอร์",
            (d, w) -> {
                if (!apkUrl.isEmpty()) {
                    a.startActivity(
                        new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(apkUrl)
                        )
                    );
                }
            }
        );

        if (force) {
            b.setCancelable(false);
        } else {
            b.setCancelable(true);
        }

        b.show();
    }
}
