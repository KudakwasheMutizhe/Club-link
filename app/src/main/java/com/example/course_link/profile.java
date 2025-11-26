package com.example.course_link;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;

import de.hdodenhof.circleimageview.CircleImageView;

public class profile extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnSettings;
    private CircleImageView ivProfilePic;
    private ImageButton btnEditPhoto;
    private TextView tvDisplayName;
    private TextView tvEmail;
    private TextView tvBio;
    private TextView tvCampus;

    // Activity result launcher for edit profile
    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            // Reload profile data when coming back from EditProfileActivity
                            loadUserProfile();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Handle system bars padding for root view with id @+id/main
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // ---------- Bottom Navigation ----------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // This flag prevents the listener from re-navigating when the screen first loads.
        final boolean[] isInitialSelection = {true};

        // 1. Set the listener FIRST
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // If this is the first selection event (on screen load), ignore it.
            if (isInitialSelection[0]) {
                isInitialSelection[0] = false; // Mark as handled
                return true; // Consume the event, but do nothing.
            }

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(profile.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_events) {
                startActivity(new Intent(profile.this, EventsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                Intent intent = new Intent(profile.this, ChatListActivity.class);
                intent.putExtra("CHAT_ID", "GLOBAL_CHAT");
                intent.putExtra("CHAT_NAME", "Messages");
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_announcements) {
                startActivity(new Intent(profile.this, AnnouncementsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Already on the profile screen, do nothing.
                return true;
            }

            return false;
        });

        // 2. Set the selected item SECOND. This will trigger the listener one time.
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        // ---------- Initialize views ---------
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnEditPhoto = findViewById(R.id.btnEditPhoto);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvEmail = findViewById(R.id.tvEmail);
        tvBio = findViewById(R.id.tvBio);
        tvCampus = findViewById(R.id.tvCampus);

        // ---------- Listeners ----------
        setupListeners();

        // ---------- Load profile data from SQLite ----------
        loadUserProfile();
    }

    private void setupListeners() {

        // Settings button
        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        );

        // Edit photo button
        btnEditPhoto.setOnClickListener(v ->
                Toast.makeText(this, "Photo upload coming soon", Toast.LENGTH_SHORT).show()
        );

        // Edit profile button (still launches your EditProfileActivity if you use it)
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("displayName", tvDisplayName.getText().toString());
            intent.putExtra("bio", tvBio.getText().toString());
            intent.putExtra("campus", tvCampus.getText().toString());
            editProfileLauncher.launch(intent);
        });

        // Change password button (now uses SQLite)
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> showChangePasswordDialog());

        // Logout button
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        // Delete account button
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteAccountDialog());
    }

    /**
     * Load user profile from SQLite using the username saved in SharedPreferences.
     */
    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = prefs.getString("logged_in_username", null);

        if (username == null) {
            // No logged-in user – show default guest data
            tvEmail.setText("Not logged in");
            tvDisplayName.setText("Guest");
            tvBio.setText("No bio yet");
            tvCampus.setText("Main Campus");
            return;
        }

        UserDbHelper dbHelper = new UserDbHelper(this);
        User user = dbHelper.getUserByUsername(username);

        if (user != null) {
            // Email from SQLite
            tvEmail.setText(user.getEmail());

            // ✅ Display name: prefer updated one from SharedPreferences, fallback to SQLite fullname
            String displayNameKey = "displayName_" + username;
            String displayNamePref = prefs.getString(displayNameKey, null);

            if (displayNamePref != null && !displayNamePref.isEmpty()) {
                tvDisplayName.setText(displayNamePref);
            } else {
                tvDisplayName.setText(user.getFullname());
            }

            // Bio & campus from SharedPreferences (with defaults)
            String bioKey = "bio_" + username;
            String campusKey = "campus_" + username;

            String bio = prefs.getString(bioKey, "No bio yet");
            String campus = prefs.getString(campusKey, "Main Campus");

            tvBio.setText(bio);
            tvCampus.setText(campus);
        } else {
            // If something went wrong / user missing
            tvEmail.setText("Not logged in");
            tvDisplayName.setText("Guest");
            tvBio.setText("No bio yet");
            tvCampus.setText("Main Campus");
        }
    }


    /**
     * Show dialog to change password (SQLite version).
     */
    private void showChangePasswordDialog() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = prefs.getString("logged_in_username", null);

        if (username == null) {
            Toast.makeText(this, "You must be logged in to change password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Input for new password
        final EditText input = new EditText(this);
        input.setHint("New password");

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("Enter your new password.")
                .setView(input)
                .setPositiveButton("Change", (dialog, which) -> {
                    String newPassword = input.getText().toString().trim();
                    if (newPassword.isEmpty()) {
                        Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    UserDbHelper dbHelper = new UserDbHelper(this);
                    boolean success = dbHelper.updatePassword(username, newPassword);
                    if (success) {
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show logout confirmation dialog. Clears SharedPreferences.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Clear logged-in username
                    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    prefs.edit().remove("logged_in_username").apply();

                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                    // Optionally go to LoginActivity or a welcome screen
                    Intent intent = new Intent(profile.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show dialog to delete account from SQLite.
     */
    private void showDeleteAccountDialog() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = prefs.getString("logged_in_username", null);

        if (username == null) {
            Toast.makeText(this, "You must be logged in to delete your account", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Continue", (dialog, which) -> {
                    // Second confirmation with "DELETE" text
                    final EditText input = new EditText(this);
                    input.setHint("Type DELETE to confirm");

                    new AlertDialog.Builder(this)
                            .setTitle("Confirm Deletion")
                            .setMessage("Type 'DELETE' to confirm account deletion")
                            .setView(input)
                            .setPositiveButton("Delete", (d, w) -> {
                                String confirmation = input.getText().toString().trim();
                                if (!"DELETE".equals(confirmation)) {
                                    Toast.makeText(this, "Deletion cancelled (wrong confirmation)", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                UserDbHelper dbHelper = new UserDbHelper(this);
                                boolean success = dbHelper.deleteUser(username);

                                if (success) {
                                    // Clear session
                                    prefs.edit().remove("logged_in_username").apply();
                                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

                                    // Go back to login or welcome
                                    Intent intent = new Intent(profile.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, "Failed to delete account", Toast.LENGTH_LONG).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
