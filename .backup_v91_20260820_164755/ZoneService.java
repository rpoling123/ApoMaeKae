package com.apomaekae.dragon;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

public class ZoneService extends Service implements LocationListener {
    static final String CHANNEL="dragon_guard";
    static final int NOTIFY_ID=9001;
    LocationManager lm;
    NotificationManager nm;
    JSONArray dragonPolygons = new JSONArray();
    boolean lastSafe = true;
    String lastHit = "";

    final String[] dragonNames = {
        "มังกรส้ม","มังกรทอง","มังกรเหลือง","มังกรฟ้า","มังกรเงิน",
        "มังกรแดง","มังกรดำ","มังกรขาว","มังกรหยก"
    };

    @Override public void onCreate() {
        super.onCreate();
        nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT>=26) {
            nm.createNotificationChannel(new NotificationChannel(
                CHANNEL,"Dragon Zone Guard",NotificationManager.IMPORTANCE_HIGH));
        }
        try {
            InputStream in=getAssets().open("zones.json");
            byte[] b=new byte[in.available()];
            int n=in.read(b); in.close();
            JSONObject root=new JSONObject(new String(b,0,n,StandardCharsets.UTF_8));
            JSONArray groups=root.getJSONArray("groups");
            for(int g=0;g<groups.length();g++) {
                JSONObject gr=groups.getJSONObject(g);
                if(gr.getInt("id")!=3) continue;
                JSONArray ps=gr.getJSONArray("polygons");
                for(int k=0;k<ps.length();k++) {
                    JSONObject poly=ps.getJSONObject(k);
                    String name=poly.getString("name").trim();
                    for(String wanted:dragonNames) {
                        if(name.equals(wanted)) {
                            JSONObject copy=new JSONObject();
                            copy.put("name",wanted);
                            copy.put("points",poly.getJSONArray("points"));
                            dragonPolygons.put(copy);
                            break;
                        }
                    }
                }
            }
        } catch(Exception ignored) {}
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        Notification n=buildNotification("กำลังตรวจ GPS • Buffer 500 เมตร");
        startForeground(NOTIFY_ID,n);

        try {
            lm=(LocationManager)getSystemService(LOCATION_SERVICE);
            if(Build.VERSION.SDK_INT>=23 &&
               checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return START_NOT_STICKY;
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,10,this);
        } catch(Exception e) {}
        return START_STICKY;
    }

    Notification buildNotification(String text) {
        return new Notification.Builder(this, Build.VERSION.SDK_INT>=26?CHANNEL:null)
            .setContentTitle("🐉 APO MAEKAE • DRAGON ZONE")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true).build();
    }

    @Override public void onLocationChanged(Location l) {
        boolean safe=false;
        String hit="";
        android.content.SharedPreferences p=getSharedPreferences("dragon_zones",0);

        try {
            for(int i=0;i<dragonPolygons.length();i++) {
                JSONObject poly=dragonPolygons.getJSONObject(i);
                String name=poly.getString("name");
                if(!p.getBoolean(name,true)) continue;
                JSONArray pts=poly.getJSONArray("points");
                if(insideOrNear(l.getLatitude(),l.getLongitude(),pts,500)) {
                    safe=true; hit=name; break;
                }
            }
        } catch(Exception ignored) {}

        if(!safe && lastSafe) alert("⚠️ ออกนอกโซนมังกร","เกินขอบเขตที่เลือกมากกว่า 500 เมตร");
        if(safe && !lastSafe) alert("✅ กลับเข้าโซนแล้ว",hit);
        if(safe && !hit.equals(lastHit)) {
            nm.notify(NOTIFY_ID,buildNotification("อยู่ใน "+hit+" • Buffer 500 เมตร"));
            lastHit=hit;
        }
        lastSafe=safe;
    }

    void alert(String title,String text) {
        Notification n=new Notification.Builder(this,Build.VERSION.SDK_INT>=26?CHANNEL:null)
            .setContentTitle(title).setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true).build();
        nm.notify((int)(System.currentTimeMillis()%100000),n);
    }

    boolean insideOrNear(double lat,double lon,JSONArray a,double buf)throws Exception {
        int n=a.length();
        boolean inside=false;
        double min=Double.MAX_VALUE;
        for(int i=0,j=n-1;i<n;j=i++) {
            JSONArray pi=a.getJSONArray(i), pj=a.getJSONArray(j);
            double yi=pi.getDouble(0), xi=pi.getDouble(1);
            double yj=pj.getDouble(0), xj=pj.getDouble(1);
            if(((yi>lat)!=(yj>lat)) &&
               (lon<(xj-xi)*(lat-yi)/(yj-yi+1e-15)+xi)) inside=!inside;
            min=Math.min(min,segMeters(lat,lon,yi,xi,yj,xj));
        }
        return inside || min<=buf;
    }

    double segMeters(double lat,double lon,double a,double b,double c,double d) {
        double ky=111320.0;
        double kx=111320.0*Math.cos(Math.toRadians(lat));
        double px=lon*kx, py=lat*ky;
        double x1=b*kx,y1=a*ky,x2=d*kx,y2=c*ky;
        double dx=x2-x1,dy=y2-y1;
        double t=((px-x1)*dx+(py-y1)*dy)/(dx*dx+dy*dy+1e-9);
        t=Math.max(0,Math.min(1,t));
        return Math.hypot(px-(x1+t*dx),py-(y1+t*dy));
    }

    @Override public void onDestroy() {
        try { if(lm!=null) lm.removeUpdates(this); } catch(Exception ignored) {}
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
    @Override public void onProviderEnabled(String p){}
    @Override public void onProviderDisabled(String p){}
    @Override public void onStatusChanged(String p,int s,Bundle b){}
}
