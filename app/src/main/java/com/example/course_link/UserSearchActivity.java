package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows all other users from Firebase so you can start a DM.
 * Uses /users in Firebase; never shows the logged-in user.
 */
public class UserSearchActivity extends AppCompatActivity {

    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    private RecyclerView rvUsers;
    private EditText etSearch;

    private UserSearchAdapter adapter;
    private SessionManager sessionManager;
    private long currentUserId;
    private String currentUserIdStr;
    private String currentUsername;

    private DatabaseReference usersRef;
    private DatabaseReference chatsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);

        // Session info
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        currentUserIdStr = String.valueOf(currentUserId);
        currentUsername = sessionManager.getUsername();

        if (currentUserId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        rvUsers = findViewById(R.id.rvUsers);
        etSearch = findViewById(R.id.etSearch);

        adapter = new UserSearchAdapter(user -> openOrCreateChatWith(user));
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        usersRef = db.getReference("users");
        chatsRef = db.getReference("chats");

        // Load all users from Firebase
        loadUsersFromFirebase();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsersFromFirebase() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserDbHelper.SimpleUser> list = new ArrayList<>();
                Map<Long, UserDbHelper.SimpleUser> unique = new LinkedHashMap<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    AppUserFirebase fbUser = child.getValue(AppUserFirebase.class);
                    if (fbUser == null) continue;

                    long id = fbUser.id;
                    String username = fbUser.username != null ? fbUser.username : "";
                    String fullname = fbUser.fullname != null ? fbUser.fullname : "";

                    boolean isMeById = (id == currentUserId);
                    boolean isMeByUsername = currentUsername != null
                            && currentUsername.equalsIgnoreCase(username);

                    // Skip myself
                    if (!isMeById && !isMeByUsername) {
                        unique.put(id, new UserDbHelper.SimpleUser(id, username, fullname));
                    }
                }

                list.addAll(unique.values());
                adapter.setUsers(list);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserSearchActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Open existing DM or create it if it doesn't exist.
     * One chat per pair of users (no duplicates), no self-chat.
     */
    private void openOrCreateChatWith(UserDbHelper.SimpleUser otherUser) {
        long otherId = otherUser.getId();
        String otherIdStr = String.valueOf(otherId);

        if (otherId == currentUserId) {
            Toast.makeText(this, "You can't chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deterministic DM chat id: dmv2_<smallId>_<bigId>
        String a = currentUserIdStr;
        String b = otherIdStr;
        if (a.compareTo(b) > 0) {
            String tmp = a;
            a = b;
            b = tmp;
        }
        String chatId = "dmv2_" + a + "_" + b;
        String defaultChatName = "Chat with " + otherUser.getUsername();

        DatabaseReference chatRef = chatsRef.child(chatId);
        chatRef.get().addOnSuccessListener(snapshot -> {
            String chatNameToUse = defaultChatName;

            if (snapshot.exists()) {
                String existingName = snapshot.child("name").getValue(String.class);
                if (existingName != null) chatNameToUse = existingName;
            } else {
                // Create new chat node
                Map<String, Object> chatData = new LinkedHashMap<>();
                Map<String, Boolean> participants = new LinkedHashMap<>();
                participants.put(currentUserIdStr, true);
                participants.put(otherIdStr, true);

                chatData.put("name", defaultChatName);
                chatData.put("createdAt", System.currentTimeMillis());
                chatData.put("participants", participants);

                chatRef.setValue(chatData);
            }

            openChatScreen(chatId, chatNameToUse);
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error opening chat", Toast.LENGTH_SHORT).show());
    }

    private void openChatScreen(String chatId, String chatName) {
        Intent intent = new Intent(UserSearchActivity.this, DashboardActivity.class);
        intent.putExtra("CHAT_ID", chatId);
        intent.putExtra("CHAT_NAME", chatName);
        startActivity(intent);
        finish();
    }
}
