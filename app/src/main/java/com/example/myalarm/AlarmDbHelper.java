package com.example.myalarm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AlarmDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "AlarmDb";
    private static final int VERSION = 3;
    public static final String TABLE_NAME = "alarms";
    public static final String ID_COLUMN = "id";
    public static final String HOUR_COLUMN = "hour";
    public static final String MINUTE_COLUMN = "minute";
    public static final String ACTIVE_COLUMN = "active";
    public static final String TYPE_COLUMN = "type";
    public static final String IMG_COLUMN = "img";
    public static final String RINGTONE_COLUMN = "ringtone";
    public static final int REPEAT = 1;
    public static final int ONCE = 2;

    public AlarmDbHelper(Context context){
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // создаем таблицу с полями
        db.execSQL("create table " + TABLE_NAME + "("
                + "id integer primary key autoincrement,"
                + "hour integer,"
                + "minute integer,"
                + "active integer,"
                + "type integer default 1,"
                + "img varchar(200),"
                + "ringtone integer default 0" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // добавляем поле type
        if (oldVersion < 2) {
            db.execSQL("alter table " + TABLE_NAME + " add column type integer default 1;");
            db.execSQL("alter table " + TABLE_NAME + " add column img varchar(200);");
        }
        if (oldVersion < 3){
            db.execSQL("alter table " + TABLE_NAME + " add column ringtone integer default 0;");
        }
    }
}
