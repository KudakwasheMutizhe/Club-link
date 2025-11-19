package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;

public class DetailActivity extends AppCompatActivity {

    private ImageView imagedclubs, backImage;
    ;
    private TextView clubNameD, descriptionClubD;
    private MaterialButton btnEditClub;

    private String clubId;  // 🔹 store it here
    private static final String TAG = "DetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1️⃣ Bind views
        imagedclubs       = findViewById(R.id.imagedclubs);
        clubNameD         = findViewById(R.id.clubNameD);
        descriptionClubD  = findViewById(R.id.descriptionClubD);
        btnEditClub       = findViewById(R.id.btnEditClub);
        backImage = findViewById(R.id.backImage);

        backImage.setOnClickListener(v -> {
            // Go back to previous screen
            finish();
        });


        // 2️⃣ Read extras from adapter
        Intent intent      = getIntent();
        String clubName    = intent.getStringExtra("clubName");
        String description = intent.getStringExtra("shortDescription");
        String imageUrl    = intent.getStringExtra("imageUrl");
        clubId             = intent.getStringExtra("clubId");   // 🔹 important

        // 3️⃣ Populate UI
        if (clubName != null) {
            clubNameD.setText(clubName);
        }
        if (description != null) {
            descriptionClubD.setText(description);
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.image_1)
                    .error(R.drawable.image_1)
                    .into(imagedclubs);
        } else {
            imagedclubs.setImageResource(R.drawable.image_1);
        }

        // 4️⃣ Single, correct click listener
        btnEditClub.setOnClickListener(view -> {
            Intent editIntent = new Intent(DetailActivity.this, EditClubInfo.class);
            editIntent.putExtra("clubId", clubId);              // 🔹 pass ID
            editIntent.putExtra("clubName", clubName);
            editIntent.putExtra("shortDescription", description);
            editIntent.putExtra("imageUrl", imageUrl);
            startActivity(editIntent);
        });
    }
}
