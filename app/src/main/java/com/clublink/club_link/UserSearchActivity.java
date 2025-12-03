package com.clublink.club_link;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
 * Chat identity key = username.toLowerCase()
 */
public class UserSearchActivity extends AppCompatActivity {

    private static final String TAG = "UserSearchActivity";
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

        Log.d(TAG, "Session - UserID: " + currentUserId + ", Username: '" + currentUsername + "'");

        if (currentUserId == -1) {
            // truly not logged in
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            Toast.makeText(this, "Session error: Username missing", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // ✅ Use consistent key format: lowercase username (no fallback to uid_)
        currentUserKey = currentUsername.toLowerCase(Locale.ROOT);
        Log.d(TAG, "Current user key: " + currentUserKey);

        // --------- Views ----------
        rvUsers = findViewById(R.id.rvUsers);
        etSearch = findViewById(R.id.etSearch);

        adapter = new UserSearchAdapter(this::openOrCreateChatWith);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        // --------- Firebase refs ----------
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        usersRef = db.getReference(); // ✅ Read from root, not /users/
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
     * Example: "alice" and "jane" -> "dmv2_alice_jane"
     * Example: "bob" and "alice" -> "dmv2_alice_bob"
     */
    private String buildDmChatId(String key1, String key2) {
        String a = key1.toLowerCase(Locale.ROOT);
        String b = key2.toLowerCase(Locale.ROOT);

        // Alphabetical ordering for consistency
        if (a.compareTo(b) > 0) {
            String tmp = a;
            a = b;
            b = tmp;
        }
        return "dmv2_" + a + "_" + b;
    }

    // ---------- Load all users from /users ----------

    private void loadUsersFromFirebase() {
        Log.d(TAG, "Starting to load users from Firebase...");
        Log.d(TAG, "Current user - ID: " + currentUserId + ", Username: '" + currentUsername + "'");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserDbHelper.SimpleUser> list = new ArrayList<>();
                Map<Long, UserDbHelper.SimpleUser> unique = new LinkedHashMap<>();

                Log.d(TAG, "Firebase snapshot children count: " + snapshot.getChildrenCount());

                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    if (key == null) {
                        continue;
                    }

                    Log.d(TAG, "Processing node: " + key);

                    // Skip system nodes
                    if (key.equals("chats") ||
                            key.equals("messages") ||
                            key.equals("messages_v2") ||
                            key.equals("ClubInfo") ||
                            key.equals("Events") ||
                            key.equals("Announcements") ||
                            key.equals("announcements") ||
                            key.equals("typing")) {
                        Log.d(TAG, "Skipping system node: " + key);
                        continue;
                    }

                    // Special handling for /users/ node - it contains nested users
                    if (key.equals("users")) {
                        Log.d(TAG, "Processing nested /users/ node");
                        for (DataSnapshot userChild : child.getChildren()) {
                            processUserNode(userChild, unique);
                        }
                        continue;
                    }

                    // Process root-level user nodes
                    processUserNode(child, unique);
                }

                list.addAll(unique.values());
                adapter.setUsers(list);
                Log.d(TAG, "=== FINAL RESULT: Loaded " + list.size() + " users (excluding self) ===");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserSearchActivity.this,
                        "Failed to load users", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to load users: " + error.getMessage());
            }
        });
    }

    /**
     * Process a single user node and add to the unique map if valid
     */
    private void processUserNode(DataSnapshot userSnapshot, Map<Long, UserDbHelper.SimpleUser> unique) {
        AppUserFirebase fbUser = userSnapshot.getValue(AppUserFirebase.class);
        if (fbUser == null) {
            Log.w(TAG, "Failed to parse user from node: " + userSnapshot.getKey());
            return;
        }

        long id = fbUser.id;
        String username = fbUser.username != null ? fbUser.username : "";
        String fullname = fbUser.fullname != null ? fbUser.fullname : "";

        Log.d(TAG, "Found user in Firebase - ID: " + id + ", Username: '" + username + "', Fullname: '" + fullname + "'");

        // Skip users without username
        if (username.trim().isEmpty()) {
            Log.w(TAG, "Skipping user with ID " + id + " - no username");
            return;
        }

        boolean isMeById = (id == currentUserId);
        boolean isMeByUsername = username.equalsIgnoreCase(currentUsername);

        Log.d(TAG, "Comparing - isMeById: " + isMeById + ", isMeByUsername: " + isMeByUsername);

        // Skip myself
        if (!isMeById && !isMeByUsername) {
            // Only add if not already in map (prevents duplicates from root and /users/)
            if (!unique.containsKey(id)) {
                unique.put(id, new UserDbHelper.SimpleUser(id, username, fullname));
                Log.d(TAG, "✓ Added user: " + username + " (ID: " + id + ")");
            } else {
                Log.d(TAG, "User already added: " + username);
            }
        } else {
            Log.d(TAG, "✗ Skipped self: " + username);
        }
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
            Log.e(TAG, "Attempted to chat with user without username");
            return;
        }

        String otherKey = otherUsername.toLowerCase(Locale.ROOT);

        // ✅ SECURITY: Prevent self-chat
        if (otherKey.equals(currentUserKey)) {
            Toast.makeText(this, "You can't chat with yourself", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Prevented self-chat attempt");
            return;
        }

        // 1) Build deterministic DM chat id based on the keys
        String chatId = buildDmChatId(currentUserKey, otherKey);
        Log.d(TAG, "Opening/creating chat: " + chatId + " between " + currentUserKey + " and " + otherKey);

        // What to show as chat name
        String defaultChatName = otherUsername;

        DatabaseReference chatRef = chatsRef.child(chatId);

        // 2) Check if chat exists; if not, create it
        chatRef.get().addOnSuccessListener(snapshot -> {
            String chatNameToUse = defaultChatName;

            if (snapshot.exists()) {
                // Chat exists - verify we're a participant
                DataSnapshot participantsSnapshot = snapshot.child("participants");
                if (!participantsSnapshot.hasChild(currentUserKey)) {
                    Log.e(TAG, "Security violation: User " + currentUserKey + " not in existing chat " + chatId);
                    Toast.makeText(this, "Access denied to this chat", Toast.LENGTH_SHORT).show();
                    return;
                }

                String existingName = snapshot.child("name").getValue(String.class);
                if (existingName != null && !existingName.trim().isEmpty()) {
                    chatNameToUse = existingName;
                }
                Log.d(TAG, "Chat exists: " + chatId);
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
                Log.d(TAG, "Created new chat: " + chatId);
            }

            // 3) Open DashboardActivity for this DM
            openChatScreen(chatId, chatNameToUse);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error opening chat", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening chat: " + e.getMessage(), e);
        });
    }

    private void openChatScreen(String chatId, String chatName) {
        Intent intent = new Intent(UserSearchActivity.this, DashboardActivity.class);
        intent.putExtra("CHAT_ID", chatId);
        intent.putExtra("CHAT_NAME", chatName);
        intent.putExtra("CURRENT_USERNAME", currentUsername); // ✅ Fixed: Added missing parameter
        startActivity(intent);
        finish();
    }
}