#!/data/data/com.termux/files/usr/bin/bash
set -e

cd ~/ApoMaeKae

echo "======================================"
echo " APO MAE KAE - FIX BUILDCONFIG"
echo "======================================"

FILE="app/src/main/java/com/apomaekae/license/LicenseClient.java"

if [ ! -f "$FILE" ]; then
    echo "❌ ไม่พบ $FILE"
    exit 1
fi

cp "$FILE" "${FILE}.bak_$(date +%Y%m%d_%H%M%S)"

python3 <<'PY'
from pathlib import Path
import re

p = Path("app/src/main/java/com/apomaekae/license/LicenseClient.java")
s = p.read_text(encoding="utf-8")

# ลบการอ้าง BuildConfig.VERSION_NAME
s = s.replace(
    'BuildConfig.VERSION_NAME',
    'getAppVersion(context)'
)

# เพิ่มเมธอดอ่าน version จาก APK
if 'private static String getAppVersion(Context context)' not in s:

    pos = s.rfind('\n    public static final class Result')

    method = r'''
    private static String getAppVersion(Context context) {
        try {
            android.content.pm.PackageManager pm =
                    context.getPackageManager();

            android.content.pm.PackageInfo info =
                    pm.getPackageInfo(
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
'''

    if pos >= 0:
        s = s[:pos] + method + s[pos:]
    else:
        s += method

p.write_text(s, encoding="utf-8")

print("✅ แก้ BuildConfig แล้ว")
PY

echo ""
echo "===== ตรวจสอบ ====="

grep -n "BuildConfig" "$FILE" || true
grep -n "getAppVersion" "$FILE" | head -20

echo ""
echo "======================================"
echo "🔨 BUILD"
echo "======================================"

chmod +x ./gradlew

./gradlew clean assembleDebug --no-daemon

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "❌ ไม่พบ APK"
    exit 1
fi

mkdir -p "$HOME/storage/downloads"

VERSION=$(sed -n \
's/.*versionName[[:space:]]*["'\'']\([^"'\'']*\)["'\''].*/\1/p' \
app/build.gradle | head -1)

VERSION=${VERSION:-9.2.3}

OUT="$HOME/storage/downloads/APO_MAE_KAE-${VERSION}-KEY-LOCK.apk"

cp -f "$APK" "$OUT"

echo ""
echo "======================================"
echo "📦 BUILD สำเร็จ"
echo "======================================"

ls -lh "$OUT"

echo ""
echo "======================================"
echo "☁️ GIT UPDATE"
echo "======================================"

git add app/src/main/java/com/apomaekae/license/LicenseClient.java

git commit -m "Fix LicenseClient version detection for KEY lock" || true

git push origin main

echo ""
echo "======================================"
echo "✅ เสร็จทั้งหมด"
echo "======================================"
echo "📱 VERSION : $VERSION"
echo "🔐 KEY     : VERSION ล่าสุด"
echo "⏳ EXPIRY  : Countdown"
echo "☁️ GIT     : PUSH แล้ว"
echo "📦 APK     : $OUT"
echo "======================================"
