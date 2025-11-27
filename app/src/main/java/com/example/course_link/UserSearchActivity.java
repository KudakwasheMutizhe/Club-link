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
import java.util.Locale;
import java.util.Map;

/**
 * Shows all other users from Firebase so you can start a DM.
 * Uses /users in Firebase; never shows the logged-in user.
 * Chat identity key = username.toLowerCase(), or "uid_<id>" fallback.
 */
public class UserSearchActivity extends AppCompatActivity {

    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    private RecyclerView rvUsers;
    private EditText etSearch;

    private UserSearchAdapter adapter;
    private SessionManager sessionManager;
    private long currentUserId;
    private String currentUsername;
    private String currentUserKey;   // 🔑 identity used in chats

    private DatabaseReference usersRef;
    private DatabaseReference chatsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);

        // --------- Session info ----------
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        currentUsername = sessionManager.getUsername();

        if (currentUserId == -1) {
            // truly not logged in
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Build the same kind of identity key we used in ChatListActivity
        if (currentUsername != null && !currentUsername.trim().isEmpty()) {
            currentUserKey = currentUsername.toLowerCase(Locale.ROOT);
        } else {
            currentUserKey = "uid_" + currentUserId;
        }

        // --------- Views ----------
        rvUsers = findViewById(R.id.rvUsers);
        etSearch = findViewById(R.id.etSearch);

        adapter = new UserSearchAdapter(this::openOrCreateChatWith);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        // --------- Firebase refs ----------
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        usersRef = db.getReference("users");
        chatsRef = db.getReference("chats");

        // Load all users from Firebase
        loadUsersFromFirebase();

        // Simple client-side name filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    // ---------- Helper: deterministic DM chat id for a pair of user keys ----------

    /**
     * Returns a DM chat id that is unique per pair of keys and consistent
     * no matter who starts the conversation.
     *
     * Example: "am" and "jane" -> "dmv2_am_jane"
     * Example: "uid_3" and "am" -> "dmv2_am_uid_3"
     */
    private String buildDmChatId(String key1, String key2) {
        String a = key1;
        String b = key2;
        if (a.compareTo(b) > 0) {
            String tmp = a;
            a = b;
            b = tmp;
        }
        return "dmv2_" + a + "_" + b;
    }

    // ---------- Load all users from /users ----------

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
                            && username.equalsIgnoreCase(currentUsername);

                    // Skip myself via id OR username match
                    if (!isMeById && !isMeByUsername) {
                        unique.put(id, new UserDbHelper.SimpleUser(id, username, fullname));
                    }
                }

                list.addAll(unique.values());
                adapter.setUsers(list);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserSearchActivity.this,
                        "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------- Open existing DM or create new one ----------

    /**
     * Open existing DM or create it if it doesn't exist.
     * One chat per pair of users (no duplicates), no self-chat.
     */
    private void openOrCreateChatWith(UserDbHelper.SimpleUser otherUser) {
        String otherUsername = otherUser.getUsername();
        if (otherUsername == null || otherUsername.trim().isEmpty()) {
            Toast.makeText(this, "User has no username", Toast.LENGTH_SHORT).show();
            return;
        }

        String otherKey = otherUsername.toLowerCase(Locale.ROOT);

        if (otherKey.equals(currentUserKey)) {
            Toast.makeText(this, "You can't chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Build deterministic DM chat id based on the keys
        String chatId = buildDmChatId(currentUserKey, otherKey);

        // What to show as chat name (can be improved later)
        String defaultChatName = otherUsername;

        DatabaseReference chatRef = chatsRef.child(chatId);

        // 2) Check if chat exists; if not, create it
        chatRef.get().addOnSuccessListener(snapshot -> {
            String chatNameToUse = defaultChatName;

            if (snapshot.exists()) {
                String existingName = snapshot.child("name").getValue(String.class);
                if (existingName != null && !existingName.trim().isEmpty()) {
                    chatNameToUse = existingName;
                }
            } else {
                // Create new chat node under /chats/{chatId}
                Map<String, Object> chatData = new LinkedHashMap<>();
                Map<String, Boolean> participants = new LinkedHashMap<>();
                participants.put(currentUserKey, true);
                participants.put(otherKey, true);

                chatData.put("name", defaultChatName);
                chatData.put("createdAt", System.currentTimeMillis());
                chatData.put("participants", participants);

                chatRef.setValue(chatData);
            }

            // 3) Open DashboardActivity for this DM
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
