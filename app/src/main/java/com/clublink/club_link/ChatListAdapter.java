package com.clublink.club_link;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Adapter for displaying chat previews in a list.
 * This adapter uses an interface to delegate click handling to the Activity/Fragment.
 */
public class ChatListAdapter extends ListAdapter<ChatPreview, ChatListAdapter.ViewHolder> {

    // An interface that the Activity or Fragment will implement.
    public interface OnChatClickListener {
        void onChatClick(ChatPreview chatPreview);
    }

    private final OnChatClickListener clickListener;

    public ChatListAdapter(@NonNull OnChatClickListener listener) {
        super(DIFF_CALLBACK);
        this.clickListener = listener;
    }

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
        // The ViewHolder binds the data and sets the click listener from the constructor.
        holder.bind(chat, clickListener);
    }

    // DiffUtil helps the RecyclerView efficiently update the list.
    private static final DiffUtil.ItemCallback<ChatPreview> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatPreview>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatPreview oldItem, @NonNull ChatPreview newItem) {
                    return oldItem.getChatId().equals(newItem.getChatId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatPreview oldItem, @NonNull ChatPreview newItem) {
                    return oldItem.equals(newItem); // Use the equals method in ChatPreview for a cleaner check
                }
            };

    /**
     * The ViewHolder for a single chat preview item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvChatName;
        final TextView tvLastMessage;
        final TextView tvTimestamp;
        final TextView tvUnreadBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatName = itemView.findViewById(R.id.tvChatName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }

        void bind(final ChatPreview chat, final OnChatClickListener listener) {
            tvChatName.setText(chat.getChatName());
            tvLastMessage.setText(chat.getLastMessage());
            tvTimestamp.setText(formatTimestamp(chat.getTimestamp()));

            if (chat.getUnreadCount() > 0) {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                tvUnreadBadge.setText(String.valueOf(chat.getUnreadCount()));
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
            }

            // When the item is clicked, call the interface method.
            itemView.setOnClickListener(v -> listener.onChatClick(chat));
        }

        private String formatTimestamp(long timestamp) {
            if (timestamp == 0) return ""; // Guard against invalid timestamp

            Date date = new Date(timestamp);
            Date now = new Date();

            long diff = now.getTime() - date.getTime();
            long oneDay = 24 * 60 * 60 * 1000;
            long sevenDays = 7 * oneDay;

            // Use android.text.format.DateUtils for more reliable formatting if you can,
            // but this logic is also fine.
            if (diff < oneDay && date.getDay() == now.getDay()) {
                // Today
                return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
            } else if (diff < sevenDays) {
                // This week
                return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
            } else {
                // Older
                return new SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(date);
            }
        }
    }
}
