package com.example.course_link;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class UserDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "clublink.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_FULLNAME = "fullname";
    public static final String COL_EMAIL = "email";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    public UserDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FULLNAME + " TEXT, " +
                COL_EMAIL + " TEXT, " +
                COL_USERNAME + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // Insert a new user (kept as you had it, returns success/failure)
    public boolean insertUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FULLNAME, user.getFullname());
        cv.put(COL_EMAIL, user.getEmail());
        cv.put(COL_USERNAME, user.getUsername());
        cv.put(COL_PASSWORD, user.getPassword());
        long result = db.insert(TABLE_USERS, null, cv);
        db.close();
        return result != -1;
    }

    /**
     * Login method that returns the user's id if credentials are correct,
     * or -1 if login fails.
     */
    public long loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + COL_ID + " FROM " + TABLE_USERS +
                        " WHERE " + COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password}
        );

        long userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getLong(0); // first column = id
        }

        cursor.close();
        db.close();
        return userId;
    }

    // Check login credentials (now just uses loginUser internally)
    public boolean checkUser(String username, String password) {
        return loginUser(username, password) != -1;
    }

    // Get a user by username
    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_FULLNAME, COL_EMAIL, COL_USERNAME, COL_PASSWORD},
                COL_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            String fullname = cursor.getString(cursor.getColumnIndexOrThrow(COL_FULLNAME));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL));
            String uname = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME));
            String pwd = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD));

            user = new User(fullname, email, uname, pwd);
        }

        if (cursor != null) cursor.close();
        db.close();
        return user;
    }

    // 🔹 NEW: Get a user by database id (for ProfileActivity screen)
    // Get a user by ID (for profile screen)
    public User getUserById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_FULLNAME, COL_EMAIL, COL_USERNAME, COL_PASSWORD},
                COL_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            String fullname = cursor.getString(cursor.getColumnIndexOrThrow(COL_FULLNAME));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL));
            String uname = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME));
            String pwd = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD));

            user = new User(fullname, email, uname, pwd);
        }

        if (cursor != null) cursor.close();
        db.close();
        return user;
    }


    // Update password for a given username
    public boolean updatePassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PASSWORD, newPassword);

        int rows = db.update(TABLE_USERS, cv, COL_USERNAME + " = ?", new String[]{username});
        db.close();
        return rows > 0;
    }

    // Delete user by username
    public boolean deleteUser(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_USERS, COL_USERNAME + " = ?", new String[]{username});
        db.close();
        return rows > 0;
    }

    // 🔹 NEW: Delete user by database id (for "Delete account" in ProfileActivity)
    public boolean deleteUserById(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_USERS, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }
    // Simple user model for search list
    public static class SimpleUser {
        public long id;
        public String username;
        public String fullname;

        public SimpleUser(long id, String username, String fullname) {
            this.id = id;
            this.username = username;
            this.fullname = fullname;
        }


        public String getUsername() {
            return username;
        }

        public long getId() {
            return id;
        }

        public String getFullname() {
            return fullname;
        }
    }

    // Get all users except the given id
    public java.util.List<SimpleUser> getAllUsersExcept(long excludeId) {
        java.util.List<SimpleUser> users = new java.util.ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_ID + ", " + COL_USERNAME + ", " + COL_FULLNAME +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COL_ID + " != ?",
                new String[]{String.valueOf(excludeId)}
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME));
                String fullname = cursor.getString(cursor.getColumnIndexOrThrow(COL_FULLNAME));
                users.add(new SimpleUser(id, username, fullname));
            }
            cursor.close();
        }

        db.close();
        return users;
    }

}

