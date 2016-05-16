package org.kraflapps.motiondroid;

import android.Manifest;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ragnarok.rxcamera.RxCamera;
import com.ragnarok.rxcamera.config.RxCameraConfig;
import com.ragnarok.rxcamera.config.RxCameraConfigChooser;

import java.util.Arrays;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;

    private CameraManager mCameraManager;
    private CameraPreview mPreview;
    private String mCurrentCameraId;
    private CameraDevice mCameraDevice;

    private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            SurfaceView view = (SurfaceView) getView().findViewById(R.id.surface_view);
            mPreview = new CameraPreview(getContext(), mCameraDevice, view.getHolder());
            view.getHolder().addCallback(mPreview);
            Log.d(LOG_TAG, "Successfully opened camera " + camera.getId());

        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };


    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        /*// Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCameraManager);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);*/
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // obtain an instance of camera manager

        mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = mCameraManager.getCameraIdList();
            if (cameraIdList.length == 0) {
                Log.d(LOG_TAG, "Could not find any cameras");
                Toast.makeText(getContext(), "Problems accessing the camera", Toast.LENGTH_SHORT).show();
            } else {
                mCurrentCameraId = cameraIdList[0];
                checkPermissions();
            }
            Log.d(LOG_TAG, "cameraIdList: " + cameraIdList[0]);
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, "Problems accesing the camera", e);
        }

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

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        } else {
            initializePreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    initializePreview();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void initializePreview() {
            /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }*/

        try {
            mCameraManager.openCamera(mCurrentCameraId, mCameraCallback, null);
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, "Could not access camera", e);
        }




        /*RxCameraConfig config = RxCameraConfigChooser.obtain().
                useBackCamera().
                setAutoFocus(true).
                setPreferPreviewFrameRate(15, 30).
                setPreferPreviewSize(new Point(640, 480)).
                setHandleSurfaceEvent(true).
                get();

        Observable<RxCamera> rxCameraObservable = RxCamera
                .open(getContext(), config);

        rxCameraObservable.subscribe(
                new Action1<RxCamera>() {
                    @Override
                    public void call(RxCamera rxCamera) {
                        Log.d(LOG_TAG, "RxCamera called " + rxCamera.getNativeCamera());
                    }
                },
                new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error in observable", throwable);
                    }
                });


        rxCameraObservable
                .flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
                    @Override
                    public Observable<RxCamera> call(RxCamera rxCamera) {
                        return rxCamera.bindSurface(surfaceView);
                    }
                });

        rxCameraObservable.subscribe(
                new Action1<RxCamera>() {
                    @Override
                    public void call(RxCamera rxCamera) {
                        Log.d(LOG_TAG, "RxCamera called " + rxCamera.getNativeCamera());
                    }
                },
                new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error in observable", throwable);
                    }
                });


        rxCameraObservable
                .flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
                    @Override
                    public Observable<RxCamera> call(RxCamera rxCamera) {
                        return rxCamera.startPreview();
                    }
                });

        rxCameraObservable.subscribe(
                new Action1<RxCamera>() {
                    @Override
                    public void call(RxCamera rxCamera) {
                        Log.d(LOG_TAG, "RxCamera called " + rxCamera.getNativeCamera());
                    }
                },
                new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error in observable", throwable);
                    }
                });*/


    }
}
