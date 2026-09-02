#!/data/data/com.termux/files/usr/bin/bash

set -u

REPO="rpoling123/ApoMaeKae"
API="https://api.github.com/repos/$REPO/releases/latest"
DOWNLOAD_DIR="$HOME/storage/downloads"

echo "=============================================="
echo "📦 APO MAE KAE — GITHUB LATEST INSTALLER"
echo "=============================================="

mkdir -p "$DOWNLOAD_DIR"

# ตรวจ GitHub Release ล่าสุด
JSON="$(curl -fsSL "$API")"

if [ -z "$JSON" ]; then
    echo "❌ ติดต่อ GitHub ไม่สำเร็จ"
    exit 1
fi

LATEST_TAG="$(printf '%s' "$JSON" \
    | grep '"tag_name":' \
    | head -1 \
    | sed 's/.*"tag_name":[[:space:]]*"//;s/".*//')"

APK_URL="$(printf '%s' "$JSON" \
    | grep '"browser_download_url":' \
    | grep -E '\.apk"' \
    | head -1 \
    | sed 's/.*"browser_download_url":[[:space:]]*"//;s/".*//')"

if [ -z "$LATEST_TAG" ]; then
    echo "❌ อ่าน Version ล่าสุดจาก GitHub ไม่ได้"
    exit 1
fi

if [ -z "$APK_URL" ]; then
    echo "❌ Release ล่าสุดไม่มี APK"
    echo "Release: $LATEST_TAG"
    exit 1
fi

echo
echo "📌 GitHub Release ล่าสุด: $LATEST_TAG"

# ------------------------------------------------
# หาชื่อ Package และ Version จาก APK ที่ติดตั้ง
# ------------------------------------------------
CURRENT_VERSION=""

if command -v dumpsys >/dev/null 2>&1; then
    CURRENT_VERSION="$(
        dumpsys package com.apomaekae 2>/dev/null \
        | grep -m1 'versionName=' \
        | sed 's/.*versionName=//;s/[[:space:]].*//'
    )"
fi

if [ -z "$CURRENT_VERSION" ]; then
    CURRENT_VERSION="UNKNOWN"
fi

echo "📱 Version ในเครื่อง: $CURRENT_VERSION"

# ตัด v ออกจาก GitHub tag
LATEST_NUM="${LATEST_TAG#v}"
CURRENT_NUM="${CURRENT_VERSION#v}"

echo

# ------------------------------------------------
# ถ้าเป็น Version ล่าสุดแล้ว ไม่ต้องดาวน์โหลด
# ------------------------------------------------
if [ "$CURRENT_NUM" = "$LATEST_NUM" ]; then
    echo "=============================================="
    echo "✅ เป็น Version ล่าสุดแล้ว"
    echo "🚫 ไม่ดาวน์โหลด APK"
    echo "🚫 ไม่สร้าง Version ใหม่"
    echo "=============================================="
    exit 0
fi

echo "🆕 พบ Version ใหม่"
echo "   เครื่อง : $CURRENT_VERSION"
echo "   GitHub  : $LATEST_TAG"

# ------------------------------------------------
# ดาวน์โหลด APK
# ------------------------------------------------
APK="$DOWNLOAD_DIR/ApoMaeKae-$LATEST_TAG.apk"

echo
echo "⬇️ กำลังดาวน์โหลด..."
echo "$APK_URL"

if ! curl -fL "$APK_URL" -o "$APK"; then
    echo "❌ ดาวน์โหลด APK ไม่สำเร็จ"
    rm -f "$APK"
    exit 1
fi

if [ ! -s "$APK" ]; then
    echo "❌ APK ว่างหรือดาวน์โหลดไม่สมบูรณ์"
    rm -f "$APK"
    exit 1
fi

echo
echo "=============================================="
echo "✅ ดาวน์โหลดสำเร็จ"
echo "📦 $APK"
echo "📌 Version: $LATEST_TAG"
echo "=============================================="

# เปิดติดตั้ง
if command -v termux-open >/dev/null 2>&1; then
    termux-open "$APK"
else
    echo "📂 เปิดไฟล์นี้เพื่อติดตั้ง:"
    echo "$APK"
fi
