package com.example.watermanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView welcomeLabel, supplyLevelLabel, supplyMessageLabel;
    private EditText newSupplyField;
    private RecyclerView requestTable;
    private Button btnUpdateSupply, refreshButton, logoutButton, approveButton, rejectButton;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WaterManagementPrefs";

    private String currentFullName;
    private List<WaterRequest> allRequestsList;
    private AdminRequestAdapter adapter;
    private WaterRequest selectedRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        // Get SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Get admin info
        currentFullName = sharedPreferences.getString("fullName", "Admin");

        // Get from intent if available
        String intentFullName = getIntent().getStringExtra("fullName");
        if (intentFullName != null && !intentFullName.isEmpty()) {
            currentFullName = intentFullName;
        }

        // Initialize views
        initializeViews();

        // Set welcome message
        welcomeLabel.setText("Welcome, " + currentFullName + "!");

        // Setup RecyclerView with selection listener
        allRequestsList = new ArrayList<>();
        adapter = new AdminRequestAdapter(allRequestsList, this::onRequestSelected);
        requestTable.setLayoutManager(new LinearLayoutManager(this));
        requestTable.setAdapter(adapter);

        // Setup button listeners
        setupListeners();

        // Load initial data
        loadSupplyLevel();
        loadAllRequests();
    }

    private void initializeViews() {
        welcomeLabel = findViewById(R.id.welcomeLabel);
        supplyLevelLabel = findViewById(R.id.supplyLevelLabel);
        supplyMessageLabel = findViewById(R.id.supplyMessageLabel);
        newSupplyField = findViewById(R.id.newSupplyField);
        requestTable = findViewById(R.id.requestTable);
        btnUpdateSupply = findViewById(R.id.btnUpdateSupply);
        refreshButton = findViewById(R.id.refreshButton);
        logoutButton = findViewById(R.id.btnLogout);
        approveButton = findViewById(R.id.approveButton);
        rejectButton = findViewById(R.id.rejectButton);

        // Initially disable approve/reject buttons
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
    }

    private void setupListeners() {
        btnUpdateSupply.setOnClickListener(v -> updateSupplyLevel());
        refreshButton.setOnClickListener(v -> {
            adapter.clearSelection();
            selectedRequest = null;
            updateButtonStates();
            loadAllRequests();
        });
        logoutButton.setOnClickListener(v -> logout());

        // Approve button click
        approveButton.setOnClickListener(v -> {
            if (selectedRequest != null) {
                updateRequestStatus(selectedRequest, "Approved");
            } else {
                Toast.makeText(this, "Please select a request first", Toast.LENGTH_SHORT).show();
            }
        });

        // Reject button click
        rejectButton.setOnClickListener(v -> {
            if (selectedRequest != null) {
                updateRequestStatus(selectedRequest, "Rejected");
            } else {
                Toast.makeText(this, "Please select a request first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onRequestSelected(WaterRequest request, int position) {
        selectedRequest = request;
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (selectedRequest != null && "Pending".equalsIgnoreCase(selectedRequest.getStatus())) {
            approveButton.setEnabled(true);
            rejectButton.setEnabled(true);
        } else {
            approveButton.setEnabled(false);
            rejectButton.setEnabled(false);
        }
    }

    private void loadSupplyLevel() {
        databaseReference.child("supply_level").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double level = snapshot.getValue(Double.class);
                    if (level != null) {
                        supplyLevelLabel.setText(String.format("%.1f L", level));
                    }
                } else {
                    supplyLevelLabel.setText("0.0 L");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                supplyMessageLabel.setText("Error loading supply level");
                supplyMessageLabel.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateSupplyLevel() {
        String supplyStr = newSupplyField.getText().toString().trim();

        if (supplyStr.isEmpty()) {
            supplyMessageLabel.setText("Please enter a supply level.");
            supplyMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        double newLevel;
        try {
            newLevel = Double.parseDouble(supplyStr);
        } catch (NumberFormatException e) {
            supplyMessageLabel.setText("Invalid number format.");
            supplyMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        if (newLevel < 0) {
            supplyMessageLabel.setText("Supply level cannot be negative.");
            supplyMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        databaseReference.child("supply_level").setValue(newLevel)
                .addOnSuccessListener(aVoid -> {
                    supplyMessageLabel.setText("Supply level updated successfully!");
                    supplyMessageLabel.setVisibility(View.VISIBLE);
                    newSupplyField.setText("");
                })
                .addOnFailureListener(e -> {
                    supplyMessageLabel.setText("Failed to update: " + e.getMessage());
                    supplyMessageLabel.setVisibility(View.VISIBLE);
                });
    }

    private void loadAllRequests() {
        databaseReference.child("water_requests")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allRequestsList.clear();
                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            WaterRequest request = requestSnapshot.getValue(WaterRequest.class);
                            if (request != null) {
                                allRequestsList.add(request);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (allRequestsList.isEmpty()) {
                            Toast.makeText(AdminDashboardActivity.this, "No requests found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminDashboardActivity.this,
                                "Error loading requests: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateRequestStatus(WaterRequest request, String newStatus) {
        if (request.getRequestId() == null) {
            Toast.makeText(this, "Error: Invalid request ID", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child("water_requests").child(request.getRequestId())
                .child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminDashboardActivity.this,
                            "Request " + newStatus.toLowerCase() + " successfully!", Toast.LENGTH_SHORT).show();
                    // Clear selection and reload
                    adapter.clearSelection();
                    selectedRequest = null;
                    updateButtonStates();
                    loadAllRequests();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminDashboardActivity.this,
                            "Failed to update request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        // Sign out from Firebase Auth
        mAuth.signOut();

        // Clear SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Navigate back to login
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if admin is still authenticated
        String savedRole = sharedPreferences.getString("role", "");
        if (mAuth.getCurrentUser() == null && !sharedPreferences.getBoolean("isLoggedIn", false)) {
            // User not logged in, redirect to login
            redirectToLogin();
        } else if (!"admin".equals(savedRole)) {
            // Not an admin, redirect to login
            redirectToLogin();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

