package com.example.emergencyapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.services.EmergencyListenerService;
import com.example.emergencyapp.services.LocationService;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.EmergencyButton;
import com.example.emergencyapp.utils.PreferenceManager;
import com.example.emergencyapp.utils.UpdateChecker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class MainActivity extends AppCompatActivity implements EmergencyButton.EmergencyButtonListener {

    private EmergencyButton emergencyButton;
    private TextView tvUserName;
    private TextView tvLongPressHint;
    private FrameLayout progressOverlay;
    private LinearLayout btnChat;
    private LinearLayout btnLocation;
    private LinearLayout btnHistory;
    private LinearLayout statusIndicator;

    private PreferenceManager prefManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefManager = new PreferenceManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupLocationUpdates();
        setupBottomButtons();

        startEmergencyListenerService();
        startLocationService();

        // Güncelleme kontrolü
        UpdateChecker.checkForUpdate(this, null);
    }

    private void initViews() {
        emergencyButton = findViewById(R.id.emergencyButton);
        tvUserName = findViewById(R.id.tvUserName);
        tvLongPressHint = findViewById(R.id.tvLongPressHint);
        progressOverlay = findViewById(R.id.progressOverlay);
        btnChat = findViewById(R.id.btnChat);
        btnLocation = findViewById(R.id.btnLocation);
        btnHistory = findViewById(R.id.btnHistory);
        statusIndicator = findViewById(R.id.statusIndicator);

        tvUserName.setText(prefManager.getUserName());
        emergencyButton.setListener(this);

        // Status indicator'a uzun basınca admin girişi
        statusIndicator.setOnLongClickListener(v -> {
            showAdminLoginDialog();
            return true;
        });

        // Eğer zaten admin ise, status indicator'a tıklayınca admin paneli
        statusIndicator.setOnClickListener(v -> {
            if (prefManager.isAdmin()) {
                startActivity(new Intent(this, AdminActivity.class));
            }
        });

        // Admin ise göstergeyi değiştir
        updateAdminIndicator();
    }

    private void updateAdminIndicator() {
        if (prefManager.isAdmin()) {
            TextView tvStatus = statusIndicator.findViewById(android.R.id.text1);
            if (tvStatus == null) {
                // Layout'taki text view'u bul
                for (int i = 0; i < statusIndicator.getChildCount(); i++) {
                    View child = statusIndicator.getChildAt(i);
                    if (child instanceof TextView) {
                        ((TextView) child).setText("Admin");
                        break;
                    }
                }
            }
        }
    }

    private void showAdminLoginDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Yönetici şifresi");

        new AlertDialog.Builder(this)
                .setTitle("🔐 Yönetici Girişi")
                .setMessage("Yönetici paneline erişmek için şifreyi girin:")
                .setView(input)
                .setPositiveButton("Giriş", (dialog, which) -> {
                    String password = input.getText().toString();
                    if (password.equals(Constants.ADMIN_PASSWORD)) {
                        prefManager.setAdmin(true);
                        Toast.makeText(this, "Yönetici girişi başarılı!", Toast.LENGTH_SHORT).show();
                        updateAdminIndicator();
                        startActivity(new Intent(this, AdminActivity.class));
                    } else {
                        Toast.makeText(this, "Yanlış şifre!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .setNeutralButton("Çıkış Yap", (dialog, which) -> {
                    prefManager.setAdmin(false);
                    Toast.makeText(this, "Admin oturumu kapatıldı", Toast.LENGTH_SHORT).show();
                    updateAdminIndicator();
                })
                .show();
    }

    private void setupBottomButtons() {
        btnChat.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatActivity.class));
        });

        btnLocation.setOnClickListener(v -> {
            if (currentLocation != null) {
                String message = String.format(
                        "Konumunuz:\nEnlem: %.6f\nBoylam: %.6f",
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude()
                );
                new AlertDialog.Builder(this)
                        .setTitle("📍 Mevcut Konum")
                        .setMessage(message)
                        .setPositiveButton("Tamam", null)
                        .show();
            } else {
                Toast.makeText(this, "Konum alınıyor...", Toast.LENGTH_SHORT).show();
            }
        });

        btnHistory.setOnClickListener(v -> {
            if (prefManager.isAdmin()) {
                startActivity(new Intent(this, AdminActivity.class));
            } else {
                Toast.makeText(this, "Geçmiş özelliği yakında!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startEmergencyListenerService() {
        Intent serviceIntent = new Intent(this, EmergencyListenerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void setupLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLocation = location;
                    FirebaseManager.getInstance().updateUserLocation(
                            prefManager.getUserId(),
                            location.getLatitude(),
                            location.getLongitude()
                    );
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    @Override
    public void onEmergencyTriggered(int emergencyType) {
        triggerEmergency(emergencyType);
    }

    @Override
    public void onDirectionSelected(int direction) {
        if (direction != -1) {
            showConfirmationDialog(direction);
        }
    }

    @Override
    public void onLongPressProgress(float progress) {
        if (progress > 0) {
            int percentage = (int) (progress * 100);
            tvLongPressHint.setText("Tam Acil Durum: %" + percentage);
            tvLongPressHint.setVisibility(View.VISIBLE);
        } else {
            tvLongPressHint.setVisibility(View.GONE);
        }
    }

    private void showConfirmationDialog(int emergencyType) {
        String typeText = getEmergencyTypeText(emergencyType);

        new AlertDialog.Builder(this)
                .setTitle("⚠️ Acil Durum Onayı")
                .setMessage(typeText + "\n\nAcil durumu ilan etmek istediğinize emin misiniz?\n\nTüm kullanıcılara bildirim gidecek!")
                .setPositiveButton("EVET, GÖNDER!", (dialog, which) -> triggerEmergency(emergencyType))
                .setNegativeButton("İptal", null)
                .setCancelable(true)
                .show();
    }

    private String getEmergencyTypeText(int type) {
        switch (type) {
            case Constants.EMERGENCY_KIDNAPPING: return "🏃 Kaçırılıyorum";
            case Constants.EMERGENCY_BEING_FOLLOWED: return "👁️ Takip Ediliyorum";
            case Constants.EMERGENCY_AMBULANCE: return "🚑 Ambulans/Kaza";
            case Constants.EMERGENCY_COME_HERE: return "📍 Hemen Buraya Gelin";
            case Constants.EMERGENCY_FULL: return "⚠️ TAM ACİL DURUM";
            default: return "Acil Durum";
        }
    }

    private void triggerEmergency(int emergencyType) {
        if (currentLocation == null) {
            Toast.makeText(this, "Konum alınamadı, lütfen bekleyin...", Toast.LENGTH_SHORT).show();
            return;
        }

        progressOverlay.setVisibility(View.VISIBLE);

        String oderId = prefManager.getUserId();
        String userName = prefManager.getUserName();

        String emergencyId = FirebaseManager.getInstance().createEmergency(
                oderId, userName, emergencyType,
                currentLocation.getLatitude(), currentLocation.getLongitude()
        );

        progressOverlay.setVisibility(View.GONE);

        Intent intent = new Intent(this, EmergencyReceivedActivity.class);
        intent.putExtra("emergency_id", emergencyId);
        intent.putExtra("sender_name", userName);
        intent.putExtra("emergency_type", emergencyType);
        intent.putExtra("latitude", currentLocation.getLatitude());
        intent.putExtra("longitude", currentLocation.getLongitude());
        intent.putExtra("is_sender", true);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}