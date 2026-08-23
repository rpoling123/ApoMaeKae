package com.apomaekae.dragon;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.*;

public class MainActivity extends Activity {
    LinearLayout zonesBox;
    TextView status;
    static final String PREF = "dragon_zones";

    final String[] dragonNames = {
        "มังกรส้ม","มังกรทอง","มังกรเหลือง","มังกรฟ้า","มังกรเงิน",
        "มังกรแดง","มังกรดำ","มังกรขาว","มังกรหยก"
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        requestPermissionsIfNeeded();
    }

    void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,28,28,28);

        TextView title = new TextView(this);
        title.setText("🐉 APO MAEKAE V9\nDRAGON ZONE");
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("เลือกโซนมังกรที่ต้องการเฝ้าระวัง\nระบบตรวจ GPS แบบออฟไลน์ • Buffer 500 เมตร");
        info.setTextSize(16);
        info.setPadding(0,20,0,12);
        root.addView(info);

        status = new TextView(this);
        status.setText("สถานะ: ยังไม่เริ่มตรวจ");
        status.setTextSize(17);
        root.addView(status);

        CheckBox all = new CheckBox(this);
        all.setText("เลือกทุกโซนมังกร");
        all.setTextSize(18);
        all.setChecked(true);
        root.addView(all);

        zonesBox = new LinearLayout(this);
        zonesBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(zonesBox);

        loadDragonZones();

        all.setOnCheckedChangeListener((v, checked) -> {
            for (int i=0;i<zonesBox.getChildCount();i++) {
                ViewLike.setChecked(zonesBox.getChildAt(i), checked);
            }
        });

        Button start = new Button(this);
        start.setText("▶ เริ่มแจ้งเตือนออกนอกโซน");
        root.addView(start);

        Button stop = new Button(this);
        stop.setText("■ หยุดแจ้งเตือน");
        root.addView(stop);

        Button maps = new Button(this);
        maps.setText("🗺️ เปิด Google Maps");
        root.addView(maps);

        start.setOnClickListener(v -> {
            saveSelection();
            Intent i = new Intent(this, ZoneService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            status.setText("สถานะ: 🟢 กำลังตรวจโซนมังกรแบบเรียลไทม์");
        });

        stop.setOnClickListener(v -> {
            stopService(new Intent(this, ZoneService.class));
            status.setText("สถานะ: ⚪ หยุดตรวจแล้ว");
        });

        maps.setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("geo:13.7563,100.5018?q=Bangkok"))));

        scroll.addView(root);
        setContentView(scroll);
    }

    void loadDragonZones() {
        android.content.SharedPreferences p = getSharedPreferences(PREF,0);
        for (String name : dragonNames) {
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setTextSize(18);
            cb.setChecked(p.getBoolean(name, true));
            zonesBox.addView(cb);
        }
    }

    void saveSelection() {
        android.content.SharedPreferences.Editor e =
            getSharedPreferences(PREF,0).edit();
        for (int i=0;i<zonesBox.getChildCount();i++) {
            CheckBox cb = (CheckBox) zonesBox.getChildAt(i);
            e.putBoolean(cb.getText().toString(), cb.isChecked());
        }
        e.apply();
    }

    void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, 7001);
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7002);
        }
    }

    // Tiny helper to avoid another UI dependency.
    static class ViewLike {
        static void setChecked(android.view.View v, boolean checked) {
            if (v instanceof CheckBox) ((CheckBox)v).setChecked(checked);
        }
    }
}
