package com.lamniscate.pantheon;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class EmergencyCallActivity extends AppCompatActivity {

    private EditText emergencyContactEditText;
    private Button saveButton;
    private Button emergencyCallButton; // New button for emergency call
    private SharedPreferences sharedPreferences;
    private static final String EMERGENCY_CONTACT_KEY = "emergency_contact";
    private String emergencyNumber = "*400#"; // Default emergency number
    private static final int CALL_PHONE_REQUEST_CODE = 101; // Request code for CALL_PHONE permission

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_call);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        // Find EditText, Button, and emergency call Button views
        emergencyContactEditText = findViewById(R.id.emergencyContactEditText);
        saveButton = findViewById(R.id.saveButton);
        emergencyCallButton = findViewById(R.id.emergencyCallButton); // Initialize the new button

        // Load the saved emergency contact number
        emergencyNumber = sharedPreferences.getString(EMERGENCY_CONTACT_KEY, emergencyNumber);
        emergencyContactEditText.setText(emergencyNumber);

        // Set OnClickListener for the saveButton
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the entered emergency contact number
                String enteredNumber = emergencyContactEditText.getText().toString().trim();

                // Validate the entered number
                if (!enteredNumber.isEmpty()) {
                    emergencyNumber = enteredNumber;
                    // Save the emergency contact number
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(EMERGENCY_CONTACT_KEY, emergencyNumber);
                    editor.apply();
                    Toast.makeText(EmergencyCallActivity.this, "Emergency contact saved", Toast.LENGTH_SHORT).show();
                } else {
                    // Inform the user to enter a valid number
                    Toast.makeText(EmergencyCallActivity.this, "Please enter a valid emergency contact number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set OnClickListener for the emergencyCallButton
        emergencyCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if CALL_PHONE permission is granted
                if (ContextCompat.checkSelfPermission(EmergencyCallActivity.this, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request CALL_PHONE permission
                    ActivityCompat.requestPermissions(EmergencyCallActivity.this,
                            new String[]{Manifest.permission.CALL_PHONE}, CALL_PHONE_REQUEST_CODE);
                } else {
                    // Permission is already granted, make the emergency call
                    makeEmergencyCall();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PHONE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, make the emergency call
                makeEmergencyCall();
            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "Permission denied. Emergency call cannot be made.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void makeEmergencyCall() {
        // Create an intent to initiate a phone call
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + emergencyNumber));

        // Check if the device can make phone calls before initiating the call
        if (callIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(callIntent);
        } else {
            // Device doesn't support phone calls or no app to handle the intent
            Toast.makeText(this, "Emergency call not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }
}
