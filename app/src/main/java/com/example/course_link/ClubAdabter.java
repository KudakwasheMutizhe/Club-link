package com.example.course_link;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class ClubAdabter extends RecyclerView.Adapter<ClubAdabter.ViewHolder> {

    private final Context context;
    private ArrayList<ClubModal> clubModalArrayList;

    private static final String TAG = "ClubAdabter";

    public ClubAdabter(Context context, ArrayList<ClubModal> clubModalArrayList) {
        this.context = context;
        this.clubModalArrayList = clubModalArrayList;
        setHasStableIds(true); // optional if each club has a stable ID
    }

    public void setFilteredList(ArrayList<ClubModal> filteredList) {
        this.clubModalArrayList = filteredList;
        notifyDataSetChanged(); // can be upgraded to DiffUtil later
    }

    @Override
    public long getItemId(int position) {
        // if clubId is unique & stable, use it (or hashCode)
        return clubModalArrayList.get(position).getClubId().hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.all_clubs_rows, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClubModal model = clubModalArrayList.get(position);

        holder.txtClubName.setText(model.getClubName());
        holder.txtClubShortDescription.setText(model.getShortDescription());

        Log.d(TAG, "Bind club: id=" + model.getClubId()
                + " name=" + model.getClubName()
                + " category=" + model.getCategory()
                + " imagePath=" + model.getImagePath());

        String imageUrl = model.getImagePath();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.image_1)
                    .error(R.drawable.image_2)
                    .into(holder.clubImage);
        } else {
            holder.clubImage.setImageResource(R.drawable.image_1);
        }

        holder.itemView.setOnClickListener(view -> {
            int selectPosition = holder.getBindingAdapterPosition();
            if (selectPosition == RecyclerView.NO_POSITION) return;

            ClubModal clicked = clubModalArrayList.get(selectPosition);
            Intent intent = new Intent(view.getContext(), DetailActivity.class);
            intent.putExtra("clubName", clicked.getClubName());
            intent.putExtra("shortDescription", clicked.getShortDescription());
            intent.putExtra("imageUrl", clicked.getImagePath());
            intent.putExtra("clubId", clicked.getClubId());
            intent.putExtra("category", clicked.getCategory());
            view.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return clubModalArrayList != null ? clubModalArrayList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtClubName, txtClubShortDescription;
        ImageView clubImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtClubName = itemView.findViewById(R.id.txtNameClub);
            txtClubShortDescription = itemView.findViewById(R.id.txtShortDescription);
            clubImage = itemView.findViewById(R.id.imageClub);
        }
    }
}

