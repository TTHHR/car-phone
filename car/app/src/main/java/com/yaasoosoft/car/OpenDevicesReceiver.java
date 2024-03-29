package com.yaasoosoft.car;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public class OpenDevicesReceiver extends BroadcastReceiver {

    private OpenDevicesListener mOpenDevicesListener;

    public OpenDevicesReceiver(OpenDevicesListener openDevicesListener) {
        mOpenDevicesListener = openDevicesListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (usbDevice != null) {
                mOpenDevicesListener.openAccessoryModel(usbDevice);
            } else {
                mOpenDevicesListener.openDevicesError("usb设备为空");
            }
        } else {
            mOpenDevicesListener.openDevicesError("没有权限");
        }
    }

    public interface OpenDevicesListener {
        /**
         * 打开Accessory模式
         *
         * @param usbDevice
         */
        void openAccessoryModel(UsbDevice usbDevice);

        /**
         * 打开设备(手机)失败
         */
        void openDevicesError(String msg);
    }
}