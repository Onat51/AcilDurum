package com.example.emergencyapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.example.emergencyapp.utils.Constants;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.config.Configuration;

public class EmergencyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Initialize OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Create notification channel
        createNotificationChannel();

        // Cleanup old data
        FirebaseManager.getInstance().cleanupOldData();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID,
                    "Acil Durum Bildirimleri",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Acil durum bildirimleri için kanal");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}