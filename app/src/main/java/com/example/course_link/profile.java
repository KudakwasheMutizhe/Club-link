package com.example.course_link;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de .hdodenhof.circleimageview.CircleImageView;

public class profile extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnSettings;
    private CircleImageView ivProfilePic;
    private ImageButton btnEditPhoto;
    private TextView tvDisplayName;
    private TextView tvEmail;
    private TextView tvBio;
    private TextView tvCampus;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String userId;

    // Activity result launcher for edit profile
    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Reload profile data
                    loadUserProfile();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Handle system bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
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
            // No user logged in, redirect to login
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnSettings = findViewById(R.id.btnSettings);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnEditPhoto = findViewById(R.id.btnEditPhoto);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvEmail = findViewById(R.id.tvEmail);
        tvBio = findViewById(R.id.tvBio);
        tvCampus = findViewById(R.id.tvCampus);

        // Set up listeners
        setupListeners();

        // Load user profile
        loadUserProfile();
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Settings button (for now, just a toast)
        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // Edit photo button
        btnEditPhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Photo upload coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Implement photo upload
        });

        // Edit profile button
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("displayName", tvDisplayName.getText().toString());
            intent.putExtra("bio", tvBio.getText().toString());
            intent.putExtra("campus", tvCampus.getText().toString());
            editProfileLauncher.launch(intent);
        });

        // Change password button
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        // Logout button
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            showLogoutDialog();
        });

        // Delete account button
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> {
            showDeleteAccountDialog();
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Set email
            tvEmail.setText(currentUser.getEmail());

            // Load profile data from Firebase
            usersRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String displayName = snapshot.child("displayName").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);
                    String campus = snapshot.child("campus").getValue(String.class);

                    tvDisplayName.setText(displayName != null ? displayName : "User");
                    tvBio.setText(bio != null && !bio.isEmpty() ? bio : "No bio yet");
                    tvCampus.setText(campus != null ? campus : "Main Campus");
                } else {
                    // No profile data, set defaults
                    tvDisplayName.setText("User");
                    tvBio.setText("No bio yet");
                    tvCampus.setText("Main Campus");
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showChangePasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("A password reset email will be sent to your email address.")
                .setPositiveButton("Send Email", (dialog, which) -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        mAuth.sendPasswordResetEmail(user.getEmail())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    // TODO: Navigate to login/welcome screen
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone and all your data will be removed.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Show confirmation dialog
                    new AlertDialog.Builder(this)
                            .setTitle("Confirm Deletion")
                            .setMessage("Type 'DELETE' to confirm account deletion")
                            .setPositiveButton("Confirm", (d, w) -> {
                                deleteAccount();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Delete user data from database first
            usersRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Delete authentication account
                        user.delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                    // TODO: Navigate to welcome/login screen
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}