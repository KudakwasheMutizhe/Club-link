package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class UserSearchActivity extends AppCompatActivity {

    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    private RecyclerView rvUsers;
    private EditText etSearch;
    private UserSearchAdapter adapter;

    private SessionManager sessionManager;
    private long currentUserId;
    private String currentUserIdStr;

    private UserDbHelper dbHelper;
    private List<UserDbHelper.SimpleUser> fullUserList = new ArrayList<>();

    private DatabaseReference chatsRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);

        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        if (currentUserId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserIdStr = String.valueOf(currentUserId);

        dbHelper = new UserDbHelper(this);
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        chatsRef = database.getReference("chats");

        rvUsers = findViewById(R.id.rvUsers);
        etSearch = findViewById(R.id.etSearch);

        adapter = new UserSearchAdapter(user -> {
            // On user click → create chat and go to DashboardActivity
            startChatWithUser(user);
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        loadUsers();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void loadUsers() {
        fullUserList = dbHelper.getAllUsersExcept(currentUserId);
        adapter.setUsers(new ArrayList<>(fullUserList));
    }

    private void filterUsers(String query) {
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            adapter.setUsers(new ArrayList<>(fullUserList));
            return;
        }

        List<UserDbHelper.SimpleUser> filtered = new ArrayList<>();
        for (UserDbHelper.SimpleUser u : fullUserList) {
            String uname = u.username != null ? u.username.toLowerCase() : "";
            String fname = u.fullname != null ? u.fullname.toLowerCase() : "";
            if (uname.contains(q) || fname.contains(q)) {
                filtered.add(u);
            }
        }
        adapter.setUsers(filtered);
    }

    private void startChatWithUser(UserDbHelper.SimpleUser target) {
        if (target.id == currentUserId) {
            Toast.makeText(this, "You can't chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId = chatsRef.push().getKey();
        if (chatId == null) {
            Toast.makeText(this, "Error creating chat", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetIdStr = String.valueOf(target.id);

        java.util.HashMap<String, Object> chatData = new java.util.HashMap<>();
        java.util.HashMap<String, Boolean> participants = new java.util.HashMap<>();
        participants.put(currentUserIdStr, true);
        participants.put(targetIdStr, true);

        // Use the other user's username as the chat name (DM style)
        chatData.put("name", target.username);
        chatData.put("createdAt", System.currentTimeMillis());
        chatData.put("participants", participants);

        chatsRef.child(chatId).setValue(chatData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Chat started with " + target.username, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(UserSearchActivity.this, DashboardActivity.class);
                    intent.putExtra("CHAT_ID", chatId);
                    intent.putExtra("CHAT_NAME", target.username);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show();
                });
    }
}
