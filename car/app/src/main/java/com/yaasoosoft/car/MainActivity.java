package com.yaasoosoft.car;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.yaasoosoft.EventMessage;
import com.yaasoosoft.car.utils.UsbHelper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class MainActivity extends BaseActivity {

    private static final String TAG ="main" ;
    private TextView mLog;
    private EditText mMessage;
    private Button mSendMessage,gotoPlay;
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
        mLog = findViewById(R.id.log);
        mMessage =  findViewById(R.id.message);
        mSendMessage = findViewById(R.id.sendmessage);
        gotoPlay=findViewById(R.id.gotoPlay);
    }

    private void initListener() {
        mSendMessage.setOnClickListener((v)->{
            final String messageContent = mMessage.getText().toString();
            EventMessage message=new EventMessage();
            message.setMsg(messageContent);
            message.msgType=EventMessage.MESSAGE_TEXT;
            if (!TextUtils.isEmpty(messageContent)) {
                usbHelper.writeData(message);
            }
        });
        gotoPlay.setOnClickListener((v)->{
            Intent intent=new Intent(this,PlayerActivity.class);
            startActivity(intent);
        });
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventMessage msg) {
        if(msg.msgType==EventMessage.MESSAGE_LOG||msg.msgType==EventMessage.MESSAGE_TEXT) {
            Log.e(TAG,msg.getMsg());
            mLog.append("\n");
            mLog.append(msg.getMsg());
        }
    }
    private void initData() {
        usbHelper=new UsbHelper(getApplicationContext());
        usbHelper.init();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    usbHelper.release();

    }
}