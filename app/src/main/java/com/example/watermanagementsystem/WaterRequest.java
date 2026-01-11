package com.example.watermanagementsystem;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class WaterRequest {

    @PropertyName("requestId")
    private String requestId;

    @PropertyName("username")
    private String username;

    @PropertyName("fullName")
    private String fullName;

    @PropertyName("volume")
    private double volume;

    @PropertyName("status")
    private String status;

    @PropertyName("timestamp")
    private long timestamp;

    @PropertyName("billed")
    private boolean billed;

    @PropertyName("billId")
    private String billId;

    // Default constructor required for Firebase
    public WaterRequest() {
    }

    public WaterRequest(String requestId, String username, String fullName, double volume, String status, long timestamp) {
        this.requestId = requestId;
        this.username = username;
        this.fullName = fullName;
        this.volume = volume;
        this.status = status;
        this.timestamp = timestamp;
        this.billed = false;
        this.billId = null;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @PropertyName("billed")
    public boolean isBilled() {
        return billed;
    }

    @PropertyName("billed")
    public void setBilled(boolean billed) {
        this.billed = billed;
    }

    @PropertyName("billId")
    public String getBillId() {
        return billId;
    }

    @PropertyName("billId")
    public void setBillId(String billId) {
        this.billId = billId;
    }
}

