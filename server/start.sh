#!/bin/sh
set -eu
: "${ADMIN_TOKEN:?กรุณาตั้ง ADMIN_TOKEN ก่อนรัน}"
node server.js
