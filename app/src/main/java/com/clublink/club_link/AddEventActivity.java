package com.clublink.club_link;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.clublink.club_link.databinding.ActivityAddEventBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private ActivityAddEventBinding b;
    private EventsRepository repo;

    // For images
    private Uri selectedImageUri = null;      // gallery image
    private String selectedBuiltinKey = null; // e.g. "builtin:preset1"

    // For date
    private long selectedDateMillis = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAddEventBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        repo = new EventsRepository(this);
        setTitle("Add Event");

        // 🔙 HARD REDIRECT: Back arrow → ALWAYS go to EventsActivity
        b.backArrow.setOnClickListener(v -> {
            Intent intent = new Intent(AddEventActivity.this, EventsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // --- Location autocomplete (internal list) ---
        String[] locations = getResources().getStringArray(R.array.campus_locations);
        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                locations
        );
        b.editLocation.setAdapter(locAdapter);
        b.editLocation.setThreshold(1);

        // --- Date picker ---
        b.txtDate.setOnClickListener(v -> showDatePicker());

        // --- Image: pick from phone ---
        b.btnPickImage.setOnClickListener(v -> pickImageFromPhone());

        // --- Image: pick from app presets ---
        b.imgPreset1.setOnClickListener(v ->
                selectBuiltin("builtin:preset1", R.drawable.event_preset1));

        b.imgPreset2.setOnClickListener(v ->
                selectBuiltin("builtin:preset2", R.drawable.event_preset2));

        b.imgPreset3.setOnClickListener(v ->
                selectBuiltin("builtin:preset3", R.drawable.event_preset3));

        // --- Save button ---
        b.btnSave.setOnClickListener(v -> saveEvent());
    }

    // ---------------- DATE PICKER ----------------

    private void showDatePicker() {
        final Calendar now = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar c = Calendar.getInstance();
                    // Set selected date, choose a default time (e.g. 12:00 noon)
                    c.set(year, month, dayOfMonth, 12, 0, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    selectedDateMillis = c.getTimeInMillis();

                    SimpleDateFormat fmt =
                            new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
                    b.txtDate.setText(fmt.format(c.getTime()));
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    // ---------------- IMAGE PICKER (PHONE) ----------------

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedImageUri = uri;
                            selectedBuiltinKey = null; // override preset if user picks phone image

                            Glide.with(this)
                                    .load(uri)
                                    .into(b.imgPreview);
                        }
                    });

    private void pickImageFromPhone() {
        pickImageLauncher.launch("image/*");
    }

    // ---------------- APP PRESET IMAGE SELECTION ----------------

    private void selectBuiltin(String key, int drawableRes) {
        selectedBuiltinKey = key;
        selectedImageUri = null; // override phone selection

        Glide.with(this)
                .load(drawableRes)
                .into(b.imgPreview);
    }

    // ---------------- SAVE EVENT ----------------

    private void saveEvent() {
        String title = b.editTitle.getText().toString().trim();
        String location = b.editLocation.getText().toString().trim();
        String club = b.editClub.getText().toString().trim();
        String category = b.editCategory.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            b.editTitle.setError("Title required");
            return;
        }

        // If the user picked a date, use that; else default to "now + 1 hour"
        long startTime;
        if (selectedDateMillis > 0) {
            startTime = selectedDateMillis;
        } else {
            startTime = System.currentTimeMillis() + 60L * 60L * 1000L;
        }

        Event e = new Event();
        e.title = title;
        e.location = location;
        e.club = club;
        e.category = category;
        e.startEpochMillis = startTime;

        // Decide what to store in imageUrl:
        if (selectedImageUri != null) {
            // user picked from phone
            e.imageUrl = selectedImageUri.toString();
        } else if (selectedBuiltinKey != null) {
            // user picked a preset
            e.imageUrl = selectedBuiltinKey;  // like "builtin:preset1"
        } else {
            // nothing picked
            e.imageUrl = "";
        }

        e.isBookmarked = false;
        e.isGoing = false;

        long id = repo.insert(e);
        if (id != -1) {
            Toast.makeText(this, "Event added ✅", Toast.LENGTH_SHORT).show();

            // After saving, also HARD REDIRECT to EventsActivity
            Intent intent = new Intent(AddEventActivity.this, EventsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            finish();
        } else {
            Toast.makeText(this, "Error saving event", Toast.LENGTH_SHORT).show();
        }
    }
}
