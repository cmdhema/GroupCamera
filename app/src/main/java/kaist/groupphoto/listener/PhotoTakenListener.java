package kaist.groupphoto.listener;

import android.graphics.Bitmap;

import java.util.List;

import kaist.groupphoto.GroupPhoto;

/**
 * Created by kjwook on 2016. 5. 31..
 */
public interface PhotoTakenListener {

    void detectMaxEye(List<GroupPhoto> photos);
    void compositePhotoTaken(String imagePath, Bitmap bitmap);
}
