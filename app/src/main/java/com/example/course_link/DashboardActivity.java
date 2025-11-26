package com.example.course_link;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * DashboardActivity - Real-time Firebase chat with push notifications.
 * Uses SQLite userId + username, supports typing indicator.
 */
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
    private DatabaseReference typingRef;            // typing/{chatId}
    private ValueEventListener typingListener;
    private Handler typingHandler;
    private Runnable typingTimeoutRunnable;

    // Chat info
    private String currentChatId;
    private String currentChatName;

    // Logged-in user (from SessionManager + prefs)
    private SessionManager sessionManager;
    private long currentUserId;
    private String myId;     // String version of userId
    private String myName;   // username

    private static final String PREFS_NAME = "user_prefs"; // same as you used for logged_in_username
    private static final String KEY_USERNAME = "logged_in_username";

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
        setContentView(R.layout.activity_messages);   // uses your messages layout

        // Handle system bars padding for root @+id/main
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // ---------- Chat info from intent ----------
        Intent intent = getIntent();
        currentChatId = intent.getStringExtra("CHAT_ID");
        currentChatName = intent.getStringExtra("CHAT_NAME");

        if (currentChatId == null) currentChatId = "GLOBAL_CHAT";
        if (currentChatName == null) currentChatName = "Messages";

        // ---------- Logged-in user from SessionManager + username prefs ----------
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        if (currentUserId == -1) {
            // No logged-in user, go back to Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        myId = String.valueOf(currentUserId);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        myName = prefs.getString(KEY_USERNAME, "User-" + myId);

        Log.d(TAG, "myId = " + myId + ", myName = " + myName);
        Log.d(TAG, "chatId = " + currentChatId + ", chatName = " + currentChatName);

        // ---------- Firebase ----------
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        messagesRef = database.getReference("messages_v2");
        typingRef = database.getReference("typing").child(currentChatId);

        // ---------- Hook views ----------
        recyclerView = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.message);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvChatName = findViewById(R.id.tvChatName);
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator);

        tvChatName.setText(currentChatName);
        tvTypingIndicator.setVisibility(android.view.View.GONE);

        btnBack.setOnClickListener(v -> finish());

        // ---------- RecyclerView setup ----------
        adapter = new MessageAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Send message on button tap
        btnSend.setOnClickListener(v -> sendMessage());

        // Send on keyboard 'Send' IME action
        etMessage.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // ---------- Typing indicator setup ----------
        typingHandler = new Handler(Looper.getMainLooper());
        setupTypingListener();
        setupTypingPublisher();

        // Listen for messages in this chat only
        setupFirebaseMessagesListener();

        // Ask for notification permission when needed
        requestNotificationPermission();
    }

    // ---------- Notification permission ----------
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

    // ---------- Firebase messages listener ----------
    private void setupFirebaseMessagesListener() {
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                MessageModal message = snapshot.getValue(MessageModal.class);
                if (message != null && message.getId() != null) {
                    if (currentChatId.equals(message.getChatId())) {
                        if (messageIds.add(message.getId())) {
                            messages.add(message);
                            adapter.submitList(new ArrayList<>(messages));
                            recyclerView.scrollToPosition(messages.size() - 1);
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) { }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                MessageModal message = snapshot.getValue(MessageModal.class);
                if (message != null && message.getId() != null) {
                    messageIds.remove(message.getId());
                    messages.removeIf(m -> m.getId() != null && m.getId().equals(message.getId()));
                    adapter.submitList(new ArrayList<>(messages));
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.orderByChild("chatId")
                .equalTo(currentChatId)
                .addChildEventListener(messagesListener);
    }

    // ---------- Typing indicator: listen to others ----------
    private void setupTypingListener() {
        typingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean someoneElseTyping = false;

                for (DataSnapshot child : snapshot.getChildren()) {
                    String userId = child.getKey();
                    Boolean isTyping = child.getValue(Boolean.class);

                    if (userId == null || isTyping == null) continue;

                    if (!userId.equals(myId) && isTyping) {
                        someoneElseTyping = true;
                        break;
                    }
                }

                tvTypingIndicator.setVisibility(
                        someoneElseTyping ? android.view.View.VISIBLE : android.view.View.GONE
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

        typingRef.addValueEventListener(typingListener);
    }

    // ---------- Typing indicator: publish my typing state ----------
    private void setupTypingPublisher() {
        etMessage.addTextChangedListener(new TextWatcher() {

            private void setTyping(boolean typing) {
                typingRef.child(myId).setValue(typing);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isTyping = s != null && s.length() > 0;
                setTyping(isTyping);

                // Auto turn off typing after 3 seconds of no change
                if (typingTimeoutRunnable != null) {
                    typingHandler.removeCallbacks(typingTimeoutRunnable);
                }

                if (isTyping) {
                    typingTimeoutRunnable = () -> setTyping(false);
                    typingHandler.postDelayed(typingTimeoutRunnable, 3000);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    // ---------- Send message ----------
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(DashboardActivity.this, "Type a message first", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show();
            return;
        }

        MessageModal message = new MessageModal(
                messageId,
                text,
                myId,
                myName,
                System.currentTimeMillis(),
                currentChatId
        );

        etMessage.setText("");
        // stop typing when sending
        typingRef.child(myId).setValue(false);

        messagesRef.child(messageId).setValue(message)
                .addOnFailureListener(e -> {
                    Toast.makeText(DashboardActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "sendMessage failed", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        if (typingRef != null) {
            typingRef.child(myId).setValue(false); // make sure I’m not “typing” forever
            if (typingListener != null) {
                typingRef.removeEventListener(typingListener);
            }
        }
    }

    @Override
    public String getMyId() {
        return myId;
    }
}


