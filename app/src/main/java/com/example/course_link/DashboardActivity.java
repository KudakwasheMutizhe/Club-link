package com.example.course_link;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.ArrayList;
import java.util.HashSet;

/**
 * DashboardActivity - Real-time Firebase chat with push notifications.
 * Used as the Messages screen for a single chat.
 *
 * Uses:
 *  - SessionManager → logged-in SQLite user id
 *  - SharedPreferences("user_prefs") → username (logged_in_username)
 */
public class DashboardActivity extends AppCompatActivity implements MessageAdapter.MyIdProvider {

    private static final String TAG = "DashboardActivity";

    // Views
    private ImageButton btnSend;
    private ImageButton btnBack;
    private EditText etMessage;
    private RecyclerView recyclerView;
    private TextView tvChatName;

    // Adapter + data
    private MessageAdapter adapter;
    private final ArrayList<MessageModal> messages = new ArrayList<>();
    private final HashSet<String> messageIds = new HashSet<>();

    // Firebase
    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;

    // Chat info
    private String currentChatId;
    private String currentChatName;

    // Logged-in user identity
    private SessionManager sessionManager;
    private long currentUserId;
    private String myId;    // string version of SQLite user id
    private String myName;  // username shown in messages

    private static final String USER_PREFS = "user_prefs";
    private static final String KEY_LOGGED_IN_USERNAME = "logged_in_username";

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
        setContentView(R.layout.activity_messages);   // uses your messages.xml layout

        // Handle system bars padding for root @+id/main
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // ---------- Session / logged-in user ----------
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        if (currentUserId == -1) {
            // Nobody logged in → back to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        myId = String.valueOf(currentUserId);

        // Get username from the same prefs you use in LoginActivity / SignupActivity
        SharedPreferences userPrefs = getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        myName = userPrefs.getString(KEY_LOGGED_IN_USERNAME, null);

        if (myName == null || myName.trim().isEmpty()) {
            myName = "User-" + myId;  // fallback
        }

        // ---------- Get chat info from intent ----------
        Intent intent = getIntent();
        currentChatId = intent.getStringExtra("CHAT_ID");
        currentChatName = intent.getStringExtra("CHAT_NAME");

        if (currentChatId == null) currentChatId = "GLOBAL_CHAT";
        if (currentChatName == null) currentChatName = "Messages";

        Log.d(TAG, "myId = " + myId + ", myName = " + myName);
        Log.d(TAG, "chatId = " + currentChatId + ", chatName = " + currentChatName);

        // ---------- Firebase ----------
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("messages_v2");

        // ---------- Hook views ----------
        recyclerView = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.message);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvChatName = findViewById(R.id.tvChatName);

        // Set chat name in header
        tvChatName.setText(currentChatName);

        // Back button: go back to chat list
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

        // Listen for messages in this chat only
        setupFirebaseListener();

        // Notification permission
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

    // ---------- Firebase listener ----------
    private void setupFirebaseListener() {
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

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) { }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                MessageModal message = snapshot.getValue(MessageModal.class);
                if (message != null && message.getId() != null) {
                    messageIds.remove(message.getId());
                    messages.removeIf(m -> m.getId() != null && m.getId().equals(message.getId()));
                    adapter.submitList(new ArrayList<>(messages));
                }
            }

            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.orderByChild("chatId")
                .equalTo(currentChatId)
                .addChildEventListener(messagesListener);
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
                myId,          // senderId = logged in user id (as String)
                myName,        // senderName = username from user_prefs
                System.currentTimeMillis(),
                currentChatId
        );

        etMessage.setText("");

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
    }

    @Override
    public String getMyId() {
        return myId;
    }
}

