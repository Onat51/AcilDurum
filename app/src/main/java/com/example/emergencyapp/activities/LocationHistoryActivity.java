package com.example.emergencyapp.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.models.LocationHistory;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationHistoryActivity extends AppCompatActivity {

    private MapView mapView;
    private SeekBar seekBar;
    private TextView tvTime, tvSpeed, tvStatus;
    private Button btnPlay, btnPause, btnStop;
    private Button btn1x, btn2x, btn4x;
    private Button btnForward, btnBack;
    private ImageButton btnClose;

    private List<LocationHistory> historyPoints = new ArrayList<>();
    private Marker playerMarker;
    private Polyline trailLine;
    private int currentIndex = 0;
    private int playSpeed = 1; // 1, 2, or 4
    private boolean isPlaying = false;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable playRunnable;
    private static final long BASE_INTERVAL_MS = 800; // base playback interval

    private PreferenceManager prefManager;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault());

    private String targetUserId;
    private String targetUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_history);

        prefManager = new PreferenceManager(this);
        targetUserId = getIntent().getStringExtra("user_id");
        targetUserName = getIntent().getStringExtra("user_name");

        Configuration.getInstance().setUserAgentValue(getPackageName());

        initViews();
        setupMap();
        loadHistory();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        seekBar = findViewById(R.id.seekBar);
        tvTime = findViewById(R.id.tvTime);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvStatus = findViewById(R.id.tvStatus);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btn1x = findViewById(R.id.btn1x);
        btn2x = findViewById(R.id.btn2x);
        btn4x = findViewById(R.id.btn4x);
        btnForward = findViewById(R.id.btnForward);
        btnBack = findViewById(R.id.btnBack);
        btnClose = findViewById(R.id.btnClose);

        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null && targetUserName != null) {
            tvTitle.setText(targetUserName + " - 24s Geçmişi");
        }

        btnClose.setOnClickListener(v -> finish());

        btnPlay.setOnClickListener(v -> startPlayback());
        btnPause.setOnClickListener(v -> pausePlayback());
        btnStop.setOnClickListener(v -> stopPlayback());

        btn1x.setOnClickListener(v -> setSpeed(1));
        btn2x.setOnClickListener(v -> setSpeed(2));
        btn4x.setOnClickListener(v -> setSpeed(4));

        btnForward.setOnClickListener(v -> {
            if (currentIndex < historyPoints.size() - 10) {
                currentIndex += 10;
            } else {
                currentIndex = historyPoints.size() - 1;
            }
            updateMarker();
        });

        btnBack.setOnClickListener(v -> {
            currentIndex = Math.max(0, currentIndex - 10);
            updateMarker();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !historyPoints.isEmpty()) {
                    currentIndex = (int) ((progress / 100.0) * (historyPoints.size() - 1));
                    updateMarker();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { pausePlayback(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        setSpeed(1);
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(14.0);
    }

    private void loadHistory() {
        tvStatus.setText("Yükleniyor...");
        String userId = targetUserId != null ? targetUserId : prefManager.getUserId();

        FirebaseManager.getInstance().listenToLocationHistory(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyPoints.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    LocationHistory loc = child.getValue(LocationHistory.class);
                    if (loc != null) historyPoints.add(loc);
                }

                // Sort by timestamp
                historyPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                if (historyPoints.isEmpty()) {
                    tvStatus.setText("24 saatlik hareket verisi yok");
                    return;
                }

                tvStatus.setText(historyPoints.size() + " nokta yüklendi");
                seekBar.setMax(100);
                currentIndex = 0;

                // Draw full trail
                drawFullTrail();
                updateMarker();

                // Center on first point
                GeoPoint first = new GeoPoint(
                        historyPoints.get(0).getLatitude(),
                        historyPoints.get(0).getLongitude());
                mapView.getController().setCenter(first);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvStatus.setText("Hata: " + error.getMessage());
            }
        });
    }

    private void drawFullTrail() {
        if (trailLine != null) mapView.getOverlays().remove(trailLine);
        trailLine = new Polyline();
        List<GeoPoint> pts = new ArrayList<>();
        for (LocationHistory loc : historyPoints) {
            pts.add(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
        }
        trailLine.setPoints(pts);
        trailLine.setColor(Color.parseColor("#40AAAAFF"));
        trailLine.setWidth(3f);
        mapView.getOverlays().add(0, trailLine);

        if (playerMarker == null) {
            playerMarker = new Marker(mapView);
            playerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            playerMarker.setTitle(targetUserName != null ? targetUserName : "Konum");
            mapView.getOverlays().add(playerMarker);
        }
        mapView.invalidate();
    }

    private void updateMarker() {
        if (historyPoints.isEmpty() || currentIndex >= historyPoints.size()) return;
        LocationHistory loc = historyPoints.get(currentIndex);
        GeoPoint pt = new GeoPoint(loc.getLatitude(), loc.getLongitude());

        if (playerMarker == null) {
            playerMarker = new Marker(mapView);
            playerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(playerMarker);
        }
        playerMarker.setPosition(pt);

        // Update time label
        tvTime.setText(timeFormat.format(new Date(loc.getTimestamp())));

        // Update seekbar
        int progress = (int) ((currentIndex / (float) (historyPoints.size() - 1)) * 100);
        seekBar.setProgress(progress);

        mapView.getController().animateTo(pt);
        mapView.invalidate();
    }

    private void startPlayback() {
        if (historyPoints.isEmpty()) { Toast.makeText(this, "Veri yok", Toast.LENGTH_SHORT).show(); return; }
        if (currentIndex >= historyPoints.size() - 1) currentIndex = 0;

        isPlaying = true;
        tvStatus.setText("Oynatılıyor...");
        scheduleNext();
    }

    private void scheduleNext() {
        long interval = BASE_INTERVAL_MS / playSpeed;
        playRunnable = () -> {
            if (!isPlaying) return;
            currentIndex++;
            if (currentIndex >= historyPoints.size()) {
                stopPlayback();
                tvStatus.setText("Tamamlandı");
                return;
            }
            updateMarker();
            scheduleNext();
        };
        handler.postDelayed(playRunnable, interval);
    }

    private void pausePlayback() {
        isPlaying = false;
        handler.removeCallbacks(playRunnable);
        tvStatus.setText("Duraklatıldı");
    }

    private void stopPlayback() {
        isPlaying = false;
        handler.removeCallbacks(playRunnable);
        currentIndex = 0;
        updateMarker();
        tvStatus.setText("Durduruldu");
    }

    private void setSpeed(int speed) {
        playSpeed = speed;
        tvSpeed.setText(speed + "x");
        btn1x.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                speed == 1 ? Color.parseColor("#2196F3") : Color.parseColor("#1B2838")));
        btn2x.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                speed == 2 ? Color.parseColor("#FF9800") : Color.parseColor("#1B2838")));
        btn4x.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                speed == 4 ? Color.parseColor("#FF5722") : Color.parseColor("#1B2838")));
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pausePlayback();
    }
}