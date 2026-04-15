package com.example.emergencyapp.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencyapp.R;
import com.example.emergencyapp.models.User;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Kullanıcının kimin alarmını alacağını ve kimin alarmı alacağını seçtiği ekran.
 *
 * Firebase yapısı:
 *   user_connections/{userId}/sends_to/{targetId} = true   → userId, targetId'ye alarm gönderir
 *   user_connections/{userId}/receives_from/{sourceId} = true → userId, sourceId'den alarm alır
 *
 * Bu iki liste karşılıklı olarak senkronize tutulur.
 */
public class UserSelectionActivity extends AppCompatActivity {

    private static final String TAG = "UserSelectionActivity";
    private static final String PATH_CONNECTIONS = "user_connections";

    // Görünümler
    private LinearLayout userListLayout;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private ImageButton btnBack;

    // Veri
    private PreferenceManager prefManager;
    private DatabaseReference usersRef;
    private DatabaseReference connectionsRef;
    private ValueEventListener usersListener;

    /** Admin modunda başka bir kullanıcı için düzenleme yapılıyorsa dolu olur */
    private String targetUserId = null;
    private String targetUserName = null;

    /** Mevcut seçili bağlantılar (targetUser'ın alarm göndereceği kişiler) */
    private Set<String> currentSendsTo = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_selection);

        prefManager = new PreferenceManager(this);

        // Admin başkası için düzenliyorsa intent'ten al
        targetUserId   = getIntent().getStringExtra("target_user_id");
        targetUserName = getIntent().getStringExtra("target_user_name");

        // Hedef yoksa kendi kullanıcımız
        if (targetUserId == null) {
            targetUserId   = prefManager.getUserId();
            targetUserName = prefManager.getUserName();
        }

        usersRef       = FirebaseDatabase.getInstance().getReference(Constants.PATH_USERS);
        connectionsRef = FirebaseDatabase.getInstance()
                .getReference(PATH_CONNECTIONS)
                .child(targetUserId)
                .child("sends_to");

        initViews();
        loadCurrentConnections();
    }

    private void initViews() {
        userListLayout = findViewById(R.id.userListLayout);
        tvTitle        = findViewById(R.id.tvTitle);
        tvSubtitle     = findViewById(R.id.tvSubtitle);
        btnBack        = findViewById(R.id.btnBack);

        // Başlık
        boolean isAdminMode = getIntent().getBooleanExtra("admin_mode", false);
        if (isAdminMode && targetUserName != null) {
            tvTitle.setText("👤 " + targetUserName + " – Bağlantılar");
            tvSubtitle.setText("Bu kişinin alarm göndereceği kullanıcıları seçin");
        } else {
            tvTitle.setText("📡 Alarm Bağlantıları");
            tvSubtitle.setText("Alarm gönderileceği ve alınacağı kişileri seçin");
        }

        btnBack.setOnClickListener(v -> finish());
    }

    /** Önce mevcut "sends_to" listesini çek, sonra tüm kullanıcıları yükle */
    private void loadCurrentConnections() {
        connectionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentSendsTo.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean val = child.getValue(Boolean.class);
                    if (Boolean.TRUE.equals(val)) {
                        currentSendsTo.add(child.getKey());
                    }
                }
                loadUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Bağlantılar okunamadı: " + error.getMessage());
                loadUsers();
            }
        });
    }

    private void loadUsers() {
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userListLayout.removeAllViews();

                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user == null || user.getId() == null) continue;

                    // Kendisini listede gösterme
                    if (user.getId().equals(targetUserId)) continue;

                    addUserRow(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserSelectionActivity.this,
                        "Kullanıcılar yüklenemedi", Toast.LENGTH_SHORT).show();
            }
        };

        usersRef.addValueEventListener(usersListener);
    }

    private void addUserRow(User user) {
        // Satır konteyneri
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(20, 16, 20, 16);
        row.setBackgroundResource(R.drawable.bg_user_item);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, 10);
        row.setLayoutParams(rowParams);

        // Kullanıcıya özgü renk (Constants.NAME_COLORS dizisinden)
        int colorIndex = Math.abs(user.getId().hashCode()) % Constants.NAME_COLORS.length;
        int nameColor  = (int) Constants.NAME_COLORS[colorIndex] | 0xFF000000; // alpha ekle

        // Avatar dairesi
        TextView tvAvatar = new TextView(this);
        tvAvatar.setText(user.getName().isEmpty() ? "?" :
                String.valueOf(user.getName().charAt(0)).toUpperCase());
        tvAvatar.setTextColor(Color.WHITE);
        tvAvatar.setTextSize(18);
        tvAvatar.setGravity(android.view.Gravity.CENTER);
        tvAvatar.setBackground(makeCircleDrawable(nameColor));

        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(52, 52);
        avatarParams.setMarginEnd(14);
        tvAvatar.setLayoutParams(avatarParams);

        // İsim
        TextView tvName = new TextView(this);
        tvName.setText(user.getName());
        tvName.setTextColor(nameColor);
        tvName.setTextSize(16);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameParams);

        // Switch
        SwitchMaterial sw = new SwitchMaterial(this);
        sw.setChecked(currentSendsTo.contains(user.getId()));
        sw.setThumbTintList(android.content.res.ColorStateList.valueOf(
                sw.isChecked() ? Color.parseColor("#4CAF50") : Color.parseColor("#78909C")));

        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sw.setThumbTintList(android.content.res.ColorStateList.valueOf(
                    isChecked ? Color.parseColor("#4CAF50") : Color.parseColor("#78909C")));
            updateConnection(user.getId(), isChecked);
        });

        row.addView(tvAvatar);
        row.addView(tvName);
        row.addView(sw);
        userListLayout.addView(row);
    }

    /**
     * Bağlantıyı Firebase'de günceller.
     * sends_to  → targetUserId'den user.id'ye alarm gider
     * receives_from → user.id'nin listesinde targetUserId ekle/çıkar
     *   (karşılıklı senkron: A→B seçilince B de A'nın alarmını alır)
     */
    private void updateConnection(String otherId, boolean connected) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference(PATH_CONNECTIONS);

        Map<String, Object> updates = new HashMap<>();

        if (connected) {
            // targetUser → other
            updates.put(targetUserId + "/sends_to/"   + otherId, true);
            // other → targetUser (karşılıklı)
            updates.put(otherId    + "/receives_from/" + targetUserId, true);
        } else {
            // Sil
            updates.put(targetUserId + "/sends_to/"   + otherId, null);
            updates.put(otherId    + "/receives_from/" + targetUserId, null);
        }

        root.updateChildren(updates)
                .addOnSuccessListener(v -> {
                    if (connected) currentSendsTo.add(otherId);
                    else           currentSendsTo.remove(otherId);
                    Toast.makeText(this,
                            connected ? "Bağlantı eklendi ✓" : "Bağlantı kaldırıldı",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Programatik yuvarlak arka plan oluşturur */
    private android.graphics.drawable.GradientDrawable makeCircleDrawable(int color) {
        android.graphics.drawable.GradientDrawable gd =
                new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(color);
        return gd;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
    }
}