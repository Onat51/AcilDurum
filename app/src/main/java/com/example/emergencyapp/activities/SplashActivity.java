package com.example.emergencyapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencyapp.R;
import com.example.emergencyapp.utils.PreferenceManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            PreferenceManager prefManager = new PreferenceManager(this);

            Intent intent;
            if (prefManager.isSetupComplete()) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, SetupActivity.class);
            }

            startActivity(intent);
            finish();
        }, 2000);
    }
}