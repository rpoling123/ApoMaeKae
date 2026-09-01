#!/data/data/com.termux/files/usr/bin/bash
set -e

APP="$HOME/ApoMaeKae"
PKG="com.apomaekae"
JAVA="$APP/app/src/main/java/$PKG"

cd "$APP"

echo "=========================================="
echo " APO MAE KAE - KEY DATE/TIME UPDATE"
echo "=========================================="

# -----------------------------
# หา MainActivity
# -----------------------------
MAIN="$(find "$JAVA" -name MainActivity.java -type f | head -n 1)"

if [ -z "$MAIN" ]; then
    echo "❌ ไม่พบ MainActivity.java"
    exit 1
fi

echo "✅ MainActivity: $MAIN"

# -----------------------------
# Backup
# -----------------------------
STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP="$APP/.backup_key_datetime_$STAMP"

mkdir -p "$BACKUP"
cp -a app/src/main "$BACKUP/"

echo "✅ Backup: $BACKUP"

# -----------------------------
# สร้าง KeyExpiryOverlay.java
# -----------------------------
mkdir -p "$JAVA/license"

cat > "$JAVA/license/KeyExpiryOverlay.java" <<'JAVA'
package com.apomaekae.license;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apomaekae.license.LicenseClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class KeyExpiryOverlay {

    private static boolean installed = false;

    private KeyExpiryOverlay() {}

    public static void install(Application app) {

        if (installed) return;
        installed = true;

        app.registerActivityLifecycleCallbacks(
            new Application.ActivityLifecycleCallbacks() {

                @Override
                public void onActivityCreated(
                        Activity activity,
                        android.os.Bundle state) {

                    attach(activity);
                }

                @Override
                public void onActivityStarted(Activity activity) {}

                @Override
                public void onActivityResumed(Activity activity) {
                    attach(activity);
                }

                @Override
                public void onActivityPaused(Activity activity) {}

                @Override
                public void onActivityStopped(Activity activity) {}

                @Override
                public void onActivitySaveInstanceState(
                        Activity activity,
                        android.os.Bundle outState) {}

                @Override
                public void onActivityDestroyed(Activity activity) {}
            }
        );
    }

    private static void attach(final Activity activity) {

        if (activity == null) return;

        View decor = activity.getWindow().getDecorView();

        if (!(decor instanceof ViewGroup)) return;

        ViewGroup root = (ViewGroup) decor;

        if (root.findViewWithTag("APO_KEY_EXPIRY_BAR") != null) {
            return;
        }

        FrameLayout overlay = new FrameLayout(activity);

        overlay.setTag("APO_KEY_EXPIRY_BAR");

        FrameLayout.LayoutParams overlayParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        overlayParams.gravity =
                Gravity.TOP | Gravity.END;

        overlayParams.setMargins(
                0,
                dp(activity, 8),
                dp(activity, 8),
                0
        );

        LinearLayout box = new LinearLayout(activity);

        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.END);
        box.setPadding(
                dp(activity, 10),
                dp(activity, 7),
                dp(activity, 10),
                dp(activity, 7)
        );

        box.setBackgroundColor(Color.argb(
                235,
                20,
                25,
                30
        ));

        TextView date = new TextView(activity);

        date.setTag("APO_CURRENT_DATETIME");
        date.setTextSize(12);
        date.setTextColor(Color.WHITE);
        date.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView key = new TextView(activity);

        key.setTag("APO_KEY_EXPIRY");
        key.setTextSize(12);
        key.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        box.addView(date);
        box.addView(key);

        overlay.addView(box);

        root.addView(
                overlay,
                overlayParams
        );

        final Handler handler =
                new Handler(Looper.getMainLooper());

        final Runnable ticker = new Runnable() {

            @Override
            public void run() {

                if (activity.isFinishing()) {
                    return;
                }

                update(
                        activity,
                        date,
                        key
                );

                handler.postDelayed(this, 1000);
            }
        };

        overlay.setTag(
                "APO_KEY_EXPIRY_BAR"
        );

        overlay.post(ticker);
    }

    private static void update(
            Activity activity,
            TextView date,
            TextView key) {

        try {

            Date now = new Date();

            SimpleDateFormat df =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault()
                    );

            date.setText(
                    "📅 " + df.format(now)
            );

            String exp =
                    LicenseClient.getExpires(activity);

            if (exp == null ||
                    exp.trim().isEmpty()) {

                key.setText(
                        "🔑 KEY: ไม่พบวันหมดอายุ"
                );

                key.setTextColor(
                        Color.YELLOW
                );

                return;
            }

            Date expiry = parse(exp);

            if (expiry == null) {

                key.setText(
                        "🔑 KEY หมดอายุ: " + exp
                );

                key.setTextColor(
                        Color.YELLOW
                );

                return;
            }

            long diff =
                    expiry.getTime() -
                    System.currentTimeMillis();

            String expiryText =
                    df.format(expiry);

            if (diff <= 0) {

                key.setText(
                        "🔴 KEY หมดอายุ: " +
                        expiryText
                );

                key.setTextColor(
                        Color.RED
                );

            } else {

                key.setText(
                        "🟢 KEY หมดอายุ: " +
                        expiryText
                );

                key.setTextColor(
                        Color.rgb(
                                0,
                                255,
                                120
                        )
                );
            }

        } catch (Exception e) {

            key.setText(
                    "🔑 KEY: ตรวจสอบไม่ได้"
            );

            key.setTextColor(
                    Color.YELLOW
            );
        }
    }

    private static Date parse(String value) {

        String[] formats = {

                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "dd/MM/yyyy HH:mm:ss",
                "dd/MM/yyyy HH:mm",
                "dd/MM/yyyy"
        };

        for (String f : formats) {

            try {

                SimpleDateFormat sdf =
                        new SimpleDateFormat(
                                f,
                                Locale.getDefault()
                        );

                sdf.setLenient(false);

                return sdf.parse(value);

            } catch (ParseException ignored) {}
        }

        return null;
    }

    private static int dp(
            Context context,
            int value) {

        return (int)(
                value *
                context.getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
JAVA

echo "✅ สร้าง KeyExpiryOverlay.java แล้ว"

# -----------------------------
# แก้ MainActivity ให้ติดตั้งระบบ
# -----------------------------
python3 - "$MAIN" <<'PY'
from pathlib import Path
import sys

p = Path(sys.argv[1])
s = p.read_text(encoding="utf-8")

old = s

# import
if "import com.apomaekae.license.KeyExpiryOverlay;" not in s:
    lines = s.splitlines()

    pos = 0

    for i, line in enumerate(lines):
        if line.startswith("package "):
            pos = i + 1
            break

    lines.insert(
        pos,
        "import com.apomaekae.license.KeyExpiryOverlay;"
    )

    s = "\n".join(lines) + "\n"

# ติดตั้งหลัง super.onCreate
if "KeyExpiryOverlay.install(getApplication());" not in s:

    marker = "super.onCreate(savedInstanceState);"

    if marker in s:

        s = s.replace(
            marker,
            marker +
            "\n        KeyExpiryOverlay.install(getApplication());",
            1
        )

    else:

        # fallback
        marker2 = "protected void onCreate("

        idx = s.find(marker2)

        if idx >= 0:

            body = s.find("{", idx)

            if body >= 0:

                s = (
                    s[:body+1] +
                    "\n        KeyExpiryOverlay.install(getApplication());" +
                    s[body+1:]
                )

        else:

            raise SystemExit(
                "ไม่พบ onCreate() ใน MainActivity"
            )

if s == old:
    print("⚠️ MainActivity ไม่มีการเปลี่ยนแปลง")
else:
    p.write_text(
        s,
        encoding="utf-8"
    )

    print("✅ MainActivity เชื่อมระบบ KEY แล้ว")
PY

# -----------------------------
# ตรวจ
# -----------------------------
echo
echo "=== ตรวจจุดสำคัญ ==="

grep -R -n \
    "KeyExpiryOverlay.install" \
    "$MAIN" || true

grep -R -n \
    "KEY หมดอายุ" \
    "$JAVA/license/KeyExpiryOverlay.java" || true

# -----------------------------
# Build
# -----------------------------
echo
echo "=========================================="
echo " BUILD APK"
echo "=========================================="

chmod +x ./gradlew

./gradlew clean assembleDebug --no-daemon

# -----------------------------
# หา APK
# -----------------------------
APK="$APP/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then

    APK="$(find \
        "$APP/app/build/outputs/apk" \
        -type f \
        -name "*.apk" \
        | head -n 1)"

fi

if [ -z "$APK" ] || [ ! -f "$APK" ]; then

    echo "❌ Build ผ่านแต่ไม่พบ APK"
    exit 1

fi

# -----------------------------
# Copy Download
# -----------------------------
mkdir -p "$HOME/storage/downloads" 2>/dev/null || true

OUT="$HOME/storage/downloads/APO_MAEKAE_KEY_DATETIME.apk"

cp -f "$APK" "$OUT"

chmod 644 "$OUT"

echo
echo "=========================================="
echo "        ✅ BUILD สำเร็จ"
echo "=========================================="
echo
echo "📅 วัน/เวลาปัจจุบัน : ทุกหน้า"
echo "🔑 วันหมดอายุ KEY   : ทุกหน้า"
echo "⏱️ อัปเดต           : ทุก 1 วินาที"
echo "🟢 KEY ยังใช้งานได้"
echo "🔴 KEY หมดอายุแล้ว"
echo
echo "📦 APK:"
echo "$OUT"
echo
echo "💾 Backup:"
echo "$BACKUP"
echo
ls -lh "$OUT"
echo
echo "=========================================="
echo "เสร็จแล้ว"
echo "=========================================="
