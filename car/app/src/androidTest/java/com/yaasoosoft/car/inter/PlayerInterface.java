package com.yaasoosoft.car.inter;

import android.content.Context;

public interface PlayerInterface {
    public Context getThis();
    public void onFrame(byte[] buff);
}
