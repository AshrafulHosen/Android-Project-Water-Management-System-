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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText adminUsernameField;
    private EditText adminPasswordField;
    private TextView adminMessageLabel;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WaterManagementPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if admin is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String savedRole = sharedPreferences.getString("role", "");
        if (currentUser != null && "admin".equals(savedRole)) {
            // Admin is already logged in, navigate to dashboard
            navigateToAdminDashboard(sharedPreferences.getString("fullName", "Admin"));
            return;
        }

        adminUsernameField = findViewById(R.id.adminUsernameField);
        adminPasswordField = findViewById(R.id.adminPasswordField);
        adminMessageLabel = findViewById(R.id.adminMessageLabel);
        Button handleAdminLogin = findViewById(R.id.handleAdminLogin);
        Button btnBack = findViewById(R.id.btnBackToMain);

        // Get Firebase Database reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("users");
        databaseReference.keepSynced(true);

        // Create default admin if not exists (run once)
        createDefaultAdmin();

        handleAdminLogin.setOnClickListener(v -> loginAdmin());

        btnBack.setOnClickListener(v -> finish());
    }

    private void createDefaultAdmin() {
        databaseReference.orderByChild("role").equalTo("admin")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            // Create default admin account
                            Users admin = new Users("Ashraful Hosen", "admin", "admin123", "admin");
                            databaseReference.child("admin").setValue(admin)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(AdminLoginActivity.this, "Default admin created", Toast.LENGTH_SHORT).show();
                                        // Also create Firebase Auth account for admin
                                        createAdminAuthAccount("admin", "admin123");
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(AdminLoginActivity.this, "Failed to create admin", Toast.LENGTH_SHORT).show()
                                    );
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error silently
                    }
                });
    }

    private void createAdminAuthAccount(String username, String password) {
        String email = username + "@watermanagement.app";
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // Sign out after creating so admin needs to login
                    mAuth.signOut();
                });
    }

    private void loginAdmin() {
        String username = adminUsernameField.getText().toString().trim();
        String password = adminPasswordField.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            adminMessageLabel.setText("Please enter both username and password.");
            adminMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        // First check Firebase Database to verify admin role
        databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                Users user = userSnapshot.getValue(Users.class);
                                if (user != null && user.getPassword().equals(password)) {
                                    // Check if user has admin role
                                    if (user.isAdmin()) {
                                        // Save admin session
                                        saveAdminSession(user);
                                        // Sign in with Firebase Auth
                                        signInWithFirebaseAuth(username, password, user.getFullName());
                                    } else {
                                        adminMessageLabel.setText("Access denied. This account is not an admin.");
                                        adminMessageLabel.setVisibility(View.VISIBLE);
                                    }
                                    return;
                                }
                            }
                            adminMessageLabel.setText("Invalid password.");
                            adminMessageLabel.setVisibility(View.VISIBLE);
                        } else {
                            adminMessageLabel.setText("Admin not found.");
                            adminMessageLabel.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        adminMessageLabel.setText("Database error: " + error.getMessage());
                        adminMessageLabel.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void signInWithFirebaseAuth(String username, String password, String fullName) {
        String email = username + "@watermanagement.app";
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToAdminDashboard(fullName);
                    } else {
                        // If sign in fails, try to create account first
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, createTask -> {
                                    // Navigate to dashboard regardless (DB auth passed)
                                    navigateToAdminDashboard(fullName);
                                });
                    }
                });
    }

    private void saveAdminSession(Users user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", user.getUsername());
        editor.putString("fullName", user.getFullName());
        editor.putString("role", user.getRole());
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }

    private void navigateToAdminDashboard(String fullName) {
        Toast.makeText(AdminLoginActivity.this, "Welcome Admin: " + fullName, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
        intent.putExtra("fullName", fullName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
