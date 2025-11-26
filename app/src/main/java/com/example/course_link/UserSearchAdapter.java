package com.example.course_link;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(UserDbHelper.SimpleUser user);
    }

    private final List<UserDbHelper.SimpleUser> fullList = new ArrayList<>();
    private final List<UserDbHelper.SimpleUser> filteredList = new ArrayList<>();
    private final OnUserClickListener listener;

    public UserSearchAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserDbHelper.SimpleUser> users) {
        fullList.clear();
        fullList.addAll(users);

        filteredList.clear();
        filteredList.addAll(users);

        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String q = query.trim().toLowerCase();
            for (UserDbHelper.SimpleUser u : fullList) {
                if (u.getUsername().toLowerCase().contains(q)
                        || u.getFullname().toLowerCase().contains(q)) {
                    filteredList.add(u);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_row, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserDbHelper.SimpleUser user = filteredList.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        TextView tvFullname;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvFullname = itemView.findViewById(R.id.tvFullname);
        }

        void bind(UserDbHelper.SimpleUser user, OnUserClickListener listener) {
            tvUsername.setText(user.getUsername());
            tvFullname.setText(user.getFullname());
            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }
    }
}
