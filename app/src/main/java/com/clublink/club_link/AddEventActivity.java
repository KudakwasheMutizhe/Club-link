package com.clublink.club_link;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.clublink.club_link.databinding.ActivityAddEventBinding;

public class AddEventActivity extends AppCompatActivity {

    private ActivityAddEventBinding b;
    private EventsRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAddEventBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        repo = new EventsRepository(this);

        // Simple title bar text if you want
        setTitle("Add Event");

        b.btnSave.setOnClickListener(v -> saveEvent());
    }

    private void saveEvent() {
        String title = b.editTitle.getText().toString().trim();
        String location = b.editLocation.getText().toString().trim();

        // Optional extras
        String club = b.editClub.getText().toString().trim();
        String category = b.editCategory.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            b.editTitle.setError("Title required");
            return;
        }

        // For now: event time = 1 hour from now
        long startTime = System.currentTimeMillis() + 60L * 60L * 1000L;

        Event e = new Event();
        e.title = title;
        e.location = location;
        e.club = club;
        e.category = category;
        e.startEpochMillis = startTime;
        e.imageUrl = "";        // no image yet
        e.isBookmarked = false;
        e.isGoing = false;

        long id = repo.insert(e);
        if (id != -1) {
            Toast.makeText(this, "Event added ", Toast.LENGTH_SHORT).show();
            finish();  // go back to EventsActivity
        } else {
            Toast.makeText(this, "Error saving event", Toast.LENGTH_SHORT).show();
        }
    }
}


