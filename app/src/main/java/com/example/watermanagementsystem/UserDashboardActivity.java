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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDashboardActivity extends AppCompatActivity {

    private TextView welcomeLabel, statusMessageLabel;
    private EditText volumeField;
    private RecyclerView requestTable;
    private Button submitRequestButton, refreshRequestsButton, logoutButton, myBillsButton;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WaterManagementPrefs";

    private String currentUsername;
    private String currentFullName;
    private List<WaterRequest> requestList;
    private WaterRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        // Get SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Get user info
        currentUsername = sharedPreferences.getString("username", "");
        currentFullName = sharedPreferences.getString("fullName", "User");

        // Get from intent if available
        String intentFullName = getIntent().getStringExtra("fullName");
        String intentUsername = getIntent().getStringExtra("username");
        if (intentFullName != null && !intentFullName.isEmpty()) {
            currentFullName = intentFullName;
        }
        if (intentUsername != null && !intentUsername.isEmpty()) {
            currentUsername = intentUsername;
        }

        // Debug: Check if username is available
        if (currentUsername.isEmpty()) {
            Toast.makeText(this, "Error: Username not found. Please login again.", Toast.LENGTH_LONG).show();
        }

        // Initialize views
        initializeViews();

        // Set welcome message
        welcomeLabel.setText("Welcome, " + currentFullName + "!");

        // Setup RecyclerView
        requestList = new ArrayList<>();
        adapter = new WaterRequestAdapter(requestList);
        requestTable.setLayoutManager(new LinearLayoutManager(this));
        requestTable.setAdapter(adapter);

        // Setup button listeners
        setupListeners();

        // Load initial data
        loadUserRequests();
    }

    private void initializeViews() {
        welcomeLabel = findViewById(R.id.welcomeLabel);
        statusMessageLabel = findViewById(R.id.statusMessageLabel);
        volumeField = findViewById(R.id.volumeField);
        requestTable = findViewById(R.id.requestTable);
        submitRequestButton = findViewById(R.id.submitRequestButton);
        refreshRequestsButton = findViewById(R.id.refreshRequestsButton);
        logoutButton = findViewById(R.id.logoutButton);
        myBillsButton = findViewById(R.id.myBillsButton);
    }

    private void setupListeners() {
        submitRequestButton.setOnClickListener(v -> submitWaterRequest());
        refreshRequestsButton.setOnClickListener(v -> loadUserRequests());
        logoutButton.setOnClickListener(v -> logout());
        myBillsButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, UserBillingActivity.class);
            startActivity(intent);
        });
    }

    private void submitWaterRequest() {
        String volumeStr = volumeField.getText().toString().trim();

        if (volumeStr.isEmpty()) {
            statusMessageLabel.setText("Please enter volume.");
            statusMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        double volume;
        try {
            volume = Double.parseDouble(volumeStr);
        } catch (NumberFormatException e) {
            statusMessageLabel.setText("Invalid volume format.");
            statusMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        if (volume <= 0) {
            statusMessageLabel.setText("Volume must be greater than 0.");
            statusMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        // Create water request
        String requestId = databaseReference.child("water_requests").push().getKey();
        if (requestId == null) {
            statusMessageLabel.setText("Error creating request.");
            statusMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("username", currentUsername);
        request.put("fullName", currentFullName);
        request.put("volume", volume);
        request.put("status", "Pending");
        request.put("timestamp", ServerValue.TIMESTAMP);
        request.put("billed", false);  // New requests are not billed yet

        databaseReference.child("water_requests").child(requestId).setValue(request)
                .addOnSuccessListener(aVoid -> {
                    statusMessageLabel.setText("Request submitted successfully!");
                    statusMessageLabel.setVisibility(View.VISIBLE);
                    volumeField.setText("");
                    loadUserRequests();
                })
                .addOnFailureListener(e -> {
                    statusMessageLabel.setText("Failed to submit request: " + e.getMessage());
                    statusMessageLabel.setVisibility(View.VISIBLE);
                });
    }

    private void loadUserRequests() {
        if (currentUsername.isEmpty()) {
            statusMessageLabel.setText("Error: No username. Please login again.");
            statusMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        // Load all requests and filter in code to avoid Firebase index requirement
        databaseReference.child("water_requests")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requestList.clear();
                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            WaterRequest request = requestSnapshot.getValue(WaterRequest.class);
                            // Filter requests for current user
                            if (request != null && currentUsername.equals(request.getUsername())) {
                                requestList.add(request);
                            }
                        }

                        // Sort by timestamp - most recent first (descending order)
                        requestList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                        adapter.notifyDataSetChanged();

                        if (requestList.isEmpty()) {
                            statusMessageLabel.setText("No requests found.");
                            statusMessageLabel.setVisibility(View.VISIBLE);
                        } else {
                            statusMessageLabel.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusMessageLabel.setText("Error loading requests: " + error.getMessage());
                        statusMessageLabel.setVisibility(View.VISIBLE);
                    }
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
        Intent intent = new Intent(UserDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is still authenticated
        if (mAuth.getCurrentUser() == null && !sharedPreferences.getBoolean("isLoggedIn", false)) {
            // User not logged in, redirect to login
            Intent intent = new Intent(UserDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}

