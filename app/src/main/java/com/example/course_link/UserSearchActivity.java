package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;

public class UserSearchActivity extends AppCompatActivity {

    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    private RecyclerView rvUsers;
    private EditText etSearch;

    private UserSearchAdapter adapter;
    private SessionManager sessionManager;
    private long currentUserId;
    private String currentUserIdStr;

    private DatabaseReference chatsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        rvUsers = findViewById(R.id.rvUsers);
        etSearch = findViewById(R.id.etSearch);

        adapter = new UserSearchAdapter(user -> openOrCreateChatWith(user));
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        // Load users from SQLite
        UserDbHelper dbHelper = new UserDbHelper(this);
        List<UserDbHelper.SimpleUser> users = dbHelper.getAllUsersExcept(currentUserId);
        adapter.setUsers(users);

        // Search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        chatsRef = database.getReference("chats");
    }

    private void openOrCreateChatWith(UserDbHelper.SimpleUser otherUser) {
        // For now, just create a new chat every time they tap someone
        String chatName = "Chat with " + otherUser.getUsername();

        String chatId = chatsRef.push().getKey();
        if (chatId == null) {
            Toast.makeText(this, "Error creating chat", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> chatData = new HashMap<>();
        HashMap<String, Boolean> participants = new HashMap<>();
        participants.put(currentUserIdStr, true);
        participants.put(String.valueOf(otherUser.getId()), true);

        chatData.put("name", chatName);
        chatData.put("createdAt", System.currentTimeMillis());
        chatData.put("participants", participants);

        chatsRef.child(chatId).setValue(chatData)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(UserSearchActivity.this, DashboardActivity.class);
                    intent.putExtra("CHAT_ID", chatId);
                    intent.putExtra("CHAT_NAME", chatName);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show());
    }
}
