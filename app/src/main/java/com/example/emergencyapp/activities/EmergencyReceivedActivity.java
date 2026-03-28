package com.example.emergencyapp.activities;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.adapters.MessageAdapter;
import com.example.emergencyapp.adapters.TabPagerAdapter;
import com.example.emergencyapp.models.EmergencyEvent;
import com.example.emergencyapp.models.LocationData;
import com.example.emergencyapp.models.Message;
import com.example.emergencyapp.utils.AlertManager;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmergencyReceivedActivity extends AppCompatActivity {
    private static final String TAG = "EmergencyReceived";

    // Views
    private TextView tvEmergencyType;
    private TextView tvSenderName;
    private MapView mapView;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Button btnMuteAlarm;
    private Button btnCancelEmergency;
    private LinearLayout controlButtons;

    // Data
    private String emergencyId;
    private String senderName;
    private int emergencyType;
    private double targetLat, targetLng;
    private boolean isSender;

    private PreferenceManager prefManager;
    private AlertManager alertManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Map<String, Marker> userMarkers = new HashMap<>();
    private Marker targetMarker;
    private Polyline routeLine;

    private ValueEventListener locationListener;
    private ValueEventListener emergencyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Wake up screen and show on lock screen
        wakeUpScreen();

        setContentView(R.layout.activity_emergency_received);

        // Get intent data
        emergencyId = getIntent().getStringExtra("emergency_id");
        senderName = getIntent().getStringExtra("sender_name");
        emergencyType = getIntent().getIntExtra("emergency_type", 0);
        targetLat = getIntent().getDoubleExtra("latitude", 0);
        targetLng = getIntent().getDoubleExtra("longitude", 0);
        isSender = getIntent().getBooleanExtra("is_sender", false);

        prefManager = new PreferenceManager(this);
        alertManager = new AlertManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupMap();
        setupTabs();
        startLocationUpdates();
        listenToLocations();
        listenToEmergencyStatus();

        // Start alarm if not sender
        if (!isSender) {
            startEmergencyAlert();
        }
    }

    private void wakeUpScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "EmergencyApp:WakeLock"
        );
        wakeLock.acquire(60 * 1000L);
    }

    private void initViews() {
        tvEmergencyType = findViewById(R.id.tvEmergencyType);
        tvSenderName = findViewById(R.id.tvSenderName);
        mapView = findViewById(R.id.mapView);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        btnMuteAlarm = findViewById(R.id.btnMuteAlarm);
        btnCancelEmergency = findViewById(R.id.btnCancelEmergency);
        controlButtons = findViewById(R.id.controlButtons);

        // Set emergency info
        tvSenderName.setText(senderName);
        tvEmergencyType.setText(getEmergencyTypeText(emergencyType));
        setEmergencyTypeColor();

        // Show cancel button only for sender
        btnCancelEmergency.setVisibility(isSender ? View.VISIBLE : View.GONE);

        btnMuteAlarm.setOnClickListener(v -> {
            alertManager.stopAlert();
            btnMuteAlarm.setText("Alarm Susturuldu");
            btnMuteAlarm.setEnabled(false);
        });

        btnCancelEmergency.setOnClickListener(v -> showCancelConfirmation());
    }

    private String getEmergencyTypeText(int type) {
        switch (type) {
            case Constants.EMERGENCY_KIDNAPPING: return "🏃 KAÇIRILIYORUM";
            case Constants.EMERGENCY_BEING_FOLLOWED: return "👁️ TAKİP EDİLİYORUM";
            case Constants.EMERGENCY_AMBULANCE: return "🚑 AMBULANS/KAZA";
            case Constants.EMERGENCY_COME_HERE: return "📍 HEMEN BURAYA GELİN";
            case Constants.EMERGENCY_FULL: return "⚠️ TAM ACİL DURUM";
            default: return "ACİL DURUM";
        }
    }

    private void setEmergencyTypeColor() {
        int color;
        switch (emergencyType) {
            case Constants.EMERGENCY_KIDNAPPING:
                color = Color.parseColor("#FF5722");
                break;
            case Constants.EMERGENCY_BEING_FOLLOWED:
                color = Color.parseColor("#9C27B0");
                break;
            case Constants.EMERGENCY_AMBULANCE:
                color = Color.parseColor("#4CAF50");
                break;
            case Constants.EMERGENCY_COME_HERE:
                color = Color.parseColor("#2196F3");
                break;
            case Constants.EMERGENCY_FULL:
                color = Color.parseColor("#FF1744");
                break;
            default:
                color = Color.RED;
        }
        tvEmergencyType.setBackgroundColor(color);
    }

    private void setupMap() {
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);

        // Add target marker
        GeoPoint targetPoint = new GeoPoint(targetLat, targetLng);
        targetMarker = new Marker(mapView);
        targetMarker.setPosition(targetPoint);
        targetMarker.setTitle(senderName + " - Acil Durum Konumu");
        targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(targetMarker);

        mapController.setCenter(targetPoint);
    }

    private void setupTabs() {
        TabPagerAdapter adapter = new TabPagerAdapter(this, emergencyId, isSender);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Harita");
                    break;
                case 1:
                    tab.setText("Mesajlar");
                    break;
                case 2:
                    tab.setText("İpuçları");
                    break;
            }
        }).attach();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();

                    // Update location to Firebase
                    FirebaseManager.getInstance().updateEmergencyLocation(
                            emergencyId,
                            prefManager.getUserId(),
                            prefManager.getUserName(),
                            lat, lng
                    );
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void listenToLocations() {
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    LocationData location = child.getValue(LocationData.class);
                    if (location != null) {
                        updateUserMarker(location);
                    }
                }
                updateRouteLine();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        FirebaseManager.getInstance().listenToEmergencyLocations(emergencyId, locationListener);
    }

    private void updateUserMarker(LocationData location) {
        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());

        Marker marker = userMarkers.get(location.getUserId());
        if (marker == null) {
            marker = new Marker(mapView);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
            userMarkers.put(location.getUserId(), marker);
        }

        marker.setPosition(point);
        marker.setTitle(location.getUserName());
        mapView.invalidate();
    }

    private void updateRouteLine() {
        // Simple line to target - for full navigation, integrate with routing API
        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
        }

        Marker myMarker = userMarkers.get(prefManager.getUserId());
        if (myMarker != null && targetMarker != null) {
            routeLine = new Polyline();
            List<GeoPoint> points = new ArrayList<>();
            points.add(myMarker.getPosition());
            points.add(targetMarker.getPosition());
            routeLine.setPoints(points);
            routeLine.setColor(Color.BLUE);
            routeLine.setWidth(5f);
            mapView.getOverlays().add(routeLine);
            mapView.invalidate();
        }
    }

    private void listenToEmergencyStatus() {
        emergencyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                EmergencyEvent event = snapshot.getValue(EmergencyEvent.class);
                if (event == null || !event.isActive()) {
                    // Emergency cancelled
                    alertManager.stopAlert();
                    Toast.makeText(EmergencyReceivedActivity.this,
                            "Acil durum iptal edildi", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    // Update target location if it changed
                    if (targetMarker != null) {
                        targetMarker.setPosition(new GeoPoint(event.getLatitude(), event.getLongitude()));
                        mapView.invalidate();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        FirebaseManager.getInstance().getDatabase()
                .child(Constants.PATH_ACTIVE_EMERGENCY)
                .addValueEventListener(emergencyListener);
    }

    private void startEmergencyAlert() {
        EmergencyEvent event = new EmergencyEvent();
        event.setSenderName(senderName);
        event.setEmergencyType(emergencyType);

        alertManager.startEmergencyAlert(event.getTTSMessage(), emergencyType);
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Acil Durumu İptal Et")
                .setMessage("Acil durumu iptal etmek istediğinize emin misiniz?")
                .setPositiveButton("Evet, İptal Et", (dialog, which) -> {
                    FirebaseManager.getInstance().cancelEmergency(emergencyId);
                    finish();
                })
                .setNegativeButton("Hayır", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alertManager.release();

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (locationListener != null) {
            FirebaseManager.getInstance().getDatabase()
                    .child(Constants.PATH_EMERGENCIES).child(emergencyId)
                    .child("userLocations").removeEventListener(locationListener);
        }

        if (emergencyListener != null) {
            FirebaseManager.getInstance().getDatabase()
                    .child(Constants.PATH_ACTIVE_EMERGENCY)
                    .removeEventListener(emergencyListener);
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent accidental back press during emergency
        new AlertDialog.Builder(this)
                .setTitle("Uyarı")
                .setMessage("Acil durum devam ediyor. Çıkmak istediğinize emin misiniz?")
                .setPositiveButton("Çık", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("Kal", null)
                .show();
    }
}