package com.clublink.club_link;

public class Event {
    public long id;
    public String title;
    public String club;
    public long startEpochMillis;
    public String location;
    public String category; // Workshops, Socials, Sports, etc.
    public String imageUrl; // optional (local URI or remote)
    public boolean isBookmarked;
    public boolean isGoing;


    public Event() {}

    public Event(long id, String title, String club, long startEpochMillis, String location,
                 String category, String imageUrl, boolean isBookmarked, boolean isGoing) {
        this.id = id;
        this.title = title;
        this.club = club;
        this.startEpochMillis = startEpochMillis;
        this.location = location;
        this.category = category;
        this.imageUrl = imageUrl;
        this.isBookmarked = isBookmarked;
        this.isGoing = isGoing;
    }
}
