package org.kraflapps.motiondroid;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */

public class PreviewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Bitmap> {

    public static final String FRAGMENT_TAG = "PREVIEW_FRAGMENT";
    private static final String LOG_TAG = PreviewFragment.class.getName();
    public static final int LOADER_ID = 100;
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

        Loader loader = getLoaderManager().getLoader(LOADER_ID);
        if (loader == null) {
            Bundle args = new Bundle();
            args.putString(getString(R.string.pref_folder_key), dirName);
            getLoaderManager().initLoader(LOADER_ID, args, PreviewFragment.this);
        }

        super.onStart();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID) {
            String dirName = args.getString(getString(R.string.pref_folder_key));
            return new ImageTaskLoader(dirName, getContext());
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Bitmap> loader, Bitmap bitmap) {
        if (bitmap != null) {
            ((ImageView) mRootView.findViewById(R.id.imageView)).setImageBitmap(bitmap);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
}
