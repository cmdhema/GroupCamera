package kaist.groupphoto;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import kaist.groupphoto.camera.CameraSurfaceView;
import kaist.groupphoto.camera.GraphicOverlay;
import kaist.groupphoto.composite.CompositeSelectActivity;
import kaist.groupphoto.controls.CameraSettingsPopup;
import kaist.groupphoto.listener.PhotoTakenListener;
import kaist.groupphoto.listener.SpinnerSelectListener;

//AppCompatActivity
public class CameraActivity extends Activity {

    private static final String TAG = "GroupPhoto::MainActivi";

    private CameraSurfaceView cameraPreview;

    private ImageButton captureBtn, cameraSettingBtn, imagePreviewBtn;
    private TextView faceNumberTv;

    private int faceNumber;

    private static int captureMode = Constant.MODE_FULL;

    CameraSettingsPopup settingsPopup;

    private ImageView overlayView;

    private String compositeOriginalImagePath;
    private String compositeNewImagePath;
    private Bitmap compositeOriginalImage;
    private Bitmap compositeNewImage;

    private ProgressDialog dialog;

    int px;

    private String currentImageName;

    private ArrayList<FaceData> faceDataList;
    private ArrayList<Rect> detectFaceList;

    private CameraSource mCameraSource = null;

    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private Detector.Detections<Face> detection;
    private int photoCounter = 5;
    private FaceDetector detector;


    private boolean afterGallerySelected;
    private boolean isFirstCapture = true;

