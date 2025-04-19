package com.jomc;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;

public class MainActivity extends AppCompatActivity
{
    private final static String PREFS_NAME = "jomc_prefs";
    TextView tiHost;
    TextView tiPort;
    Button bConnect;
    ColorPickerView colorPickerView;

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

        setHostFromStored();

        colorPickerView = findViewById(R.id.color_picker_view);
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

    private void storeHost()
    {
        SharedPreferences prefs = this.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("jomc_host", this.tiHost.getText().toString());
        editor.putString("jomc_port", this.tiPort.getText().toString());
        editor.apply();
    }

    private void setHostFromStored()
    {
        SharedPreferences prefs = this.getSharedPreferences(PREFS_NAME, 0);

        String host = prefs.getString("jomc_host", null);
        if (host != null && !host.isEmpty()) {
            tiHost.setText(host);
            tiHost.requestLayout();
        }
        String port = prefs.getString("jomc_port", null);
        if (port != null && !port.isEmpty()) {
            tiPort.setText(port);
            tiPort.requestLayout();
        }
    }

    private void onConnectButtonClick(View v)
    {
        // have connection
        if (orgb.isConnAlive()) {
            this.orgb.closeConn();
            this.orgb.executorAdd(() -> {
                if (!this.orgb.isConnAlive()) {
                    bConnect.setText((CharSequence) "Connect");
                    bConnect.requestLayout();
                }
            });
            return;
        }

        String ip = tiHost.getText().toString();
        int port = Integer.parseInt(tiPort.getText().toString());

        this.orgb.connect(ip, port, () -> {
            Toast.makeText(this, "Connection opened", Toast.LENGTH_SHORT)
              .show();

            if (!this.orgb.isConnAlive()) {
                return;
            }
            // on successful connection

            this.storeHost();

            bConnect.setText((CharSequence) "Disconnect");
            bConnect.requestLayout();

            this.orgb.updateControllerCount();

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
