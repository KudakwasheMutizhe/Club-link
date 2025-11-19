package com.example.course_link;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;


import com.example.events.R;
import com.example.events.Event;
import com.example.events.EventsRepository;
import com.example.events.databinding.ActivityEventsBinding;


import java.util.List;

public class EventsActivity extends AppCompatActivity implements EventsAdapter.Callbacks{
    private ActivityEventsBinding b;
    private EventsAdapter adapter;
    private EventsRepository repo;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        b = ActivityEventsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());


        setSupportActionBar(b.toolbar);


        repo = new EventsRepository(this);
        adapter = new EventsAdapter(this);
        b.recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        b.recyclerEvents.setAdapter(adapter);


// Calendar day filter
        b.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            long selected = new java.util.GregorianCalendar(year, month, dayOfMonth).getTimeInMillis();
            showForDay(selected);
        });


// Bottom nav (stub destinations)
        b.bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_events) return true; // already here
            Toast.makeText(this, "TODO: navigate to " + item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        });
        // FAB – hook up to your "Create Event" flow
        b.fabAdd.setOnClickListener(v -> Toast.makeText(this, "TODO: Add Event", Toast.LENGTH_SHORT).show());


// Initial data – today
        showForDay(System.currentTimeMillis());
    }


    private String currentQuery = "";
    private long currentDay = -1L;


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


    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) { return super.onOptionsItemSelected(item); }


    @Override public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_events, menu);
        MenuItem mi = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) mi.getActionView();
        sv.setQueryHint("Search events, clubs, places…");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { searchAndDisplay(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { searchAndDisplay(newText); return true; }
        });
        return true;
    }


    // Callbacks from adapter
   /* @Override public void onBookmarkClicked(Event e) {
        repo.toggleBookmark(e.id, !e.isBookmarked);
        if (currentQuery.isEmpty()) showForDay(currentDay); else searchAndDisplay(currentQuery);
    }*/


    @Override public void onRsvpClicked(Event e) {
        repo.toggleGoing(e.id, !e.isGoing);
        if (currentQuery.isEmpty()) showForDay(currentDay); else searchAndDisplay(currentQuery);
    }
    @Override
    public void onItemClicked(Event e) {
        Toast.makeText(this, e.title, Toast.LENGTH_SHORT).show();
    }
}
