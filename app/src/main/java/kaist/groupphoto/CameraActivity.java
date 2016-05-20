package kaist.groupphoto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
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

    private static final String    TAG                 = "AutoCam::MainActivity";
//    private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
//    public static final int        JAVA_DETECTOR       = 0;

    final int REQ_CODE_SELECT_IMAGE=100;

//    private Mat mRgba;
    private File                   mCascadeFile;
    private CascadeClassifier mJavaDetector;


    private float mRelativeFaceSize = 0.3f;
    private int mAbsoluteFaceSize = 0;

    private MyOpenCVView mOpenCvCameraView;

    private Bitmap originalImage;

    private Spinner modeSpinner;
    private Button captureBtn;
    private TextView faceNumberTv;
    private TextView eyeNumberTv;

    private int faceNumber;
    private int maxFaceNumber;

    //0 : Full mode, 1 : Group Shooting mode, 2 : Composite mode
    private int captureMode = 0;

    private Mat mGrayscaleImage;

    private CropView cropView;


    CompositeListener compositeListener = new CompositeListener() {
        @Override
        public void cropDone(List<Point> points) {
            int widthOfscreen = 0;
            int heightOfScreen = 0;

            DisplayMetrics dm = new DisplayMetrics();
            try {
                getWindowManager().getDefaultDisplay().getMetrics(dm);
            } catch (Exception ex) {
            }
            widthOfscreen = dm.widthPixels;
            heightOfScreen = dm.heightPixels;

            Bitmap croppedImage = Bitmap.createBitmap(widthOfscreen, heightOfScreen, originalImage.getConfig());

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
//            compositeImageView.setImageBitmap(croppedImage);
        }
    };

    private void setFullMode() {

    }

    private void setGroupShootingMode() {

    }

    private void setCompositeMode() {

    }

    private void cropImage(List<Point> points) {

    }

    private void overLayCroppedImage(Paint paint) {

    }

    private void compositeImage(Bitmap croppedImage, Bitmap newImage) {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        Log.i(TAG, "called onCreate");
        Camera.Size resolution = null;

        cropView = (CropView) findViewById(R.id.cropview);
        cropView.setOnCompositeListener(compositeListener);

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

                if ( captureMode == 0 ) {
//                    detectFa
                } else if (captureMode == 1 ) {

                } else if ( captureMode == 2 ) {

                }
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

                int captureNumber = 0;

                while ( captureNumber < captureNumByFormula(faceNumber) ) {
                    takePicture();
                    captureNumber++;
                }

                saveMaxEyesPhoto();
            }
        });

        mOpenCvCameraView = (MyOpenCVView) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);

//        mOpenCvCameraView.setResolution(resolution);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex( CameraBridgeViewBase.CAMERA_ID_BACK);
        mOpenCvCameraView.enableFpsMeter();
    }

    private void saveMaxEyesPhoto() {
        ArrayList<GroupPhoto> photoList = mOpenCvCameraView.photoList;
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
//        mRgba = new Mat();
        mGrayscaleImage = new Mat(height, width, CvType.CV_8UC4);

    }

    @Override
    public void onCameraViewStopped() {
//        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        faceNumber = 0;
        Mat inputMat = cvCameraViewFrame.rgba();
        Imgproc.cvtColor(inputMat, mGrayscaleImage, Imgproc.COLOR_RGBA2RGB);


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
        faceNumber = facesArray.length;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faceNumberTv.setText("Faces : " + faceNumber);
                if ( maxFaceNumber < faceNumber )
                    maxFaceNumber = faceNumber;
            }
        });
        return inputMat;
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
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        String xmlDataFileName = "haarcascade_frontalface_alt.xml";
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
                    cropView.setOriginalImage(originalImage);

                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private int captureNumByFormula(int faceNum) {

        //초당 눈을 깜빡이는 횟수
        float x;
        //카메라의 셔터 스피드와 평균 눈깜빡이는 시간의 합
        float t;

//        return 1/(1-x*t)*faceNum;

        return 5;

    }

}
