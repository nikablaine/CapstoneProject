package org.kraflapps.motiondroid;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class.
 */

public class Util {

    public static final int POLICY_OVERWRITE = 0;
    public static final int POLICY_KEEP = 1;

    /**
     * Analyzes the difference between bitmaps.
     *
     * @param bitmap1 first bitmap
     * @param bitmap2 second bitmap
     * @return difference in percents
     */
    public static double percentDifference(Bitmap bitmap1, Bitmap bitmap2) {

        long diff = 0;
        int height = bitmap1.getHeight();
        int width = bitmap1.getWidth();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = bitmap1.getPixel(x, y);
                int rgb2 = bitmap2.getPixel(x, y);
                int r1 = (rgb1 >> 16) & 0xff;
                int g1 = (rgb1 >> 8) & 0xff;
                int b1 = (rgb1) & 0xff;
                int r2 = (rgb2 >> 16) & 0xff;
                int g2 = (rgb2 >> 8) & 0xff;
                int b2 = (rgb2) & 0xff;
                diff += Math.abs(r1 - r2);
                diff += Math.abs(g1 - g2);
                diff += Math.abs(b1 - b2);
            }
        }
        double n = width * height * 3;
        double p = diff / n / 255.0;
        return (p * 100.0);
    }

    /**
     * Analyzes 1000 random pixels and calculates the difference percentage between them.
     *
     * @param bitmap1 first bitmap
     * @param bitmap2 second bitmap
     * @return difference in percents
     */
    static double percentDifferenceLite(Bitmap bitmap1, Bitmap bitmap2) {

        long diff = 0;
        int height = bitmap1.getHeight();
        int width = bitmap1.getWidth();

        Random random = new Random();
        List<Pair<Integer, Integer>> generated = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            while (true) {
                Integer nextX = random.nextInt(width);
                Integer nextY = random.nextInt(height);
                Pair<Integer, Integer> next = new Pair<>(nextX, nextY);

                if (!generated.contains(next)) {
                    generated.add(next);
                    break;
                }
            }
        }

        for (Pair<Integer, Integer> pair : generated) {
            int x = pair.first;
            int y = pair.second;
            int rgb1 = bitmap1.getPixel(x, y);
            int rgb2 = bitmap2.getPixel(x, y);
            int r1 = (rgb1 >> 16) & 0xff;
            int g1 = (rgb1 >> 8) & 0xff;
            int b1 = (rgb1) & 0xff;
            int r2 = (rgb2 >> 16) & 0xff;
            int g2 = (rgb2 >> 8) & 0xff;
            int b2 = (rgb2) & 0xff;
            diff += Math.abs(r1 - r2);
            diff += Math.abs(g1 - g2);
            diff += Math.abs(b1 - b2);
        }
        double n = width * height * 3;
        double p = diff / n / 255.0;
        return p * 100.0;
    }

    /**
     * Converts image to bitmap.
     *
     * @param image image to convert
     * @return bitmap representing converted image
     */
    static Bitmap image2Bitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        int length = buffer.remaining();
        byte[] bitmapArray = new byte[length];
        buffer.get(bitmapArray);
        return BitmapFactory.decodeByteArray(bitmapArray, 0, length);
    }

    static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    static boolean isMotionServiceRunning(Context context) {
        return isServiceRunning(context, MotionService.class);
    }

    static int getJpegOrientation(CameraCharacteristics characteristics, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }
}
