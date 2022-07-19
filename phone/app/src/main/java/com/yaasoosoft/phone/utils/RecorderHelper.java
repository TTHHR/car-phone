package com.yaasoosoft.phone.utils;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import com.yaasoosoft.phone.RecorderService;


import java.util.List;

public class RecorderHelper {
    private Context context;
    private ActivityResultLauncher<Intent> requestActivity;
    private Intent service;
    private String TAG="RecorderHelper";
    public RecorderHelper(Context context)
    {
        this.context=context;
    }
    public void init()
    {
        service = new Intent(context, RecorderService.class);
        requestActivity=((AppCompatActivity)context).registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode()== Activity.RESULT_OK) {
                service.putExtra("code", result.getResultCode());
                service.putExtra("data", result.getData());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    (context).startForegroundService(service);
                } else {
                    (context).startService(service);
                }
            }
            else
            {
                Log.e(TAG,"需要权限");
                //addMsg(InfoType.NORMAL,"需要权限");
            }
        });
    }
    public void start()
    {
        if(!isServiceRunning(context, RecorderService.class.getName()))
        {
            MediaProjectionManager mMediaProjectionManager;
            mMediaProjectionManager = (MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE);
            requestActivity.launch(mMediaProjectionManager.createScreenCaptureIntent());
        }
    }
    /**
     * 用来判断服务是否运行.
     * @param mContext
     * @param className 判断的服务名字
     * @return true 在运行 false 不在运行
     */
    public static boolean isServiceRunning(Context mContext, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList
                = activityManager.getRunningServices(30);
        if (!(serviceList.size()>0)) {
            return false;
        }
        for (int i=0; i<serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className)) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }
}
