package com.example.soa_tp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Monitoring extends AppCompatActivity{
    private ArrayList<BarEntry> bars;
    private ArrayList<Float> dataPerHour;
    private int currentHour;



    BarChart barChart;

    private BroadcastReceiver receiverSENSORS;
    private BroadcastReceiver receiverNOBLUETOOTH;
    private BroadcastReceiver receiverBLUETOOTHDISABLED;
    private BroadcastReceiver receiverBLUETOOTHDISCONNECTED;

    Thread tr;

    SharedPreferences listData;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        listData = getSharedPreferences("dataList",MODE_PRIVATE);
        editor = listData.edit();


        initReceivers();

        Date now = new Date();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String today = sdf.format(now);
        TextView textDate = findViewById(R.id.text_date);
        textDate.setText("Análisis del día: " + today);

        dataPerHour = new ArrayList<Float>();
        currentHour = -1;

        barChart = findViewById(R.id.barChartGraphic);
        bars = new ArrayList<>();
        initBars();
        downloadData();


        BarDataSet dataSet = new BarDataSet(bars, "Intensidad de luz por hora");
        dataSet.setColor(Color.BLACK);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);


        XAxis xAxis = barChart.getXAxis();
        xAxis.setTextColor(Color.BLACK);
        xAxis.setGridColor(Color.BLACK);
        xAxis.setTextSize(12f);
        xAxis.setGranularity(1f); // seteo la diferencia entre valores de barras en eje X 1-2-3-...
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String  getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%d", (int) value);
            }
        });
        YAxis leftYAxis = barChart.getAxisLeft();
        leftYAxis.setTextColor(Color.BLACK);
        leftYAxis.setGridColor(Color.BLACK);
        leftYAxis.setTextSize(12f);
        leftYAxis.setAxisMinimum(0f); //
        leftYAxis.setAxisMaximum(100f); //

        YAxis rightYAxis = barChart.getAxisRight();
        rightYAxis.setTextColor(Color.BLACK);
        rightYAxis.setTextSize(10f);

        barChart.invalidate(); // pinta el grafico
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("pausa", "pausando");
        unregisterReceivers();
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.e("stop", "stop");
        finish();
        unregisterReceivers();
    }
    @Override
    protected void onResume() {
        super.onResume();
        downloadData();
        initReceivers();
    }
    
    
    private void initBars(){
        for( int i = 0; i < 24; i++){
            bars.add(new BarEntry((float)i,0));
        }
    }

    private void updateBar(int sensorEast, int sensorWest){
        Calendar calendar = Calendar.getInstance();
        int newHour = calendar.get(Calendar.HOUR_OF_DAY);

        if(currentHour != newHour){
            dataPerHour.clear();
            currentHour = newHour;
        }

        float successPercentage = ((sensorEast + sensorWest) * 100)/2046 ;
        dataPerHour.add(successPercentage);
        bars.get(currentHour).setY(getAvgValue());

        uploadData(currentHour);
        barChart.invalidate();
    }

    private Float getAvgValue(){
        int cant= dataPerHour.size();
        float value = 0;

        for(int i = 0; i < dataPerHour.size(); i++){
            value +=dataPerHour.get(i);
        }
        float avg = value/cant;
        return avg;
    }
    private void initReceivers(){
        receiverSENSORS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int sensorEast = intent.getIntExtra("sensorEast", 0);
                int sensorWest = intent.getIntExtra("sensorWest", 0);
                updateBar(sensorEast, sensorWest);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverSENSORS, new IntentFilter("monitoring.UPDATE_BAR"));

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

    }

    private void unregisterReceivers(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverSENSORS);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverNOBLUETOOTH);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverBLUETOOTHDISABLED);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverBLUETOOTHDISCONNECTED);
    }

    private void uploadData(int hour){
        editor.putString(Integer.toString(hour), "");
        editor.commit();

        editor.putString(Integer.toString(hour), String.valueOf(getAvgValue()));
        editor.commit();
        
    }

    private void downloadData() {


        // fecha de shared preference
        String today_sh = listData.getString("fecha", "default");

        // fecha de hoy
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String today = sdf.format(now);

        // si la fecha no existe en el sh, entonces subo la fecha de hoy
        if(today_sh.equals("default")) {
            editor.putString("fecha", today);
            for(int i = 0; i< 24; i++) {
                bars.set(i, new BarEntry((float)i,0));
                editor.putString(Integer.toString(i), "0");
            }
            editor.commit();
        } else if (!today_sh.equals(today)){    // si la fecha de hoy no es igual a la guardada, limpio las barras
            for(int i = 0; i< 24; i++) {
                bars.set(i, new BarEntry((float)i,0));
            }
        } else {    // si la fecha de hoy, es igual a la guardada, entonces actualizo las barras
            String data;
            for(int i = 0; i< 24; i++) {
                data = listData.getString(Integer.toString(i), "default");
                bars.set(i, new BarEntry((float)i,Float.parseFloat(data)));
            }
        }
/*
        String data;
        for(int i = 0; i< 24; i++) {
            data = listData.getString(Integer.toString(i), "default");
            if (!data.equals("default")){
                bars.set(i, new BarEntry((float)i,Float.parseFloat(data)));
            }

        }*/

        barChart.invalidate();
    }
}

