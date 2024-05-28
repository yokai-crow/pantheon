package com.lamniscate.pantheon;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ConnectedWifiNetworks {

    public static final int PERMISSION_REQUEST_CODE = 123;

    public static List<String> getConnectedNetworks(Context context) {
        List<String> connectedNetworks = new ArrayList<>();

        if (hasLocationPermission(context)) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo != null) {
                            String ssid = wifiInfo.getSSID();
                            if (ssid != null && !ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                                connectedNetworks.add(ssid);
                            }
                        }
                    } else {
                        // Permission is not granted, request it
                        requestLocationPermission((Activity) context);
                    }
                } catch (SecurityException e) {
                    // Handle SecurityException
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    // Handle NullPointerException
                    e.printStackTrace();
                }
            }
        }

        return connectedNetworks;
    }

    public String getNetworkInfo(String networkName, Context context) {
        StringBuilder networkInfo = new StringBuilder();

        if (hasLocationPermission(context)) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        for (ScanResult scanResult : scanResults) {
                            if (scanResult.SSID != null && scanResult.SSID.equals(networkName)) {
                                networkInfo.append("SSID: ").append(scanResult.SSID).append("\n");
                                networkInfo.append("BSSID: ").append(scanResult.BSSID).append("\n");
                                networkInfo.append("Frequency: ").append(scanResult.frequency).append(" MHz\n");
                                networkInfo.append("Signal strength: ").append(scanResult.level).append(" dBm\n");
                                networkInfo.append("Capabilities: ").append(scanResult.capabilities).append("\n");
                                networkInfo.append("Timestamp: ").append(scanResult.timestamp).append("\n");
                                networkInfo.append("Security Type: ").append(getSecurityType(scanResult)).append("\n");
                                networkInfo.append("Authentication Type: ").append(getAuthType(scanResult)).append("\n");


                                break; // Exit the loop after finding the specified network
                            }
                        }
                    } else {
                        // Permission is not granted, request it
                        requestLocationPermission((Activity) context);
                    }
                } catch (SecurityException e) {
                    // Handle SecurityException
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    // Handle NullPointerException
                    e.printStackTrace();
                }
            }
        }

        return networkInfo.toString();
    }

    public static String getAuthType(ScanResult scanResult) {
        if (scanResult.capabilities.contains("EAP")) {
            return "EAP";
        } else if (scanResult.capabilities.contains("PSK")) {
            return "PSK";
        } else {
            return "None";
        }
    }

    public static String getSecurityType(ScanResult scanResult) {
        if (scanResult.capabilities.contains("WPA3")) {
            return "WPA3";
        } else if (scanResult.capabilities.contains("WPA2")) {
            return "WPA2";
        } else if (scanResult.capabilities.contains("WPA")) {
            return "WPA";
        } else if (scanResult.capabilities.contains("WEP")) {
            return "WEP";
        } else {
            return "Open";
        }
    }


    public static void requestLocationPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}