    private void compositeImage(int direction, float xPoint) {
        xPoint /= 2;
        Log.i(TAG, "Direction : " + direction + ", " + "xPoint : " + xPoint);
        try {
            OutputStream os = new FileOutputStream(compositeNewImagePath + "_" + ".jpg");
            currentImageName = compositeNewImagePath + "_" + ".jpg";
            Log.i(TAG, "Width : " + compositeNewImage.getWidth() + ", " + "Height : " + compositeNewImage.getHeight());

            if (direction == Constant.SELECT_AREA_LEFT) {
                if ( compositeOriginalImage == null )
                    Log.i(TAG, "CompositeOriginalImage is null");
                Bitmap bmOverlay = Bitmap.createBitmap(compositeNewImage.getWidth(), compositeNewImage.getHeight(), compositeNewImage.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(Bitmap.createBitmap(compositeNewImage, 0, 0, compositeNewImage.getWidth(), compositeNewImage.getHeight()), 0, 0, null);
                Bitmap originalImage = Bitmap.createBitmap(compositeOriginalImage, 0, 0, (int) (xPoint), compositeOriginalImage.getHeight());
                canvas.drawBitmap(originalImage, 0, 0, null);
                bmOverlay.compress(Bitmap.CompressFormat.PNG, 50, os);
                os.close();
            } else {
                Bitmap bmOverlay = Bitmap.createBitmap(compositeOriginalImage.getWidth(), compositeOriginalImage.getHeight(), compositeNewImage.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(Bitmap.createBitmap(compositeOriginalImage, 0, 0, compositeOriginalImage.getWidth(), compositeOriginalImage.getHeight()), 0, 0, null);
                Bitmap newImage = Bitmap.createBitmap(compositeNewImage, 0, 0, (int) xPoint, compositeOriginalImage.getHeight());
                canvas.drawBitmap(newImage, 0, 0, null);
                bmOverlay.compress(Bitmap.CompressFormat.PNG, 50, os);
                os.close();
            }

            overlayView.setVisibility(View.GONE);
            setPreviewImage(compositeNewImagePath + "_" + ".jpg");
            photoCounter = 5;
            isFirstCapture = true;
            captureMode = Constant.MODE_FULL;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void setPreviewImage(String file) {
        Bitmap bitmap = BitmapFactory.decodeFile(file);
        imagePreviewBtn.setImageBitmap(bitmap);
    }

    private void setButtonViewIDs() {

        overlayView = (ImageView) findViewById(R.id.iv_overlay);
        faceNumberTv = (TextView) findViewById(R.id.tv_face_number);
        cameraPreview = (CameraSurfaceView) findViewById(R.id.view_preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        cameraSettingBtn = (ImageButton) findViewById(R.id.settings);
        cameraSettingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsPopup.show();
            }
        });

        captureBtn = (ImageButton) findViewById(R.id.btn_capture);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog = ProgressDialog.show(CameraActivity.this, "사진 촬영", "사진 촬영중입니다. 핸드폰을 고정해주세요", true);
                    }
                });
                takePicture();
            }
        });

        imagePreviewBtn = (ImageButton) findViewById(R.id.preview);
        imagePreviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageFile();
            }
        });

        settingsPopup = new CameraSettingsPopup(this);
        settingsPopup.setOnSpinnerSelectListener(new SpinnerSelectListener() {
            @Override
            public void onItemSelect(int item) {
                captureMode = item;

                if ( captureMode == Constant.MODE_COMPOSITE) {
                    detector.release();
                    startCompositeMode();
                }
            }
        });


        cameraPreview.setOnMaxEyesPhotoListener(photoTakenListener);
    }


    void showImageFile() {

        Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
        intent.putExtra("name", currentImageName);
        startActivity(intent);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());


        setButtonViewIDs();
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        settingsPopup.setOnSpinnerSelectListener(new SpinnerSelectListener() {
            @Override
            public void onItemSelect(int item) {
                captureMode = item;
                if (captureMode == Constant.MODE_COMPOSITE)
                    startCompositeMode();
            }
        });

        faceDataList = new ArrayList<>();
        detectFaceList = new ArrayList<>();
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
//        Manifest.permission.WRI
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }


    private void saveMaxEyesPhoto(List<GroupPhoto> photoList) {
        Collections.sort(photoList, new Comparator<GroupPhoto>() {
            @Override
            public int compare(GroupPhoto photo1, GroupPhoto photo2) {
                return (photo1.getEyesNum() > photo2.getEyesNum()) ? -1 : (photo1.getEyesNum() > photo2.getEyesNum()) ? 1 : 0;
            }
        });

        GroupPhoto maxEyePhoto = photoList.get(0);

        try {
            Log.i(TAG, "saveMaxEyesPhoto, Eye : " + maxEyePhoto.getEyesNum() + ", " + "Path : " + maxEyePhoto.getFilePath());
            FileOutputStream fos = new FileOutputStream(maxEyePhoto.getFilePath() + ".jpg");
            fos.write(maxEyePhoto.getData());
            fos.close();
            compositeOriginalImagePath = maxEyePhoto.getFilePath();
            compositeOriginalImage = BitmapFactory.decodeFile(compositeOriginalImagePath + ".jpg");
            if (captureMode == Constant.MODE_FULL) {
                overlayImage(compositeOriginalImage);
            } else if (captureMode == Constant.MODE_EYE_DETECTION) {
                captureMode = Constant.MODE_FULL;
                currentImageName = compositeNewImagePath + ".jpg";
                setPreviewImage(currentImageName);
            }
            dialog.dismiss();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void overlayImage(Bitmap image) {
        overlayView.setVisibility(View.VISIBLE);
        overlayView.setImageBitmap(image);
    }

    private void startCompositeMode() {

        if (afterGallerySelected ) {
            afterGallerySelected = false;
            Intent intent = new Intent(getApplicationContext(), CompositeSelectActivity.class);
            intent.putExtra("path", compositeOriginalImagePath);
            Log.i(TAG, "afterGallerySelected : " + compositeOriginalImagePath);
            startActivityForResult(intent, Constant.REQUEST_COMPOSITE);
        } else if (captureMode == Constant.MODE_FULL) {
            Intent intent = new Intent(getApplicationContext(), CompositeSelectActivity.class);
            intent.putExtra("name", compositeOriginalImagePath);
            Log.i(TAG, "name : " + compositeOriginalImagePath);
            startActivityForResult(intent, Constant.REQUEST_COMPOSITE);
        } else {
            //Call gallery
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
            intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, Constant.REQUEST_GALLERY);
        }
    }

    private void takePicture() {

        if (captureMode == Constant.MODE_FULL) {

            if ( isFirstCapture ) {
                if (--photoCounter > 0) {
                    try {
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                cameraPreview.takePicture(detection, 4);
                                takePicture();
                            }
                        }, 1000);

                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        return;
                    }
                } else
                    photoCounter = 5;
            } else
                cameraPreview.takePicture();
        } else if ( captureMode == Constant.MODE_EYE_DETECTION ) {
            if (--photoCounter > 0) {
                try {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            cameraPreview.takePicture(detection, 4);
                            takePicture();
                        }
                    }, 1000);

                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return;
                }
            } else
                photoCounter = 5;
        } else if ( captureMode == Constant.MODE_COMPOSITE ) {
            cameraPreview.takePicture();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "resultcode : " + resultCode);
        Toast.makeText(getBaseContext(), "resultCode : " + resultCode, Toast.LENGTH_SHORT).show();

        if (requestCode == Constant.REQUEST_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    Log.i(TAG, "gallery result");
                    compositeOriginalImagePath = getImageNameToUri(data.getData());
                    compositeOriginalImage = BitmapFactory.decodeFile(compositeOriginalImagePath);
                    overlayImage(compositeOriginalImage);
//                    cameraPreview.start(mCameraSource, mGraphicOverlay);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else if (requestCode == Constant.REQUEST_COMPOSITE) {
            if (data != null)
                compositeImage(data.getIntExtra("direction", 0), data.getFloatExtra("xPoint", 0));
        }
    }

    public String getImageNameToUri(Uri data) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(data, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();

        String imgPath = cursor.getString(column_index);
        return imgPath;
    }

    private PhotoTakenListener photoTakenListener = new PhotoTakenListener() {
        @Override
        public void detectMaxEye(List<GroupPhoto> photos) {
            isFirstCapture = false;
            File file = new File(Constant.PHOTO_DIR + System.currentTimeMillis() + ".txt");
            FileWriter fw = null;
            for (GroupPhoto photo : photos) {
                int eyesNum = 0;
                SparseArray<Face> faces = photo.getFaces();
                Log.i(TAG, "detectMaXEYE : " + photo.getFilePath());
                if ( faces != null ) {
                    if (faces.size() > 0) {

                        for (int i = 0; i < faces.size(); i++) {
                            Face face = faces.valueAt(i);
                            Log.i(TAG, "Left eye : " + face.getIsLeftEyeOpenProbability() + ", " + "Right eye : " + face.getIsRightEyeOpenProbability());
                            if (face.getIsLeftEyeOpenProbability() >= 0.3) {
                                Log.i(TAG, "Left Eye open!");
                                eyesNum++;
                            } else
                                Log.i(TAG, "Left Eye close!");
                            if (face.getIsRightEyeOpenProbability() >= 0.3) {
                                Log.i(TAG, "Right Eye open!");
                                eyesNum++;
                            } else {
                                Log.i(TAG, "Right Eye close!");
                            }

                            Log.i(TAG, "Open Eye number : " + eyesNum + "\n");
                        }
                    } else {
                        Log.i(TAG, "All eye close");
                    }
                }
                try {
                    fw = new FileWriter(file, true);
                    fw.write("Eye number : " + eyesNum + "\n");
                    fw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "All Eye number : " + eyesNum + "\n");
                photo.setEyesNum(eyesNum);
            }

            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            saveMaxEyesPhoto(photos);
        }

        @Override
        public void compositePhotoTaken(String imagePath, Bitmap bitmap) {
            dialog.dismiss();
            compositeNewImagePath = imagePath;
            compositeNewImage = bitmap;
            if (compositeNewImage == null)
                Log.i(TAG, "compositeNewImage is NULL!!!!!!!!!!!!!");
            Log.i(TAG, "CompositePhotoTaken " + compositeNewImagePath);

            if ( captureMode == Constant.MODE_COMPOSITE)
                afterGallerySelected = true;
            startCompositeMode();
        }

        @Override
        public void takePhotoError() {
            dialog.dismiss();
        }
    };

    private int getCaptureNumByFormula() {

        //초당 눈을 깜빡이는 횟수
        float x;
        //카메라의 셔터 스피드와 평균 눈깜빡이는 시간의 합
        float t;

//        return 1/(1-x*t)*faceNumber;

        return 5;

    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        cameraPreview.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        startCameraSource();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                cameraPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(final FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);

            detection = detectionResults;
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }

}
