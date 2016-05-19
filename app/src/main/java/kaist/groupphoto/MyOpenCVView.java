package kaist.groupphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.opencv.android.JavaCameraView;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import kaist.groupphoto.GroupPhoto;

/**
 * Created by meast on 2016/3/29.
 */
public class MyOpenCVView extends JavaCameraView implements Camera.PictureCallback {

    private static final String TAG = "AutoCam::MyOpenCVView";
    private String mPictureFileName;

    protected ArrayList photoList;

    private Context context;

    private int eyesNum;
    private Bitmap mBitmap;
    private SafeFaceDetector safeFaceDetector;


    public MyOpenCVView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        photoList = new ArrayList();
    }

    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .build();
        safeFaceDetector = new SafeFaceDetector(detector);
        mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Frame frame = new Frame.Builder().setBitmap(mBitmap).build();
        SparseArray<Face> faces = safeFaceDetector.detect(frame);
        safeFaceDetector.release();
        mBitmap.recycle();
        Log.i("MainActivity", "Face num : " + faces.size());
        if (faces.size() > 0) {

            for (int i = 0; i < faces.size(); i++) {
                Face face = faces.valueAt(i);

                if ( face.getIsLeftEyeOpenProbability() >= 0.5 )
                    eyesNum++;
                if ( face.getIsRightEyeOpenProbability() >= 0.5 )
                    eyesNum++;
            }
        }

        GroupPhoto photo = new GroupPhoto();
        photo.setData(data);
        photo.setEyesNum(eyesNum);
        photo.setFileName(mPictureFileName);

        photoList.add(photo);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }

    }
}
