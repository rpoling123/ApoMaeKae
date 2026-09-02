#!/data/data/com.termux/files/usr/bin/bash
set -e

cd "$HOME/ApoMaeKae"

echo "=============================================="
echo " APO MAE KAE - KEY VERSION LOCK UPDATE"
echo "=============================================="

echo "===== GIT PULL ====="
git pull --ff-only origin main || true

LATEST_VERSION=$(sed -n "s/.*versionName ['\"]\([^'\"]*\)['\"].*/\1/p" \
  app/build.gradle | head -n1)

VERSION_CODE=$(sed -n "s/.*versionCode \([0-9][0-9]*\).*/\1/p" \
  app/build.gradle | head -n1)

LATEST_VERSION=${LATEST_VERSION:-9.2.3}
VERSION_CODE=${VERSION_CODE:-94}

echo "📱 Latest Version : $LATEST_VERSION"
echo "🔢 Version Code   : $VERSION_CODE"

SERVER="server/server.js"
CLIENT="app/src/main/java/com/apomaekae/license/LicenseClient.java"
COUNTDOWN="app/src/main/java/com/apomaekae/license/KeyCountdown.java"
BUY="server/public/buy-key.html"

for f in "$SERVER" "$CLIENT" "$COUNTDOWN" "$BUY"; do
    if [ ! -f "$f" ]; then
        echo "❌ ไม่พบไฟล์: $f"
        exit 1
    fi
done

STAMP=$(date +%Y%m%d_%H%M%S)
BACKUP=".backup_key_latest_$STAMP"

mkdir -p "$BACKUP"

cp "$SERVER" "$BACKUP/server.js"
cp "$CLIENT" "$BACKUP/LicenseClient.java"
cp "$COUNTDOWN" "$BACKUP/KeyCountdown.java"
cp "$BUY" "$BACKUP/buy-key.html"

echo "💾 Backup: $BACKUP"

export LATEST_VERSION VERSION_CODE SERVER CLIENT COUNTDOWN BUY

python3 <<'PY'
from pathlib import Path
import os
import re
import json

v = os.environ["LATEST_VERSION"]
vc = int(os.environ["VERSION_CODE"])

server = Path(os.environ["SERVER"])
client = Path(os.environ["CLIENT"])
countdown = Path(os.environ["COUNTDOWN"])
buy = Path(os.environ["BUY"])

# ============================================================
# SERVER
# ============================================================

s = server.read_text(encoding="utf-8")

# Latest version constant
if "const LATEST_VERSION=" in s:
    s = re.sub(
        r'const LATEST_VERSION="[^"]*";',
        f'const LATEST_VERSION="{v}";',
        s,
        count=1
    )
else:
    pos = s.find("const PORT=")
    if pos >= 0:
        s = s[:pos] + f'const LATEST_VERSION="{v}";' + s[pos:]
    else:
        s = f'const LATEST_VERSION="{v}";\n' + s

# ============================================================
# KEY GENERATOR
# KEY ที่สร้างใหม่จะถูกผูกกับเวอร์ชันล่าสุด
# ============================================================

if "appVersion: LATEST_VERSION" not in s:
    s = s.replace(
        "createdAt:new Date().toISOString()",
        "appVersion: LATEST_VERSION,createdAt:new Date().toISOString()",
        1
    )

# ============================================================
# API CHECK
# ============================================================

start = s.find('if(q.method==="POST"&&p==="/api/license/check")')

if start >= 0:
    end = s.find(
        'if(q.method==="POST"&&p==="/api/license/heartbeat")',
        start
    )

    if end < 0:
        end = len(s)

    block = s[start:end]

    marker = (
        'let db=read(DB,{keys:{}}),'
        'k=db.keys[String(b.key||"").trim().toUpperCase()],'
        'now=Date.now();'
    )

    if marker in block and 'code:"VERSION_MISMATCH"' not in block:

        patch = (
            'let clientVersion=String(b.appVersion||"").trim();'
            'if(clientVersion!==LATEST_VERSION)'
            'return send(r,200,{'
            'active:false,'
            'online:false,'
            'code:"VERSION_MISMATCH",'
            'message:"KEY ใช้ได้เฉพาะเวอร์ชันล่าสุด "+LATEST_VERSION,'
            'serverTime:now,'
            'latestVersion:LATEST_VERSION,'
            'appVersion:clientVersion'
            '});'
        )

        block = block.replace(
            marker,
            marker + patch,
            1
        )

    # KEY ที่สร้างจากเวอร์ชันเก่า ห้ามใช้กับเวอร์ชันใหม่
    marker2 = "let z=Date.parse(k.expiresAt);"

    if marker2 in block and 'code:"KEY_VERSION_OLD"' not in block:

        patch2 = (
            'if(String(k.appVersion||"")!==LATEST_VERSION)'
            'return send(r,200,{'
            'active:false,'
            'online:false,'
            'code:"KEY_VERSION_OLD",'
            'message:"KEY นี้เป็นของเวอร์ชันเก่า",'
            'serverTime:now,'
            'expiresAt:z,'
            'keyVersion:k.appVersion||"",'
            'latestVersion:LATEST_VERSION'
            '});'
        )

        block = block.replace(
            marker2,
            marker2 + patch2,
            1
        )

    s = s[:start] + block + s[end:]

# ============================================================
# ADMIN KEY GENERATOR
# ============================================================

if "appVersion:LATEST_VERSION" not in s:
    s = s.replace(
        "maxDevices:Number(b.maxDevices||1),",
        "maxDevices:Number(b.maxDevices||1),appVersion:LATEST_VERSION,",
        1
    )

