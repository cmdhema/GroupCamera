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
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import kaist.groupphoto.composite.CompositeSelectActivity;
import kaist.groupphoto.controls.CameraSettingsPopup;
import kaist.groupphoto.listener.PhotoTakenListener;
import kaist.groupphoto.listener.SpinnerSelectListener;

//AppCompatActivity
public class CameraActivity extends Activity  implements CameraBridgeViewBase.CvCameraViewListener2 {


    private static final String    TAG                 = "GroupPhoto::MainActivi";
    private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);

    private CascadeClassifier mJavaFaceDetector;
    private CascadeClassifier mJavaEyeDetector;

    private MyOpenCVView mOpenCvCameraView;

    private ImageButton captureBtn, cameraSettingBtn, imagePreviewBtn;
    private TextView faceNumberTv;

    private int faceNumber;

    private static int captureMode = Constant.MODE_FULL;

    CameraSettingsPopup settingsPopup;

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

    private String currentImageName;

    private ArrayList<FaceData> faceDataList;
    private ArrayList<Rect> detectFaceList;

    private class FaceDetectorTimerTask extends TimerTask {

        @Override
        public void run() {
            mOpenCvCameraView.takePicture(Constant.MODE_NONE);
        }
    }

    private void compositeImage(int direction, float xPoint) {
        Log.i(TAG, "Px : " + px);
        xPoint /= 2;
        try {
            OutputStream os = new FileOutputStream(compositeNewImagePath+"_"+ ".jpg");
            currentImageName = compositeNewImagePath+"_"+ ".jpg";
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
        Bitmap bitmap = BitmapFactory.decodeFile(file);
        imagePreviewBtn.setImageBitmap(bitmap);
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

        Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
        intent.putExtra("name", currentImageName);
        startActivity(intent);

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

        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex( CameraBridgeViewBase.CAMERA_ID_BACK);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setOnMaxEyesPhotoListener(photoTakenListener);

        settingsPopup.setOnSpinnerSelectListener(new SpinnerSelectListener() {
            @Override
            public void onItemSelect(int item) {
                captureMode = item;
                if ( captureMode == Constant.MODE_COMPOSITE)
                    startCompositeMode();
            }
        });

        faceDataList = new ArrayList<>();
        detectFaceList = new ArrayList<>();
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
            if ( captureMode == Constant.MODE_FULL ) {
                overlayImage(compositeOriginalImage);
            } else if ( captureMode == Constant.MODE_EYE_DETECTION) {
                currentImageName = compositeNewImagePath+".jpg";
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

        if ( afterGallerySelected == true ) {
            afterGallerySelected = false;
            Intent intent = new Intent(getApplicationContext(), CompositeSelectActivity.class);
            intent.putExtra("path", compositeOriginalImagePath);
            Log.i(TAG, "path : " + compositeOriginalImagePath);
            startActivityForResult(intent, Constant.REQUEST_COMPOSITE);
        } else if ( captureMode == Constant.MODE_FULL) {
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

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {

        Mat inputMat = cvCameraViewFrame.rgba();

        if ( captureMode == Constant.MODE_FULL ) {
            detectHidden(inputMat);
        } else if ( captureMode == Constant.MODE_EYE_DETECTION) {
        } else if ( captureMode == Constant.MODE_COMPOSITE) {
        }
        return inputMat;
    }

    private void detectHidden(final Mat mat) {
        faceNumber = 0;

        Imgproc.cvtColor(mat, mGrayscaleImage, Imgproc.COLOR_RGBA2RGB);

        MatOfRect faceRect = new MatOfRect();
        MatOfRect eyeRect = new MatOfRect();

        double scaleFactor = 5;
        double minEyeSize = 3.6;
        double maxEyeSize = 50;
        double minFaceSize = 30;
        double maxFaceSize = 500;
        int neighborhood = 4;

        if (mJavaFaceDetector != null)
            mJavaFaceDetector.detectMultiScale(mGrayscaleImage, faceRect, scaleFactor, neighborhood, 0, new Size(minFaceSize, minFaceSize),new Size(maxFaceSize, maxFaceSize));

        if ( mJavaEyeDetector != null )
            mJavaEyeDetector.detectMultiScale(mGrayscaleImage, eyeRect, scaleFactor, neighborhood, 0, new Size(minEyeSize, minEyeSize),new Size(maxEyeSize, maxEyeSize));


        final Rect[] facesArray = faceRect.toArray();
        final Rect[] eyesArray = eyeRect.toArray();
        for ( int i = 0; i < facesArray.length; i++)
            detectFaceList.add(facesArray[i]);

        new Thread(new Runnable() {
            @Override
            public void run() {
                drawHidden(mat, eyesArray);
            }
        }).start();
    }

    private void drawHidden(Mat mat,Rect[] eyesArray) {
        faceDataList.clear();
        for ( int i = 0; i < eyesArray.length; i++) {
            FaceData data = new FaceData();
            for ( int j = 0; j < detectFaceList.size(); j++ ) {
                if ( j == 100) {
                    detectFaceList.clear();
                    continue;
                }
                if ( (detectFaceList.get(j).contains(eyesArray[i].tl()) && detectFaceList.get(j).contains(eyesArray[i].br()))) {
                    data.eye = eyesArray[i];
                }
            }
            if ( data.eye == null ) {
                data.pointBR = eyesArray[i].br();
                data.pointTL = eyesArray[i].tl();
                faceDataList.add(data);
            }
        }

        for ( FaceData data : faceDataList) {
            if ( data.eye == null ) {
                Imgproc.rectangle(mat, data.pointTL, data.pointBR,FACE_RECT_COLOR, 1);
            }
        }
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
                        File mCascadeFaceFile = new File(cascadeDir, xmlFaceDataFileName);
                        FileOutputStream os = new FileOutputStream(mCascadeFaceFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaFaceDetector = new CascadeClassifier(mCascadeFaceFile.getAbsolutePath());
                        if (mJavaFaceDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaFaceDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFaceFile.getAbsolutePath());

                        cascadeDir.delete();


                        // load cascade file from application resources
                        is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
//                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        String xmlEyeDataFileName = "haarcascade_eye_tree_eyeglasses.xml";
                        File mCascadeEyeFile = new File(cascadeDir, xmlEyeDataFileName);
                        os = new FileOutputStream(mCascadeEyeFile);

                        buffer = new byte[4096];
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaEyeDetector = new CascadeClassifier(mCascadeEyeFile.getAbsolutePath());
                        if (mJavaEyeDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaEyeDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeEyeFile.getAbsolutePath());

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
        Log.i(TAG, "resultcode : " + resultCode);
        Toast.makeText(getBaseContext(), "resultCode : "+resultCode,Toast.LENGTH_SHORT).show();

        if (requestCode==Constant.REQUEST_GALLERY ) {
            if ( resultCode == Activity.RESULT_OK ) {
                try {
                    Log.i(TAG, "gallery result");
                    compositeOriginalImagePath = getImageNameToUri(data.getData());
                    Bitmap bitmap = BitmapFactory.decodeFile(compositeOriginalImagePath);
                    overlayImage(bitmap);
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

    public String getImageNameToUri(Uri data)
    {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(data, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();

        String imgPath = cursor.getString(column_index);
        return imgPath;
    }

    private boolean afterGallerySelected;
    private PhotoTakenListener photoTakenListener = new PhotoTakenListener() {
        @Override
        public void detectMaxEye(List<GroupPhoto> photos) {
            File file = new File(Constant.PHOTO_DIR+System.currentTimeMillis()+".txt");
            FileWriter fw = null;
            int eyesNum = 0;
            for ( GroupPhoto photo : photos) {
                SparseArray<Face> faces = getFaces(photo.getData());
                if (faces.size() > 0) {

                    for (int i = 0; i < faces.size(); i++) {
                        Face face = faces.valueAt(i);
                        Log.i(TAG, "Left eye : " + face.getIsLeftEyeOpenProbability() +", " + "Right eye : " + face.getIsRightEyeOpenProbability());
                        if (face.getIsLeftEyeOpenProbability() >= 0.2) {
                            Log.i(TAG, "Left Eye open!");
                            eyesNum++;
                        } else
                            Log.i(TAG, "Left Eye close!");
                        if (face.getIsRightEyeOpenProbability() >= 0.2) {
                            Log.i(TAG, "Right Eye open!");
                            eyesNum++;
                        } else {
                            Log.i(TAG, "Right Eye close!");
                        }

                        Log.i(TAG, "Open Eye number : " + eyesNum +"\n");
                    }
                } else {
                    eyesNum = 0;
                    Log.i(TAG, "All eye close");
                }
                try {
                    fw = new FileWriter(file, true);
                    fw.write("Eye number : " + eyesNum +"\n");
                    fw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "All Eye number : " + eyesNum +"\n");
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
            if ( compositeNewImage== null )
                Log.i(TAG, "compositeNewImage is NULL!!!!!!!!!!!!!");
            Log.i(TAG, "CompositePhotoTaken " + compositeNewImagePath);
            afterGallerySelected = true;
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

    }

    @Override
    public void onCameraViewStopped() {
    }
}
