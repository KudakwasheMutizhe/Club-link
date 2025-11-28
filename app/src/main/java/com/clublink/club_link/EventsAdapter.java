package com.clublink.club_link;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.clublink.club_link.R;

public class EventsAdapter extends ListAdapter<Event, EventsAdapter.VH>{
    public interface Callbacks {

        void onRsvpClicked(Event e);
        void onItemClicked(Event e);
    }


    private final Callbacks callbacks;


    public EventsAdapter(Callbacks callbacks) {
        super(DIFF);
        this.callbacks = callbacks;
    }

    private static final DiffUtil.ItemCallback<Event> DIFF = new DiffUtil.ItemCallback<Event>() {
        @Override public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) { return oldItem.id == newItem.id; }
        @Override public boolean areContentsTheSame(@NonNull Event o, @NonNull Event n) {
            return o.title.equals(n.title) && o.isBookmarked == n.isBookmarked && o.isGoing == n.isGoing && o.startEpochMillis == n.startEpochMillis &&
                    safe(o.club).equals(safe(n.club)) && safe(o.location).equals(safe(n.location)) && safe(o.category).equals(safe(n.category)) && safe(o.imageUrl).equals(safe(n.imageUrl));
        }
        private String safe(String s){ return s==null?"":s; }
    };


    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new VH(v);
    }


    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Event e = getItem(pos);
        h.title.setText(e.title);
        h.club.setText(e.club);
        String when = DateFormat.format("EEE, MMM d • h:mm a", e.startEpochMillis).toString();
        h.dateTime.setText(when);
        h.location.setText(e.location);


        Glide.with(h.banner.getContext())
                .load(e.imageUrl == null || e.imageUrl.isEmpty() ? R.drawable.ic_event_placeholder : e.imageUrl)
                .into(h.banner);




        h.rsvp.setText(e.isGoing ? "Going" : "RSVP");
        h.rsvp.setOnClickListener(v -> callbacks.onRsvpClicked(e));


        h.itemView.setOnClickListener(v -> callbacks.onItemClicked(e));
    }


    static class VH extends RecyclerView.ViewHolder {
        ImageView banner; TextView title; TextView club; TextView dateTime; TextView location; com.google.android.material.button.MaterialButton rsvp; ImageButton bookmark;
        VH(@NonNull View v) {
            super(v);
            banner = v.findViewById(R.id.imgBanner);
            title = v.findViewById(R.id.txtTitle);
            club = v.findViewById(R.id.txtTitle);
            dateTime = v.findViewById(R.id.txtDateTime);
            location = v.findViewById(R.id.txtLocation);
            rsvp = v.findViewById(R.id.btnRsvp);

        }
    }

}
