package com.lamniscate.pantheon;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

public class AirplaneModeManager {
    private Context context;

    public AirplaneModeManager(Context context) {
        this.context = context;
    }

    public void enableAirplaneMode() {
        Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        showToast("Redirected to Mode Manager for Enable");
    }

    public void disableAirplaneMode() {
        Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        intent.putExtra("state", false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        showToast("Redirected to Mode Manager for Disable");
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}

