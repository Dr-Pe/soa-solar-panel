package com.example.soa_tp;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class SensorBT extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Intent bluetoothServiceIntent;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothServiceIntent = new Intent(this, BluetoothService.class);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if(x > 10 || y > 10 || z > 10){  // osea si hubo algun cambio significativo enviamos el mensaje
                bluetoothServiceIntent.putExtra("message", "R");
                startService(bluetoothServiceIntent);
                Log.e("test", "entro sensor");
            }
        }
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {

    }
}
