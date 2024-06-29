package com.example.soa_tp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class MainActivity extends AppCompatActivity implements SensorEventListener {


    // Constantes
    private static final int MIN_AXIS_X = 20;
    private static final int MIN_AXIS_Y = 20;
    private static final int MIN_AXIS_Z = 20;
    // Bluetooth
    private Intent bluetoothServiceIntent;
    private Thread btThread;

    // Sensor
    private SensorManager mSensorManager;

    // Receivers
    private BroadcastReceiver receiver_no_bluetooth;
    private BroadcastReceiver receiver_bluetooth_disabled;
    private BroadcastReceiver receiver_bluetooth_disconnected;
    private BroadcastReceiver receiver_bluetooth_HC_05_error;
    private BroadcastReceiver receiver_bluetooth_msg_ok;

    // Interfaz
    private Button restartSystemButton;
    private Button openMonitoringButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothServiceIntent = new Intent(this, BluetoothService.class);

        restartSystemButton = findViewById(R.id.restartSystemButton);
        restartSystemButton.setOnClickListener(v -> {
            disableButtons();
            bluetoothServiceIntent.putExtra("message", "R");
            btThread = new Thread(this::initBt);
            btThread.start();
        });

        openMonitoringButton = findViewById(R.id.openMonitoringButton);
        openMonitoringButton.setOnClickListener(v -> {
            disableButtons();
            Intent intent = new Intent(getApplicationContext(), Monitoring.class);
            startActivity(intent);
            btThread = new Thread(this::initBt);
            btThread.start();
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    private void initBt(){
        startService(bluetoothServiceIntent);   // como ya esta iniciado, solo envia el mensaje y se toma con el metodo "onStartCommand"
        bluetoothServiceIntent.removeExtra("message");
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceivers();
    }

    @Override
    protected void onStop(){
        super.onStop();
        unregisterReceivers();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initReceivers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initReceivers();
        enableButtons();

    }

    private void disableButtons(){
        restartSystemButton.setEnabled(false);
        openMonitoringButton.setEnabled(false);

        int colorGrey = ContextCompat.getColor(this, R.color.grey);
        restartSystemButton.setBackgroundTintList(ColorStateList.valueOf(colorGrey));
        openMonitoringButton.setBackgroundTintList(ColorStateList.valueOf(colorGrey));
    }

    private void enableButtons(){
        restartSystemButton.setEnabled(true);
        openMonitoringButton.setEnabled(true);

        int colorYellow = ContextCompat.getColor(this, R.color.yellow);
        restartSystemButton.setBackgroundTintList(ColorStateList.valueOf(colorYellow));
        openMonitoringButton.setBackgroundTintList(ColorStateList.valueOf(colorYellow));
    }


    private void initReceivers(){
        receiver_no_bluetooth = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();
                enableButtons();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver_no_bluetooth, new IntentFilter("all_activities.NO_BLUETOOTH"));

        receiver_bluetooth_disabled = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "El dispositivo tiene el Bluetooth desactivado", Toast.LENGTH_LONG).show();
                enableButtons();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver_bluetooth_disabled, new IntentFilter("all_activities.BLUETOOTH_DISABLED"));

        receiver_bluetooth_disconnected = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "Fallo la conexion con el dispositivo", Toast.LENGTH_LONG).show();
                enableButtons();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver_bluetooth_disconnected, new IntentFilter("all_activities.BLUETOOTH_DISCONNECTED"));

        receiver_bluetooth_HC_05_error = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "Fallo la conexion con el dispositivo", Toast.LENGTH_LONG).show();
                enableButtons();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver_bluetooth_HC_05_error, new IntentFilter("all_activities.HC_05_ERROR"));

        receiver_bluetooth_msg_ok = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "Mensaje enviado con exito", Toast.LENGTH_LONG).show();
                enableButtons();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver_bluetooth_msg_ok, new IntentFilter("MainActivity.HC_05_OK"));

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterReceivers(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver_no_bluetooth);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver_bluetooth_disabled);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver_bluetooth_disconnected);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver_bluetooth_HC_05_error);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver_bluetooth_msg_ok);
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if(x > MIN_AXIS_X || y > MIN_AXIS_Y || z > MIN_AXIS_Z){
                bluetoothServiceIntent.putExtra("message", "R");
                btThread = new Thread(this::initBt);
                btThread.start();

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
