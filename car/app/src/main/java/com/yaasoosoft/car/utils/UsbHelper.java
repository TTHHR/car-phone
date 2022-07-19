package com.yaasoosoft.car.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.yaasoosoft.car.OpenDevicesReceiver;
import com.yaasoosoft.car.UsbReceiver;
import com.yaasoosoft.EventMessage;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;

public class UsbHelper implements UsbReceiver.UsbListener, OpenDevicesReceiver.OpenDevicesListener{
    private final Context context;
    private static final String USB_ACTION = "com.yaasoosoft.usbaction";
    private static final String TAG ="UsbHelper" ;
    private UsbManager mUsbManager;
    private UsbReceiver mUsbDetachedReceiver;
    private OpenDevicesReceiver mOpenDevicesReceiver;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbInterface mUsbInterface;
    private UsbEndpoint mUsbEndpointOut;
    private UsbEndpoint mUsbEndpointIn;
    private boolean linkSuccess;
    EventMessage em=new EventMessage();
    public UsbHelper(Context context)
    {
        this.context=context;
    }
    public void init()
    {
//        初始化USB插入断开的广播接收器，用来申请权限之类的
        mUsbDetachedReceiver = new UsbReceiver(this);
        IntentFilter intentFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        context.registerReceiver(mUsbDetachedReceiver, intentFilter);


        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);


        //权限申请结束后，拿到USB设备的广播
        IntentFilter intentFilter2 = new IntentFilter(USB_ACTION);
        mOpenDevicesReceiver = new OpenDevicesReceiver(this);
        context.registerReceiver(mOpenDevicesReceiver, intentFilter2);



