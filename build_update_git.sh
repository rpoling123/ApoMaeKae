#!/data/data/com.termux/files/usr/bin/bash

set -e

PROJECT="$HOME/ApoMaeKae"
REPO="rpoling123/ApoMaeKae"

cd "$PROJECT"

echo "=============================================="
echo " APO MAE KAE"
echo " BUILD + KEY LOCK + VERSION + GITHUB"
echo "=============================================="

# ------------------------------------------------
# 1. อ่าน VERSION จากโปรเจกต์จริง
# ------------------------------------------------

echo
echo "🔎 อ่าน VERSION จาก SOURCE..."

VERSION_NAME=""

for FILE in \
    app/build.gradle \
    app/build.gradle.kts
do
    if [ -f "$FILE" ]; then

        VERSION_NAME=$(grep -m1 "versionName" "$FILE" 2>/dev/null \
        | sed -E 's/.*versionName[[:space:]]*[=:]?[[:space:]]*["'\'']([^"'\'']+)["'\''].*/\1/')

        if [ -n "$VERSION_NAME" ] &&
           [[ "$VERSION_NAME" != *"versionName"* ]]; then
            break
        fi

        VERSION_NAME=""
    fi
done

if [ -z "$VERSION_NAME" ]; then
    echo
    echo "❌ ไม่พบ versionName"
    echo "⛔ หยุด เพื่อป้องกันการสร้าง VERSION เอง"
    exit 1
fi

echo "📱 SOURCE VERSION : $VERSION_NAME"

# ------------------------------------------------
# 2. อ่าน versionCode จากโปรเจกต์จริง
# ------------------------------------------------

VERSION_CODE=""

for FILE in \
    app/build.gradle \
    app/build.gradle.kts
do
    if [ -f "$FILE" ]; then

        VERSION_CODE=$(grep -m1 "versionCode" "$FILE" 2>/dev/null \
        | sed -E 's/.*versionCode[[:space:]]*[=:]?[[:space:]]*([0-9]+).*/\1/')

        if [[ "$VERSION_CODE" =~ ^[0-9]+$ ]]; then
            break
        fi

        VERSION_CODE=""
    fi
done

echo "🔢 SOURCE CODE    : ${VERSION_CODE:-ไม่พบ}"

# ------------------------------------------------
# 3. GitHub Release ล่าสุด
# ------------------------------------------------

echo
echo "🌐 ตรวจ GitHub Release ล่าสุด..."

API="https://api.github.com/repos/$REPO/releases/latest"

JSON=$(curl -fsSL \
    -H "Accept: application/vnd.github+json" \
    "$API")

LATEST_TAG=$(printf '%s' "$JSON" \
    | grep '"tag_name"' \
    | head -1 \
    | sed -E 's/.*"tag_name":[[:space:]]*"([^"]+)".*/\1/')

if [ -z "$LATEST_TAG" ]; then
    echo "❌ อ่าน GitHub Release ไม่ได้"
    exit 1
fi

LATEST_VERSION="${LATEST_TAG#v}"

echo "🌐 GITHUB VERSION : $LATEST_VERSION"

# ------------------------------------------------
# 4. หา APK จาก Release จริง
# ------------------------------------------------

APK_URL=$(printf '%s' "$JSON" \
    | grep '"browser_download_url"' \
    | grep -Ei '\.apk"' \
    | head -1 \
    | sed -E 's/.*"browser_download_url":[[:space:]]*"([^"]+)".*/\1/')

if [ -n "$APK_URL" ]; then
    echo "📦 RELEASE APK    : พบ"
else
    echo "⚠️ RELEASE APK    : ไม่พบ"
fi

# ------------------------------------------------
# 5. เปรียบเทียบ
# ------------------------------------------------

echo
echo "=============================================="
echo " VERSION CHECK"
echo "=============================================="

if [ "$VERSION_NAME" = "$LATEST_VERSION" ]; then

    echo "✅ SOURCE เป็น VERSION ล่าสุดแล้ว"
    echo "📱 $VERSION_NAME"
    echo
    echo "⛔ จะไม่ดาวน์โหลด APK จาก GitHub"

else

    echo "ℹ️ VERSION ไม่ตรงกับ Release ล่าสุด"
    echo "📱 SOURCE : $VERSION_NAME"
    echo "🌐 GITHUB : $LATEST_VERSION"
    echo
    echo "⚠️ จะไม่แก้ VERSION ให้เอง"
fi

# ------------------------------------------------
# 6. ล้างเฉพาะไฟล์ Build ที่สร้างขึ้น
# ------------------------------------------------

echo
echo "🧹 CLEAN BUILD..."

./gradlew clean

# ------------------------------------------------
# 7. Build
# ------------------------------------------------

echo
echo "=============================================="
echo " 📦 BUILD APK"
echo "=============================================="

./gradlew :app:assembleDebug

if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo
    echo "❌ BUILD ไม่พบ APK"
    echo "⛔ ไม่ Push Git"
    exit 1
fi

echo
echo "✅ BUILD SUCCESSFUL"

# ------------------------------------------------
# 8. ตั้งชื่อ APK จาก VERSION จริง
# ห้ามกำหนด VERSION เอง
# ------------------------------------------------

DOWNLOAD_DIR="$HOME/storage/downloads"

mkdir -p "$DOWNLOAD_DIR"

OUT="$DOWNLOAD_DIR/ApoMaeKae-${VERSION_NAME}.apk"

cp -f \
    app/build/outputs/apk/debug/app-debug.apk \
    "$OUT"

echo
echo "📦 APK สำเร็จ:"
echo "$OUT"

ls -lh "$OUT"

# ------------------------------------------------
# 9. เอาไฟล์ขยะออกจาก Git staging
# ------------------------------------------------

echo
echo "🧹 ตรวจไฟล์ที่ไม่ควรขึ้น Git..."

git add -A

# ไม่ให้ build output ขึ้น Git
git restore --staged build 2>/dev/null || true
git restore --staged app/build 2>/dev/null || true

# ไม่ให้ backup ขึ้น Git
find . -maxdepth 3 \
    \( -name "*.bak_*" -o -name ".backup_*" \) \
    -print 2>/dev/null \
    | while read -r F; do
        git restore --staged "$F" 2>/dev/null || true
      done

# ไม่ให้ generated report ขึ้น Git
git restore --staged \
    build/reports \
    2>/dev/null || true

# ------------------------------------------------
# 10. แสดงสิ่งที่จะ Commit
# ------------------------------------------------

echo
echo "=============================================="
echo " 📋 FILES TO COMMIT"
echo "=============================================="

git status --short

# ------------------------------------------------
# 11. Commit + Push
# ------------------------------------------------

if git diff --cached --quiet; then

    echo
    echo "ℹ️ ไม่มี Source ใหม่สำหรับ Commit"

else

    echo
    echo "🚀 COMMIT..."

    git commit \
        -m "Update key lock and release version checker"

    echo
    echo "🚀 PUSH..."

    git push origin main

    echo
    echo "✅ Git push สำเร็จ"
fi

# ------------------------------------------------
# 12. สรุป
# ------------------------------------------------

echo
echo "=============================================="
echo " 🎉 เสร็จสิ้น"
echo "=============================================="

echo "📱 SOURCE VERSION : $VERSION_NAME"
echo "🔢 VERSION CODE   : ${VERSION_CODE:-ไม่พบ}"
echo "🌐 GITHUB RELEASE : $LATEST_VERSION"
echo "📦 APK            : $OUT"

echo
echo "❗ ไม่มีการสร้าง VERSION ใหม่"
echo "❗ Build ไม่ผ่าน = ไม่ Push"
echo "=============================================="
