package com.yaasoosoft.phone;


import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.yaasoosoft.EventMessage;
import com.yaasoosoft.phone.utils.UsbHelper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;



public class MainActivity extends BaseActivity {


    private TextView mLog;
    private Button mSend;
    private EditText mMessage;

    private String TAG="Main";
    private UsbHelper usbHelper;
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
            EventMessage message=new EventMessage();
            message.setMsg(mMessageContent);
            message.msgType= EventMessage.MESSAGE_TEXT;
            if (!TextUtils.isEmpty(mMessageContent)) {
                usbHelper.writeData(message);
            }
        });
    }

    private void initData() {
        usbHelper=new UsbHelper(getApplicationContext());
        usbHelper.init();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventMessage msg) {
        if(msg.msgType==EventMessage.MESSAGE_LOG||msg.msgType==EventMessage.MESSAGE_TEXT) {
            Log.e(TAG,msg.getMsg());
            mLog.append("\n");
            mLog.append(msg.getMsg());
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        usbHelper.release();
    }
}