package com.clublink.club_link;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfilePic;
    private ImageButton btnEditPhoto;
    private TextView tvDisplayName;
    private TextView tvEmail;
    private TextView tvBio;
    private TextView tvCampus;

    // Helpers
    private SessionManager sessionManager;
    private UserDbHelper dbHelper;
    private User currentUser;

    // Single background thread for DB work
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    // Activity result launcher for EditProfileActivity
    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            // Reload Profile data when coming back from EditProfileActivity
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

        sessionManager = new SessionManager(this);
        dbHelper = new UserDbHelper(this);

        // ---------- Bottom Navigation ----------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        final boolean[] isInitialSelection = {true};

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (isInitialSelection[0]) {
                isInitialSelection[0] = false;
                return true;
            }

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_events) {
                startActivity(new Intent(ProfileActivity.this, EventsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_messages) {
                Intent intent = new Intent(ProfileActivity.this, ChatListActivity.class);
                intent.putExtra("CHAT_ID", "GLOBAL_CHAT");
                intent.putExtra("CHAT_NAME", "Messages");
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_announcements) {
                startActivity(new Intent(ProfileActivity.this, AnnouncementsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Already here
                return true;
            }

            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        // ---------- Initialize views ----------
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnEditPhoto = findViewById(R.id.btnEditPhoto);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvEmail = findViewById(R.id.tvEmail);
        tvBio = findViewById(R.id.tvBio);
        tvCampus = findViewById(R.id.tvCampus);

        // ---------- Listeners ----------
        setupListeners();

        // ---------- Load Profile data ----------
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile when returning to this screen
        loadUserProfile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }

    private void setupListeners() {
        // Edit photo button
        btnEditPhoto.setOnClickListener(v ->
                Toast.makeText(this, "Photo upload coming soon", Toast.LENGTH_SHORT).show()
        );

        // Edit Profile button
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "No user loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("displayName", tvDisplayName.getText().toString());
            intent.putExtra("bio", tvBio.getText().toString());
            intent.putExtra("campus", tvCampus.getText().toString());
            editProfileLauncher.launch(intent);
        });

        // Change password button
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> showChangePasswordDialog());

        // Logout button
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        // Delete account button
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteAccountDialog());
    }

    /**
     * Load user profile using SessionManager + SQLite on a background thread.
     * Also applies per-user displayName/bio/campus from SharedPreferences.
     */
    private void loadUserProfile() {
        long userId = sessionManager.getUserId();
        String usernameFromSession = sessionManager.getUsername();

        dbExecutor.execute(() -> {
            User user = null;

            // Try to get user by ID first (more reliable)
            if (userId != -1) {
                user = dbHelper.getUserById(userId);
            }

            // Fallback: if ID failed but we have a username
            if (user == null && usernameFromSession != null) {
                user = dbHelper.getUserByUsername(usernameFromSession);
            }

            User finalUser = user;
            runOnUiThread(() -> applyUserToUi(finalUser));
        });
    }

    /**
     * UI-only method: updates views with a User object, or guest state.
     */
    private void applyUserToUi(User user) {
        if (user == null) {
            tvEmail.setText("Not logged in");
            tvDisplayName.setText("Guest");
            tvBio.setText("No bio yet");
            tvCampus.setText("Main Campus");
            currentUser = null;
            return;
        }

        currentUser = user;
        String username = user.getUsername();

        // ---- Core info from SQLite ----
        tvEmail.setText(user.getEmail());

        // ---- Per-user profile preferences (displayName, bio, campus) ----
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Display name: first try stored override, else use fullname from DB
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
    }

    /**
     * Show dialog to change password (SQLite version), updating in background.
     */
    private void showChangePasswordDialog() {
        long userId = sessionManager.getUserId();
        if (userId == -1 || currentUser == null) {
            Toast.makeText(this, "You must be logged in to change password", Toast.LENGTH_SHORT).show();
            return;
        }

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

                    // Run DB update in background
                    dbExecutor.execute(() -> {
                        boolean success = dbHelper.updatePassword(currentUser.getUsername(), newPassword);
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show logout confirmation dialog. Clears SessionManager.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Clear session
                    sessionManager.clearSession();

                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show dialog to delete account from SQLite (deletes in background).
     */
    private void showDeleteAccountDialog() {
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to delete your account", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Continue", (dialog, which) -> {
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

                                // DB delete on background thread
                                dbExecutor.execute(() -> {
                                    boolean success = dbHelper.deleteUser(currentUser.getUsername());

                                    runOnUiThread(() -> {
                                        if (success) {
                                            sessionManager.clearSession();
                                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
