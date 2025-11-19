package com.example.course_link;

/**
 * Message model for Firebase
 * Now includes chatId to support multiple chats
 */
public class MessageModal {
    private String id;
    private String text;
    private String senderId;
    private String senderName;
    private long createdAt;
    private String chatId;  // NEW: which chat this message belongs to

    // Required empty constructor for Firebase
    public MessageModal() {
    }

    public MessageModal(String id, String text, String senderId, String senderName, long createdAt, String chatId) {
        this.id = id;
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.createdAt = createdAt;
        this.chatId = chatId;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
}