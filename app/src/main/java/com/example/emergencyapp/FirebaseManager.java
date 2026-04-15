package com.example.emergencyapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.emergencyapp.models.CustomEmergencyCode;
import com.example.emergencyapp.models.EmergencyEvent;
import com.example.emergencyapp.models.LocationData;
import com.example.emergencyapp.models.LocationHistory;
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

    // ─── User Operations ───────────────────────────────────────────────────────

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

    public void getUser(String userId, ValueEventListener listener) {
        database.child(Constants.PATH_USERS).child(userId).addListenerForSingleValueEvent(listener);
    }

    // ─── Location History (24h) ─────────────────────────────────────────────────

    /**
     * Save a location history point (only called on admin devices).
     */
    public void saveLocationHistory(String userId, String userName, double lat, double lng) {
        String key = database.child(Constants.PATH_LOCATION_HISTORY).child(userId).push().getKey();
        LocationHistory loc = new LocationHistory(userId, userName, lat, lng);
        database.child(Constants.PATH_LOCATION_HISTORY).child(userId).child(key).setValue(loc);
    }

    public void listenToLocationHistory(String userId, ValueEventListener listener) {
        long cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(Constants.LOCATION_HISTORY_HOURS);
        database.child(Constants.PATH_LOCATION_HISTORY).child(userId)
                .orderByChild("timestamp")
                .startAt(cutoff)
                .addValueEventListener(listener);
    }

    public void listenToAllLocationHistories(ValueEventListener listener) {
        database.child(Constants.PATH_LOCATION_HISTORY).addValueEventListener(listener);
    }

    /** Prune entries older than 24h for a user. */
    public void pruneLocationHistory(String userId) {
        long cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        database.child(Constants.PATH_LOCATION_HISTORY).child(userId)
                .orderByChild("timestamp")
                .endAt(cutoff)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) child.getRef().removeValue();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ─── Emergency Operations ───────────────────────────────────────────────────

    public String createEmergency(String senderId, String senderName,
                                  int emergencyType, double lat, double lng) {
        return createEmergency(senderId, senderName, emergencyType, lat, lng, null, null);
    }

    public String createEmergency(String senderId, String senderName,
                                  int emergencyType, double lat, double lng,
                                  String customTitle, String customDescription) {
        String emergencyId = database.child(Constants.PATH_EMERGENCIES).push().getKey();
        EmergencyEvent event = new EmergencyEvent(emergencyId, senderId, senderName,
                emergencyType, lat, lng);
        if (customTitle != null) event.setCustomTitle(customTitle);
        if (customDescription != null) event.setCustomDescription(customDescription);

        database.child(Constants.PATH_EMERGENCIES).child(emergencyId).setValue(event);
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

    // ─── Emergency Location ─────────────────────────────────────────────────────

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

    // ─── Messaging ─────────────────────────────────────────────────────────────

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

    // ─── Custom Emergency Codes (Admin) ────────────────────────────────────────

    public void createCustomCode(String code, String title, String description,
                                 ValueEventListener checkListener) {
        // Check uniqueness first
        database.child(Constants.PATH_CUSTOM_CODES)
                .orderByChild("code").equalTo(code.toLowerCase().trim())
                .addListenerForSingleValueEvent(checkListener);
    }

    public void saveCustomCode(CustomEmergencyCode codeObj) {
        String id = database.child(Constants.PATH_CUSTOM_CODES).push().getKey();
        codeObj.setId(id);
        database.child(Constants.PATH_CUSTOM_CODES).child(id).setValue(codeObj);
    }

    public void deleteCustomCode(String id) {
        database.child(Constants.PATH_CUSTOM_CODES).child(id).removeValue();
    }

    public void lookupCode(String code, ValueEventListener listener) {
        database.child(Constants.PATH_CUSTOM_CODES)
                .orderByChild("code").equalTo(code.toLowerCase().trim())
                .addListenerForSingleValueEvent(listener);
    }

    public void listenToCustomCodes(ValueEventListener listener) {
        database.child(Constants.PATH_CUSTOM_CODES).addValueEventListener(listener);
    }

    // ─── Remote Commands ────────────────────────────────────────────────────────

    /**
     * Send a remote command to a specific user device (e.g. wake up / ping).
     * command: "wake" | "ping" | "restart_service"
     */
    public void sendRemoteCommand(String targetUserId, String command) {
        Map<String, Object> cmd = new HashMap<>();
        cmd.put("command", command);
        cmd.put("timestamp", ServerValue.TIMESTAMP);
        cmd.put("processed", false);
        database.child(Constants.PATH_REMOTE_COMMANDS).child(targetUserId).setValue(cmd);
    }

    /** Listen for remote commands directed at this device. */
    public void listenForRemoteCommands(String myUserId, ValueEventListener listener) {
        database.child(Constants.PATH_REMOTE_COMMANDS).child(myUserId).addValueEventListener(listener);
    }

    public void markCommandProcessed(String userId) {
        database.child(Constants.PATH_REMOTE_COMMANDS).child(userId).child("processed").setValue(true);
    }

    // ─── User list / FCM ────────────────────────────────────────────────────────

    public void getAllUsers(ValueEventListener listener) {
        database.child(Constants.PATH_USERS).addListenerForSingleValueEvent(listener);
    }

    public void listenToUsers(ValueEventListener listener) {
        database.child(Constants.PATH_USERS).addValueEventListener(listener);
    }

    // ─── Cleanup ────────────────────────────────────────────────────────────────

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
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Cleanup error: " + error.getMessage());
                    }
                });
    }

    public DatabaseReference getDatabase() { return database; }
}