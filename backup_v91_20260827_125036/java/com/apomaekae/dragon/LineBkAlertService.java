package com.apomaekae.dragon;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import java.io.*;
import java.net.*;
import java.util.regex.*;

public class LineBkAlertService extends NotificationListenerService {
    private static final String SERVER = "https://apomaekae-2.onrender.com/api/payment/linebk-alert";
    private static final String APP_TOKEN = "APO_V91_LINEBK";

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        String title = "", text = "";
        try {
            Bundle e = sbn.getNotification().extras;
            title = String.valueOf(e.getCharSequence("android.title",""));
            text = String.valueOf(e.getCharSequence("android.text",""));
        } catch(Exception ignored) {}

        String all = (title + " " + text).toLowerCase();
        // Only forward notifications that look like LINE BK / money-in alerts.
        if (!(all.contains("line bk") || all.contains("linebk") || all.contains("เงินเข้า") ||
              all.contains("เงินโอนเข้า") || all.contains("รับเงิน") || all.contains("ยอดเงิน"))) return;

        final String fPkg=pkg, fTitle=title, fText=text;
        new Thread(() -> post(fPkg,fTitle,fText)).start();
    }

    private void post(String pkg,String title,String text) {
        HttpURLConnection c=null;
        try {
            URL u=new URL(SERVER);
            c=(HttpURLConnection)u.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(10000); c.setReadTimeout(10000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            c.setRequestProperty("X-App-Token",APP_TOKEN);
            String json="{\"package\":\""+esc(pkg)+"\",\"title\":\""+esc(title)+"\",\"text\":\""+esc(text)+"\"}";
            OutputStream out=c.getOutputStream();
            out.write(json.getBytes("UTF-8")); out.close();
            c.getResponseCode();
        } catch(Exception ignored) {
        } finally { if(c!=null)c.disconnect(); }
    }

    private String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");}
}
