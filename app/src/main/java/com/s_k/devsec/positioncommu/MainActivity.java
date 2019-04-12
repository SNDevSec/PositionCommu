package com.s_k.devsec.positioncommu;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.annotation.NonNull;
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
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements LocationListener {

    SharedPreferences sharedPref;
    private static final String PREF_FILE_NAME = "DataStore";

    TextView tvDistance;
    TextView tvAngle;

    TextView tvSSID;
    TextView tvIpAddress;
    TextView tvPortNumber;
    TextView tvLatitude;
    TextView tvLongitude;
    TextView tvProvider;
    TextView tvMeasCount;
    TextView tvMeasInterval;
    TextView tvFixedLatitude;
    TextView tvFixedLongitude;

    TextView tvPeerIpAddress;
    TextView tvPeerPortNumber;
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

    UDPReceiverThread mUDPTestReceiver = null;
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
    String myLogName = "";
    String receiveLogName = "";

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
        sharedPref = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);

        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String ipString = sharedPref.getString("PEER_IP_ADDRESS", "");
        if(ipString.equals("")){
            int ipAddr = info.getIpAddress();
            ipString = String.format(Locale.US, "%d.%d.%d.1",
                    (ipAddr)&0xff, (ipAddr>>8)&0xff, (ipAddr>>16)&0xff);
        }
        Log.d("MainActivity", "Initial peer's address is:" + ipString);
        globals.setPeerIPAddress(ipString);

        mWifiStatusUpdateThread = new WifiStatusUpdateThread();
        mWifiStatusUpdateThread.start();

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横向きの場合
            LinearLayout llMain = findViewById(R.id.llMain);
            llMain.setOrientation(LinearLayout.HORIZONTAL);
        }

        final CustomView customView = findViewById(R.id.customView);
        final ViewTreeObserver vto = customView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d("MainActivity", "onGlobalLayout()");
                customViewWidth = findViewById(R.id.customView).getWidth();
                customViewHeight = findViewById(R.id.customView).getHeight();
                Log.d("MainActivity", "CustomView幅:"+ customViewWidth);
                Log.d("MainActivity", "CustomView高:"+ customViewHeight);
                int orientation = getResources().getConfiguration().orientation;
                CustomView customView = findViewById(R.id.customView);
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams)customView.getLayoutParams();

                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Log.d("MainActivity", "onGlobalLayout():ORIENTATION_PORTRAIT"+ customViewHeight);
                    // 縦向きの場合
                    customView.setLayoutParams(marginLayoutParams);
                }

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // 横向きの場合
                    Log.d("MainActivity", "onGlobalLayout():ORIENTATION_LANDSCAPE"+ customViewHeight);
                    customViewHeight = customViewWidth;
                    marginLayoutParams.height = customViewHeight;
                    Log.d("MainActivity", "CustomView高(修正):"+ customViewHeight);
                    customView.setLayoutParams(marginLayoutParams);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    customView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    customView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

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

        tvPeerIpAddress = findViewById(R.id.tvPeerIpAddress);
        tvPeerIpAddress.setText(globals.getPeerIPAddress());
        tvPeerPortNumber = findViewById(R.id.tvPeerPortNumber);
        tvPeerPortNumber.setText(globals.getPeerPortNumber());

        tvPeerLatitude = findViewById(R.id.tvPeerLatitude);
        tvPeerLongitude = findViewById(R.id.tvPeerLongitude);
        tvPeerProvider = findViewById(R.id.tvPeerProvider);
        tvReceiveCount = findViewById(R.id.tvReceiveCount);
        tvReceiveInterval = findViewById(R.id.tvReceiveInterval);

        tvSendDist = findViewById(R.id.tvSendDist);
        tvSendAngle = findViewById(R.id.tvSendAngle);

        //LOG STARTボタンの動作
        btLogStart = findViewById(R.id.btLogStart);
        btLogStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "btLogStart.onClick()");
                //内部ストレージのパスを取得し、そこにtxtログ格納フォルダ(PClog)へのパスを追記
                path = Environment.getExternalStorageDirectory().getPath() + File.separator + "PClog";
                Log.d("btLogStart.onClick", "Log storing path is:" + path);

                //パーミッション取得済みならファイル生成
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    File f = new File(path);
                    if (!f.exists()) {
                        boolean result = f.mkdir();
                        Log.d("btLogStart.onClick", "dir create result is:" + result);
                    }

                    //LOG START押下の度に、現在日時からtxtファイル名を生成
                    Date d = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmm", Locale.US);
                    String dEdit = sdf.format(d);
                    myLogName = path + File.separator + dEdit + "_"+ Build.MODEL + ".txt";
                    Log.d("btLogStart.onClick", "myLogName is:" + myLogName);
                    //受信用ログファイルについては、ファイル名の途中まで作成しておく
                    //(日付をmyLogと合わせるため)
                    receiveLogName = path + File.separator + dEdit + "_";

                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(new File(myLogName),true);
                        BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(fos));
                        bf.write("Date, ");
                        bf.write("Latitude, ");
                        bf.write("Longitude, ");
                        bf.write("Provider, ");
                        bf.write("Interval\n");
                        bf.flush();
                        bf.close();

                        Log.d("onLocationChanged()", "Row Titles written.");

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
                    }

                    //onLocationChanged()発生時に、合わせて上記txtにログが出力されるフラグをON
                    isFileSaving = true;

                    btLogStart.setEnabled(false);
                    btLogStop.setEnabled(true);
                    Toast.makeText(MainActivity.this, "測定ログ取得開始", Toast.LENGTH_SHORT).show();
                } else { //未取得ならパーミッション取得要求
                    Toast.makeText(MainActivity.this, "ログ出力不可能(ストレージアクセス未許可の為)", Toast.LENGTH_SHORT).show();
                    String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
                }
            }
        });

        //LOG STOPボタンの動作
        btLogStop = findViewById(R.id.btLogStop);
        btLogStop.setEnabled(false);
        btLogStop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "btLogStop.onClick()");
                //onLocationChanged()発生時に、合わせて上記txtにログが出力しなくするだけ
                isFileSaving = false;

                btLogStart.setEnabled(true);
                btLogStop.setEnabled(false);
                Toast.makeText(MainActivity.this, "測定ログ取得終了", Toast.LENGTH_SHORT).show();
            }
        });

        //FIXボタンの動作
        btFixed = findViewById(R.id.btFixed);
        btFixed.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "btFixed.onClick()");
                fixedLatitude = latitude;
                fixedLongitude = longitude;
                tvFixedLatitude.setBackgroundColor(Color.argb(255,255,255,255));
                tvFixedLatitude.setText(String.valueOf(fixedLatitude));
                tvFixedLongitude.setBackgroundColor(Color.argb(255,255,255,255));
                tvFixedLongitude.setText(String.valueOf(fixedLongitude));
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

        //位置情報取得開始までの手続。
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //パーミッション取得済みならそのまま位置情報取得要求&ピアデバイスからの受信待ちスレッド開始
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, this);
            mUDPTestReceiver = new UDPReceiverThread();
            mUDPTestReceiver.start();
        } else { //未取得ならパーミッション取得要求
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE_ACCESS_FINE_LOCATION_PERMISSION);
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
        super.onRestart();
        tvPortNumber.setText(globals.getMyPortNumber());
        commPort = Integer.parseInt(globals.getMyPortNumber());
        tvPeerIpAddress.setText(globals.getPeerIPAddress());
        tvPeerPortNumber.setText(globals.getPeerPortNumber());
    }

    @Override
    public void onDestroy() {
        Log.d("MainActivity", "onDestroy()");
        locationManager.removeUpdates(this);
        mUDPTestReceiver.onStop();
        mWifiStatusUpdateThread.onStop();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("MainActivity", "onLocationChanged():" + " Provider:" + location.getProvider() + " Acc:" + location.getAccuracy());
        if (isMeasStart) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            provider = location.getProvider();

            tvLatitude.setText(String.valueOf(latitude));
            tvLongitude.setText(String.valueOf(longitude));
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
                    fos = new FileOutputStream(new File(myLogName),true);
                    BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(fos));

                    Date d = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd/_HH:mm:ss", Locale.US);
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
                }
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("MainActivity", "onProviderDisabled()");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("MainActivity", "onProviderEnabled()");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("MainActivity", "onStatusChanged()");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        Log.d("MainActivity", "onRequestPermissionsResult()");
        if(requestCode == REQUEST_CODE_ACCESS_FINE_LOCATION_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, this);
            mUDPTestReceiver = new UDPReceiverThread();
            mUDPTestReceiver.start();
        }
        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("MainActivity", "onCreateOptionsMenu()");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("MainActivity", "onOptionsItemSelected()");
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menuListOptionSetting:
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private static String getWifiIPAddress(Context context) {
        WifiManager manager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        int ipAddr = info.getIpAddress();
        return String.format(Locale.US,"%d.%d.%d.%d",
                (ipAddr)&0xff, (ipAddr>>8)&0xff, (ipAddr>>16)&0xff, (ipAddr>>24)&0xff);
    }

    private static String getWifiSSID(Context context) {
        WifiManager manager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        return info.getSSID();
    }

    /**
     * オブジェクトを送信する。
     *
     * @param map     オブジェクト
     * @param address 宛先アドレス。192.168.1.255のようにネットワークアドレスを指定するとブロードキャスト送信。
     * @param port    宛先ポート。受信側と揃える。
     * @throws IOException シリアライズに失敗した時に発生する
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    static void udpSend(Map<String, String> map, String address, int port) throws IOException {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress IPAddress = InetAddress.getByName(address);
            byte[] sendData = convertToBytes(map);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            clientSocket.send(sendPacket);
        }
    }

    /**
     * オブジェクトをバイト配列に変換する。
     *
     * @param  object Serializableを実装していなければいけない。
     * @return バイト配列
     * @throws IOException シリアライズに失敗した時に発生する
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }

    /**
     * 多種のUDPパケット(テストデータ、測定データ、IPアドレス)を待ち受けるスレッド
     * 受け取ったUDPパケット(Map型オブジェクト)
     */
    class UDPReceiverThread extends Thread {
        private static final String TAG="UDPReceiverThread";

        DatagramSocket mDatagramRecvSocket= null;
        boolean mIsArive= false; //スレッド生存フラグ

        Map<String, String> receiveMap = new HashMap<>();

        long receiveInterval;
        int fileWriteCount = 0;

        UDPReceiverThread() {
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
            Log.d(TAG,"mIsAlive status:"+ mIsArive);
            super.start();
        }

        void onStop() {
            Log.d(TAG,"onStop()");
            mIsArive= false;
            mDatagramRecvSocket.close();
            mDatagramRecvSocket= null;
            Log.d(TAG,"mIsAlive status:"+ mIsArive);
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

                    Date d = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd/_HH:mm:ss", Locale.US);
                    String dEdit = sdf.format(d);

                    if(receiveMap.containsKey("test")) { //テストデータを受け取ったとき
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
                    }else if(receiveMap.containsKey("positionInfo")) { //緯度経度を受け取ったとき
                        final String sendLatitude = receiveMap.get("sendLatitude");
                        Log.d(TAG,"sendLatitude: " + sendLatitude);
                        final String sendLongitude = receiveMap.get("sendLongitude");
                        Log.d(TAG,"sendLongitude: " + sendLongitude);
                        final String sendProvider = receiveMap.get("sendProvider");
                        Log.d(TAG,"sendProvider: " + sendLongitude);
                        final String model = receiveMap.get("model");

                        receiveCount++;
                        mHandler.post(new Runnable() { //画面更新
                            @Override
                            public void run() {
                                tvPeerLatitude.setText(sendLatitude);
                                tvPeerLongitude.setText(sendLongitude);
                                tvPeerProvider.setText(sendProvider);
                                tvReceiveCount.setText(String.valueOf(receiveCount));
                            }
                        });
                        if(lastReceiveTime == 0){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvReceiveInterval.setText("0");
                                }
                            });
                            lastReceiveTime = System.currentTimeMillis();
                        }else {
                            receiveInterval = System.currentTimeMillis() - lastReceiveTime;
                            lastReceiveTime = System.currentTimeMillis();
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvReceiveInterval.setText(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(receiveInterval)));
                                }
                            });
                        }

                        if(isFileSaving){
                            if(fileWriteCount == 0){
                                receiveLogName += model + ".txt";
                                Log.d(TAG, "receiveLogName is:" + receiveLogName);
                            }
                            FileOutputStream fos = null;
                            try {
                                fos = new FileOutputStream(new File(receiveLogName),true);
                                BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(fos));

                                if(fileWriteCount == 0){
                                    bf.write("Date, ");
                                    bf.write("Latitude, ");
                                    bf.write("Longitude, ");
                                    bf.write("Provider, ");
                                    bf.write("Interval, ");
                                    bf.write("dist, ");
                                    bf.write("angle\n");
                                }
                                bf.write(dEdit + ", ");
                                bf.write(sendLatitude + ", ");
                                bf.write(sendLongitude + ", ");
                                bf.write(sendProvider + ", ");
                                bf.write(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(receiveInterval)) + ", ");
                                if(!isFixed){
                                    bf.write(", ");
                                    bf.write("\n");
                                }
                                bf.flush();
                                bf.close();

                                Log.d(TAG, "Received Log File written.");

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
                            }
                            fileWriteCount++;
                        }

                        if(isFixed){
                            //自己緯度経度、受信緯度経度から距離、角度算出
                            //常に方位角は計算が必要なので、⊿x、⊿yは毎回求める
                            double dLatitude = (Double.parseDouble(sendLatitude) - fixedLatitude) * (Math.PI / 180); //緯度変位[rad]
                            double dLongitude = (Double.parseDouble(sendLongitude) - fixedLongitude) * (Math.PI / 180); //経度変位[rad]
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
                            if(isFileSaving){
                                FileOutputStream fos = null;
                                try {
                                    fos = new FileOutputStream(new File(receiveLogName),true);
                                    BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(fos));

                                    bf.write(dist + ", ");
                                    bf.write(angle + "\n");
                                    bf.flush();
                                    bf.close();

                                    Log.d(TAG, "Received Log File written.");

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
                                }
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

    /**
     * 測定データをUDPパケットにして送信する単発スレッド
     * Map型オブジェクトにString型データを詰め込み、バイト変換してバイトストリームで送信する(upsend()メソッド)
     */
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
                map.put("positionInfo", "positionInfo"); //UDPパケットの識別子。測定データ受信時処理に分岐
                map.put("sendLatitude", sendLatitude);
                map.put("sendLongitude", sendLongitude);
                map.put("sendProvider", sendProvider);
                map.put("model", Build.MODEL);
                udpSend(map, globals.getPeerIPAddress(), Integer.parseInt(globals.getPeerPortNumber()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG,"In run(): thread end.");
        }
    }

    /**
     * テストデータをUDPパケットにして送信する単発スレッド
     */
    class UDPTestSenderThread extends Thread{
        private static final String TAG="UDPReceiverThread";

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
            map.put("test", "test"); //UDPパケットの識別子。テストデータ受信時処理に分岐
            map.put("dist", dist);
            map.put("angle", angle);
            try {
                Log.d(TAG,"Peer's IP Address: " + globals.getPeerIPAddress());
                udpSend(map, globals.getPeerIPAddress(), Integer.parseInt(globals.getPeerPortNumber()));
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

    /**
     * スレッド有効期間中、テストデータを送信し続けるスレッド
     */
    class UDPTestContSenderThread extends Thread{
        private static final String TAG="UDPTestContSenderThread";
        boolean mIsAlive = false;

        private UDPTestContSenderThread(){
            super();
        }

        @Override
        public void start() {
            Log.d(TAG,"start()");
            mIsAlive = true;
            Log.d(TAG,"mIsAlive status:"+ mIsAlive);
            super.start();
        }

        void onStop() {
            Log.d(TAG,"onStop()");
            mIsAlive = false;
            Log.d(TAG,"mIsAlive status:"+ mIsAlive);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run(){
            Log.d(TAG,"In run(): thread start.");
            int cnt = -60;
            boolean reverse = false;
            try {
                while(mIsAlive){
                    dist = String.valueOf(cnt);
                    angle= String.valueOf(cnt);
                    Map<String, String> map = new HashMap<>();
                    map.put("test", "test"); //UDPパケットの識別子。テストデータ受信時処理に分岐
                    map.put("dist", dist);
                    map.put("angle", angle);
                    udpSend(map, globals.getPeerIPAddress(), Integer.parseInt(globals.getPeerPortNumber()));
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

    /**
     * 接続先WiFiが変わったら自動的にアプリ上の自端末IP表示を更新するスレッド
     * 5秒置きに現在のIP設定をポーリング&表示を更新し続ける
     */
    class WifiStatusUpdateThread extends Thread {
        private static final String TAG="WifiStatusUpdateThread";
        boolean mIsAlive = false;

        WifiStatusUpdateThread() {
            super();
        }

        @Override
        public void start() {
            Log.d(TAG,"start()");
            mIsAlive = true;
            Log.d(TAG,"mIsAlive status:"+ mIsAlive);
            super.start();
        }

        void onStop() {
            Log.d(TAG,"onStop()");
            mIsAlive = false;
            Log.d(TAG,"mIsAlive status:"+ mIsAlive);
        }

        @Override
        public void run() {
            Log.d(TAG,"In run(): thread start.");
            while (mIsAlive) {
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
//                        Log.d(TAG, "run(): mHandler.post() executed.");
                    }
                });
            }
            Log.d(TAG,"In run(): thread end.");
        }
    }

}
