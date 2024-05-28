package com.lamniscate.pantheon;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.lamniscate.pantheon.ConnectedBluetoothDevices;
import com.lamniscate.pantheon.ConnectedWifiNetworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WlanDiscoveryActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int REQUEST_ENABLE_BT = 456;

    private ListView deviceListView;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wlan_discovery);

        deviceListView = findViewById(R.id.bluetoothDevicesListView);

        // Check and request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            displayDevices();
        }

        // Check Bluetooth state and request enable if not enabled
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth enabled, display devices
                displayDevices();
            } else {
                Toast.makeText(this, "Bluetooth is required for device discovery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayDevices() {
        List<String> devices = new ArrayList<>();
        List<String> wifiDevices = ConnectedWifiNetworks.getConnectedNetworks(this);
        List<String> bluetoothDevices = ConnectedBluetoothDevices.getConnectedDevices(this);

        // Add Wi-Fi devices with (w) indicator
        for (String wifiDevice : wifiDevices) {
            devices.add("(w) " + wifiDevice);
        }

        // Add Bluetooth devices with (b) indicator
        for (String bluetoothDevice : bluetoothDevices) {
            devices.add("(b) " + bluetoothDevice);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, devices);
        deviceListView.setAdapter(adapter);

        // Set click listener for list items
        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDevice = devices.get(position);
            if (selectedDevice.startsWith("(b)")) {
                // Bluetooth device selected
                String bluetoothDeviceDetails = getBluetoothDeviceDetails(selectedDevice);
                showDetailsDialog(this, "Bluetooth Device Details", bluetoothDeviceDetails);
            } else if (selectedDevice.startsWith("(w)")) {
                // Wi-Fi network selected
                String wifiNetworkDetails = getWifiNetworkDetails(selectedDevice);
                showDetailsDialog(this, "Wi-Fi Network Details", wifiNetworkDetails);
            }
        });
    }

    private String getBluetoothDeviceDetails(String selectedDevice) {
        // Extract Bluetooth device name from the selectedDevice string
        String deviceName = selectedDevice.substring(4);

        // Check if Bluetooth permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are not granted, handle accordingly (e.g., request permissions)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                    PERMISSION_REQUEST_CODE);
            // Return an empty string for now
            return "";
        }

        // Permissions are granted, continue with getting device info
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName)) {
                    // Found the BluetoothDevice corresponding to the selected device name
                    // Now get its device info
                    ConnectedBluetoothDevices cb = new ConnectedBluetoothDevices();
                    return cb.getDeviceInfo(device, this);
                }
            }
        }

        // If the selected device is not found or Bluetooth is not supported, return an empty string
        return "";
    }


    private String getWifiNetworkDetails(String selectedDevice) {
        // Extract Wi-Fi network name from the selectedDevice string
        String networkName = selectedDevice.substring(4);
        // Get the network info using the Wi-Fi network name
        ConnectedWifiNetworks cw = new ConnectedWifiNetworks();
        String networkInfo = cw.getNetworkInfo(networkName, this);
        return networkInfo;
    }

    private void showDetailsDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, display devices
                displayDevices();
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(this, "Location permission required for device discovery", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
