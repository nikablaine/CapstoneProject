package org.kraflapps.motiondroid;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static org.kraflapps.motiondroid.R.string.pref_folder_key;
import static org.kraflapps.motiondroid.Util.image2Bitmap;
import static org.kraflapps.motiondroid.Util.percentDifferenceLite;

public class MotionService extends IntentService {

    private static final String NAME = MotionService.class.getSimpleName();
    private static final String LOG_TAG = MotionService.class.getName();
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private ImageReader.OnImageAvailableListener mImageListener;
    private CameraCaptureSession.CaptureCallback mCaptureCallback;
    private CameraDevice.StateCallback mCameraDeviceStateCallback;
    private Surface mDummySurface;
    private Bitmap mCurrentImage;
    private CameraCaptureSession mSession;
    private SurfaceTexture mDummyPreview;

    /**
     * Creates a service with a default name.
     */
    public MotionService() {
        super(NAME);
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MotionService(String name) {
        super(name);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            mCameraManager = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
            String cameraId = getCameraId();
            if (cameraId != null)
                try {
                    Log.d(LOG_TAG, "Before open camera");
                    mCameraManager.openCamera(cameraId, mCameraDeviceStateCallback, null);
                    Log.d(LOG_TAG, "After open camera");
                } catch (CameraAccessException e) {
                    Log.e(LOG_TAG, "Camera access problem", e);
                }
        }

        return START_STICKY;

    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();

        mDummyPreview = new SurfaceTexture(1);
        mDummySurface = new Surface(mDummyPreview);
        mImageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(mImageListener, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDummyPreview.release();
        if (mSession != null) {
            try {
                mSession.stopRepeating();
            } catch (CameraAccessException e) {
                Log.e(LOG_TAG, "Error on stop repeating", e);
            }
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }

    private void init() {
        mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(final CameraDevice cameraDevice) {
                Log.d(LOG_TAG, "Camera opened: " + cameraDevice.getId());
                mCameraDevice = cameraDevice;
                try {
                    CaptureRequest.Builder captureRequestBuilder =
                            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    captureRequestBuilder.addTarget(mImageReader.getSurface());
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    final CaptureRequest captureRequest = captureRequestBuilder.build();

                    Log.d(LOG_TAG, "Capture request created");
                    cameraDevice.createCaptureSession(
                            Arrays.asList(mImageReader.getSurface(), mDummySurface),
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(CameraCaptureSession session) {
                                    try {
                                        mSession = session;
                                        Log.d(LOG_TAG, "Session configured");
                                        CaptureRequest.Builder previewRequestBuilder =
                                                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                        previewRequestBuilder.addTarget(mDummySurface);
                                        CaptureRequest previewRequest = previewRequestBuilder.build();
                                        // a small hack to make camera more sane
                                        for (int i = 0; i < 10; i++) {
                                            session.capture(previewRequest, null, null);
                                        }
                                        session.setRepeatingRequest(captureRequest, mCaptureCallback, null);
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onConfigureFailed(CameraCaptureSession session) {

                                }
                            },
                            null
                    );
                    Log.d(LOG_TAG, "Capture session created");

                } catch (CameraAccessException e) {
                    Log.e(LOG_TAG, "Capture request problem", e);
                }

            }

            @Override
            public void onDisconnected(CameraDevice camera) {

            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.e(LOG_TAG, "Camera error: " + error);
            }
        };

        mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
            }

            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
            }
        };

        mImageListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();

                // if there is no valid image available, do not do anything
                if (image == null) {
                    return;
                }

                if (mCurrentImage == null) {
                    mCurrentImage = image2Bitmap(image);

                } else {
                    Bitmap newBitmap = image2Bitmap(image);
                    double differenceLite = percentDifferenceLite(newBitmap, mCurrentImage);
                    Log.v(LOG_TAG, "Difference = " + differenceLite);
                    mCurrentImage = newBitmap;

                    if (differenceLite >= 0.02) {
                        Log.d(LOG_TAG, "Diff is significant, saving..");
                        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String dirPath = defaultSharedPreferences.getString(getResources().getString(pref_folder_key), Environment.getExternalStorageDirectory().getPath());
                        String name = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(System.currentTimeMillis())) + ".png";
                        File file = new File(dirPath, name);

                        Integer capacity = Integer.valueOf(defaultSharedPreferences.getString(getResources().getString(R.string.pref_capacity_key), "100"));
                        Integer policy = Integer.valueOf(defaultSharedPreferences.getString(getResources().getString(R.string.pref_overwrite_key), "0"));

                        new ImageSaver(mCurrentImage, file, capacity, policy).run();
                    }
                }

                image.close();
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    private String getCameraId() {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return preferences.getString(getString(R.string.camera_id_key), null);
    }


}
