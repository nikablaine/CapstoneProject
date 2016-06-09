package org.kraflapps.motiondroid;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class CameraFragment extends Fragment {

    private static final String LOG_TAG = CameraFragment.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 2;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 3;

    private CameraManager mCameraManager;
    private String mCurrentCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private TextureView mTextureView;
    private boolean mClosed;

    private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mClosed = true;
            mCameraDevice = camera;
            mTextureView = (TextureView) getView().findViewById(R.id.cameraView);
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

        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
            mClosed = true;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
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
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(LOG_TAG, "On surface texture destroyed");
            try {
                if (mCameraDevice != null) {
                    if (!mClosed) {
                        mCaptureSession.abortCaptures();
                    }
                }
            } catch (CameraAccessException e) {
                Log.e(LOG_TAG, "On surface texture destroyed", e);
            }
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

        private void startPreview(SurfaceTexture surfaceTexture) {
            try {
                final CaptureRequest.Builder mPreviewRequestBuilder
                        = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                Surface surface = new Surface(surfaceTexture);
                mPreviewRequestBuilder.addTarget(surface);

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
                                mCaptureSession = cameraCaptureSession;
                                try {
                                    // Auto focus should be continuous for camera preview.
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    // Flash is automatically enabled when necessary.
                                    //setAutoFlash(mPreviewRequestBuilder);

                                    // Finally, we start displaying the camera preview.
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
                        }, null
                );
            } catch (CameraAccessException e) {
                Log.e(LOG_TAG, "Error accessing the camera", e);
            }
        }
    };

    public CameraFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        checkPermissions();

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
            Log.e(LOG_TAG, "Problems accesing the camera", e);
        }

        mTextureView = (TextureView) getActivity().findViewById(R.id.cameraView);

        initializePreview();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mTextureView.isAvailable()) {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setPreviewSize(CameraCharacteristics cameraCharacteristics, int width, int height, boolean isPortrait) {
        Log.d(LOG_TAG, "cameraCharacteristics = [" + cameraCharacteristics + "], width = [" + width + "], height = [" + height + "]");
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size previewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height, isPortrait);
        Log.d(LOG_TAG, "previewSize: width = [" + previewSize.getWidth() + "], height = [" + previewSize.getHeight() + "]");
        if (mTextureView.isAvailable()) {
            Log.d(LOG_TAG, "Setting height and width for the textureView");
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                mTextureView.setLayoutParams(new RelativeLayout.LayoutParams(
                        previewSize.getHeight(), previewSize.getWidth()));
            } else {
                mTextureView.setLayoutParams(new RelativeLayout.LayoutParams(
                        previewSize.getWidth(), previewSize.getHeight()));
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
            initializePreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                initializePreview();
            }
        }
    }

    private void initializePreview() {
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
                    mCameraManager.openCamera(mCurrentCameraId, mCameraCallback, null);
                } catch (CameraAccessException e) {
                    Log.e(LOG_TAG, "Could not access camera", e);
                }
            }
        }
    }

    public void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }
}
