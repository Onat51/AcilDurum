package com.example.emergencyapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class LocationService extends Service {
    private static final String TAG = "LocationService";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PreferenceManager prefManager;

    private ValueEventListener remoteCommandListener;
    private long lastHistorySave = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        prefManager = new PreferenceManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();
        startForeground(Constants.LOCATION_NOTIFICATION_ID, createNotification());
        startLocationUpdates();
        listenForRemoteCommands();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "location_channel", "Konum Servisi", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "location_channel")
                .setContentTitle("Acil Durum Uygulaması")
                .setContentText("Konum paylaşımı aktif")
                .setSmallIcon(R.drawable.ic_location)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void startLocationUpdates() {
        // Frequent updates for admin history recording; normal update otherwise
        int intervalMs = prefManager.isAdmin() ? 15000 : 30000;

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() == null) return;

                String userId = prefManager.getUserId();
                String userName = prefManager.getUserName();
                if (userId == null) return;

                double lat = locationResult.getLastLocation().getLatitude();
                double lng = locationResult.getLastLocation().getLongitude();

                // Always update current location
                FirebaseManager.getInstance().updateUserLocation(userId, lat, lng);

                // Save history only on admin devices, with throttle
                if (prefManager.isAdmin()) {
                    long now = System.currentTimeMillis();
                    if (now - lastHistorySave >= Constants.LOCATION_HISTORY_INTERVAL) {
                        lastHistorySave = now;
                        FirebaseManager.getInstance().saveLocationHistory(userId, userName, lat, lng);
                        // Also prune old history
                        FirebaseManager.getInstance().pruneLocationHistory(userId);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission denied", e);
        }
    }

    /** Listen for remote "wake" commands sent from admin panel */
    private void listenForRemoteCommands() {
        String userId = prefManager.getUserId();
        if (userId == null) return;

        remoteCommandListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                Boolean processed = snapshot.child("processed").getValue(Boolean.class);
                if (processed != null && processed) return;

                String command = snapshot.child("command").getValue(String.class);
                if (command == null) return;

                Log.d(TAG, "Remote command received: " + command);

                switch (command) {
                    case "wake":
                    case "restart_service":
                        // Restart emergency listener service
                        Intent serviceIntent = new Intent(LocationService.this, EmergencyListenerService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent);
                        } else {
                            startService(serviceIntent);
                        }
                        break;
                    case "ping":
                        // Update location immediately
                        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                            if (loc != null) {
                                String uid = prefManager.getUserId();
                                if (uid != null) {
                                    FirebaseManager.getInstance().updateUserLocation(uid, loc.getLatitude(), loc.getLongitude());
                                }
                            }
                        });
                        break;
                }

                FirebaseManager.getInstance().markCommandProcessed(userId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Remote command listener error: " + error.getMessage());
            }
        };

        FirebaseManager.getInstance().listenForRemoteCommands(userId, remoteCommandListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}