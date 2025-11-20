package com.example.course_link;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EventsRepository {
    private final EventsDbHelper helper;


    public EventsRepository(Context ctx) { this.helper = new EventsDbHelper(ctx); }


    public List<Event> getAllUpcoming() {
        SQLiteDatabase db = helper.getReadableDatabase();
        long now = System.currentTimeMillis();
        Cursor c = db.query(EventsDbHelper.T_EVENTS, null,
                "start_epoch >= ?", new String[]{String.valueOf(now)},
                null, null, "start_epoch ASC");
        try { return map(c); } finally { c.close(); }
    }
    public List<Event> getOnDay(long dayUtcMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dayUtcMillis);
// start of day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
// end of day
        cal.add(Calendar.DAY_OF_MONTH, 1);
        long end = cal.getTimeInMillis();


        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(EventsDbHelper.T_EVENTS, null,
                "start_epoch >= ? AND start_epoch < ?",
                new String[]{String.valueOf(start), String.valueOf(end)},
                null, null, "start_epoch ASC");
        try { return map(c); } finally { c.close(); }
    }


    public List<Event> search(String query, String categoryOrNull) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<String> args = new ArrayList<>();
        StringBuilder sel = new StringBuilder("(title LIKE ? OR club LIKE ? OR location LIKE ?)");
        String like = "%" + query + "%";
        args.add(like); args.add(like); args.add(like);
        sel.append(" AND start_epoch >= ?");
        args.add(String.valueOf(System.currentTimeMillis()));
        if (categoryOrNull != null && !categoryOrNull.equals("All")) {
            sel.append(" AND category = ?");
            args.add(categoryOrNull);
        }
        Cursor c = db.query(EventsDbHelper.T_EVENTS, null, sel.toString(), args.toArray(new String[0]), null, null, "start_epoch ASC");
        try { return map(c); } finally { c.close(); }
    }


    public void toggleBookmark(long id, boolean value) { updateFlag(id, "is_bookmarked", value); }
    public void toggleGoing(long id, boolean value) { updateFlag(id, "is_going", value); }


    private void updateFlag(long id, String col, boolean value) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(col, value ? 1 : 0);
        db.update(EventsDbHelper.T_EVENTS, cv, "id=?", new String[]{String.valueOf(id)});
    }


    private List<Event> map(Cursor c) {
        List<Event> list = new ArrayList<>();
        while (c.moveToNext()) {
            Event e = new Event();
            e.id = c.getLong(c.getColumnIndexOrThrow("id"));
            e.title = c.getString(c.getColumnIndexOrThrow("title"));
            e.club = c.getString(c.getColumnIndexOrThrow("club"));
            e.startEpochMillis = c.getLong(c.getColumnIndexOrThrow("start_epoch"));
            e.location = c.getString(c.getColumnIndexOrThrow("location"));
            e.category = c.getString(c.getColumnIndexOrThrow("category"));
            e.imageUrl = c.getString(c.getColumnIndexOrThrow("image_url"));
            e.isBookmarked = c.getInt(c.getColumnIndexOrThrow("is_bookmarked")) == 1;
            e.isGoing = c.getInt(c.getColumnIndexOrThrow("is_going")) == 1;
            list.add(e);
        }
        return list;
    }

}