server.write_text(s, encoding="utf-8")

# ============================================================
# LICENSE CLIENT
# ============================================================

c = client.read_text(encoding="utf-8")

# ส่ง version จริงของ APK ไป server
if 'body.put("appVersion"' not in c:

    target = 'body.put("deviceId", getDeviceId(context));'

    if target in c:
        c = c.replace(
            target,
            target +
            '\n            body.put("appVersion", BuildConfig.VERSION_NAME);',
            1
        )

# ============================================================
# เก็บข้อมูล VERSION จาก SERVER
# ============================================================

if '.putString("keyVersion"' not in c:

    target = '.putString("status", status)'

    if target in c:
        patch = '''
                        .putString("keyVersion",
                                json.optString(
                                        "keyVersion",
                                        json.optString(
                                                "appVersion",
                                                BuildConfig.VERSION_NAME
                                        )
                                )
                        )
                        .putString(
                                "latestVersion",
                                json.optString(
                                        "latestVersion",
                                        BuildConfig.VERSION_NAME
                                )
                        )
                        .putLong(
                                "serverTime",
                                json.optLong(
                                        "serverTime",
                                        System.currentTimeMillis()
                                )
                        )'''

        c = c.replace(
            target,
            target + patch,
            1
        )

# ============================================================
# GETTERS
# ============================================================

if "getKeyVersion(Context context)" not in c:

    pos = c.rfind("\n    public static final class Result")

    if pos >= 0:

        getters = '''
    public static String getKeyVersion(Context context) {
        return context
                .getSharedPreferences("license", Context.MODE_PRIVATE)
                .getString("keyVersion", "");
    }

    public static String getLatestVersion(Context context) {
        return context
                .getSharedPreferences("license", Context.MODE_PRIVATE)
                .getString(
                        "latestVersion",
                        BuildConfig.VERSION_NAME
                );
    }

    public static long getServerTime(Context context) {
        return context
                .getSharedPreferences("license", Context.MODE_PRIVATE)
                .getLong(
                        "serverTime",
                        System.currentTimeMillis()
                );
    }
'''

        c = c[:pos] + getters + c[pos:]

client.write_text(c, encoding="utf-8")

# ============================================================
# COUNTDOWN
# ============================================================

cd = countdown.read_text(encoding="utf-8")

cd = cd.replace(
    "🔑 KEY หมดอายุแล้ว",
    "🔴 KEY หมดอายุแล้ว"
)

cd = cd.replace(
    '"🔑 KEY หมดอายุใน "',
    '"⏳ KEY หมดอายุใน "'
)

countdown.write_text(cd, encoding="utf-8")

# ============================================================
# BUY KEY PAGE
# ============================================================

bu = buy.read_text(encoding="utf-8")

bu = bu.replace(
    "APO MAEKAE V9.1",
    "APO MAEKAE V" + v
)

buy.write_text(bu, encoding="utf-8")

# ============================================================
# UPDATE JSON
# ============================================================

update = Path("update.json")

if update.exists():

    try:
        data = json.loads(
            update.read_text(encoding="utf-8")
        )
    except Exception:
        data = {}

    data["versionCode"] = vc
    data["versionName"] = v
    data["minVersion"] = v
    data["forceUpdate"] = True
    data["note"] = (
        "KEY ใช้ได้เฉพาะเวอร์ชันล่าสุด + "
        "วันหมดอายุ + Countdown"
    )

    update.write_text(
        json.dumps(
            data,
            ensure_ascii=False,
            indent=2
        ) + "\n",
        encoding="utf-8"
    )

print("")
print("✅ PATCH สำเร็จ")
print("📱 VERSION =", v)
print("🔢 CODE    =", vc)
PY

echo ""
echo "===== ตรวจสอบ KEY VERSION ====="

grep -n "LATEST_VERSION" "$SERVER" | head -10 || true

echo ""
echo "===== ตรวจสอบ CLIENT VERSION ====="

grep -n "appVersion" "$CLIENT" | head -10 || true

echo ""
echo "=============================================="
echo "🔨 BUILD APK"
echo "=============================================="

chmod +x ./gradlew

./gradlew clean assembleDebug --no-daemon

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "❌ BUILD ไม่พบ APK"
    exit 1
fi

mkdir -p "$HOME/storage/downloads"

OUT="$HOME/storage/downloads/APO_MAE_KAE-${LATEST_VERSION}-KEY-LOCK.apk"

cp -f "$APK" "$OUT"

echo ""
echo "=============================================="
echo "📦 APK พร้อมแล้ว"
echo "=============================================="

ls -lh "$OUT"

echo ""
echo "=============================================="
echo "☁️ GIT UPDATE"
echo "=============================================="

git add \
    "$SERVER" \
    "$CLIENT" \
    "$COUNTDOWN" \
    "$BUY" \
    update.json

git commit -m \
    "KEY lock latest version $LATEST_VERSION"

git push origin main

echo ""
echo "=============================================="
echo "✅ UPDATE + BUILD + GIT สำเร็จ"
echo "=============================================="
echo "📱 VERSION : $LATEST_VERSION"
echo "🔢 CODE    : $VERSION_CODE"
echo "🔐 KEY     : VERSION ล่าสุดเท่านั้น"
echo "📅 EXPIRY  : วัน/เวลา"
echo "⏳ COUNTDOWN: วัน/ชม./นาที/วินาที"
echo "☁️ GIT     : PUSH สำเร็จ"
echo "📦 APK     : $OUT"
echo "💾 BACKUP  : $BACKUP"
echo "=============================================="
