package kaist.groupphoto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.hardware.Camera;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import kaist.groupphoto.composite.CompositeListener;
import kaist.groupphoto.composite.CropView;
import kaist.groupphoto.composite.Point;

//AppCompatActivity
public class CameraActivity extends Activity  implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String    TAG                 = "GroupPhoto::MainActivi";
    private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
//    public static final int        JAVA_DETECTOR       = 0;

    final int REQ_CODE_SELECT_IMAGE=100;

    private File                   mCascadeFile;
    private CascadeClassifier mJavaDetector;

    private RelativeLayout liveViewLayout;

    private float mRelativeFaceSize = 0.3f;
    private int mAbsoluteFaceSize = 0;

    private MyOpenCVView mOpenCvCameraView;

    private Bitmap originalImage;
    private Bitmap croppedImage;

    private Spinner modeSpinner;
    private Button captureBtn;
    private TextView faceNumberTv;

    private int faceNumber;
    private int maxFaceNumber;

    private int captureMode = 0;

    private Mat mGrayscaleImage;

    private CropView cropView;
    private ImageView overlayView;

    private SafeFaceDetector safeFaceDetector;
    private FaceDetector detector;

    CompositeListener compositeListener = new CompositeListener() {
        @Override
        public void cropDone(List<Point> points) {
            overLayCroppedImage(points);
        }
    };

    private void setFullMode() {
        captureMode = Constant.MODE_FULL;
    }

    private void setGroupShootingMode() {
        captureMode = Constant.MODE_GROUP;
    }

    private void setCompositeMode() {
        captureMode = Constant.MODE_COMPOSITE;
        startCompositeMode();
    }

    private void overLayCroppedImage(List<Point> points) {

        int widthOfscreen = 0;
        int heightOfScreen = 0;

        DisplayMetrics dm = new DisplayMetrics();
        try {
            getWindowManager().getDefaultDisplay().getMetrics(dm);
        } catch (Exception ex) {
        }
        widthOfscreen = dm.widthPixels;
        heightOfScreen = dm.heightPixels;

        croppedImage = Bitmap.createBitmap(widthOfscreen, heightOfScreen, originalImage.getConfig());

        Canvas canvas = new Canvas(croppedImage);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Path path = new Path();
        for (int i = 0; i < points.size(); i++) {
            path.lineTo(points.get(i).x, points.get(i).y);
        }
        canvas.drawPath(path, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(originalImage, 0, 0, paint);
        overlayView.setImageBitmap(croppedImage);
        cropView.setVisibility(View.GONE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        Camera.Size resolution = null;
        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .build();
        safeFaceDetector = new SafeFaceDetector(detector);
        overlayView = (ImageView) findViewById(R.id.iv_overlay);

        cropView = (CropView) findViewById(R.id.cropview);
        cropView.setOnCompositeListener(compositeListener);

        liveViewLayout = (RelativeLayout) findViewById(R.id.view_container);

        ArrayList spinnerItem = new ArrayList<String>();
        spinnerItem.add("Full");
        spinnerItem.add("Group");
        spinnerItem.add("Composite");
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, spinnerItem);
        modeSpinner = (Spinner) findViewById(R.id.spinner_mode);
        modeSpinner.setAdapter(adapter);
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                if ( i == 0 ) {
                    setFullMode();
                } else if (i == 1 ) {
                    setGroupShootingMode();
                } else if ( i == 2 ) {
                    setCompositeMode();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        faceNumberTv = (TextView) findViewById(R.id.tv_face_number);
        captureBtn = (Button) findViewById(R.id.btn_capture);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if ( captureMode != Constant.MODE_COMPOSITE ) {
                        takePicture();
//                    saveMaxEyesPhoto();
                } else
                    takePicture();

            }
        });

        mOpenCvCameraView = (MyOpenCVView) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);

//        mOpenCvCameraView.setResolution(resolution);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex( CameraBridgeViewBase.CAMERA_ID_BACK);
        mOpenCvCameraView.enableFpsMeter();

        mOpenCvCameraView.setOnMaxEyesPhotoListener(maxEyesPhotoTakenListener);
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
            FileOutputStream fos = new FileOutputStream(maxEyePhoto.getFileName());
            fos.write(maxEyePhoto.getData());
            fos.close();

            if ( captureMode == Constant.MODE_FULL )
                startCompositeMode();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startCompositeMode() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_CODE_SELECT_IMAGE);
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

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {

        Mat inputMat = cvCameraViewFrame.rgba();

        if ( captureMode == Constant.MODE_FULL ) {
            detectHidden(inputMat);
        } else if ( captureMode == Constant.MODE_GROUP ) {
            detectFace(inputMat);
        }


        return inputMat;
    }

    private void detectFace(Mat mat) {
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
        faceNumber = facesArray.length;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faceNumberTv.setText("Faces : " + faceNumber);
                if ( maxFaceNumber < faceNumber )
                    maxFaceNumber = faceNumber;
            }
        });
    }

    private void detectHidden(Mat mat) {

    }

    private void takePicture() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_S");
                String currentDateandTime = sdf.format(new Date());
                String saveDir = Constant.PHOTO_DIR;
                File dirCheck = new File(saveDir);
                if(!dirCheck.exists()) {
                    dirCheck.mkdirs();
                }
                String fileName = saveDir + "/" + currentDateandTime;
                mOpenCvCameraView.takePicture(fileName, captureMode, croppedImage);
//                Toast.makeText(getApplicationContext(), fileName + " saved", Toast.LENGTH_SHORT).show();
            }
        });
        t.start();

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
                        InputStream is = getResources().openRawResource(R.raw.cascade_hidden);
//                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
//                        String xmlDataFileName = "haarcascade_frontalface_alt.xml";
                        String xmlDataFileName = "cascade_hidden.xml";
                        mCascadeFile = new File(cascadeDir, xmlDataFileName);
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

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

        Toast.makeText(getBaseContext(), "resultCode : "+resultCode,Toast.LENGTH_SHORT).show();

        if(requestCode == REQ_CODE_SELECT_IMAGE)
        {
            if(resultCode==Activity.RESULT_OK)
            {
                try {

                    originalImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                    cropView.setVisibility(View.VISIBLE);
                    cropView.setOriginalImage(originalImage);

                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private MaxEyesPhotoTakenListener maxEyesPhotoTakenListener = new MaxEyesPhotoTakenListener() {
        @Override
        public void takesPhotoDone(List<GroupPhoto> photos) {
            int eyesNum = 0;
            Log.i(TAG, "Photo list size : " + photos.size());
            for ( GroupPhoto photo : photos) {
                Bitmap mBitmap = BitmapFactory.decodeByteArray(photo.getData(), 0, photo.getData().length);

                Frame frame = new Frame.Builder().setBitmap(mBitmap).build();
                detector = new FaceDetector.Builder(getApplicationContext())
                        .setTrackingEnabled(false)
                        .build();
                SafeFaceDetector safeFaceDetector = new SafeFaceDetector(detector);
                SparseArray<Face> faces = safeFaceDetector.detect(frame);
                safeFaceDetector.release();
                mBitmap.recycle();
                Log.i(TAG, "Face num : " + faces.size());
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
    };

    private int captureNumByFormula(int faceNum) {

        //초당 눈을 깜빡이는 횟수
        float x;
        //카메라의 셔터 스피드와 평균 눈깜빡이는 시간의 합
        float t;

//        return 1/(1-x*t)*faceNum;

        return 5;

    }


}
