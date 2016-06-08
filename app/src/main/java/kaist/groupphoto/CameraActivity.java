package kaist.groupphoto;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import kaist.groupphoto.composite.CompositeSelectActivity;
import kaist.groupphoto.controls.CameraSettingsPopup;
import kaist.groupphoto.listener.PhotoTakenListener;

//AppCompatActivity
public class CameraActivity extends Activity  implements CameraBridgeViewBase.CvCameraViewListener2 {


    private static final String    TAG                 = "GroupPhoto::MainActivi";
    private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);

    private File mCascadeFaceFile;
    private CascadeClassifier mJavaDetector;

    private float mRelativeFaceSize = 0.3f;
    private int mAbsoluteFaceSize = 0;

    private MyOpenCVView mOpenCvCameraView;

    private Bitmap originalImage;
    private Bitmap croppedImage;

    private ImageButton captureBtn, cameraSettingBtn, imagePreviewBtn;
    private TextView faceNumberTv;

    private int faceNumber;

    private int captureMode = Constant.MODE_FULL;

    public static String mSelectedCameraMode, mSelectedCEMode;
    private static CountDownTimer mCountdownTimer = null;

    CameraSettingsPopup settingsPopup;
    CameraModeHandler cameraModeHandler = new CameraModeHandler();

    private static final int MSG_FULL_MODE = 1;
    private static final int MSG_HIDDEN_PERSON_DETECT = 2;
    private static final int MSG_CLOSED_EYE_DETECT = 3;
    private static final int MSG_COMPOSITE_PICTURE = 4;

    private Mat mGrayscaleImage;

    private ImageView overlayView;

    private SafeFaceDetector safeFaceDetector;
    private FaceDetector detector;

    private String compositeOriginalImagePath;
    private String compositeNewImagePath;
    private Bitmap compositeOriginalImage;
    private Bitmap compositeNewImage;

    private ProgressDialog dialog;

    private Timer faceDetectTimer;
    private FaceDetectorTimerTask faceDetectorTimerTask;
    int px;


    class CameraModeHandler extends Handler {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_FULL_MODE:
                    captureMode = Constant.MODE_FULL;
                    break;
                case MSG_CLOSED_EYE_DETECT:
                    captureMode = Constant.MODE_EYE_DETECTION;
                    break;
                case MSG_COMPOSITE_PICTURE:
                    captureMode = Constant.MODE_COMPOSITE;
                    startCompositeMode();
                    break;
                default:
                    break;
            }
        }
    }


    private class FaceDetectorTimerTask extends TimerTask {

        @Override
        public void run() {
            mOpenCvCameraView.takePicture(Constant.MODE_NONE);
        }
    }

    private boolean isTimerRunning;

    private void compositeImage(int direction, float xPoint) {
        Log.i(TAG, "Px : " + px);
        xPoint /= 2;
        try {
            OutputStream os = new FileOutputStream(compositeNewImagePath+"_"+ ".jpg");
            Log.i(TAG, "Width : " + compositeNewImage.getWidth() +", " + "Height : " + compositeNewImage.getHeight());

            if ( direction == Constant.SELECT_AREA_LEFT ) {
                Bitmap bmOverlay = Bitmap.createBitmap(compositeNewImage.getWidth(), compositeNewImage.getHeight(), compositeNewImage.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(Bitmap.createBitmap(compositeNewImage, 0, 0, compositeNewImage.getWidth(), compositeNewImage.getHeight()), 0, 0, null);
                Bitmap originalImage = Bitmap.createBitmap(compositeOriginalImage,  0,0, (int)(xPoint + 40), compositeOriginalImage.getHeight());
                canvas.drawBitmap(originalImage,0,0, null);
                bmOverlay.compress(Bitmap.CompressFormat.PNG, 50, os);
                os.close();
            } else {
                Bitmap bmOverlay = Bitmap.createBitmap(compositeOriginalImage.getWidth(), compositeOriginalImage.getHeight(), compositeNewImage.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(Bitmap.createBitmap(compositeOriginalImage, 0, 0, compositeOriginalImage.getWidth(), compositeOriginalImage.getHeight()),0,0,null);
                Bitmap newImage = Bitmap.createBitmap(compositeNewImage, 0,0, (int)xPoint, compositeOriginalImage.getHeight());
                canvas.drawBitmap(newImage,0,0, null);
                bmOverlay.compress(Bitmap.CompressFormat.PNG, 50, os);
                os.close();
            }

            overlayView.setVisibility(View.GONE);
            setPreviewImage(compositeNewImagePath+"_"+ ".jpg");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void setPreviewImage(String file) {

    }

    private void setButtonViewIDs() {

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
//                faceDetectorTimerTask.cancel();
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
    }


    void showImageFile() {

/*        if (new File(imageFilePath).exists()) {

        } else {
            ImageButton preview = (ImageButton) findViewById(R.id.preview);
            preview.setVisibility(View.INVISIBLE);
        }*/

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        px  = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        faceDetectTimer = new Timer();
        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .build();
        safeFaceDetector = new SafeFaceDetector(detector);
        overlayView = (ImageView) findViewById(R.id.iv_overlay);

        settingsPopup = new CameraSettingsPopup(this);

        setButtonViewIDs();

        faceNumberTv = (TextView) findViewById(R.id.tv_face_number);

        mOpenCvCameraView = (MyOpenCVView) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);

//        mOpenCvCameraView.setResolution(resolution);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex( CameraBridgeViewBase.CAMERA_ID_BACK);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setOnMaxEyesPhotoListener(photoTakenListener);

    }

    public void sendCameraModeMsg (String mSelectedCameraMode) {

        Message msg = null;
        int mode = 0;

        if(cameraModeHandler == null) {
            Log.i(TAG, "cameraModeHandler is null");
            return;
        }

        if (mSelectedCameraMode.equals("Full") ) {
            mode = MSG_FULL_MODE;
        } else if (mSelectedCameraMode.equals("Closed Eye Detection")) {
            mode = MSG_CLOSED_EYE_DETECT;
        } else if (mSelectedCameraMode.equals("Composite Picture")) {
            mode = MSG_COMPOSITE_PICTURE;
        } else {
            Log.d(TAG, "Receive wrong mode");
        }

        msg = cameraModeHandler.obtainMessage(mode);
        cameraModeHandler.sendMessage(msg);
    }


    private void saveMaxEyesPhoto(List<GroupPhoto> photoList) {
        Collections.sort(photoList, new Comparator<GroupPhoto>() {
            @Override
            public int compare(GroupPhoto photo1, GroupPhoto photo2) {
                return ( photo1.getEyesNum() > photo2.getEyesNum() ) ? -1 : ( photo1.getEyesNum() > photo2.getEyesNum() ) ? 1:0;
            }
        });

        GroupPhoto maxEyePhoto = photoList.get(0);

        try {
            FileOutputStream fos = new FileOutputStream(maxEyePhoto.getFilePath()+".jpg");
            fos.write(maxEyePhoto.getData());
            fos.close();
            compositeOriginalImagePath = maxEyePhoto.getFilePath();
            compositeOriginalImage = BitmapFactory.decodeFile(compositeOriginalImagePath+".jpg");
            overlayView.setVisibility(View.VISIBLE);
            overlayView.setImageBitmap(compositeOriginalImage);
//            if ( captureMode == Constant.MODE_FULL )
//                startCompositeMode();
            dialog.dismiss();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startCompositeMode() {
        if ( captureMode == Constant.MODE_FULL ) {
            Intent intent = new Intent(getApplicationContext(), CompositeSelectActivity.class);
            intent.putExtra("path", compositeOriginalImagePath);
            startActivityForResult(intent, Constant.REQUEST_COMPOSITE);
        } else {
            //Call gallery
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
            intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, Constant.REQUEST_GALLERY);
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {

        Mat inputMat = cvCameraViewFrame.rgba();

        if ( captureMode == Constant.MODE_FULL ) {
            detectHidden(inputMat);
        } else if ( captureMode == Constant.MODE_EYE_DETECTION) {
//            detectFace(inputMat);
        }
        return inputMat;
    }

    private void detectHidden(Mat mat) {
        faceNumber = 0;

        Imgproc.cvtColor(mat, mGrayscaleImage, Imgproc.COLOR_RGBA2RGB);

        if (mAbsoluteFaceSize == 0) {
            int height = mGrayscaleImage.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }

        }

        MatOfRect faceRect = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mGrayscaleImage, faceRect, 1.1, 2, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),new Size());


        Rect[] facesArray = faceRect.toArray();

        for ( int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(mat, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

    }

    private void takePicture() {

        switch (captureMode) {
            case Constant.MODE_EYE_DETECTION:
            case Constant.MODE_FULL:
                mOpenCvCameraView.takePicture(captureMode, getCaptureNumByFormula());
                break;
            case Constant.MODE_COMPOSITE:
                mOpenCvCameraView.takePicture(captureMode);
                break;
        }

    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    //System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
//                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
//                        String xmlFaceDataFileName = "haarcascade_frontalface_alt.xml";
                        String xmlFaceDataFileName = "haarcascade_frontalface_alt.xml";
                        mCascadeFaceFile = new File(cascadeDir, xmlFaceDataFileName);
                        FileOutputStream os = new FileOutputStream(mCascadeFaceFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFaceFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFaceFile.getAbsolutePath());

                        cascadeDir.delete();


                        // load cascade file from application resources
                        is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
//                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        String xmlEyeDataFileName = "haarcascade_eye_tree_eyeglasses.xml";
                        mCascadeFaceFile = new File(cascadeDir, xmlEyeDataFileName);
                        os = new FileOutputStream(mCascadeFaceFile);

                        buffer = new byte[4096];
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFaceFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFaceFile.getAbsolutePath());

                        cascadeDir.delete();


                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;

            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(getBaseContext(), "resultCode : "+resultCode,Toast.LENGTH_SHORT).show();

        if(requestCode == Constant.REQUEST_GALLERY) {
            if(resultCode==Activity.RESULT_OK) {
                try {

                    Cursor c = getContentResolver().query(Uri.parse(data.getAction()), null,null,null,null);
                    c.moveToNext();
                    String path = c.getString(c.getColumnIndex(MediaStore.MediaColumns.DATA));
                    compositeOriginalImagePath = path;
                    Intent intent = new Intent(getApplicationContext(), CompositeSelectActivity.class);
                    intent.putExtra("path", path);

                    startActivityForResult(intent, Constant.REQUEST_COMPOSITE);
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        } else if ( requestCode == Constant.REQUEST_COMPOSITE ) {
            if ( data != null )
                compositeImage(data.getIntExtra("direction", 0), data.getFloatExtra("xPoint", 0));
        }
    }

    private PhotoTakenListener photoTakenListener = new PhotoTakenListener() {
        @Override
        public void detectMaxEye(List<GroupPhoto> photos) {
            int eyesNum = 0;
            Log.i(TAG, "Photo list size : " + photos.size());
            for ( GroupPhoto photo : photos) {
                SparseArray<Face> faces = getFaces(photo.getData());
                if (faces.size() > 0) {

                    for (int i = 0; i < faces.size(); i++) {
                        Face face = faces.valueAt(i);

                        if (face.getIsLeftEyeOpenProbability() >= 0.5)
                            eyesNum++;
                        if (face.getIsRightEyeOpenProbability() >= 0.5)
                            eyesNum++;
                    }
                } else
                    eyesNum = 0;

                photo.setEyesNum(eyesNum);
            }
            saveMaxEyesPhoto(photos);
        }

        @Override
        public void compositePhotoTaken(String imagePath, Bitmap bitmap) {
            dialog.dismiss();
            compositeNewImagePath = imagePath;
            compositeNewImage = bitmap;
            if ( compositeNewImage== null )
                Log.i(TAG, "compositeNewImage is NULL!!!!!!!!!!!!!");
            Log.i(TAG, "CompositePhotoTaken " + compositeNewImagePath);
            startCompositeMode();
        }

        @Override
        public void autoFocus(byte[] data) {
            Log.i(TAG, "AutoFocus");
            SparseArray<Face> facesArray = getFaces(data);
            faceNumber = facesArray.size();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    faceNumberTv.setText("Faces : " + faceNumber);
                }
            });
        }

        @Override
        public void takePhotoError() {
            dialog.dismiss();
        }
    };

    private SparseArray<Face> getFaces(byte[] data) {
        Bitmap mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        Frame frame = new Frame.Builder().setBitmap(mBitmap).build();
        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .build();
        SafeFaceDetector safeFaceDetector = new SafeFaceDetector(detector);
        SparseArray<Face> faces = safeFaceDetector.detect(frame);
        safeFaceDetector.release();
        mBitmap.recycle();
        Log.i(TAG, "Face num : " + faces.size());

        return faces;
    }

    private int getCaptureNumByFormula() {

        //초당 눈을 깜빡이는 횟수
        float x;
        //카메라의 셔터 스피드와 평균 눈깜빡이는 시간의 합
        float t;

//        return 1/(1-x*t)*faceNumber;

        return 5;

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int height, int width) {
        mGrayscaleImage = new Mat(height, width, CvType.CV_8UC4);
//        mOpenCvCameraView.setResolution();
//        if ( !isTimerRunning ) {
//            isTimerRunning = true;
//            faceDetectorTimerTask = new FaceDetectorTimerTask();
//            faceDetectTimer.schedule(faceDetectorTimerTask, 0, 3000);
//        } else {
//            faceDetectTimer = new Timer();
//            faceDetectorTimerTask = new FaceDetectorTimerTask();
//            faceDetectTimer.schedule(faceDetectorTimerTask,0, 3000);
//        }
    }

    @Override
    public void onCameraViewStopped() {
//        faceDetectTimer.cancel();
    }
}
