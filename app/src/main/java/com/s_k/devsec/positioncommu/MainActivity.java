package com.s_k.devsec.positioncommu;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationListener {

    TextView tvDistance;
    TextView tvAngle;
    TextView tvPortNumber;
    TextView tvSSID;
    TextView tvIpAddress;

    static int customViewWidth;
    static int customViewHeight;

    Globals globals;
    UDPReceiverThread mUDPReceiver = null;
    WifiStatusUpdateThread mWifiStatusUpdateThread = null;
    int commPort;

    Handler mHandler;

    String naviIpAddress = "";
    String naviPortNumber = "5000";

    LocationManager locationManager;
    double latitude = 0; //緯度フィールド
    double longitude = 0; //経度フィールド
    boolean isMeasStart = true;

    String dist = "";
    String angle = "";

    View mButtonClicked;

    UDPContSenderThread mUDPCSThread;

    TextView tvLatitude;
    TextView tvLongitude;
    TextView tvProvider;
    TextView tvSendDist;
    TextView tvSendAngle;
    Button btMeasStart;
    Button btMeasStop;
    Button btSendDemo1;
    Button btSendDemo2;
    Button btContStart;
    Button btContStop;
    EditText etIpAddress;
    EditText etPortNumber;
    Button btIpSetting;
    Button btPortSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        globals = (Globals) this.getApplication();

        tvDistance = findViewById(R.id.tvDistance);
        tvDistance.setText("0");
        tvAngle = findViewById(R.id.tvAngle);
        tvAngle.setText("0");
        tvSSID = findViewById(R.id.tvSSID);
        tvSSID.setText(getWifiSSID(MainActivity.this));
        tvIpAddress = findViewById(R.id.tvIpAddress);
        tvIpAddress.setText(getWifiIPAddress(MainActivity.this));
        tvPortNumber = findViewById(R.id.tvPortNumber);
        tvPortNumber.setText(globals.getPortNumber());

        final CustomView customView = findViewById(R.id.customView);

        Button button1 = findViewById(R.id.btDemo1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double angle = 30;
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

        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvProvider = findViewById(R.id.tvProvider);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1000);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

        tvSendDist = findViewById(R.id.tvSendDist);
        tvSendAngle = findViewById(R.id.tvSendAngle);

        etIpAddress = findViewById(R.id.etIpAddress);
        etIpAddress.setText(getWifiIPAddress3octet(MainActivity.this));
        etPortNumber = findViewById(R.id.etPortNumber);
        etPortNumber.setText(naviPortNumber);

        btMeasStart = findViewById(R.id.btMeasStart);
        btMeasStart.setEnabled(false);
        btMeasStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                isMeasStart = true;
                Toast.makeText(MainActivity.this, "位置情報取得開始", Toast.LENGTH_SHORT).show();
            }
        });

        btMeasStop = findViewById(R.id.btMeasStop);
        btMeasStop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                isMeasStart = false;
                tvLatitude.setText("");
                tvLongitude.setText("");
                tvProvider.setText("");
                btMeasStart.setEnabled(true);
                btMeasStop.setEnabled(false);
                Toast.makeText(MainActivity.this, "位置情報取得停止", Toast.LENGTH_SHORT).show();
            }
        });

        btSendDemo1 = findViewById(R.id.btSendDemo1);
        btSendDemo1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dist = "30";
                angle = "40";
                tvSendDist.setText(dist);
                tvAngle.setText(angle);
                mButtonClicked = view;
                UDPSenderThread mUDPSender = new UDPSenderThread();
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
                tvAngle.setText(angle);
                mButtonClicked = view;
                UDPSenderThread mUDPSender = new UDPSenderThread();
                mUDPSender.start();
                btSendDemo2.setEnabled(false);
            }
        });

        btContStart = findViewById(R.id.btContStart);
        btContStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mUDPCSThread = new UDPContSenderThread();
                mUDPCSThread.start();
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
                mUDPCSThread.onStop();
                btContStop.setEnabled(false);
                Toast.makeText(MainActivity.this, "連続送信停止", Toast.LENGTH_SHORT).show();
            }
        });

        btIpSetting = findViewById(R.id.btIpSetting);
        btIpSetting.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                String getString = etIpAddress.getText().toString();
                if(getString.length() != 0){
                    naviIpAddress = getString;
                    Toast.makeText(MainActivity.this, naviIpAddress + " を送信先IPアドレスに設定しました", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this, "IPアドレスが入力されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btPortSetting = findViewById(R.id.btPortSetting);
        btPortSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String getString = etPortNumber.getText().toString();
                if(getString.length() != 0){
                    naviPortNumber = getString;
                    Toast.makeText(MainActivity.this, naviPortNumber + " を送信先ポート番号に設定しました", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this, "ポート番号が入力されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mHandler = new Handler();

        mUDPReceiver = new UDPReceiverThread();
        mUDPReceiver.start();

        mWifiStatusUpdateThread = new WifiStatusUpdateThread();
        mWifiStatusUpdateThread.start();
    }

    private static String getWifiIPAddress(Context context) {
        WifiManager manager = (WifiManager)context.getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        int ipAddr = info.getIpAddress();
        String ipString = String.format("%d.%d.%d.%d",
                (ipAddr>>0)&0xff, (ipAddr>>8)&0xff, (ipAddr>>16)&0xff, (ipAddr>>24)&0xff);
        return ipString;
    }

    private static String getWifiIPAddress3octet(Context context) {
        WifiManager manager = (WifiManager)context.getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        int ipAddr = info.getIpAddress();
        String ipString = String.format("%d.%d.%d.",
                (ipAddr>>0)&0xff, (ipAddr>>8)&0xff, (ipAddr>>16)&0xff);
        return ipString;
    }

    private static String getWifiSSID(Context context) {
        WifiManager manager = (WifiManager)context.getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String  ssid = info.getSSID();
        return ssid;
    }

    class UDPReceiverThread extends Thread {
        private static final String TAG="UDPReceiverThread";

        DatagramSocket mDatagramRecvSocket= null;
        boolean mIsArive= false;
        DatagramPacket receivePacket = null;
        byte[] receiveBuffer = null;

        Map<String, String> receiveMap = new HashMap<>();

        public UDPReceiverThread() {
            super();
            commPort = Integer.parseInt(globals.getPortNumber());
            Log.d(TAG, "Globalsポート番号:"+ commPort);
            // ソケット生成
            try {
                mDatagramRecvSocket= new DatagramSocket(commPort);
            }catch( Exception e ) {
                e.printStackTrace();
            }

        }
        @Override
        public void start() {
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
            // UDPパケット生成（最初に一度だけ生成して使いまわし）
            receiveBuffer = new byte[1024];
            receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            // スレッドループ開始
            Log.d(TAG,"In run(): thread start.");
            try {
                while (mIsArive) {
                    // UDPパケット受信
                    mDatagramRecvSocket.receive(receivePacket);

                    try {
                        ByteArrayInputStream bis = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
                        ObjectInput in = new ObjectInputStream(bis);
                        receiveMap = (Map<String, String>)in.readObject();
                    }catch (Exception ex) {
                    }
                    Log.d(TAG,"In run(): packet received :" + receiveMap);

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
                            Log.d(TAG, "In run(): Canvas refleshed");
                        }
                    });

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
            receivePacket= null;
            receiveBuffer= null;
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
            mIsArive= true;
            Log.d(TAG,"mIsArive status:"+ mIsArive);
            super.start();
        }

        public void onStop() {
            Log.d(TAG,"onStop()");
            mIsArive= false;
            Log.d(TAG,"mIsArive status:"+ mIsArive);
        }

//        int cnt = 0;

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
//                        tvSSID.setText(String.valueOf(cnt));
//                        tvIpAddress.setText(String.valueOf(cnt));
//                        cnt++;
                        tvSSID.setText(getWifiSSID(MainActivity.this));
                        tvIpAddress.setText(getWifiIPAddress(MainActivity.this));
                        Log.d(TAG, "run(): mHandler.post() executed.");
                    }
                });
            }
            Log.d(TAG,"In run(): thread end.");
        }
    }

    class UDPSenderThread extends Thread{
        private static final String TAG="UDPReceiverThread";

        private UDPSenderThread(){
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
            Map<String, String> map = new HashMap<>(); // 適当なデータを用意
            map.put("dist", dist);
            map.put("angle", angle);
            try {
                UDPObjectTransfer.send(map, naviIpAddress, Integer.parseInt(naviPortNumber));
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

    class UDPContSenderThread extends Thread{
        private static final String TAG="UDPContSenderThread";
        boolean mIsArive= false;

        private UDPContSenderThread(){
            super();
        }

        @Override
        public void start() {
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
                    Map<String, String> map = new HashMap<>(); // 適当なデータを用意
                    map.put("dist", dist);
                    map.put("angle", angle);
                    UDPObjectTransfer.send(map, naviIpAddress, Integer.parseInt(naviPortNumber));
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

    @Override
    protected void onResume() {
        Log.d("MainActivity", "onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d("MainActivity", "onPause()");
        super.onPause();
    }

    @Override
    public void onRestart() {
        Log.d("MainActivity", "onRestart()");
        tvPortNumber.setText(globals.getPortNumber());
        commPort = Integer.parseInt(globals.getPortNumber());
        super.onRestart();
    }

    @Override
    public void onDestroy() {
        Log.d("MainActivity", "onDestroy()");
        locationManager.removeUpdates(this);
        mUDPReceiver.onStop();
        mWifiStatusUpdateThread.onStop();
        super.onDestroy();
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
            customViewWidth = customViewHeight;
            marginLayoutParams.width = customViewWidth;
            Log.d("MainActivity", "CustomView幅(修正):"+ customViewWidth);
            customView.setLayoutParams(marginLayoutParams);
        }

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 縦向きの場合
            customViewHeight = customViewWidth;
            marginLayoutParams.height = customViewHeight;
            Log.d("MainActivity", "CustomView高(修正):"+ customViewHeight);
            customView.setLayoutParams(marginLayoutParams);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d("MainActivity", "onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);
        //処理
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("MainActivity", "onLocationChanged():" + location.getProvider() + " " + String.valueOf(location.getAccuracy()) + " " + location.getTime());
        if (isMeasStart) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            tvLatitude.setText(Double.toString(latitude));
            tvLongitude.setText(Double.toString(longitude));
            tvProvider.setText(location.getProvider());
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
        if(requestCode == 1000 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
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
