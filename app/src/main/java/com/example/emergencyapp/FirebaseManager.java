package com.example.emergencyapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.emergencyapp.models.EmergencyEvent;
import com.example.emergencyapp.models.LocationData;
import com.example.emergencyapp.models.Message;
import com.example.emergencyapp.models.User;
import com.example.emergencyapp.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FirebaseManager {
    private static FirebaseManager instance;
    private DatabaseReference database;
    private static final String TAG = "FirebaseManager";

    private FirebaseManager() {
        database = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // User Operations
    public String createUser(String name, String fcmToken) {
        String userId = database.child(Constants.PATH_USERS).push().getKey();
        User user = new User(userId, name, fcmToken);
        database.child(Constants.PATH_USERS).child(userId).setValue(user);
        return userId;
    }

    public void updateUserFcmToken(String userId, String token) {
        database.child(Constants.PATH_USERS).child(userId).child("fcmToken").setValue(token);
    }

    public void updateUserLocation(String userId, double latitude, double longitude) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);
        updates.put("lastLocationUpdate", ServerValue.TIMESTAMP);
        database.child(Constants.PATH_USERS).child(userId).updateChildren(updates);
    }

    // Emergency Operations
    public String createEmergency(String senderId, String senderName,
                                  int emergencyType, double lat, double lng) {
        String emergencyId = database.child(Constants.PATH_EMERGENCIES).push().getKey();
        EmergencyEvent event = new EmergencyEvent(emergencyId, senderId, senderName,
                emergencyType, lat, lng);

        // Save emergency
        database.child(Constants.PATH_EMERGENCIES).child(emergencyId).setValue(event);

        // Set as active emergency
        database.child(Constants.PATH_ACTIVE_EMERGENCY).setValue(event);

        return emergencyId;
    }

    public void cancelEmergency(String emergencyId) {
        database.child(Constants.PATH_EMERGENCIES).child(emergencyId).child("isActive").setValue(false);
        database.child(Constants.PATH_ACTIVE_EMERGENCY).removeValue();
    }

    public void listenToActiveEmergency(ValueEventListener listener) {
        database.child(Constants.PATH_ACTIVE_EMERGENCY).addValueEventListener(listener);
    }

    public void removeActiveEmergencyListener(ValueEventListener listener) {
        database.child(Constants.PATH_ACTIVE_EMERGENCY).removeEventListener(listener);
    }

    // Location Operations for Emergency
    public void updateEmergencyLocation(String emergencyId, String oderId,
                                        String userName, double lat, double lng) {
        LocationData locationData = new LocationData(oderId, userName, lat, lng);
        database.child(Constants.PATH_EMERGENCIES).child(emergencyId)
                .child("userLocations").child(oderId).setValue(locationData);
    }

    public void listenToEmergencyLocations(String emergencyId, ValueEventListener listener) {
        database.child(Constants.PATH_EMERGENCIES).child(emergencyId)
                .child("userLocations").addValueEventListener(listener);
    }

    // Message Operations
    public void sendMessage(String emergencyId, String senderId, String senderName,
                            String content, boolean isHint) {
        String messageId = database.child(Constants.PATH_MESSAGES).child(emergencyId).push().getKey();
        Message message = new Message(messageId, senderId, senderName, content, emergencyId, isHint);
        database.child(Constants.PATH_MESSAGES).child(emergencyId).child(messageId).setValue(message);

        if (isHint) {
            database.child(Constants.PATH_HINTS).child(emergencyId).child(messageId).setValue(message);
        }
    }

    public void listenToMessages(String emergencyId, ValueEventListener listener) {
        database.child(Constants.PATH_MESSAGES).child(emergencyId)
                .orderByChild("timestamp").addValueEventListener(listener);
    }

    public void listenToHints(String emergencyId, ValueEventListener listener) {
        database.child(Constants.PATH_HINTS).child(emergencyId)
                .orderByChild("timestamp").addValueEventListener(listener);
    }

    // Get all users for FCM notification
    public void getAllUsers(ValueEventListener listener) {
        database.child(Constants.PATH_USERS).addListenerForSingleValueEvent(listener);
    }

    // Data cleanup (30 days)
    public void cleanupOldData() {
        long thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

        database.child(Constants.PATH_EMERGENCIES).orderByChild("timestamp")
                .endAt(thirtyDaysAgo).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String emergencyId = child.getKey();
                            child.getRef().removeValue();
                            database.child(Constants.PATH_MESSAGES).child(emergencyId).removeValue();
                            database.child(Constants.PATH_HINTS).child(emergencyId).removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Cleanup error: " + error.getMessage());
                    }
                });
    }

    public DatabaseReference getDatabase() {
        return database;
    }
}