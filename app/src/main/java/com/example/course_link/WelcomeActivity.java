package com.example.course_link;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;
import android.view.View;
import android.util.Log;

public class WelcomeActivity extends AppCompatActivity {

    Button btnLogin, btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Find views
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        // Debug: log if any view could not be found (prevents the NPE and helps diagnose)
        if (btnLogin == null || btnSignup == null) {
            try {
                String resName = getResources().getResourceName(R.layout.activity_welcome);
                Log.e("WelcomeActivity", "Missing view(s) after inflate. btnLogin==null:" + (btnLogin==null) + " btnSignup==null:" + (btnSignup==null) + " layout=" + resName);
            } catch (Exception e) {
                Log.e("WelcomeActivity", "Missing view(s) after inflate and failed to resolve layout name", e);
            }
        }

        // Go to Login page
        if (btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
                }
            });
        } else {
            Log.w("WelcomeActivity", "btnLogin is null — click listener not attached");
        }

        // Go to Sign Up page
        if (btnSignup != null) {
            btnSignup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(WelcomeActivity.this, SignupActivity.class));
                }
            });
        }
    }
}
