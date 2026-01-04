package com.example.watermanagementsystem;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class Users {
    @PropertyName("fullName")
    private String fullName;

    @PropertyName("username")
    private String username;

    @PropertyName("password")
    private String password;

    @PropertyName("role")
    private String role;

    // Default constructor required for Firebase
    public Users() {
    }

    public Users(String fullName, String username, String password, String role) {
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Exclude
    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
