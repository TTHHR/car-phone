package com.yaasoosoft.phone;

import android.app.Application;

import com.yaasoosoft.phone.entity.Setting;


public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Setting.getInstance().init(this);
    }
}
