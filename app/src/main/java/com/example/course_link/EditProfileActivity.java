package com.example.club_link;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etDisplayName;
    private EditText etBio;
    private Spinner spinnerCampus;
    private Button btnCancel;
    private Button btnSave;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String userId;

    // Campus options
    private final String[] campusOptions = {
            "Main Campus",
            "North Campus",
            "South Campus",
            "East Campus",
            "West Campus",
            "Online Campus"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.edit_profile);

        // Handle system bars padding
        ViewCompat.setOnApplyWindowInsetsListener
                (findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
            usersRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        } else {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        etDisplayName = findViewById(R.id.etDisplayName);
        etBio = findViewById(R.id.etBio);
        spinnerCampus = findViewById(R.id.spinnerCampus);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);

        // Set up campus spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                campusOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCampus.setAdapter(adapter);

        // Load current profile data
        loadCurrentData();

        // Set up listeners
        setupListeners();
    }

    private void loadCurrentData() {
        // Get data passed from ProfileActivity
        String displayName = getIntent().getStringExtra("displayName");
        String bio = getIntent().getStringExtra("bio");
        String campus = getIntent().getStringExtra("campus");

        if (displayName != null) {
            etDisplayName.setText(displayName);
        }

        if (bio != null && !bio.equals("No bio yet")) {
            etBio.setText(bio);
        }

        if (campus != null) {
            // Set spinner selection
            for (int i = 0; i < campusOptions.length; i++) {
                if (campusOptions[i].equals(campus)) {
                    spinnerCampus.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupListeners() {
        // Cancel button
        btnCancel.setOnClickListener(v -> finish());

        // Save button
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        // Get input values
        String displayName = etDisplayName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String campus = spinnerCampus.getSelectedItem().toString();

        // Validate display name
        if (TextUtils.isEmpty(displayName)) {
            etDisplayName.setError("Name is required");
            etDisplayName.requestFocus();
            return;
        }

        // Disable button during save
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Create profile data map
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("displayName", displayName);
        profileData.put("bio", bio);
        profileData.put("campus", campus);
        profileData.put("updatedAt", System.currentTimeMillis());

        // Save to Firebase
        usersRef.updateChildren(profileData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Notify ProfileActivity to reload
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                });
    }
}