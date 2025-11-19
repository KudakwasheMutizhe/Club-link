package com.example.course_link;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AnnouncementAdapter extends ListAdapter<AnnouncementModal, AnnouncementAdapter.AnnouncementViewHolder> {

    private OnAnnouncementClickListener clickListener;

    // Interface for handling clicks
    public interface OnAnnouncementClickListener {
        void onAnnouncementClick(AnnouncementModal announcement);
    }

    public AnnouncementAdapter(OnAnnouncementClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    // DiffUtil for efficient list updates
    private static final DiffUtil.ItemCallback<AnnouncementModal> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AnnouncementModal>() {
                @Override
                public boolean areItemsTheSame(@NonNull AnnouncementModal oldItem, @NonNull AnnouncementModal newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AnnouncementModal oldItem, @NonNull AnnouncementModal newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle()) &&
                            oldItem.getMessage().equals(newItem.getMessage()) &&
                            oldItem.isRead() == newItem.isRead();
                }
            };

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        AnnouncementModal announcement = getItem(position);
        holder.bind(announcement, clickListener);
    }

    // ViewHolder class
    static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvMessage;
        TextView tvDate;
        ImageView ivBookmark;

        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivBookmark = itemView.findViewById(R.id.ivBookmark);
        }

        public void bind(AnnouncementModal announcement, OnAnnouncementClickListener listener) {
            // Set text content
            tvTitle.setText(announcement.getTitle());
            tvMessage.setText(announcement.getMessage());
            tvDate.setText(formatTimeAgo(announcement.getCreatedAt()));

            // Make title bold if unread
            if (announcement.isRead()) {
                tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
            } else {
                tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            }

            // Item click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAnnouncementClick(announcement);
                }
            });

            // Bookmark click (optional - you can add functionality later)
            ivBookmark.setOnClickListener(v -> {
                // TODO: Toggle bookmark status
            });
        }

        // Format timestamp to "2h ago", "Yesterday", etc.
        private String formatTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            long days = TimeUnit.MILLISECONDS.toDays(diff);

            if (seconds < 60) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + "m ago";
            } else if (hours < 24) {
                return hours + "h ago";
            } else if (days == 1) {
                return "Yesterday";
            } else if (days < 7) {
                return days + "d ago";
            } else {
                // For older dates, show actual date
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}