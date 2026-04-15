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
import com.example.emergencyapp.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mesaj adaptörü.
 *
 * Değişiklikler:
 *  - Her kullanıcıya Constants.NAME_COLORS dizisinden tutarlı bir renk atanır.
 *    Renk, senderId'nin hash'i ile hesaplanır → uygulama yeniden açılsa bile aynı renk.
 *  - Benim mesajlarım → sağa hizalı, #4FC3F7 (açık mavi) isim rengi
 *  - Karşı tarafın mesajları → sola hizalı, kullanıcıya özgü renk
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    /** senderId → atanmış renk önbelleği (aynı kişiye hep aynı renk) */
    private final Map<String, Integer> colorCache = new HashMap<>();

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages      = messages;
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
        boolean isMe    = senderId != null && senderId.equals(currentUserId);

        // ── İsim ──────────────────────────────────────────────────────────────
        String senderName = message.getSenderName();
        holder.tvSender.setText(senderName != null ? senderName : "Bilinmeyen");

        // ── İçerik ────────────────────────────────────────────────────────────
        String content = message.getContent();
        holder.tvContent.setText(content != null ? content : "");

        // ── Zaman ─────────────────────────────────────────────────────────────
        long timestamp = message.getTimestamp();
        holder.tvTime.setText(timestamp > 0
                ? timeFormat.format(new Date(timestamp)) : "");

        // ── Layout parametreleri ───────────────────────────────────────────────
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();

        if (isMe) {
            // Kendi mesajım → sağ taraf, sabit açık mavi isim rengi
            params.gravity      = Gravity.END;
            params.setMarginStart(80);
            params.setMarginEnd(8);
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_me);
            holder.tvSender.setTextColor(Color.parseColor("#4FC3F7"));
        } else {
            // Başkasının mesajı → sol taraf, kullanıcıya özgü renk
            params.gravity      = Gravity.START;
            params.setMarginStart(8);
            params.setMarginEnd(80);
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_other);
            holder.tvSender.setTextColor(resolveUserColor(senderId));
        }
        holder.messageContainer.setLayoutParams(params);

        // ── İpucu vurgusu ─────────────────────────────────────────────────────
        if (message.isHint()) {
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_hint);
            holder.tvHintLabel.setVisibility(View.VISIBLE);
        } else {
            holder.tvHintLabel.setVisibility(View.GONE);
        }
    }

    /**
     * senderId'ye sabit ama tutarlı bir renk döndürür.
     * NAME_COLORS dizisinden hashCode modüle göre seçilir.
     */
    private int resolveUserColor(String senderId) {
        if (senderId == null) return Color.parseColor("#FF8A65");
        if (colorCache.containsKey(senderId)) return colorCache.get(senderId);

        int index = Math.abs(senderId.hashCode()) % Constants.NAME_COLORS.length;
        // NAME_COLORS long dizisi; Color'a çevir
        int color = (int) Constants.NAME_COLORS[index] | 0xFF000000;
        colorCache.put(senderId, color);
        return color;
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainer;
        TextView tvSender;
        TextView tvContent;
        TextView tvTime;
        TextView tvHintLabel;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            tvSender         = itemView.findViewById(R.id.tvSender);
            tvContent        = itemView.findViewById(R.id.tvContent);
            tvTime           = itemView.findViewById(R.id.tvTime);
            tvHintLabel      = itemView.findViewById(R.id.tvHintLabel);
        }
    }
}