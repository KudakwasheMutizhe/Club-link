package com.example.course_link;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class UserDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "clublink.db";
    private static final int DATABASE_VERSION = 1;

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

    // Insert a new user
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

    // Check login credentials
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS +
                        " WHERE " + COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
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
}
