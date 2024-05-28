package com.lamniscate.pantheon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat; // Import SwitchCompat

public class MitigationSetting extends AppCompatActivity {

    private static final String PREFS_NAME = "MitigationPrefs";
    private static final String ISOLATION_ENABLED_KEY = "isolationEnabled";
    private static final String RESOURCE_BACKOFF_ENABLED_KEY = "resourceBackoffEnabled";

    private SharedPreferences preferences;
    private SwitchCompat isolationSwitch; // Change Switch to SwitchCompat
    private SwitchCompat resourceBackoffSwitch; // Change Switch to SwitchCompat

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mitigation_setting);

        // Initialize SharedPreferences
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize switches
        isolationSwitch = findViewById(R.id.isolationSwitch);
        resourceBackoffSwitch = findViewById(R.id.resourceBackoffSwitch);

        // Restore switch states
        isolationSwitch.setChecked(preferences.getBoolean(ISOLATION_ENABLED_KEY, false));
        resourceBackoffSwitch.setChecked(preferences.getBoolean(RESOURCE_BACKOFF_ENABLED_KEY, false));

        // Set switch listeners
        isolationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save switch state
            preferences.edit().putBoolean(ISOLATION_ENABLED_KEY, isChecked).apply();
            if (isChecked) {
                // Enable isolation
                enableIsolation();
            } else {
                // Disable isolation
                disableIsolation();
            }
        });

        resourceBackoffSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save switch state
            preferences.edit().putBoolean(RESOURCE_BACKOFF_ENABLED_KEY, isChecked).apply();
            if (isChecked) {
                // Enable resource backoff
                enableResourceBackoff();
            } else {
                // Disable resource backoff
                disableResourceBackoff();
            }
        });
    }

    private void enableIsolation() {
        AirplaneModeManager am = new AirplaneModeManager(this);
        am.enableAirplaneMode();
        Toast.makeText(this, "Isolation enabled", Toast.LENGTH_SHORT).show();
    }

    private void disableIsolation() {
        AirplaneModeManager am = new AirplaneModeManager(this);
        am.disableAirplaneMode();
        Toast.makeText(this, "Isolation disabled", Toast.LENGTH_SHORT).show();
    }

    private void enableResourceBackoff() {
        ResourceBackoffManager rm = new ResourceBackoffManager(this);
        rm.enable();
        Toast.makeText(this, "Resource backoff enabled", Toast.LENGTH_SHORT).show();
    }

    private void disableResourceBackoff() {
        ResourceBackoffManager rm = new ResourceBackoffManager(this);
        rm.disable();
        Toast.makeText(this, "Resource backoff disabled", Toast.LENGTH_SHORT).show();
    }
}
