package com.apo.maekae;
import android.app.*; import android.content.*; import android.location.*; import android.os.*; 
import org.json.*; import java.io.*; import java.util.*;
public class ZoneService extends Service implements LocationListener {
 JSONArray groups; boolean lastSafe=true; NotificationManager nm;
 public void onCreate(){super.onCreate(); nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26){nm.createNotificationChannel(new NotificationChannel("guard","Zone Guard",NotificationManager.IMPORTANCE_HIGH));}
  try{InputStream in=getAssets().open("zones.json"); byte[] b=new byte[in.available()];in.read(b); groups=new JSONObject(new String(b,"UTF-8")).getJSONArray("groups");}catch(Exception e){}
 }
 public int onStartCommand(Intent i,int f,int id){
  Notification n=new Notification.Builder(this,Build.VERSION.SDK_INT>=26?"guard":"").setContentTitle("อาโปแมะเก๊")
   .setContentText("กำลังตรวจโซน GPS • Buffer 500 เมตร").setSmallIcon(android.R.drawable.ic_menu_mylocation).build();
  startForeground(77,n);
  try{LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE); lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,10,this);}catch(SecurityException e){}
  return START_STICKY;
 }
 public void onLocationChanged(Location l){boolean safe=false; String hit="";
  android.content.SharedPreferences p=getSharedPreferences("z",0);
  try{for(int g=0;g<groups.length();g++){if(!p.getBoolean("g"+g,true))continue; JSONObject gr=groups.getJSONObject(g);JSONArray ps=gr.getJSONArray("polygons");
    for(int k=0;k<ps.length();k++){JSONArray a=ps.getJSONObject(k).getJSONArray("points"); if(insideOrNear(l.getLatitude(),l.getLongitude(),a,500)){safe=true;hit=gr.getString("name");break;}} if(safe)break;}}
  catch(Exception e){}
  if(!safe && lastSafe) alert("ออกนอกโซน","ตำแหน่งอยู่นอก Polygon ที่เลือกเกิน 500 เมตร");
  if(safe && !lastSafe) alert("กลับเข้าโซนแล้ว",hit);
  lastSafe=safe;
 }
 void alert(String t,String x){Notification n=new Notification.Builder(this,Build.VERSION.SDK_INT>=26?"guard":"").setContentTitle(t).setContentText(x)
   .setSmallIcon(android.R.drawable.ic_dialog_alert).setAutoCancel(true).build();nm.notify((int)(System.currentTimeMillis()%100000),n);}
 boolean insideOrNear(double lat,double lon,JSONArray a,double buf)throws Exception{
  int n=a.length(); boolean c=false; double min=1e18;
  for(int i=0,j=n-1;i<n;j=i++){JSONArray pi=a.getJSONArray(i),pj=a.getJSONArray(j);double yi=pi.getDouble(0),xi=pi.getDouble(1),yj=pj.getDouble(0),xj=pj.getDouble(1);
   if(((yi>lat)!=(yj>lat))&&(lon<(xj-xi)*(lat-yi)/(yj-yi+1e-15)+xi))c=!c;
   min=Math.min(min,segMeters(lat,lon,yi,xi,yj,xj));
  } return c||min<=buf;
 }
 double segMeters(double lat,double lon,double a,double b,double c,double d){
  double ky=111320, kx=111320*Math.cos(Math.toRadians(lat));double px=lon*kx,py=lat*ky,x1=b*kx,y1=a*ky,x2=d*kx,y2=c*ky;
  double dx=x2-x1,dy=y2-y1,t=((px-x1)*dx+(py-y1)*dy)/(dx*dx+dy*dy+1e-9);t=Math.max(0,Math.min(1,t));
  return Math.hypot(px-(x1+t*dx),py-(y1+t*dy));
 }
 public void onProviderEnabled(String p){} public void onProviderDisabled(String p){} public void onStatusChanged(String p,int s,Bundle b){}
 public android.os.IBinder onBind(Intent i){return null;}
}