package com.example.club_link;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import android.widget.EditText;
import android.widget.Toast;

/**
 * ChatListActivity - Shows list of all chat conversations
 * Users can create new chats with custom names
 */
public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private final ArrayList<ChatPreview> chatPreviews = new ArrayList<>();
    private final HashMap<String, ChatPreview> chatMap = new HashMap<>();

    private DatabaseReference chatsRef;
    private DatabaseReference messagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        Log.d(TAG, "onCreate: ChatListActivity created");

        recyclerView = findViewById(R.id.rvChatList);
        ImageButton btnNewChat = findViewById(R.id.btnNewChat);

        if (btnNewChat == null) {
            Log.e(TAG, "btnNewChat is NULL! Check your layout file and id.");
            Toast.makeText(this, "btnNewChat is NULL", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "btnNewChat found successfully");
        }

        adapter = new ChatListAdapter(chatPreview -> {
            Intent intent = new Intent(ChatListActivity.this, MainActivity.class);
            intent.putExtra("CHAT_ID", chatPreview.getChatId());
            intent.putExtra("CHAT_NAME", chatPreview.getChatName());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        chatsRef = database.getReference("chats");
        messagesRef = database.getReference("messages_v2");

        loadChats();

        if (btnNewChat != null) {
            btnNewChat.setOnClickListener(v -> {
                Log.d(TAG, "New Chat button clicked!");
                Toast.makeText(ChatListActivity.this, "Button clicked!", Toast.LENGTH_SHORT).show();
                showCreateChatDialog();
            });
            Log.d(TAG, "Click listener set on btnNewChat");
        } else {
            Log.e(TAG, "Click listener NOT set because btnNewChat is null");
        }


        // Set up adapter with click listener
        adapter = new ChatListAdapter(chatPreview -> {
            // Open the chat when clicked
            Intent intent = new Intent(ChatListActivity.this, MainActivity.class);
            intent.putExtra("CHAT_ID", chatPreview.getChatId());
            intent.putExtra("CHAT_NAME", chatPreview.getChatName());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Listen for chats
        loadChats();

        // Create new chat button

        if (btnNewChat == null) {
            Log.e(TAG, "btnNewChat is NULL after using ImageButton!");
        } else {
            Log.d(TAG, "btnNewChat (ImageButton) found successfully");
            btnNewChat.setOnClickListener(v -> {
                Log.d(TAG, "New Chat button clicked!");
                Toast.makeText(ChatListActivity.this, "Button clicked!", Toast.LENGTH_SHORT).show();
                showCreateChatDialog();
            });
        }
    }
        public void onNewChatClick(android.view.View view) {
        Log.d(TAG, "XML onClick fired!");
        Toast.makeText(ChatListActivity.this, "XML onClick fired", Toast.LENGTH_SHORT).show();
        showCreateChatDialog();
    }

    private void loadChats() {
        chatsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String chatId = snapshot.getKey();
                String chatName = snapshot.child("name").getValue(String.class);
                Long createdAt = snapshot.child("createdAt").getValue(Long.class);

                if (chatId != null && chatName != null) {
                    // Get the last message for this chat
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

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
            }

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
                .addListenerForSingleValueEvent(new ValueEventListener() {
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

        // Also listen for new messages in this chat
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

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void updateChatPreview(String chatId, String chatName, MessageModal message) {
        ChatPreview preview = chatMap.get(chatId);
        if (preview != null) {
            preview.setLastMessage(message.getSenderName() + ": " + message.getText());
            preview.setTimestamp(message.getCreatedAt());

            // Move to top
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

        HashMap<String, Object> chatData = new HashMap<>();
        chatData.put("name", chatName);
        chatData.put("createdAt", System.currentTimeMillis());

        chatsRef.child(chatId).setValue(chatData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Chat created successfully, id: " + chatId);
                    Toast.makeText(this, "Chat created!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ChatListActivity.this, MainActivity.class);
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