package com.yaasoosoft.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UsbReceiver extends BroadcastReceiver {
    private static final String TAG = "UsbDetached";
    private UsbListener mUsbListener;

    public UsbReceiver(UsbListener usbListener) {
        mUsbListener = usbListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG,"attached "+intent.getAction());
        mUsbListener.usbDetached();
    }

    public interface UsbListener {
        /**
         * usb断开连接
         */
        void usbDetached();
    }
}
