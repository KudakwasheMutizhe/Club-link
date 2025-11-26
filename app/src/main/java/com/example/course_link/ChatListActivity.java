package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

/**
 * ChatListActivity - Shows list of chat conversations.
 * Only shows chats where the logged-in user is a participant.
 *
 * Rules:
 *  - When Account A logs in, they only see chats where participants/<A_id> == true.
 *  - A does NOT see B's chats with C/D unless A is also in participants.
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

    // Session / user info
    private SessionManager sessionManager;
    private long currentUserId;
    private String currentUserIdStr;

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

        if (currentUserId == -1) {
            // No logged-in user → send to Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUserIdStr = String.valueOf(currentUserId);
        Log.d(TAG, "Logged in as userId = " + currentUserIdStr);

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
                    startActivity(new Intent(ChatListActivity.this, profile.class));
                    finish();
                    return true;
                }

                return false;
            });

            bottomNavigationView.setSelectedItemId(R.id.nav_messages);
        }

        // ---------- Adapter + RecyclerView ----------
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

        // Listen for chats where THIS user is a participant
        loadChatsForCurrentUser();

        // Create new chat button
        if (btnNewChat != null) {
            btnNewChat.setOnClickListener(v -> {
                Log.d(TAG, "New Chat button clicked!");
                showCreateChatDialog();
            });
        } else {
            Log.e(TAG, "btnNewChat is NULL! Check your layout file.");
        }
    }

    // Optional XML onClick hook if used in layout
    public void onNewChatClick(android.view.View view) {
        Log.d(TAG, "XML onClick fired!");
        Toast.makeText(ChatListActivity.this, "XML onClick fired", Toast.LENGTH_SHORT).show();
        showCreateChatDialog();
    }

    /**
     * Load only chats where participants/<currentUserId> == true.
     * So Account A only sees chats they are actually part of.
     */
    private void loadChatsForCurrentUser() {
        chatPreviews.clear();
        chatMap.clear();

        String childPath = "participants/" + currentUserIdStr;

        chatsRef.orderByChild(childPath)
                .equalTo(true)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                        String chatId = snapshot.getKey();
                        String chatName = snapshot.child("name").getValue(String.class);
                        Long createdAt = snapshot.child("createdAt").getValue(Long.class);

                        if (chatId != null && chatName != null) {
                            loadLastMessage(chatId, chatName, createdAt != null ? createdAt : 0);
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                        String chatId = snapshot.getKey();
                        String chatName = snapshot.child("name").getValue(String.class);

                        if (chatId != null && chatName != null) {
                            ChatPreview preview = chatMap.get(chatId);
                            if (preview != null) {
                                preview.setChatName(chatName);
                                chatPreviews.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                                adapter.submitList(new ArrayList<>(chatPreviews));
                            }
                        }
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

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase error: " + error.getMessage());
                    }
                });
    }

    private void loadLastMessage(String chatId, String chatName, long chatCreatedAt) {
        messagesRef.orderByChild("chatId")
                .equalTo(chatId)
                .limitToLast(1)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String lastMessage = "No messages yet";
                        long timestamp = chatCreatedAt;

                        for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                            MessageModal message = msgSnapshot.getValue(MessageModal.class);
                            if (message != null) {
                                lastMessage = message.getSenderName() + ": " + message.getText();
                                timestamp = message.getCreatedAt();
                            }
                        }

                        ChatPreview preview = chatMap.get(chatId);
                        if (preview == null) {
                            preview = new ChatPreview(chatId, chatName, lastMessage, timestamp, 0);
                            chatMap.put(chatId, preview);
                            chatPreviews.add(preview);
                        } else {
                            preview.setLastMessage(lastMessage);
                            preview.setTimestamp(timestamp);
                        }

                        chatPreviews.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        adapter.submitList(new ArrayList<>(chatPreviews));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading last message", error.toException());
                    }
                });

        // Also listen live for new messages in this chat to update preview
        messagesRef.orderByChild("chatId")
                .equalTo(chatId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                        MessageModal message = snapshot.getValue(MessageModal.class);
                        if (message != null) {
                            updateChatPreview(chatId, chatName, message);
                        }
                    }

                    @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) { }
                    @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) { }
                    @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }
                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void updateChatPreview(String chatId, String chatName, MessageModal message) {
        ChatPreview preview = chatMap.get(chatId);
        if (preview != null) {
            preview.setLastMessage(message.getSenderName() + ": " + message.getText());
            preview.setTimestamp(message.getCreatedAt());

            // Move this chat to top
            chatPreviews.remove(preview);
            chatPreviews.add(0, preview);

            adapter.submitList(new ArrayList<>(chatPreviews));
        }
    }

    private void showCreateChatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatListActivity.this);
        builder.setTitle("Create New Chat");

        final EditText input = new EditText(ChatListActivity.this);
        input.setHint("Enter chat name");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String chatName = input.getText().toString().trim();
            if (!chatName.isEmpty()) {
                createNewChat(chatName);
            } else {
                Toast.makeText(ChatListActivity.this, "Please enter a chat name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createNewChat(String chatName) {
        Log.d(TAG, "createNewChat called with name: " + chatName);

        String chatId = chatsRef.push().getKey();
        if (chatId == null) {
            Toast.makeText(this, "Error creating chat", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "chatId is null from Firebase push()");
            return;
        }

        // Include participants so we can filter by this user later
        HashMap<String, Object> chatData = new HashMap<>();
        HashMap<String, Boolean> participants = new HashMap<>();
        participants.put(currentUserIdStr, true);  // current user is a participant

        chatData.put("name", chatName);
        chatData.put("createdAt", System.currentTimeMillis());
        chatData.put("participants", participants);

        chatsRef.child(chatId).setValue(chatData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Chat created successfully, id: " + chatId);
                    Toast.makeText(this, "Chat created!", Toast.LENGTH_SHORT).show();

                    // Open the new chat
                    Intent intent = new Intent(ChatListActivity.this, DashboardActivity.class);
                    intent.putExtra("CHAT_ID", chatId);
                    intent.putExtra("CHAT_NAME", chatName);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error creating chat", e);
                });
    }
}
