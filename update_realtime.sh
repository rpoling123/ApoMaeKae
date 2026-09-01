#!/data/data/com.termux/files/usr/bin/bash

set -e

echo "======================================"
echo "🐉 ApoMaeKae V9.2.1"
echo "📡 REALTIME UPDATE"
echo "📍 Zone Buffer 100 เมตร"
echo "======================================"

cd ~/ApoMaeKae

echo ""
echo "1) ดึงโค้ดล่าสุดจาก GitHub..."
git pull origin main

echo ""
echo "2) ตรวจค่า Buffer..."
if [ -f update_config.json ]; then
    grep -n "buffer_meters" update_config.json || true
fi

echo ""
echo "3) ตรวจ Source..."
grep -Rni "Buffer 500\|Buffer 500 เมตร\|buffer_meters" app/src/main \
    --exclude="*.bak" \
    --exclude="*.apk" \
    || true

echo ""
echo "4) เตรียม Gradle..."
chmod +x gradlew

echo ""
echo "5) Clean + Build APK..."
./gradlew clean
./gradlew assembleDebug

echo ""
echo "6) สร้างโฟลเดอร์ releases..."
mkdir -p releases

echo ""
echo "7) คัดลอก APK..."
cp app/build/outputs/apk/debug/app-debug.apk \
   releases/ApoMaeKae-v9.2.1-100m.apk

echo ""
echo "8) ตรวจสอบ APK..."
if [ -f releases/ApoMaeKae-v9.2.1-100m.apk ]; then
    echo "✅ APK พร้อมติดตั้ง"
    ls -lh releases/ApoMaeKae-v9.2.1-100m.apk
else
    echo "❌ ไม่พบ APK"
    exit 1
fi

echo ""
echo "9) Git..."
git add app/src update_config.json releases/ApoMaeKae-v9.2.1-100m.apk 2>/dev/null || true

git add -A

if git diff --cached --quiet; then
    echo "ℹ️ ไม่มีไฟล์ใหม่สำหรับ Commit"
else
    git commit -m "ApoMaeKae v9.2.1 realtime Zone Buffer 100m"
    git push origin main
fi

echo ""
echo "======================================"
echo "✅ UPDATE COMPLETE"
echo "======================================"
echo "🐉 Version : V9.2.1"
echo "📍 Buffer  : 100 เมตร"
echo "📡 Realtime: พร้อม"
echo "📦 APK     : releases/ApoMaeKae-v9.2.1-100m.apk"
echo "🔗 Git     : origin/main"
echo "======================================"

echo ""
echo "เปิดโฟลเดอร์ APK:"
echo "termux-open releases/ApoMaeKae-v9.2.1-100m.apk"

echo ""
echo "ติดตั้ง APK:"
echo "pm install -r releases/ApoMaeKae-v9.2.1-100m.apk"
