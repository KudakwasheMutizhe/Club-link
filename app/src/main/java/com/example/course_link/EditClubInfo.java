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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditClubInfo extends AppCompatActivity {

    private static final String TAG = "EditClubInfo";

    private CardView cameraCard, photoCard;
    private ImageView imageView4, backImage;
    private EditText edtClubNameAdd;
    private TextInputLayout tilDescription, tilCategory;
    private TextInputEditText edtClubDescriptionAdd;
    private AutoCompleteTextView actCategory;
    private MaterialButton btnSubmit, btnCancel, btnDelete;

    private Uri imageUri = null;          // new selected image (if any)
    private String originalImageUrl = ""; // existing Firebase Storage URL
    private String clubId;                // Firebase key
    private String originalCategory = ""; // existing category

    private DatabaseReference clubRef;

    // Activity Result launchers
    private ActivityResultLauncher<Void> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    // Threading helpers
    private ExecutorService ioExecutor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_club_info);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ---------- Bottom Navigation ----------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);

        // Prevents the listener from firing navigation on initial selection
        final boolean[] isInitialSelection = {true};

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (isInitialSelection[0]) {
                isInitialSelection[0] = false;
                return true;
            }

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // From Edit → go back to main clubs list
                Intent intent = new Intent(EditClubInfo.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_events) {
                startActivity(new Intent(EditClubInfo.this, EventsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                startActivity(new Intent(EditClubInfo.this, ChatListActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_announcements) {
                startActivity(new Intent(EditClubInfo.this, AnnouncementsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(EditClubInfo.this, ProfileActivity.class));
                finish();
                return true;
            }

            return false;
        });

        // Highlight the current tab (home/clubs)
        bottomNavigationView.setSelectedItemId(R.id.nav_home);


        // Threading init
        ioExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        bindViews();
        initFirebase();
        setupCategoryDropdown();
        readIntentExtras();
        setupImagePickers();
        setupButtons();

        backImage.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        cameraCard            = findViewById(R.id.cameraCard);
        photoCard             = findViewById(R.id.photoCard);
        imageView4            = findViewById(R.id.imageView4);
        edtClubNameAdd        = findViewById(R.id.edtClubNameAdd);
        tilDescription        = findViewById(R.id.tilDescription);
        edtClubDescriptionAdd = findViewById(R.id.edtClubDescriptionAdd);
        tilCategory           = findViewById(R.id.tilCategory);
        actCategory           = findViewById(R.id.actCategory);
        btnSubmit             = findViewById(R.id.btnSubmit);
        btnCancel             = findViewById(R.id.btnCancel);
        btnDelete             = findViewById(R.id.btnDelete);
        backImage             = findViewById(R.id.backImage);
    }

    private void initFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference rootRef = database.getReference("ClubInfo");
        // clubRef will be set properly once we know clubId (in readIntentExtras)
        clubRef = rootRef;
    }

    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(R.array.club_categories);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        actCategory.setAdapter(adapter);

        // make dropdown open when clicked
        actCategory.setOnClickListener(v -> actCategory.showDropDown());
    }

    private void readIntentExtras() {
        Intent intent      = getIntent();
        clubId             = intent.getStringExtra("clubId");
        String clubName    = intent.getStringExtra("clubName");
        String desc        = intent.getStringExtra("shortDescription");
        originalImageUrl   = intent.getStringExtra("imageUrl");
        originalCategory   = intent.getStringExtra("category");

        if (clubId == null || clubId.isEmpty()) {
            Toast.makeText(this, "Missing club ID, cannot edit.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Now point directly at /ClubInfo/<clubId>
        clubRef = clubRef.child(clubId);

        // Prefill fields
        if (clubName != null) {
            edtClubNameAdd.setText(clubName);
        }
        if (desc != null) {
            edtClubDescriptionAdd.setText(desc);
        }
        if (originalCategory != null && !originalCategory.isEmpty()) {
            actCategory.setText(originalCategory, false);
        }

        // Load existing image
        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(originalImageUrl)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView4);
        }
    }

    private void setupImagePickers() {
        // Camera: TakePicturePreview -> Bitmap -> save to gallery on background thread -> Uri
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                (Bitmap bitmap) -> {
                    if (bitmap != null) {

                        // Offload saving to gallery to background thread
                        ioExecutor.execute(() -> {
                            Uri saved = saveImageToGallery(bitmap);

                            // Back to main thread for UI updates
                            mainHandler.post(() -> {
                                if (isFinishing() || isDestroyed()) return;

                                if (saved != null) {
                                    imageUri = saved;  // new image chosen
                                }
                                // Show preview regardless of save success
                                imageView4.setImageBitmap(bitmap);
                            });
                        });
                    }
                }
        );

        // Gallery: pick from device (already async)
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (Uri uri) -> {
                    if (uri != null) {
                        imageUri = uri;              // new image chosen
                        imageView4.setImageURI(uri);
                    }
                }
        );

        cameraCard.setOnClickListener(v -> takePictureLauncher.launch(null));
        photoCard.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String name             = edtClubNameAdd.getText().toString().trim();
            String desc             = edtClubDescriptionAdd.getText().toString().trim();
            String selectedCategory = actCategory.getText().toString().trim();

            if (name.isEmpty() || desc.isEmpty() || selectedCategory.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false);

            // If a new image was selected, upload it first
            if (imageUri != null) {
                uploadImageAndUpdateClub(name, desc, selectedCategory);
            } else {
                // No new image → keep original URL
                updateClubInFirebase(name, desc, originalImageUrl, selectedCategory);
            }
        });

        // Delete button
        btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(EditClubInfo.this)
                    .setTitle("Delete club")
                    .setMessage("Are you sure you want to delete this club?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteClub())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Upload the new image to Firebase Storage, then update club data with new download URL.
     */
    private void uploadImageAndUpdateClub(String name, String description, String category) {
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
                    updateClubInFirebase(name, description, downloadUrl, category);
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(EditClubInfo.this,
                            "Image upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    /**
     * Actually update the club document in Realtime Database.
     */
    private void updateClubInFirebase(String name,
                                      String description,
                                      String imageUrl,
                                      String category) {
        ClubModal updated = new ClubModal();
        updated.setClubId(clubId);
        updated.setClubName(name);
        updated.setShortDescription(description);
        updated.setImagePath(imageUrl);
        updated.setCategory(category);

        clubRef.setValue(updated)
                .addOnSuccessListener(unused -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(EditClubInfo.this,
                            "Club updated successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Go to main page and clear back stack
                    Intent intent = new Intent(EditClubInfo.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(EditClubInfo.this,
                            "Failed to update club: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    /**
     * Save camera bitmap to gallery and return its Uri so we can upload to Firebase Storage.
     * This is called on a background thread.
     */
    private Uri saveImageToGallery(Bitmap bitmap) {
        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                "ClubImage_" + System.currentTimeMillis(),
                "Club image"
        );

        if (savedImageURL != null) {
            return Uri.parse(savedImageURL);
        } else {
            return null;
        }
    }

    private void deleteClub() {
        if (clubRef == null || clubId == null || clubId.isEmpty()) {
            Toast.makeText(this, "Cannot delete: missing club reference.", Toast.LENGTH_LONG).show();
            return;
        }

        btnDelete.setEnabled(false);

        // 1) Delete from Realtime Database
        clubRef.removeValue()
                .addOnSuccessListener(unused -> {

                    // 2) (Optional) delete image from Firebase Storage
                    StorageReference imgRef = FirebaseStorage.getInstance()
                            .getReference()
                            .child("clubImages")
                            .child(clubId + ".jpg");

                    imgRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                // Image deleted
                            })
                            .addOnFailureListener(e -> {
                                // Silent fail for image delete
                            });

                    Toast.makeText(EditClubInfo.this,
                            "Club deleted successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Go back to main screen and clear back stack
                    Intent intent = new Intent(EditClubInfo.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnDelete.setEnabled(true);
                    Toast.makeText(EditClubInfo.this,
                            "Failed to delete club: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
        }
    }
}
