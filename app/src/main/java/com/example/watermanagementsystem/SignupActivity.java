package com.example.watermanagementsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        EditText username = findViewById(R.id.usernameField);
        EditText password = findViewById(R.id.passwordField);
        TextView messageLabel = findViewById(R.id.messageLabel);
        Button btnCreateAccount = findViewById(R.id.btnUserLoginSubmit);
        Button btnBackToLogin = findViewById(R.id.btnBackToMain);

        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = username.getText().toString();
                String pass = password.getText().toString();

                if (user.isEmpty()) {
                    messageLabel.setVisibility(View.VISIBLE);
                    messageLabel.setText("Please enter a username");
                }
                else if (pass.isEmpty()) {
                    messageLabel.setVisibility(View.VISIBLE);
                    messageLabel.setText("Please enter a password");
                }
                else {
                    Toast.makeText(SignupActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}