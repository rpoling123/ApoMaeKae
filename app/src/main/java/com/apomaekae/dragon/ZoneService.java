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
    boolean lastSafe=true;
    String lastHit="";
    NotificationManager nm;
    TextToSpeech tts;

    @Override public void onCreate(){
        super.onCreate();
        nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT>=26){
            nm.createNotificationChannel(new NotificationChannel(
                "guard","Zone Guard",NotificationManager.IMPORTANCE_HIGH));
        }

        tts=new TextToSpeech(this, status -> {
            if(status==TextToSpeech.SUCCESS) tts.setLanguage(new Locale("th","TH"));
        });

        try{
            InputStream in=getAssets().open("zones.json");
            byte[] b=new byte[in.available()];
            in.read(b); in.close();
            groups=new JSONObject(new String(b,"UTF-8")).getJSONArray("groups");
        }catch(Exception ignored){}
    }

    @Override public int onStartCommand(Intent i,int flags,int id){
        Notification n;
        if(Build.VERSION.SDK_INT>=26){
            n=new Notification.Builder(this,"guard")
                .setContentTitle("อาโปแมะเก๊ V9.1")
                .setContentText("กำลังตรวจโซนมังกร • Buffer 500 เมตร")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true).build();
        }else{
            n=new Notification.Builder(this)
                .setContentTitle("อาโปแมะเก๊ V9.1")
                .setContentText("กำลังตรวจโซนมังกร • Buffer 500 เมตร")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true).build();
        }
        startForeground(77,n);

        try{
            LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
            if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                ==android.content.pm.PackageManager.PERMISSION_GRANTED){
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,5,this);
            }
        }catch(Exception ignored){}

        return START_STICKY;
    }

    @Override public void onLocationChanged(Location l){
        boolean safe=false;
        String hit="";

        SharedPreferences p=getSharedPreferences("z",0);

        try{
            for(int g=0;g<groups.length();g++){
                JSONObject gr=groups.getJSONObject(g);
                JSONArray ps=gr.getJSONArray("polygons");

                for(int k=0;k<ps.length();k++){
                    boolean selected=p.getBoolean("z_"+g+"_"+k, false);

                    // Backward compatibility: if no individual dragon setting exists,
                    // group selection can still be used.
                    String prefKey="z_"+g+"_"+k;
                    if(!p.contains(prefKey) && p.getBoolean("g"+g,true)) selected=true;

                    if(!selected) continue;

                    JSONObject poly=ps.getJSONObject(k);
                    JSONArray a=poly.getJSONArray("points");

                    if(insideOrNear(l.getLatitude(),l.getLongitude(),a,500)){
                        safe=true;
                        hit=poly.optString("name",gr.optString("name","โซน"));
                        break;
                    }
                }
                if(safe)break;
            }
        }catch(Exception ignored){}

        if(!safe && lastSafe){
            alert("ออกนอกโซน","ตำแหน่งอยู่นอกโซนที่เลือกเกิน 500 เมตร");
            speak("แจ้งเตือน คุณอยู่นอกโซน");
        }

        if(safe && !lastSafe){
            alert("เข้าโซนแล้ว",hit);
            speak("เข้าโซนแล้ว "+hit);
        }

        if(safe && !hit.equals(lastHit) && !lastHit.isEmpty()){
            speak("เข้าสู่ "+hit);
        }

        lastSafe=safe;
        lastHit=hit;
    }

    private void speak(String s){
        try{
            if(tts!=null) tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"apo_zone");
        }catch(Exception ignored){}
    }

    private void alert(String title,String msg){
        Notification n;
        if(Build.VERSION.SDK_INT>=26){
            n=new Notification.Builder(this,"guard")
                .setContentTitle(title).setContentText(msg)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true).build();
        }else{
            n=new Notification.Builder(this)
                .setContentTitle(title).setContentText(msg)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true).build();
        }
        nm.notify((int)(System.currentTimeMillis()%100000),n);
    }

    private boolean insideOrNear(double lat,double lon,JSONArray a,double buf)throws Exception{
        int n=a.length();
        boolean c=false;
        double min=1e18;

        for(int i=0,j=n-1;i<n;j=i++){
            JSONArray pi=a.getJSONArray(i),pj=a.getJSONArray(j);
            double yi=pi.getDouble(0),xi=pi.getDouble(1);
            double yj=pj.getDouble(0),xj=pj.getDouble(1);

            if(((yi>lat)!=(yj>lat)) &&
              (lon<(xj-xi)*(lat-yi)/(yj-yi+1e-15)+xi)) c=!c;

            double dx=(xi-lon)*111320.0*Math.cos(Math.toRadians(lat));
            double dy=(yi-lat)*111320.0;
            min=Math.min(min,Math.sqrt(dx*dx+dy*dy));
        }
        return c || min<=buf;
    }

    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onProviderEnabled(String p){}
    @Override public void onProviderDisabled(String p){}
    @Override public void onStatusChanged(String p,int s,Bundle b){}
    @Override public void onDestroy(){
        try{if(tts!=null){tts.stop();tts.shutdown();}}catch(Exception ignored){}
        super.onDestroy();
    }
}
