package com.yaasoosoft.car;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yaasoosoft.EventMessage;

import org.greenrobot.eventbus.EventBus;

public class UsbReceiver extends BroadcastReceiver {

    private static final String TAG = "UsbDetached";
    private UsbListener mUsbListener;
    private EventMessage em=new EventMessage();
    public UsbReceiver(UsbListener usbListener) {
        mUsbListener = usbListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED"))
        {
            mUsbListener.connectDevice();
        }
        else
        {
            em.setMsg(intent.getAction());
            em.msgType=EventMessage.MESSAGE_USB_DETACH;
            EventBus.getDefault().post(em);
        }

    }

    public interface UsbListener {
        void connectDevice();
    }
}
