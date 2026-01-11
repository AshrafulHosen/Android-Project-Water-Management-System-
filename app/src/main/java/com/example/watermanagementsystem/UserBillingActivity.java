package com.example.watermanagementsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserBillingActivity extends AppCompatActivity {

    private TextView titleLabel, totalDueLabel, paidBillsLabel, pendingBillsLabel;
    private RecyclerView billsRecyclerView;
    private Button btnPayBill, btnRefresh, btnBack;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WaterManagementPrefs";

    private String currentUsername;
    private String currentFullName;
    private List<Bill> billList;
    private BillAdapter adapter;
    private Bill selectedBill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_billing);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        // Get SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Get user info
        currentUsername = sharedPreferences.getString("username", "");
        currentFullName = sharedPreferences.getString("fullName", "User");

        if (currentUsername.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Setup RecyclerView
        billList = new ArrayList<>();
        adapter = new BillAdapter(billList, this::onBillSelected);
        billsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        billsRecyclerView.setAdapter(adapter);

        // Setup listeners
        setupListeners();

        // Load data
        loadUserBills();
    }

    private void initializeViews() {
        titleLabel = findViewById(R.id.titleLabel);
        totalDueLabel = findViewById(R.id.totalDueLabel);
        paidBillsLabel = findViewById(R.id.paidBillsLabel);
        pendingBillsLabel = findViewById(R.id.pendingBillsLabel);
        billsRecyclerView = findViewById(R.id.billsRecyclerView);
        btnPayBill = findViewById(R.id.btnPayBill);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnBack = findViewById(R.id.btnBack);

        titleLabel.setText("My Bills - " + currentFullName);
        btnPayBill.setEnabled(false);
    }

    private void setupListeners() {
        btnPayBill.setOnClickListener(v -> showPaymentDialog());
        btnRefresh.setOnClickListener(v -> {
            adapter.clearSelection();
            selectedBill = null;
            btnPayBill.setEnabled(false);
            loadUserBills();
        });
        btnBack.setOnClickListener(v -> finish());
    }

    private void onBillSelected(Bill bill, int position) {
        selectedBill = bill;
        btnPayBill.setEnabled(bill != null && "Pending".equals(bill.getStatus()));
    }

    private void loadUserBills() {
        if (currentUsername.isEmpty()) {
            Toast.makeText(this, "Error: No username found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use addValueEventListener for real-time updates
        databaseReference.child("bills")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        billList.clear();
                        double totalDue = 0;
                        int paidCount = 0;
                        int pendingCount = 0;

                        for (DataSnapshot billSnapshot : snapshot.getChildren()) {
                            Bill bill = billSnapshot.getValue(Bill.class);
                            // Filter bills for current user
                            if (bill != null && currentUsername.equals(bill.getUsername())) {
                                billList.add(bill);
                                if ("Paid".equals(bill.getStatus())) {
                                    paidCount++;
                                } else {
                                    pendingCount++;
                                    totalDue += bill.getAmount();
                                }
                            }
                        }

                        // Sort by createdAt - most recent first (descending order)
                        billList.sort((b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));

                        adapter.notifyDataSetChanged();
                        updateStatistics(totalDue, paidCount, pendingCount);

                        if (billList.isEmpty()) {
                            Toast.makeText(UserBillingActivity.this, "No bills found for " + currentUsername, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserBillingActivity.this,
                                "Error loading bills: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateStatistics(double totalDue, int paidCount, int pendingCount) {
        totalDueLabel.setText(String.format(Locale.getDefault(), "$%.2f", totalDue));
        paidBillsLabel.setText(String.valueOf(paidCount));
        pendingBillsLabel.setText(String.valueOf(pendingCount));
    }

    private void showPaymentDialog() {
        if (selectedBill == null || !"Pending".equals(selectedBill.getStatus())) {
            Toast.makeText(this, "Please select a pending bill", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create payment method selection dialog
        String[] paymentMethods = {"Cash", "Card", "Mobile Banking"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Payment Method");
        builder.setMessage(String.format(Locale.getDefault(),
                "Bill Amount: $%.2f\nPeriod: %s",
                selectedBill.getAmount(), selectedBill.getBillingPeriod()));

        builder.setSingleChoiceItems(paymentMethods, 0, null);

        builder.setPositiveButton("Pay Now", (dialog, which) -> {
            int selectedMethod = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            String paymentMethod = paymentMethods[selectedMethod];
            processPayment(selectedBill, paymentMethod);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void processPayment(Bill bill, String paymentMethod) {
        // Generate transaction ID
        String transactionId = "TXN-" + System.currentTimeMillis();

        // Create payment record
        String paymentId = databaseReference.child("payments").push().getKey();
        if (paymentId == null) {
            Toast.makeText(this, "Error generating payment ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("billId", bill.getBillId());
        paymentData.put("username", currentUsername);
        paymentData.put("fullName", currentFullName);
        paymentData.put("amount", bill.getAmount());
        paymentData.put("paymentMethod", paymentMethod);
        paymentData.put("transactionId", transactionId);
        paymentData.put("timestamp", ServerValue.TIMESTAMP);
        paymentData.put("status", "Completed");

        // Save payment and update bill status
        databaseReference.child("payments").child(paymentId).setValue(paymentData)
                .addOnSuccessListener(aVoid -> {
                    // Update bill status
                    Map<String, Object> billUpdates = new HashMap<>();
                    billUpdates.put("status", "Paid");
                    billUpdates.put("paidAt", ServerValue.TIMESTAMP);

                    databaseReference.child("bills").child(bill.getBillId())
                            .updateChildren(billUpdates)
                            .addOnSuccessListener(aVoid2 -> {
                                showPaymentConfirmation(transactionId, bill.getAmount(), paymentMethod);
                                adapter.clearSelection();
                                selectedBill = null;
                                btnPayBill.setEnabled(false);
                                loadUserBills();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(UserBillingActivity.this,
                                        "Payment recorded but bill update failed", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserBillingActivity.this,
                            "Payment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showPaymentConfirmation(String transactionId, double amount, String paymentMethod) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        String message = String.format(Locale.getDefault(),
                "Payment Successful!\n\n" +
                "Transaction ID: %s\n" +
                "Amount: $%.2f\n" +
                "Method: %s\n" +
                "Date: %s",
                transactionId, amount, paymentMethod, dateTime);

        new AlertDialog.Builder(this)
                .setTitle("Payment Confirmation")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
}

