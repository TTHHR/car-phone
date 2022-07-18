package com.yaasoosoft.car;

import androidx.appcompat.app.AppCompatActivity;

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
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements UsbReceiver.UsbListener, OpenDevicesReceiver.OpenDevicesListener{

    private static final String USB_ACTION = "com.yaasoosoft.usbaction";
    private static final String TAG ="main" ;
    private TextView mLog;
    private EditText mMessage;
    private Button mSendMessage;
    private Context mContext;
    private ExecutorService mThreadPool;
    private UsbManager mUsbManager;
    private UsbReceiver mUsbDetachedReceiver;
    private OpenDevicesReceiver mOpenDevicesReceiver;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbInterface mUsbInterface;
    private UsbEndpoint mUsbEndpointOut;
    private UsbEndpoint mUsbEndpointIn;
    private boolean mToggle = true;
    private boolean isDetached        = false;
    private boolean isReceiverMessage =true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        initData();
    }
    private void initView() {
        mLog = findViewById(R.id.log);
        mMessage =  findViewById(R.id.message);
        mSendMessage = findViewById(R.id.sendmessage);

    }

    private void initListener() {
        mSendMessage.setOnClickListener((v)->{
            final String messageContent = mMessage.getText().toString();
            if (!TextUtils.isEmpty(messageContent)) {
                mThreadPool.execute(() -> {
                    /**
                     * 发送数据的地方 , 只接受byte数据类型的数据
                     */
                    int i = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, messageContent.getBytes(), messageContent.getBytes().length, 3000);
                    if (i > 0) {//大于0表示发送成功
                        Log.e(TAG,"send ok");
                    }
                });
            }
        });
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String msg) {
        mLog.append("\n");
        mLog.append(msg);
    }
    private void initData() {
        mContext = getApplicationContext();
        mUsbDetachedReceiver = new UsbReceiver(this);
        IntentFilter intentFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(mUsbDetachedReceiver, intentFilter);

        mThreadPool = Executors.newFixedThreadPool(5);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        openDevices();
    }
    /**
     * 打开设备 , 让车机和手机端连起来
     */
    private void openDevices() {

        IntentFilter intentFilter = new IntentFilter(USB_ACTION);
        mOpenDevicesReceiver = new OpenDevicesReceiver(this);
        registerReceiver(mOpenDevicesReceiver, intentFilter);

       connectDevice();
    }

    /**
     * 发送命令 , 让手机进入Accessory模式
     *
     * @param usbDevice
     */
    private void initAccessory(UsbDevice usbDevice) {
        UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(usbDevice);
        if (usbDeviceConnection == null) {
            EventBus.getDefault().post("请连接USB");
            return;
        }

        //根据AOA协议打开Accessory模式
        initStringControlTransfer(usbDeviceConnection, 0, "Google, Inc."); // MANUFACTURER
        initStringControlTransfer(usbDeviceConnection, 1, "AccessoryChat"); // MODEL
        initStringControlTransfer(usbDeviceConnection, 2, "Accessory Chat"); // DESCRIPTION
        initStringControlTransfer(usbDeviceConnection, 3, "1.0"); // VERSION
        initStringControlTransfer(usbDeviceConnection, 4, "http://www.android.com"); // URI
        initStringControlTransfer(usbDeviceConnection, 5, "0123456789"); // SERIAL
        usbDeviceConnection.controlTransfer(0x40, 53, 0, 0, new byte[]{}, 0, 100);
        usbDeviceConnection.close();
        EventBus.getDefault().post("initAccessory success");
        initDevice();
    }
    private void initStringControlTransfer(UsbDeviceConnection deviceConnection, int index, String string) {
        deviceConnection.controlTransfer(0x40, 52, 0, index, string.getBytes(), string.length(), 100);
    }
    /**
     * 初始化设备(手机) , 当手机进入Accessory模式后 , 手机的PID会变为Google定义的2个常量值其中的一个 ,
     */
    private void initDevice() {
        mThreadPool.execute(() -> {
            while (mToggle) {
                SystemClock.sleep(1000);
                HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
                Collection<UsbDevice> values = deviceList.values();
                if (!values.isEmpty()) {
                    for (UsbDevice usbDevice : values) {
                        int productId = usbDevice.getProductId();
//                        if (productId == 0x2D00 || productId == 0x2D01)
                        {
                            if (mUsbManager.hasPermission(usbDevice)) {
                                mUsbDeviceConnection = mUsbManager.openDevice(usbDevice);
                                if (mUsbDeviceConnection != null) {
                                    mUsbInterface = usbDevice.getInterface(0);
                                    int endpointCount = mUsbInterface.getEndpointCount();
                                    for (int i = 0; i < endpointCount; i++) {
                                        UsbEndpoint usbEndpoint = mUsbInterface.getEndpoint(i);
                                        if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                                            if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                                mUsbEndpointOut = usbEndpoint;
                                            } else if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                                                mUsbEndpointIn = usbEndpoint;
                                            }
                                        }
                                    }
                                    if (mUsbEndpointOut != null && mUsbEndpointIn != null) {
                                        EventBus.getDefault().post("连接成功");
                                        loopReceiverMessage();
                                        mToggle = false;
                                        isDetached = true;
                                    }
                                }
                            } else {
                                mUsbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(mContext, 0, new Intent(""), 0));
                            }
                        }
                    }
                } else {
                    finish();
                }
            }
        });
    }
    /**
     * 接受消息线程 , 此线程在设备(手机)初始化完成后 , 就一直循环接受消息
     */
    private void loopReceiverMessage() {
        mThreadPool.execute(new Runnable() {
            byte[] mBytes=new byte[16384];
            @Override
            public void run() {
                SystemClock.sleep(1000);
                while (isReceiverMessage) {
                    /**
                     * 循环接受数据的地方 , 只接受byte数据类型的数据
                     */
                    if (mUsbDeviceConnection != null && mUsbEndpointIn != null) {
                        int i = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, mBytes, mBytes.length, 3000);
                        if (i > 0) {
                            EventBus.getDefault().post(new String(mBytes, 0, i));
                            Log.e(TAG,"rec");
                        }
                    }
                }
            }
        });
    }

    @Override
    public void connectDevice() {
        PendingIntent pendingIntent ;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(USB_ACTION),  PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(USB_ACTION),  PendingIntent.FLAG_ONE_SHOT);
        }
        //列举设备(手机)
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (deviceList != null&&deviceList.size()!=0) {
            for (UsbDevice usbDevice : deviceList.values()) {
                int productId = usbDevice.getProductId();
                Log.e(TAG,"product "+productId);
                mLog.setText("product "+productId);
                if (productId != 377 && productId != 7205) {
                    if (mUsbManager.hasPermission(usbDevice)) {
                        initAccessory(usbDevice);
                    } else {
                        mUsbManager.requestPermission(usbDevice, pendingIntent);
                    }
                }
            }
        } else {
            mLog.setText("请连接USB");
        }
    }

    @Override
    public void usbDetached() {
        if (isDetached) {
           // finish();
        }
    }

    @Override
    public void openAccessoryModel(UsbDevice usbDevice) {
        initAccessory(usbDevice);
    }

    @Override
    public void openDevicesError(String msg) {
        EventBus.getDefault().post(msg);
    }
    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mUsbDeviceConnection != null) {
            mUsbDeviceConnection.releaseInterface(mUsbInterface);
            mUsbDeviceConnection.close();
            mUsbDeviceConnection = null;
        }
        mUsbEndpointIn = null;
        mUsbEndpointOut = null;
        mToggle = false;
        isReceiverMessage = false;
        mThreadPool.shutdownNow();
        unregisterReceiver(mUsbDetachedReceiver);
        unregisterReceiver(mOpenDevicesReceiver);
    }
}