package com.apomaekae.license;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class KeyStatusView {

    private static final String TAG =
            "APO_KEY_STATUS_VIEW";

    private KeyStatusView() {}

    public static void attach(Context context) {

        if (!(context instanceof android.app.Activity)) {
            return;
        }

        android.app.Activity activity =
                (android.app.Activity) context;

        ViewGroup root =
                activity.findViewById(
                        android.R.id.content
                );

        if (root == null) {
            return;
        }

        TextView old =
                root.findViewWithTag(TAG);

        if (old != null) {
            update(context, old);
            return;
        }

        TextView view =
                new TextView(context);

        view.setTag(TAG);

        view.setTextSize(11);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setPadding(
                12,
                5,
                12,
                5
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(
                Color.argb(
                        220,
                        20,
                        20,
                        20
                )
        );

        bg.setCornerRadius(30);

        view.setBackground(bg);

        ViewGroup.LayoutParams params =
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        if (root instanceof android.widget.FrameLayout) {

            android.widget.FrameLayout.LayoutParams fp =
                    new android.widget.FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            fp.gravity =
                    Gravity.TOP |
                    Gravity.CENTER_HORIZONTAL;

            fp.topMargin = 8;

            ((android.widget.FrameLayout)
                    root).addView(
                            view,
                            fp
                    );

        } else {

            root.addView(
                    view,
                    params
            );
        }

        update(context, view);
    }

    public static void update(
            Context context,
            TextView view
    ) {

        String key =
                LicenseClient.getKey(context);

        String status =
                context
                        .getSharedPreferences(
                                "license",
                                Context.MODE_PRIVATE
                        )
                        .getString(
                                "status",
                                "inactive"
                        );

        String expires =
                LicenseClient.getExpires(
                        context
                );

        String keyVersion =
                LicenseClient.getKeyVersion(
                        context
                );

        String latestVersion =
                LicenseClient.getLatestVersion(
                        context
                );

        String expireText =
                formatDate(expires);

        String text;

        if ("active".equalsIgnoreCase(status)
                && !key.isEmpty()) {

            text =
                    "🔑 KEY: ใช้งานได้"
                    + " • หมดอายุ: "
                    + expireText
                    + " • V"
                    + (
                        keyVersion.isEmpty()
                                ? latestVersion
                                : keyVersion
                    );

        } else {

            text =
                    "🔑 KEY: "
                    + status
                    + " • หมดอายุ: "
                    + expireText;
        }

        view.setText(text);
    }

    private static String formatDate(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "ไม่ทราบ";
        }

        try {

            long millis;

            if (value.matches("\\d+")) {

                millis =
                        Long.parseLong(value);

                if (millis < 100000000000L) {
                    millis *= 1000L;
                }

            } else {

                String s =
                        value.trim()
                                .replace(
                                        "T",
                                        " "
                                )
                                .replace(
                                        "Z",
                                        ""
                                );

                SimpleDateFormat input =
                        new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.US
                        );

                millis =
                        input.parse(s)
                                .getTime();
            }

            return new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.US
            ).format(
                    new Date(millis)
            );

        } catch (Exception e) {

            return value;
        }
    }
}
