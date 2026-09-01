#!/data/data/com.termux/files/usr/bin/bash

set -e

PROJECT="$HOME/ApoMaeKae"
cd "$PROJECT"

REPO="rpoling123/ApoMaeKae"

echo "=============================================="
echo " APO MAE KAE"
echo " KEY LOCK + COUNTDOWN + GITHUB RELEASE"
echo "=============================================="

# ==================================================
# 1. หา VERSION จาก source จริง
# ห้ามสร้าง version เอง
# ==================================================

CURRENT_VERSION=""

for FILE in \
    app/build.gradle \
    app/build.gradle.kts \
    build.gradle \
    build.gradle.kts
do
    if [ -f "$FILE" ]; then

        V=$(grep -m1 -E \
        'versionName[[:space:]]*[= ]+[\"'\''][^\"'\'']+[\"'\'']' \
        "$FILE" 2>/dev/null \
        | sed -E 's/.*versionName[[:space:]]*[= ]+[\"'\'']([^\"'\'']+)[\"'\''].*/\1/')

        if [ -n "$V" ]; then
            CURRENT_VERSION="$V"
            break
        fi
    fi
done

if [ -z "$CURRENT_VERSION" ]; then
    echo "❌ ไม่พบ versionName ใน Gradle"
    echo "หยุดทันที เพื่อป้องกันการสร้าง VERSION เอง"
    exit 1
fi

echo
echo "📱 VERSION จาก SOURCE : $CURRENT_VERSION"

# ==================================================
# 2. ดึง Release ล่าสุดจาก GitHub
# ไม่กำหนด VERSION เอง
# ==================================================

API="https://api.github.com/repos/$REPO/releases/latest"

JSON=$(curl -fsSL \
    -H "Accept: application/vnd.github+json" \
    "$API")

LATEST_TAG=$(printf '%s' "$JSON" |
    grep '"tag_name"' |
    head -1 |
    sed -E 's/.*"tag_name":[[:space:]]*"([^"]+)".*/\1/')

if [ -z "$LATEST_TAG" ]; then
    echo "❌ อ่าน GitHub Release ล่าสุดไม่ได้"
    exit 1
fi

LATEST_VERSION="${LATEST_TAG#v}"

echo "🌐 GITHUB LATEST     : $LATEST_VERSION"

# ==================================================
# 3. หา APK จาก Release จริง
# ==================================================

APK_URL=$(printf '%s' "$JSON" |
    grep '"browser_download_url"' |
    grep -Ei '\.apk"' |
    head -1 |
    sed -E 's/.*"browser_download_url":[[:space:]]*"([^"]+)".*/\1/')

if [ -z "$APK_URL" ]; then
    echo "⚠️ Release ล่าสุดยังไม่มี APK"
else
    echo "📦 APK URL:"
    echo "$APK_URL"
fi

# ==================================================
# 4. เปรียบเทียบ VERSION
# ==================================================

version_gt() {
    [ "$(printf '%s\n' "$1" "$2" | sort -V | tail -1)" = "$1" ] &&
    [ "$1" != "$2" ]
}

echo
echo "=============================================="
echo " VERSION CHECK"
echo "=============================================="

if [ "$CURRENT_VERSION" = "$LATEST_VERSION" ]; then

    echo "✅ ใช้เวอร์ชันล่าสุดแล้ว"
    echo "📱 Current : $CURRENT_VERSION"
    echo "🌐 Latest  : $LATEST_VERSION"
    echo
    echo "⛔ ไม่ดาวน์โหลด APK"
    echo "⛔ ไม่ติดตั้ง APK"
    echo "=============================================="

else

    if version_gt "$LATEST_VERSION" "$CURRENT_VERSION"; then

        echo "🆕 พบเวอร์ชันใหม่"
        echo "📱 Current : $CURRENT_VERSION"
        echo "🌐 Latest  : $LATEST_VERSION"

        if [ -z "$APK_URL" ]; then
            echo "❌ ไม่มี APK ใน Release ล่าสุด"
            exit 1
        fi

        mkdir -p "$HOME/storage/downloads"

        FILE_NAME=$(basename "$APK_URL")
        OUT="$HOME/storage/downloads/$FILE_NAME"

        echo
        echo "⬇️ ดาวน์โหลด APK จาก GitHub"
        echo "$APK_URL"

        curl -fL \
            --retry 3 \
            -o "$OUT" \
            "$APK_URL"

        echo
        echo "✅ ดาวน์โหลดสำเร็จ"
        ls -lh "$OUT"

    else

        echo "⚠️ VERSION ใน SOURCE ใหม่กว่า GitHub"
        echo "📱 Source  : $CURRENT_VERSION"
        echo "🌐 GitHub  : $LATEST_VERSION"
        echo
        echo "⛔ ไม่สร้าง VERSION ใหม่"
        echo "⛔ ไม่ดาวน์โหลด"
    fi
fi

# ==================================================
# 5. Git
# ==================================================

echo
echo "=============================================="
echo " GIT UPDATE"
echo "=============================================="

git status --short

git add \
    app/src/main \
    server \
    update_key_lock_install.sh \
    2>/dev/null || true

if git diff --cached --quiet; then

    echo "ℹ️ ไม่มี source code ใหม่สำหรับ commit"

else

    git commit -m "Update key lock and GitHub latest release check"
    git push origin main

    echo "✅ Git push สำเร็จ"
fi

echo
echo "=============================================="
echo "✅ เสร็จสิ้น"
echo "=============================================="
