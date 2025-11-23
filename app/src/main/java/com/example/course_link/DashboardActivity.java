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
import java.util.UUID;

/**
 * DashboardActivity - Real-time Firebase chat with push notifications.
 * This is used as the Messages screen and includes bottom navigation.
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

    // Per-device user identity
    private static final String PREFS_NAME = "club_link_prefs";
    private static final String KEY_MY_ID = "my_id";
    private static final String KEY_MY_NAME = "my_name";

    private String myId;
    private String myName;

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
        setContentView(R.layout.activity_messages);   // ✅ uses your messages layout

        // Handle system bars padding for root @+id/main
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // ---------- Get chat info from intent ----------
        Intent intent = getIntent();
        currentChatId = intent.getStringExtra("CHAT_ID");
        currentChatName = intent.getStringExtra("CHAT_NAME");

        // Provide safe defaults if nothing was passed
        if (currentChatId == null) currentChatId = "GLOBAL_CHAT";
        if (currentChatName == null) currentChatName = "Messages";

        // ---------- User identity (per device) ----------
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        myId = prefs.getString(KEY_MY_ID, null);
        myName = prefs.getString(KEY_MY_NAME, null);

        if (myId == null) {
            myId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_MY_ID, myId).apply();
        }

        if (myName == null) {
            myName = "User-" + myId.substring(0, 4);
            prefs.edit().putString(KEY_MY_NAME, myName).apply();
        }

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

        // Back button: go back to previous screen
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
        // Assuming you already have NotificationHelper implemented
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
                    // Only show messages from this chat
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

        // Listen only to messages from this chat
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

        // Create message with chatId
        MessageModal message = new MessageModal(
                messageId,
                text,
                myId,
                myName,
                System.currentTimeMillis(),
                currentChatId  // Include chat ID
        );

        etMessage.setText("");

        // Send to Firebase
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
