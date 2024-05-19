package com.example.soa_tp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.OptionalDouble;

public class Monitoring extends AppCompatActivity{
    private ArrayList<BarEntry> bars;
    private ArrayList<Integer> dataPerHour;
    private int currentHour;

    BarChart barChart;
    public static int val = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitoreo_actividad);

        dataPerHour = new ArrayList<Integer>();
        currentHour = -1;

        barChart = findViewById(R.id.barChartGraphic);
        bars = new ArrayList<>();
        initBars();

        BarDataSet dataSet = new BarDataSet(bars, "valor promedio x hora");
        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);


        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f); // seteo la diferencia entre valores de barras en eje X 1-2-3-...
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String  getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%d", (int) value);
            }
        });
        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setAxisMinimum(0f); //
        yAxis.setAxisMaximum(100f); //

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int sensorEast = intent.getIntExtra("sensorEast", 0);
                int sensorWest = intent.getIntExtra("sensorWest", 0);
                updateBar(sensorEast, sensorWest);
            }
        }, new IntentFilter("monitoring.UPDATE_BAR"));



        barChart.invalidate(); // pinta el grafico


    }
    //TODO:
    // posiblemente en onPause haya que guardar la lista y desregistrar el receiver (esto si fuese un intent no se podria hacer)
    // onResume recuperar la lista e imprimirla y volver a registrar el receiver


    private void initBars(){
        for( int i = 0; i < 24; i++){
            bars.add(new BarEntry((float)i,3));
        }
/*
        barras.add(new BarEntry(0f, 1));
        barras.add(new BarEntry(1f, 3));
        barras.add(new BarEntry(2f, 10));
        barras.add(new BarEntry(3f, 1));
        barras.add(new BarEntry(4f, 2));
        barras.add(new BarEntry(5f, 77));
        barras.add(new BarEntry(6f, 1));
        barras.add(new BarEntry(7f, 2));
        barras.add(new BarEntry(8f, 3));
        barras.add(new BarEntry(9f, 1));
        barras.add(new BarEntry(10f, 2));
        barras.add(new BarEntry(11f, 3));
        barras.add(new BarEntry(12f, 45));
        barras.add(new BarEntry(13f, 2));
        barras.add(new BarEntry(14f, 3));
        barras.add(new BarEntry(15f, 44));
        barras.add(new BarEntry(16f, 45));
        barras.add(new BarEntry(17f, 100));
        barras.add(new BarEntry(18f, 25));
        barras.add(new BarEntry(19f, 25));
        barras.add(new BarEntry(20f, 25));
        barras.add(new BarEntry(21f, 25));
        barras.add(new BarEntry(22f, 50));
        barras.add(new BarEntry(23f, 100));*/
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


}

