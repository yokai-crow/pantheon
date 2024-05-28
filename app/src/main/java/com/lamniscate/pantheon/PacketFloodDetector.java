package com.lamniscate.pantheon;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import android.net.TrafficStats;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.CompoundButton;
import android.widget.Toast;


public class PacketFloodDetector extends AppCompatActivity {
    private PieChart trafficPieChart;
    private TextView alertTextView;
    private SwitchCompat toggleSmoothing;
    private boolean isSmoothingEnabled = true; // Assuming smoothing is initially enabled



    //private static final long SAMPLE_INTERVAL_MS = 1000; // 1 second
    //private static final int SAMPLE_WINDOW_SIZE = 10; // Number of samples to consider for moving average
  //  private static final double ANOMALY_THRESHOLD = 2.5; // Threshold for detecting anomalies (z-score)

    //for descreasing false positive
    private static final int SAMPLE_WINDOW_SIZE = 10; // Number of samples to consider for moving average
    private static final double ANOMALY_THRESHOLD = 2.0; // Initial threshold for detecting anomalies (z-score)
    private static final double THRESHOLD_MULTIPLIER = 1.5; // Multiplier for adjusting threshold dynamically
    private static final double MINIMUM_THRESHOLD = 2.0; // Minimum threshold to prevent excessive sensitivity

    //incorporating smoothing techniques and threshold hysteresis
    private Queue<Double> smoothedTrafficData = new ArrayDeque<>();

    private double anomalyThreshold = ANOMALY_THRESHOLD;

    //for decreasing false positive

