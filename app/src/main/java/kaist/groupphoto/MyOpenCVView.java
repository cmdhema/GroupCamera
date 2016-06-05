package kaist.groupphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kaist.groupphoto.listener.PhotoTakenListener;

/**
 * Created by meast on 2016/3/29.
 */
public class MyOpenCVView extends JavaCameraView implements Camera.PictureCallback {

    private static final String TAG = "GroupPhoto:OpenCVView";

    protected ArrayList photoList;

    private int captureMode;

    private boolean isPreviewRunning = false;

    private PhotoTakenListener photoTakenListener;

    int photoCounter = 5;
    private boolean isMaxEyeDetectDone;

    public MyOpenCVView(Context context, AttributeSet attrs) {
        super(context, attrs);
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

    public void takePicture(int mode) {
        Log.i(TAG, "Taking picture");

        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);
        captureMode = mode;

        if ( !isPreviewRunning ) {
            mCamera.takePicture(null, null, this);
            isPreviewRunning = false;
        } else
            return;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "onPictureTaken " + photoCounter +"times");
        // The camera preview was automatically stopped. Start it again.
        mCamera.setPreviewCallback(this);
//        mCamera.stopPreview();
        mCamera.startPreview();
        if ( captureMode != Constant.MODE_COMPOSITE ) {

            if ( isMaxEyeDetectDone ) {
                photoCounter = 5;
                photoList.clear();
                savePhoto(data);
            } else {
                GroupPhoto photo = new GroupPhoto();
                photo.setData(data);
                photo.setFilePath(getSaveFileName());
                photoList.add(photo);

                if ( photoList.size() == 5 ) {
                    isMaxEyeDetectDone = true;
                    photoTakenListener.detectMaxEye(photoList);
                }

                if ( --photoCounter > 0 )
                    mCamera.takePicture(null, null, this);
                else
                    photoCounter = 5;
            }

        } else {
            savePhoto(data);
        }
    }


    public void savePhoto(byte[] data) {
        FileOutputStream fos;
        try {
            String fileName = getSaveFileName() +".jpg";
            fos = new FileOutputStream(fileName);

            fos.write(data);
            fos.close();
            Log.i(TAG, "Save Photo : " + fileName+".jpg");
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            photoTakenListener.compositePhotoTaken(fileName, bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setOnMaxEyesPhotoListener(PhotoTakenListener listener ) {
        this.photoTakenListener = listener;
    }

    private String getSaveFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_S");
        String currentDateandTime = sdf.format(new Date());
        String saveDir = Constant.PHOTO_DIR;
        File dirCheck = new File(saveDir);
        if(!dirCheck.exists()) {
            dirCheck.mkdirs();
        }
        return saveDir + currentDateandTime;

    }
}
