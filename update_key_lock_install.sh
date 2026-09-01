#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

cd "$(dirname "$0")"

REPO="rpoling123/ApoMaeKae"
PKG="app/build.gradle"
LICENSE="app/src/main/java/com/apomaekae/license/LicenseClient.java"
COUNTDOWN="app/src/main/java/com/apomaekae/license/KeyCountdown.java"

DOWNLOAD_DIR="$HOME/storage/downloads"

echo "=============================================="
echo " APO MAE KAE"
echo " KEY LOCK + COUNTDOWN + GITHUB APK"
echo "=============================================="

# -----------------------------
# VERSION
# -----------------------------
VERSION=$(sed -n \
's/.*versionName[[:space:]]*['"'"'"][^'"'"'"]*['"'"'"].*/\0/p' \
"$PKG" | grep -oE "[0-9]+\.[0-9]+\.[0-9]+" | head -n1)

if [ -z "$VERSION" ]; then
    echo "❌ หา VERSION ไม่เจอ"
    exit 1
fi

TAG="v$VERSION"

echo "📱 VERSION : $VERSION"
echo "🔐 KEY     : VERSION LOCK"
echo "⏳ COUNTDOWN: เปิดใช้งาน"
echo ""

# -----------------------------
# ตรวจ KEY SYSTEM
# -----------------------------
if ! grep -q "api/license/check" "$LICENSE"; then
    echo "❌ LicenseClient ไม่ได้เชื่อมระบบ KEY"
    exit 1
fi

if ! grep -q "keyVersion" "$LICENSE"; then
    echo "❌ ไม่มี keyVersion"
    exit 1
fi

if ! grep -q "latestVersion" "$LICENSE"; then
    echo "❌ ไม่มี latestVersion"
    exit 1
fi

if ! grep -q "CountDownTimer" "$COUNTDOWN"; then
    echo "❌ ไม่มีระบบ Countdown"
    exit 1
fi

echo "✅ KEY API เชื่อมระบบเดิม"
echo "✅ KEY VERSION LOCK"
echo "✅ KEY COUNTDOWN"
echo ""

# -----------------------------
# BUILD
# -----------------------------
echo "🧹 CLEAN..."
chmod +x ./gradlew
./gradlew clean

echo ""
echo "📦 BUILD APK..."
./gradlew assembleDebug --stacktrace

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "❌ ไม่พบ APK"
    exit 1
fi

# -----------------------------
# COPY DOWNLOADS
# -----------------------------
mkdir -p "$DOWNLOAD_DIR"

OUT="$DOWNLOAD_DIR/ApoMaeKae-$VERSION-KEY-LOCK.apk"

cp -f "$APK" "$OUT"

echo ""
echo "=============================================="
echo "📦 APK BUILD สำเร็จ"
echo "=============================================="

ls -lh "$OUT"

# -----------------------------
# INSTALL
# -----------------------------
echo ""
echo "📲 เปิดติดตั้ง APK..."

if command -v termux-open >/dev/null 2>&1; then
    termux-open "$OUT" || true
else
    echo "เปิดไฟล์นี้เพื่อติดตั้ง:"
    echo "$OUT"
fi

# -----------------------------
# GIT
# -----------------------------
echo ""
echo "=============================================="
echo "🔄 GIT UPDATE"
echo "=============================================="

git add \
    app/build.gradle \
    app/src/main/java/com/apomaekae/license/LicenseClient.java \
    app/src/main/java/com/apomaekae/license/KeyCountdown.java \
    update_key_lock_install.sh

if git diff --cached --quiet; then
    echo "ℹ️ ไม่มี Source เปลี่ยน"
else
    git commit -m "Update KEY LOCK COUNTDOWN $TAG"
    git push origin main
    echo "✅ Git push สำเร็จ"
fi

# -----------------------------
# GITHUB RELEASE
# -----------------------------
echo ""
echo "=============================================="
echo "🚀 GITHUB RELEASE"
echo "=============================================="

if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then

    if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then

        echo "♻️ พบ Release $TAG"
        echo "📤 อัปโหลด APK ทับ..."

        gh release upload \
            "$TAG" \
            "$OUT" \
            --repo "$REPO" \
            --clobber

    else

        echo "🆕 สร้าง Release $TAG"

        gh release create \
            "$TAG" \
            "$OUT" \
            --repo "$REPO" \
            --title "ApoMaeKae $TAG" \
            --generate-notes

    fi

    echo ""
    echo "✅ GitHub Release สำเร็จ"

    echo ""
    echo "🔗 Release:"
    echo "https://github.com/$REPO/releases/tag/$TAG"

    echo ""
    echo "⬇️ APK:"
    echo "https://github.com/$REPO/releases/download/$TAG/ApoMaeKae-$VERSION-KEY-LOCK.apk"

else

    echo "⚠️ ยังไม่ได้สร้าง Release อัตโนมัติ"
    echo ""
    echo "ถ้าต้องการให้สคริปต์สร้าง Release:"
    echo ""
    echo "pkg install gh -y"
    echo "gh auth login"
    echo "./update_key_lock_install.sh"

fi

echo ""
echo "=============================================="
echo "🎉 UPDATE เสร็จทั้งหมด"
echo "=============================================="
echo "VERSION : $VERSION"
echo "APK     : $OUT"
echo "KEY     : VERSION LOCK"
echo "EXPIRY  : COUNTDOWN"
echo "=============================================="
