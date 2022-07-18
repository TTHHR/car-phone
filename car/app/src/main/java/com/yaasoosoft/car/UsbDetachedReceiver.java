package com.yaasoosoft.car;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UsbDetachedReceiver extends BroadcastReceiver {

    private static final String TAG = "UsbDetached";
    private UsbDetachedListener mUsbDetachedListener;

    public UsbDetachedReceiver(UsbDetachedListener usbDetachedListener) {
        mUsbDetachedListener = usbDetachedListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG,"attached "+intent.getAction());
        mUsbDetachedListener.usbDetached();
    }

    public interface UsbDetachedListener {
        /**
         * usb断开连接
         */
        void usbDetached();
    }
}
