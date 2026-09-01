package com.apomaekae.license;

import android.os.CountDownTimer;
import android.widget.TextView;

public class KeyCountdown {

    private CountDownTimer timer;

    public void start(
            TextView view,
            long expiresMillis
    ) {

        if (timer != null) {
            timer.cancel();
        }

        long remain = expiresMillis - System.currentTimeMillis();

        if (remain <= 0) {
            view.setText("🔴 KEY หมดอายุแล้ว");
            return;
        }

        timer = new CountDownTimer(remain, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                long totalSeconds = millisUntilFinished / 1000;

                long days = totalSeconds / 86400;
                long hours = (totalSeconds % 86400) / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;

                view.setText(
                        "⏳ KEY หมดอายุใน " +
                        days + " วัน " +
                        hours + " ชม. " +
                        minutes + " นาที " +
                        seconds + " วินาที"
                );
            }

            @Override
            public void onFinish() {
                view.setText("🔴 KEY หมดอายุแล้ว");
            }

        }.start();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
