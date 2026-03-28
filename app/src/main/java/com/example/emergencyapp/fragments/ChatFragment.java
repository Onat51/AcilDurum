package com.example.emergencyapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import java.util.List;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend;
    private CheckBox cbHint;

    private String emergencyId;
    private boolean isSender;
    private PreferenceManager prefManager;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();

    private DatabaseReference messagesRef;
    private DatabaseReference hintsRef;
    private ValueEventListener messagesListener;

    public static ChatFragment newInstance(String emergencyId, boolean isSender) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("emergency_id", emergencyId);
        args.putBoolean("is_sender", isSender);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            emergencyId = getArguments().getString("emergency_id");
            isSender = getArguments().getBoolean("is_sender");
        }
        prefManager = new PreferenceManager(requireContext());

        // Firebase referansları
        messagesRef = FirebaseDatabase.getInstance()
                .getReference("messages").child(emergencyId);
        hintsRef = FirebaseDatabase.getInstance()
                .getReference("hints").child(emergencyId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMessages = view.findViewById(R.id.rvMessages);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        cbHint = view.findViewById(R.id.cbHint);

        // İpucu checkbox'ı sadece gönderene göster
        cbHint.setVisibility(isSender ? View.VISIBLE : View.GONE);

        setupRecyclerView();

        btnSend.setOnClickListener(v -> sendMessage());

        listenToMessages();
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messages, prefManager.getUserId());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        String oderId = prefManager.getUserId();
        String userName = prefManager.getUserName();

        if (oderId == null || userName == null) {
            Toast.makeText(getContext(), "Kullanıcı bilgisi bulunamadı!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isHint = cbHint.isChecked();

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Toast.makeText(getContext(), "Mesaj gönderilemedi!", Toast.LENGTH_SHORT).show();
            return;
        }

        Message message = new Message();
        message.setId(messageId);
        message.setSenderId(oderId);
        message.setSenderName(userName);
        message.setContent(content);
        message.setEmergencyId(emergencyId);
        message.setHint(isHint);
        message.setTimestamp(System.currentTimeMillis());

        // Mesajı kaydet
        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Mesaj gönderildi: " + content);
                    etMessage.setText("");
                    cbHint.setChecked(false);

                    // Eğer ipucu ise hints'e de kaydet
                    if (isHint) {
                        hintsRef.child(messageId).setValue(message);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Mesaj gönderilemedi: " + e.getMessage());
                    Toast.makeText(getContext(), "Mesaj gönderilemedi!", Toast.LENGTH_SHORT).show();
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

                if (!messages.isEmpty()) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }

                Log.d(TAG, "Acil durum mesaj sayısı: " + messages.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Mesajlar alınamadı: " + error.getMessage());
            }
        };

        messagesRef.orderByChild("timestamp").addValueEventListener(messagesListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}