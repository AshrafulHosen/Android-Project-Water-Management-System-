package com.example.watermanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class UserLoginActivity extends AppCompatActivity {

    private EditText usernameField;
    private EditText passwordField;
    private TextView messageLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        usernameField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
        messageLabel = findViewById(R.id.messageLabel);
        Button btnUserLoginSubmit = findViewById(R.id.btnUserLoginSubmit);
        Button btnBack = findViewById(R.id.btnBackToMain);

        btnUserLoginSubmit.setOnClickListener(v -> {
            String username = usernameField.getText().toString();
            String password = passwordField.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Please enter both username and password.");
                messageLabel.setVisibility(View.VISIBLE);
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }
}
