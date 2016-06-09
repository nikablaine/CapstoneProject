package org.kraflapps.motiondroid;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.NameFileComparator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Saves a JPEG {@link Image} into the specified {@link File}.
 */

public class ImageSaver implements Runnable {

    public static final String LOG_TAG = ImageSaver.class.getSimpleName();
    /**
     * The JPEG image.
     */
    private Image mImage;

    /**
     * Bitmap.
     */
    private Bitmap mBitmap;

    /**
     * The file we save the image into.
     */
    private final File mFile;
    private int mCapacity;
    private int mPolicy;

    public ImageSaver(Image image, File file) {
        mImage = image;
        mFile = file;
    }

    public ImageSaver(Bitmap bitmap, File file, int capacity, int policy) {
        mBitmap = bitmap;
        mFile = file;
        mCapacity = capacity;
        mPolicy = policy;
    }

    @Override
    public void run() {

        if (mCapacity != 0) {
            Collection<File> files = FileUtils.listFiles(mFile.getParentFile(), null, true);
            if (files.size() >= mCapacity) {
                switch (mPolicy) {
                    case Util.POLICY_KEEP:
                        return;
                    case Util.POLICY_OVERWRITE:
                        removeOldest(files);
                        break;
                }
            }
        }


        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "File problems", e);
        }


        if (mImage != null) {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            try {
                output.write(bytes);
                Log.i(LOG_TAG, "image saved in " + mFile.getName());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Problems saving the file", e);
                    }
                }
            }
        } else {
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
    }

    private void removeOldest(Collection<File> files) {
        NameFileComparator comparator = new NameFileComparator();
        List<File> sortedList = comparator.sort(new ArrayList<>(files));
        int size = sortedList.size();
        for (int i = 0; i < size - mCapacity + 1; i++) {
            File file = sortedList.get(i);
            try {
                Log.d(LOG_TAG, "Removing file " + file);
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problems removing file", e);
            }
        }
    }
}