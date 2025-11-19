package com.example.club_link;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class MessageAdapter extends ListAdapter<MessageModal, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MINE = 1;
    private static final int VIEW_TYPE_THEIRS = 2;

    private final MyIdProvider myIdProvider;

    // Interface to get current user's ID from MainActivity
    public interface MyIdProvider {
        String getMyId();
    }

    public MessageAdapter(MyIdProvider myIdProvider) {
        super(DIFF_CALLBACK);
        this.myIdProvider = myIdProvider;
    }

    // DiffUtil for efficient list updates
    private static final DiffUtil.ItemCallback<MessageModal> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MessageModal>() {
                @Override
                public boolean areItemsTheSame(@NonNull MessageModal oldItem,
                                               @NonNull MessageModal newItem) {
                    return oldItem.getId() != null
                            && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MessageModal oldItem,
                                                  @NonNull MessageModal newItem) {
                    return safeEquals(oldItem.getText(), newItem.getText())
                            && safeEquals(oldItem.getSenderName(), newItem.getSenderName())
                            && oldItem.getCreatedAt() == newItem.getCreatedAt();
                }

                private boolean safeEquals(String a, String b) {
                    if (a == null && b == null) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };

    @Override
    public int getItemViewType(int position) {
        MessageModal message = getItem(position);
        String senderId = message.getSenderId();
        String myId = myIdProvider.getMyId();

        if (senderId != null && myId != null && senderId.equals(myId)) {
            return VIEW_TYPE_MINE;
        } else {
            return VIEW_TYPE_THEIRS;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_MINE) {
            View view = inflater.inflate(R.layout.message_mine, parent, false);
            return new MyMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.message_theirs, parent, false);
            return new TheirMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position) {
        MessageModal message = getItem(position);

        if (holder instanceof MyMessageViewHolder) {
            ((MyMessageViewHolder) holder).bind(message);
        } else if (holder instanceof TheirMessageViewHolder) {
            ((TheirMessageViewHolder) holder).bind(message);
        }
    }

    // ViewHolder for MY messages (right side with message_mine)
    static class MyMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;

        MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvText);
        }

        void bind(MessageModal message) {
            tvText.setText(message.getText());
        }
    }

    // ViewHolder for THEIR messages (left side with message_theirs)
    static class TheirMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName;
        TextView tvText;

        TheirMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvText = itemView.findViewById(R.id.tvText);
        }

        void bind(MessageModal message) {
            String name = message.getSenderName();
            if (name == null || name.trim().isEmpty()) {
                name = "Unknown";
            }
            tvSenderName.setText(name);
            tvText.setText(message.getText());
        }
    }
}
