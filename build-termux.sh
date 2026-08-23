#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"
echo "=== APO MAEKAE V9 • DRAGON ZONE ==="
echo "🐉 9 โซนมังกร | GPS | แจ้งเตือนออกนอกโซน | Buffer 500m"
chmod +x gradlew 2>/dev/null || true
if [ -x ./gradlew ]; then
  ./gradlew clean assembleDebug --no-daemon
else
  gradle clean assembleDebug --no-daemon
fi
APK="$(find app/build/outputs/apk -type f -name '*.apk' | head -n 1)"
if [ -z "$APK" ]; then
  echo "❌ BUILD ผ่านแต่หา APK ไม่พบ"
  exit 1
fi
OUT="$HOME/APO_MAEKAE_V9_DragonZone.apk"
cp -f "$APK" "$OUT"
echo
echo "=========================================="
echo "✅ BUILD SUCCESSFUL"
echo "📦 APK: $OUT"
echo "=========================================="
ls -lh "$OUT"
