package org.kraflapps.motiondroid;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */

public class PhotoContract {

    public static final String CONTENT_AUTHORITY = "org.kraflapps.motiondroid.PhotoFileProvider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_PHOTOS = "photos";

    public static final class PhotoEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PHOTOS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PHOTOS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PHOTOS;

        public static final String TABLE_NAME = "photos";
        public static final String PHOTO_ID = "id";
        public static final String DIFF = "diff";

        public static Uri buildPhotoUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
