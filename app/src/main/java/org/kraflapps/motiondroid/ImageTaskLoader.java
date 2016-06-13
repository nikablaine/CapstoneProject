package org.kraflapps.motiondroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.FileObserver;
import android.support.v4.content.AsyncTaskLoader;

import java.io.File;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */
public class ImageTaskLoader extends AsyncTaskLoader<Bitmap> {

    private String mDirName;
    private String mPath;
    private FileObserver mObserver;

    public ImageTaskLoader(String dirName, Context context) {
        super(context);
        mDirName = dirName;
        mObserver = new FileObserver(mDirName) {
            @Override
            public void onEvent(int event, String path) {
                if (event == CLOSE_WRITE) {
                    mPath = path;
                    onContentChanged();
                }
            }
        };
        mObserver.startWatching();
    }

    @Override
    public Bitmap loadInBackground() {
        File file = new File(mDirName, mPath);
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }
}
