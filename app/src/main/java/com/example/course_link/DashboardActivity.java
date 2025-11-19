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
import android.widget.Button;          // ✅ NEW
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

/**
 * DashboardActivity - Real-time Firebase chat with push notifications
 * Now supports multiple chats
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

        // Handle system bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Get chat info from intent
        Intent intent = getIntent();
        currentChatId = intent.getStringExtra("CHAT_ID");
        currentChatName = intent.getStringExtra("CHAT_NAME");

        if (currentChatId == null || currentChatName == null) {
            Toast.makeText(this, "Error: No chat selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // User identity
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

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("messages_v2");

        // Hook views
        recyclerView = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.message);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvChatName = findViewById(R.id.tvChatName);

        // Set chat name in header
        tvChatName.setText(currentChatName);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Set up RecyclerView
        adapter = new MessageAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Send message on button tap
        btnSend.setOnClickListener(v -> sendMessage());

        // Send on keyboard 'Send' action
        etMessage.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Listen for messages in this chat only
        setupFirebaseListener();

        // Request notification permission
        requestNotificationPermission();
    }

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

        // Load messages for this chat only
        messagesRef.orderByChild("chatId")
                .equalTo(currentChatId)
                .addChildEventListener(messagesListener);
    }

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

    /**
     * ✅ Bottom navigation: Messages / Announcements / Profile
     * Requires:
     *  - Button IDs: btnNavMessages, btnNavAnnouncements, btnNavProfile in activity_messages.xml
     *  - Activities: AnnouncementsActivity, ProfileActivity
     */
    private void setupBottomNav() {
        Button btnNavMessages = findViewById(R.id.btnMessages);
        Button btnNavAnnouncements = findViewById(R.id.btnAnnouncements);
        Button btnNavProfile = findViewById(R.id.btnProfile);

        // Messages tab – we are already on Messages, so do nothing
        btnNavMessages.setOnClickListener(v -> {
            // Optional: scroll to bottom of chat if you want
            if (!messages.isEmpty()) {
                recyclerView.scrollToPosition(messages.size() - 1);
            }
        });

        // Announcements tab
        btnNavAnnouncements.setOnClickListener(v -> {
            Intent i = new Intent(DashboardActivity.this, AnnouncementsActivity.class);
            // If you want to pass user or chat info, add extras here
            // i.putExtra("MY_ID", myId);
            startActivity(i);
        });

        // Profile tab
        btnNavProfile.setOnClickListener(v -> {
            Intent i = new Intent(DashboardActivity.this, profile.class);
            // i.putExtra("MY_ID", myId);
            startActivity(i);
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
