package com.example.soa_tp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
// bluetooth

    private Intent bluetoothServiceIntent;

    private SensorManager mSensorManager;
    private BroadcastReceiver receiverNOBLUETOOTH;
    private BroadcastReceiver receiverBLUETOOTHDISABLED;
    private BroadcastReceiver receiverBLUETOOTHDISCONNECTED;
    private BroadcastReceiver receiverBLUETOOTHHC_05_ERROR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothServiceIntent = new Intent(this, BluetoothService.class);
        //startService(bluetoothServiceIntent);   // inicia el servicio si no esta iniciado

        Button restartSystemButton = findViewById(R.id.restartSystemButton);
        restartSystemButton.setOnClickListener(v -> {
            bluetoothServiceIntent.putExtra("message", "R");
            startService(bluetoothServiceIntent);   // como ya esta iniciado, solo envia el mensaje y se toma con el metodo "onStartCommand"
        });

        Button openMonitoringButton = findViewById(R.id.openMonitoringButton);
        openMonitoringButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Monitoring.class);
            startActivity(intent);
            startService(bluetoothServiceIntent);

        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

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

    private void initReceivers(){
        receiverNOBLUETOOTH = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverNOBLUETOOTH, new IntentFilter("all_activities.NO_BLUETOOTH"));

        receiverBLUETOOTHDISABLED = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "El dispositivo tiene el Bluetooth desactivado", Toast.LENGTH_LONG).show();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverBLUETOOTHDISABLED, new IntentFilter("all_activities.BLUETOOTH_DISABLED"));

        receiverBLUETOOTHDISCONNECTED = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "Fallo la conexion con el dispositivo", Toast.LENGTH_LONG).show();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverBLUETOOTHDISCONNECTED, new IntentFilter("all_activities.BLUETOOTH_DISCONNECTED"));

        receiverBLUETOOTHHC_05_ERROR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "Fallo la conexion con el dispositivo", Toast.LENGTH_LONG).show();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverBLUETOOTHHC_05_ERROR, new IntentFilter("all_activities.HC_05_ERROR"));


        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),   SensorManager.SENSOR_DELAY_NORMAL);

    }

    private void unregisterReceivers(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverNOBLUETOOTH);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverBLUETOOTHDISABLED);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverBLUETOOTHDISCONNECTED);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverBLUETOOTHHC_05_ERROR);
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }


    // TODO: se podria limitar con algun timer que se pueda activar varias veces:
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if(x > 20 || y > 20 || z > 20){  // osea si hubo algun cambio significativo enviamos el mensaje
                bluetoothServiceIntent.putExtra("message", "R");
                startService(bluetoothServiceIntent);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
