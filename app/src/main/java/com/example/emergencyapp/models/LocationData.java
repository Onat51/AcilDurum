package com.example.emergencyapp.models;

public class LocationData {
    private String oderId;
    private String userName;
    private double latitude;
    private double longitude;
    private long timestamp;

    public LocationData() {}

    public LocationData(String oderId, String userName, double latitude, double longitude) {
        this.oderId = oderId;
        this.userName = userName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUserId() { return oderId; }
    public void setUserId(String oderId) { this.oderId = oderId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}