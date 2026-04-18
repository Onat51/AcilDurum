package com.example.emergencyapp.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;           // Firebase Realtime DB push key (veya Auth UID)
    private String uid;          // Firebase Auth UID (hesap yönetimi için)
    private String name;         // displayName
    private String email;        // e-posta (opsiyonel — Google girişinde dolu)
    private String authProvider; // "anonymous" | "google" | "email"
    private String fcmToken;
    private long createdAt;
    private double latitude;
    private double longitude;
    private long lastLocationUpdate;
    private List<String> groups; // Grup üyelikleri

    public User() {}

    /** Anonim / isim tabanlı kayıt (eski akış) */
    public User(String id, String name, String fcmToken) {
        this.id = id;
        this.uid = id;
        this.name = name;
        this.fcmToken = fcmToken;
        this.authProvider = "anonymous";
        this.createdAt = System.currentTimeMillis();
        this.groups = new ArrayList<>();
    }

    /** Firebase Auth ile kayıt */
    public User(String uid, String displayName, String email,
                String authProvider, String fcmToken) {
        this.id = uid;
        this.uid = uid;
        this.name = displayName;
        this.email = email;
        this.authProvider = authProvider;
        this.fcmToken = fcmToken;
        this.createdAt = System.currentTimeMillis();
        this.groups = new ArrayList<>();
    }

    // ─── Getters / Setters ──────────────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getLastLocationUpdate() { return lastLocationUpdate; }
    public void setLastLocationUpdate(long lastLocationUpdate) { this.lastLocationUpdate = lastLocationUpdate; }

    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups) { this.groups = groups; }
}
