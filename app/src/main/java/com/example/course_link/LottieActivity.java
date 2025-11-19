package com.example.course_link;

import android.content.Intent;
import android.os.Bundle;

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


        // Find the LottieAnimationView by ID
        lottieAnimationView = findViewById(R.id.animation_view);

        // Optionally, start the animation programmatically
        lottieAnimationView.playAnimation();

        Intent intent = new Intent(LottieActivity.this, MainActivity.class);
        startActivity(intent);

        // You can also stop the animation, pause, or control playback speed programmatically:
        // lottieAnimationView.pauseAnimation();
        // lottieAnimationView.setSpeed(1.5f); // Adjust speed
    }

}
