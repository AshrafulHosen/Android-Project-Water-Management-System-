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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BillingActivity extends AppCompatActivity {

    private TextView titleLabel, totalBillsLabel, totalRevenueLabel, pendingAmountLabel;
    private EditText ratePerLiterField, searchUserField;
    private Spinner userSpinner;
    private RecyclerView billsRecyclerView;
    private Button btnGenerateBill, btnMarkAsPaid, btnRefresh, btnBack;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WaterManagementPrefs";

    private List<Bill> billList;
    private List<Users> usersList;
    private BillAdapter adapter;
    private Bill selectedBill;

    private double ratePerLiter = 0.05; // Default rate per liter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        // Get SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if admin
        String savedRole = sharedPreferences.getString("role", "");
        if (!"admin".equals(savedRole)) {
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Setup RecyclerView
        billList = new ArrayList<>();
        usersList = new ArrayList<>();
        adapter = new BillAdapter(billList, this::onBillSelected);
        billsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        billsRecyclerView.setAdapter(adapter);

        // Setup listeners
        setupListeners();

        // Load data
        loadUsers();
        loadBills();
        loadSettings();
    }

    private void initializeViews() {
        titleLabel = findViewById(R.id.titleLabel);
        totalBillsLabel = findViewById(R.id.totalBillsLabel);
        totalRevenueLabel = findViewById(R.id.totalRevenueLabel);
        pendingAmountLabel = findViewById(R.id.pendingAmountLabel);
        ratePerLiterField = findViewById(R.id.ratePerLiterField);
        userSpinner = findViewById(R.id.userSpinner);
        billsRecyclerView = findViewById(R.id.billsRecyclerView);
        btnGenerateBill = findViewById(R.id.btnGenerateBill);
        btnMarkAsPaid = findViewById(R.id.btnMarkAsPaid);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnBack = findViewById(R.id.btnBack);

        btnMarkAsPaid.setEnabled(false);
    }

    private void setupListeners() {
        btnGenerateBill.setOnClickListener(v -> showGenerateBillDialog());
        btnMarkAsPaid.setOnClickListener(v -> markBillAsPaid());
        btnRefresh.setOnClickListener(v -> {
            adapter.clearSelection();
            selectedBill = null;
            btnMarkAsPaid.setEnabled(false);
            loadBills();
        });
        btnBack.setOnClickListener(v -> finish());

        ratePerLiterField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveRatePerLiter();
            }
        });
    }

    private void onBillSelected(Bill bill, int position) {
        selectedBill = bill;
        btnMarkAsPaid.setEnabled(bill != null && "Pending".equals(bill.getStatus()));
    }

    private void loadSettings() {
        databaseReference.child("settings").child("ratePerLiter")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Double rate = snapshot.getValue(Double.class);
                            if (rate != null) {
                                ratePerLiter = rate;
                                ratePerLiterField.setText(String.format(Locale.getDefault(), "%.2f", rate));
                            }
                        } else {
                            ratePerLiterField.setText(String.format(Locale.getDefault(), "%.2f", ratePerLiter));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        ratePerLiterField.setText(String.format(Locale.getDefault(), "%.2f", ratePerLiter));
                    }
                });
    }

    private void saveRatePerLiter() {
        String rateStr = ratePerLiterField.getText().toString().trim();
        if (!rateStr.isEmpty()) {
            try {
                double newRate = Double.parseDouble(rateStr);
                if (newRate > 0) {
                    ratePerLiter = newRate;
                    databaseReference.child("settings").child("ratePerLiter").setValue(newRate);
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        }
    }

    private void loadUsers() {
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();
                List<String> userNames = new ArrayList<>();
                userNames.add("Select User");

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Users user = userSnapshot.getValue(Users.class);
                    if (user != null && "user".equals(user.getRole())) {
                        usersList.add(user);
                        userNames.add(user.getFullName() + " (" + user.getUsername() + ")");
                    }
                }

                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        BillingActivity.this,
                        android.R.layout.simple_spinner_item,
                        userNames
                );
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                userSpinner.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BillingActivity.this, "Error loading users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBills() {
        databaseReference.child("bills").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                billList.clear();
                double totalRevenue = 0;
                double pendingAmount = 0;

                for (DataSnapshot billSnapshot : snapshot.getChildren()) {
                    Bill bill = billSnapshot.getValue(Bill.class);
                    if (bill != null) {
                        billList.add(bill);
                        if ("Paid".equals(bill.getStatus())) {
                            totalRevenue += bill.getAmount();
                        } else {
                            pendingAmount += bill.getAmount();
                        }
                    }
                }

                // Sort by createdAt - most recent first (descending order)
                billList.sort((b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));

                adapter.notifyDataSetChanged();
                updateStatistics(billList.size(), totalRevenue, pendingAmount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BillingActivity.this, "Error loading bills", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatistics(int totalBills, double totalRevenue, double pendingAmount) {
        totalBillsLabel.setText(String.valueOf(totalBills));
        totalRevenueLabel.setText(String.format(Locale.getDefault(), "$%.2f", totalRevenue));
        pendingAmountLabel.setText(String.format(Locale.getDefault(), "$%.2f", pendingAmount));
    }

    private void showGenerateBillDialog() {
        int selectedIndex = userSpinner.getSelectedItemPosition();
        if (selectedIndex <= 0) {
            Toast.makeText(this, "Please select a user", Toast.LENGTH_SHORT).show();
            return;
        }

        Users selectedUser = usersList.get(selectedIndex - 1);
        String selectedUsername = selectedUser.getUsername();

        if (selectedUsername == null || selectedUsername.isEmpty()) {
            Toast.makeText(this, "Error: Invalid user selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current billing period
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentBillingPeriod = monthFormat.format(new Date());

        // Directly proceed to calculate volume and generate bill
        // Multiple bills per user per month are allowed
        calculateVolumeAndShowDialog(selectedUser, currentBillingPeriod);
    }


    private void calculateVolumeAndShowDialog(Users selectedUser, String billingPeriod) {
        // Calculate total approved water volume for the user (only unbilled requests)
        // Load all requests and filter in code to avoid Firebase index requirement
        databaseReference.child("water_requests")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalVolume = 0;
                        List<String> unbilledRequestIds = new ArrayList<>();

                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            WaterRequest request = requestSnapshot.getValue(WaterRequest.class);
                            if (request != null &&
                                selectedUser.getUsername().equals(request.getUsername()) &&
                                "Approved".equals(request.getStatus()) &&
                                !request.isBilled()) {  // Only unbilled requests
                                totalVolume += request.getVolume();
                                unbilledRequestIds.add(request.getRequestId());
                            }
                        }

                        if (totalVolume <= 0) {
                            Toast.makeText(BillingActivity.this,
                                    "No unbilled approved water requests for this user", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        showBillConfirmationDialog(selectedUser, totalVolume, billingPeriod, unbilledRequestIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(BillingActivity.this, "Error calculating usage", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showBillConfirmationDialog(Users user, double totalVolume, String billingPeriod, List<String> requestIds) {
        double amount = totalVolume * ratePerLiter;

        // Calculate due date (end of next month)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        long dueDate = calendar.getTimeInMillis();

        String message = String.format(Locale.getDefault(),
                "Generate bill for:\n\nUser: %s\nVolume: %.1f L\nRate: $%.2f/L\nTotal Amount: $%.2f\nRequests: %d\n\nBilling Period: %s",
                user.getFullName(), totalVolume, ratePerLiter, amount, requestIds.size(), billingPeriod);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Bill Generation")
                .setMessage(message)
                .setPositiveButton("Generate", (dialog, which) -> {
                    generateBill(user, totalVolume, amount, billingPeriod, dueDate, requestIds);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateBill(Users user, double volume, double amount, String billingPeriod, long dueDate, List<String> requestIds) {
        String billId = databaseReference.child("bills").push().getKey();
        if (billId == null) {
            Toast.makeText(this, "Error generating bill ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> billData = new HashMap<>();
        billData.put("billId", billId);
        billData.put("username", user.getUsername());
        billData.put("fullName", user.getFullName());
        billData.put("amount", amount);
        billData.put("waterVolume", volume);
        billData.put("status", "Pending");
        billData.put("dueDate", dueDate);
        billData.put("createdAt", ServerValue.TIMESTAMP);
        billData.put("billingPeriod", billingPeriod);
        billData.put("paidAt", 0);
        billData.put("requestIds", requestIds);  // Store which requests are included in this bill

        databaseReference.child("bills").child(billId).setValue(billData)
                .addOnSuccessListener(aVoid -> {
                    // Mark all included requests as billed
                    markRequestsAsBilled(requestIds, billId);
                    Toast.makeText(BillingActivity.this, "Bill generated successfully!", Toast.LENGTH_SHORT).show();
                    loadBills();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BillingActivity.this, "Failed to generate bill: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void markRequestsAsBilled(List<String> requestIds, String billId) {
        // Mark each water request as billed
        for (String requestId : requestIds) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("billed", true);
            updates.put("billId", billId);

            databaseReference.child("water_requests").child(requestId)
                    .updateChildren(updates);
        }
    }

    private void markBillAsPaid() {
        if (selectedBill == null) {
            Toast.makeText(this, "Please select a bill first", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Mark as Paid")
                .setMessage("Mark this bill as paid?\n\nBill: " + selectedBill.getBillId() +
                        "\nAmount: $" + String.format(Locale.getDefault(), "%.2f", selectedBill.getAmount()))
                .setPositiveButton("Confirm", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "Paid");
                    updates.put("paidAt", ServerValue.TIMESTAMP);

                    databaseReference.child("bills").child(selectedBill.getBillId())
                            .updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Create payment record
                                createPaymentRecord(selectedBill);
                                Toast.makeText(BillingActivity.this, "Bill marked as paid!", Toast.LENGTH_SHORT).show();
                                adapter.clearSelection();
                                selectedBill = null;
                                btnMarkAsPaid.setEnabled(false);
                                loadBills();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(BillingActivity.this, "Failed to update bill", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createPaymentRecord(Bill bill) {
        String paymentId = databaseReference.child("payments").push().getKey();
        if (paymentId == null) return;

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("billId", bill.getBillId());
        paymentData.put("username", bill.getUsername());
        paymentData.put("fullName", bill.getFullName());
        paymentData.put("amount", bill.getAmount());
        paymentData.put("paymentMethod", "Admin Marked");
        paymentData.put("transactionId", "ADMIN-" + System.currentTimeMillis());
        paymentData.put("timestamp", ServerValue.TIMESTAMP);
        paymentData.put("status", "Completed");

        databaseReference.child("payments").child(paymentId).setValue(paymentData);
    }
}

