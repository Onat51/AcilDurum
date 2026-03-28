package com.example.emergencyapp.adapters;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencyapp.R;
import com.example.emergencyapp.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages;
    private String currentUserId;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        if (message == null) return;

        String senderId = message.getSenderId();
        boolean isMe = senderId != null && senderId.equals(currentUserId);

        // Gönderen adı
        String senderName = message.getSenderName();
        holder.tvSender.setText(senderName != null ? senderName : "Bilinmeyen");

        // Mesaj içeriği
        String content = message.getContent();
        holder.tvContent.setText(content != null ? content : "");

        // Zaman
        long timestamp = message.getTimestamp();
        if (timestamp > 0) {
            holder.tvTime.setText(timeFormat.format(new Date(timestamp)));
        } else {
            holder.tvTime.setText("");
        }

        // Stil ayarla - ben mi yoksa başkası mı
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();

        if (isMe) {
            params.gravity = Gravity.END;
            params.setMarginStart(80);
            params.setMarginEnd(8);
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_me);
            holder.tvSender.setTextColor(Color.parseColor("#4FC3F7"));
        } else {
            params.gravity = Gravity.START;
            params.setMarginStart(8);
            params.setMarginEnd(80);
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_other);
            holder.tvSender.setTextColor(Color.parseColor("#FF8A65"));
        }
        holder.messageContainer.setLayoutParams(params);

        // İpucu mesajı ise vurgula
        if (message.isHint()) {
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_hint);
            holder.tvHintLabel.setVisibility(View.VISIBLE);
        } else {
            holder.tvHintLabel.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainer;
        TextView tvSender;
        TextView tvContent;
        TextView tvTime;
        TextView tvHintLabel;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvHintLabel = itemView.findViewById(R.id.tvHintLabel);
        }
    }
}