package com.yaasoosoft;

import java.io.Serializable;

public class EventMessage implements Serializable {
    public static final int MESSAGE_TEXT = 0;
    public static final int MESSAGE_USB_DETACH = 3;
    public static int MESSAGE_LOG=1;
    public static int MESSAGE_VIDEO=2;
    private byte[] videoBuff;
    public  int msgType=MESSAGE_LOG;
    private  String msg="";
    public void setMsg(String msg)
    {
        StackTraceElement[] stack = (new Throwable()).getStackTrace();
        StackTraceElement ste=stack[2];
        String className = ste.getClassName();
        this.msg=className+":"+msg;
    }
    public String getMsg()
    {
        return this.msg;
    }

    public byte[] getVideoBuff() {
        return videoBuff;
    }

    public void setVideoBuff(byte[] videoBuff) {
        this.videoBuff = videoBuff;
    }
}
