package com.clublink.club_link;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

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
import java.util.Locale;

/**
 * ChatListActivity - Shows list of all chat conversations
 * Only shows chats where the logged-in user is a participant.
 re
 * Identity for chats:
 *  - Prefer username.toLowerCase()
 *  - If username missing, fall back to "uid_<numericId>"
 */
public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";
    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;

    private final ArrayList<ChatPreview> chatPreviews = new ArrayList<>();
    private final HashMap<String, ChatPreview> chatMap = new HashMap<>();

    private final HashMap<String, com.google.firebase.database.ValueEventListener> messageListeners = new HashMap<>();

    private DatabaseReference chatsRef;
    private DatabaseReference messagesRef;
    private ChildEventListener chatsListener;
    private SessionManager sessionManager;
    private long currentUserId;
    private String currentUsername;
    private String currentUserKey; // 🔑 identity used in Firebase participants

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        recyclerView = findViewById(R.id.rvChatList);
        ImageButton btnNewChat = findViewById(R.id.btnNewChat);

        // ---------- Session ----------
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        currentUsername = sessionManager.getUsername();

        if (currentUserId == -1) {
            Log.e(TAG, "No logged-in user id, redirecting to Login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Build a stable identity key used in Firebase:
        //  - prefer username.toLowerCase()
        //  - else fall back to "uid_<id>"
        if (currentUsername != null && !currentUsername.trim().isEmpty()) {
            currentUserKey = currentUsername.toLowerCase(Locale.ROOT);
        } else {
            currentUserKey = "uid_" + currentUserId;
        }

        Log.d(TAG, "ChatList for userKey = " + currentUserKey + " (username=" + currentUsername + ")");

        // ---------- Bottom Navigation ----------
        // In ChatListActivity.java -> onCreate()

// ---------- Bottom Navigation ----------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            // Set the "Messages" icon as active without triggering the listener
            bottomNavigationView.getMenu().findItem(R.id.nav_messages).setChecked(true);

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(ChatListActivity.this, MainActivity.class));

                    return true;
                } else if (itemId == R.id.nav_events) {
                    startActivity(new Intent(ChatListActivity.this, EventsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    // User is clicking the icon for the screen they are already on. Do nothing.
                    return true;
                } else if (itemId == R.id.nav_announcements) {
                    startActivity(new Intent(ChatListActivity.this, AnnouncementsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(ChatListActivity.this, ProfileActivity.class));
                    return true;
                }

                return false;
            });
        }


        // ---------- RecyclerView + Adapter ----------
        adapter = new ChatListAdapter(chatPreview -> {
            Intent intent = new Intent(ChatListActivity.this, DashboardActivity.class);
            intent.putExtra("CHAT_ID", chatPreview.getChatId());
            intent.putExtra("CHAT_NAME", chatPreview.getChatName());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // ---------- Firebase refs ----------
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        chatsRef = database.getReference("chats");
        messagesRef = database.getReference("messages_v2");

        // Load only chats where this user is a participant (by userKey)
        loadChatsForCurrentUser();

        // New chat button → user search
        if (btnNewChat != null) {
            btnNewChat.setOnClickListener(v -> {
                Intent i = new Intent(ChatListActivity.this, UserSearchActivity.class);
                startActivity(i);
            });
        }
    }

    /**
     * Load only chats where participants/<currentUserKey> == true.
     */
    private void loadChatsForCurrentUser() {
        String childPath = "participants/" + currentUserKey;
        Log.d(TAG, "Querying chats where " + childPath + " = true");
        // First, define the listener
        chatsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded: chat " + snapshot.getKey());
                buildChatPreviewForSnapshot(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged: chat " + snapshot.getKey());
                buildChatPreviewForSnapshot(snapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String chatId = snapshot.getKey();
                Log.d(TAG, "onChildRemoved: chat " + chatId);
                removeChatFromList(chatId);
            }

            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage(), error.toException());
            }
        };

        // Now, attach the listener using the variable
        chatsRef.orderByChild(childPath)
                .equalTo(true)
                .addChildEventListener(chatsListener);

        // --- END OF CHANGES ---
    }


    /**
     * Builds or updates a ChatPreview for this snapshot.
     * Title = chat.name (set when DM was created).
     */
    private void buildChatPreviewForSnapshot(@NonNull DataSnapshot snapshot) {
        String chatId = snapshot.getKey();
        if (chatId == null) return;

        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        long chatCreatedAt = createdAt != null ? createdAt : 0L;

        String displayName = snapshot.child("name").getValue(String.class);
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "Chat";
        }

        loadLastMessage(chatId, displayName, chatCreatedAt);
    }

    /**
     * Load the last message for a given chat and update ChatPreview list.
     */

    private void loadLastMessage(String chatId, String displayName, long chatCreatedAt) {
        // If a listener for this chat is already active, don't create another one.
        if (messageListeners.containsKey(chatId)) {
            return;
        }

        com.google.firebase.database.ValueEventListener listener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastMessageText = "No messages yet";
                long timestamp = chatCreatedAt;

                if (snapshot.exists()) {
                    for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                        MessageModal message = msgSnapshot.getValue(MessageModal.class);
                        if (message != null) {
                            lastMessageText = message.getSenderName() + ": " + message.getText();
                            timestamp = message.getCreatedAt();
                        }
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

                // Sort newest-first every time there's an update
                chatPreviews.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.submitList(new ArrayList<>(chatPreviews));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading last message for " + chatId, error.toException());
            }
        };

        // Store the listener so we can remove it later
        messageListeners.put(chatId, listener);

        // Attach the PERSISTENT listener using addValueEventListener
        messagesRef.orderByChild("chatId")
                .equalTo(chatId)
                .limitToLast(1)
                .addValueEventListener(listener);
    }

    private void removeChatFromList(String chatId) {
        if (chatId == null) return;

        // Clean up the message listener to prevent memory leaks
        if (messageListeners.containsKey(chatId)) {
            com.google.firebase.database.ValueEventListener listener = messageListeners.remove(chatId);
            if (listener != null) {
                // We must use the exact same query to remove the listener
                messagesRef.orderByChild("chatId").equalTo(chatId).limitToLast(1).removeEventListener(listener);
            }
        }

        ChatPreview preview = chatMap.remove(chatId);
        if (preview != null) {
            chatPreviews.remove(preview);
            adapter.submitList(new ArrayList<>(chatPreviews));
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }

        // Clean up all Firebase Listeners to prevent memory leaks and crashes
        if (messagesRef != null) {
            for (java.util.Map.Entry<String, com.google.firebase.database.ValueEventListener> entry : messageListeners.entrySet()) {
                messagesRef.orderByChild("chatId").equalTo(entry.getKey()).limitToLast(1).removeEventListener(entry.getValue());
            }
            messageListeners.clear();
        }

    }

}
