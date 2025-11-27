package com.example.course_link;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;
import java.util.Random;

public class SignupActivity extends AppCompatActivity {

    EditText etFullname, etEmail, etUsername, etPassword;
    Button btnSignup;
    TextView tvLogin;
    TextView tvUsernameStatus;   // 🔹 status under username

    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

    private DatabaseReference usersRef;

    private final Handler usernameCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable usernameCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etFullname = findViewById(R.id.etFullname);
        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        tvUsernameStatus = findViewById(R.id.tvUsernameStatus);

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        usersRef = db.getReference("users"); // we’ll key this by username.toLowerCase()

        // 🔹 Live username availability check
        setupUsernameWatcher();

        btnSignup.setOnClickListener(view -> attemptSignup());

        tvLogin.setOnClickListener(view -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    // ---------------- LIVE USERNAME CHECK ----------------

    private void setupUsernameWatcher() {
        etUsername.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear previous scheduled check
                if (usernameCheckRunnable != null) {
                    usernameCheckHandler.removeCallbacks(usernameCheckRunnable);
                }

                final String username = s.toString().trim();

                if (username.isEmpty()) {
                    clearUsernameStatus();
                    return;
                }

                // Debounce: wait 500ms after user stops typing
                usernameCheckRunnable = () -> checkUsernameAvailability(username);
                usernameCheckHandler.postDelayed(usernameCheckRunnable, 500);
            }

            @Override public void afterTextChanged(android.text.Editable s) { }
        });
    }

    private void clearUsernameStatus() {
        tvUsernameStatus.setVisibility(View.GONE);
        // reset color of EditText back to default (black)
        etUsername.setTextColor(0xFF000000);
    }

    /**
     * Check Firebase /users/<usernameKey> to see if username exists.
     * If taken → RED with suggestions
     * If available → GREEN "Username available"
     */
    private void checkUsernameAvailability(String username) {
        final String usernameKey = username.toLowerCase(Locale.ROOT);

        usersRef.child(usernameKey).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // ❌ Username taken → RED
                etUsername.setTextColor(0xFFFF4444); // red
                tvUsernameStatus.setVisibility(View.VISIBLE);
                tvUsernameStatus.setTextColor(0xFFFF4444);

                String suggestions = buildSuggestions(usernameKey);
                tvUsernameStatus.setText("Username taken. Try: " + suggestions);

            } else {
                // ✅ Username available → GREEN
                etUsername.setTextColor(0xFF00C853); // green
                tvUsernameStatus.setVisibility(View.VISIBLE);
                tvUsernameStatus.setTextColor(0xFF00C853);
                tvUsernameStatus.setText("Username available ✔");
            }
        }).addOnFailureListener(e -> {
            // In case of network error, just clear status
            clearUsernameStatus();
        });
    }

    private String buildSuggestions(String base) {
        Random rnd = new Random();
        String s1 = base + (rnd.nextInt(90) + 10);         // base + 2-digit number
        String s2 = base + "_" + (rnd.nextInt(900) + 100); // base_3-digit
        String s3 = base + "_club";
        return s1 + ", " + s2 + ", " + s3;
    }

    // ---------------- SIGNUP FLOW ----------------

    private void attemptSignup() {
        String fullname = etFullname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (fullname.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(SignupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        final String usernameKey = username.toLowerCase(Locale.ROOT);

        // 🔒 Re-check username in Firebase before creating account
        usersRef.child(usernameKey).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Already taken → show red + toast
                etUsername.setTextColor(0xFFFF4444);
                tvUsernameStatus.setVisibility(View.VISIBLE);
                tvUsernameStatus.setTextColor(0xFFFF4444);
                String suggestions = buildSuggestions(usernameKey);
                tvUsernameStatus.setText("Username taken. Try: " + suggestions);

                Toast.makeText(SignupActivity.this,
                        "That username is already taken.", Toast.LENGTH_SHORT).show();
            } else {
                // Username free → create local user + push to Firebase
                createUserAccount(fullname, email, username, usernameKey, password);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(SignupActivity.this,
                    "Error checking username. Please try again.", Toast.LENGTH_SHORT).show();
        });
    }

    private void createUserAccount(String fullname,
                                   String email,
                                   String username,
                                   String usernameKey,
                                   String password) {

        UserDbHelper db = new UserDbHelper(SignupActivity.this);
        User newUser = new User(fullname, email, username, password);

        // Insert into SQLite
        if (db.insertUser(newUser)) {

            // Get the SQLite id for this user
            long userId = db.loginUser(username, password);
            if (userId == -1) {
                Toast.makeText(this, "Error retrieving user id", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save login session (still using numeric id locally + username)
            SessionManager sm = new SessionManager(SignupActivity.this);
            sm.saveLogin(userId, username);

            // Push profile into Firebase /users/{usernameKey}
            AppUserFirebase fbUser = new AppUserFirebase(
                    userId,
                    fullname,
                    username,
                    email
            );

            usersRef.child(usernameKey)
                    .setValue(fbUser)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(SignupActivity.this,
                                "Account created successfully!", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignupActivity.this,
                                "Failed to save user online, but local account exists.",
                                Toast.LENGTH_LONG).show();
                    });

        } else {
            // Local SQLite found duplicate username (extra safety)
            Toast.makeText(SignupActivity.this,
                    "Username already exists locally!", Toast.LENGTH_SHORT).show();
        }
    }
}
