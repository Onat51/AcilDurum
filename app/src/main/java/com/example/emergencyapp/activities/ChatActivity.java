package com.example.emergencyapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencyapp.R;
import com.example.emergencyapp.adapters.MessageAdapter;
import com.example.emergencyapp.models.Message;
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

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnBack;
    private TextView tvTitle;

    private PreferenceManager prefManager;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private DatabaseReference messagesRef;
    private ValueEventListener messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        prefManager = new PreferenceManager(this);

        // Firebase referansını başlat
        messagesRef = FirebaseDatabase.getInstance().getReference("general_messages");

        initViews();
        setupRecyclerView();
        listenToMessages();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);

        if (tvTitle != null) {
            tvTitle.setText("Grup Sohbeti");
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendMessage());
        }
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messages, prefManager.getUserId());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void sendMessage() {
        if (etMessage == null) return;

        String content = etMessage.getText().toString().trim();

        if (content.isEmpty()) {
            return;
        }

        String userId = prefManager.getUserId();
        String userName = prefManager.getUserName();

        if (userId == null || userName == null) {
            Log.e(TAG, "Kullanıcı bilgisi bulunamadı");
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Log.e(TAG, "Mesaj ID oluşturulamadı");
            return;
        }

        // HashMap ile mesaj oluştur
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("id", messageId);
        messageMap.put("senderId", userId);
        messageMap.put("senderName", userName);
        messageMap.put("content", content);
        messageMap.put("emergencyId", "general");
        messageMap.put("isHint", false);
        messageMap.put("timestamp", System.currentTimeMillis());

        // Firebase'e gönder
        messagesRef.child(messageId).setValue(messageMap)
                .addOnSuccessListener(aVoid -> {
                    // Başarılı - sadece text'i temizle
                    etMessage.setText("");
                })
                .addOnFailureListener(e -> {
                    // Hata durumunda log tut
                    Log.e(TAG, "Mesaj gönderilemedi: " + e.getMessage());
                });
    }

    private void listenToMessages() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message message = child.getValue(Message.class);
                    if (message != null) {
                        messages.add(message);
                    }
                }
                adapter.notifyDataSetChanged();

                // En son mesaja scroll
                if (!messages.isEmpty()) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Mesajlar alınamadı: " + error.getMessage());
            }
        };

        messagesRef.orderByChild("timestamp").addValueEventListener(messagesListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}