package com.example.events;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.events.databinding.ActivityEventDetailsBinding;


public class EventDetailsActivity extends AppCompatActivity {
    private ActivityEventDetailsBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityEventDetailsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        long id = getIntent().getLongExtra("event_id", -1);
// TODO: query repo by id and render
    }
}
