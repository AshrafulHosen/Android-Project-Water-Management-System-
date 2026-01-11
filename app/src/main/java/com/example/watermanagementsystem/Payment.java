package com.example.watermanagementsystem;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class Payment {

    @PropertyName("paymentId")
    private String paymentId;

    @PropertyName("billId")
    private String billId;

    @PropertyName("username")
    private String username;

    @PropertyName("fullName")
    private String fullName;

    @PropertyName("amount")
    private double amount;

    @PropertyName("paymentMethod")
    private String paymentMethod; // "Cash", "Card", "Mobile Banking"

    @PropertyName("transactionId")
    private String transactionId;

    @PropertyName("timestamp")
    private long timestamp;

    @PropertyName("status")
    private String status; // "Completed", "Pending", "Failed"

    // Default constructor required for Firebase
    public Payment() {
    }

    public Payment(String paymentId, String billId, String username, String fullName,
                   double amount, String paymentMethod, String transactionId,
                   long timestamp, String status) {
        this.paymentId = paymentId;
        this.billId = billId;
        this.username = username;
        this.fullName = fullName;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

