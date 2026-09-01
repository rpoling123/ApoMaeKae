package com.apomaekae.license;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class KeyStatusView {

    private static final String TAG = "APO_KEY_STATUS";

    private KeyStatusView() {}

    public static void attach(Context context, ViewGroup root) {

        if (root == null) {
            return;
        }

        TextView old = root.findViewWithTag(TAG);

        if (old != null) {
            update(context, old);
            return;
        }

        TextView view = new TextView(context);

        view.setTag(TAG);
        view.setTextSize(11);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(18, 4, 18, 4);
        view.setBackgroundColor(Color.argb(210, 0, 100, 70));

        ViewGroup.LayoutParams lp =
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        root.addView(view, lp);

        update(context, view);
    }

    public static void update(Context context, TextView view) {

        String key = LicenseClient.getKey(context);
        String expires = LicenseClient.getExpires(context);
        String status = context
                .getSharedPreferences(
                        "license",
                        Context.MODE_PRIVATE
                )
                .getString("status", "inactive");

        String keyVersion =
                LicenseClient.getKeyVersion(context);

        if (key == null || key.trim().isEmpty()) {

            view.setText(
                    "🔑 KEY: ยังไม่ได้ลงทะเบียน"
            );

            return;
        }

        String expiryText =
                formatExpiry(expires);

        String text =
                "🔑 KEY: " +
                status.toUpperCase(Locale.US) +
                "  •  หมดอายุ: " +
                expiryText;

        if (keyVersion != null &&
                !keyVersion.isEmpty()) {

            text +=
                    "  •  Version: " +
                    keyVersion;
        }

        view.setText(text);
    }

    private static String formatExpiry(String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "ไม่ทราบ";
        }

        try {

            long time;

            if (value.matches("[0-9]+")) {

                time = Long.parseLong(value);

                if (time < 100000000000L) {
                    time *= 1000L;
                }

            } else {

                String s = value.trim();

                String[] formats = {
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        "yyyy-MM-dd HH:mm:ss",
                        "yyyy-MM-dd"
                };

                Date parsed = null;

                for (String f : formats) {

                    try {

                        SimpleDateFormat sdf =
                                new SimpleDateFormat(
                                        f,
                                        Locale.US
                                );

                        sdf.setLenient(false);

                        parsed = sdf.parse(s);

                        if (parsed != null) {
                            break;
                        }

                    } catch (Exception ignored) {
                    }
                }

                if (parsed == null) {
                    return value;
                }

                time = parsed.getTime();
            }

            SimpleDateFormat out =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault()
                    );

            return out.format(new Date(time));

        } catch (Exception e) {

            return value;
        }
    }
}
