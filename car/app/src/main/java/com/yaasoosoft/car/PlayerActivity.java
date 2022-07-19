package com.yaasoosoft.car;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.yaasoosoft.EventMessage;
import com.yaasoosoft.car.entity.Setting;
import com.yaasoosoft.car.inter.PlayerInterface;
import com.yaasoosoft.car.presenter.PlayPresenter;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayerActivity extends BaseActivity implements PlayerInterface, SurfaceHolder.Callback {
    private String TAG="PlayA";
    private PlayPresenter playPresenter;
    private SurfaceView surfaceview_play;
    private MediaCodec decoder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_player);

        playPresenter=new PlayPresenter(this);

        surfaceview_play = ((SurfaceView) findViewById(R.id.surfaceView));

        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(Setting.getInstance().screenW, Setting.getInstance().screenH);
        surfaceview_play.setLayoutParams(lp);

        surfaceview_play.getHolder().addCallback(this);


    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void usbDetached(EventMessage eventMessage) {
        if(eventMessage.msgType!=EventMessage.MESSAGE_USB_DETACH)
            return;
        finish();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFrame(EventMessage eventMessage) {

            if(eventMessage.msgType!=EventMessage.MESSAGE_VIDEO) {
return;

        }
        if(decoder==null)
            return;
        Log.e(TAG,"video "+eventMessage.getVideoBuff().length);
        try {
            int inputBufferIndex = decoder.dequeueInputBuffer(0);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(eventMessage.getVideoBuff(), 0, eventMessage.getVideoBuff().length);
                decoder.queueInputBuffer(inputBufferIndex, 0, eventMessage.getVideoBuff().length, System.currentTimeMillis(), 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
                decoder.releaseOutputBuffer(outputBufferIndex, true);
                outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        initMediaDecoder(surfaceHolder.getSurface());
        surfaceview_play.setOnTouchListener(playPresenter);
        surfaceview_play.setClickable(true);
        surfaceview_play.setOnClickListener(playPresenter);

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }
    private void initMediaDecoder(Surface surface) {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, Setting.getInstance().screenW,Setting.getInstance().screenH);
        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            decoder.configure(mediaFormat, surface, null, 0);
            decoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        playPresenter.stop();
        decoder.stop();
        decoder.release();
        decoder=null;
        super.onDestroy();
    }

}
