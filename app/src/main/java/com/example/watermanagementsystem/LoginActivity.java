package com.example.watermanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button btnUserLogin = findViewById(R.id.btnUserLogin);
        Button btnAdminLogin = findViewById(R.id.btnAdminLogin);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnUserLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, UserLoginActivity.class);
            startActivity(intent);
        });

        btnAdminLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });

        btnCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }
}
