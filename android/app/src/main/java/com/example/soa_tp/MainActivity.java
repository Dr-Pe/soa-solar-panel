package com.example.soa_tp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class MainActivity extends AppCompatActivity {
// bluetooth

    private Intent bluetoothServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO:  talvez esto tambien va en el monitoring
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();

                finish();
            }
        }, new IntentFilter("main_activity.NO_BLUETOOTH"));


        bluetoothServiceIntent = new Intent(this, BluetoothService.class);
        startService(bluetoothServiceIntent);   // inicia el servicio si no esta iniciado

        Button restartSystemButton = findViewById(R.id.restartSystemButton);
        restartSystemButton.setOnClickListener(v -> {
            bluetoothServiceIntent.putExtra("message", "restart");
            startService(bluetoothServiceIntent);   // como ya esta iniciado, solo envia el mensaje y se toma con el metodo "onStartCommand"
        });

        Button openMonitoringButton = findViewById(R.id.openMonitoringButton);
        openMonitoringButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Monitoring.class);
            startActivity(intent);
        });



    }


}
