package org.kraflapps.motiondroid;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A placeholder fragment containing a simple view.
 */
public class CameraFragment extends Fragment {

    private static final String LOG_TAG = CameraFragment.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 2;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 3;
    public static final String FRAGMENT_TAG = "CAMERA_FRAGMENT";

    private CameraManager mCameraManager;
    private String mCurrentCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private TextureView mTextureView;
    private boolean mClosed;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            mTextureView = (TextureView) mRootView.findViewById(R.id.cameraView);
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

            Log.d(LOG_TAG, "Successfully opened camera " + camera.getId());

            // save the opened camera id to the shared preferences in order to use it in the motion service
            SharedPreferences prefs = getContext().getSharedPreferences(getString(R.string.prefs_name), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.camera_id_key), camera.getId());
            editor.apply();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            Log.e(LOG_TAG, "Camera error: " + error);
        }
    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            try {
                if (mCurrentCameraId != null) {
                    setPreviewSize(
                            mCameraManager.getCameraCharacteristics(mCurrentCameraId),
                            width,
                            height,
                            getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
                    startPreview(surfaceTexture);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width, height);

            /*Log.d(LOG_TAG, "SurfaceTextureSizeChanged: width = " + width + ", height = " + height);
            try {
                if (mCurrentCameraId != null) {
                    setPreviewSize(
                            mCameraManager.getCameraCharacteristics(mCurrentCameraId),
                            width,
                            height,
                            getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
                    startPreview(surfaceTexture);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }*/
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {

            Log.d(LOG_TAG, "On surface texture destroyed");
            try {
                if (mCaptureSession != null && !Util.isMotionServiceRunning(getContext())) {
                    mCaptureSession.abortCaptures();
                    mCaptureSession = null;
                }
            } catch (CameraAccessException e) {
                Log.e(LOG_TAG, "On surface texture destroyed", e);
            } catch (IllegalStateException e) {
                Log.e(LOG_TAG, "Camera session already closed", e);
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            Log.d(LOG_TAG, "CameraFragment.onSurfaceTextureUpdated");
            if (mCaptureSession == null) {
                try {
                    startPreview(surfaceTexture);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Exception on surface updated", e);
                }
            }
        }


    };
    private ImageView mImageView;
    private View mRootView;
    private Size mPreviewSize;

    public CameraFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_main, container, false);
        return mRootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        mTextureView = (TextureView) getActivity().findViewById(R.id.cameraView);
        // openCamera();
    }

    private void openCamera() {
        checkPermissions();


        mTextureView.setVisibility(View.VISIBLE);
        mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = mCameraManager.getCameraIdList();
            if (cameraIdList.length == 0) {
                Log.d(LOG_TAG, "Could not find any cameras");
                Toast.makeText(getContext(), "Problems accessing the camera", Toast.LENGTH_SHORT).show();
            } else {
                mCurrentCameraId = cameraIdList[0];
                Log.d(LOG_TAG, "cameraIdList: " + cameraIdList[0]);
            }
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, "Problems accessing the camera", e);
        }

        initializePreview(mCameraCallback);
    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "CameraFragment.onPause");
        closeCamera();
        super.onPause();

    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "CameraFragment.onResume");
        super.onResume();

        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }


    private void setPreviewSize(CameraCharacteristics cameraCharacteristics, int width, int height, boolean isPortrait) {
        Log.d(LOG_TAG, "cameraCharacteristics = [" + cameraCharacteristics + "], width = [" + width + "], height = [" + height + "]");
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mPreviewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height, isPortrait);
        Log.d(LOG_TAG, "previewSize: width = [" + mPreviewSize.getWidth() + "], height = [" + mPreviewSize.getHeight() + "]");
        if (mTextureView.isAvailable()) {
            Log.d(LOG_TAG, "Setting height and width for the textureView");
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                mTextureView.setLayoutParams(new RelativeLayout.LayoutParams(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth()));
            } else {
                mTextureView.setLayoutParams(new RelativeLayout.LayoutParams(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight()));
            }
        }
    }

    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height, boolean isPortrait) {
        Log.d(LOG_TAG, "MapSizes: " + Arrays.toString(mapSizes));
        List<Size> collectorSizes = new ArrayList<>();
        for (Size option : mapSizes) {
            if (option.getWidth() <= (isPortrait ? height : width) &&
                    option.getHeight() <= (isPortrait ? width : height)) {
                collectorSizes.add(option);
            }
        }

        if (collectorSizes.size() > 0) {
            return Collections.max(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }

        return mapSizes[0];

            /*if (width > height) {
                if (option.getWidth() > width &&
                        option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if (option.getWidth() > height &&
                        option.getHeight() > width) {
                    collectorSizes.add(option);
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return mapSizes[0];*/
    }


    private void checkPermissions() {
        Log.d(LOG_TAG, "Checking permissions for the camera");

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Permissions not granted");

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.CAPTURE_VIDEO_OUTPUT},
                    MY_PERMISSIONS_REQUEST_CAMERA);

        }

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Permissions not granted");

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_STORAGE);

        }

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Permissions not granted");

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
        } else {
            initializePreview(mCameraCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                initializePreview(mCameraCallback);
            }
        }
    }

    private void initializePreview(CameraDevice.StateCallback callback) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            TextureView cameraView = (TextureView) getActivity().findViewById(R.id.cameraView);
            cameraView.setVisibility(View.INVISIBLE);
            TextView cameraOffTextView = (TextView) getActivity().findViewById(R.id.textView);
            cameraOffTextView.setVisibility(View.VISIBLE);
            cameraOffTextView.setText(R.string.camera_permissions_not_granted);
        } else {
            if (mCameraManager != null && mCurrentCameraId != null) {
                try {
                    if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                    }
                    mCameraManager.openCamera(mCurrentCameraId, callback, null);
                } catch (CameraAccessException e) {
                    Log.e(LOG_TAG, "Could not access camera", e);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
            }
        }
    }

    public void closeCamera() {
        try {
            Log.d(LOG_TAG, "Closing camera. Acquiring the lock..");
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            Log.d(LOG_TAG, "Closing camera. Released the lock");
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    public void startPreview(SurfaceTexture surfaceTexture) {

        try {
            if (mCameraDevice != null) {

                final CaptureRequest.Builder mPreviewRequestBuilder
                        = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                Surface surface = new Surface(surfaceTexture);
                mPreviewRequestBuilder.addTarget(surface);

                    /*if (mCaptureSession != null) {
                        // mCaptureSession.abortCaptures();
                        mCaptureSession.close();
                    }*/

                // Here, we create a CameraCaptureSession for camera preview.
                mCameraDevice.createCaptureSession(Arrays.asList(surface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    return;
                                }

                                // When the session is ready, we start displaying the preview.
                                configureCaptureSession(cameraCaptureSession);
                            }

                            private void configureCaptureSession(@NonNull CameraCaptureSession cameraCaptureSession) {
                                mCaptureSession = cameraCaptureSession;
                                try {
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    mPreviewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                                            Util.getJpegOrientation(
                                                    mCameraManager.getCameraCharacteristics(mCurrentCameraId),
                                                    getResources().getConfiguration().orientation));
                                    CaptureRequest captureRequest = mPreviewRequestBuilder.build();
                                    mCaptureSession.setRepeatingRequest(captureRequest,
                                            new CameraCaptureSession.CaptureCallback() {
                                                @Override
                                                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                                                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                                                }
                                            },
                                            null
                                    );
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(
                                    @NonNull CameraCaptureSession cameraCaptureSession) {
                                Log.e(LOG_TAG, "Failed");
                            }

                            @Override
                            public void onClosed(CameraCaptureSession session) {
                                super.onClosed(session);
                                Log.d(LOG_TAG, "Capture session closed");
                            }
                        }, null
                );
            }
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, "Error accessing the camera", e);
        }
    }
}
