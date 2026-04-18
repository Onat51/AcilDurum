package com.example.emergencyapp.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmergencyEvent {
    private String id;
    private String senderId;
    private String senderName;
    private int emergencyType;
    private double latitude;
    private double longitude;
    private long timestamp;
    private boolean isActive;
    private Map<String, LocationData> userLocations;
    private String customTitle;
    private String customDescription;

    /**
     * Alarmın gönderileceği kişilerin uid listesi.
     * Gönderici ContactsActivity'deki seçili kişilerini bu listeye yazar.
     * Alıcı cihazlar bu listeye bakarak "bu alarm bana mı?" diye kontrol eder.
     */
    private List<String> targetUserIds;

    public EmergencyEvent() {
        userLocations = new HashMap<>();
        targetUserIds = new ArrayList<>();
    }

    public EmergencyEvent(String id, String senderId, String senderName,
                          int emergencyType, double latitude, double longitude) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.emergencyType = emergencyType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
        this.isActive = true;
        this.userLocations = new HashMap<>();
        this.targetUserIds = new ArrayList<>();
    }

    public String getEmergencyTypeText() {
        if (emergencyType == 99 && customTitle != null) return "⚡ " + customTitle.toUpperCase();
        switch (emergencyType) {
            case 1: return "Kaçırılıyorum 🏃";
            case 2: return "Takip Ediliyorum 👁️";
            case 3: return "Ambulans/Kaza 🚑";
            case 4: return "Hemen Buraya Gelin 📍";
            case 5: return "TAM ACİL DURUM ⚠️";
            default: return "Acil Durum";
        }
    }

    public String getTTSMessage() {
        String typeText;
        if (emergencyType == 99 && customTitle != null) {
            typeText = customTitle + " durumu var";
        } else {
            switch (emergencyType) {
                case 1: typeText = "Kaçırılıyor"; break;
                case 2: typeText = "Takip Ediliyor"; break;
                case 3: typeText = "Ambulans İstiyor"; break;
                case 4: typeText = "Hemen Orada Olmanızı İstiyor"; break;
                case 5: typeText = "Tam Acil Durum İlan Etti"; break;
                default: typeText = "Acil Durum"; break;
            }
        }
        return senderName + " " + typeText;
    }

    // ─── Getters / Setters ──────────────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public int getEmergencyType() { return emergencyType; }
    public void setEmergencyType(int emergencyType) { this.emergencyType = emergencyType; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Map<String, LocationData> getUserLocations() { return userLocations; }
    public void setUserLocations(Map<String, LocationData> userLocations) { this.userLocations = userLocations; }

    public String getCustomTitle() { return customTitle; }
    public void setCustomTitle(String customTitle) { this.customTitle = customTitle; }

    public String getCustomDescription() { return customDescription; }
    public void setCustomDescription(String customDescription) { this.customDescription = customDescription; }

    public List<String> getTargetUserIds() { return targetUserIds; }
    public void setTargetUserIds(List<String> targetUserIds) { this.targetUserIds = targetUserIds; }
}
