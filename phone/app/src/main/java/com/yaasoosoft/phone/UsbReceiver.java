package com.yaasoosoft.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yaasoosoft.EventMessage;

import org.greenrobot.eventbus.EventBus;

public class UsbReceiver extends BroadcastReceiver {
    private static final String TAG = "UsbDetached";
    EventMessage eventMessage=new EventMessage();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG,"attached "+intent.getAction());
        eventMessage.msgType=EventMessage.MESSAGE_USB_DETACH;
        EventBus.getDefault().post(eventMessage);
    }


}
