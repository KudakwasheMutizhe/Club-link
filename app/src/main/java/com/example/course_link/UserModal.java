package com.example.club_link;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class UserModal {
    private String userId;
    private String email;
    private String displayName;
    private String bio;
    private String campus;
    private String profilePicUrl;
    private long createdAt;
    private long updatedAt;

    // Default constructor required for Firebase
    public UserModal() { }

    // Full constructor
    public UserModal(String userId, String email, String displayName, String bio,
                     String campus, String profilePicUrl, long createdAt, long updatedAt) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.bio = bio;
        this.campus = campus;
        this.profilePicUrl = profilePicUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }

    public String getCampus() {
        return campus;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Convert to Map for Firebase
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("email", email);
        result.put("displayName", displayName);
        result.put("bio", bio);
        result.put("campus", campus);
        result.put("profilePicUrl", profilePicUrl);
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        return result;
    }
}