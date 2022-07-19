package com.yaasoosoft.car.presenter;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.yaasoosoft.car.inter.PlayerInterface;

import java.io.IOException;
import java.net.Socket;

public class PlayPresenter implements View.OnTouchListener , View.OnClickListener{
    PlayerInterface playerInterface;
    private boolean running=false;
    Socket client =null;
    private String TAG="playpre";
    private PlayPresenter(){}
    private boolean isClick=false;
    public PlayPresenter(PlayerInterface playerInterface)
    {
        this.playerInterface=playerInterface;

    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if(isClick)//上次click还没发出去
            return true;
        float x= event.getX(),y=event.getY();


        Log.e(TAG,"touch x,y="+(int)event.getX()+','+(int)event.getY()+" %="+x+" "+y);
//        if(x<leftUnused
//            ||
//                x>rightUnused
//        )
//            return true;//X越界
//
//        if(y<upUnused
//                ||
//                y>downUnused
//        )
//            return true;//Y越界
//
//        //获得相对坐标
//        x=x-leftUnused;
//        y=y-upUnused;



//        //计算坐标在屏幕所占得千分比
//        long xpre = (long)(x*1000f/ Setting.getInstance().getScreenW());
//        long ypre = (long)(y*1000f/ Setting.getInstance().getScreenH());
//
//        if(controlMsg.getTouchTime()==0)//第一次点击时间
//        {
//            controlMsg.setTouchTime(System.currentTimeMillis());
//        }
//        controlMsg.getPoints().add(xpre*1000+ypre);

        return false;
    }

    @Override
    public void onClick(View view) {
//        controlMsg.setTouchTime(System.currentTimeMillis()- controlMsg.getTouchTime());//计算点击、滑动时长
//        //touch结束
//        if(controlMsg.getPoints().size()<=2)//click会有两次touch
//        {
//            controlMsg.setType(ControlMsg.MsgType.CLICK);
//        }
//        else
//        {
//            controlMsg.setType(ControlMsg.MsgType.TOUCH);
//        }
//        isClick=true;
    }
    public void stop()
    {
        running=false;
        if(client!=null)
        {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
