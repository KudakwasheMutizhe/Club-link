package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class AnnouncementsActivity extends AppCompatActivity implements AnnouncementAdapter.OnAnnouncementClickListener {

    private RecyclerView recyclerView;
    private AnnouncementAdapter adapter;
    private LinearLayout emptyState;
    private FloatingActionButton fabAddAnnouncement;
    private DatabaseReference dbRef;
    private String justCreatedId = null;

    private final ArrayList<AnnouncementModal> announcements = new ArrayList<>();
    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    // Session + current user id
    private SessionManager sessionManager;
    private long currentUserId;

    // Background executor for announcement list work
    private final ExecutorService announcementsExecutor = Executors.newSingleThreadExecutor();

    // Activity result launcher for adding announcements
    private final ActivityResultLauncher<Intent> addAnnouncementLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    // Create announcement from returned data
                    AnnouncementModal newAnnouncement = new AnnouncementModal(
                            data.getStringExtra("announcement_id"),
                            data.getStringExtra("announcement_title"),
                            data.getStringExtra("announcement_message"),
                            data.getStringExtra("announcement_author"),
                            data.getLongExtra("announcement_date", System.currentTimeMillis()),
                            data.getBooleanExtra("announcement_is_read", false)
                    );

                    // Add on background thread, then refresh UI on main
                    announcementsExecutor.execute(() -> {
                        announcements.add(0, newAnnouncement);
                        runOnUiThread(this::updateAnnouncementsList);
                    });
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_announcements);
        justCreatedId = getIntent().getStringExtra("new_id");

        // Get current user from session
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        // If no user logged in, send back to Login
        if (currentUserId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Handle system bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // ---------- Bottom Navigation ----------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {

            // This flag prevents the listener from re-navigating when the screen first loads.
            final boolean[] isInitialSelection = {true};

            bottomNavigationView.setOnItemSelectedListener(item -> {
                // If this is the first selection event (on screen load), ignore it.
                if (isInitialSelection[0]) {
                    isInitialSelection[0] = false; // Mark as handled
                    return true; // Consume the event, but do nothing.
                }

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(AnnouncementsActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_events) {
                    startActivity(new Intent(AnnouncementsActivity.this, EventsActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    Intent intent = new Intent(AnnouncementsActivity.this, ChatListActivity.class);
                    intent.putExtra("CHAT_ID", "GLOBAL_CHAT");
                    intent.putExtra("CHAT_NAME", "Messages");
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_announcements) {
                    // already here
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(AnnouncementsActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }

                return false;
            });

            bottomNavigationView.setSelectedItemId(R.id.nav_announcements);  // highlight Announcements tab
        }

        // ---------- Initialize views ----------
        recyclerView = findViewById(R.id.rvAnnouncements);
        emptyState = findViewById(R.id.emptyState);
        fabAddAnnouncement = findViewById(R.id.fabAddAnnouncement);

        // Set up RecyclerView
        adapter = new AnnouncementAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // FAB click listener to add new announcement
        fabAddAnnouncement.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddAnnouncementActivity.class);
            // optional: pass currentUserId if you want
            intent.putExtra("user_id", currentUserId);
            addAnnouncementLauncher.launch(intent);
        });

        // Load announcements from Firebase
        loadAnnouncements();
    }

    private void loadAnnouncements() {
        // per-user announcements: /announcements/{userId}/{announcementId}
        dbRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("announcements")
                .child(String.valueOf(currentUserId));

        // Listen for this user's announcements ordered by time
        dbRef.orderByChild("createdAt").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                announcementsExecutor.execute(() -> {
                    announcements.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        AnnouncementModal a = child.getValue(AnnouncementModal.class);
                        if (a != null) announcements.add(a);
                    }

                    // newest first
                    announcements.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    // capture and reset justCreatedId safely for this UI run
                    String createdId = justCreatedId;
                    justCreatedId = null;

                    runOnUiThread(() -> refreshUiAfterLoad(createdId));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Optional: show a toast here
            }
        });
    }

    /**
     * Runs on main thread after announcements list has been updated in background.
     */
    private void refreshUiAfterLoad(String createdId) {
        updateAnnouncementsList();

        // If we just created one, scroll to it and open detail once
        if (createdId != null && !announcements.isEmpty()) {
            int index = IntStream.range(0, announcements.size())
                    .filter(i -> createdId.equals(announcements.get(i).getId()))
                    .findFirst()
                    .orElse(-1);

            if (index >= 0) {
                recyclerView.scrollToPosition(index);
                recyclerView.postDelayed(() -> {
                    onAnnouncementClick(announcements.get(index));
                }, 600); // small delay before opening detail
            }
        }
    }

    private void updateAnnouncementsList() {
        if (announcements.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(new ArrayList<>(announcements));
        }
    }

    // Just sample data helper if you ever want it (currently unused)
    private void addSampleAnnouncements() {
        // Sample announcement 1 (unread) - 2 hours ago
        announcements.add(new AnnouncementModal(
                UUID.randomUUID().toString(),
                "Meeting Reminder",
                "Don't forget about our general meeting tomorrow at 5 PM in the student center.",
                "Admin",
                System.currentTimeMillis() - 7200000, // 2 hours ago
                false // unread
        ));

        // Sample announcement 2 (read) - Yesterday
        announcements.add(new AnnouncementModal(
                UUID.randomUUID().toString(),
                "Volunteer Event Signup",
                "Sign up to volunteer at the community clean-up this Saturday! More details attached.",
                "Events Coordinator",
                System.currentTimeMillis() - 86400000, // 1 day ago
                true // read
        ));

        // Sample announcement 3 (unread) - 2 days ago
        announcements.add(new AnnouncementModal(
                UUID.randomUUID().toString(),
                "Important Update",
                "We've updated our club's constitution. Please review the new document.",
                "Club President",
                System.currentTimeMillis() - 172800000, // 2 days ago
                false // unread
        ));
    }

    @Override
    public void onAnnouncementClick(AnnouncementModal announcement) {
        // Mark as read in memory (on main thread, safe)
        announcement.setRead(true);

        // Refresh the list to update UI
        adapter.submitList(new ArrayList<>(announcements));

        // Navigate to detail screen
        Intent intent = new Intent(this, AnnouncementDetailActivity.class);
        intent.putExtra("announcement_id", announcement.getId());
        intent.putExtra("announcement_title", announcement.getTitle());
        intent.putExtra("announcement_message", announcement.getMessage());
        intent.putExtra("announcement_author", announcement.getAuthorName());
        intent.putExtra("announcement_date", announcement.getCreatedAt());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        announcementsExecutor.shutdown();
    }
}
