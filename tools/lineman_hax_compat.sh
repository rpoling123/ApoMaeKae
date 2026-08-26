#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

APK="${1:-$HOME/storage/downloads/LINE MAN HAX.apk}"
OUT="${2:-build/hax-report}"

[ -f "$APK" ] || { echo "ไม่พบ APK: $APK"; exit 1; }
mkdir -p "$OUT"

unzip -t "$APK" >/dev/null
sha256sum "$APK" > "$OUT/sha256.txt"
aapt dump badging "$APK" > "$OUT/badging.txt" 2>&1 || true
grep -E 'package:|sdkVersion:|targetSdkVersion:|native-code:|launchable-activity:'   "$OUT/badging.txt" || true

rm -rf "$OUT/unzip"
mkdir -p "$OUT/unzip"
unzip -q "$APK" -d "$OUT/unzip"

find "$OUT/unzip/lib" -type f 2>/dev/null |
  sed 's#^.*/lib/#lib/#' |
  sort > "$OUT/native-libs.txt" || true

find "$OUT/unzip" -maxdepth 1 -name '*.dex' -print0 |
  xargs -0 strings 2>/dev/null |
  grep -Ei 'matchPercent|declineCount|DECLINE_LIMIT|GPS|job|order|rider|commission|percentage' |
  sort -u > "$OUT/keywords.txt" || true

echo "รายงานอยู่ที่ $OUT"
echo "ไม่แก้ HAX และไม่ bypass signature/anti-tamper/server"
