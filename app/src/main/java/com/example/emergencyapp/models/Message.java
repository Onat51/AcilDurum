package com.example.emergencyapp.models;

public class Message {
    private String id;
    private String senderId;
    private String senderName;
    private String content;
    private long timestamp;
    private boolean isHint;
    private String emergencyId;

    // Boş constructor (Firebase için gerekli)
    public Message() {
    }

    public Message(String id, String senderId, String senderName,
                   String content, String emergencyId, boolean isHint) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.emergencyId = emergencyId;
        this.isHint = isHint;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public boolean isHint() { return isHint; }
    public String getEmergencyId() { return emergencyId; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setHint(boolean hint) { this.isHint = hint; }
    public void setEmergencyId(String emergencyId) { this.emergencyId = emergencyId; }
}