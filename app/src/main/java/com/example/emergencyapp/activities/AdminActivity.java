package com.example.emergencyapp.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencyapp.R;
import com.example.emergencyapp.models.User;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.PreferenceManager;
import com.example.emergencyapp.utils.UpdateChecker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView tvUserCount;
    private TextView tvCurrentVersion;
    private Button btnUpdateVersion;
    private Button btnRefreshUsers;
    private ImageButton btnBack;
    private LinearLayout userListLayout;

    private PreferenceManager prefManager;
    private DatabaseReference usersRef;
    private ValueEventListener usersListener;
    private Map<String, Marker> userMarkers = new HashMap<>();

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        prefManager = new PreferenceManager(this);

        // Yönetici kontrolü
        if (!prefManager.isAdmin()) {
            Toast.makeText(this, "Yetkisiz erişim!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Configuration.getInstance().setUserAgentValue(getPackageName());

        initViews();
        setupMap();
        loadUsers();
        loadCurrentVersion();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        tvUserCount = findViewById(R.id.tvUserCount);
        tvCurrentVersion = findViewById(R.id.tvCurrentVersion);
        btnUpdateVersion = findViewById(R.id.btnUpdateVersion);
        btnRefreshUsers = findViewById(R.id.btnRefreshUsers);
        btnBack = findViewById(R.id.btnBack);
        userListLayout = findViewById(R.id.userListLayout);

        btnBack.setOnClickListener(v -> finish());

        btnUpdateVersion.setOnClickListener(v -> showUpdateVersionDialog());

        btnRefreshUsers.setOnClickListener(v -> {
            loadUsers();
            Toast.makeText(this, "Kullanıcılar yenilendi", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(10.0);
        // Türkiye merkezi
        mapController.setCenter(new GeoPoint(39.0, 35.0));
    }

    private void loadUsers() {
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.PATH_USERS);

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userListLayout.removeAllViews();
                userMarkers.clear();
                mapView.getOverlays().clear();

                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user != null) {
                        count++;
                        addUserToList(user);
                        addUserMarker(user);
                    }
                }

                tvUserCount.setText("Toplam Kullanıcı: " + count);
                mapView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Kullanıcılar yüklenemedi", Toast.LENGTH_SHORT).show();
            }
        };

        usersRef.addValueEventListener(usersListener);
    }

    private void addUserToList(User user) {
        LinearLayout userItem = new LinearLayout(this);
        userItem.setOrientation(LinearLayout.VERTICAL);
        userItem.setPadding(16, 12, 16, 12);
        userItem.setBackgroundResource(R.drawable.bg_user_item);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        userItem.setLayoutParams(params);

        // İsim
        TextView tvName = new TextView(this);
        tvName.setText("👤 " + user.getName());
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(16);
        userItem.addView(tvName);

        // Konum
        TextView tvLocation = new TextView(this);
        if (user.getLatitude() != 0 && user.getLongitude() != 0) {
            tvLocation.setText(String.format(Locale.getDefault(),
                    "📍 %.4f, %.4f", user.getLatitude(), user.getLongitude()));
        } else {
            tvLocation.setText("📍 Konum bilgisi yok");
        }
        tvLocation.setTextColor(Color.parseColor("#78909C"));
        tvLocation.setTextSize(12);
        userItem.addView(tvLocation);

        // Son güncelleme
        TextView tvLastUpdate = new TextView(this);
        if (user.getLastLocationUpdate() > 0) {
            tvLastUpdate.setText("🕐 " + dateFormat.format(new Date(user.getLastLocationUpdate())));
        } else {
            tvLastUpdate.setText("🕐 Güncelleme yok");
        }
        tvLastUpdate.setTextColor(Color.parseColor("#78909C"));
        tvLastUpdate.setTextSize(12);
        userItem.addView(tvLastUpdate);

        // Tıklanınca haritada göster
        userItem.setOnClickListener(v -> {
            if (user.getLatitude() != 0 && user.getLongitude() != 0) {
                GeoPoint point = new GeoPoint(user.getLatitude(), user.getLongitude());
                mapView.getController().animateTo(point);
                mapView.getController().setZoom(16.0);
            }
        });

        userListLayout.addView(userItem);
    }

    private void addUserMarker(User user) {
        if (user.getLatitude() == 0 && user.getLongitude() == 0) return;

        GeoPoint point = new GeoPoint(user.getLatitude(), user.getLongitude());
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(user.getName());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mapView.getOverlays().add(marker);
        userMarkers.put(user.getId(), marker);
    }

    private void loadCurrentVersion() {
        DatabaseReference appInfoRef = FirebaseDatabase.getInstance()
                .getReference(Constants.PATH_APP_INFO);

        appInfoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String versionName = snapshot.child("versionName").getValue(String.class);
                    Integer versionCode = snapshot.child("versionCode").getValue(Integer.class);

                    String text = "Firebase'deki Versiyon: ";
                    if (versionName != null && versionCode != null) {
                        text += versionName + " (" + versionCode + ")";
                    } else {
                        text += "Ayarlanmamış";
                    }
                    text += "\nMevcut Uygulama: " + Constants.APP_VERSION_NAME + " (" + Constants.APP_VERSION_CODE + ")";

                    tvCurrentVersion.setText(text);
                } else {
                    tvCurrentVersion.setText("Firebase'de versiyon bilgisi yok\nMevcut: " +
                            Constants.APP_VERSION_NAME + " (" + Constants.APP_VERSION_CODE + ")");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showUpdateVersionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_version, null);

        EditText etVersionName = dialogView.findViewById(R.id.etVersionName);
        EditText etVersionCode = dialogView.findViewById(R.id.etVersionCode);
        EditText etUpdateMessage = dialogView.findViewById(R.id.etUpdateMessage);
        EditText etDownloadLink = dialogView.findViewById(R.id.etDownloadLink);
        CheckBox cbForceUpdate = dialogView.findViewById(R.id.cbForceUpdate);

        // Varsayılan değerler
        etVersionName.setText(Constants.APP_VERSION_NAME);
        etVersionCode.setText(String.valueOf(Constants.APP_VERSION_CODE));
        etDownloadLink.setText(Constants.GOOGLE_DRIVE_LINK);

        new AlertDialog.Builder(this)
                .setTitle("Versiyon Güncelle")
                .setView(dialogView)
                .setPositiveButton("Güncelle", (dialog, which) -> {
                    String versionName = etVersionName.getText().toString().trim();
                    String versionCodeStr = etVersionCode.getText().toString().trim();
                    String updateMessage = etUpdateMessage.getText().toString().trim();
                    String downloadLink = etDownloadLink.getText().toString().trim();
                    boolean forceUpdate = cbForceUpdate.isChecked();

                    if (versionName.isEmpty() || versionCodeStr.isEmpty()) {
                        Toast.makeText(this, "Versiyon bilgisi boş olamaz!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int versionCode;
                    try {
                        versionCode = Integer.parseInt(versionCodeStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Versiyon kodu sayı olmalı!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    UpdateChecker.updateVersionInfo(versionName, versionCode, updateMessage,
                            forceUpdate, downloadLink, new UpdateChecker.OnCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(AdminActivity.this,
                                            "Versiyon güncellendi!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(String error) {
                                    Toast.makeText(AdminActivity.this,
                                            "Hata: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
    }
}