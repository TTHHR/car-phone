package com.yaasoosoft.phone;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OpenAccessoryReceiver.OpenAccessoryListener, UsbDetachedReceiver.UsbDetachedListener{

    private static final String USB_ACTION = "com.yaasoosoft.usbaction";
    private TextView mLog;
    private Button mSend;
    private EditText mMessage;
    private UsbManager mUsbManager;
    private ExecutorService mThreadPool;
    private UsbDetachedReceiver mUsbDetachedReceiver;
    private OpenAccessoryReceiver mOpenAccessoryReceiver;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        initData();
    }
    private void initView() {
        mLog =  findViewById(R.id.log);
        mMessage =  findViewById(R.id.message);
        mSend =  findViewById(R.id.send);
    }

    private void initListener() {
        mSend.setOnClickListener((v)->{
            final String mMessageContent = mMessage.getText().toString();
            if (!TextUtils.isEmpty(mMessageContent)) {
                mThreadPool.execute(() -> {
                    try {
                        mFileOutputStream.write(mMessageContent.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void initData() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mThreadPool = Executors.newFixedThreadPool(3);

        mUsbDetachedReceiver = new UsbDetachedReceiver(this);
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbDetachedReceiver, filter);

        mOpenAccessoryReceiver = new OpenAccessoryReceiver(this);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(USB_ACTION), 0);
        IntentFilter intentFilter = new IntentFilter(USB_ACTION);
        registerReceiver(mOpenAccessoryReceiver, intentFilter);

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory usbAccessory = (accessories == null ? null : accessories[0]);
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
            FileDescriptor fileDescriptor = mParcelFileDescriptor.getFileDescriptor();
            mFileInputStream = new FileInputStream(fileDescriptor);
            mFileOutputStream = new FileOutputStream(fileDescriptor);
            mSend.setEnabled(true);

            mThreadPool.execute(new Runnable() {
                byte[] mBytes=new byte[1024];
                @Override
                public void run() {
                    int i = 0;
                    while (i >= 0) {
                        try {
                            i = mFileInputStream.read(mBytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }
                        if (i > 0) {
                            EventBus.getDefault().post(new String(mBytes, 0, i));
                        }
                    }
                }
            });
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String msg) {
        mLog.append(msg);
        mLog.append("\n");
    }
    @Override
    public void usbDetached() {
        finish();
    }
    @Override
    public void openAccessoryModel(UsbAccessory usbAccessory) {
        openAccessory(usbAccessory);
    }

    @Override
    public void openAccessoryError() {

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

        unregisterReceiver(mOpenAccessoryReceiver);
        unregisterReceiver(mUsbDetachedReceiver);
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