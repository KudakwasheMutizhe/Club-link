package com.clublink.club_link;

/**
 * User ProfileActivity stored in Firebase Realtime Database under /users/{userId}
 */
public class AppUserFirebase {
    public long id;
    public String fullname;
    public String username;
    public String email;

    // Required empty constructor for Firebase
    public AppUserFirebase() { }

    public AppUserFirebase(long id, String fullname, String username, String email) {
        this.id = id;
        this.fullname = fullname;
        this.username = username;
        this.email = email;
    }
}
