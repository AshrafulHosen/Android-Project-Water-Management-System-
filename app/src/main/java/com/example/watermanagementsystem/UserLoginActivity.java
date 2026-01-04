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

public class UserLoginActivity extends AppCompatActivity {

    private EditText usernameField, passwordField;
    private TextView messageLabel;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        usernameField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
        messageLabel = findViewById(R.id.messageLabel);
        Button btnLogin = findViewById(R.id.btnUserLoginSubmit);
        Button btnBack = findViewById(R.id.btnBackToMain);

        // Enable offline persistence
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);

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

        databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                Users user = userSnapshot.getValue(Users.class);
                                if (user != null && user.getPassword().equals(password) && "user".equals(user.getRole())) {
                                    Toast.makeText(UserLoginActivity.this, "Welcome " + user.getFullName(), Toast.LENGTH_SHORT).show();
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
}
