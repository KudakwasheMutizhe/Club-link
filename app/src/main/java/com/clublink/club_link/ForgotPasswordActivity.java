package com.clublink.club_link;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import android.content.Intent;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etNewPassword, etConfirmPassword;
    private Button btnResetPassword;

    private TextView tvReqLength, tvReqUpperLower, tvReqDigit, tvReqSpecial, tvReqNoSpace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        tvReqLength = findViewById(R.id.tvReqLength);
        tvReqUpperLower = findViewById(R.id.tvReqUpperLower);
        tvReqDigit = findViewById(R.id.tvReqDigit);
        tvReqSpecial = findViewById(R.id.tvReqSpecial);
        tvReqNoSpace = findViewById(R.id.tvReqNoSpace);

        // Live update of password requirements
        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordRequirements(s.toString());
            }

            @Override public void afterTextChanged(Editable s) { }
        });

        btnResetPassword.setOnClickListener(v -> handleReset());
    }

    // ------ Email & Password helpers ------

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches();
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
                // no spaces allowed
                return false;
            }
        }

        return hasUpper && hasLower && hasDigit && hasSymbol;
    }

    private void updatePasswordRequirements(String pwd) {
        // Length
        tvReqLength.setTextColor(pwd.length() >= 8 ? 0xFF00C853 : 0xFFAA0000);

        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSymbol = false, hasSpace = false;

        for (char c : pwd.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (Character.isWhitespace(c)) hasSpace = true;
            else hasSymbol = true;
        }

        tvReqUpperLower.setTextColor((hasUpper && hasLower) ? 0xFF00C853 : 0xFFAA0000);
        tvReqDigit.setTextColor(hasDigit ? 0xFF00C853 : 0xFFAA0000);
        tvReqSpecial.setTextColor(hasSymbol ? 0xFF00C853 : 0xFFAA0000);
        tvReqNoSpace.setTextColor(!hasSpace ? 0xFF00C853 : 0xFFAA0000);
    }

    private void handleReset() {
        String email = etEmail.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (!isValidEmail(email)) {
            etEmail.setError("Enter a valid email");
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isStrongPassword(newPass)) {
            etNewPassword.setError("Password does not meet requirements");
            Toast.makeText(this, "Password must meet all requirements", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Update password in SQLite (you may instead search by username if you prefer)
        UserDbHelper db = new UserDbHelper(this);
        boolean updated = db.updatePasswordByEmail(email, newPass);

        if (updated) {
            Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "No user found with that email", Toast.LENGTH_SHORT).show();
        }
    }
}

