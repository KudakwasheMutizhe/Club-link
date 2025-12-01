package com.clublink.club_link;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;
import java.util.Random;

public class SignupActivity extends AppCompatActivity {

    EditText etFullname, etEmail, etUsername, etPassword;
    Button btnSignup;
    TextView tvLogin;
    TextView tvUsernameStatus;   // status under username

    // 🔹 Password requirement views
    TextView tvReqLength, tvReqUpperLower, tvReqDigit, tvReqSpecial, tvReqNoSpace;

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

        // 🔹 Find password requirement TextViews
        tvReqLength = findViewById(R.id.tvReqLength);
        tvReqUpperLower = findViewById(R.id.tvReqUpperLower);
        tvReqDigit = findViewById(R.id.tvReqDigit);
        tvReqSpecial = findViewById(R.id.tvReqSpecial);
        tvReqNoSpace = findViewById(R.id.tvReqNoSpace);

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        usersRef = db.getReference("users"); // we’ll key this by username.toLowerCase()

        // 🔹 Live username availability check
        setupUsernameWatcher();

        // 🔹 Live password strength indicator
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordRequirements(s.toString());
            }

            @Override public void afterTextChanged(Editable s) { }
        });

        btnSignup.setOnClickListener(view -> attemptSignup());

        tvLogin.setOnClickListener(view -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });

        // 🔙 HARD REDIRECT: Back arrow → LoginActivity
        View backArrow = findViewById(R.id.backArrow);
        if (backArrow != null) {
            backArrow.setOnClickListener(v -> {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

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

    private boolean isValidEmail(String email) {
        return !android.text.TextUtils.isEmpty(email) &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isWhitespace(c)) hasSymbol = true;
            else if (Character.isWhitespace(c)) {
                return false; // no spaces
            }
        }

        return hasUpper && hasLower && hasDigit && hasSymbol;
    }

    // 🔹 Update requirement colors as user types
    private void updatePasswordRequirements(String pwd) {
        // default colors: red = fail, green = pass
        int red = 0xFFAA0000;
        int green = 0xFF00C853;

        // Length
        tvReqLength.setTextColor(pwd.length() >= 8 ? green : red);

        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSymbol = false, hasSpace = false;

        for (char c : pwd.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (Character.isWhitespace(c)) hasSpace = true;
            else hasSymbol = true;
        }

        tvReqUpperLower.setTextColor((hasUpper && hasLower) ? green : red);
        tvReqDigit.setTextColor(hasDigit ? green : red);
        tvReqSpecial.setTextColor(hasSymbol ? green : red);
        tvReqNoSpace.setTextColor(!hasSpace ? green : red);
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

        boolean hasError = false;

        if (fullname.isEmpty()) {
            etFullname.setError("Full name is required");
            hasError = true;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            hasError = true;
        } else if (!isValidEmail(email)) {
            etEmail.setError("Enter a valid email");
            hasError = true;
        }

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            hasError = true;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            hasError = true;
        } else if (!isStrongPassword(password)) {
            etPassword.setError("Password must meet security requirements");
            Toast.makeText(this,
                    "Password must be at least 8 characters with upper, lower, digit, special, and no spaces.",
                    Toast.LENGTH_LONG).show();
            hasError = true;
        }

        if (hasError) {
            Toast.makeText(this, "Please fix the errors above", Toast.LENGTH_SHORT).show();
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
