package com.example.emergencyapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.emergencyapp.R;
import com.example.emergencyapp.activities.EmergencyReceivedActivity;
import com.example.emergencyapp.models.EmergencyEvent;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Set;

public class EmergencyListenerService extends Service {
    private static final String TAG = "EmergencyListener";
    private static final int FOREGROUND_ID = 2001;

    private DatabaseReference activeEmergencyRef;
    private ValueEventListener emergencyListener;
    private PreferenceManager prefManager;
    private String lastEmergencyId = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        prefManager = new PreferenceManager(this);
        createNotificationChannels();
        startForeground(FOREGROUND_ID, createForegroundNotification());
        startListening();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "service_channel", "Arka Plan Servisi", NotificationManager.IMPORTANCE_LOW);

            NotificationChannel emergencyChannel = new NotificationChannel(
                    Constants.CHANNEL_ID, "Acil Durum Bildirimleri", NotificationManager.IMPORTANCE_HIGH);
            emergencyChannel.setDescription("Acil durum bildirimleri");
            emergencyChannel.enableVibration(true);
            emergencyChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            emergencyChannel.setBypassDnd(true);
            emergencyChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            emergencyChannel.setSound(alarmSound, audioAttributes);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            manager.createNotificationChannel(emergencyChannel);
        }
    }

    private Notification createForegroundNotification() {
        Intent intent = new Intent(this, com.example.emergencyapp.activities.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, "service_channel")
                .setContentTitle("Acil Durum Uygulaması")
                .setContentText("Acil durumlar dinleniyor...")
                .setSmallIcon(R.drawable.ic_emergency)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void startListening() {
        activeEmergencyRef = FirebaseDatabase.getInstance()
                .getReference(Constants.PATH_ACTIVE_EMERGENCY);

        emergencyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    lastEmergencyId = null;
                    return;
                }

                EmergencyEvent event = snapshot.getValue(EmergencyEvent.class);
                if (event == null || !event.isActive()) return;

                String myId = prefManager.getUserId();

                // Don't alert for our own emergencies
                if (myId != null && myId.equals(event.getSenderId())) return;

                // ─── CONTACT FILTER ──────────────────────────────────────────
                // Only alert if sender is in our contacts list.
                // Exception: admin sees everything.
                if (!prefManager.isAdmin()) {
                    Set<String> contacts = prefManager.getSelectedUsers();
                    if (contacts.isEmpty() || !contacts.contains(event.getSenderId())) {
                        Log.d(TAG, "Emergency from non-contact " + event.getSenderId() + " — ignoring");
                        return;
                    }
                }
                // ─────────────────────────────────────────────────────────────

                // Don't show same emergency twice
                if (lastEmergencyId != null && lastEmergencyId.equals(event.getId())) return;

                lastEmergencyId = event.getId();
                showEmergencyNotification(event);
                openEmergencyActivity(event);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        };

        activeEmergencyRef.addValueEventListener(emergencyListener);
        Log.d(TAG, "Listening for emergencies");
    }

    private void showEmergencyNotification(EmergencyEvent event) {
        Intent intent = buildEmergencyIntent(event);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        String title = event.getEmergencyType() == Constants.EMERGENCY_CUSTOM && event.getCustomTitle() != null
                ? "⚡ " + event.getCustomTitle().toUpperCase() + "!"
                : "🚨 ACİL DURUM!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_emergency)
                .setContentTitle(title)
                .setContentText(event.getSenderName() + " - " + event.getEmergencyTypeText())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(event.getSenderName() + " acil durum ilan etti!\n" + event.getEmergencyTypeText()))
                .setAutoCancel(true)
                .setSound(alarmSound)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(new long[]{0, 500, 200, 500, 200, 500})
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(Constants.NOTIFICATION_ID, builder.build());
    }

    private void openEmergencyActivity(EmergencyEvent event) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "EmergencyApp:WakeLock");
        wakeLock.acquire(10 * 1000L);

        Intent intent = buildEmergencyIntent(event);
        startActivity(intent);
    }

    private Intent buildEmergencyIntent(EmergencyEvent event) {
        Intent intent = new Intent(this, EmergencyReceivedActivity.class);
        intent.putExtra("emergency_id", event.getId());
        intent.putExtra("sender_name", event.getSenderName());
        intent.putExtra("emergency_type", event.getEmergencyType());
        intent.putExtra("latitude", event.getLatitude());
        intent.putExtra("longitude", event.getLongitude());
        if (event.getCustomTitle() != null) intent.putExtra("custom_title", event.getCustomTitle());
        if (event.getCustomDescription() != null) intent.putExtra("custom_description", event.getCustomDescription());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activeEmergencyRef != null && emergencyListener != null) {
            activeEmergencyRef.removeEventListener(emergencyListener);
        }
        Log.d(TAG, "Service destroyed");
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}