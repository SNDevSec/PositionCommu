package com.s_k.devsec.positioncommu;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {

    private Globals globals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Intent intent = getIntent();
        globals = (Globals) this.getApplication();

        EditText etPortNumber = findViewById(R.id.etPortNumber);
        etPortNumber.setText(globals.getPortNumber());

        Button btSetPortNumber = findViewById(R.id.btSetPortNumber);
        btSetPortNumber.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                EditText input = findViewById(R.id.etPortNumber);
                String inputStr = input.getText().toString();
                globals.setPortNumber(inputStr);
                Toast.makeText(SettingActivity.this, inputStr + " を待受ポート番号に設定しました", Toast.LENGTH_SHORT).show();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
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
