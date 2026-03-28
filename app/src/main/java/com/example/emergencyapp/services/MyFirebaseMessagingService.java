package com.example.emergencyapp.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.activities.EmergencyReceivedActivity;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New token: " + token);

        PreferenceManager prefManager = new PreferenceManager(this);
        prefManager.setFcmToken(token);

        String userId = prefManager.getUserId();
        if (userId != null) {
            FirebaseManager.getInstance().updateUserFcmToken(userId, token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received");

        if (remoteMessage.getData().size() > 0) {
            String emergencyId = remoteMessage.getData().get("emergency_id");
            String senderName = remoteMessage.getData().get("sender_name");
            String emergencyTypeStr = remoteMessage.getData().get("emergency_type");

            if (emergencyId != null) {
                int emergencyType = Integer.parseInt(emergencyTypeStr != null ? emergencyTypeStr : "0");
                showEmergencyNotification(emergencyId, senderName, emergencyType);
            }
        }
    }

    private void showEmergencyNotification(String emergencyId, String senderName, int emergencyType) {
        Intent intent = new Intent(this, EmergencyReceivedActivity.class);
        intent.putExtra("emergency_id", emergencyId);
        intent.putExtra("sender_name", senderName);
        intent.putExtra("emergency_type", emergencyType);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_emergency)
                .setContentTitle("🚨 ACİL DURUM!")
                .setContentText(senderName + " acil durum ilan etti!")
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(Constants.NOTIFICATION_ID, builder.build());
    }
}