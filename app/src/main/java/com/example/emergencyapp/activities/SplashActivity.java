package com.example.emergencyapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencyapp.R;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            PreferenceManager prefManager = new PreferenceManager(this);
            boolean setupDone = prefManager.isSetupComplete();
            boolean authValid = FirebaseAuth.getInstance().getCurrentUser() != null;

            Intent intent;
            if (setupDone && authValid) {
                // Her iki koşul sağlandıysa direkt ana ekrana
                intent = new Intent(this, MainActivity.class);
            } else {
                // Firebase Auth varsa ama setup bitmemişse (eski kurulum) → Login'e gönder
                // Bu şekilde eski kullanıcılar da akıştan geçer
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 1500);
    }
}
