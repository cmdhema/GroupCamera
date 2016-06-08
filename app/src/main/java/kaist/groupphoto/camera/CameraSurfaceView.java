package kaist.groupphoto.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kaist.groupphoto.Constant;
import kaist.groupphoto.GroupPhoto;
import kaist.groupphoto.listener.PhotoTakenListener;

/**
 * Created by kjwook on 2016. 6. 8..
 */
public class CameraSurfaceView extends ViewGroup implements SurfaceHolder.Callback {
    String TAG = "GroupPhoto:CameraSurfaceVi";
    private SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Context mContext;
    private boolean mStartRequested;
    private boolean mSurfaceAvailable;
    private CameraSource mCameraSource;
    private GraphicOverlay mOverlay;

    private PhotoTakenListener photoTakenListener;
    private boolean isMaxEyeDetectDone;
    private List<GroupPhoto> photoList;

    public CameraSurfaceView(Context context) {
        super(context);
        mContext = context;
        mStartRequested = false;
        mSurfaceAvailable = false;
        mSurfaceView = new SurfaceView(context);
        mSurfaceView.getHolder().addCallback(this);
        addView(mSurfaceView);
        photoList = new ArrayList<>();
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mStartRequested = false;
        mSurfaceAvailable = false;
        mSurfaceView = new SurfaceView(context);
        mSurfaceView.getHolder().addCallback(this);
        addView(mSurfaceView);
        photoList = new ArrayList<>();

    }

    public void start(CameraSource cameraSource) throws IOException {

        if (cameraSource == null) {
            stop();
        }

        mCameraSource = cameraSource;
        if (mCameraSource != null) {
            mStartRequested = true;
            startIfReady();
        }
    }

    public void start(CameraSource cameraSource, GraphicOverlay overlay) throws IOException {
        mOverlay = overlay;
        start(cameraSource);
    }

    public void stop() {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
    }

    public void release() {
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    public void takePicture() {

        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes) {
                savePhoto(bytes);
            }
        });
    }

    private void startIfReady() throws IOException {
        if (mStartRequested && mSurfaceAvailable) {
            mCameraSource.start(mSurfaceView.getHolder());
            if (mOverlay != null) {
                Log.i(TAG, "startIfReady()");
                Size size = mCameraSource.getPreviewSize();
                int min = Math.min(size.getWidth(), size.getHeight());
                int max = Math.max(size.getWidth(), size.getHeight());
                mOverlay.setCameraInfo(max, min, mCameraSource.getCameraFacing());
                mOverlay.clear();
            }
            mStartRequested = false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mSurfaceAvailable = true;
        Log.i(TAG, "Surface Createed!!!");
        try {
            startIfReady();
        } catch (IOException e) {
            Log.e(TAG, "Could not start camera source.", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mSurfaceAvailable = false;
    }

    public void takePicture(final Detector.Detections<Face> detectionResults, final int capNum) {
        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data) {
                GroupPhoto photo = new GroupPhoto();
                if ( data != null ) {
//                    Log.i(TAG, "Landmark : " + detectionResults.getDetectedItems().size() + ", " + "data : " + data.length);

                    photo.setData(data);
                    photo.setFilePath(getSaveFileName());

                    if ( detectionResults == null )
                        photo.setFaces(null);
                    else
                        photo.setFaces(detectionResults.getDetectedItems());

                }
                photoList.add(photo);

                if (photoList.size() == capNum) {
                    photoTakenListener.detectMaxEye(photoList);
                    photoList.clear();
                }
            }
        });
    }

    public void savePhoto(byte[] data) {
        FileOutputStream fos;
        try {
            String fileName = getSaveFileName();
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
    
    public void setOnMaxEyesPhotoListener(PhotoTakenListener listener ) {
        this.photoTakenListener = listener;
    }



    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        int width = 320;
//        int height = 240;
        int width = 0;
        int height = 0;
        if (mCameraSource != null) {
            Size size = mCameraSource.getPreviewSize();
            if (size != null) {
                width = size.getWidth();
                height = size.getHeight();
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees

        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        // Computes height and width for potentially doing fit width.
        int childWidth = layoutWidth;
        int childHeight = (int)(((float) layoutWidth / (float) width) * height);

        // If height is too tall using fit width, does fit height instead.
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int)(((float) layoutHeight / (float) height) * width);
        }

        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0, 0, childWidth, childHeight);
        }

        try {
            startIfReady();
        } catch (IOException e) {
            Log.e(TAG, "Could not start camera source.", e);
        }
    }


}
