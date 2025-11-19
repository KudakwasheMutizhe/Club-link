package com.example.club_link;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AnnouncementDetailActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvDate;
    private TextView tvMessage;
    private ImageButton btnBack;
    private ImageButton btnMore;
    private Button btnShare;
    private Button btnBookmark;

    private String title;
    private String message;
    private String authorName;
    private long createdAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_announcement_detail);

        // Handle system bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Initialize views
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDate = findViewById(R.id.tvDate);
        tvMessage = findViewById(R.id.tvMessage);
        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);
        btnShare = findViewById(R.id.btnShare);
        btnBookmark = findViewById(R.id.btnBookmark);

        // Get data from intent
        Intent intent = getIntent();
        if (intent != null) {
            title = intent.getStringExtra("announcement_title");
            message = intent.getStringExtra("announcement_message");
            authorName = intent.getStringExtra("announcement_author");
            createdAt = intent.getLongExtra("announcement_date", System.currentTimeMillis());

            // Display data
            displayAnnouncement();
        }

        // Set up button listeners
        btnBack.setOnClickListener(v -> finish());
        btnMore.setOnClickListener(v ->
                Toast.makeText(this, "More options", Toast.LENGTH_SHORT).show()
        );

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            shareIntent.putExtra(Intent.EXTRA_TEXT, message + "\n\n- Posted by " + authorName);
            startActivity(Intent.createChooser(shareIntent, "Share announcement via"));
        });

        btnBookmark.setOnClickListener(v ->
                Toast.makeText(this, "Announcement saved!", Toast.LENGTH_SHORT).show()
        );
    }

    private void displayAnnouncement() {
        tvTitle.setText(title);
        tvAuthor.setText("Posted by " + authorName);
        tvDate.setText(formatTimeAgo(createdAt));
        tvMessage.setText(message);
    }

    // Format timestamp to relative time
    private String formatTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (seconds < 60) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        if (days == 1) return "Yesterday";
        if (days < 7) return days + "d ago";

        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(new Date(timestamp));
    }
}
