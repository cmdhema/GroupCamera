package kaist.groupphoto;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//AppCompatActivity
public class CameraActivity extends Activity  implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String    TAG                 = "AutoCam::MainActivity";
    private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;

    private MenuItem mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
    private MenuItem               mItemCameraId;
    private MenuItem               mItemExit;

    private Mat mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private File                   mHaarCascadeEyeFile;
    private CascadeClassifier mJavaDetector;

    private CascadeClassifier      mJavaEyeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;
    private float mRelativeEyeSize = 0.05f;
    private int mAbsoluteEyeSize = 0;


    private MyOpenCVView            mOpenCvCameraView;

    private Spinner modeSpinner;
    private Button captureBtn;
    private TextView faceNumberTv;
    private TextView eyeNumberTv;

    private int maxFaceNumber;
    private int maxEyeNumber;

    private double maxEyeSize;
    private double minEyeSize;
    private double maxFaceSize;
    private double minFaceSize;

    //0 : Full mode, 1 : Group Shooting mode, 2 : Composite mode
    private int captureMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        Log.i(TAG, "called onCreate");
        Camera.Size resolution = null;

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
                captureMode = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        eyeNumberTv = (TextView) findViewById(R.id.tv_eye_number);
        faceNumberTv = (TextView) findViewById(R.id.tv_face_number);
        captureBtn = (Button) findViewById(R.id.btn_capture);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });

        mOpenCvCameraView = (MyOpenCVView) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);

        //mOpenCvCameraView.setResolution(resolution);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex( CameraBridgeViewBase.CAMERA_ID_BACK);
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
    public void onCameraViewStarted(int i, int i1) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {

        mRgba = cvCameraViewFrame.rgba();
        mGray = cvCameraViewFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                mAbsoluteEyeSize = Math.round(height * mRelativeEyeSize);
            }

        }

        MatOfRect faceRect = new MatOfRect();
        MatOfRect eyeRect = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null) {
                mJavaDetector.detectMultiScale(mGray, faceRect, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

            }
            if ( mJavaEyeDetector != null) {
                mJavaEyeDetector.detectMultiScale(mGray, eyeRect, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteEyeSize, mAbsoluteEyeSize), new Size());
            }
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        final Rect[] facesArray = faceRect.toArray();
        final Rect[] eyesArray = eyeRect.toArray();
        for (int i = 0; i < eyesArray.length; i++) {
            Imgproc.rectangle(mRgba, eyesArray[i].tl(), eyesArray[i].br(), FACE_RECT_COLOR, 3);
            Log.d(TAG, "eye array " + String.valueOf(i));
        }
        //TODO
        if(facesArray.length > 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int faceNum = facesArray.length;
                    int eyeNum = eyesArray.length;
                    faceNumberTv.setText("Faces : " + faceNum);
                    if ( maxFaceNumber < faceNum )
                        maxFaceNumber = faceNum;
                    eyeNumberTv.setText("Eyes : " + eyeNum);
                    if ( maxEyeNumber < eyeNum )
                        maxEyeNumber = eyeNum;
                }
            });

        }

        return mRgba;
    }

    private void takePicture() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_S");
                String currentDateandTime = sdf.format(new Date());
                String saveDir = Environment.getExternalStorageDirectory().getPath() + "/DCIM/GroupPhoto/";
                File dirCheck = new File(saveDir);
                if(!dirCheck.exists()) {
                    dirCheck.mkdirs();
                }
                String fileName = saveDir + "/" + currentDateandTime + ".jpg";
                try {
                    mOpenCvCameraView.takePicture(fileName);
                    Toast.makeText(getApplicationContext(), fileName + " saved", Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        String xmlDataFileName = "lbpcascade_frontalface.xml";
                        mCascadeFile = new File(cascadeDir, xmlDataFileName);
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // eyes detect
                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                        InputStream is2 = getResources().openRawResource(R.raw.haarcascade_eye);
                        File haarcascadeDir = getDir("haarcascade", Context.MODE_PRIVATE);
                        String xmlDFName = "haarcascade_eye.xml";
                        mHaarCascadeEyeFile = new File(haarcascadeDir, xmlDFName);
                        FileOutputStream os2 = new FileOutputStream(mHaarCascadeEyeFile);
                        byte[] buffer2 = new byte[4096];
                        int bytesRead2;
                        while((bytesRead2 = is2.read(buffer)) != -1) {
                            os2.write(buffer, 0, bytesRead2);
                        }
                        is2.close();
                        os2.close();
                        mJavaEyeDetector = new CascadeClassifier(mHaarCascadeEyeFile.getAbsolutePath());
                        if(mJavaEyeDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaEyeDetector = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + mHaarCascadeEyeFile.getAbsolutePath());
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(CameraActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


}
