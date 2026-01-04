package com.example.watermanagementsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

public class SignupActivity extends AppCompatActivity {

    private EditText fullNameField, usernameField, passwordField;
    private TextView messageLabel;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        fullNameField = findViewById(R.id.fullNameField);
        usernameField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
        messageLabel = findViewById(R.id.messageLabel);
        Button btnSubmit = findViewById(R.id.btnUserLoginSubmit);
        Button btnBack = findViewById(R.id.btnBackToMain);

        // Enable offline persistence (call once in app)
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);

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

        databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            messageLabel.setText("Username already exists.");
                            messageLabel.setVisibility(View.VISIBLE);
                        } else {
                            saveUserToDatabase(fullName, username, password);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        messageLabel.setText("Error: " + error.getMessage());
                        messageLabel.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void saveUserToDatabase(String fullName, String username, String password) {
        Users user = new Users(fullName, username, password, "user");
        databaseReference.child(username).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    clearFields();
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