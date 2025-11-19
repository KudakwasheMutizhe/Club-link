package com.example.club_link;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.UUID;
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

                    // Add to the beginning of the list (newest first)
                    announcements.add(0, newAnnouncement);

                    // Update UI
                    updateAnnouncementsList();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_announcements);
        justCreatedId = getIntent().getStringExtra("new_id");

        // Handle system bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Initialize views
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
            addAnnouncementLauncher.launch(intent);
        });

        // Load announcements
        loadAnnouncements();
    }

    private void loadAnnouncements() {
        dbRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("announcements");

// Listen for all announcements ordered by time (newest first in code)
        dbRef.orderByChild("createdAt").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                announcements.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AnnouncementModal a = child.getValue(AnnouncementModal.class);
                    if (a != null) announcements.add(a);
                }
                // newest first
                announcements.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                updateAnnouncementsList();

                // If we just created one, scroll to it and open detail once
                if (justCreatedId != null) {
                    int index = IntStream.range(0, announcements.size()).filter(i -> justCreatedId.equals(announcements.get(i).getId())).findFirst().orElse(-1);
                    if (index >= 0) {
                        recyclerView.scrollToPosition(index);
                        recyclerView.postDelayed(() -> {
                            onAnnouncementClick(announcements.get(index));
                        }, 3000); // wait 600ms before opening detail
                    }
                    justCreatedId = null;

                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { /* show toast if you like */ }
        });

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
                System.currentTimeMillis() - 86400000, // 1 day ago (Yesterday)
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
        // Mark as read
        announcement.setRead(true);

        // Refresh the list to update UI (removes bold, changes appearance)
        adapter.submitList(new ArrayList<>(announcements));

        // Navigate to AnnouncementDetailActivity to show full content
        Intent intent = new Intent(this, AnnouncementDetailActivity.class);
        intent.putExtra("announcement_id", announcement.getId());
        intent.putExtra("announcement_title", announcement.getTitle());
        intent.putExtra("announcement_message", announcement.getMessage());
        intent.putExtra("announcement_author", announcement.getAuthorName());
        intent.putExtra("announcement_date", announcement.getCreatedAt());
        startActivity(intent);
    }
}
