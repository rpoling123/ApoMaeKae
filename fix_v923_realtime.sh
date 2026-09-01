#!/data/data/com.termux/files/usr/bin/bash
set -e

cd ~/ApoMaeKae

MAIN="app/src/main/java/com/apomaekae/dragon/MainActivity.java"

echo "======================================"
echo " APO MAEKAE V9.2.3 GPS FIX"
echo "======================================"

# สำรองก่อน
cp "$MAIN" "$MAIN.bak_v923_$(date +%Y%m%d_%H%M%S)"

python3 - <<'PY'
from pathlib import Path
import re

p=Path("app/src/main/java/com/apomaekae/dragon/MainActivity.java")
s=p.read_text(encoding="utf-8")

# --------------------------------------------------
# 1. ลบชุด GPS V9.2.3 ที่อาจแทรกผิดตำแหน่ง
# --------------------------------------------------

s=re.sub(
    r'\s*// APO_V923_REALTIME_GPS.*?(?=\n\s*(?:private void requestPermissionsIfNeeded|@Override protected void onDestroy|}\s*$))',
    '\n',
    s,
    flags=re.S
)

s=re.sub(
    r'\s*// APO_V923_STOP_GPS.*?(?=\n\s*@Override protected void onDestroy)',
    '\n',
    s,
    flags=re.S
)

# ลบ method ซ้ำถ้ามี
s=re.sub(
    r'\s*private void startApoRealtimeGps\(\)\s*\{.*?\n\s*\}',
    '',
    s,
    flags=re.S
)

s=re.sub(
    r'\s*private void openCurrentGoogleMaps\(\)\s*\{.*?\n\s*\}',
    '',
    s,
    flags=re.S
)

s=re.sub(
    r'\s*private void stopApoRealtimeGps\(\)\s*\{.*?\n\s*\}',
    '',
    s,
    flags=re.S
)

# --------------------------------------------------
# 2. เพิ่ม import ที่จำเป็น
# --------------------------------------------------

if "import android.location.LocationListener;" not in s:
    s=s.replace(
        "import android.location.LocationManager;",
        "import android.location.LocationManager;\nimport android.location.LocationListener;"
    )

# --------------------------------------------------
# 3. เพิ่มตัวแปร GPS หลัง class
# --------------------------------------------------

marker="public class MainActivity extends Activity {"

if "private LocationManager realtimeLocationManager;" not in s:
    fields="""
    // APO MAEKAE V9.2.3 REALTIME GPS
    private LocationManager realtimeLocationManager;
    private LocationListener realtimeLocationListener;
    private Location realtimeLocation;
"""
    s=s.replace(marker,marker+fields,1)

# --------------------------------------------------
# 4. onCreate เรียก startRealtimeGPS
# --------------------------------------------------

if "startRealtimeGPS();" not in s:
    s=s.replace(
        "showKeyPage();requestPermissionsIfNeeded();",
        "showKeyPage();requestPermissionsIfNeeded();startRealtimeGPS();",
        1
    )

# --------------------------------------------------
# 5. เปลี่ยน openMaps ให้ใช้ตำแหน่งจริง
# --------------------------------------------------

old=r'private void openMaps\(\)\{try\{startActivity\(new Intent\(Intent\.ACTION_VIEW,Uri\.parse\("https://maps\.google\.com"\)\)\);\}catch\(Exception e\)\{Toast\.makeText\(this,"เปิด Google Maps ไม่ได้",Toast\.LENGTH_LONG\)\.show\(\);\}\}'

new="""private void openMaps(){
        try{
            Location l=realtimeLocation;

            if(l==null && realtimeLocationManager!=null){
                if(Build.VERSION.SDK_INT<23 ||
                   checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                   ==PackageManager.PERMISSION_GRANTED){

                    l=realtimeLocationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER);

                    if(l==null){
                        l=realtimeLocationManager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER);
                    }
                }
            }

            if(l==null){
                Toast.makeText(this,
                        "ยังไม่พบตำแหน่ง GPS ปัจจุบัน",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String uri="google.navigation:q="
                    +l.getLatitude()+","+l.getLongitude();

            Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(uri));
            i.setPackage("com.google.android.apps.maps");

            try{
                startActivity(i);
            }catch(Exception e){
                Intent web=new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://www.google.com/maps/search/?api=1&query="
                            +l.getLatitude()+","+l.getLongitude()
                        )
                );
                startActivity(web);
            }

        }catch(Exception e){
            Toast.makeText(this,
                    "เปิด Google Maps ไม่ได้",
                    Toast.LENGTH_LONG).show();
        }
    }"""

