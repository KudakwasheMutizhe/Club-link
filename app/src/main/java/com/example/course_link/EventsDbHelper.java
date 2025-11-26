package com.example.course_link;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class EventsDbHelper extends SQLiteOpenHelper{
    public static final String DB_NAME = "clublink.db";
    public static final int DB_VERSION = 2;


    public static final String T_EVENTS = "events";


    public EventsDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_EVENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "club TEXT, " +
                "start_epoch INTEGER NOT NULL, " +
                "location TEXT, " +
                "category TEXT, " +
                "image_url TEXT, " +
                "is_bookmarked INTEGER DEFAULT 0, " +
                "is_going INTEGER DEFAULT 0)" );

    db.execSQL("INSERT INTO events(title, club, start_epoch, location, category, image_url) VALUES" +
            "('Hack Night', 'CS Club', strftime('%s','now','+3 days')*1000, 'OM 1335', 'Workshops', '')," +
            "('Pumpkin Social', 'TRU Social', strftime('%s','now','+5 days')*1000, 'Student Union', 'Socials', '')," +
            "('Intramural Soccer', 'Athletics', strftime('%s','now','+7 days')*1000, 'Hillside Field', 'Sports', '')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_EVENTS);
        onCreate(db);
    }
}
