#!/data/data/com.termux/files/usr/bin/bash

set -e

PROJECT="$HOME/ApoMaeKae"
JAVA="$PROJECT/app/src/main/java"
LATEST_VERSION="9.1.0"

echo "=========================================="
echo " APO MAE KAE - KEY COUNTDOWN + VERSION LOCK"
echo "=========================================="
echo "Latest App Version : $LATEST_VERSION"
echo

MAIN=$(find "$JAVA" -type f -name "MainActivity.java" | head -n 1)
LICENSE=$(find "$JAVA" -type f -name "LicenseClient.java" | head -n 1)

if [ -z "$MAIN" ]; then
    echo "❌ ไม่พบ MainActivity.java"
    find "$JAVA" -type f -name "*.java" | head -50
    exit 1
fi

if [ -z "$LICENSE" ]; then
    echo "❌ ไม่พบ LicenseClient.java"
    find "$JAVA" -type f -name "*.java" | head -50
    exit 1
fi

echo "MainActivity : $MAIN"
echo "LicenseClient : $LICENSE"
echo

mkdir -p "$JAVA/com/apomaekae/license"

cat > "$JAVA/com/apomaekae/license/KeyInfo.java" <<'EOF'
package com.apomaekae.license;

public class KeyInfo {

    public final boolean valid;
    public final boolean versionValid;
    public final String key;
    public final String expires;
    public final String message;

    public KeyInfo(
            boolean valid,
            boolean versionValid,
            String key,
            String expires,
            String message
    ) {
        this.valid = valid;
        this.versionValid = versionValid;
        this.key = key;
        this.expires = expires;
        this.message = message;
    }
}
EOF

cat > "$JAVA/com/apomaekae/license/KeyCountdown.java" <<'EOF'
package com.apomaekae.license;

import android.os.CountDownTimer;
import android.widget.TextView;

public class KeyCountdown {

    private CountDownTimer timer;

    public void start(
            TextView view,
            long expiresMillis
    ) {

        if (timer != null) {
            timer.cancel();
        }

        long remain = expiresMillis - System.currentTimeMillis();

        if (remain <= 0) {
            view.setText("🔑 KEY หมดอายุแล้ว");
            return;
        }

        timer = new CountDownTimer(remain, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                long totalSeconds = millisUntilFinished / 1000;

                long days = totalSeconds / 86400;
                long hours = (totalSeconds % 86400) / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;

                view.setText(
                        "🔑 KEY หมดอายุใน " +
                        days + " วัน " +
                        hours + " ชม. " +
                        minutes + " นาที " +
                        seconds + " วินาที"
                );
            }

            @Override
            public void onFinish() {
                view.setText("🔑 KEY หมดอายุแล้ว");
            }

        }.start();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
EOF

echo
echo "=========================================="
echo "✅ เตรียมระบบ KEY สำเร็จ"
echo "=========================================="
echo "เวอร์ชันล่าสุด : $LATEST_VERSION"
echo "MainActivity   : $MAIN"
echo "LicenseClient  : $LICENSE"
echo
echo "ขั้นต่อไปให้ Build APK"
