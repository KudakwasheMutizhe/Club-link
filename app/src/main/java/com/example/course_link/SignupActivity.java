package com.example.course_link;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.content.Intent;
import android.content.SharedPreferences;
import com.google.firebase.FirebaseApp; // <-- Add this import
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    EditText etFullname, etEmail, etUsername, etPassword;
    Button btnSignup;
    TextView tvLogin;

    private static final String DB_URL = "https://club-link-default-rtdb.firebaseio.com/";

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

        btnSignup.setOnClickListener(view -> {
            String fullname = etFullname.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (fullname.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

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

                // Save login session
                SessionManager sm = new SessionManager(SignupActivity.this);
                sm.saveLogin(userId, username);

                // Also push user profile into Firebase /users/{userId}
                AppUserFirebase fbUser = new AppUserFirebase(
                        userId,
                        fullname,
                        username,
                        email
                );

                FirebaseDatabase.getInstance(DB_URL)
                        .getReference("users")
                        .child(String.valueOf(userId))
                        .setValue(fbUser);

                Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();

            } else {
                Toast.makeText(SignupActivity.this, "Username already exists!", Toast.LENGTH_SHORT).show();
            }
        });

        tvLogin.setOnClickListener(view -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }
}
