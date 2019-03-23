package com.s_k.devsec.positioncommu;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class SettingActivity extends AppCompatActivity {

    private Globals globals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        globals = (Globals) this.getApplication();

        EditText etMyPortNumber = findViewById(R.id.etSetMyPorNumber);
        etMyPortNumber.setText(globals.getMyPortNumber());

        Button btSetMyPortNumber = findViewById(R.id.btSetMyPortNumber);
        btSetMyPortNumber.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                EditText input = findViewById(R.id.etSetMyPorNumber);
                String inputStr = input.getText().toString();
                globals.setMyPortNumber(inputStr);
                Toast.makeText(SettingActivity.this, inputStr + " を待受ポート番号に設定しました", Toast.LENGTH_SHORT).show();
            }
        });

        EditText etSetPeerIPAddress = findViewById(R.id.etSetPeerIPAddress);
        etSetPeerIPAddress.setText(getWifiIPAddress3octet(SettingActivity.this));

        Button btSetPeerIPAddress = findViewById(R.id.btSetPeerIPAddress);
        btSetPeerIPAddress.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                EditText input = findViewById(R.id.etSetPeerIPAddress);
                String inputStr = input.getText().toString();
                globals.setPeerIPAddress(inputStr);
                Toast.makeText(SettingActivity.this, inputStr + " を送信IPアドレスに設定しました", Toast.LENGTH_SHORT).show();
            }
        });

        EditText etSetPeerPortNumber = findViewById(R.id.etSetPeerPortNumber);
        etSetPeerPortNumber.setText(globals.getMyPortNumber());

        Button btSetPeerPortNumber = findViewById(R.id.btSetPeerPortNumber);
        btSetPeerPortNumber.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                EditText input = findViewById(R.id.etSetPeerPortNumber);
                String inputStr = input.getText().toString();
                globals.setPeerPortNumber(inputStr);
                Toast.makeText(SettingActivity.this, inputStr + " を送信ポート番号に設定しました", Toast.LENGTH_SHORT).show();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private static String getWifiIPAddress3octet(Context context) {
        WifiManager manager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        int ipAddr = info.getIpAddress();
        return String.format(Locale.US, "%d.%d.%d.",
                (ipAddr)&0xff, (ipAddr>>8)&0xff, (ipAddr>>16)&0xff);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
