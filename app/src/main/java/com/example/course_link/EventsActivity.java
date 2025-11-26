package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.course_link.databinding.ActivityEventsBinding;

import java.util.List;

public class EventsActivity extends AppCompatActivity implements EventsAdapter.Callbacks {

    private ActivityEventsBinding b;
    private EventsAdapter adapter;
    private EventsRepository repo;

    private String currentQuery = "";
    private long currentDay = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        b = ActivityEventsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);

        // Repo + adapter
        repo = new EventsRepository(this);
        adapter = new EventsAdapter(this);
        b.recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        b.recyclerEvents.setAdapter(adapter);

        // Calendar day filter
        b.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            long selected = new java.util.GregorianCalendar(year, month, dayOfMonth).getTimeInMillis();
            showForDay(selected);
        });

        // --- START: CORRECTED NAVIGATION BLOCK ---

        // This flag prevents the listener from re-navigating when the screen first loads.
        final boolean[] isInitialSelection = {true};

        // Use the binding variable 'b.bottomNav' for the listener
        b.bottomNav.setOnItemSelectedListener(item -> {
            // If this is the first selection event (on screen load), ignore it.
            if (isInitialSelection[0]) {
                isInitialSelection[0] = false; // Mark as handled
                return true; // Consume the event, but do nothing.
            }

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(EventsActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_events) {
                // Already here, do nothing.
                return true;
            } else if (itemId == R.id.nav_messages) {
                Intent intent = new Intent(EventsActivity.this, DashboardActivity.class);
                intent.putExtra("CHAT_ID", "GLOBAL_CHAT");
                intent.putExtra("CHAT_NAME", "Messages");
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_announcements) {
                startActivity(new Intent(EventsActivity.this, AnnouncementsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                // CORRECTED: This should start the ProfileActivity activity
                startActivity(new Intent(EventsActivity.this, ProfileActivity.class));
                finish();
                return true;
            }

            return false;
        });

        // Set the selected item on the correct view AFTER setting the listener
        b.bottomNav.setSelectedItemId(R.id.nav_events);

        // --- END: CORRECTED NAVIGATION BLOCK ---

        // FAB – hook up to your "Create Event" flow
        b.fabAdd.setOnClickListener(v ->
                Toast.makeText(this, "TODO: Add Event", Toast.LENGTH_SHORT).show()
        );

        // Initial data – today
        showForDay(System.currentTimeMillis());
    }

    private void showForDay(long dayMillis) {
        currentDay = dayMillis;
        List<Event> events = repo.getOnDay(dayMillis);
        adapter.submitList(events);
    }

    private void searchAndDisplay(String q) {
        currentQuery = q == null ? "" : q.trim();
        // For simplicity, search ignores the calendar day filter; you can combine if desired.
        List<Event> results = repo.search(currentQuery, "All");
        adapter.submitList(results);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_events, menu);
        MenuItem mi = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) mi.getActionView();
        sv.setQueryHint("Search events, clubs, places…");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchAndDisplay(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchAndDisplay(newText);
                return true;
            }
        });
        return true;
    }

    // Callbacks from adapter
    @Override
    public void onRsvpClicked(Event e) {
        repo.toggleGoing(e.id, !e.isGoing);
        if (currentQuery.isEmpty()) {
            showForDay(currentDay);
        } else {
            searchAndDisplay(currentQuery);
        }
    }

    @Override
    public void onItemClicked(Event e) {
        Toast.makeText(this, e.title, Toast.LENGTH_SHORT).show();
    }
}
