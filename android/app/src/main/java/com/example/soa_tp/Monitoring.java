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
    // Constantes
    private static final float TEXT_SIZE = 11;
    private static final float GRANULARITY = 1;
    private static final float MAX_AXIS_Y = 100;
    private static final float MIN_AXIS_Y = 0;
    private static final int MAX_SENSOR_VALUE = 2046;
    private static final int MAX_PERCENT = 100;
    private static final int MAX_BAR = 24;
    private static final int MIN_BAR = 0;
    private static final int DEFAULT_VALUE_BAR = 0;
    private static final int DEFAULT_HOUR = -1;

    // Interfaz
    private BarChart barChart;
    private ArrayList<BarEntry> bars;
    private ArrayList<Float> dataPerHour;
    private int currentHour;

    // Receivers
    private BroadcastReceiver receiverSENSORS;
    private BroadcastReceiver receiverNOBLUETOOTH;
    private BroadcastReceiver receiverBLUETOOTHDISABLED;
    private BroadcastReceiver receiverBLUETOOTHDISCONNECTED;

    // Persistencia de datos
    private SharedPreferences listData;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("Estado", "Create monitoring");

        setContentView(R.layout.activity_monitoring);

        listData = getSharedPreferences("dataList",MODE_PRIVATE);
        editor = listData.edit();

        initReceivers();
        initComponents();
    }
    @Override
    protected void onStart(){
        super.onStart();
        Log.i("Estado", "Start monitoring");
        barChart.invalidate(); // pinta el grafico
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("Estado", "Pause monitoring");
        unregisterReceivers();
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.i("Estado", "Stop monitoring");
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Estado", "Resume monitoring");
        downloadDataBars();
        barChart.invalidate();
        registerReceivers();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.i("Estado", "Destroy monitoring");
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
        receiverNOBLUETOOTH = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();
            }
        };
        receiverBLUETOOTHDISABLED = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "El dispositivo tiene el Bluetooth desactivado", Toast.LENGTH_LONG).show();
            }
        };
        receiverBLUETOOTHDISCONNECTED = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "Fallo la conexion con el dispositivo", Toast.LENGTH_LONG).show();
            }
        };
    }

    private void initComponents(){
        // Texto del dia
        String today = getCurrentDate();
        TextView textDate = findViewById(R.id.text_date);
        textDate.setText("Análisis del día: " + today);

        // Grafico de barras
        dataPerHour = new ArrayList<Float>();
        currentHour = DEFAULT_HOUR;

        barChart = findViewById(R.id.barChartGraphic);
        bars = new ArrayList<>();
        for( int i = MIN_BAR; i < MAX_BAR; i++){
            bars.add(new BarEntry((float)i,DEFAULT_VALUE_BAR));
        }

        BarDataSet dataSet = new BarDataSet(bars, "Intensidad de luz por hora");
        dataSet.setColor(Color.BLACK);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(TEXT_SIZE);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setTextColor(Color.BLACK);
        xAxis.setGridColor(Color.BLACK);
        xAxis.setTextSize(TEXT_SIZE);
        xAxis.setGranularity(GRANULARITY); // seteo la diferencia entre valores de barras en eje X 1-2-3-...
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String  getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%d", (int) value);
            }
        });
        YAxis leftYAxis = barChart.getAxisLeft();
        leftYAxis.setTextColor(Color.BLACK);
        leftYAxis.setGridColor(Color.BLACK);
        leftYAxis.setTextSize(TEXT_SIZE);
        leftYAxis.setAxisMinimum(MIN_AXIS_Y);
        leftYAxis.setAxisMaximum(MAX_AXIS_Y);

        YAxis rightYAxis = barChart.getAxisRight();
        rightYAxis.setEnabled(false);
    }

    private void updateBar(int sensorEast, int sensorWest){
        Calendar calendar = Calendar.getInstance();
        int newHour = calendar.get(Calendar.HOUR_OF_DAY);

        if(currentHour != newHour){
            dataPerHour.clear();
            currentHour = newHour;
        }

        float successPercentage = ((sensorEast + sensorWest) * MAX_PERCENT)/MAX_SENSOR_VALUE ;
        dataPerHour.add(successPercentage);
        bars.get(currentHour).setY(getAvgValue());

        uploadDataBars(currentHour);
        barChart.invalidate();
    }

    private void registerReceivers(){
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverSENSORS, new IntentFilter("monitoring.UPDATE_BAR"));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverNOBLUETOOTH, new IntentFilter("all_activities.NO_BLUETOOTH"));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverBLUETOOTHDISABLED, new IntentFilter("all_activities.BLUETOOTH_DISABLED"));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverBLUETOOTHDISCONNECTED, new IntentFilter("all_activities.BLUETOOTH_DISCONNECTED"));
    }

    private void unregisterReceivers(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverSENSORS);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverNOBLUETOOTH);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverBLUETOOTHDISABLED);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverBLUETOOTHDISCONNECTED);
    }

    private void uploadDataBars(int hour){
        editor.putString(Integer.toString(hour), "");
        editor.commit();

        editor.putString(Integer.toString(hour), String.valueOf(getAvgValue()));
        editor.commit();
    }

    private void downloadDataBars() {
        // fecha de shared preference
        String today_sh = listData.getString("fecha", "default");

        // fecha de hoy
        String today = getCurrentDate();

        if(today_sh.equals(today)){ // si la fecha de hoy, es igual a la guardada, entonces actualizo las barras
            String value_bar;
            for(int i = MIN_BAR; i < MAX_BAR; i++) {
                value_bar = listData.getString(Integer.toString(i), "default");
                bars.set(i, new BarEntry((float)i,Float.parseFloat(value_bar)));
            }
        } else if (today_sh.equals("default")){ // si la fecha no existe en el sh, entonces subo la fecha de hoy
            for(int i = MIN_BAR; i < MAX_BAR; i++) {
                bars.set(i, new BarEntry((float)i,DEFAULT_VALUE_BAR));
            }
        } else{ // si la fecha de hoy no es igual a la guardada, limpio las barras
            editor.putString("fecha", today);
            for(int i = MIN_BAR; i < MAX_BAR; i++) {
                bars.set(i, new BarEntry((float)i,DEFAULT_VALUE_BAR));
                editor.putString(Integer.toString(i), String.valueOf(DEFAULT_VALUE_BAR));
            }
            editor.commit();
        }
    }

    // Metodos auxiliares
    private Float getAvgValue(){
        int cant= dataPerHour.size();
        float value = 0;

        for(int i = 0; i < dataPerHour.size(); i++){
            value += dataPerHour.get(i);
        }

        return value/cant;
    }
    private String getCurrentDate(){
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(now);
    }
}

