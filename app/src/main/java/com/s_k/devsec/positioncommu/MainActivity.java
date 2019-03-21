package com.s_k.devsec.positioncommu;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements LocationListener {

    TextView tvDistance;
    TextView tvAngle;
    TextView tvPortNumber;
    TextView tvSSID;
    TextView tvIpAddress;
    TextView tvLatitude;
    TextView tvLongitude;
    TextView tvProvider;
    TextView tvMeasCount;
    TextView tvMeasInterval;
    TextView tvFixedLatitude;
    TextView tvFixedLongitude;
    TextView tvPeerLatitude;
    TextView tvPeerLongitude;
    TextView tvPeerProvider;
    TextView tvReceiveCount;
    TextView tvReceiveInterval;
    TextView tvSendDist;
    TextView tvSendAngle;

    Button btFixed;
    Button btSendDemo1;
    Button btSendDemo2;
    Button btContStart;
    Button btContStop;
    Button btLogStart;
    Button btLogStop;

    View mButtonClicked;

    static int customViewWidth;
    static int customViewHeight;

    Globals globals;
    Handler mHandler;

    UDPMeasReceiverThread mUDPTestReceiver = null;
    WifiStatusUpdateThread mWifiStatusUpdateThread = null;
    UDPTestContSenderThread mUDPTestCSThread;

    int REQUEST_CODE_ACCESS_FINE_LOCATION_PERMISSION = 1000;
    int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1001;
    int commPort;
    int measCount = 0;
    long lastMeasTime =0;
    int receiveCount = 0;
    long lastReceiveTime = 0;

    LocationManager locationManager;

    double latitude = 0; //緯度フィールド
    double longitude = 0; //経度フィールド
    static double EARTHRADIUS = 6378137;
    double fixedLatitude = 0;
    double fixedLongitude = 0;
    double initAngle = 0;

    String provider = "";
    String dist = "";
    String angle = "";
    String path = "";
    String fileName = "";

    boolean isMeasStart = true;
    boolean isFixed = false;
    boolean isFirstReceive = true;
    boolean isFileSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        globals = (Globals) this.getApplication();
        mHandler = new Handler();

        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        int ipAddr = info.getIpAddress();
        String ipString = String.format("%d.%d.%d.1",
                (ipAddr>>0)&0xff, (ipAddr>>8)&0xff, (ipAddr>>16)&0xff);
        Log.d("MainActivity", "peer's address is:" + ipString);
        globals.setPeerIPAddress(ipString);

        mWifiStatusUpdateThread = new WifiStatusUpdateThread();
        mWifiStatusUpdateThread.start();

        final CustomView customView = findViewById(R.id.customView);

        tvDistance = findViewById(R.id.tvDistance);
        tvDistance.setText("0");
        tvAngle = findViewById(R.id.tvAngle);
        tvAngle.setText("0");

        tvSSID = findViewById(R.id.tvSSID);
        tvSSID.setText(getWifiSSID(MainActivity.this));
        tvIpAddress = findViewById(R.id.tvIpAddress);
        tvIpAddress.setText(getWifiIPAddress(MainActivity.this));
        tvPortNumber = findViewById(R.id.tvPortNumber);
        tvPortNumber.setText(globals.getMyPortNumber());

        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvProvider = findViewById(R.id.tvProvider);
        tvMeasCount = findViewById(R.id.tvMeasCount);
        tvMeasInterval = findViewById(R.id.tvMeasInterval);
        tvFixedLatitude = findViewById(R.id.tvFixedLatitude);
        tvFixedLongitude = findViewById(R.id.tvFixedLongitude);

        tvPeerLatitude = findViewById(R.id.tvPeerLatitude);
        tvPeerLongitude = findViewById(R.id.tvPeerLongitude);
        tvPeerProvider = findViewById(R.id.tvPeerProvider);
        tvReceiveCount = findViewById(R.id.tvReceiveCount);
        tvReceiveInterval = findViewById(R.id.tvReceiveInterval);

        tvSendDist = findViewById(R.id.tvSendDist);
        tvSendAngle = findViewById(R.id.tvSendAngle);

        btLogStart = findViewById(R.id.btLogStart);
        btLogStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                path = Environment.getExternalStorageDirectory().getPath() + File.separator + "PClog";
//        path = "/storage/1A80-DF3D" + File.separator + this.getPackageName();
//        Log.d("MainActivity", "external storage path is:" + path);

                Log.d("MainActivity", "path is:" + path);

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    File f = new File(path);
                    if (!f.exists()) {
                        boolean result = f.mkdir();
                        Log.d("MainActivity", "dir create result is:" + result);
                    }

                    Date d = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
                    String dEdit = sdf.format(d);
//                fileName = path + File.separator + dEdit + ".txt";
                    fileName = path + File.separator + dEdit + ".txt";
                    Log.d("MainActivity", "fileName is:" + fileName);

                    isFileSaving = true;

                    btLogStart.setEnabled(false);
                    btLogStop.setEnabled(true);
                    Toast.makeText(MainActivity.this, "測定ログ取得開始", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "ストレージアクセスが許可されていません", Toast.LENGTH_SHORT).show();
                    String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
                    return;
                }

            }
        });

        btLogStop = findViewById(R.id.btLogStop);
        btLogStop.setEnabled(false);
        btLogStop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                isFileSaving = false;

                btLogStart.setEnabled(true);
                btLogStop.setEnabled(false);
                Toast.makeText(MainActivity.this, "測定ログ取得終了", Toast.LENGTH_SHORT).show();
            }
        });
        btFixed = findViewById(R.id.btFixed);
        btFixed.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                fixedLatitude = latitude;
                fixedLongitude = longitude;
                tvFixedLatitude.setBackgroundColor(Color.argb(255,255,255,255));
                tvFixedLatitude.setText(Double.toString(fixedLatitude));
                tvFixedLongitude.setBackgroundColor(Color.argb(255,255,255,255));
                tvFixedLongitude.setText(Double.toString(fixedLongitude));
                angle = "0";
                tvAngle.setText(angle);
                isFixed = true;
                isFirstReceive = true;
                CustomView customView = findViewById(R.id.customView);
                customView.showCanvas(false, Double.parseDouble(angle));
                Log.d("btFixed", "Canvas refreshed");
            }
        });

        btSendDemo1 = findViewById(R.id.btSendDemo1);
        btSendDemo1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dist = "30";
                angle = "40";
                tvSendDist.setText(dist);
                tvSendAngle.setText(angle);
                mButtonClicked = view;
                UDPTestSenderThread mUDPSender = new UDPTestSenderThread();
                mUDPSender.start();
                btSendDemo1.setEnabled(false);
            }
        });

        btSendDemo2 = findViewById(R.id.btSendDemo2);
        btSendDemo2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dist = "10";
                angle = "-50";
                tvSendDist.setText(dist);
                tvSendAngle.setText(angle);
                mButtonClicked = view;
                UDPTestSenderThread mUDPSender = new UDPTestSenderThread();
                mUDPSender.start();
                btSendDemo2.setEnabled(false);
            }
        });

        btContStart = findViewById(R.id.btContStart);
        btContStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mUDPTestCSThread = new UDPTestContSenderThread();
                mUDPTestCSThread.start();
                btContStart.setEnabled(false);
                btContStop.setEnabled(true);
                Toast.makeText(MainActivity.this, "連続送信開始", Toast.LENGTH_SHORT).show();
            }
        });

        btContStop = findViewById(R.id.btContStop);
        btContStop.setEnabled(false);
        btContStop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mUDPTestCSThread.onStop();
                btContStop.setEnabled(false);
                Toast.makeText(MainActivity.this, "連続送信停止", Toast.LENGTH_SHORT).show();
            }
        });

        Button btPeerRest = findViewById(R.id.btPeerReset);
        btPeerRest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvPeerLatitude.setText("");
                tvPeerLongitude.setText("");
                tvPeerProvider.setText("");
                receiveCount = 0;
                tvReceiveCount.setText(String.valueOf(receiveCount));
                tvReceiveInterval.setText("");
            }
        });
        Button button1 = findViewById(R.id.btDemo1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double angle = 80;
                customView.showCanvas(false, angle);
                Log.d("MainActivity", "bt_number=1");
                tvDistance.setText("10");
                tvAngle.setText("" + angle);
            }
        });
        Button button2 = findViewById(R.id.btDemo2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double angle = 60;
                customView.showCanvas(false, angle);
                Log.d("MainActivity", "bt_number=2");
                tvDistance.setText("20");
                tvAngle.setText("" + angle);
            }
        });
        Button button3 = findViewById(R.id.btDemo3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double angle = -10;
                customView.showCanvas(false, angle);
                Log.d("MainActivity", "bt_number=3");
                tvDistance.setText("5");
                tvAngle.setText("" + angle);
            }
        });
        Button button4 = findViewById(R.id.btReset);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double angle = 0;
                CustomView customView = findViewById(R.id.customView);
                customView.showCanvas(true, angle);
                Log.d("MainActivity", "bt_number=4");
                tvDistance.setText("0");
                tvAngle.setText("" + angle);
            }
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, this);
            mUDPTestReceiver = new UDPMeasReceiverThread();
            mUDPTestReceiver.start();
        } else {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE_ACCESS_FINE_LOCATION_PERMISSION);
            return;
        }


    }

    @Override
    protected void onResume() {
        Log.d("MainActivity", "onResume()");
        super.onResume();

        lastMeasTime = System.currentTimeMillis();
    }

    @Override
    protected void onPause() {
        Log.d("MainActivity", "onPause()");
//        locationManager.removeUpdates(this);
        super.onPause();
    }

    @Override
    public void onRestart() {
        Log.d("MainActivity", "onRestart()");
        tvPortNumber.setText(globals.getMyPortNumber());
        commPort = Integer.parseInt(globals.getMyPortNumber());
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1000);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, this);
        super.onRestart();
    }

    @Override
    public void onDestroy() {
        Log.d("MainActivity", "onDestroy()");
        locationManager.removeUpdates(this);
        mUDPTestReceiver.onStop();
        mWifiStatusUpdateThread.onStop();
        super.onDestroy();
    }

    private static String getWifiIPAddress(Context context) {
        WifiManager manager = (WifiManager)context.getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        int ipAddr = info.getIpAddress();
        String ipString = String.format("%d.%d.%d.%d",
                (ipAddr>>0)&0xff, (ipAddr>>8)&0xff, (ipAddr>>16)&0xff, (ipAddr>>24)&0xff);
        return ipString;
    }

    private static String getWifiSSID(Context context) {
        WifiManager manager = (WifiManager)context.getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String  ssid = info.getSSID();
        return ssid;
    }

    class UDPMeasReceiverThread extends Thread {
        private static final String TAG="UDPMeasReceiverThread";

        DatagramSocket mDatagramRecvSocket= null;
        boolean mIsArive= false; //スレッド生存フラグ

        Map<String, String> receiveMap = new HashMap<>();

        public UDPMeasReceiverThread() {
            super();
            // ソケット生成
            commPort = Integer.parseInt(globals.getMyPortNumber());
            Log.d(TAG, "Globalsポート番号:"+ commPort);
            try {
                mDatagramRecvSocket = new DatagramSocket(null);
                mDatagramRecvSocket.setReuseAddress(true);
                mDatagramRecvSocket.setBroadcast(true);
                mDatagramRecvSocket.bind(new InetSocketAddress(commPort));
            } catch (SocketException e) {
                e.printStackTrace();
            }

        }
        @Override
        public void start() {
            Log.d(TAG,"start()");
            mIsArive= true;
            Log.d(TAG,"mIsArive status:"+ mIsArive);
            super.start();
        }

        public void onStop() {
            Log.d(TAG,"onStop()");
            mIsArive= false;
            mDatagramRecvSocket.close();
            mDatagramRecvSocket= null;
            Log.d(TAG,"mIsArive status:"+ mIsArive);
        }

        // 受信用スレッドのメイン関数
        @Override
        public void run() {
            // スレッドループ開始
            Log.d(TAG,"In run(): thread start.");
            // UDPパケットの受け皿生成（最初に一度だけ生成して使いまわし）
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            try {
                while (mIsArive) {
                    mDatagramRecvSocket.receive(receivePacket); //UDPパケット受信
                    try {
                        ByteArrayInputStream bis = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength()); //バイトストリームから入力
                        ObjectInput in = new ObjectInputStream(bis);
                        receiveMap = (Map<String, String>)in.readObject();
                    }catch (Exception ex) {
                    }
                    Log.d(TAG,"In run(): packet received :" + receiveMap);

                    if(receiveMap.containsKey("dist")) { //テストdist,angleデータを受け取ったとき
                        final String dist = receiveMap.get("dist");
                        Log.d(TAG,"dist: " + dist);
                        final String angle = receiveMap.get("angle");
                        Log.d(TAG,"angle: " + angle);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvDistance.setText(dist);
                                tvAngle.setText(angle);
                                CustomView customView = findViewById(R.id.customView);
                                customView.showCanvas(false, Double.parseDouble(angle));
                                Log.d(TAG, "In run(): Canvas refreshed");
                            }
                        });
                    }else if(receiveMap.containsKey("sendLatitude")) { //緯度経度を受け取ったとき
                        final String sendLatitude = receiveMap.get("sendLatitude");
                        Log.d(TAG,"sendLatitude: " + sendLatitude);
                        final String sendLongitude = receiveMap.get("sendLongitude");
                        Log.d(TAG,"sendLongitude: " + sendLongitude);
                        final String sendProvieder = receiveMap.get("sendProvider");
                        Log.d(TAG,"sendProvieder: " + sendLongitude);

                        receiveCount++;
                        mHandler.post(new Runnable() { //画面更新
                            @Override
                            public void run() {
                                tvPeerLatitude.setText(sendLatitude);
                                tvPeerLongitude.setText(sendLongitude);
                                tvPeerProvider.setText(sendProvieder);
                                tvReceiveCount.setText(String.valueOf(receiveCount));
                            }
                        });
                        if(lastReceiveTime ==0){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvReceiveInterval.setText(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(lastReceiveTime)));
                                    lastReceiveTime = System.currentTimeMillis();
                                }
                            });
                        }else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    long receiveInterval = System.currentTimeMillis() - lastReceiveTime;
                                    lastReceiveTime = System.currentTimeMillis();
                                    tvReceiveInterval.setText(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(receiveInterval)));
                                }
                            });
                        }

                        if(isFixed){
                            //自己緯度経度、受信緯度経度から距離、角度算出
                            //常に方位角は計算が必要なので、⊿x、⊿yは毎回求める
                            double dLatitude = (fixedLatitude - Double.parseDouble(sendLatitude)) * (Math.PI / 180); //緯度変位[rad]
                            double dLongitude = (fixedLongitude - Double.parseDouble(sendLongitude)) * (Math.PI / 180); //経度変位[rad]
                            double dy = EARTHRADIUS * dLatitude; //緯度方向変位距離
                            double dx = EARTHRADIUS * Math.cos(fixedLatitude) * dLongitude; //経度方向変位距離
                            long length = Math.round(Math.sqrt((dx*dx + dy*dy)));
                            double angle2 = Math.atan((dy/dx)) * 180 / Math.PI;
                            dist = String.valueOf(length);

                            Log.d(TAG, "isFirstReceive is:" + isFirstReceive);
                            if(isFirstReceive) {
                                initAngle = angle2;
                                Log.d(TAG, "initAngle is:" + initAngle);
                                mHandler.post(new Runnable() { //画面更新
                                    @Override
                                    public void run() {
                                        tvFixedLatitude.setBackgroundColor(Color.argb(255,255,250,205));
                                        tvFixedLongitude.setBackgroundColor(Color.argb(255,255,250,205));
                                        tvDistance.setText(dist);
                                        tvAngle.setText(angle);
                                        CustomView customView = findViewById(R.id.customView);
                                        customView.showCanvas(false, Double.parseDouble(angle));
                                        Log.d(TAG, "In run(): Canvas refreshed");
                                    }
                                });
                                isFirstReceive = false;
                            } else {
                                angle2 = - (initAngle - angle2);
                                angle = String.valueOf(Math.round(angle2));
                                final double canvasAngle = angle2;
                                mHandler.post(new Runnable() { //画面更新
                                    @Override
                                    public void run() {
                                        tvDistance.setText(dist);
                                        tvAngle.setText(angle);
                                        CustomView customView = findViewById(R.id.customView);
                                        customView.showCanvas(false, canvasAngle);
                                        Log.d(TAG, "In run(): Canvas refreshed");
                                    }
                                });
                            }
                        }

                    }

                }
            }catch( Exception e ) {
                e.printStackTrace();
            }
            Log.d(TAG,"In run(): thread end.");
            // ソケットclose（これをしないと2回目以降の起動ができない）
            if(mDatagramRecvSocket != null){
                mDatagramRecvSocket.close();
                mDatagramRecvSocket= null;
            }
        }
    }

    class UDPMeasSenderThread extends Thread{
        private static final String TAG="UDPMeasSenderThread";

        private UDPMeasSenderThread(){
            super();
        }

        @Override
        public void start() {
            super.start();
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run(){
            Log.d(TAG,"In run(): thread start.");
            try {
                Log.d(TAG,"Peer's IP Address: " + globals.getPeerIPAddress());
                String sendLatitude = String.valueOf(latitude);
                String sendLongitude = String.valueOf(longitude);
                String sendProvider = provider;
                Map<String, String> map = new HashMap<>();
                map.put("sendLatitude", sendLatitude);
                map.put("sendLongitude", sendLongitude);
                map.put("sendProvider", sendProvider);
                UDPObjectTransfer.send(map, globals.getPeerIPAddress(), Integer.parseInt(globals.getPeerPortNumber()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG,"In run(): thread end.");
        }
    }

    class UDPTestSenderThread extends Thread{
        private static final String TAG="UDPMeasReceiverThread";

        private UDPTestSenderThread(){
            super();
        }

        @Override
        public void start() {
            super.start();
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run(){
            Log.d(TAG,"In run(): thread start.");
            final int button_id = mButtonClicked.getId();
            Map<String, String> map = new HashMap<>();
            map.put("dist", dist);
            map.put("angle", angle);
            try {
                Log.d(TAG,"Peer's IP Address: " + globals.getPeerIPAddress());
                UDPObjectTransfer.send(map, globals.getPeerIPAddress(), Integer.parseInt(globals.getPeerPortNumber()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "送信済", Toast.LENGTH_SHORT).show();
                    switch (button_id) {
                        case R.id.btSendDemo1:
                            btSendDemo1.setEnabled(true);
                            break;
                        case R.id.btSendDemo2:
                            btSendDemo2.setEnabled(true);
                            break;
                    }

                }
            });
            Log.d(TAG,"In run(): thread end.");

        }
    }

    class UDPTestContSenderThread extends Thread{
        private static final String TAG="UDPTestContSenderThread";
        boolean mIsArive= false;

        private UDPTestContSenderThread(){
            super();
        }

        @Override
        public void start() {
            Log.d(TAG,"start()");
            mIsArive= true;
            Log.d(TAG,"mIsArive status:"+ mIsArive);
            super.start();
        }

        public void onStop() {
            Log.d(TAG,"onStop()");
            mIsArive= false;
            Log.d(TAG,"mIsArive status:"+ mIsArive);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run(){
            Log.d(TAG,"In run(): thread start.");
            int cnt = -60;
            boolean reverse = false;
            try {
                while(mIsArive){
                    dist = String.valueOf(cnt);
                    angle= String.valueOf(cnt);
                    Map<String, String> map = new HashMap<>();
                    map.put("dist", dist);
                    map.put("angle", angle);
                    UDPObjectTransfer.send(map, globals.getPeerIPAddress(), Integer.parseInt(globals.getPeerPortNumber()));
                    if(!reverse) {
                        if (cnt != 60) {
                            cnt += 10;
                        } else {
                            reverse = true;
                            cnt -= 10;
                        }
                    }else {
                        if (cnt != -60) {
                            cnt -= 10;
                        } else {
                            reverse = false;
                            cnt += 10;
                        }
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvSendDist.setText(dist);
                            tvSendAngle.setText(angle);
                        }
                    });
                    Thread.sleep(1000);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    btContStart.setEnabled(true);
                    Toast.makeText(MainActivity.this, "連続送信スレッド停止", Toast.LENGTH_SHORT).show();
                }
            });
            Log.d(TAG,"In run(): thread end.");
        }
    }

    class WifiStatusUpdateThread extends Thread {
        private static final String TAG="WifiStatusUpdateThread";
        boolean mIsArive= false;

        public WifiStatusUpdateThread() {
            super();
        }

        @Override
        public void start() {
            Log.d(TAG,"start()");
            mIsArive= true;
            Log.d(TAG,"mIsArive status:"+ mIsArive);
            super.start();
        }

        public void onStop() {
            Log.d(TAG,"onStop()");
            mIsArive= false;
            Log.d(TAG,"mIsArive status:"+ mIsArive);
        }

        @Override
        public void run() {
            Log.d(TAG,"In run(): thread start.");
            while (mIsArive) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvSSID.setText(getWifiSSID(MainActivity.this));
                        tvIpAddress.setText(getWifiIPAddress(MainActivity.this));
                        Log.d(TAG, "run(): mHandler.post() executed.");
                    }
                });
            }
            Log.d(TAG,"In run(): thread end.");
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        customViewWidth = findViewById(R.id.customView).getWidth();
        customViewHeight = findViewById(R.id.customView).getHeight();
        Log.d("MainActivity", "CustomView幅:"+ customViewWidth);
        Log.d("MainActivity", "CustomView高:"+ customViewHeight);
        int orientation = getResources().getConfiguration().orientation;
        CustomView customView = findViewById(R.id.customView);
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams)customView.getLayoutParams();

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横向きの場合
            Log.d("MainActivity", "onWindowFocusChanged():ORIENTATION_LANDSCAPE"+ customViewHeight);
            customViewHeight = customViewWidth;
            marginLayoutParams.height = customViewHeight;
            Log.d("MainActivity", "CustomView高(修正):"+ customViewHeight);
            customView.setLayoutParams(marginLayoutParams);
        }

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d("MainActivity", "onWindowFocusChanged():ORIENTATION_PORTRAIT"+ customViewHeight);
            // 縦向きの場合
            customView.setLayoutParams(marginLayoutParams);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d("MainActivity", "onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);
        //処理
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onLocationChanged(Location location) {
        Log.d("MainActivity", "onLocationChanged():" + location.getProvider() + " " + String.valueOf(location.getAccuracy()) + " " + location.getTime());
        if (isMeasStart) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            provider = location.getProvider();

            tvLatitude.setText(Double.toString(latitude));
            tvLongitude.setText(Double.toString(longitude));
            tvProvider.setText(provider);
            measCount++;
            tvMeasCount.setText(String.valueOf(measCount));
            long receiveInterval = System.currentTimeMillis() - lastMeasTime;
            lastMeasTime = System.currentTimeMillis();
            tvMeasInterval.setText(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(receiveInterval)));

            UDPMeasSenderThread mUDPMeasSenderThread = new UDPMeasSenderThread();
            mUDPMeasSenderThread.start();

            if(isFileSaving){
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(new File(fileName),true);
                    OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                    BufferedWriter bf = new BufferedWriter(osw);

                    Date d = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd/_HH:mm:ss");
                    String dEdit = sdf.format(d);
                    bf.write(dEdit + ", ");
                    bf.write(Double.toString(latitude) + ", ");
                    bf.write(Double.toString(longitude) + ", ");
                    bf.write(provider + ", ");
                    bf.write(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(receiveInterval)) + "\n");
                    bf.flush();
                    bf.close();

                    Log.d("onLocationChanged()", "File written.");

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if(fos != null)
                            fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    fos = null;
                }
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == REQUEST_CODE_ACCESS_FINE_LOCATION_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, this);
            mUDPTestReceiver = new UDPMeasReceiverThread();
            mUDPTestReceiver.start();
        }
        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menuListOptionSetting:
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
