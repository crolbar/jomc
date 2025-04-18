package com.jomc;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    final String ip = "192.168.1.12";
    final int port = 6742;

    ORGB orgb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.orgb = new ORGB(ip, port, this);

        Button con_button = findViewById(R.id.connect_button);
        Button dis_button = findViewById(R.id.disconnect_button);

        con_button.setOnClickListener(this::onButtonClickConnect);

        dis_button.setOnClickListener((v) -> {
            orgb.closeConn();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orgb.isConnAlive()) {
            orgb.closeConn();
        }
    }

    private void onButtonClickConnect(View v) {
        orgb.updateControllers();
    }
}
