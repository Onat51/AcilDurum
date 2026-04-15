package com.example.emergencyapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.models.CustomEmergencyCode;
import com.example.emergencyapp.models.User;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.PreferenceManager;
import com.example.emergencyapp.utils.UpdateChecker;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView tvUserCount;
    private TextView tvCurrentVersion;
    private Button btnUpdateVersion;
    private Button btnRefreshUsers;
    private Button btnAddCode;
    private ImageButton btnBack;
    private LinearLayout userListLayout;
    private LinearLayout codesLayout;
    private TabLayout tabLayout;
    private ScrollView scrollView;

    private PreferenceManager prefManager;
    private ValueEventListener usersListener;
    private ValueEventListener codesListener;
    private Map<String, Marker> userMarkers = new HashMap<>();
    private List<CustomEmergencyCode> customCodes = new ArrayList<>();

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        prefManager = new PreferenceManager(this);

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
        loadCustomCodes();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        tvUserCount = findViewById(R.id.tvUserCount);
        tvCurrentVersion = findViewById(R.id.tvCurrentVersion);
        btnUpdateVersion = findViewById(R.id.btnUpdateVersion);
        btnRefreshUsers = findViewById(R.id.btnRefreshUsers);
        btnAddCode = findViewById(R.id.btnAddCode);
        btnBack = findViewById(R.id.btnBack);
        userListLayout = findViewById(R.id.userListLayout);
        codesLayout = findViewById(R.id.codesLayout);
        tabLayout = findViewById(R.id.tabLayout);
        scrollView = findViewById(R.id.scrollView);

        btnBack.setOnClickListener(v -> finish());
        btnUpdateVersion.setOnClickListener(v -> showUpdateVersionDialog());
        btnRefreshUsers.setOnClickListener(v -> {
            loadUsers();
            Toast.makeText(this, "Yenilendi", Toast.LENGTH_SHORT).show();
        });
        btnAddCode.setOnClickListener(v -> showAddCodeDialog());

        tabLayout.addTab(tabLayout.newTab().setText("Kullanıcılar"));
        tabLayout.addTab(tabLayout.newTab().setText("Özel Kodlar"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    userListLayout.setVisibility(View.VISIBLE);
                    codesLayout.setVisibility(View.GONE);
                    btnAddCode.setVisibility(View.GONE);
                } else {
                    userListLayout.setVisibility(View.GONE);
                    codesLayout.setVisibility(View.VISIBLE);
                    btnAddCode.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        IMapController mc = mapView.getController();
        mc.setZoom(10.0);
        mc.setCenter(new GeoPoint(39.0, 35.0)); // Turkey center
    }

    private void loadUsers() {
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

        FirebaseManager.getInstance().listenToUsers(usersListener);
    }

    private void addUserToList(User user) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(16, 12, 16, 12);
        item.setBackgroundResource(R.drawable.bg_user_item);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        item.setLayoutParams(params);

        TextView tvName = new TextView(this);
        tvName.setText("👤 " + user.getName());
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(16);
        item.addView(tvName);

        TextView tvLocation = new TextView(this);
        if (user.getLatitude() != 0 && user.getLongitude() != 0) {
            tvLocation.setText(String.format(Locale.getDefault(),
                    "📍 %.4f, %.4f", user.getLatitude(), user.getLongitude()));
        } else {
            tvLocation.setText("📍 Konum bilgisi yok");
        }
        tvLocation.setTextColor(Color.parseColor("#78909C"));
        tvLocation.setTextSize(12);
        item.addView(tvLocation);

        TextView tvLastUpdate = new TextView(this);
        if (user.getLastLocationUpdate() > 0) {
            tvLastUpdate.setText("🕐 " + dateFormat.format(new Date(user.getLastLocationUpdate())));
        } else {
            tvLastUpdate.setText("🕐 Güncelleme yok");
        }
        tvLastUpdate.setTextColor(Color.parseColor("#78909C"));
        tvLastUpdate.setTextSize(12);
        item.addView(tvLastUpdate);

        // Action buttons row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 8, 0, 0);
        item.addView(btnRow);

        // Haritada göster
        Button btnMap = new Button(this);
        btnMap.setText("Harita");
        btnMap.setTextSize(11);
        btnMap.setPadding(8, 4, 8, 4);
        LinearLayout.LayoutParams bParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bParams.setMarginEnd(4);
        btnMap.setLayoutParams(bParams);
        btnMap.setOnClickListener(v -> {
            if (user.getLatitude() != 0) {
                mapView.getController().animateTo(new GeoPoint(user.getLatitude(), user.getLongitude()));
                mapView.getController().setZoom(16.0);
            }
        });
        btnRow.addView(btnMap);

        // Geçmiş görüntüle
        Button btnHistory = new Button(this);
        btnHistory.setText("24s Geçmiş");
        btnHistory.setTextSize(11);
        btnHistory.setPadding(8, 4, 8, 4);
        LinearLayout.LayoutParams bParams2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bParams2.setMarginEnd(4);
        btnHistory.setLayoutParams(bParams2);
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationHistoryActivity.class);
            intent.putExtra("user_id", user.getId());
            intent.putExtra("user_name", user.getName());
            startActivity(intent);
        });
        btnRow.addView(btnHistory);

        // Uzaktan uyandır
        Button btnWake = new Button(this);
        btnWake.setText("🔔 Uyandır");
        btnWake.setTextSize(11);
        btnWake.setPadding(8, 4, 8, 4);
        LinearLayout.LayoutParams bParams3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnWake.setLayoutParams(bParams3);
        btnWake.setOnClickListener(v -> {
            FirebaseManager.getInstance().sendRemoteCommand(user.getId(), "wake");
            Toast.makeText(this, user.getName() + " cihazına uyandırma gönderildi", Toast.LENGTH_SHORT).show();
        });
        btnRow.addView(btnWake);

        userListLayout.addView(item);
    }

    private void addUserMarker(User user) {
        if (user.getLatitude() == 0 && user.getLongitude() == 0) return;
        GeoPoint point = new GeoPoint(user.getLatitude(), user.getLongitude());
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(user.getName());
        if (user.getLastLocationUpdate() > 0) {
            marker.setSnippet("Son güncelleme: " + dateFormat.format(new Date(user.getLastLocationUpdate())));
        }
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        userMarkers.put(user.getId(), marker);
    }

    // ─── Custom Codes ─────────────────────────────────────────────────────────

    private void loadCustomCodes() {
        codesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customCodes.clear();
                codesLayout.removeAllViews();

                for (DataSnapshot child : snapshot.getChildren()) {
                    CustomEmergencyCode code = child.getValue(CustomEmergencyCode.class);
                    if (code != null) {
                        customCodes.add(code);
                        addCodeToList(code);
                    }
                }

                if (customCodes.isEmpty()) {
                    TextView tvEmpty = new TextView(AdminActivity.this);
                    tvEmpty.setText("Henüz özel kod yok.\n\n+ butonuna basarak ekleyin.");
                    tvEmpty.setTextColor(Color.parseColor("#78909C"));
                    tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    tvEmpty.setPadding(16, 32, 16, 16);
                    codesLayout.addView(tvEmpty);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        FirebaseManager.getInstance().listenToCustomCodes(codesListener);
    }

    private void addCodeToList(CustomEmergencyCode code) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(16, 12, 16, 12);
        item.setBackgroundResource(R.drawable.bg_user_item);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        item.setLayoutParams(params);

        TextView tvCode = new TextView(this);
        tvCode.setText("🔑 Kod: " + code.getCode());
        tvCode.setTextColor(Color.parseColor("#FFB74D"));
        tvCode.setTextSize(14);
        item.addView(tvCode);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚡ " + code.getTitle());
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16);
        item.addView(tvTitle);

        if (code.getDescription() != null && !code.getDescription().isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText(code.getDescription());
            tvDesc.setTextColor(Color.parseColor("#78909C"));
            tvDesc.setTextSize(12);
            item.addView(tvDesc);
        }

        Button btnDelete = new Button(this);
        btnDelete.setText("🗑️ Sil");
        btnDelete.setTextSize(12);
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Kodu Sil")
                    .setMessage("\"" + code.getCode() + "\" kodunu silmek istediğinizden emin misiniz?")
                    .setPositiveButton("Sil", (d, w) -> {
                        FirebaseManager.getInstance().deleteCustomCode(code.getId());
                        Toast.makeText(this, "Kod silindi", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("İptal", null)
                    .show();
        });
        item.addView(btnDelete);

        codesLayout.addView(item);
    }

    private void showAddCodeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_code, null);
        EditText etCode = dialogView.findViewById(R.id.etCode);
        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etDesc = dialogView.findViewById(R.id.etDesc);

        new AlertDialog.Builder(this)
                .setTitle("Özel Acil Durum Kodu Ekle")
                .setView(dialogView)
                .setPositiveButton("Ekle", (dialog, which) -> {
                    String code = etCode.getText().toString().trim().toLowerCase();
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();

                    if (code.isEmpty() || title.isEmpty()) {
                        Toast.makeText(this, "Kod ve başlık boş olamaz!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check uniqueness then save
                    FirebaseManager.getInstance().createCustomCode(code, title, desc,
                            new com.google.firebase.database.ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        Toast.makeText(AdminActivity.this,
                                                "Bu kod zaten kullanılıyor!", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    CustomEmergencyCode codeObj = new CustomEmergencyCode(null, code, title, desc);
                                    FirebaseManager.getInstance().saveCustomCode(codeObj);
                                    Toast.makeText(AdminActivity.this,
                                            "Kod eklendi: " + code, Toast.LENGTH_SHORT).show();
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void loadCurrentVersion() {
        FirebaseManager.getInstance().getDatabase()
                .child(Constants.PATH_APP_INFO)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            tvCurrentVersion.setText("Versiyon bilgisi yok");
                            return;
                        }
                        String vn = snapshot.child("versionName").getValue(String.class);
                        Integer vc = snapshot.child("versionCode").getValue(Integer.class);
                        String text = "Firebase: " + (vn != null ? vn : "?") + " (" + (vc != null ? vc : "?") + ")\n" +
                                "Bu cihaz: " + Constants.APP_VERSION_NAME + " (" + Constants.APP_VERSION_CODE + ")";
                        tvCurrentVersion.setText(text);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showUpdateVersionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_version, null);
        EditText etVersionName = dialogView.findViewById(R.id.etVersionName);
        EditText etVersionCode = dialogView.findViewById(R.id.etVersionCode);
        EditText etUpdateMessage = dialogView.findViewById(R.id.etUpdateMessage);
        EditText etDownloadLink = dialogView.findViewById(R.id.etDownloadLink);
        CheckBox cbForceUpdate = dialogView.findViewById(R.id.cbForceUpdate);

        etVersionName.setText(Constants.APP_VERSION_NAME);
        etVersionCode.setText(String.valueOf(Constants.APP_VERSION_CODE));
        etDownloadLink.setText(Constants.GOOGLE_DRIVE_LINK);

        new AlertDialog.Builder(this)
                .setTitle("Versiyon Güncelle")
                .setView(dialogView)
                .setPositiveButton("Güncelle", (dialog, which) -> {
                    String versionName = etVersionName.getText().toString().trim();
                    String versionCodeStr = etVersionCode.getText().toString().trim();
                    if (versionName.isEmpty() || versionCodeStr.isEmpty()) {
                        Toast.makeText(this, "Boş olamaz!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int vc = Integer.parseInt(versionCodeStr);
                        String msg = etUpdateMessage.getText().toString().trim();
                        String link = etDownloadLink.getText().toString().trim();
                        boolean force = cbForceUpdate.isChecked();
                        UpdateChecker.updateVersionInfo(versionName, vc, msg, force, link,
                                new UpdateChecker.OnCompleteListener() {
                                    @Override public void onSuccess() {
                                        Toast.makeText(AdminActivity.this, "Güncellendi!", Toast.LENGTH_SHORT).show();
                                    }
                                    @Override public void onFailure(String error) {
                                        Toast.makeText(AdminActivity.this, "Hata: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Versiyon kodu sayı olmalı!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    @Override protected void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Listeners are auto-cleaned by FirebaseDatabase lifecycle, but explicitly clean up
        FirebaseManager.getInstance().getDatabase()
                .child(Constants.PATH_USERS).removeEventListener(usersListener);
        if (codesListener != null) {
            FirebaseManager.getInstance().getDatabase()
                    .child(Constants.PATH_CUSTOM_CODES).removeEventListener(codesListener);
        }
    }
}