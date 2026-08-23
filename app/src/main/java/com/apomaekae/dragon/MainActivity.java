package com.apomaekae.dragon;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.apomaekae.license.LicenseClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LinearLayout root;
    private EditText keyEdit;
    private TextView licenseStatus, expiresText, remainText, gpsText;
    private LinearLayout zonesBox;
    private final Handler handler = new Handler();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        showLicensePage();
        requestPermissionsIfNeeded();
    }

    private void showLicensePage() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 18, 22, 18);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("อาโปแมะเก๊ • V9.1", 26, Gravity.CENTER);
        root.addView(title, full());

        TextView waiting = text("กำลังตรวจระบบ KEY / GPS", 17, Gravity.CENTER);
        root.addView(waiting, full());

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 18, 18, 18);
        card.setBackgroundColor(0xFFF1F3F2);
        root.addView(card, full());

        TextView head = text("🔑 LICENSE / KEY", 25, Gravity.LEFT);
        card.addView(head, full());

        keyEdit = new EditText(this);
        keyEdit.setText(LicenseClient.getKey(this));
        keyEdit.setTextSize(20);
        keyEdit.setSingleLine(true);
        keyEdit.setHint("ใส่ KEY");
        keyEdit.setInputType(InputType.TYPE_CLASS_TEXT);
        card.addView(keyEdit, full());

        licenseStatus = text("⚪ LICENSE ยังไม่ได้ตรวจ", 18, Gravity.LEFT);
        card.addView(licenseStatus, full());

        expiresText = text("หมดอายุ: -", 17, Gravity.LEFT);
        card.addView(expiresText, full());

        remainText = text("เหลือ: -", 17, Gravity.LEFT);
        card.addView(remainText, full());

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button check = new Button(this);
        check.setText("บันทึก / ตรวจ KEY");
        Button refresh = new Button(this);
        refresh.setText("รีเฟรช");
        buttons.addView(check, new LinearLayout.LayoutParams(0,-2,1));
        buttons.addView(refresh, new LinearLayout.LayoutParams(0,-2,1));
        card.addView(buttons, full());

        check.setOnClickListener(v -> verifyKey());
        refresh.setOnClickListener(v -> verifyKey());

        String saved = LicenseClient.getKey(this);
        if (!saved.isEmpty()) verifyKey();

        TextView sep = text("────────────────────────", 16, Gravity.CENTER);
        root.addView(sep, full());
    }

    private void verifyKey() {
        final String key = keyEdit.getText().toString().trim();
        if (key.isEmpty()) {
            licenseStatus.setText("🔴 กรุณาใส่ KEY");
            return;
        }

        licenseStatus.setText("🟡 กำลังตรวจ KEY...");
        executor.execute(() -> {
            LicenseClient.Result r = LicenseClient.verify(this, key);
            runOnUiThread(() -> {
                licenseStatus.setText(r.ok ? "🟢 LICENSE ACTIVE" : "🔴 " + r.message);
                expiresText.setText("หมดอายุ: " + (r.expires.isEmpty() ? "-" : formatDate(r.expires)));
                updateRemaining(r.expires);
                if (r.ok) showZonePage();
            });
        });
    }

    private void showZonePage() {
        root.removeViews(2, Math.max(0, root.getChildCount()-2));

        TextView h = text("🐉 DRAGON ZONE V9.1", 24, Gravity.CENTER);
        root.addView(h, full());

        TextView sub = text("เลือกโซนมังกรที่ต้องการเฝ้าระวัง • Buffer 500 เมตร", 16, Gravity.LEFT);
        root.addView(sub, full());

        gpsText = text("สถานะ: ยังไม่เริ่มตรวจ GPS", 17, Gravity.LEFT);
        root.addView(gpsText, full());

        CheckBox all = new CheckBox(this);
        all.setText("เลือกทุกโซนมังกร");
        all.setTextSize(18);
        root.addView(all, full());

        zonesBox = new LinearLayout(this);
        zonesBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(zonesBox, full());

        loadDragonZones();

        all.setOnCheckedChangeListener((v, checked) -> {
            for (int i=0;i<zonesBox.getChildCount();i++) {
                View x=zonesBox.getChildAt(i);
                if (x instanceof CheckBox) ((CheckBox)x).setChecked(checked);
            }
        });

        Button start = new Button(this);
        start.setText("▶ เริ่มตรวจโซน GPS");
        root.addView(start, full());

        Button stop = new Button(this);
        stop.setText("■ หยุดแจ้งเตือน");
        root.addView(stop, full());

        start.setOnClickListener(v -> startGuard());
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, ZoneService.class));
            gpsText.setText("สถานะ: หยุดแจ้งเตือน");
        });

        startRemainTimer();
    }

    private void loadDragonZones() {
        try {
            InputStream in = getAssets().open("dragon_zones.json");
            BufferedReader br =
                new BufferedReader(
                    new InputStreamReader(in, "UTF-8")
                );

            StringBuilder s = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null)
                s.append(line);

            br.close();

            JSONObject root = new JSONObject(s.toString());
            JSONArray zones = root.getJSONArray("groups");

            java.util.LinkedHashSet<String> seen =
                new java.util.LinkedHashSet<>();

            int count = 0;

            for (int i = 0; i < zones.length(); i++) {

                JSONObject z = zones.getJSONObject(i);

                String name =
                    z.optString("name", "").trim();

                if (name.isEmpty())
                    continue;

                String key =
                    name.replaceAll("\\s+", " ")
                        .trim()
                        .toLowerCase(
                            java.util.Locale.ROOT
                        );

                // ตัดชื่อซ้ำ
                if (seen.contains(key))
                    continue;

                seen.add(key);

                CheckBox cb = new CheckBox(this);

                cb.setText("🐉 " + name);
                cb.setTextSize(18);
                cb.setPadding(8, 8, 8, 8);

                final String prefKey =
                    "dragon_name_" +
                    Math.abs(key.hashCode());

                android.content.SharedPreferences pref =
                    getSharedPreferences("z", 0);

                cb.setChecked(
                    pref.getBoolean(prefKey, true)
                );

                cb.setOnCheckedChangeListener(
                    (v, checked) ->
                        pref.edit()
                            .putBoolean(prefKey, checked)
                            .apply()
                );

                zonesBox.addView(cb);

                count++;
            }

            if (count == 0) {
                TextView none =
                    text(
                        "⚠️ ไม่พบโซนจาก KMZ ล่าสุด",
                        17,
                        Gravity.LEFT
                    );

                zonesBox.addView(none, full());
            }

        } catch (Exception e) {

            TextView err =
                text(
                    "❌ อ่านรายการโซนไม่ได้\n" +
                    e.getMessage(),
                    16,
                    Gravity.LEFT
                );

            zonesBox.addView(err, full());
        }
    }


    private void startGuard() {
        Intent i=new Intent(this,ZoneService.class);
        if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
        gpsText.setText("🟢 กำลังตรวจ GPS • Buffer 500 เมตร • ทำงานเบื้องหลัง");
    }

    private void requestPermissionsIfNeeded() {
        if(Build.VERSION.SDK_INT>=23 &&
           checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION},10);
        }
        if(Build.VERSION.SDK_INT>=33 &&
           checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
        }
    }

    private void updateRemaining(String exp) {
        if(exp==null || exp.isEmpty()){remainText.setText("เหลือ: -");return;}
        try {
            Date d=parseDate(exp);
            long ms=d.getTime()-System.currentTimeMillis();
            if(ms<=0){remainText.setText("เหลือ: หมดอายุ");return;}
            long sec=ms/1000, days=sec/86400; sec%=86400;
            long h=sec/3600; sec%=3600; long m=sec/60; long s=sec%60;
            remainText.setText(String.format(Locale.getDefault(),
                "เหลือ: %d วัน %02d:%02d:%02d",days,h,m,s));
        } catch(Exception e){remainText.setText("เหลือ: "+exp);}
    }

    private void startRemainTimer(){
        handler.postDelayed(new Runnable(){
            @Override public void run(){
                updateRemaining(LicenseClient.getExpires(MainActivity.this));
                handler.postDelayed(this,1000);
            }
        },1000);
    }

    private String formatDate(String s){
        try {
            Date d=parseDate(s);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.getDefault()).format(d);
        } catch(Exception e){return s;}
    }

    private Date parseDate(String s)throws Exception{
        String x=s;
        if(x.endsWith("Z")) x=x.substring(0,x.length()-1);
        if(x.contains("T")) x=x.replace("T"," ");
        if(x.contains(".")) x=x.substring(0,x.indexOf('.'));
        if(x.length()>19)x=x.substring(0,19);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).parse(x);
    }

    private TextView text(String s,float size,int gravity){
        TextView t=new TextView(this);
        t.setText(s); t.setTextSize(size); t.setGravity(gravity); t.setPadding(0,8,0,8);
        return t;
    }

    private LinearLayout.LayoutParams full(){
        return new LinearLayout.LayoutParams(-1,-2);
    }

    @Override protected void onDestroy(){
        executor.shutdownNow();
        super.onDestroy();
    }

    private void openBuyKey() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://apomaekae-2.onrender.com/buy-key.html"));
            startActivity(i);
        } catch (Throwable e) {
            Toast.makeText(this, "เปิดหน้าซื้อ Key ไม่ได้", Toast.LENGTH_LONG).show();
        }
    }

}
