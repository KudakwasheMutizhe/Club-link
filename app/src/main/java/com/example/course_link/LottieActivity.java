package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class LottieActivity extends AppCompatActivity {
    private LottieAnimationView lottieAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lottie);

        lottieAnimationView = findViewById(R.id.animation_view);

        // Start animation
        lottieAnimationView.playAnimation();

        // Delay 2 seconds → go to MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(LottieActivity.this, MainActivity.class));
            finish(); // close LottieActivity so user can't go back to it
        }, 2000); // 2000ms = 2 seconds
    }
}
