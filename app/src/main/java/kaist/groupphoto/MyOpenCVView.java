package kaist.groupphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.JavaCameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import kaist.groupphoto.listener.PhotoTakenListener;

/**
 * Created by meast on 2016/3/29.
 */
public class MyOpenCVView extends JavaCameraView  {

    private static final String TAG = "GroupPhoto:OpenCVView";

    protected ArrayList photoList;

    private int captureMode;

    private boolean isPreviewRunning = false;

    private PhotoTakenListener photoTakenListener;

    private int photoCounter = 5;
    private int photoNum = 5;

    private boolean isMaxEyeDetectDone;

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, "onPictureTaken " + photoCounter +"times");
//            mCamera.setPreviewCallback(pictureCallback);

            if ( (captureMode == Constant.MODE_NONE) && ( photoCounter == photoNum ) )
                photoTakenListener.autoFocus(data);
            else if ( captureMode == Constant.MODE_FULL || captureMode == Constant.MODE_COMPOSITE ) {

                if ( isMaxEyeDetectDone ) {
                    isMaxEyeDetectDone = false;
                    Log.i(TAG, "PhotoNum : " + photoNum);
                    photoCounter = photoNum;
                    photoList.clear();
                    savePhoto(data);
                } else {
                    GroupPhoto photo = new GroupPhoto();
                    photo.setData(data);
                    photo.setFilePath(getSaveFileName());
                    photoList.add(photo);

                    if ( photoList.size() == photoNum ) {
                        isMaxEyeDetectDone = true;
                        photoTakenListener.detectMaxEye(photoList);
                    }

                    if ( --photoCounter > 0 ) {
                        try {
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    mCamera.takePicture(null, null, pictureCallback);
                                }
                            },100);

                        } catch (RuntimeException e) {
                            e.printStackTrace();
                            showToast();
                            isMaxEyeDetectDone = false;
                            photoCounter = photoNum;
                            photoList.clear();
                            mCamera.startPreview();
                            photoTakenListener.takePhotoError();
                            return;
                        }

                    }
                    else
                        photoCounter = photoNum;
                }

            } else if ( captureMode == Constant.MODE_COMPOSITE ){
                savePhoto(data);
            }
        }
    };
    public MyOpenCVView(Context context, AttributeSet attrs) {
        super(context, attrs);
        photoList = new ArrayList();

//
    }

//    public List<String> getEffectList() {
//        return mCamera.getParameters().getSupportedColorEffects();
//    }
//
//    public boolean isEffectSupported() {
//        return (mCamera.getParameters().getColorEffect() != null);
//    }
//
//    public String getEffect() {
//        return mCamera.getParameters().getColorEffect();
//    }
//
//    public void setEffect(String effect) {
//        Camera.Parameters params = mCamera.getParameters();
//        params.setColorEffect(effect);
//        mCamera.setParameters(params);
//    }
//
//    public List<Camera.Size> getResolutionList() {
//        return mCamera.getParameters().getSupportedPreviewSizes();
//    }
//
//    public void setResolution(Camera.Size resolution) {
//        disconnectCamera();
//        mMaxHeight = resolution.height;
//        mMaxWidth = resolution.width;
//        connectCamera(getWidth(), getHeight());
//    }

    public void setResolution() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureSize(1280, 720);
        mCamera.setParameters(parameters);
    }
//
//    public Camera.Size getResolution() {
//        return mCamera.getParameters().getPreviewSize();
//    }

    public void takePicture(int mode) {
        Log.i(TAG, "Taking picture");

        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
//        mCamera.release();
//        mCamera.setPreviewCallback(null);
        captureMode = mode;

        if ( !isPreviewRunning ) {
            if ( mCamera != null ) {
                mCamera.startPreview();
                try {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mCamera.takePicture(null, null, pictureCallback);
                        }
                    },100);
                } catch (RuntimeException e) {
                    showToast();
                    photoTakenListener.takePhotoError();
                }

            }
            isPreviewRunning = false;
        } else
            return;
    }

    private void showToast() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "다시 시도해주세요", Toast.LENGTH_SHORT).show();
                    }
                }, 10);
                Looper.loop();
            }
        }).start();
    }

    public void takePicture(int mode, int pictureNum) {
        photoCounter = pictureNum;
        photoNum = pictureNum;
        captureMode = mode;

//        mCamera.setPreviewCallback(null);
        captureMode = mode;

        if ( !isPreviewRunning ) {
            if ( mCamera != null ) {
                mCamera.startPreview();
                try {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mCamera.takePicture(null, null, pictureCallback);
                        }
                    },100);
                } catch (RuntimeException e) {

                    showToast();
                    photoTakenListener.takePhotoError();
                }
            }
            isPreviewRunning = false;
        } else
            return;
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
