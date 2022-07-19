package com.yaasoosoft.car;

import android.app.Application;

import com.yaasoosoft.car.entity.Setting;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Setting.getInstance().init(this);
    }
}
