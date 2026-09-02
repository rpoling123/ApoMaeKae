package com.apomaekae.dragon;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.speech.tts.TextToSpeech;
import org.json.*;
import java.io.*;
import java.util.*;

public class ZoneService extends Service implements LocationListener {

    JSONArray groups;
    boolean lastInside = true;
    boolean warned100m = false;
    String lastHit = "";
    NotificationManager nm;
    TextToSpeech tts;

    private static final double WARNING_DISTANCE = 100.0;

    @Override public void onCreate() {
        super.onCreate();

        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        /*
         * Foreground notification ใช้ LOW
         * ไม่มีเสียง / ไม่มี popup
         * การแจ้งเตือนใช้ TTS แยกต่างหาก
         */
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                "guard",
                "Zone Guard",
                NotificationManager.IMPORTANCE_LOW
            );
            ch.setDescription("Dragon Zone Guard");
            nm.createNotificationChannel(ch);
        }

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("th", "TH"));
                if (r == TextToSpeech.LANG_MISSING_DATA ||
                    r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
            }
        });

        try {
            InputStream in = getAssets().open("zones.json");
            byte[] b = new byte[in.available()];
            in.read(b);
            in.close();

            groups = new JSONObject(
                new String(b, "UTF-8")
            ).getJSONArray("groups");

        } catch (Exception ignored) {
            groups = new JSONArray();
        }
    }

    @Override public int onStartCommand(Intent i, int flags, int id) {

        Notification n;

        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, "guard")
                .setContentTitle("อาโปแมะเก๊ V9.1")
                .setContentText("กำลังตรวจโซนมังกร • แจ้งเตือนด้วยเสียง")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();

        } else {
            n = new Notification.Builder(this)
                .setContentTitle("อาโปแมะเก๊ V9.1")
                .setContentText("กำลังตรวจโซนมังกร • แจ้งเตือนด้วยเสียง")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
        }

        startForeground(77, n);

        try {
            LocationManager lm =
                (LocationManager)getSystemService(LOCATION_SERVICE);

            if (checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    this
                );
            }

        } catch (Exception ignored) {}

        return START_STICKY;
    }

    @Override public void onLocationChanged(Location l) {

        boolean inside = false;
        String hit = "";
        double nearestDistance = Double.MAX_VALUE;

        SharedPreferences p =
            getSharedPreferences("z", 0);

        try {

            for (int g = 0; g < groups.length(); g++) {

                JSONObject gr = groups.getJSONObject(g);
                JSONArray ps = gr.getJSONArray("polygons");

                for (int k = 0; k < ps.length(); k++) {

                    String prefKey = "z_" + g + "_" + k;

                    boolean selected =
                        p.getBoolean(prefKey, false);

                    if (!p.contains(prefKey) &&
                        p.getBoolean("g" + g, true)) {
                        selected = true;
                    }

                    if (!selected) continue;

                    JSONObject poly =
                        ps.getJSONObject(k);

                    JSONArray a =
                        poly.getJSONArray("points");

                    boolean thisInside =
                        isInside(
                            l.getLatitude(),
                            l.getLongitude(),
                            a
                        );

                    double distance =
                        distanceToPolygon(
                            l.getLatitude(),
                            l.getLongitude(),
                            a
                        );

                    if (thisInside) {
                        inside = true;
                        hit = poly.optString(
                            "name",
                            gr.optString("name", "โซน")
                        );
                    }

                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                    }
                }
            }

        } catch (Exception ignored) {}

        /*
         * ==============================
         * เตือนก่อนออกโซน 100 เมตร
         * ==============================
         */
        if (inside &&
            nearestDistance <= WARNING_DISTANCE &&
            nearestDistance > 0 &&
            !warned100m) {

            speak("ระวัง อีก 100 เมตรจะออกโซน");
            warned100m = true;
        }

        /*
         * ==============================
         * ออกโซนจริง
         * ==============================
         */
        if (!inside && lastInside) {

            speak("แจ้งเตือน คุณอยู่นอกโซน");
            warned100m = false;
        }

        /*
         * ==============================
         * กลับเข้าโซนตรงเส้น
         * ==============================
         */
        if (inside && !lastInside) {

            speak("เข้าโซนแล้ว " + hit);
            warned100m = false;
        }

        /*
         * เปลี่ยนโซน
         */
        if (inside &&
            !hit.equals(lastHit) &&
            !lastHit.isEmpty()) {

            speak("เข้าสู่ " + hit);
            warned100m = false;
        }

        lastInside = inside;
        lastHit = hit;
    }

    private void speak(String text) {

        try {

            if (tts != null) {

                tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "apo_zone_" + System.currentTimeMillis()
                );
            }

        } catch (Exception ignored) {}
    }

    /*
     * ตรวจว่า GPS อยู่ภายใน Polygon จริงหรือไม่
     * ไม่มี Buffer 500 เมตร
     */
    private boolean isInside(
        double lat,
        double lon,
        JSONArray a
    ) throws Exception {

        int n = a.length();
        boolean c = false;

        for (int i = 0, j = n - 1; i < n; j = i++) {

            JSONArray pi = a.getJSONArray(i);
            JSONArray pj = a.getJSONArray(j);

            double yi = pi.getDouble(0);
            double xi = pi.getDouble(1);

            double yj = pj.getDouble(0);
            double xj = pj.getDouble(1);

            if (((yi > lat) != (yj > lat)) &&
                (lon <
                (xj - xi) *
                (lat - yi) /
                (yj - yi + 1e-15) + xi)) {

                c = !c;
            }
        }

        return c;
    }

    /*
     * หาระยะจาก GPS ถึงเส้น Polygon
     * หน่วยเมตร
     */
    private double distanceToPolygon(
        double lat,
        double lon,
        JSONArray a
    ) throws Exception {

        int n = a.length();
        double min = Double.MAX_VALUE;

        for (int i = 0; i < n; i++) {

            JSONArray p1 = a.getJSONArray(i);
            JSONArray p2 = a.getJSONArray((i + 1) % n);

            double lat1 = p1.getDouble(0);
            double lon1 = p1.getDouble(1);

            double lat2 = p2.getDouble(0);
            double lon2 = p2.getDouble(1);

            double dx =
                (lon2 - lon1) *
                111320.0 *
                Math.cos(Math.toRadians(lat));

            double dy =
                (lat2 - lat1) *
                111320.0;

            double px =
                (lon - lon1) *
                111320.0 *
                Math.cos(Math.toRadians(lat));

            double py =
                (lat - lat1) *
                111320.0;

            double len2 = dx * dx + dy * dy;

            double t = 0;

            if (len2 > 0) {
                t = (px * dx + py * dy) / len2;
                t = Math.max(0, Math.min(1, t));
            }

            double cx = dx * t;
            double cy = dy * t;

            double dist =
                Math.sqrt(
                    (px - cx) * (px - cx) +
                    (py - cy) * (py - cy)
                );

            min = Math.min(min, dist);
        }

        return min;
    }

    @Override public IBinder onBind(Intent i) {
        return null;
    }

    @Override public void onProviderEnabled(String p) {}

    @Override public void onProviderDisabled(String p) {}

    @Override public void onStatusChanged(
        String p,
        int s,
        Bundle b
    ) {}

    @Override public void onDestroy() {

        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
        } catch (Exception ignored) {}

        super.onDestroy();
    }
}
