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

    private BroadcastReceiver receiverNOBLUETOOTH;
    private BroadcastReceiver receiverBLUETOOTHDISABLED;
    private BroadcastReceiver receiverBLUETOOTHDISCONNECTED;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothServiceIntent = new Intent(this, BluetoothService.class);
        //startService(bluetoothServiceIntent);   // inicia el servicio si no esta iniciado

        Button restartSystemButton = findViewById(R.id.restartSystemButton);
        restartSystemButton.setOnClickListener(v -> {
            bluetoothServiceIntent.putExtra("message", "restart");
            startService(bluetoothServiceIntent);   // como ya esta iniciado, solo envia el mensaje y se toma con el metodo "onStartCommand"
        });

        Button openMonitoringButton = findViewById(R.id.openMonitoringButton);
        openMonitoringButton.setOnClickListener(v -> {
            startService(bluetoothServiceIntent);
            Intent intent = new Intent(getApplicationContext(), Monitoring.class);
            startActivity(intent);
            finish();
        });
    }

    private void initReceivers(){
        //TODO:  talvez esto tambien va en el monitoring
        receiverNOBLUETOOTH = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiverNOBLUETOOTH, new IntentFilter("main_activity.NO_BLUETOOTH"));

        receiverBLUETOOTHDISABLED = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "El dispositivo tiene el Bluetooth desactivado", Toast.LENGTH_LONG).show();
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiverBLUETOOTHDISABLED, new IntentFilter("main_activity.BLUETOOTH_DISABLED"));


        receiverBLUETOOTHDISCONNECTED = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "Fallo la conexion con el dispositivo", Toast.LENGTH_LONG).show();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverBLUETOOTHDISCONNECTED, new IntentFilter("main_activity.BLUETOOTH_DISCONNECTED"));
    }

    private void unregisterReceivers(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverNOBLUETOOTH);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverBLUETOOTHDISABLED);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverBLUETOOTHDISCONNECTED);
    }




    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceivers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initReceivers();
    }
}