    private long prevRxPackets;
    private Queue<Long> rxPacketSamples = new ArrayDeque<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packet_flood_detector);
        trafficPieChart = findViewById(R.id.trafficPieChart);

        // Initialize pie chart
        setupPieChart();

        // Start updating pie chart with real-time data
        startUpdatingChart();

        alertTextView = findViewById(R.id.alertTextView);
        System.out.println(TrafficStats.getTotalTxPackets());

        toggleSmoothing = findViewById(R.id.toggleSmoothing); // Obtain reference to the SwitchCompat
        // Set up the initial state of smoothing (assuming it's initially enabled)
        // Retrieve the saved state of the toggle switch from SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        isSmoothingEnabled = preferences.getBoolean("smoothing_enabled", true); // Default value: true

        // Update the state of the toggle switch
        toggleSmoothing.setChecked(isSmoothingEnabled);

        // Set up a listener for the toggle switch to save its state when changed by the user
        toggleSmoothing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Update the state of smoothing
                isSmoothingEnabled = isChecked;

                // Save the state of the toggle switch to SharedPreferences
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("smoothing_enabled", isChecked);
                editor.apply();
            }
        });
    }

    private void setupPieChart() {


        trafficPieChart.getDescription().setEnabled(false);
        trafficPieChart.setUsePercentValues(true);

        trafficPieChart.setEntryLabelTextSize(12f); // Set text size


        trafficPieChart.setDrawHoleEnabled(true);
        trafficPieChart.setHoleColor(android.R.color.transparent);
        trafficPieChart.setTransparentCircleRadius(50f); // Set hole size


        trafficPieChart.setDrawCenterText(true);
        trafficPieChart.setCenterText("Network Traffic");
        trafficPieChart.setRotationAngle(0);
        trafficPieChart.setRotationEnabled(true);
        trafficPieChart.setHighlightPerTapEnabled(true);

        // Apply animation
        trafficPieChart.animateY(1400, Easing.EaseInOutQuad);

        Legend legend = trafficPieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setEnabled(true);
    }

    private void startUpdatingChart() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateTrafficData();
                handler.postDelayed(this, 1000); // Update every second
            }
        }, 1000); // Initial delay
    }

    private String getCurrentTimeAsString() {
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(currentTimeMillis));
    }

    private void updateTrafficData() {
        long incomingTraffic = TrafficStats.getTotalRxBytes();
        long outgoingTraffic = TrafficStats.getTotalTxBytes();

        // Calculate percentage increase
        double percentIncrease = calculatePercentageIncrease(incomingTraffic, outgoingTraffic);
        String detectionTime = getCurrentTimeAsString();


        if (incomingTraffic == 0 && outgoingTraffic == 0) {
            // No data available
            trafficPieChart.setVisibility(View.GONE);
            alertTextView.setVisibility(View.VISIBLE);
            alertTextView.setText(getString(R.string.no_data_available));
            alertTextView.setTextColor(ContextCompat.getColor(this, R.color.orange_dark));
        } else {
            // Data available, update PieChart
            trafficPieChart.setVisibility(View.VISIBLE);
            alertTextView.setVisibility(View.GONE);

            List<PieEntry> entries = new ArrayList<>();
            entries.add(new PieEntry(incomingTraffic, "Incoming Traffic"));
            entries.add(new PieEntry(outgoingTraffic, "Outgoing Traffic"));

            int purpleColor = ContextCompat.getColor(this, R.color.pie_blue);
            int greenColor = ContextCompat.getColor(this, R.color.pie_green);
            PieDataSet dataSet = new PieDataSet(entries, "Network Traffic");
            dataSet.setColors(purpleColor, greenColor); // Colors for each entry

            PieData pieData = new PieData(dataSet);
            pieData.setValueFormatter(new PercentFormatter(trafficPieChart)); // Display percentages
            trafficPieChart.setData(pieData);
            trafficPieChart.invalidate(); // Refresh chart

            // Check for packet flood
            if(isPacketFloodDetected()){

                // Decrease false positives by dynamically adjusting the threshold
                anomalyThreshold = Math.max(anomalyThreshold * THRESHOLD_MULTIPLIER, MINIMUM_THRESHOLD);

                // Display alert message to the user
                showAlert( percentIncrease, detectionTime);
                alertTextView.setVisibility(View.VISIBLE); // Set alertTextView visible
                alertTextView.setText(getString(R.string.packet_flood_detected));
                alertTextView.setTextColor(ContextCompat.getColor(this, R.color.red_dark));
            }else{
                alertTextView.setVisibility(View.VISIBLE); // Set alertTextView visible
                alertTextView.setText(getString(R.string.network_traffic_healthy));
                alertTextView.setTextColor(ContextCompat.getColor(this, R.color.green_dark));
            }
        }
    }


    //testing
    private double calculatePercentageIncrease(long incomingTraffic, long outgoingTraffic) {
        long previousTraffic = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
        double increase = ((incomingTraffic + outgoingTraffic) - previousTraffic) * 100.0 / previousTraffic;
        return increase;
    }

    public boolean isPacketFloodDetected() {
        long currRxPackets = TrafficStats.getTotalRxPackets();

        // Calculate difference from previous sample
        long rxDiff = currRxPackets - prevRxPackets;

        // Update previous sample
        prevRxPackets = currRxPackets;

        // Add current Rx packet count to the sample queue
        rxPacketSamples.offer(rxDiff);

        // If the sample queue exceeds the window size, remove the oldest sample
        if (rxPacketSamples.size() > SAMPLE_WINDOW_SIZE) {
            rxPacketSamples.poll();
        }

        // Calculate moving average and standard deviation of received packets
        double averageRxRate = getAverage(rxPacketSamples);
        double stdDevRxRate = getStandardDeviation(rxPacketSamples, averageRxRate);

        // Calculate z-score of current sample
        double zScore = (rxDiff - averageRxRate) / stdDevRxRate;

        // Apply smoothing to z-score calculation
        double smoothedZScore = applySmoothing(zScore);

        //for debug
        System.out.println(zScore);
        System.out.print(smoothedZScore);

        // Check if z-score exceeds threshold, indicating an anomaly (simplified if else)
        //important// return smoothedZScore  > ANOMALY_THRESHOLD;

        //enable disable smoothing
        if (isSmoothingEnabled) {
            return smoothedZScore > ANOMALY_THRESHOLD;
        } else {
            // If smoothing is disabled, use the original value without smoothing
            return zScore > ANOMALY_THRESHOLD;
        }
    }

    private double applySmoothing(double value) {
        // Apply simple exponential smoothing to the data
        double alpha = 0.2; // Smoothing factor (0 < alpha < 1)
        double smoothedValue = value;
        if (!smoothedTrafficData.isEmpty()) {
            double previousSmoothedValue = smoothedTrafficData.peek();
            smoothedValue = alpha * value + (1 - alpha) * previousSmoothedValue;
        }
        smoothedTrafficData.offer(smoothedValue);
        return smoothedValue;
    }

    private void showAlert(double percentIncrease, String detectionTime) {
        String message = "A packet flood has been detected.\n";
        //message += String.format("There was a sudden increase in traffic by %.2f%% at %s.", percentIncrease, detectionTime);
        message += String.format("There was a sudden increase in traffic by some percentage at %s.", detectionTime);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Packet Flood Detected")
                .setMessage(message)
                .setPositiveButton("Enable Isolation", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked on Enable Isolation
                        AirplaneModeManager am = new AirplaneModeManager(PacketFloodDetector.this);
                        am.enableAirplaneMode();
                        dialog.dismiss();
                    }
                })
                .setNeutralButton("Enable Resource Backoff", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked on Enable Resource Backoff
                        enableResourceBackoff();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked on Dismiss
                        dialog.dismiss();

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void enableResourceBackoff() {
        // Enable resource backoff
        ResourceBackoffManager resourceBackoffManager = new ResourceBackoffManager(this);
        resourceBackoffManager.enable();
        // Show a toast
        //Toast.makeText(this, "Resource backoff enabled", Toast.LENGTH_SHORT).show();
    }



    private double getAverage(Queue<Long> samples) {
        long total = 0;
        for (long sample : samples) {
            total += sample;
        }
        return (double) total / samples.size();
    }

    private double getStandardDeviation(Queue<Long> samples, double mean) {
        double sumOfSquaredDiffs = 0;
        for (long sample : samples) {
            sumOfSquaredDiffs += Math.pow(sample - mean, 2);
        }
        return Math.sqrt(sumOfSquaredDiffs / samples.size());
    }



}