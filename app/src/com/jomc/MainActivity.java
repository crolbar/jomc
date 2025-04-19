package com.jomc;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;

public class MainActivity extends AppCompatActivity
{
    TextView tiHost;
    TextView tiPort;
    Button bConnect;

    ORGB orgb;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.orgb = new ORGB(this);

        tiHost = findViewById(R.id.host);
        tiPort = findViewById(R.id.port);
        bConnect = findViewById(R.id.connect_button);
        bConnect.setOnClickListener(this::onConnectButtonClick);

        ColorPickerView colorPickerView = findViewById(R.id.color_picker_view);
        colorPickerView.attachBrightnessSlider(
          findViewById(R.id.color_picker_value));
    }

    @Override protected void onDestroy()
    {
        super.onDestroy();
        if (orgb.isConnAlive()) {
            orgb.closeConn();
        }
    }

    private void onConnectButtonClick(View v)
    {
        // have connection
        if (orgb.isConnAlive()) {
            this.orgb.closeConn();
            this.orgb.executorAdd(() -> {
                if (!this.orgb.isConnAlive()) {
                    bConnect.setText("Connect");
                    bConnect.requestLayout();
                }
            });
            return;
        }

        String ip = tiHost.getText().toString();
        int port = Integer.parseInt(tiPort.getText().toString());

        this.orgb.connect(ip, port);

        this.orgb.executorAdd(() -> {
            if (!this.orgb.isConnAlive()) {
                return;
            }
            bConnect.setText("Disconnect");
            bConnect.requestLayout();

            this.orgb.updateControllerCount();

            ColorPickerView colorPickerView =
              findViewById(R.id.color_picker_view);
            colorPickerView.setColorListener(new ColorListener() {
                @Override
                public void onColorSelected(int color, boolean fromUser)
                {
                    orgb.setColor(new byte[] {
                      (byte)Color.red(color),
                      (byte)Color.green(color),
                      (byte)Color.blue(color),
                      0,
                    });
                }
            });
        });
    }
}
