package com.example.club_link;


import com.google.firebase.database.Exclude;


import java.util.HashMap;
import java.util.Map;

public class AnnouncementModal {
    private String id;
    private String title;
    private String message;
    private String authorName;
    private long createdAt;
    private boolean isRead;

    // Default constructor required for Firebase
    public AnnouncementModal() { }

    // Full constructor
    public AnnouncementModal(String id, String title, String message, String authorName, long createdAt, boolean isRead) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.isRead = isRead;
    }

    // Getters (required for Firebase)
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getAuthorName() {
        return authorName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    // Setters (required for Firebase)
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    // Convert to Map for Firebase (optional, but useful)
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("message", message);
        result.put("authorName", authorName);
        result.put("createdAt", createdAt);
        result.put("isRead", isRead);
        return result;
    }
}