package com.clublink.club_link;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AddAnnouncementActivity extends AppCompatActivity {
    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    // ⚠️ For demo / class only.
    // In a REAL app, never ship your FCM server key in the client.
    private static final String FCM_SERVER_KEY = "YOUR_SERVER_KEY_HERE";
    private static final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";

    private EditText etTitle;
    private EditText etMessage;
    private EditText etAuthor;
    private Button btnCancel;
    private MaterialButton btnPost;

    // 🔹 NEW: session + current user id
    private SessionManager sessionManager;
    private long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.add_announcement);

        // Handle system bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // 🔹 NEW: get current user from SessionManager
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        // If nobody logged in, send back to Login
        if (currentUserId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize views
        etTitle = findViewById(R.id.etTitle);
        etMessage = findViewById(R.id.etMessage);
        etAuthor = findViewById(R.id.etAuthor);
        btnCancel = findViewById(R.id.btnCancel);
        btnPost = findViewById(R.id.btnPost);

        // Set up listeners
        setupListeners();
    }

    private void setupListeners() {
        // Cancel button - go back without saving
        btnCancel.setOnClickListener(v -> finish());

        // Post button - validate and create announcement
        btnPost.setOnClickListener(v -> postAnnouncement());
    }

    private void postAnnouncement() {
        String title = etTitle.getText().toString().trim();
        String message = etMessage.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(message)) {
            etMessage.setError("Message is required");
            etMessage.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(author)) {
            etAuthor.setError("Author name is required");
            etAuthor.requestFocus();
            return;
        }

        // 🔴 OLD: global announcements node (everyone mixed together)
        // DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
        //        .getReference("announcements");

        // 🟢 NEW: scope announcements by user id
        // Path: /announcements/{userId}/{announcementId}
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("announcements")
                .child(String.valueOf(currentUserId));

        String key = ref.push().getKey();
        if (key == null) {
            Toast.makeText(this, "Something went wrong, try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();

        // 🔹 Use the updated AnnouncementModal with userId support
        AnnouncementModal a = new AnnouncementModal(
                key,
                currentUserId,   // tie this announcement to the logged-in account
                title,
                message,
                author,
                now,
                false
        );

        // Kick off the write
        ref.child(key).setValue(a)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Announcement posted!", Toast.LENGTH_SHORT).show();

                        // 🔔 You can still send a notification; topic can be global or per-user
                        // For per-user topics you could use "announcements_{currentUserId}"
                        sendNotificationToTopic(title, message, key);

                    } else {
                        Exception e = task.getException();
                        Toast.makeText(
                                this,
                                "Post failed" + (e != null ? (": " + e.getMessage()) : ""),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

        // Navigate immediately; the list screen will load it from DB and auto-open it
        Intent i = new Intent(this, AnnouncementsActivity.class);
        i.putExtra("new_id", key);
        startActivity(i);
        finish();
    }

    /**
     * Sends a push notification to /topics/announcements using FCM HTTP v1 legacy API.
     * NOTE: For production this belongs on a backend or Cloud Function, not inside the app.
     *
     * If you want notifications per account only, you can change the topic to something like
     * "/topics/announcements_{currentUserId}" and subscribe each logged-in user to that topic.
     */
    private void sendNotificationToTopic(String title, String body, String announcementId) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(FCM_API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "key=" + FCM_SERVER_KEY);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                // Build JSON payload
                JSONObject root = new JSONObject();

                // 🔴 Global topic:
                root.put("to", "/topics/announcements");

                // 🟢 If you want per-user:
                // root.put("to", "/topics/announcements_" + currentUserId);

                // Notification payload (what shows in the system tray)
                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", body);
                notification.put("click_action", "OPEN_ANNOUNCEMENT_DETAIL");
                root.put("notification", notification);

                // Data payload (custom stuff for your app)
                JSONObject data = new JSONObject();
                data.put("announcementId", announcementId);
                data.put("title", title);
                data.put("message", body);
                root.put("data", data);

                // Send
                OutputStream os = conn.getOutputStream();
                os.write(root.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                // (optional) log or inspect responseCode here

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
}
