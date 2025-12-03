package com.clublink.club_link;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";
    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    // Views
    private RecyclerView recyclerView;
    private BottomNavigationView bottomNavigationView;

    // Data
    private ChatListAdapter adapter;
    private final ArrayList<ChatPreview> chatPreviews = new ArrayList<>();
    private final HashMap<String, ChatPreview> chatMap = new HashMap<>();
    private final HashMap<String, ValueEventListener> messageListeners = new HashMap<>();

    // Firebase
    private DatabaseReference chatsRef;
    private DatabaseReference messagesRef;
    private ChildEventListener chatsListener;

    // Session
    private SessionManager sessionManager;
    private long currentUserId;
    private String currentUsername;
    private String currentUserKey; // 🔑 identity used in Firebase participants

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // --- 1. One-Time Setup ---
        sessionManager = new SessionManager(this);
        initViews();
        setupRecyclerView();
        setupNavigation();

        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        chatsRef = database.getReference("chats");
        messagesRef = database.getReference("messages_v2");

        // Initial validation when activity is first created
        if (!validateSession()) {
            return; // Stop if session is invalid from the start
        }
    }

    // THIS METHOD IS CALLED WHEN THE ACTIVITY IS RE-LAUNCHED WHILE ALREADY ON TOP
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the activity's intent
        Log.d(TAG, "onNewIntent called. Activity is being reused.");
        // No need to do anything else here, onResume will handle the refresh.
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Reloading session and attaching listeners.");

        // Ensure the correct menu item is checked
        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().findItem(R.id.nav_messages).setChecked(true);
        }

        // --- 2. Refresh Data and Attach Listeners ---
        if (validateSession()) {
            loadChatsForCurrentUser();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Detaching all listeners.");
        // --- 3. Detach Listeners to prevent background work and leaks ---
        detachAllListeners();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rvChatList);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        ImageButton btnNewChat = findViewById(R.id.btnNewChat);
        if (btnNewChat != null) {
            btnNewChat.setOnClickListener(v ->
                    startActivity(new Intent(ChatListActivity.this, UserSearchActivity.class)));
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatListAdapter(chatPreview -> {
            String currentUsernameOnClick = sessionManager.getUsername();
            if (currentUsernameOnClick == null || currentUsernameOnClick.trim().isEmpty()) {
                Toast.makeText(this, "Your session is invalid. Please log in again.", Toast.LENGTH_LONG).show();
                forceLogout();
                return;
            }

            Intent intent = new Intent(ChatListActivity.this, DashboardActivity.class);
            intent.putExtra("CHAT_ID", chatPreview.getChatId());
            intent.putExtra("CHAT_NAME", chatPreview.getChatName());
            intent.putExtra("CURRENT_USERNAME", currentUsernameOnClick);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupNavigation() {
        if (bottomNavigationView != null) {
            // Set the listener
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                // If the selected item is the current item, do nothing but consume the event.
                if (itemId == R.id.nav_messages) {
                    return true; // Consume the event, VERY IMPORTANT
                }

                // Otherwise, prepare to launch the new activity
                Intent intent = null;
                if (itemId == R.id.nav_home) {
                    intent = new Intent(this, MainActivity.class);
                } else if (itemId == R.id.nav_events) {
                    intent = new Intent(this, EventsActivity.class);
                } else if (itemId == R.id.nav_announcements) {
                    intent = new Intent(this, AnnouncementsActivity.class);
                } else if (itemId == R.id.nav_profile) {
                    intent = new Intent(this, ProfileActivity.class);
                }

                // If an intent was created, launch it
                if (intent != null) {
                    // This flag works with launchMode="singleTop" to reuse existing activities.
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true; // Consume the event
                }

                // If the item ID was not recognized, do not consume the event
                return false;
            });
        }
    }

    private boolean validateSession() {
        currentUserId = sessionManager.getUserId();
        currentUsername = sessionManager.getUsername();

        Log.d(TAG, "Validating session - UserID: " + currentUserId + ", Username: '" + currentUsername + "'");

        if (currentUserId == -1) {
            Log.e(TAG, "Session validation failed: No user ID.");
            forceLogout();
            return false;
        }

        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            Log.e(TAG, "Session validation failed: Username is missing.");
            Toast.makeText(this, "Your session data is incomplete. Please log in again.", Toast.LENGTH_LONG).show();
            forceLogout();
            return false;
        }

        // Use consistent key format: lowercase username
        currentUserKey = currentUsername.toLowerCase(Locale.ROOT);
        Log.d(TAG, "Session validated successfully. UserKey = " + currentUserKey);
        return true;
    }

    private void forceLogout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadChatsForCurrentUser() {
        if (currentUserKey == null || currentUserKey.isEmpty()) {
            Log.e(TAG, "Cannot load chats, currentUserKey is null or empty.");
            return;
        }
        clearChatData();

        String childPath = "participants/" + currentUserKey;
        Log.d(TAG, "Attaching chat listener for path: " + childPath);

        if (chatsListener != null) {
            Log.w(TAG, "chatsListener not null before attaching, this should not happen if onPause is working.");
            detachAllListeners();
        }

        chatsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                buildChatPreviewForSnapshot(snapshot);
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                buildChatPreviewForSnapshot(snapshot);
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                removeChatFromList(snapshot.getKey());
            }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase chat listener cancelled: " + error.getMessage(), error.toException());
            }
        };
        chatsRef.orderByChild(childPath).equalTo(true).addChildEventListener(chatsListener);
    }

    private void buildChatPreviewForSnapshot(@NonNull DataSnapshot snapshot) {
        String chatId = snapshot.getKey();
        if (chatId == null) return;

        // ✅ SECURITY: Verify current user is actually a participant
        DataSnapshot participantsSnapshot = snapshot.child("participants");
        if (!participantsSnapshot.hasChild(currentUserKey)) {
            Log.w(TAG, "Security: User " + currentUserKey + " is not a participant in chat " + chatId);
            return;
        }

        // ✅ FILTER: Check if this is a self-chat and skip it
        int participantCount = 0;
        boolean hasOtherUser = false;

        for (DataSnapshot participantSnap : participantsSnapshot.getChildren()) {
            participantCount++;
            String participantKey = participantSnap.getKey();
            if (participantKey != null && !participantKey.equals(currentUserKey)) {
                hasOtherUser = true;
            }
        }

        // Skip if this is a self-chat (only 1 participant or all participants are the same user)
        if (participantCount == 1 || !hasOtherUser) {
            Log.d(TAG, "Skipping self-chat: " + chatId);
            return;
        }

        String displayName = snapshot.child("name").getValue(String.class);
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        long chatCreatedAt = (createdAt != null) ? createdAt : 0L;

        loadLastMessage(chatId, (displayName != null ? displayName : "Chat"), chatCreatedAt);
    }

    private void loadLastMessage(String chatId, String displayName, long chatCreatedAt) {
        if (messageListeners.containsKey(chatId)) return;

        ValueEventListener listener = new ValueEventListener() {
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
                updateChatPreview(chatId, displayName, lastMessageText, timestamp);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading last message for " + chatId, error.toException());
            }
        };
        messageListeners.put(chatId, listener);
        messagesRef.orderByChild("chatId").equalTo(chatId).limitToLast(1).addValueEventListener(listener);
    }

    private void updateChatPreview(String chatId, String name, String lastMsg, long timestamp) {
        ChatPreview preview = chatMap.get(chatId);
        if (preview == null) {
            preview = new ChatPreview(chatId, name, lastMsg, timestamp, 0);
            chatMap.put(chatId, preview);
            chatPreviews.add(preview);
        } else {
            preview.setChatName(name);
            preview.setLastMessage(lastMsg);
            preview.setTimestamp(timestamp);
        }
        chatPreviews.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        adapter.submitList(new ArrayList<>(chatPreviews));
    }

    private void removeChatFromList(String chatId) {
        if (chatId == null) return;
        ChatPreview preview = chatMap.remove(chatId);
        if (preview != null) {
            chatPreviews.remove(preview);
            adapter.submitList(new ArrayList<>(chatPreviews));
        }
    }

    private void clearChatData() {
        if (adapter != null) {
            chatPreviews.clear();
            chatMap.clear();
            adapter.submitList(null);
        }
    }

    private void detachAllListeners() {
        if (chatsRef != null && chatsListener != null && currentUserKey != null && !currentUserKey.isEmpty()) {
            String childPath = "participants/" + currentUserKey;
            chatsRef.orderByChild(childPath).equalTo(true).removeEventListener(chatsListener);
            chatsListener = null;
            Log.d(TAG, "Detached main chat listener.");
        }

        if (messagesRef != null && !messageListeners.isEmpty()) {
            for (java.util.Map.Entry<String, ValueEventListener> entry : messageListeners.entrySet()) {
                messagesRef.orderByChild("chatId").equalTo(entry.getKey()).limitToLast(1).removeEventListener(entry.getValue());
            }
            messageListeners.clear();
            Log.d(TAG, "Detached all message listeners.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachAllListeners();
    }
}