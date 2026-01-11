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
import com.google.firebase.database.*;

public class UserLoginActivity extends AppCompatActivity {

    private EditText usernameField, passwordField;
    private TextView messageLabel;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WaterManagementPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, navigate to dashboard
            navigateToDashboard(currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : currentUser.getEmail());
            return;
        }

        usernameField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
        messageLabel = findViewById(R.id.messageLabel);
        Button btnLogin = findViewById(R.id.btnUserLoginSubmit);
        Button btnBack = findViewById(R.id.btnBackToMain);

        // Enable offline persistence
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        try {
            database.setPersistenceEnabled(true);
        } catch (Exception e) {
            // Persistence already enabled
        }

        databaseReference = database.getReference("users");
        databaseReference.keepSynced(true);

        btnLogin.setOnClickListener(v -> loginUser());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loginUser() {
        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both fields.");
            messageLabel.setVisibility(View.VISIBLE);
            return;
        }

        // First check Firebase Database for user info
        databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                Users user = userSnapshot.getValue(Users.class);
                                if (user != null && user.getPassword().equals(password) && "user".equals(user.getRole())) {
                                    // Save user info to SharedPreferences for session
                                    saveUserSession(user);

                                    // Sign in with Firebase Auth (create email from username)
                                    String email = username + "@watermanagement.app";
                                    signInWithFirebaseAuth(email, password, user.getFullName());
                                    return;
                                }
                            }
                            messageLabel.setText("Invalid password.");
                            messageLabel.setVisibility(View.VISIBLE);
                        } else {
                            messageLabel.setText("User not found.");
                            messageLabel.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        messageLabel.setText("Error: " + error.getMessage());
                        messageLabel.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void signInWithFirebaseAuth(String email, String password, String fullName) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        navigateToDashboard(fullName);
                    } else {
                        // If sign in fails, try to create account first
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, createTask -> {
                                    if (createTask.isSuccessful()) {
                                        navigateToDashboard(fullName);
                                    } else {
                                        // Still navigate to dashboard since database auth passed
                                        navigateToDashboard(fullName);
                                    }
                                });
                    }
                });
    }

    private void saveUserSession(Users user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", user.getUsername());
        editor.putString("fullName", user.getFullName());
        editor.putString("role", user.getRole());
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }

    private void navigateToDashboard(String fullName) {
        String username = sharedPreferences.getString("username", "");
        Toast.makeText(UserLoginActivity.this, "Welcome " + fullName, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(UserLoginActivity.this, UserDashboardActivity.class);
        intent.putExtra("fullName", fullName);
        intent.putExtra("username", username);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
