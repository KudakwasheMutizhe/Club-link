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

    Context context;
    ArrayList<ClubModal> clubModalArrayList;

    private static final String TAG = "ClubAdabter";

    public ClubAdabter(Context context, ArrayList<ClubModal> clubModalArrayList) {
        this.context = context;
        this.clubModalArrayList = clubModalArrayList;
    }

    public void setFilteredList(ArrayList<ClubModal> filteredList){
        this.clubModalArrayList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClubAdabter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.all_clubs_rows, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubAdabter.ViewHolder holder, int position) {
        ClubModal model = clubModalArrayList.get(position);

        holder.txtClubName.setText(model.getClubName());
        holder.txtClubShortDescription.setText(model.getShortDescription());

        // Optional: log to verify category + id
        Log.d(TAG, "Bind club: id=" + model.getClubId()
                + " name=" + model.getClubName()
                + " category=" + model.getCategory()
                + " imagePath=" + model.getImagePath());

        // 🔹 Load image from Firebase Storage URL using Glide
        String imageUrl = model.getImagePath();   // this should now be the download URL

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    // replace these with your own drawables if you have them
                    .placeholder(R.drawable.image_1)
                    .error(R.drawable.image_2)
                    .into(holder.clubImage);
        } else {
            // no image → show placeholder
            holder.clubImage.setImageResource(R.drawable.image_1);
        }

        holder.itemView.setOnClickListener(view -> {
            int selectPosition = holder.getBindingAdapterPosition();
            if (selectPosition == RecyclerView.NO_POSITION) return;

            ClubModal clicked = clubModalArrayList.get(selectPosition);
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("clubName", clicked.getClubName());
            intent.putExtra("shortDescription", clicked.getShortDescription());
            intent.putExtra("imageUrl", clicked.getImagePath());
            intent.putExtra("clubId", clicked.getClubId());
            intent.putExtra("category", clicked.getCategory());  // ⬅️ pass category too
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return clubModalArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtClubName, txtClubShortDescription;
        ImageView clubImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtClubName = itemView.findViewById(R.id.txtNameClub);
            txtClubShortDescription = itemView.findViewById(R.id.txtShortDescription);
            clubImage = itemView.findViewById(R.id.imageClub);
            // If you add a category TextView in the row layout, you can bind it here too.
        }
    }
}
