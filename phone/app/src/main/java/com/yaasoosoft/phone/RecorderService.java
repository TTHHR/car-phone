package com.yaasoosoft.phone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import com.yaasoosoft.EventMessage;
import com.yaasoosoft.phone.coder.MediaEncoder;
import com.yaasoosoft.phone.entity.Setting;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.security.spec.ECField;

public class RecorderService extends Service implements MediaEncoder.OnScreenCallBack {
    private final String TAG = "recs";
    private MediaProjection mMediaProjection;
    public boolean isRunning = false;
    private MediaEncoder encoder = null;
    private Intent intent = null;
    private final String key = "FD:94:FE:3B:D6:FC:8B:DA:19:4C:7B:85:FA:57:DA:71:3B:EF:C1:8F:8F:32:68:78";
    private EventMessage videoMessage=new EventMessage();
    public RecorderService() {
    }

    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class); //点击后跳转的界面，可以设置跳转数据

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, PendingIntent.FLAG_IMMUTABLE)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                .setContentTitle("投屏") // 设置下拉列表里的标题
//                    .setSmallIcon(R.mipmap.logo_sm) // 设置状态栏内的小图标
                .setContentText("正在投屏") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);

    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (this.intent == null && intent != null) {
            createNotificationChannel();
            int mResultCode = intent.getIntExtra("code", -1);
            Parcelable mResultData = intent.getParcelableExtra("data");

            mMediaProjection = ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE)).getMediaProjection(mResultCode, (Intent) mResultData);
            Log.e(TAG, "mMediaProjection created: " + mMediaProjection);
            isRunning = true;
            recordThread();
            this.intent = intent;
        }
        videoMessage.msgType= EventMessage.MESSAGE_VIDEO;
        Log.e(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "destroy");
        isRunning = false;
        stopForeground(true);
        release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //释放资源
    private void release() {
        try {
            if (encoder != null)
                encoder.release();
            encoder = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void recordThread() {

        new Thread(() -> {

            while (isRunning) {

                if (encoder == null) {
                    try {
                        encoder = new MediaEncoder(mMediaProjection, Setting.getInstance().screenW, Setting.getInstance().screenH, Setting.getInstance().screenDPI)
                                .setVideoBit(Setting.getInstance().BIT)
                                .setVideoFPS(Setting.getInstance().FPS);
                        if (encoder != null) {
                            encoder.setOnScreenCallBack(this);
                            encoder.start();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }

                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void usbDetached(EventMessage eventMessage) {
        if(eventMessage.msgType!=EventMessage.MESSAGE_USB_DETACH)
            return;
        stopSelf();
        eventMessage.msgType=EventMessage.MESSAGE_LOG;
        eventMessage.setMsg("断开连接，停止投屏");
        EventBus.getDefault().post(eventMessage);
    }
    @Override
    public void onScreenInfo(byte[] bytes) {
        Log.e(TAG,""+bytes.length);
        videoMessage.setVideoBuff(bytes);
        EventBus.getDefault().post(videoMessage);
    }
}
