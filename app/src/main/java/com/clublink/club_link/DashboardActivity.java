package com.clublink.club_link;

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
import android.view.View;
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
    private String myId;    // 🔑 CONSISTENT identity key (lowercase username)
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
            Log.e(TAG, "FATAL: CHAT_ID missing in intent. Cannot open chat.");
            Toast.makeText(this, "Error: No chat selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentChatName == null || currentChatName.trim().isEmpty()) {
            currentChatName = "Messages";
        }

        // --------- User / Session ---------
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        // PRIMARY: get who is logged in from SessionManager
        String sessionUsername = sessionManager.getUsername();   // <-- use your actual getter name

        // SECONDARY / BACKUP: username passed in Intent (if you still send it)
        String intentUsername = intent.getStringExtra("CURRENT_USERNAME");

        // Priority: session username first, then intent
        if (sessionUsername != null && !sessionUsername.trim().isEmpty()) {
            currentUsername = sessionUsername;
        } else if (intentUsername != null && !intentUsername.trim().isEmpty()) {
            currentUsername = intentUsername;
        } else {
            // Only if BOTH are missing do we force login again
            Log.e(TAG, "FATAL: Cannot open chat without a valid username (session + intent both empty).");
            Toast.makeText(this, "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // The user's unique, consistent ID is their lowercase username.
        myId = currentUsername.toLowerCase(Locale.ROOT);
        myName = currentUsername; // The display name is the original username.

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
        tvTypingIndicator.setVisibility(View.GONE);

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

                // Ensure we only accept messages for this chat
                if (!currentChatId.equals(msg.getChatId())) {
                    Log.w(TAG, "Received a message for a different chat. Ignoring. ChatID: " + msg.getChatId());
                    return;
                }

                if (!messageIds.add(msg.getId())) return; // Avoid duplicates

                messages.add(msg);
                adapter.submitList(new ArrayList<>(messages));
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String prev) { }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prev) { }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                MessageModal msg = snapshot.getValue(MessageModal.class);
                if (msg == null || msg.getId() == null) return;

                messageIds.remove(msg.getId());
                messages.removeIf(m -> m.getId().equals(msg.getId()));
                adapter.submitList(new ArrayList<>(messages));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
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
                    String typingUserId = child.getKey();
                    Boolean isTyping = child.getValue(Boolean.class);

                    if (typingUserId != null && isTyping != null && isTyping && !typingUserId.equals(myId)) {
                        someoneTyping = true;
                        break;
                    }
                }
                tvTypingIndicator.setVisibility(someoneTyping ? View.VISIBLE : View.GONE);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Typing listener cancelled.", error.toException());
            }
        };
        typingRef.addValueEventListener(typingListener);
    }

    private void setupTypingPublisher() {
        etMessage.addTextChangedListener(new TextWatcher() {
            private void setTyping(boolean typing) {
                if (myId != null) {
                    typingRef.child(myId).setValue(typing);
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isCurrentlyTyping = s != null && s.length() > 0;
                setTyping(isCurrentlyTyping);

                if (typingTimeoutRunnable != null) {
                    typingHandler.removeCallbacks(typingTimeoutRunnable);
                }

                if (isCurrentlyTyping) {
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
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Toast.makeText(this, "Failed to create message ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        MessageModal msg = new MessageModal(
                messageId,
                text,
                myId,     // CONSISTENT identity key (lowercase username)
                myName,   // display name
                System.currentTimeMillis(),
                currentChatId
        );

        etMessage.setText("");
        if (myId != null) {
            typingRef.child(myId).setValue(false);
        }

        messagesRef.child(messageId).setValue(msg)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message", e);
                    Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show();
                });
    }

    // ---------------- LIFECYCLE & PROVIDER ----------------

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cleaning up listeners for chat " + currentChatId);

        // Properly remove all Firebase listeners

        // 1. Remove the messages listener using the original query.
        if (messagesRef != null && messagesListener != null) {
            messagesRef.orderByChild("chatId")
                    .equalTo(currentChatId)
                    .removeEventListener(messagesListener);
        }

        // 2. Remove the typing listener and clean up typing status.
        if (typingRef != null) {
            if (myId != null) {
                typingRef.child(myId).setValue(false); // Clean up typing status
            }
            if (typingListener != null) {
                typingRef.removeEventListener(typingListener);
            }
        }

        // 3. Clean up the handler to prevent memory leaks.
        if (typingHandler != null && typingTimeoutRunnable != null) {
            typingHandler.removeCallbacks(typingTimeoutRunnable);
        }
    }

    @Override
    public String getMyId() {
        return myId;
    }
}
