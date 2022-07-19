package com.yaasoosoft.car.entity;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

public class Setting {
    public int screenW,screenH;
    public int screenDPI;
    public int BIT;
    public int FPS;
    static Setting setting=new Setting();
    public static Setting getInstance() {
        return setting;
    }

    public void init(Context context)
    {

        WindowManager wm = ((WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE));
        Display display = wm.getDefaultDisplay();
        Point screenSize = new Point();
        display.getRealSize(screenSize);
        screenW = screenSize.x;
        screenH = screenSize.y;

        screenDPI = context.getResources().getDisplayMetrics().densityDpi;
        BIT = 3000000;
        FPS = 30;//FPS
    }
}
