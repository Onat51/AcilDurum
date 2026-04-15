package com.example.emergencyapp.models;

public class CustomEmergencyCode {
    private String id;
    private String code;       // e.g. "elma"
    private String title;      // e.g. "Kavga Var"
    private String description;
    private long createdAt;
    private boolean isActive;

    public CustomEmergencyCode() {}

    public CustomEmergencyCode(String id, String code, String title, String description) {
        this.id = id;
        this.code = code.toLowerCase().trim();
        this.title = title;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}