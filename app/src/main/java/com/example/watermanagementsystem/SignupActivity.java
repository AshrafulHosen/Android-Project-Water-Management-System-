package com.example.watermanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.*;

public class SignupActivity extends AppCompatActivity {

    private EditText fullNameField, usernameField, passwordField;
    private TextView messageLabel;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        fullNameField = findViewById(R.id.fullNameField);
        usernameField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
        messageLabel = findViewById(R.id.messageLabel);
        Button btnSubmit = findViewById(R.id.btnUserLoginSubmit);
        Button btnBack = findViewById(R.id.btnBackToMain);

        // Enable offline persistence (call once in app)
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        try {
            database.setPersistenceEnabled(true);
        } catch (Exception e) {
            // Persistence already enabled
        }

        databaseReference = database.getReference("users");
        databaseReference.keepSynced(true);

        btnSubmit.setOnClickListener(v -> registerUser());
        btnBack.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String fullName = fullNameField.getText().toString().trim();
        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            messageLabel.setVisibility(View.VISIBLE);
            return;
        }

        if (password.length() < 6) {
            messageLabel.setText("Password must be at least 6 characters.");
            messageLabel.setVisibility(View.VISIBLE);
            return;
        }

        databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            messageLabel.setText("Username already exists.");
                            messageLabel.setVisibility(View.VISIBLE);
                        } else {
                            // Create Firebase Auth account and save to database
                            createFirebaseAuthAccount(fullName, username, password);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        messageLabel.setText("Error: " + error.getMessage());
                        messageLabel.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void createFirebaseAuthAccount(String fullName, String username, String password) {
        // Create email from username for Firebase Auth
        String email = username + "@watermanagement.app";

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Update user profile with display name
                        if (mAuth.getCurrentUser() != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            mAuth.getCurrentUser().updateProfile(profileUpdates);
                        }
                        // Save user to database
                        saveUserToDatabase(fullName, username, password);
                    } else {
                        // Even if Firebase Auth fails, try to save to database
                        // (might fail due to network issues, but DB auth will still work)
                        saveUserToDatabase(fullName, username, password);
                    }
                });
    }

    private void saveUserToDatabase(String fullName, String username, String password) {
        Users user = new Users(fullName, username, password, "user");
        databaseReference.child(username).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show();
                    clearFields();
                    // Sign out so user has to login
                    mAuth.signOut();
                    // Navigate to user login
                    Intent intent = new Intent(SignupActivity.this, UserLoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    messageLabel.setText("Registration failed: " + e.getMessage());
                    messageLabel.setVisibility(View.VISIBLE);
                });
    }

    private void clearFields() {
        fullNameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
    }
}