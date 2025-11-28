package com.clublink.club_link;

/**
 * Model class for chat preview in the chat list
 */
public class ChatPreview {
    private String chatId;
    private String chatName;
    private String lastMessage;
    private long timestamp;
    private int unreadCount;

    public ChatPreview() {
        // Required for Firebase
    }

    public ChatPreview(String chatId, String chatName, String lastMessage,
                       long timestamp, int unreadCount) {
        this.chatId = chatId;
        this.chatName = chatName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
