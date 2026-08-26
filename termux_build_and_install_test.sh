#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "${1:-$HOME/ApoMaeKae}"

chmod +x ./gradlew
./gradlew clean assembleDebug --no-daemon

APK="$(find app/build/outputs/apk/debug -type f -name '*.apk' | head -n 1)"
[ -n "$APK" ] || { echo "ไม่พบ debug APK"; exit 1; }

cp -f "$APK" "$HOME/storage/downloads/ApoMaeKae-test.apk"
echo "APK พร้อมทดสอบ: $HOME/storage/downloads/ApoMaeKae-test.apk"
echo "ติดตั้งด้วย Android Package Installer แล้วกดยืนยันการอัปเดต"
