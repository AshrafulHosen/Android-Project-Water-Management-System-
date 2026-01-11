package com.example.watermanagementsystem;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class Bill {

    @PropertyName("billId")
    private String billId;

    @PropertyName("username")
    private String username;

    @PropertyName("fullName")
    private String fullName;

    @PropertyName("amount")
    private double amount;

    @PropertyName("waterVolume")
    private double waterVolume;

    @PropertyName("status")
    private String status; // "Pending", "Paid", "Overdue"

    @PropertyName("dueDate")
    private long dueDate;

    @PropertyName("createdAt")
    private long createdAt;

    @PropertyName("paidAt")
    private long paidAt;

    @PropertyName("billingPeriod")
    private String billingPeriod; // e.g., "January 2026"

    // Default constructor required for Firebase
    public Bill() {
    }

    public Bill(String billId, String username, String fullName, double amount,
                double waterVolume, String status, long dueDate, long createdAt, String billingPeriod) {
        this.billId = billId;
        this.username = username;
        this.fullName = fullName;
        this.amount = amount;
        this.waterVolume = waterVolume;
        this.status = status;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.billingPeriod = billingPeriod;
        this.paidAt = 0;
    }

    // Getters and Setters
    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getWaterVolume() {
        return waterVolume;
    }

    public void setWaterVolume(double waterVolume) {
        this.waterVolume = waterVolume;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(long paidAt) {
        this.paidAt = paidAt;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    @Exclude
    public boolean isPaid() {
        return "Paid".equals(status);
    }

    @Exclude
    public boolean isPending() {
        return "Pending".equals(status);
    }

    @Exclude
    public boolean isOverdue() {
        return "Overdue".equals(status);
    }
}

