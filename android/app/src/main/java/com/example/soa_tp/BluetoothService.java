package com.example.soa_tp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothService extends Service {
    //  modulo hc-05 permite comunicarse mediante bytes en serie: servicio de puerto serie, el UUID de este servicio es:
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice sunflowerBT;
    private InputStream inStream;
    private OutputStream outStream;
    private Thread monitoingThread;

    private boolean connected = false;



    //TODO: posiblemente hay que verificar PAIR
    // hay que agregar un mensaje TOAST a todas las excepciones

    @SuppressLint("MissingPermission")  // a partir de cierta version en android pide permisos en tiempo de ejecucion, al pedo
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("error", "entro oncreate");
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        // no existe placa bluetooth
        if (btAdapter == null) {
            sendMSGtoActivities("all_activities.NO_BLUETOOTH");
            Log.e("error", "no bluetooth");
            return;
        }
        // no esta habilitado el bluetooth
        if (!btAdapter.isEnabled()) {
            sendMSGtoActivities("all_activities.BLUETOOTH_DISABLED");
            return;
        }

        // busco el hc 05
        Set<BluetoothDevice> devicesBT = btAdapter.getBondedDevices();
        for (BluetoothDevice device : devicesBT){
            if(device.getName().equals("HC-05"))
            {
                sunflowerBT = device;
            }
        }

        if(sunflowerBT == null){
            sendMSGtoActivities("all_activities.HC_05_ERROR");
            return;
        }

        try {

            btSocket = sunflowerBT.createRfcommSocketToServiceRecord(MY_UUID);    // asocia la conexion al servicio de comunicacion del hc-05
            btSocket.connect();
            connected = true;
            inStream = btSocket.getInputStream();
            outStream = btSocket.getOutputStream();

            monitorLightSensors();
        } catch (IOException e) {
            sendMSGtoActivities("all_activities.BLUETOOTH_DISCONNECTED");
        }
    }

    private void sendMSGtoActivities(String msg){
        Intent intent = new Intent();
        intent.setAction(msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        stopSelf();
    }


    // hilo que lee los valores que recibe del SE y envia los datos a la actividad monitoreo
    private void monitorLightSensors() {
        monitoingThread = new Thread(this::readLightSensors);
        monitoingThread.start();
    }
    private void readLightSensors(){
        byte[] buffer;
        buffer = new byte[1024];
        int numBytes;

        while (true) {
            try {
                if(inStream.available() > 0){
                    numBytes = inStream.read(buffer);
                    String datos = new String(buffer, 0, numBytes);
                    String[] parseo = datos.split(" ");             // el sensor envia este formato: 10-20
                    int sensorEast = Integer.parseInt(parseo[0]);
                    int sensorWest = Integer.parseInt(parseo[1]);

                    sendDataToMonitoring(sensorEast, sensorWest);
                }
            } catch (IOException e) {
                sendMSGtoActivities("all_activities.BLUETOOTH_DISCONNECTED");
            }
        }
    }


    // envia los datos de los sensores a la actividad monitoreo
    // y esa actividad lo lee con "BroadcastReceiver"

    private void sendDataToMonitoring(int sensorEast, int sensorWest) {
        Intent intent = new Intent();
        intent.setAction("monitoring.UPDATE_BAR");
        intent.putExtra("sensorEast", sensorEast);
        intent.putExtra("sensorWest", sensorWest);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    //  siempre al iniciar un servicio, se ejecuta onCreate y despues onStartCommand SIEMPRE
    //  en la primer ejecucion no existe nada en message y tira error al metodo sendMsgToSunflower...
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String message = intent.getStringExtra("message");
        if (connected == true && message != null ){
            sendMsgToSunflower(message);
        }
        return START_STICKY;
    }

    public void sendMsgToSunflower(String mensaje) {
        byte[] msgBuffer = mensaje.getBytes();
        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            sendMSGtoActivities("all_activities.BLUETOOTH_DISCONNECTED");
        }
    }

    //  se usa si una actividad usa bindService para vincularse al servicio
    //  es otro metodo de comunicar sin usar broadcast, seria mas recomendado pero es mas codigo que no lei
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
