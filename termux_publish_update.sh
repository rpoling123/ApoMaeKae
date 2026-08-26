#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

APP="${1:-$HOME/ApoMaeKae}"
cd "$APP"

VERSION_NAME="${VERSION_NAME:-9.2.0}"
VERSION_CODE="${VERSION_CODE:-92}"
TAG="v${VERSION_NAME}"

echo "=== APO MAEKAE REMOTE UPDATE PUBLISH ==="
echo "Version: ${VERSION_NAME} (${VERSION_CODE})"

# Update version in Gradle if these variables exist in the project.
if [ -f app/build.gradle ]; then
  sed -i -E "s/versionName[[:space:]]+\"[^\"]+\"/versionName \"${VERSION_NAME}\"/" app/build.gradle || true
  sed -i -E "s/versionCode[[:space:]]+[0-9]+/versionCode ${VERSION_CODE}/" app/build.gradle || true
fi

chmod +x ./gradlew
./gradlew clean assembleRelease --no-daemon

APK="$(find app/build/outputs/apk/release -type f -name '*.apk' | head -n 1)"
[ -n "$APK" ] || { echo "ไม่พบ release APK"; exit 1; }

mkdir -p releases
cp -f "$APK" "releases/ApoMaeKae-${TAG}.apk"

cat > update_manifest.json <<EOF
{
  "app_name": "ApoMaeKae",
  "version_name": "${VERSION_NAME}",
  "version_code": ${VERSION_CODE},
  "mandatory": false,
  "apk_url": "https://github.com/rpoling123/ApoMaeKae/releases/download/${TAG}/ApoMaeKae-${TAG}.apk",
  "config_url": "https://raw.githubusercontent.com/rpoling123/ApoMaeKae/main/update_config.json",
  "release_notes": "Remote Update + Remote Config"
}
EOF

git add update_manifest.json update_config.json releases/"ApoMaeKae-${TAG}.apk" app/build.gradle 2>/dev/null || true
git commit -m "Publish ApoMaeKae ${TAG} remote update" || true
git push origin main

echo
echo "สร้าง APK สำเร็จ:"
echo "$PWD/releases/ApoMaeKae-${TAG}.apk"
echo
echo "ขั้นต่อไป: สร้าง GitHub Release ${TAG} และแนบ APK ไฟล์นี้"
