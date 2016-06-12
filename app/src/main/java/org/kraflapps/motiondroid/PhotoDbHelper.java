package org.kraflapps.motiondroid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */

public class PhotoDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    static final String DATABASE_NAME = "photos.db";

    public PhotoDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_PHOTO_TABLE = "CREATE TABLE " + PhotoContract.PhotoEntry.TABLE_NAME + " (" +
                PhotoContract.PhotoEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PhotoContract.PhotoEntry.PHOTO_ID + " LONG NOT NULL," +
                PhotoContract.PhotoEntry.DIFF + " DOUBLE);";

        db.execSQL(SQL_CREATE_PHOTO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PhotoContract.PhotoEntry.TABLE_NAME);
        onCreate(db);
    }
}
