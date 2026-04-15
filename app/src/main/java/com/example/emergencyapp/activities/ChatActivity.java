package com.example.emergencyapp.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencyapp.R;
import com.example.emergencyapp.adapters.MessageAdapter;
import com.example.emergencyapp.models.Message;
import com.example.emergencyapp.utils.Constants;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grup sohbet ekranı.
 *
 * Değişiklikler:
 *  - Mesaj gönderildiğinde diğer tüm kullanıcılara "Yeni mesaj" bildirimi gider
 *    ancak mesaj içeriği bildirimde GÖSTERİLMEZ.
 *  - Bildirim, ekranda chat açıkken gönderilmez (isInForeground bayrağı).
 */
public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    // Bildirim için sabitler (servis kanalından bağımsız)
    private static final String MSG_CHANNEL_ID = "chat_message_channel";
    private static final int    MSG_NOTIF_ID   = 3001;

    // Görünümler
    private RecyclerView  rvMessages;
    private EditText      etMessage;
    private ImageButton   btnSend;
    private ImageButton   btnBack;
    private TextView      tvTitle;

    // Veri
    private PreferenceManager prefManager;
    private MessageAdapter    adapter;
    private final List<Message> messages = new ArrayList<>();
    private DatabaseReference  messagesRef;
    private ValueEventListener messagesListener;

    /** Aktivite ön planda mı? (bildirim göndermemek için) */
    private boolean isInForeground = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        prefManager = new PreferenceManager(this);
        messagesRef = FirebaseDatabase.getInstance().getReference("general_messages");

        ensureMessageChannel();
        initViews();
        setupRecyclerView();
        listenToMessages();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Bildirim kanalı oluştur (IMPORTANCE_DEFAULT → sessiz ama göster)
    // ────────────────────────────────────────────────────────────────────────
    private void ensureMessageChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    MSG_CHANNEL_ID,
                    "Sohbet Mesajları",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("Grup sohbetinde yeni mesaj var");
            ch.setShowBadge(true);
            ch.setSound(null, null); // sessiz
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage  = findViewById(R.id.etMessage);
        btnSend    = findViewById(R.id.btnSend);
        btnBack    = findViewById(R.id.btnBack);
        tvTitle    = findViewById(R.id.tvTitle);

        if (tvTitle != null) tvTitle.setText("Grup Sohbeti");
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messages, prefManager.getUserId());
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(adapter);
    }

    private void sendMessage() {
        if (etMessage == null) return;
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        String userId   = prefManager.getUserId();
        String userName = prefManager.getUserName();
        if (userId == null || userName == null) {
            Log.e(TAG, "Kullanıcı bilgisi bulunamadı");
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) { Log.e(TAG, "Mesaj ID oluşturulamadı"); return; }

        Map<String, Object> msg = new HashMap<>();
        msg.put("id",          messageId);
        msg.put("senderId",    userId);
        msg.put("senderName",  userName);
        msg.put("content",     content);
        msg.put("emergencyId", "general");
        msg.put("isHint",      false);
        msg.put("timestamp",   System.currentTimeMillis());

        messagesRef.child(messageId).setValue(msg)
                .addOnSuccessListener(v -> etMessage.setText(""))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Mesaj gönderilemedi: " + e.getMessage()));
    }

    private void listenToMessages() {
        // Başlangıçta kaç mesaj var? İlk yüklemede bildirim göndermemek için sayıyı tut.
        final long[] initialLoadTime = {System.currentTimeMillis()};

        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();
                Message latestNew = null;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Message m = child.getValue(Message.class);
                    if (m != null) {
                        messages.add(m);
                        // Başlangıçtan sonra gelen + başkasının mesajı
                        if (m.getTimestamp() > initialLoadTime[0]
                                && !prefManager.getUserId().equals(m.getSenderId())) {
                            latestNew = m;
                        }
                    }
                }

                adapter.notifyDataSetChanged();
                if (!messages.isEmpty()) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }

                // Aktivite arka plandaysa bildirim göster (içeriksiz)
                if (latestNew != null && !isInForeground) {
                    showNewMessageNotification(latestNew.getSenderName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Mesajlar alınamadı: " + error.getMessage());
            }
        };

        messagesRef.orderByChild("timestamp").addValueEventListener(messagesListener);
    }

    /**
     * Bildirimde yalnızca "Yeni mesaj var" gösterilir — içerik gizlenir.
     */
    private void showNewMessageNotification(String senderName) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String title = senderName != null ? senderName + " mesaj gönderdi" : "Yeni mesaj";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MSG_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(title)
                .setContentText("Yeni bir mesaj var")   // içerik gösterilmiyor
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Görmek için dokunun"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi);

        NotificationManager nm =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(MSG_NOTIF_ID, builder.build());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Yaşam döngüsü
    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        isInForeground = true;
        // Açık bildirim varsa temizle
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(MSG_NOTIF_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInForeground = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}