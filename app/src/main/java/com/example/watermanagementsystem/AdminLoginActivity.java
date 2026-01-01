package com.example.watermanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText adminUsernameField;
    private EditText adminPasswordField;
    private TextView adminMessageLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        adminUsernameField = findViewById(R.id.adminUsernameField);
        adminPasswordField = findViewById(R.id.adminPasswordField);
        adminMessageLabel = findViewById(R.id.adminMessageLabel);
        Button handleAdminLogin = findViewById(R.id.handleAdminLogin);
        Button btnBack = findViewById(R.id.btnBackToMain);

        handleAdminLogin.setOnClickListener(v -> {
            String username = adminUsernameField.getText().toString();
            String password = adminPasswordField.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                adminMessageLabel.setText("Please enter both username and password.");
                adminMessageLabel.setVisibility(View.VISIBLE);
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }
}
