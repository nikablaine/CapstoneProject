package org.kraflapps.motiondroid;


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String LOG_TAG = CameraPreview.class.getSimpleName();

    private CameraCharacteristics mCameraCharacteristics;
    private SurfaceHolder mHolder;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    public CameraPreview(Context context,
                         CameraDevice camera,
                         CameraCharacteristics cameraCharacteristics,
                         SurfaceHolder holder) {
        super(context);
        mCameraDevice = camera;
        mCameraCharacteristics = cameraCharacteristics;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = holder;
        mHolder.setFixedSize(100, 100);
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            Surface surface = holder.getSurface();
            // We set up a CaptureRequest.Builder with the output Surface.
            final CaptureRequest.Builder mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // try to get information about camera supported preview sizes
            StreamConfigurationMap streamConfigurationMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
            Size size = getPreferredPreviewSize(sizes, holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
            Log.d(LOG_TAG, "size = " + size);



            Log.d(LOG_TAG, "Supported sizes: " + sizes.toString());

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            mHolder.setFixedSize(300, 400);

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            /*try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                               mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                                       CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                CaptureRequest captureRequest = mPreviewRequestBuilder.build();
                                cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        new CameraCaptureSession.CaptureCallback() {
                                            @Override
                                            public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                                                super.onCaptureStarted(session, request, timestamp, frameNumber);
                                            }
                                        },
                                        null
                                );
                            } catch (CameraAccessException e) {
                                Log.e(LOG_TAG, "Camera exception", e);
                            }*/
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

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCaptureSession.abortCaptures();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            Surface surface = holder.getSurface();
            // We set up a CaptureRequest.Builder with the output Surface.
            final CaptureRequest.Builder mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
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

    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for(Size option : mapSizes) {
            if(width > height) {
                if(option.getWidth() > width &&
                        option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if(option.getWidth() > height &&
                        option.getHeight() > width) {
                    collectorSizes.add(option);
                }
            }
        }
        if(collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return mapSizes[0];
    }

}
