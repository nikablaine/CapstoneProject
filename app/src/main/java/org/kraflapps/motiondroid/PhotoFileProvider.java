package org.kraflapps.motiondroid;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */

public class PhotoFileProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private PhotoDbHelper photoDbHelper;

    private static final int PHOTO = 555;

    @Override
    public boolean onCreate() {
        photoDbHelper = new PhotoDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case PHOTO: {
                retCursor = photoDbHelper.getReadableDatabase().query(
                        PhotoContract.PhotoEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            }
            default:
                throw new IllegalStateException("Bad uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PHOTO:
                return PhotoContract.PhotoEntry.CONTENT_TYPE;
            default:
                throw new IllegalStateException("Bad uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = photoDbHelper.getWritableDatabase();
        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case PHOTO: {
                long _id = db.insert(PhotoContract.PhotoEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = PhotoContract.PhotoEntry.buildPhotoUri(_id);
                } else {
                    throw new IllegalStateException("Could not perform insert");
                }
                break;
            }
            default:
                throw new IllegalStateException("Bad uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = photoDbHelper.getWritableDatabase();
        int rowsDeleted;
        if (selection == null) {
            selection = "1";
        }

        switch (sUriMatcher.match(uri)) {
            case PHOTO:
                rowsDeleted = db.delete(PhotoContract.PhotoEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Bad uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = photoDbHelper.getWritableDatabase();
        int rowsUpdated;

        switch (sUriMatcher.match(uri)) {
            case PHOTO:
                rowsUpdated = db.update(PhotoContract.PhotoEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Bad uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = PhotoContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, PhotoContract.PATH_PHOTOS, PHOTO);

        return matcher;
    }
}
