package com.yaasoosoft.phone.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;

import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;


import com.yaasoosoft.phone.OpenAccessoryReceiver;
import com.yaasoosoft.phone.UsbReceiver;
import com.yaasoosoft.EventMessage;


import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class UsbHelper implements UsbReceiver.UsbListener, OpenAccessoryReceiver.OpenAccessoryListener{
    private final Context context;
    private static final String USB_ACTION = "com.yaasoosoft.usbaction";
    private static final String TAG ="UsbHelper" ;

    private boolean linkSuccess;
    private UsbManager mUsbManager;
    private UsbReceiver mUsbReceiver;
    private OpenAccessoryReceiver mOpenAccessoryReceiver;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    EventMessage em=new EventMessage();
    public UsbHelper(Context context)
    {
        this.context=context;
    }
    public void init()
    {

        mUsbReceiver = new UsbReceiver(this);
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        context.registerReceiver(mUsbReceiver, filter);

        mOpenAccessoryReceiver = new OpenAccessoryReceiver(this);
        PendingIntent pendingIntent ;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(USB_ACTION),  PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(USB_ACTION),  PendingIntent.FLAG_ONE_SHOT);
        }
        IntentFilter intentFilter = new IntentFilter(USB_ACTION);
        context.registerReceiver(mOpenAccessoryReceiver, intentFilter);

         mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbAccessory[] accessoryList = mUsbManager.getAccessoryList();
        UsbAccessory usbAccessory = accessoryList == null ? null : accessoryList[0];
        Log.e(TAG,"usbAccessory "+usbAccessory);
        if (usbAccessory != null) {
            if (mUsbManager.hasPermission(usbAccessory)) {
                openAccessory(usbAccessory);
            } else {
                mUsbManager.requestPermission(usbAccessory, pendingIntent);
            }
        }

    }
    /**
     * 打开Accessory模式
     *
     * @param usbAccessory
     */
    private void openAccessory(UsbAccessory usbAccessory) {
        mParcelFileDescriptor = mUsbManager.openAccessory(usbAccessory);
        if (mParcelFileDescriptor != null) {
            linkSuccess=true;
            FileDescriptor fileDescriptor = mParcelFileDescriptor.getFileDescriptor();
            mFileInputStream = new FileInputStream(fileDescriptor);
            mFileOutputStream = new FileOutputStream(fileDescriptor);

            new Thread(new Runnable() {
                byte[] mBytes=new byte[16384];
                @Override
                public void run() {

                    int i = 0;
                    while (i >= 0&&linkSuccess) {
                        try {
                            i = mFileInputStream.read(mBytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                            em.setMsg("接收数据错误");
                            EventBus.getDefault().post(em);
                            break;
                        }
                        if (i > 0) {
                            try (ByteArrayInputStream bis = new ByteArrayInputStream(mBytes);
                                 ObjectInput in = new ObjectInputStream(bis);) {
                                Object o= in.readObject();
                                if(o instanceof EventMessage)
                                    EventBus.getDefault().post(o);
                            } catch (Exception e) {
                                Log.e(TAG, "read obj" + e);
                            }
                        }
                    }
                }
            }).start();
        }
    }
    public void writeData(EventMessage eventMessage)
    {
        if(!linkSuccess)
        {
            em.setMsg("USB未连接");
        }
        else
        {
            try(
                    ByteArrayOutputStream bas = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bas);
            ){
                oos.writeObject(eventMessage);
                byte[] data=bas.toByteArray();
              mFileOutputStream.write(data);

                    em.setMsg("发送成功");
            }catch (Exception e)
            {
                em.setMsg(e.toString());
            }

        }
        EventBus.getDefault().post(em);
    }
    @Override
    public void openAccessoryModel(UsbAccessory usbAccessory) {
        openAccessory(usbAccessory);
    }

    @Override
    public void openAccessoryError(String msg) {
        em.setMsg(msg);
        EventBus.getDefault().post(em);
    }

    @Override
    public void usbDetached() {
        em.setMsg("USB断开连接");
        EventBus.getDefault().post(em);
        linkSuccess=false;
        if (mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFileInputStream != null) {
            try {
                mFileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void release()
    {
        linkSuccess=false;
        context.unregisterReceiver(mOpenAccessoryReceiver);
        context.unregisterReceiver(mUsbReceiver);
        if (mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFileInputStream != null) {
            try {
                mFileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
