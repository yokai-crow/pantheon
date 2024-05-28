package com.lamniscate.pantheon;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Timer;
import java.util.TimerTask;

import android.net.TrafficStats;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import java.net.Inet4Address;
import java.net.Inet6Address;

import android.content.Intent;
import android.view.View;





public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private TextView networkStatusTextView;
    private TextView networkTrafficTextView;
    private TextView networkTypeTextView;
    private TextView ipv4TextView;
    private TextView ipv6TextView;

    private Timer timer;

    // Variables for network speed calculation
    private long previousRxBytes = -1;
    private long previousTime = SystemClock.elapsedRealtime();

    public void onDiscoverDevicesClick(View view) {
        // Start WlanDiscoveryActivity to discover devices
        Intent intent = new Intent(this, WlanDiscoveryActivity.class);
        startActivity(intent);
    }

    // Method to handle click on "Intrusion Detection" button
    public void onIntrusionDetectionClick(View view) {
        // Get the detected IP address
        String ipAddress = getIPv4Address();
        // Start the IntrusionDetectionActivity and pass the IP address as an extra
        Intent intent = new Intent(MainActivity.this, EmergencyCallActivity.class);
        intent.putExtra("IP_ADDRESS", ipAddress);
        startActivity(intent);
    }

    public void onPacketFloodDetectionClick(View view) {
        // Start PacketFloodDetector
        Intent intent = new Intent(this, PacketFloodDetector.class);
        startActivity(intent);
    }

    public void onMalwareDetectionClick(View view){
        //start malware detection activity
        Intent intent = new Intent(this, MalwareDetectionActivity.class);
        startActivity(intent);
    }

    public void onMitigationSettingClick(View view) {
        //Mitigation setting activity
        Intent intent = new Intent(this, MitigationSetting.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        networkStatusTextView = findViewById(R.id.networkStatusTextView);
        networkTrafficTextView = findViewById(R.id.networkTrafficTextView);
        networkTypeTextView = findViewById(R.id.networkTypeTextView);
        ipv4TextView = findViewById(R.id.ipv4TextView);
        ipv6TextView = findViewById(R.id.ipv6TextView);

        // Start monitoring network connectivity
        startNetworkMonitoring();
        startUpdatingTrafficDisplay(); // Start updating the network traffic display
    }

    private void startUpdatingTrafficDisplay() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long totalTraffic = getNetworkTraffic();
                updateNetworkTraffic(totalTraffic);
            }
        }, 0, 1000); // Update every second
    }

    private void stopUpdatingTrafficDisplay() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void updateNetworkTraffic(final long totalTraffic) {
        runOnUiThread(() -> {
            String trafficInfo = formatTraffic(totalTraffic);
            networkTrafficTextView.setText(getString(R.string.network_traffic_template, trafficInfo));
        });
    }

    // Method to calculate total network traffic
    private long getNetworkTraffic() {
        long totalRxBytes = TrafficStats.getTotalRxBytes();
        if (previousRxBytes == -1) {
            previousRxBytes = totalRxBytes;
            return 0;
        }

        long currentRxBytes = totalRxBytes;
        long elapsedTime = SystemClock.elapsedRealtime() - previousTime;
        previousTime = SystemClock.elapsedRealtime();

        // Calculate download speed in bytes per second
        //long downloadSpeed = ((currentRxBytes - previousRxBytes) * 1000) / elapsedTime; // bytes per second

        long downloadSpeed = 0;
        if (elapsedTime != 0) {
            downloadSpeed = ((currentRxBytes - previousRxBytes) * 1000) / elapsedTime;
        }

        previousRxBytes = currentRxBytes;

        return downloadSpeed;
    }

    private String formatTraffic(long bytes) {
        if (bytes < 1024) {
            return bytes + " B/s"; // Bytes per second
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB/s", bytes / 1024.0); // Kilobytes per second
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB/s", bytes / (1024.0 * 1024)); // Megabytes per second
        } else {
            return String.format("%.2f GB/s", bytes / (1024.0 * 1024 * 1024)); // Gigabytes per second
        }
    }

    private void startNetworkMonitoring() {
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                updateNetworkStatus(getString(R.string.network_connected), getNetworkType());
                updateIPvAddresses();
            }

            @Override
            public void onLost(@NonNull Network network) {
                updateNetworkStatus(getString(R.string.network_disconnected), "");
                updateIPvAddresses();
            }
        };

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder();
        connectivityManager.registerNetworkCallback(
                requestBuilder.build(),
                networkCallback
        );
    }

    private void updateNetworkStatus(final String status, final String type) {
        runOnUiThread(() -> {
            String networkStatus = getString(R.string.network_status_template, status);
            String networkType = getString(R.string.network_type_label, type);

            // Set network status text
            networkStatusTextView.setText(networkStatus);

            // Set network type text
            networkTypeTextView.setText(networkType);
        });
    }





    private String getNetworkType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return getString(R.string.network_unknown);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return getString(R.string.network_wifi);
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return getCellularNetworkType();
                    }
                }
            }
        } else {
            // Prior to API level 23, you may use other methods to determine network type
            // For example, you can use NetworkInfo class, but it's deprecated from API level 29 onwards
            // Here we're returning unknown for simplicity
            return getString(R.string.network_unknown);
        }
        return getString(R.string.network_unknown);
    }


    private String getCellularNetworkType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
                return getString(R.string.network_unknown);
            }
            int networkType = telephonyManager.getDataNetworkType();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return getString(R.string.network_2g);
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return getString(R.string.network_3g);
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return getString(R.string.network_4g);
                default:
                    return getString(R.string.network_unknown);
            }
        } else {
            // Fallback for devices with lower API levels
            return getString(R.string.network_unknown);
        }
    }

    private void updateIPvAddresses() {
        runOnUiThread(() -> {
            String ipv4 = getIPv4Address();
            String ipv6 = getIPv6Address();
            ipv4TextView.setText(getString(R.string.ipv4_address_label, ipv4));
            ipv6TextView.setText(getString(R.string.ipv6_address_label, ipv6));
        });
    }

    private String getIPv4Address() {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaces) {
                List<InetAddress> inetAddresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress inetAddress : inetAddresses) {
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "N/A";
    }

    private String getIPv6Address() {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaces) {
                List<InetAddress> inetAddresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress inetAddress : inetAddresses) {
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address) {
                        // Check if the address is not a link-local address
                        if (!inetAddress.isLinkLocalAddress()) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "N/A";
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop updating the traffic display when the activity is destroyed
        stopUpdatingTrafficDisplay();
    }
}
