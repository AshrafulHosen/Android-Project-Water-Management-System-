package com.example.watermanagementsystem;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    // Statistics TextViews
    private TextView totalUsersLabel, totalRequestsLabel, approvedRequestsLabel, pendingRequestsLabel;
    private TextView rejectedRequestsLabel, totalWaterUsedLabel, totalRevenueLabel, pendingRevenueLabel;
    private TextView avgRequestVolumeLabel, currentSupplyLabel;

    // Monthly Statistics
    private TextView monthlyRequestsLabel, monthlyApprovedLabel, monthlyVolumeLabel, monthlyRevenueLabel;

    // Top Users
    private TextView topUser1Label, topUser2Label, topUser3Label;

    // Period Labels
    private TextView currentPeriodLabel;

    private Button btnRefresh, btnBack;

    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WaterManagementPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        // Get SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if admin
        String savedRole = sharedPreferences.getString("role", "");
        if (!"admin".equals(savedRole)) {
            Toast.makeText(this, "Access denied. Admin only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Setup listeners
        setupListeners();

        // Load analytics data
        loadAnalytics();
    }

    private void initializeViews() {
        // Overall Statistics
        totalUsersLabel = findViewById(R.id.totalUsersLabel);
        totalRequestsLabel = findViewById(R.id.totalRequestsLabel);
        approvedRequestsLabel = findViewById(R.id.approvedRequestsLabel);
        pendingRequestsLabel = findViewById(R.id.pendingRequestsLabel);
        rejectedRequestsLabel = findViewById(R.id.rejectedRequestsLabel);
        totalWaterUsedLabel = findViewById(R.id.totalWaterUsedLabel);
        totalRevenueLabel = findViewById(R.id.totalRevenueLabel);
        pendingRevenueLabel = findViewById(R.id.pendingRevenueLabel);
        avgRequestVolumeLabel = findViewById(R.id.avgRequestVolumeLabel);
        currentSupplyLabel = findViewById(R.id.currentSupplyLabel);

        // Monthly Statistics
        monthlyRequestsLabel = findViewById(R.id.monthlyRequestsLabel);
        monthlyApprovedLabel = findViewById(R.id.monthlyApprovedLabel);
        monthlyVolumeLabel = findViewById(R.id.monthlyVolumeLabel);
        monthlyRevenueLabel = findViewById(R.id.monthlyRevenueLabel);

        // Top Users
        topUser1Label = findViewById(R.id.topUser1Label);
        topUser2Label = findViewById(R.id.topUser2Label);
        topUser3Label = findViewById(R.id.topUser3Label);

        // Period Label
        currentPeriodLabel = findViewById(R.id.currentPeriodLabel);

        // Buttons
        btnRefresh = findViewById(R.id.btnRefresh);
        btnBack = findViewById(R.id.btnBack);

        // Set current period
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        currentPeriodLabel.setText(monthFormat.format(new Date()));
    }

    private void setupListeners() {
        btnRefresh.setOnClickListener(v -> loadAnalytics());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadAnalytics() {
        loadUserStatistics();
        loadRequestStatistics();
        loadBillingStatistics();
        loadSupplyLevel();
    }

    private void loadUserStatistics() {
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalUsers = 0;
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Users user = userSnapshot.getValue(Users.class);
                    if (user != null && "user".equals(user.getRole())) {
                        totalUsers++;
                    }
                }
                totalUsersLabel.setText(String.valueOf(totalUsers));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                totalUsersLabel.setText("--");
            }
        });
    }

    private void loadRequestStatistics() {
        databaseReference.child("water_requests").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalRequests = 0;
                int approvedRequests = 0;
                int pendingRequests = 0;
                int rejectedRequests = 0;
                double totalWaterUsed = 0;
                double totalVolume = 0;

                // Monthly stats
                int monthlyRequests = 0;
                int monthlyApproved = 0;
                double monthlyVolume = 0;

                // Top users tracking
                Map<String, Double> userVolumeMap = new HashMap<>();

                // Get current month
                Calendar cal = Calendar.getInstance();
                int currentMonth = cal.get(Calendar.MONTH);
                int currentYear = cal.get(Calendar.YEAR);

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    WaterRequest request = requestSnapshot.getValue(WaterRequest.class);
                    if (request != null) {
                        totalRequests++;
                        totalVolume += request.getVolume();

                        // Check request status
                        String status = request.getStatus();
                        if ("Approved".equals(status)) {
                            approvedRequests++;
                            totalWaterUsed += request.getVolume();

                            // Track user volumes
                            String username = request.getUsername();
                            if (username != null) {
                                userVolumeMap.put(username,
                                    userVolumeMap.getOrDefault(username, 0.0) + request.getVolume());
                            }
                        } else if ("Pending".equals(status)) {
                            pendingRequests++;
                        } else if ("Rejected".equals(status)) {
                            rejectedRequests++;
                        }

                        // Check if this month
                        if (request.getTimestamp() > 0) {
                            Calendar requestCal = Calendar.getInstance();
                            requestCal.setTimeInMillis(request.getTimestamp());
                            if (requestCal.get(Calendar.MONTH) == currentMonth &&
                                requestCal.get(Calendar.YEAR) == currentYear) {
                                monthlyRequests++;
                                if ("Approved".equals(status)) {
                                    monthlyApproved++;
                                    monthlyVolume += request.getVolume();
                                }
                            }
                        }
                    }
                }

                // Update UI
                totalRequestsLabel.setText(String.valueOf(totalRequests));
                approvedRequestsLabel.setText(String.valueOf(approvedRequests));
                pendingRequestsLabel.setText(String.valueOf(pendingRequests));
                rejectedRequestsLabel.setText(String.valueOf(rejectedRequests));
                totalWaterUsedLabel.setText(String.format(Locale.getDefault(), "%.1f L", totalWaterUsed));

                // Average volume per request
                double avgVolume = totalRequests > 0 ? totalVolume / totalRequests : 0;
                avgRequestVolumeLabel.setText(String.format(Locale.getDefault(), "%.1f L", avgVolume));

                // Monthly stats
                monthlyRequestsLabel.setText(String.valueOf(monthlyRequests));
                monthlyApprovedLabel.setText(String.valueOf(monthlyApproved));
                monthlyVolumeLabel.setText(String.format(Locale.getDefault(), "%.1f L", monthlyVolume));

                // Find top users
                updateTopUsers(userVolumeMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AnalyticsActivity.this, "Error loading request stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTopUsers(Map<String, Double> userVolumeMap) {
        // Sort users by volume
        List<Map.Entry<String, Double>> sortedUsers = new ArrayList<>(userVolumeMap.entrySet());
        sortedUsers.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Get user full names
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> usernameToFullName = new HashMap<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Users user = userSnapshot.getValue(Users.class);
                    if (user != null) {
                        usernameToFullName.put(user.getUsername(), user.getFullName());
                    }
                }

                // Update top users labels
                if (sortedUsers.size() > 0) {
                    String name = usernameToFullName.getOrDefault(sortedUsers.get(0).getKey(), sortedUsers.get(0).getKey());
                    topUser1Label.setText(String.format(Locale.getDefault(), "🥇 %s - %.1f L", name, sortedUsers.get(0).getValue()));
                } else {
                    topUser1Label.setText("🥇 No data");
                }

                if (sortedUsers.size() > 1) {
                    String name = usernameToFullName.getOrDefault(sortedUsers.get(1).getKey(), sortedUsers.get(1).getKey());
                    topUser2Label.setText(String.format(Locale.getDefault(), "🥈 %s - %.1f L", name, sortedUsers.get(1).getValue()));
                } else {
                    topUser2Label.setText("🥈 No data");
                }

                if (sortedUsers.size() > 2) {
                    String name = usernameToFullName.getOrDefault(sortedUsers.get(2).getKey(), sortedUsers.get(2).getKey());
                    topUser3Label.setText(String.format(Locale.getDefault(), "🥉 %s - %.1f L", name, sortedUsers.get(2).getValue()));
                } else {
                    topUser3Label.setText("🥉 No data");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                topUser1Label.setText("🥇 Error");
                topUser2Label.setText("🥈 Error");
                topUser3Label.setText("🥉 Error");
            }
        });
    }

    private void loadBillingStatistics() {
        databaseReference.child("bills").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalRevenue = 0;
                double pendingRevenue = 0;
                double monthlyRevenue = 0;

                // Get current month
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                String currentPeriod = monthFormat.format(new Date());

                for (DataSnapshot billSnapshot : snapshot.getChildren()) {
                    Bill bill = billSnapshot.getValue(Bill.class);
                    if (bill != null) {
                        if ("Paid".equals(bill.getStatus())) {
                            totalRevenue += bill.getAmount();

                            // Check if current month
                            if (currentPeriod.equals(bill.getBillingPeriod())) {
                                monthlyRevenue += bill.getAmount();
                            }
                        } else {
                            pendingRevenue += bill.getAmount();
                        }
                    }
                }

                totalRevenueLabel.setText(String.format(Locale.getDefault(), "$%.2f", totalRevenue));
                pendingRevenueLabel.setText(String.format(Locale.getDefault(), "$%.2f", pendingRevenue));
                monthlyRevenueLabel.setText(String.format(Locale.getDefault(), "$%.2f", monthlyRevenue));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                totalRevenueLabel.setText("$--");
                pendingRevenueLabel.setText("$--");
                monthlyRevenueLabel.setText("$--");
            }
        });
    }

    private void loadSupplyLevel() {
        databaseReference.child("supply_level").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double level = snapshot.getValue(Double.class);
                    if (level != null) {
                        currentSupplyLabel.setText(String.format(Locale.getDefault(), "%.1f L", level));

                        // Change color based on level
                        if (level < 1000) {
                            currentSupplyLabel.setTextColor(Color.parseColor("#dc3545")); // Red - Low
                        } else if (level < 5000) {
                            currentSupplyLabel.setTextColor(Color.parseColor("#ffc107")); // Yellow - Medium
                        } else {
                            currentSupplyLabel.setTextColor(Color.parseColor("#198754")); // Green - Good
                        }
                    }
                } else {
                    currentSupplyLabel.setText("0.0 L");
                    currentSupplyLabel.setTextColor(Color.parseColor("#dc3545"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                currentSupplyLabel.setText("-- L");
            }
        });
    }
}

