package com.example.course_link;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddNewClubs extends AppCompatActivity {

    private static final String TAG = "AddNewClubs";

    CardView cameraCard, photoCard;
    ImageView imageView, backImage;

    Uri imageUri;
    ClubModal clubModal;

    EditText edtClubName, edtClubDescription;

    MaterialButton btnSubmit, btnCancel;
    AutoCompleteTextView actCategory;

    private DatabaseReference databaseReference;

    // Activity Result launchers (replace startActivityForResult)
    private ActivityResultLauncher<Void> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    // Threading helpers
    private ExecutorService ioExecutor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_new_clubs);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ---------- Bottom Navigation ----------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);

        final boolean[] isInitialSelection = {true};

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (isInitialSelection[0]) {
                isInitialSelection[0] = false;
                return true;
            }

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // From "Add New Club" → go back to main clubs list
                Intent intent = new Intent(AddNewClubs.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_events) {
                startActivity(new Intent(AddNewClubs.this, EventsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                startActivity(new Intent(AddNewClubs.this, ChatListActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_announcements) {
                startActivity(new Intent(AddNewClubs.this, AnnouncementsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(AddNewClubs.this, profile.class));
                finish();
                return true;
            }

            return false;
        });

        // Highlight the current tab (home / clubs)
        bottomNavigationView.setSelectedItemId(R.id.nav_home);


        // Threading init
        ioExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        cameraCard = findViewById(R.id.cameraCard);
        photoCard = findViewById(R.id.photoCard);
        imageView = findViewById(R.id.imageView4);
        edtClubName = findViewById(R.id.edtClubNameAdd);
        edtClubDescription = findViewById(R.id.edtClubDescriptionAdd);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnCancel = findViewById(R.id.btnCancel);
        actCategory = findViewById(R.id.actCategory);
        backImage = findViewById(R.id.backImage);

        backImage.setOnClickListener(v -> finish());

        btnCancel.setOnClickListener(v -> finish());

        // Load categories from resources
        String[] categories = getResources().getStringArray(R.array.club_categories);

        // Create adapter
        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);

        // Attach adapter to AutoCompleteTextView
        actCategory.setAdapter(categoryAdapter);

        // Initialize Firebase Database instance and get reference to "ClubInfo" node
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("ClubInfo");

        clubModal = new ClubModal();

        // Set click listener for 'Add Club' button
        btnSubmit.setOnClickListener(view -> {
            String clubName = edtClubName.getText().toString().trim();
            String clubDescription = edtClubDescription.getText().toString().trim();
            String selectedCategory = actCategory.getText().toString().trim();

            if (clubName.isEmpty() || clubDescription.isEmpty() || selectedCategory.isEmpty()) {
                Toast.makeText(AddNewClubs.this, "Please enter all the data..", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate a clubId here so we can also use it in Storage path
            String clubId = databaseReference.push().getKey();
            if (clubId == null) {
                Toast.makeText(AddNewClubs.this, "Error generating club ID", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false); // avoid double-tap

            if (imageUri != null) {
                // Upload image first, then save club with download URL
                uploadImageAndSaveClub(clubId, clubName, clubDescription, selectedCategory);
            } else {
                // No image selected, just save text data
                addDatatoFirebase(clubId, clubName, clubDescription, selectedCategory, "");
            }
        });

        // Prefer WindowInsetsControllerCompat over deprecated FLAG_FULLSCREEN
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.hide(WindowInsetsCompat.Type.systemBars());
        insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        // Register the Activity Result launchers
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                (Bitmap bitmap) -> {
                    if (bitmap != null) {
                        // Offload gallery save to background thread
                        ioExecutor.execute(() -> {
                            Uri saved = saveImageToGallery(bitmap);

                            // Switch back to main thread for UI updates
                            mainHandler.post(() -> {
                                if (isFinishing() || isDestroyed()) return;

                                if (saved != null) {
                                    imageUri = saved;   // used later for upload
                                }
                                // Show preview regardless of save success
                                imageView.setImageBitmap(bitmap);
                            });
                        });
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (Uri uri) -> {
                    if (uri != null) {
                        imageUri = uri;
                        imageView.setImageURI(uri);
                    }
                }
        );

        cameraCard.setOnClickListener(view -> {
            // Launch camera preview-taking contract (returns a Bitmap)
            takePictureLauncher.launch(null);
        });

        photoCard.setOnClickListener(view -> {
            // Launch gallery picker for images
            pickImageLauncher.launch("image/*");
        });
    }

    private void uploadImageAndSaveClub(String clubId,
                                        String name,
                                        String description,
                                        String category) {

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("clubImages")
                .child(clubId + ".jpg");

        storageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    addDatatoFirebase(clubId, name, description, category, downloadUrl);
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(AddNewClubs.this,
                            "Image upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private Uri saveImageToGallery(Bitmap bitmap) {
        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                "MyImage_" + System.currentTimeMillis(),
                "Image captured by camera"
        );

        if (savedImageURL != null) {
            Uri savedImageUri = Uri.parse(savedImageURL);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, savedImageUri));
            return savedImageUri;
        } else {
            return null;
        }
    }

    // Method to add club data to Firebase
    private void addDatatoFirebase(String clubId,
                                   String name,
                                   String description,
                                   String category,
                                   String imageUrl) {

        clubModal.setClubId(clubId);
        clubModal.setClubName(name);
        clubModal.setShortDescription(description);
        clubModal.setImagePath(imageUrl);
        clubModal.setCategory(category);   // ⬅️ make sure ClubModal has this

        databaseReference.child(clubId).setValue(clubModal)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(AddNewClubs.this,
                            "Club added successfully",
                            Toast.LENGTH_SHORT
                    ).show();
                    clearInputFields();
                    imageView.setImageDrawable(null);
                    imageUri = null;
                    btnSubmit.setEnabled(true);

                    finish();  // return to previous activity
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(AddNewClubs.this,
                            "Failed to add club: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // Method to clear input fields
    private void clearInputFields() {
        edtClubName.setText("");
        edtClubDescription.setText("");
        actCategory.setText("", false); // clear selected category
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
        }
    }
}
