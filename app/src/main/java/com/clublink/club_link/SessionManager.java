package com.clublink.club_link;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "club_link_prefs";
    private static final String KEY_USER_ID = "USER_ID";
    private static final String KEY_USERNAME = "username";

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

    /**
     * Save the username to SharedPreferences.
     */
    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    /**
     * Get the currently logged-in user's username.
     * Returns null if nobody is logged in.
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    /**
     * Save both user ID and username in one call.
     */
    public void saveLogin(long userId, String username) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    /**
     * Clear all session info (for logout).
     */
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}