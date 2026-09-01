package com.apomaekae.license;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

public class ApoMaeKaeApplication
        extends Application
        implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onCreate() {

        super.onCreate();

        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityResumed(Activity activity) {

        View root = activity.getWindow()
                .getDecorView()
                .findViewById(android.R.id.content);

        if (root instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) root;

            KeyStatusView.attach(
                    activity,
                    group
            );
        }
    }

    @Override
    public void onActivityCreated(
            Activity activity,
            Bundle savedInstanceState
    ) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(
            Activity activity,
            Bundle outState
    ) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}
