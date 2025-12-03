package com.clublink.club_link;

import java.util.Objects;

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

    // --- START: ADDED METHODS FOR DiffUtil ---

    /**
     * This method is used by the ListAdapter's DiffUtil to determine
     * if the contents of two ChatPreview objects are the same.
     * If they are, the RecyclerView will not redraw the item.
     *
     * @param o The object to compare this instance with.
     * @return true if the objects represent the same data, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        // 1. Check if it's the exact same object in memory
        if (this == o) return true;
        // 2. Check if the other object is null or of a different class
        if (o == null || getClass() != o.getClass()) return false;
        // 3. Cast the object and compare all the fields
        ChatPreview that = (ChatPreview) o;
        return timestamp == that.timestamp &&
                unreadCount == that.unreadCount &&
                Objects.equals(chatId, that.chatId) &&
                Objects.equals(chatName, that.chatName) &&
                Objects.equals(lastMessage, that.lastMessage);
    }

    /**
     * This method is required whenever you override equals().
     * It generates a hash code based on the object's fields.
     *
     * @return A hash code for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(chatId, chatName, lastMessage, timestamp, unreadCount);
    }

    // --- END: ADDED METHODS FOR DiffUtil ---
}