s=re.sub(old,new,s,count=1)

# --------------------------------------------------
# 6. เพิ่ม REALTIME GPS ก่อน requestPermissions
# --------------------------------------------------

gps_code="""
    // ==================================================
    // APO MAEKAE V9.2.3 REALTIME GPS
    // ==================================================

    private void startRealtimeGPS(){
        try{
            realtimeLocationManager=
                    (LocationManager)getSystemService(LOCATION_SERVICE);

            realtimeLocationListener=new LocationListener(){

                @Override
                public void onLocationChanged(Location location){
                    if(location==null)return;

                    realtimeLocation=location;

                    runOnUiThread(()->{
                        try{
                            if(gps!=null){
                                gps.setText(
                                    String.format(
                                        Locale.US,
                                        "📍 GPS ปัจจุบัน: %.6f, %.6f\\nความแม่นยำ %.0f เมตร • REALTIME",
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        location.hasAccuracy()
                                            ? location.getAccuracy()
                                            : 0
                                    )
                                );
                            }
                        }catch(Exception ignored){}
                    });
                }

                @Override public void onProviderEnabled(String provider){}
                @Override public void onProviderDisabled(String provider){}
            };

            if(Build.VERSION.SDK_INT>=23 &&
               checkSelfPermission(
                   Manifest.permission.ACCESS_FINE_LOCATION)
                   !=PackageManager.PERMISSION_GRANTED){
                return;
            }

            realtimeLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    3.0f,
                    realtimeLocationListener
            );

            Location last=
                realtimeLocationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER);

            if(last==null){
                last=
                    realtimeLocationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER);
            }

            if(last!=null){
                realtimeLocation=last;
            }

        }catch(Exception e){
            try{
                if(gps!=null)
                    gps.setText("📍 GPS: รอสิทธิ์ตำแหน่ง");
            }catch(Exception ignored){}
        }
    }

    private void stopRealtimeGPS(){
        try{
            if(realtimeLocationManager!=null &&
               realtimeLocationListener!=null){
                realtimeLocationManager.removeUpdates(
                    realtimeLocationListener
                );
            }
        }catch(Exception ignored){}
    }

"""

if "private void startRealtimeGPS()" not in s:
    marker="    private void requestPermissionsIfNeeded(){"
    if marker in s:
        s=s.replace(marker,gps_code+marker,1)

# --------------------------------------------------
# 7. แก้ onDestroy
# --------------------------------------------------

s=re.sub(
    r'@Override protected void onDestroy\(\)\s*\{.*?\}',
    '''@Override
    protected void onDestroy(){
        stopRealtimeGPS();
        executor.shutdownNow();
        super.onDestroy();
    }''',
    s,
    count=1,
    flags=re.S
)

p.write_text(s,encoding="utf-8")

print("JAVA PATCH OK")
PY

echo
echo "ตรวจ syntax โครงสร้าง..."
grep -n "startRealtimeGPS\|stopRealtimeGPS\|openMaps\|realtimeLocation" "$MAIN" || true

echo
echo "BUILD APK..."
chmod +x ./gradlew
./gradlew clean :app:assembleDebug --no-daemon

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "❌ ไม่พบ APK"
    exit 1
fi

mkdir -p releases
cp -f "$APK" "releases/ApoMaeKae-v9.2.3-REALTIME-GPS.apk"

mkdir -p "$HOME/storage/downloads"
cp -f "$APK" \
"$HOME/storage/downloads/ApoMaeKae-v9.2.3-REALTIME-GPS.apk"

echo
echo "======================================"
echo "✅ BUILD SUCCESSFUL"
echo "📍 REALTIME GPS"
echo "🗺️ GOOGLE MAPS CURRENT LOCATION"
echo "🛡️ ZONE GUARD เดิม"
echo "🔊 THAI VOICE เดิม"
echo
echo "APK:"
echo "$HOME/storage/downloads/ApoMaeKae-v9.2.3-REALTIME-GPS.apk"
echo "======================================"

termux-open \
"$HOME/storage/downloads/ApoMaeKae-v9.2.3-REALTIME-GPS.apk" \
2>/dev/null || true
