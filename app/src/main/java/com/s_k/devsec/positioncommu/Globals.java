package com.s_k.devsec.positioncommu;

import android.app.Application;

public class Globals extends Application {

    private String myPortNumber = "5000";
    private String peerIPAddress = "";
    private String peerPortNumber = "5000";

    @Override
    public void onCreate(){
        super.onCreate();
    }

    public String getMyPortNumber(){
        return myPortNumber;
    }

    public void setMyPortNumber(String str){
        myPortNumber = str;
    }

    public String getPeerIPAddress() {
        return peerIPAddress;
    }

    public void setPeerIPAddress(String str){
        peerIPAddress = str;
    }

    public String getPeerPortNumber() {
        return peerPortNumber;
    }

    public void setPeerPortNumber(String str){
        peerPortNumber = str;
    }

}
