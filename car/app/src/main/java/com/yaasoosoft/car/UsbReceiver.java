package com.yaasoosoft.car;

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
        if(intent.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED"))
        {
            mUsbListener.connectDevice();
        }
        else
        {
            mUsbListener.usbDetached();
        }

    }

    public interface UsbListener {
        void connectDevice();
        /**
         * usb断开连接
         */
        void usbDetached();
    }
}
