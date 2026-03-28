package com.example.emergencyapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {
    private static final String TAG = "SetupActivity";

    private EditText etName;
    private Button btnStart;
    private PreferenceManager prefManager;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        prefManager = new PreferenceManager(this);

        etName = findViewById(R.id.etName);
        btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("İsminizi girin");
                return;
            }

            Log.d(TAG, "İsim girişi: " + name);
            checkPermissionsAndProceed(name);
        });
    }

    private void checkPermissionsAndProceed(String name) {
        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (permissions.isEmpty()) {
            saveUserAndProceed(name);
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            String name = etName.getText().toString().trim();
            saveUserAndProceed(name);
        }
    }

    private void saveUserAndProceed(String name) {
        btnStart.setEnabled(false);
        btnStart.setText("Kaydediliyor...");

        Log.d(TAG, "Kullanıcı kaydediliyor: " + name);

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String token = task.isSuccessful() ? task.getResult() : "";
            Log.d(TAG, "FCM Token: " + token);

            // Firebase'e kullanıcı oluştur
            String oderId = FirebaseManager.getInstance().createUser(name, token);
            Log.d(TAG, "Oluşturulan User ID: " + oderId);

            // Preference'a kaydet
            prefManager.setUserId(oderId);
            prefManager.setUserName(name);
            prefManager.setFcmToken(token);
            prefManager.setSetupComplete(true);

            // Kontrol
            Log.d(TAG, "Kaydedilen UserId: " + prefManager.getUserId());
            Log.d(TAG, "Kaydedilen UserName: " + prefManager.getUserName());

            Toast.makeText(this, "Hoş geldin " + name + "!", Toast.LENGTH_SHORT).show();

            // Subscribe to emergency topic
            FirebaseMessaging.getInstance().subscribeToTopic("emergency");

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}