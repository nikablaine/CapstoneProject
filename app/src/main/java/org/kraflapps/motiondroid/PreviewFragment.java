package org.kraflapps.motiondroid;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileObserver;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */

public class PreviewFragment extends Fragment {

    public static final String FRAGMENT_TAG = "PREVIEW_FRAGMENT";
    private static final String LOG_TAG = PreviewFragment.class.getName();
    private FileObserver mFileObserver;
    private View mRootView;

    public PreviewFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_preview, container, false);
        return mRootView;
    }

    @Override
    public void onStart() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final String dirName = sharedPreferences.getString(getResources().getString(R.string.pref_folder_key), null);
        mFileObserver = new FileObserver(dirName) {
            @Override
            public void onEvent(int event, String path) {
                if (event == CLOSE_WRITE) {
                    new ShowImageTask(dirName, path).execute();
                }
            }
        };
        mFileObserver.startWatching();
        super.onStart();
    }

    class ShowImageTask extends AsyncTask<Void, Void, Bitmap> {

        private String mDirName;
        private String mPath;

        public ShowImageTask(String dirName, String path) {
            mDirName = dirName;
            mPath = path;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            File file = new File(mDirName, mPath);
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                ((ImageView) mRootView.findViewById(R.id.imageView)).setImageBitmap(bitmap);
            }
        }
    }
}
