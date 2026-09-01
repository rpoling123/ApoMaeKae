package com.apomaekae.dragon;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;

public class RealtimeGpsService extends Service {

    private static final String CHANNEL_ID = "APO_GPS_REALTIME";
    private LocationManager locationManager;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Intent i = new Intent("APO_GPS_UPDATE");
            i.putExtra("lat", location.getLatitude());
            i.putExtra("lon", location.getLongitude());
            i.putExtra("accuracy", location.getAccuracy());
            i.putExtra("time", location.getTime());
            sendBroadcast(i);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("APO MAE KAE")
                        .setContentText("📍 GPS REALTIME กำลังทำงานเบื้องหลัง")
                        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                        .setOngoing(true)
                        .build();

        startForeground(9001, notification);

        startGps();
    }

    private void startGps() {

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                stopSelf();
                return;
            }
        }

        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    1,
                    locationListener
            );
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "APO MAE KAE GPS",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "GPS REALTIME ของ APO MAE KAE"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {

        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
