package com.apo.maekae;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    CheckBox[] boxes=new CheckBox[4];
    LicenseManager license;
    TextView licenseStatus;
    EditText licenseKey;
    ScheduledExecutorService timer;

    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);
      license = new LicenseManager(this);
      boxes[0]=findViewById(R.id.g1);boxes[1]=findViewById(R.id.g2);boxes[2]=findViewById(R.id.g3);boxes[3]=findViewById(R.id.g4);
      android.content.SharedPreferences p=getSharedPreferences("z",0);
      for(int i=0;i<4;i++){boxes[i].setChecked(p.getBoolean("g"+i,true)); final int j=i; boxes[i].setOnCheckedChangeListener((x,c)->p.edit().putBoolean("g"+j,c).apply());}
      findViewById(R.id.start).setOnClickListener(v->startGuard());
      findViewById(R.id.web).setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://dragonmaps-lp7unspw.manus.space/"))));
      licenseKey=findViewById(R.id.licenseKey); licenseStatus=findViewById(R.id.licenseStatus);
      licenseKey.setText(license.getKey());
      findViewById(R.id.saveLicense).setOnClickListener(v->{license.saveKey(licenseKey.getText().toString()); checkLicense();});
      findViewById(R.id.refreshLicense).setOnClickListener(v->checkLicense());
      checkLicense();
      if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10);
      if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
    }

    void checkLicense(){ licenseStatus.setText("กำลังตรวจ License...");
      Executors.newSingleThreadExecutor().execute(()->{try{LicenseManager.Result r=license.check(); runOnUiThread(()->showLicense(r));}catch(Exception e){runOnUiThread(()->licenseStatus.setText("🔴 ตรวจ License ไม่สำเร็จ: "+e.getMessage()));}});
    }

    void showLicense(LicenseManager.Result r){
      if(!r.active){ licenseStatus.setText("🔴 LICENSE: "+r.message); return; }
      licenseStatus.setText("🟢 LICENSE ACTIVE\n"+formatRemaining(r.expiresAt));
      if(timer==null){ timer=Executors.newSingleThreadScheduledExecutor(); timer.scheduleAtFixedRate(()->runOnUiThread(()->{
        long left=license.getExpiresAt()-license.nowServer();
        if(left<=0) licenseStatus.setText("🔴 LICENSE EXPIRED");
        else licenseStatus.setText("🟢 LICENSE ACTIVE\n"+formatRemaining(license.getExpiresAt()));
      }),0,1,TimeUnit.SECONDS); }
    }

    String formatRemaining(long expiry){
      long ms=Math.max(0, expiry-license.nowServer()); long sec=ms/1000; long d=sec/86400; sec%=86400; long h=sec/3600; sec%=3600; long m=sec/60; sec%=60;
      String date=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(expiry));
      return "หมดอายุ: "+date+"\nเหลือ: "+d+" วัน "+String.format(Locale.getDefault(),"%02d:%02d:%02d",h,m,sec);
    }

    void startGuard(){
      if(license.getKey().isEmpty() || license.getExpiresAt() <= license.nowServer()){
        Toast.makeText(this,"กรุณาใส่ License Key ที่ยังใช้งานได้ก่อน",Toast.LENGTH_LONG).show();
        checkLicense(); return;
      }
      Intent i=new Intent(this,ZoneService.class); if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
      ((TextView)findViewById(R.id.status)).setText("กำลังตรวจ GPS • Buffer 500 เมตร • ทำงานเบื้องหลัง");
    }

    @Override protected void onDestroy(){if(timer!=null)timer.shutdownNow();super.onDestroy();}
}
