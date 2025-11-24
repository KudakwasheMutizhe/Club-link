package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import android.graphics.Color;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import android.view.ContextThemeWrapper;

<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;




import android.view.ContextThemeWrapper;     //  import for rounded chip style

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
<<<<<<< Updated upstream
=======

import com.google.android.material.bottomnavigation.BottomNavigationView;

>>>>>>> Stashed changes
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private SearchView searchView;
    private ChipGroup chipGroupCategories;

    private ClubAdabter clubAdabter;
    private LinearLayoutManager linearLayoutManager;

    // Data
    private ArrayList<ClubModal> fullClubList = new ArrayList<>();
    private String activeCategoryFilter = ""; // empty = no category filter

    // Firebase
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    MaterialCardView mcvAddNewClub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

<<<<<<< Updated upstream
        // Firebase init
=======

        // Bottom Navigation setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

// This flag prevents the listener from re-navigating when the screen first loads.
        final boolean[] isInitialSelection = {true};

// 1. Set the listener FIRST
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // If this is the first selection event (on screen load), ignore it.
            if (isInitialSelection[0]) {
                isInitialSelection[0] = false; // Mark as handled
                return true; // Consume the event, but do nothing.
            }

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on the home screen, do nothing.
                return true;
            } else if (itemId == R.id.nav_events) {
                startActivity(new Intent(MainActivity.this, EventsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                Intent intent = new Intent(MainActivity.this, ChatListActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_announcements) {
                startActivity(new Intent(MainActivity.this, AnnouncementsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, profile.class));
                finish();
                return true;
            }

            return false;
        });

// Set the selected item SECOND. This will trigger the listener one time.
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // --- END: REPLACEMENT CODE BLOCK ---


        // Initialize Firebase Database instance and reference to "CourseInfo" node

>>>>>>> Stashed changes
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("ClubInfo");

        // Bind views
        recyclerView = findViewById(R.id.clubRV);
        searchView = findViewById(R.id.searchView);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        mcvAddNewClub = findViewById(R.id.mcvAddNewClub);

        mcvAddNewClub.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddNewClubs.class);
            startActivity(intent);
        });

        // Setup RecyclerView + Adapter
        clubAdabter = new ClubAdabter(this, new ArrayList<>());
        recyclerView.setAdapter(clubAdabter);
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        // SearchView behavior
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // We already filter as user types
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });

        // Setup category chips ("All", Academic, Sports, ...)
        setupCategoryChips();

        // Load clubs from Firebase
        fetchClubsFromFirebase();
    }

    /**
     * Create rounded category chips and hook up filter logic.
     */
    private void setupCategoryChips() {

        // Main blue color for the theme
        String BLUE = "#3B82F6";          // modern blue
        String BLUE_DARK = "#1D4ED8";     // darker blue (for text if you want)
        String TEXT_DARK = "#0F172A";     // near-black for fallback

        // Background colors for checked / unchecked
        ColorStateList chipBgColors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},     // checked
                        new int[]{-android.R.attr.state_checked}     // unchecked
                },
                new int[]{
                        Color.parseColor(BLUE),                      // selected bg (blue)
                        Color.parseColor("#FFFFFF")                  // unselected bg (white)
                }
        );

        // Text colors for checked / unchecked
        ColorStateList chipTextColors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        Color.parseColor("#FFFFFF"),                 // selected text (white)
                        Color.parseColor(BLUE_DARK)                  // unselected text (dark blue)
                }
        );

        // Stroke (border) color – blue outline
        ColorStateList chipStrokeColors = ColorStateList.valueOf(
                Color.parseColor(BLUE)                              // blue border
        );

        // "All" chip
        Chip allChip = new Chip(
                new ContextThemeWrapper(this, R.style.RoundedChipStyle),
                null,
                0
        );
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setChecked(true);
        allChip.setChipBackgroundColor(chipBgColors);
        allChip.setTextColor(chipTextColors);
        allChip.setChipStrokeColor(chipStrokeColors);
        allChip.setChipStrokeWidth(1f);
        chipGroupCategories.addView(allChip);

        // Other category chips
        String[] categories = getResources().getStringArray(R.array.club_categories);
        for (String cat : categories) {
            Chip chip = new Chip(
                    new ContextThemeWrapper(this, R.style.RoundedChipStyle),
                    null,
                    0
            );
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChipBackgroundColor(chipBgColors);
            chip.setTextColor(chipTextColors);
            chip.setChipStrokeColor(chipStrokeColors);
            chip.setChipStrokeWidth(1f);
            chipGroupCategories.addView(chip);
        }

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                activeCategoryFilter = "";
            } else {
                int id = checkedIds.get(0);
                Chip selectedChip = group.findViewById(id);
                if (selectedChip != null) {
                    String text = selectedChip.getText().toString();
                    activeCategoryFilter = text.equalsIgnoreCase("All") ? "" : text;
                }
            }

            String currentQuery = searchView.getQuery() != null
                    ? searchView.getQuery().toString()
                    : "";
            filterList(currentQuery);
        });
    }




    /**
     * Fetch clubs from Firebase Realtime Database and update list.
     */
    private void fetchClubsFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullClubList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ClubModal clubModal = dataSnapshot.getValue(ClubModal.class);
                    if (clubModal != null) {
                        Log.d(TAG, "Loaded club: id=" + clubModal.getClubId()
                                + " name=" + clubModal.getClubName()
                                + " category=" + clubModal.getCategory()
                                + " imagePath=" + clubModal.getImagePath());
                        fullClubList.add(clubModal);
                    }
                }

                // Initially show all clubs
                clubAdabter.setFilteredList(new ArrayList<>(fullClubList));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Failed to load data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Filter list based on text query + active category filter.
     */
    private void filterList(String newText) {
        if (clubAdabter == null) return;

        String query = (newText == null) ? "" : newText.trim().toLowerCase();
        String categoryFilterLower = activeCategoryFilter == null
                ? ""
                : activeCategoryFilter.trim().toLowerCase();

        // Nothing typed & no category filter → show all
        if (query.isEmpty() && categoryFilterLower.isEmpty()) {
            clubAdabter.setFilteredList(new ArrayList<>(fullClubList));
            return;
        }

        ArrayList<ClubModal> filteredList = new ArrayList<>();

        for (ClubModal club : fullClubList) {
            if (club == null) continue;

            String name = club.getClubName() != null
                    ? club.getClubName().toLowerCase()
                    : "";
            String desc = club.getShortDescription() != null
                    ? club.getShortDescription().toLowerCase()
                    : "";
            String category = club.getCategory() != null
                    ? club.getCategory().toLowerCase()
                    : "";

            boolean matchesText =
                    query.isEmpty()
                            || name.contains(query)
                            || desc.contains(query)
                            || category.contains(query);

            boolean matchesCategory =
                    categoryFilterLower.isEmpty()
                            || category.equals(categoryFilterLower);

            if (matchesText && matchesCategory) {
                filteredList.add(club);
            }
        }

        clubAdabter.setFilteredList(filteredList);
    }
}