        //假如USB已插入，直接申请连接
        connectDevice();


    }
    private void initStringControlTransfer(UsbDeviceConnection deviceConnection, int index, String string) {
        deviceConnection.controlTransfer(0x40, 52, 0, index, string.getBytes(), string.length(), 100);
    }
    /**
     * 发送命令 , 让手机进入Accessory模式
     *
     * @param usbDevice
     */
    private void initAccessory(UsbDevice usbDevice) {
        UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(usbDevice);
        if (usbDeviceConnection == null) {
            em.setMsg("请连接USB");
            EventBus.getDefault().post(em);
            return;
        }

        //根据AOA协议打开Accessory模式
        initStringControlTransfer(usbDeviceConnection, 0, "Yaasoosoft, Inc."); // MANUFACTURER
        initStringControlTransfer(usbDeviceConnection, 1, "AccessoryChat"); // MODEL
        initStringControlTransfer(usbDeviceConnection, 2, "Accessory Chat"); // DESCRIPTION
        initStringControlTransfer(usbDeviceConnection, 3, "1.0"); // VERSION
        initStringControlTransfer(usbDeviceConnection, 4, "http://www.android.com"); // URI
        initStringControlTransfer(usbDeviceConnection, 5, "0123456789"); // SERIAL
        usbDeviceConnection.controlTransfer(0x40, 53, 0, 0, new byte[]{}, 0, 100);
        usbDeviceConnection.close();
        em.setMsg("initAccessory success");
        EventBus.getDefault().post(em);
        initDevice();
    }
    private void openDevice(UsbDevice usbDevice)
    {
        mUsbDeviceConnection = mUsbManager.openDevice(usbDevice);
        if (mUsbDeviceConnection != null) {
            mUsbInterface = usbDevice.getInterface(0);
            int endpointCount = mUsbInterface.getEndpointCount();
            em.setMsg("endpoint "+endpointCount);
            EventBus.getDefault().post(em);
            for (int i = 0; i < endpointCount; i++) {
                UsbEndpoint usbEndpoint = mUsbInterface.getEndpoint(i);
                if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    em.setMsg("可用 的 endpoint ");
                    EventBus.getDefault().post(em);
                    if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        mUsbEndpointOut = usbEndpoint;
                    } else if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                        mUsbEndpointIn = usbEndpoint;
                    }
                }
            }
            if (mUsbEndpointOut != null && mUsbEndpointIn != null) {
                em.setMsg("连接成功");
                EventBus.getDefault().post(em);
                loopReceiverMessage();
            }
        }
        else
        {
            em.setMsg("打开设备失败");
            EventBus.getDefault().post(em);
        }
    }
    /**
     * 初始化设备(手机) , 当手机进入Accessory模式后 , 手机的PID会变为Google定义的2个常量值其中的一个 ,
     */
    private void initDevice() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
                Collection<UsbDevice> values = deviceList.values();
                if (!values.isEmpty()) {
                    for (UsbDevice usbDevice : values) {
                        int productId = usbDevice.getProductId();
                        if (productId == 0x2D00 || productId == 0x2D01)
                        {
                            if (mUsbManager.hasPermission(usbDevice)) {
                                openDevice(usbDevice);
                            } else {
                                em.setMsg("申请权限");
                                EventBus.getDefault().post(em);
                                PendingIntent pendingIntent ;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(USB_ACTION),  PendingIntent.FLAG_IMMUTABLE);
                                } else {
                                    pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(USB_ACTION),  PendingIntent.FLAG_ONE_SHOT);
                                }
                                mUsbManager.requestPermission(usbDevice, pendingIntent);
                            }
                        }
                    }
                } else {
                    em.setMsg("USB设备为空");
                    EventBus.getDefault().post(em);
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
                int i = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut,data,data.length, 3000);
                if (i > 0) {//大于0表示发送成功
                    em.setMsg("发送成功");
                }
                else {
                    em.setMsg("发送失败");
                }
            }catch (Exception e)
            {
                em.setMsg(e.toString());
            }

        }
        EventBus.getDefault().post(em);
    }

    /**
     * 接受消息线程 , 此线程在设备(手机)初始化完成后 , 就一直循环接受消息
     */
    private void loopReceiverMessage() {
        new Thread(new Runnable() {
            byte[] mBytes=new byte[16384];
            @Override
            public void run() {
                linkSuccess=true;
                while (linkSuccess) {
                    /**
                     * 循环接受数据的地方 , 只接受byte数据类型的数据
                     */
                    if (mUsbDeviceConnection != null && mUsbEndpointIn != null) {
                        int i = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, mBytes, mBytes.length, 3000);
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
            }
        }).start();
    }

    @Override
    public void openAccessoryModel(UsbDevice usbDevice) {
        initAccessory(usbDevice);
    }

    @Override
    public void openDevicesError(String msg) {
        em.setMsg(msg);
        EventBus.getDefault().post(em);
    }

    @Override
    public void connectDevice() {
        PendingIntent pendingIntent ;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(USB_ACTION),  PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(USB_ACTION),  PendingIntent.FLAG_ONE_SHOT);
        }
        //列举设备(手机)
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (deviceList != null&&deviceList.size()!=0) {
            for (UsbDevice usbDevice : deviceList.values()) {
                int productId = usbDevice.getProductId();
                em.setMsg("product :"+productId);
                EventBus.getDefault().post(em);
                if (productId != 377 && productId != 7205) {
                    if (mUsbManager.hasPermission(usbDevice)) {
                        initAccessory(usbDevice);
                    } else {
                        mUsbManager.requestPermission(usbDevice, pendingIntent);
                    }
                }
            }
        } else {
            em.setMsg("usb已断开");
            EventBus.getDefault().post(em);
        }
    }

    @Override
    public void usbDetached() {
        em.setMsg("usb已断开");
        EventBus.getDefault().post(em);
        linkSuccess=false;
        if (mUsbDeviceConnection != null) {
            mUsbDeviceConnection.releaseInterface(mUsbInterface);
            mUsbDeviceConnection.close();
            mUsbDeviceConnection = null;
        }
        mUsbEndpointIn = null;
        mUsbEndpointOut = null;
    }
    public void release()
    {
        em.setMsg("release");
        EventBus.getDefault().post(em);
        linkSuccess=false;
        if (mUsbDeviceConnection != null) {
            mUsbDeviceConnection.releaseInterface(mUsbInterface);
            mUsbDeviceConnection.close();
            mUsbDeviceConnection = null;
        }
        mUsbEndpointIn = null;
        mUsbEndpointOut = null;
        context.unregisterReceiver(mUsbDetachedReceiver);
        context.unregisterReceiver(mOpenDevicesReceiver);
    }
}
