package com.example.soa_tp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothService extends Service {

    // Constantes

    //  modulo hc-05 permite comunicarse mediante bytes en serie: servicio de puerto serie, el UUID de este servicio es:
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int TAM_BUFF = 1024;
    private static final String REGEX_FILTER = "\\([0-9]+,[0-9]+\\)\\n";
    private static final String ID_BLUETOOTH = "HC-05 ";
    // Bluetooth
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice sunflowerBT;
    private InputStream inStream;
    private OutputStream outStream;
    private boolean connected = false;
    private Thread monitoringThread;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Estado", "Create bluetoothService");

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        // no existe placa bluetooth
        if (btAdapter == null) {
            sendMSGtoActivities("all_activities.NO_BLUETOOTH");
            Log.e("error", "no bluetooth");
            stopSelf();
            return;
        }
        // no esta habilitado el bluetooth
        if (!btAdapter.isEnabled()) {
            sendMSGtoActivities("all_activities.BLUETOOTH_DISABLED");
            Log.e("error", "no habilitado");
            stopSelf();
            return;
        }
        // busco el hc 05
        Set<BluetoothDevice> devicesBT = btAdapter.getBondedDevices();
        for (BluetoothDevice device : devicesBT){
            if(device.getName().equals(ID_BLUETOOTH)) {
                sunflowerBT = device;
            }
        }

        if(sunflowerBT == null){
            sendMSGtoActivities("all_activities.HC_05_ERROR");
            Log.e("error", "hc-05 no encontrado");
            stopSelf();
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
            Log.e("error", "bt desconectado");
            stopSelf();
        }
    }
    @Override
    public  void onDestroy(){
        super.onDestroy();
        Log.i("Estado", "Destroy bluetoothService");
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String message = intent.getStringExtra("message");
        Log.i("onStartCommand", "recibiendo mensaje en bluetoothService");

        if (connected && message != null ){
            sendMsgToSunflower(message);
            sendMSGtoActivities("MainActivity.HC_05_OK");
            Log.i("onStartCommand", "mensaje enviado con exito");
        }

        return START_STICKY;
    }

    private void sendMSGtoActivities(String msg){
        Intent intent = new Intent();
        intent.setAction(msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void monitorLightSensors() {
        monitoringThread = new Thread(this::readLightSensors);
        monitoringThread.start();
    }
    private void readLightSensors(){
        byte[] buffer;
        buffer = new byte[TAM_BUFF];
        int numBytes;

        while (true) {
            try {
                if(inStream.available() > 0){
                    numBytes = inStream.read(buffer);
                    String data = new String(buffer, 0, numBytes);

                    if(data.matches(REGEX_FILTER)){
                        data = data.replace("(", "").replace(")", "").replace("\n", "");
                        String[] parseo = data.split(",");             // el sensor envia este formato: 10-20
                        int sensorEast = Integer.parseInt(parseo[0]);
                        int sensorWest = Integer.parseInt(parseo[1]);

                        sendDataToMonitoring(sensorEast, sensorWest);
                    }
                }
            } catch (IOException e) {
                sendMSGtoActivities("all_activities.BLUETOOTH_DISCONNECTED");
                Log.e("error", "bluetooth en curso desconectado");
                stopSelf();
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


    public void sendMsgToSunflower(String message) {
        byte[] msgBuffer = message.getBytes();
        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            sendMSGtoActivities("all_activities.BLUETOOTH_DISCONNECTED");
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
