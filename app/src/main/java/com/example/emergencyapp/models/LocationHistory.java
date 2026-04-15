package com.example.emergencyapp.models;

public class LocationHistory {
    private String userId;
    private String userName;
    private double latitude;
    private double longitude;
    private long timestamp;

    public LocationHistory() {}

    public LocationHistory(String userId, String userName, double latitude, double longitude) {
        this.userId = userId;
        this.userName = userName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}