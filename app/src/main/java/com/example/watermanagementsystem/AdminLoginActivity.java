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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        adminUsernameField = findViewById(R.id.adminUsernameField);
        adminPasswordField = findViewById(R.id.adminPasswordField);
        adminMessageLabel = findViewById(R.id.adminMessageLabel);
        Button handleAdminLogin = findViewById(R.id.handleAdminLogin);
        Button btnBack = findViewById(R.id.btnBackToMain);

        // Enable offline persistence
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);

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
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(AdminLoginActivity.this, "Default admin created", Toast.LENGTH_SHORT).show()
                                    )
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

    private void loginAdmin() {
        String username = adminUsernameField.getText().toString().trim();
        String password = adminPasswordField.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            adminMessageLabel.setText("Please enter both username and password.");
            adminMessageLabel.setVisibility(View.VISIBLE);
            return;
        }

        databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                Users user = userSnapshot.getValue(Users.class);
                                if (user != null && user.getPassword().equals(password)
                                        && user.isAdmin()) {
                                    Toast.makeText(AdminLoginActivity.this,
                                            "Admin login successful!", Toast.LENGTH_SHORT).show();
                                    // Navigate to admin dashboard
                                    // Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                                    // startActivity(intent);
                                    return;
                                }
                            }
                            adminMessageLabel.setText("Invalid password or not an admin.");
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
}
