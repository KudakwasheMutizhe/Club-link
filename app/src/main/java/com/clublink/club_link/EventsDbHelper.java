package com.clublink.club_link;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class EventsDbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "clublink.db";
    public static final int DB_VERSION = 3;


    public static final String T_EVENTS = "events";


    public EventsDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        android.util.Log.d("EventsDbHelper", "onCreate: creating events table");
        db.execSQL("CREATE TABLE " + T_EVENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "club TEXT, " +
                "start_epoch INTEGER NOT NULL, " +
                "location TEXT, " +
                "category TEXT, " +
                "image_url TEXT, " +
                "is_bookmarked INTEGER DEFAULT 0, " +
                "is_going INTEGER DEFAULT 0)");

        db.execSQL("INSERT INTO events(title, club, start_epoch, location, category, image_url) VALUES" +
                "('Hack Night', 'CS Club', strftime('%s','now','+3 days')*1000, 'OM 1335', 'Workshops', '')," +
                "('Pumpkin Social', 'TRU Social', strftime('%s','now','+5 days')*1000, 'Student Union', 'Socials', '')," +
                "('Intramural Soccer', 'Athletics', strftime('%s','now','+7 days')*1000, 'Hillside Field', 'Sports', '')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        android.util.Log.d("EventsDbHelper", "onUpgrade: " + oldVersion + " -> " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + T_EVENTS);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Ensure the events table exists even if the DB file was created earlier without it.
        // Use IF NOT EXISTS so this is safe to call every open.
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_EVENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "club TEXT, " +
                "start_epoch INTEGER NOT NULL, " +
                "location TEXT, " +
                "category TEXT, " +
                "image_url TEXT, " +
                "is_bookmarked INTEGER DEFAULT 0, " +
                "is_going INTEGER DEFAULT 0)");
    }
}