package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.course_link.databinding.ActivityEventDetailsBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class EventDetailsActivity extends AppCompatActivity {
    private ActivityEventDetailsBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityEventDetailsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        long id = getIntent().getLongExtra("event_id", -1);

        // ---------- Bottom Navigation ----------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            final boolean[] isInitialSelection = {true};

            bottomNavigationView.setOnItemSelectedListener(item -> {
                if (isInitialSelection[0]) {
                    isInitialSelection[0] = false;
                    return true;
                }

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(EventDetailsActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_events) {
                    startActivity(new Intent(EventDetailsActivity.this, EventsActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    // already here
                    return true;
                } else if (itemId == R.id.nav_announcements) {
                    startActivity(new Intent(EventDetailsActivity.this, AnnouncementsActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(EventDetailsActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }

                return false;
            });

            bottomNavigationView.setSelectedItemId(R.id.nav_messages);
        }
// TODO: query repo by id and render
    }


}
