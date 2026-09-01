#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

PROJECT="$HOME/ApoMaeKae"
MAIN="$PROJECT/app/src/main/java/com/apomaekae/dragon/MainActivity.java"
MANIFEST="$PROJECT/app/src/main/AndroidManifest.xml"
GRADLE="$PROJECT/app/build.gradle"

STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP="$PROJECT/.backup_v923_realtime_$STAMP"
APK="$PROJECT/app/build/outputs/apk/debug/app-debug.apk"
OUT="$PROJECT/releases/ApoMaeKae-v9.2.3-REALTIME-GPS-MAPS.apk"
DOWNLOAD="$HOME/storage/downloads/ApoMaeKae-v9.2.3-REALTIME-GPS-MAPS.apk"

echo "=============================================="
echo " APO MAEKAE V9.2.3"
echo " REALTIME GPS • CURRENT LOCATION • GOOGLE MAPS"
echo "=============================================="

[ -d "$PROJECT/.git" ] || {
  echo "❌ ไม่พบ $PROJECT"
  exit 1
}

cd "$PROJECT"

echo "[1/7] สำรองไฟล์"
mkdir -p "$BACKUP"
cp -f "$MAIN" "$BACKUP/MainActivity.java"
cp -f "$MANIFEST" "$BACKUP/AndroidManifest.xml"
cp -f "$GRADLE" "$BACKUP/app-build.gradle"

echo "Backup: $BACKUP"

echo "[2/7] เพิ่ม GPS Permission"

python3 - "$MANIFEST" <<'PY'
from pathlib import Path
import sys

p=Path(sys.argv[1])
s=p.read_text(encoding="utf-8")

perms=[
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
]

for perm in perms:
    tag=f'<uses-permission android:name="{perm}"/>'
    if tag not in s:
        s=s.replace("<application",tag+"\n    <application",1)

p.write_text(s,encoding="utf-8")
PY

echo "[3/7] แก้ MainActivity เป็น GPS REALTIME"

python3 - "$MAIN" <<'PY'
from pathlib import Path
import sys

p=Path(sys.argv[1])
s=p.read_text(encoding="utf-8")

# เพิ่ม import
if "import android.location.Location;" not in s:
    s=s.replace(
        "import android.content.pm.PackageManager;",
        "import android.content.pm.PackageManager;\n"
        "import android.location.Location;\n"
        "import android.location.LocationListener;\n"
        "import android.location.LocationManager;"
    )

# fields
if "APO_V923_REALTIME_GPS" not in s:
    marker="public class MainActivity extends Activity {"

    fields=r'''
    // APO_V923_REALTIME_GPS
    private LocationManager apoLocationManager;
    private LocationListener apoLocationListener;
    private Location apoCurrentLocation;
    private android.os.Handler apoGpsHandler = new android.os.Handler();

'''

    s=s.replace(marker,marker+"\n"+fields,1)

# onCreate เรียก realtime
if "startApoRealtimeGps();" not in s:
    # หลัง request permission
    s=s.replace(
        "requestPermissionsIfNeeded();",
        "requestPermissionsIfNeeded();\n"
        "startApoRealtimeGps();",
        1
    )

# เปลี่ยน openMaps เดิม
old='private void openMaps(){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://maps.google.com")));}catch(Exception e){Toast.makeText(this,"เปิด Google Maps ไม่ได้",Toast.LENGTH_LONG).show();}}'

if old in s:
    s=s.replace(old,"private void openMaps(){ openCurrentGoogleMaps(); }",1)

