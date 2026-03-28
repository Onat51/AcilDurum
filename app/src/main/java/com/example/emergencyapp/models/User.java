package com.example.emergencyapp.models;

public class User {
    private String id;
    private String name;
    private String fcmToken;
    private long createdAt;
    private double latitude;
    private double longitude;
    private long lastLocationUpdate;

    public User() {}

    public User(String id, String name, String fcmToken) {
        this.id = id;
        this.name = name;
        this.fcmToken = fcmToken;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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
}