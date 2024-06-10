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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.OptionalDouble;

public class Monitoring extends AppCompatActivity{
    private ArrayList<BarEntry> bars;
    private ArrayList<Integer> dataPerHour;
    private int currentHour;



    BarChart barChart;
    public static int val = 1;

    private BroadcastReceiver receiverSENSORS;
    private BroadcastReceiver receiverNOBLUETOOTH;
    private BroadcastReceiver receiverBLUETOOTHDISABLED;
    private BroadcastReceiver receiverBLUETOOTHDISCONNECTED;


    SharedPreferences listData = getSharedPreferences("dataList",MODE_PRIVATE);;
    SharedPreferences.Editor editor = listData.edit();
   // SharedPreferences sh = getSharedPreferences("dataList", MODE_APPEND);

    // TODO: al volver a la primer actividad y regresar a monitoreo, se crea otra instancia de los receivers ya que se ejecuta onCreate denuevo
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);
        initReceivers();

        Date now = new Date();


        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String today = sdf.format(now);
        TextView textDate = findViewById(R.id.text_date);
        textDate.setText("Análisis del día: " + today);


        dataPerHour = new ArrayList<Integer>();
        currentHour = -1;

        barChart = findViewById(R.id.barChartGraphic);
        bars = new ArrayList<>();
        initBars();

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
        unregisterReceivers();
    }
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceivers();
    }
    @Override
    protected void onResume() {
        super.onResume();
        initReceivers();
    }
    //TODO:
    // posiblemente en onPause haya que guardar la lista de barras y desregistrar el receiver (esto si fuese un intent no se podria hacer)
    // onResume recuperar la lista (talvez usar SQlite) e imprimirla y volver a registrar el receiver
    
    private void initBars(){
        for( int i = 0; i < 24; i++){
            bars.add(new BarEntry((float)i,3));

        }
    }

    private void updateBar(int sensorEast, int sensorWest){

        Calendar calendar = Calendar.getInstance();
        int newHour = calendar.get(Calendar.HOUR_OF_DAY);

        if(currentHour != newHour){
            dataPerHour.clear();
            currentHour = newHour;
        }

        int successPercentage = ((sensorEast + sensorWest) * 100)/2046 ;

        dataPerHour.add(successPercentage);

        OptionalDouble avgOfHour = dataPerHour.stream().mapToInt(a -> a).average();

        bars.get(currentHour).setY((int) Math.round( avgOfHour.isPresent() ? avgOfHour.getAsDouble() : 0));

        barChart.invalidate();
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

    private void uploadData(){
        //TODO: puede ser que reciba la hora y solo suba datos de la hora actual en vez de actualizar toda la lista
        for(Integer i = 0; i< dataPerHour.size(); i++){
            editor.putString(i.toString(), dataPerHour.get(i).toString());
        }
        editor.commit();
    }

    private void downloadData(){
        String data;
        for(Integer i = 0; i< dataPerHour.size(); i++) {
            data = listData.getString(i.toString(), "default");
            dataPerHour.set(i, Integer.parseInt(data));
        }
        //TODO: posiblemente haya que pintar

    }

}

