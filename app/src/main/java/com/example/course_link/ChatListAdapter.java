package com.example.course_link;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Adapter for displaying chat previews in a list
 */
public class ChatListAdapter extends ListAdapter<ChatPreview, ChatListAdapter.ViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(ChatPreview chatPreview);
    }

    private final OnChatClickListener clickListener;

    public ChatListAdapter(OnChatClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<ChatPreview> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatPreview>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatPreview oldItem,
                                               @NonNull ChatPreview newItem) {
                    return oldItem.getChatId().equals(newItem.getChatId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatPreview oldItem,
                                                  @NonNull ChatPreview newItem) {
                    return oldItem.getLastMessage().equals(newItem.getLastMessage())
                            && oldItem.getTimestamp() == newItem.getTimestamp()
                            && oldItem.getUnreadCount() == newItem.getUnreadCount();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatPreview chat = getItem(position);
        holder.bind(chat, clickListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatName;
        TextView tvLastMessage;
        TextView tvTimestamp;
        TextView tvUnreadBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatName = itemView.findViewById(R.id.tvChatName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }

        void bind(ChatPreview chat, OnChatClickListener listener) {
            tvChatName.setText(chat.getChatName());
            tvLastMessage.setText(chat.getLastMessage());
            tvTimestamp.setText(formatTimestamp(chat.getTimestamp()));

            if (chat.getUnreadCount() > 0) {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                tvUnreadBadge.setText(String.valueOf(chat.getUnreadCount()));
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onChatClick(chat));
        }

        private String formatTimestamp(long timestamp) {
            Date date = new Date(timestamp);
            Date now = new Date();

            long diff = now.getTime() - timestamp;
            long oneDay = 24 * 60 * 60 * 1000;

            if (diff < oneDay) {
                // Today - show time
                return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
            } else if (diff < 7 * oneDay) {
                // This week - show day
                return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
            } else {
                // Older - show date
                return new SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(date);
            }
        }
    }
}