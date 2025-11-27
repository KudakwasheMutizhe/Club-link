package com.example.course_link;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity implements MessageAdapter.MyIdProvider {

    private static final String TAG = "DashboardActivity";
    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    // Views
    private ImageButton btnSend;
    private ImageButton btnBack;
    private EditText etMessage;
    private RecyclerView recyclerView;
    private TextView tvChatName;
    private TextView tvTypingIndicator;

    // Adapter + data
    private MessageAdapter adapter;
    private final ArrayList<MessageModal> messages = new ArrayList<>();
    private final HashSet<String> messageIds = new HashSet<>();

    // Firebase
    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;

    // Typing
    private DatabaseReference typingRef;
    private ValueEventListener typingListener;
    private Handler typingHandler;
    private Runnable typingTimeoutRunnable;

    // Chat info
    private String currentChatId;
    private String currentChatName;

    // User
    private SessionManager sessionManager;
    private long currentUserId;
    private String currentUsername;
    private String myId;    // 🔑 identity key used in Firebase (matches ChatList/UserSearch)
    private String myName;  // display name in messages

    // Notification permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    setupNotifications();
                } else {
                    Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_messages);

        // Insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // --------- Chat info from Intent ---------
        Intent intent = getIntent();
        currentChatId = intent.getStringExtra("CHAT_ID");
        currentChatName = intent.getStringExtra("CHAT_NAME");

        if (currentChatId == null || currentChatId.trim().isEmpty()) {
            Log.e(TAG, "CHAT_ID missing in intent – cannot open chat");
            Toast.makeText(this, "Error: no chat selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentChatName == null || currentChatName.trim().isEmpty()) {
            currentChatName = "Messages";
        }

        // --------- User / Session ---------
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        currentUsername = sessionManager.getUsername();

        if (currentUserId == -1) {
            // truly not logged in
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Build the same identity key we used in ChatListActivity & UserSearchActivity
        if (currentUsername != null && !currentUsername.trim().isEmpty()) {
            myId = currentUsername.toLowerCase(Locale.ROOT);
            myName = currentUsername;
        } else {
            myId = "uid_" + currentUserId;         // fallback identity
            myName = "User-" + currentUserId;      // fallback display name
        }

        Log.d(TAG, "Opening chat " + currentChatId + " as " + myName + " (key=" + myId + ")");

        // --------- Firebase refs ---------
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        messagesRef = database.getReference("messages_v2");
        typingRef = database.getReference("typing").child(currentChatId);

        // --------- Views ---------
        recyclerView = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.message);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvChatName = findViewById(R.id.tvChatName);
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator);

        tvChatName.setText(currentChatName);
        tvTypingIndicator.setVisibility(android.view.View.GONE);

        btnBack.setOnClickListener(v -> finish());

        // --------- Recycler + Adapter ---------
        adapter = new MessageAdapter(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true); // show newest messages at bottom
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // --------- Typing indicator ---------
        typingHandler = new Handler(Looper.getMainLooper());
        setupTypingListener();
        setupTypingPublisher();

        // --------- Listen for messages in THIS chat ---------
        setupFirebaseMessagesListener();

        // --------- Notifications ---------
        requestNotificationPermission();
    }

    // ---------------- PERMISSIONS ----------------

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                setupNotifications();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            setupNotifications();
        }
    }

    private void setupNotifications() {
        NotificationHelper.initNotificationChannel(this);
        NotificationHelper.subscribeToAnnouncements();
    }

    // ---------------- MESSAGES LISTENER ----------------

    private void setupFirebaseMessagesListener() {
        messagesListener = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                MessageModal msg = snapshot.getValue(MessageModal.class);
                if (msg == null || msg.getId() == null) return;

                // ✅ Only messages for THIS chat
                if (!currentChatId.equals(msg.getChatId())) return;

                // Avoid duplicates
                if (!messageIds.add(msg.getId())) return;

                messages.add(msg);

                ArrayList<MessageModal> copy = new ArrayList<>(messages);
                adapter.submitList(copy);
                recyclerView.scrollToPosition(copy.size() - 1);
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String prev) { }

            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prev) { }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                MessageModal msg = snapshot.getValue(MessageModal.class);
                if (msg == null || msg.getId() == null) return;

                messageIds.remove(msg.getId());
                messages.removeIf(m -> m.getId().equals(msg.getId()));

                ArrayList<MessageModal> copy = new ArrayList<>(messages);
                adapter.submitList(copy);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this,
                        "Failed to load messages", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Messages listener cancelled: " + error.getMessage());
            }
        };

        messagesRef.orderByChild("chatId")
                .equalTo(currentChatId)
                .addChildEventListener(messagesListener);
    }

    // ---------------- TYPING ----------------

    private void setupTypingListener() {
        typingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean someoneTyping = false;

                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    Boolean typing = child.getValue(Boolean.class);

                    if (uid != null && typing != null && typing && !uid.equals(myId)) {
                        someoneTyping = true;
                        break;
                    }
                }

                tvTypingIndicator.setVisibility(
                        someoneTyping ? android.view.View.VISIBLE : android.view.View.GONE
                );
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };

        typingRef.addValueEventListener(typingListener);
    }

    private void setupTypingPublisher() {
        etMessage.addTextChangedListener(new TextWatcher() {

            private void setTyping(boolean typing) {
                typingRef.child(myId).setValue(typing);
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean typing = s != null && s.length() > 0;
                setTyping(typing);

                if (typingTimeoutRunnable != null) {
                    typingHandler.removeCallbacks(typingTimeoutRunnable);
                }

                if (typing) {
                    typingTimeoutRunnable = () -> setTyping(false);
                    typingHandler.postDelayed(typingTimeoutRunnable, 3000);
                }
            }

            @Override public void afterTextChanged(Editable s) { }
        });
    }

    // ---------------- SEND MESSAGE ----------------

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Type a message first", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = messagesRef.push().getKey();
        if (id == null) {
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure this matches your MessageModal constructor
        // (id, text, senderId, senderName, createdAt, chatId)
        MessageModal msg = new MessageModal(
                id,
                text,
                myId,     // 🔑 identity key (username or uid_X)
                myName,   // display name
                System.currentTimeMillis(),
                currentChatId
        );

        etMessage.setText("");
        typingRef.child(myId).setValue(false);

        messagesRef.child(id).setValue(msg)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }

        if (typingRef != null && typingListener != null) {
            typingRef.child(myId).setValue(false);
            typingRef.removeEventListener(typingListener);
        }

        if (typingHandler != null && typingTimeoutRunnable != null) {
            typingHandler.removeCallbacks(typingTimeoutRunnable);
        }
    }

    @Override
    public String getMyId() {
        return myId;
    }
}
