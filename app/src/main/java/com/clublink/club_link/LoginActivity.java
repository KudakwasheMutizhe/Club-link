package com.clublink.club_link;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.content.Intent;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    TextView tvSignup, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI elements
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // -------------------------------
        // LOGIN BUTTON LOGIC (RESTORED)
        // -------------------------------
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String user = etUsername.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                boolean hasError = false;

                if (user.isEmpty()) {
                    etUsername.setError("Username required");
                    hasError = true;
                }

                if (pass.isEmpty()) {
                    etPassword.setError("Password required");
                    hasError = true;
                }

                if (hasError) {
                    Toast.makeText(LoginActivity.this, "Please enter both fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // SQLite helper
                UserDbHelper db = new UserDbHelper(LoginActivity.this);

                // 🔹 Use loginUser to get the id
                long userId = db.loginUser(user, pass);

                if (userId != -1) {
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                    // 🔹 Save to SessionManager (this is what profile will use)
                    SessionManager sessionManager = new SessionManager(LoginActivity.this);
                    sessionManager.saveLogin(userId, user);

                    // (Optional) keep your old prefs if something else is using it
                    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("logged_in_username", user)
                            .apply();

                    // Move to next screen (your Lottie screen)
                    Intent intent = new Intent(LoginActivity.this, LottieActivity.class);
                    intent.putExtra("username", user);
                    startActivity(intent);

                    // finish(); // optional
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // -------------------------------
        // Navigate to Sign Up
        // -------------------------------
        tvSignup.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );

        // -------------------------------
        // Navigate to Forgot Password
        // -------------------------------
        tvForgotPassword.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class))
        );
    }
}



