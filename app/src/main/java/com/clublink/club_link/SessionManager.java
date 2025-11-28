package com.clublink.club_link;

import android.content.Context;
import android.content.SharedPreferences;


public class SessionManager {
    private static final String KEY_USERNAME = "username";
    String username = "";

    private static final String PREF_NAME = "club_link_prefs";
    private static final String KEY_USER_ID = "USER_ID";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Save the logged-in user's database id.
     */
    public void saveUserId(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    /**
     * Get the currently logged-in user's id.
     * Returns -1 if nobody is logged in.
     */
    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    /**
     * Clear all session info (for logout).
     */
    public void clearSession() {
        prefs.edit().clear().apply();
    }




    public void setUsername(String username) {
        this.username = username;
    }

    public void saveLogin(long userId, String username) {
        this.saveUserId(userId);
        this.setUsername(username);
    }
}
