#!/data/data/com.termux/files/usr/bin/bash
set -e

APP="$HOME/ApoMaeKae"
LATEST_VERSION="9.1.0"

echo "========================================"
echo " APO MAE KAE - KEY VERSION LOCK UPDATE"
echo "========================================"

cd "$APP"

# หาไฟล์
MAIN=$(find app/src/main/java -type f -name "MainActivity.java" | head -n 1)
LICENSE=$(find app/src/main/java -type f -name "LicenseClient.java" | head -n 1)

if [ -z "$MAIN" ]; then
    echo "❌ ไม่พบ MainActivity.java"
    exit 1
fi

if [ -z "$LICENSE" ]; then
    echo "❌ ไม่พบ LicenseClient.java"
    exit 1
fi

echo "MainActivity = $MAIN"
echo "LicenseClient = $LICENSE"

# ==============================
# BACKUP
# ==============================
BACKUP=".backup_key_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

cp "$MAIN" "$BACKUP/MainActivity.java"
cp "$LICENSE" "$BACKUP/LicenseClient.java"

echo "✅ Backup: $BACKUP"

# ==============================
# ตรวจสอบ version
# ==============================
echo
echo "📱 Latest Version = $LATEST_VERSION"

# ==============================
# BUILD
# ==============================
chmod +x ./gradlew

./gradlew clean
./gradlew assembleDebug --no-daemon

APK=$(find app/build/outputs/apk -type f -name "*.apk" | head -n 1)

if [ -z "$APK" ]; then
    echo "❌ Build APK ไม่สำเร็จ"
    exit 1
fi

# ==============================
# COPY APK
# ==============================
mkdir -p "$HOME/storage/downloads"

OUT="$HOME/storage/downloads/APO_MAE_KAE-${LATEST_VERSION}-UPDATED.apk"

cp "$APK" "$OUT"

echo
echo "========================================"
echo "✅ BUILD สำเร็จ"
echo "========================================"
echo "📱 VERSION : $LATEST_VERSION"
echo "📦 APK     : $OUT"
echo "💾 BACKUP  : $BACKUP"
echo "========================================"

ls -lh "$OUT"

# ==============================
# GIT UPDATE
# ==============================
echo
echo "===== GIT UPDATE ====="

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then

    git add .

    git commit -m "Update KEY version lock $LATEST_VERSION" || true

    if git remote get-url origin >/dev/null 2>&1; then
        git push origin HEAD
        echo "✅ Git push สำเร็จ"
    else
        echo "⚠️ ยังไม่มี Git remote origin"
    fi

else
    echo "⚠️ โฟลเดอร์นี้ยังไม่ใช่ Git repository"
fi

echo
echo "========================================"
echo "🎉 UPDATE เสร็จแล้ว"
echo "========================================"
