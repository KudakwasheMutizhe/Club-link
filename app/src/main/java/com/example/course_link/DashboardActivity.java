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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private String myId;
    private String myName;

    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_USERNAME = "logged_in_username";

    // Background thread
    private final ExecutorService chatExecutor = Executors.newSingleThreadExecutor();

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

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Chat info
        Intent intent = getIntent();
        currentChatId = intent.getStringExtra("CHAT_ID");
        currentChatName = intent.getStringExtra("CHAT_NAME");

        if (currentChatId == null) currentChatId = "GLOBAL_CHAT";
        if (currentChatName == null) currentChatName = "Messages";

        // User
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        if (currentUserId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        myId = String.valueOf(currentUserId);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        myName = prefs.getString(KEY_USERNAME, "User-" + myId);

        // Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        messagesRef = database.getReference("messages_v2");
        typingRef = database.getReference("typing").child(currentChatId);

        // Views
        recyclerView = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.message);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvChatName = findViewById(R.id.tvChatName);
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator);

        tvChatName.setText(currentChatName);
        tvTypingIndicator.setVisibility(android.view.View.GONE);

        btnBack.setOnClickListener(v -> finish());

        // Recycler + Adapter
        adapter = new MessageAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Typing
        typingHandler = new Handler(Looper.getMainLooper());
        setupTypingListener();
        setupTypingPublisher();

        setupFirebaseMessagesListener();

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
                chatExecutor.execute(() -> {
                    MessageModal msg = snapshot.getValue(MessageModal.class);
                    if (msg == null || msg.getId() == null) return;

                    if (!currentChatId.equals(msg.getChatId())) return;

                    if (messageIds.add(msg.getId())) {
                        messages.add(msg);

                        ArrayList<MessageModal> copy = new ArrayList<>(messages);
                        runOnUiThread(() -> {
                            adapter.submitList(copy);
                            recyclerView.scrollToPosition(copy.size() - 1);
                        });
                    }
                });
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String prev) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prev) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                chatExecutor.execute(() -> {
                    MessageModal msg = snapshot.getValue(MessageModal.class);
                    if (msg == null || msg.getId() == null) return;

                    messageIds.remove(msg.getId());
                    messages.removeIf(m -> m.getId().equals(msg.getId()));

                    ArrayList<MessageModal> copy = new ArrayList<>(messages);
                    runOnUiThread(() -> adapter.submitList(copy));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() ->
                        Toast.makeText(DashboardActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show()
                );
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

                    if (uid != null && typing != null && !uid.equals(myId) && typing) {
                        someoneTyping = true;
                        break;
                    }
                }

                tvTypingIndicator.setVisibility(
                        someoneTyping ? android.view.View.VISIBLE : android.view.View.GONE
                );
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        typingRef.addValueEventListener(typingListener);
    }

    private void setupTypingPublisher() {
        etMessage.addTextChangedListener(new TextWatcher() {

            private void setTyping(boolean typing) {
                typingRef.child(myId).setValue(typing);
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

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

            @Override public void afterTextChanged(Editable s) {}
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

        MessageModal msg = new MessageModal(
                id,
                text,
                myId,
                myName,
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

        chatExecutor.shutdown();
    }

    @Override
    public String getMyId() {
        return myId;
    }
}
