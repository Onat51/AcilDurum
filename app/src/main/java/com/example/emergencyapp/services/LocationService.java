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

    private static volatile boolean sRunning = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PreferenceManager prefManager;
    private ValueEventListener remoteCommandListener;
    private long lastHistorySave = 0;

    public static boolean isRunning() { return sRunning; }

    @Override
    public void onCreate() {
        super.onCreate();
        sRunning = true;
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
            channel.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "location_channel")
                .setContentTitle("Acil Durum Uygulaması")
                .setContentText("Konum paylaşımı aktif")
                .setSmallIcon(R.drawable.ic_location)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    private void startLocationUpdates() {
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

                FirebaseManager.getInstance().updateUserLocation(userId, lat, lng);

                if (prefManager.isAdmin()) {
                    long now = System.currentTimeMillis();
                    if (now - lastHistorySave >= Constants.LOCATION_HISTORY_INTERVAL) {
                        lastHistorySave = now;
                        FirebaseManager.getInstance().saveLocationHistory(userId, userName, lat, lng);
                        FirebaseManager.getInstance().pruneLocationHistory(userId);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Konum izni yok", e);
        }
    }

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

                Log.d(TAG, "Remote komut alındı: " + command);

                switch (command) {
                    case "wake":
                    case "restart_service":
                        Intent svc = new Intent(LocationService.this, EmergencyListenerService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            startForegroundService(svc);
                        else
                            startService(svc);
                        break;
                    case "ping":
                        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                            if (loc != null) {
                                String uid = prefManager.getUserId();
                                if (uid != null)
                                    FirebaseManager.getInstance().updateUserLocation(
                                            uid, loc.getLatitude(), loc.getLongitude());
                            }
                        });
                        break;
                    case "test_alarm":
                        // Test alarmı: yerel bildirim göster (gerçek alarm değil)
                        showTestAlarmNotification(snapshot.child("fromName")
                                .getValue(String.class));
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

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    private void showTestAlarmNotification(String fromName) {
        android.app.NotificationManager nm =
                (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        android.app.Notification notif = new androidx.core.app.NotificationCompat
                .Builder(this, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_emergency)
                .setContentTitle("🧪 Test Alarmı")
                .setContentText((fromName != null ? fromName : "Admin") + " tarafından test gönderildi")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 300, 200, 300})
                .build();
        nm.notify(Constants.NOTIFICATION_ID + 10, notif);
        Log.d(TAG, "Test alarm bildirimi gösterildi");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sRunning = false;
        if (fusedLocationClient != null && locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
