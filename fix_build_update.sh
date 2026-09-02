#!/data/data/com.termux/files/usr/bin/bash

set -e

echo "=========================================="
echo " APO MAE KAE"
echo " FIX BUILD + GITHUB LATEST VERSION"
echo "=========================================="

FILE="app/src/main/java/com/apomaekae/license/UpdateManager.java"

# BACKUP
cp "$FILE" "$FILE.bak_$(date +%Y%m%d_%H%M%S)"

cat > "$FILE" <<'JAVA'
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

                final String currentVersion =
                        cleanVersion(
                                LicenseClient.getAppVersion(activity)
                        );

                /*
                 * ดึง Version ล่าสุดจาก GitHub Release จริง
                 * ห้ามกำหนด Version เองในโค้ด
                 */
                ReleaseInfo release = getLatestRelease();

                if (release == null ||
                        release.version.isEmpty()) {

                    return;
                }

                final String latestVersion =
                        cleanVersion(release.version);

                final String apkUrl =
                        release.apkUrl;

                /*
                 * ถ้า APP เป็น Version ล่าสุดแล้ว
                 * ไม่ต้องแสดงปุ่มดาวน์โหลด
                 */
                if (compareVersion(
                        currentVersion,
                        latestVersion
                ) >= 0) {

                    return;
                }

                /*
                 * มี Version ใหม่
                 */
                activity.runOnUiThread(() -> {

                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(activity);

                    builder.setTitle("📦 มีเวอร์ชันใหม่");

                    builder.setMessage(
                            "เวอร์ชันปัจจุบัน : " +
                            currentVersion +
                            "\n\n" +
                            "เวอร์ชันล่าสุด : " +
                            latestVersion +
                            "\n\n" +
                            "กรุณาอัปเดตเพื่อใช้งานต่อ"
                    );

                    builder.setCancelable(false);

                    builder.setNegativeButton(
                            "ภายหลัง",
                            null
                    );

                    builder.setPositiveButton(
                            "ดาวน์โหลด",
                            (dialog, which) -> {

                                try {

                                    String url =
                                            apkUrl;

                                    if (url == null ||
                                            url.isEmpty()) {

                                        url =
                                            "https://github.com/rpoling123/ApoMaeKae/releases/latest";
                                    }

                                    Intent intent =
                                            new Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(url)
                                            );

                                    activity.startActivity(intent);

                                } catch (Exception e) {

                                    Toast.makeText(
                                            activity,
                                            "เปิดหน้าดาวน์โหลดไม่สำเร็จ",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );

                    builder.show();
                });

            } catch (Exception ignored) {
            }

        }).start();
    }

    private static ReleaseInfo getLatestRelease() {

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
                return null;
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
                    new JSONObject(
                            result.toString()
                    );

            /*
             * Version เอาจาก tag_name ของ GitHub Release
             */
            String version =
                    release.optString(
                            "tag_name",
                            ""
                    );

            JSONArray assets =
                    release.optJSONArray(
                            "assets"
                    );

            String apkUrl = "";

            if (assets != null) {

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

                    if (name
                            .toLowerCase()
                            .endsWith(".apk")
                            &&
                            !download.isEmpty()) {

                        apkUrl = download;
                        break;
                    }
                }
            }

            return new ReleaseInfo(
                    version,
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
                .replace("v", "")
                .replace("V", "");
    }

    private static int compareVersion(
            String a,
            String b
    ) {

        try {

            String[] aa =
                    cleanVersion(a)
                            .split("\\.");

            String[] bb =
                    cleanVersion(b)
                            .split("\\.");

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
cd ~/ApoMaeKae

cat > fix_build_update.sh <<'EOF'
#!/data/data/com.termux/files/usr/bin/bash

set -e

echo "=========================================="
echo " APO MAE KAE"
echo " FIX BUILD + GITHUB LATEST VERSION"
echo "=========================================="

FILE="app/src/main/java/com/apomaekae/license/UpdateManager.java"

# BACKUP
cp "$FILE" "$FILE.bak_$(date +%Y%m%d_%H%M%S)"

cat > "$FILE" <<'JAVA'
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

                final String currentVersion =
                        cleanVersion(
                                LicenseClient.getAppVersion(activity)
                        );

                /*
                 * ดึง Version ล่าสุดจาก GitHub Release จริง
                 * ห้ามกำหนด Version เองในโค้ด
                 */
                ReleaseInfo release = getLatestRelease();

                if (release == null ||
                        release.version.isEmpty()) {

                    return;
                }

                final String latestVersion =
                        cleanVersion(release.version);

                final String apkUrl =
                        release.apkUrl;

                /*
                 * ถ้า APP เป็น Version ล่าสุดแล้ว
                 * ไม่ต้องแสดงปุ่มดาวน์โหลด
                 */
                if (compareVersion(
                        currentVersion,
                        latestVersion
                ) >= 0) {

                    return;
                }

                /*
                 * มี Version ใหม่
                 */
                activity.runOnUiThread(() -> {

                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(activity);

                    builder.setTitle("📦 มีเวอร์ชันใหม่");

                    builder.setMessage(
                            "เวอร์ชันปัจจุบัน : " +
                            currentVersion +
                            "\n\n" +
                            "เวอร์ชันล่าสุด : " +
                            latestVersion +
                            "\n\n" +
                            "กรุณาอัปเดตเพื่อใช้งานต่อ"
                    );

                    builder.setCancelable(false);

                    builder.setNegativeButton(
                            "ภายหลัง",
                            null
                    );

                    builder.setPositiveButton(
                            "ดาวน์โหลด",
                            (dialog, which) -> {

                                try {

                                    String url =
                                            apkUrl;

                                    if (url == null ||
                                            url.isEmpty()) {

                                        url =
                                            "https://github.com/rpoling123/ApoMaeKae/releases/latest";
                                    }

                                    Intent intent =
                                            new Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(url)
                                            );

                                    activity.startActivity(intent);

                                } catch (Exception e) {

                                    Toast.makeText(
                                            activity,
                                            "เปิดหน้าดาวน์โหลดไม่สำเร็จ",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );

                    builder.show();
                });

            } catch (Exception ignored) {
            }

        }).start();
    }

    private static ReleaseInfo getLatestRelease() {

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
                return null;
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
                    new JSONObject(
                            result.toString()
                    );

            /*
             * Version เอาจาก tag_name ของ GitHub Release
             */
            String version =
                    release.optString(
                            "tag_name",
                            ""
                    );

            JSONArray assets =
                    release.optJSONArray(
                            "assets"
                    );

            String apkUrl = "";

            if (assets != null) {

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

                    if (name
                            .toLowerCase()
                            .endsWith(".apk")
                            &&
                            !download.isEmpty()) {

                        apkUrl = download;
                        break;
                    }
                }
            }

            return new ReleaseInfo(
                    version,
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
                .replace("v", "")
                .replace("V", "");
    }

    private static int compareVersion(
            String a,
            String b
    ) {

        try {

            String[] aa =
                    cleanVersion(a)
                            .split("\\.");

            String[] bb =
                    cleanVersion(b)
                            .split("\\.");

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

            return 0;

        } catch (Exception ignored) {

            return a.equals(b)
                    ? 0
                    : -1;
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
JAVA

echo ""
echo "=========================================="
echo " BUILD APK"
echo "=========================================="

chmod +x gradlew

./gradlew clean
./gradlew assembleDebug --no-daemon

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "❌ ไม่พบ APK"
    exit 1
fi

echo ""
echo "=========================================="
echo " COPY APK"
echo "=========================================="

mkdir -p ~/storage/downloads

cp "$APK" \
~/storage/downloads/ApoMaeKae-UPDATED.apk

echo ""
echo "=========================================="
echo " GIT UPDATE"
echo "=========================================="

git add app/src/main/java/com/apomaekae/license/UpdateManager.java

git commit -m "Fix latest GitHub release version check"

git push origin main

echo ""
echo "=========================================="
echo "✅ เสร็จทั้งหมด"
echo "=========================================="

echo "📦 APK:"
echo "~/storage/downloads/ApoMaeKae-UPDATED.apk"

echo ""
echo "🔐 KEY:"
echo "ใช้ระบบ LicenseClient เดิม"

echo ""
echo "🔄 VERSION:"
echo "อ่านจาก GitHub Release ล่าสุด"

echo ""
echo "⬇️ DOWNLOAD:"
echo "แสดงเฉพาะเมื่อ APP < GitHub Latest"

echo ""
echo "=========================================="
