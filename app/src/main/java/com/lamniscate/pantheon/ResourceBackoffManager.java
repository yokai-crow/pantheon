package com.lamniscate.pantheon;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class ResourceBackoffManager {
    private ActivityManager activityManager;
    private static final double CPU_THRESHOLD_PERCENTAGE = 80.0;
    private static long BACKOFF_DELAY_MS = 1000;

    private Context context;
    private Handler handler;

    private boolean isEnabled;

    public ResourceBackoffManager(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler();
        this.isEnabled = false;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public void enable() {
        isEnabled = true;
        monitorResourceUsage();
        // Display alert indicating that resource backoff is enabled
        showToast("Resource backoff is enabled.");
        Log.d("ResourceBackoffManager", "Resource backoff is enabled.");
    }

    public void disable() {
        isEnabled = false;
        handler.removeCallbacksAndMessages(null);
        showToast("Resource backoff is disabled.");
        Log.d("ResourceBackoffManager", "Resource backoff is disabled.");
    }

    private void monitorResourceUsage() {
        if (!isEnabled) return;

        double cpuUsage = getCpuUsagePercentage();

        if (cpuUsage > CPU_THRESHOLD_PERCENTAGE) {
            performResourceBackoff();
        } else {
            handler.postDelayed(this::monitorResourceUsage, BACKOFF_DELAY_MS);
        }
    }

    private double getCpuUsagePercentage() {
        double cpuUsagePercentage = 0.0;
        Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});

        if (memoryInfoArray.length > 0) {
            Debug.MemoryInfo memoryInfo = memoryInfoArray[0];
            long totalCpuTime = Debug.threadCpuTimeNanos();
            long uptime = android.os.SystemClock.uptimeMillis();
            cpuUsagePercentage = (totalCpuTime / (uptime * 10_000.0)) * 100;
        }

        return cpuUsagePercentage;
    }

    private void performResourceBackoff() {
        handler.postDelayed(() -> {
            reduceTaskFrequency();
            monitorResourceUsage();
        }, BACKOFF_DELAY_MS);
    }

    private void reduceTaskFrequency() {
        BACKOFF_DELAY_MS *= 2;
        Log.d("ResourceBackoffManager", "Increased delay time to: " + BACKOFF_DELAY_MS + " milliseconds");
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