# ถ้า openMaps เป็นรูปแบบหลายบรรทัด ให้ไม่ยุ่งกับของเดิม
# เพิ่ม method ก่อน requestPermissionsIfNeeded
if "private void openCurrentGoogleMaps()" not in s:

    marker="    private void requestPermissionsIfNeeded(){"

    methods=r'''
    // ============================================================
    // APO V9.2.3 • REALTIME CURRENT GPS
    // ============================================================

    private void startApoRealtimeGps() {
        try {
            apoLocationManager =
                    (LocationManager)getSystemService(LOCATION_SERVICE);

            apoLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location == null) return;

                    apoCurrentLocation = location;

                    // แสดงตำแหน่งล่าสุดในช่อง GPS ถ้ามี
                    try {
                        if (gps != null) {
                            gps.setText(
                                String.format(
                                    Locale.US,
                                    "📍 GPS ปัจจุบัน: %.6f, %.6f\nความแม่นยำ %.0f เมตร • REALTIME",
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    location.hasAccuracy()
                                        ? location.getAccuracy()
                                        : 0
                                )
                            );
                        }
                    } catch (Exception ignored) {}
                }

                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            };

            if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            apoLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    3.0f,
                    apoLocationListener
            );

            // ดึง last known location ทันที
            Location last =
                    apoLocationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER);

            if (last == null) {
                last =
                    apoLocationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER);
            }

            if (last != null) {
                apoCurrentLocation = last;
            }

        } catch (Exception e) {
            try {
                if (gps != null)
                    gps.setText("📍 GPS: รอสิทธิ์ตำแหน่ง");
            } catch (Exception ignored) {}
        }
    }

    private void openCurrentGoogleMaps() {
        try {
            Location l = apoCurrentLocation;

            if (l == null && apoLocationManager != null) {
                if (Build.VERSION.SDK_INT < 23 ||
                    checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {

                    l = apoLocationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER);

                    if (l == null) {
                        l = apoLocationManager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER);
                    }
                }
            }

            if (l == null) {
                Toast.makeText(
                        this,
                        "ยังไม่พบตำแหน่ง GPS ปัจจุบัน",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            double lat = l.getLatitude();
            double lon = l.getLongitude();

            Uri uri = Uri.parse(
                    "geo:" + lat + "," + lon +
                    "?z=17&q=" + lat + "," + lon
            );

            Intent intent =
                    new Intent(Intent.ACTION_VIEW, uri);

            intent.setPackage("com.google.android.apps.maps");

            try {
                startActivity(intent);
            } catch (Exception noGoogleMaps) {
                intent.setPackage(null);
                startActivity(intent);
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "เปิด Google Maps ไม่ได้",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

'''

    if marker in s:
        s=s.replace(marker,methods+marker,1)
    else:
        # fallback ก่อนคลาสปิด
        pos=s.rfind("}")
        s=s[:pos]+methods+s[pos:]

# onDestroy หยุด realtime GPS
if "APO_V923_STOP_GPS" not in s:
    marker="    @Override\n    protected void onDestroy()"

    stop=r'''
    // APO_V923_STOP_GPS
    private void stopApoRealtimeGps() {
        try {
            if (apoLocationManager != null &&
                apoLocationListener != null) {
                apoLocationManager.removeUpdates(
                        apoLocationListener
                );
            }
        } catch (Exception ignored) {}
    }

'''

    if marker in s:
        s=s.replace(marker,stop+marker,1)

# เพิ่ม stop ก่อน super.onDestroy
if "stopApoRealtimeGps();" not in s:
    s=s.replace(
        "super.onDestroy();",
        "stopApoRealtimeGps();\n        super.onDestroy();",
        1
    )

p.write_text(s,encoding="utf-8")
PY

echo "[4/7] อัป version 9.2.3"

python3 - "$GRADLE" <<'PY'
from pathlib import Path
import re
import sys

p=Path(sys.argv[1])
s=p.read_text(encoding="utf-8")

m=re.search(r"versionCode\s+(\d+)",s)
if m:
    old=int(m.group(1))
    s=s[:m.start()]+f"versionCode {old+1}"+s[m.end():]

s=re.sub(
    r"versionName\s+['\"][^'\"]+['\"]",
    "versionName '9.2.3'",
    s,
    count=1
)

p.write_text(s,encoding="utf-8")
PY

echo "[5/7] BUILD APK"

chmod +x ./gradlew

./gradlew clean :app:assembleDebug --no-daemon

[ -f "$APK" ] || {
  echo "❌ BUILD ผ่านแต่ไม่พบ APK"
  exit 1
}

mkdir -p "$PROJECT/releases"
mkdir -p "$HOME/storage/downloads"

cp -f "$APK" "$OUT"
cp -f "$APK" "$DOWNLOAD"

chmod 644 "$OUT" "$DOWNLOAD"

echo "[6/7] Git commit + push"

git add \
  app/src/main/java/com/apomaekae/dragon/MainActivity.java \
  app/src/main/AndroidManifest.xml \
  app/build.gradle

git add -f "$OUT"

git commit -m "ApoMaeKae v9.2.3 realtime current GPS Google Maps" || true

git push origin main || \
echo "⚠️ GitHub push ไม่สำเร็จ แต่ APK สร้างแล้ว"

echo "[7/7] เสร็จ"

echo
echo "=============================================="
echo "✅ APO MAEKAE V9.2.3 READY"
echo "📍 GPS: REALTIME"
echo "🗺️ Google Maps: CURRENT LOCATION"
echo "🛡️ Zone Guard: คงระบบเดิม"
echo "🔊 Thai Voice: คงระบบเดิม"
echo "📦 APK:"
echo "$OUT"
echo
echo "📥 Downloads:"
echo "$DOWNLOAD"
echo "=============================================="

termux-open "$DOWNLOAD" 2>/dev/null || true
