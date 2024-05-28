package com.lamniscate.pantheon;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import android.app.AlertDialog;
import android.widget.Toast;


public class ConnectedBluetoothDevices {
    private static final int PERMISSION_REQUEST_CODE = 123;
    public static List<String> getConnectedDevices(Context context) {
        List<String> connectedDevices = new ArrayList<>();

        // Check if Bluetooth permissions are granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions
            requestBluetoothPermissions(context);
            return connectedDevices; // Return an empty list for now
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                connectedDevices.add(device.getName());
            }
        }

        return connectedDevices;
    }


    public String getDeviceInfo(BluetoothDevice device, Context context) {
        StringBuilder deviceInfo = new StringBuilder();

        // Check if Bluetooth permissions are granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Handle lack of permissions
            Toast.makeText(context, "Bluetooth permissions are required to get device info.", Toast.LENGTH_SHORT).show();
            return deviceInfo.toString(); // Return empty string
        }

        if (device != null) {
            // Device Name
            String name = device.getName();
            if (name != null && !name.isEmpty()) {
                deviceInfo.append("Name: ").append(name).append("\n");
            } else {
                deviceInfo.append("Name: Unknown\n");
            }

            // MAC Address
            String address = device.getAddress();
            if (address != null && !address.isEmpty()) {
                deviceInfo.append("MAC Address: ").append(address).append("\n");
            }

            // Bluetooth Class
            BluetoothClass bluetoothClass = device.getBluetoothClass();
            if (bluetoothClass != null) {
                deviceInfo.append("Class: ").append(bluetoothClass.getDeviceClass()).append("\n");
            }

            // Connection State
            deviceInfo.append("Connected: ").append(device.getBondState() == BluetoothDevice.BOND_BONDED).append("\n");

            // Paired State
            deviceInfo.append("Paired: ").append(device.getBondState() == BluetoothDevice.BOND_BONDED).append("\n");

            // Bluetooth Profiles
            deviceInfo.append("Supported Profiles: ").append(Arrays.toString(device.getUuids())).append("\n");
        } else {
            Toast.makeText(context, "Selected Bluetooth device is null", Toast.LENGTH_SHORT).show();
        }

        return deviceInfo.toString();
    }


    private static void requestBluetoothPermissions(final Context context) {
        // Display dialog to request permissions
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Bluetooth Permissions Required")
                .setMessage("This app requires Bluetooth permissions to discover connected devices.")
                .setPositiveButton("Grant", (dialog, which) -> {
                    // Request permissions
                    ActivityCompat.requestPermissions((Activity) context,
                            new String[]{android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN},
                            PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", null)
                .show();
        Toast.makeText(context, "Bluetooth permissions required", Toast.LENGTH_SHORT).show();
    }

}
