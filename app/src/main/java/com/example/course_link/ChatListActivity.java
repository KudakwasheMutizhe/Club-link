package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * ChatListActivity - Shows list of all chat conversations
 * Only shows chats where the logged-in user is a participant,
 * and the title is the OTHER user's name.
 */
public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";
    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private final ArrayList<ChatPreview> chatPreviews = new ArrayList<>();
    private final HashMap<String, ChatPreview> chatMap = new HashMap<>();

    private DatabaseReference chatsRef;
    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;    

    // Session / user info
    private SessionManager sessionManager;
    private long currentUserId;
    private String currentUserIdStr;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        Log.d(TAG, "onCreate: ChatListActivity created");

        recyclerView = findViewById(R.id.rvChatList);
        ImageButton btnNewChat = findViewById(R.id.btnNewChat);

        // ---------- Session / SQLite user ----------
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        currentUserIdStr = String.valueOf(currentUserId);
        currentUsername = sessionManager.getUsername();

        if (currentUserId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "Logged in as userId = " + currentUserIdStr + ", username = " + currentUsername);

        // ---------- Bottom Navigation ----------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            final boolean[] isInitialSelection = {true};

            bottomNavigationView.setOnItemSelectedListener(item -> {
                if (isInitialSelection[0]) {
                    isInitialSelection[0] = false;
                    return true;
                }

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(ChatListActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_events) {
                    startActivity(new Intent(ChatListActivity.this, EventsActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    // already here
                    return true;
                } else if (itemId == R.id.nav_announcements) {
                    startActivity(new Intent(ChatListActivity.this, AnnouncementsActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(ChatListActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }

                return false;
            });

            bottomNavigationView.setSelectedItemId(R.id.nav_messages);
        }

        // ✅ Adapter: opens DashboardActivity for that chat
        adapter = new ChatListAdapter(chatPreview -> {
            Intent intent = new Intent(ChatListActivity.this, DashboardActivity.class);
            intent.putExtra("CHAT_ID", chatPreview.getChatId());
            intent.putExtra("CHAT_NAME", chatPreview.getChatName());  // this is the OTHER user's name
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // ---------- Firebase refs ----------
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        chatsRef = database.getReference("chats");
        messagesRef = database.getReference("messages_v2");
        usersRef = database.getReference("users");

        // 🔹 Only load chats where this user is a participant
        loadChatsForCurrentUser();

        // 🔹 NEW: + button opens user search (no more free-text chats)
        if (btnNewChat != null) {
            btnNewChat.setOnClickListener(v -> {
                Intent i = new Intent(ChatListActivity.this, UserSearchActivity.class);
                startActivity(i);
            });
        } else {
            Log.e(TAG, "btnNewChat is NULL! Check your layout file.");
        }
    }

    /**
     * Load only chats where participants/<currentUserId> == true.
     */
    private void loadChatsForCurrentUser() {
        String childPath = "participants/" + currentUserIdStr;

        chatsRef.orderByChild(childPath)
                .equalTo(true)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                        buildChatPreviewForSnapshot(snapshot);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                        // If chat name or participants changed, rebuild preview
                        buildChatPreviewForSnapshot(snapshot);
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                        String chatId = snapshot.getKey();
                        if (chatId != null) {
                            ChatPreview preview = chatMap.remove(chatId);
                            if (preview != null) {
                                chatPreviews.remove(preview);
                                adapter.submitList(new ArrayList<>(chatPreviews));
                            }
                        }
                    }

                    @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase error: " + error.getMessage());
                    }
                });
    }

    /**
     * Builds or updates a ChatPreview for this snapshot.
     * Title = OTHER user's username/fullname, not mine.
     */
    private void buildChatPreviewForSnapshot(@NonNull DataSnapshot snapshot) {
        String chatId = snapshot.getKey();
        if (chatId == null) return;

        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        long chatCreatedAt = createdAt != null ? createdAt : 0L;

        // 🔹 Find the "other" participant
        DataSnapshot participantsSnap = snapshot.child("participants");
        String otherUserIdStr = null;

        for (DataSnapshot p : participantsSnap.getChildren()) {
            String uid = p.getKey();
            if (uid != null && !uid.equals(currentUserIdStr)) {
                otherUserIdStr = uid;
                break;
            }
        }

        // If no other user (e.g. old self-chat or group chat), fall back to stored name
        if (otherUserIdStr == null) {
            String fallbackName = snapshot.child("name").getValue(String.class);
            if (fallbackName == null || fallbackName.trim().isEmpty()) {
                fallbackName = "Chat";
            }
            loadLastMessage(chatId, fallbackName, chatCreatedAt);
            return;
        }

        // 🔹 Look up the OTHER user's profile in /users
        String finalOtherUserIdStr = otherUserIdStr;
        usersRef.child(otherUserIdStr)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                        AppUserFirebase otherUser = userSnap.getValue(AppUserFirebase.class);
                        String displayName;

                        if (otherUser != null) {
                            if (otherUser.username != null && !otherUser.username.trim().isEmpty()) {
                                displayName = otherUser.username;
                            } else if (otherUser.fullname != null && !otherUser.fullname.trim().isEmpty()) {
                                displayName = otherUser.fullname;
                            } else {
                                displayName = "User " + finalOtherUserIdStr;
                            }
                        } else {
                            displayName = "User " + finalOtherUserIdStr;
                        }

                        // Now load last message and update preview with displayName
                        loadLastMessage(chatId, displayName, chatCreatedAt);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load user " + finalOtherUserIdStr, error.toException());
                        // Fallback to generic name
                        loadLastMessage(chatId, "User " + finalOtherUserIdStr, chatCreatedAt);
                    }
                });
    }

    /**
     * Load the last message for a given chat and update ChatPreview list.
     */
    private void loadLastMessage(String chatId, String displayName, long chatCreatedAt) {
        messagesRef.orderByChild("chatId")
                .equalTo(chatId)
                .limitToLast(1)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String lastMessageText = "No messages yet";
                        long timestamp = chatCreatedAt;

                        for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                            MessageModal message = msgSnapshot.getValue(MessageModal.class);
                            if (message != null) {
                                lastMessageText = message.getSenderName() + ": " + message.getText();
                                timestamp = message.getCreatedAt();
                            }
                        }

                        ChatPreview preview = chatMap.get(chatId);
                        if (preview == null) {
                            preview = new ChatPreview(chatId, displayName, lastMessageText, timestamp, 0);
                            chatMap.put(chatId, preview);
                            chatPreviews.add(preview);
                        } else {
                            preview.setChatName(displayName);
                            preview.setLastMessage(lastMessageText);
                            preview.setTimestamp(timestamp);
                        }

                        // Sort by latest activity
                        chatPreviews.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        adapter.submitList(new ArrayList<>(chatPreviews));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading last message", error.toException());
                    }
                });
    }
}
