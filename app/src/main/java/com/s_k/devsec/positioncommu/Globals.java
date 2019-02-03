package com.s_k.devsec.positioncommu;

import android.app.Application;

public class Globals extends Application {

    //    private String ipAddress = "192.168.1.100";
    private String portNumber = "5000";

    @Override
    public void onCreate(){
        super.onCreate();
    }

//    public String getIpAddress(){
//        return ipAddress;
//    }

//    public void setIpAddress(String str){
//        ipAddress = str;
//    }

    public String getPortNumber(){
        return portNumber;
    }

    public void setPortNumber(String str){
        portNumber = str;
    }
}
