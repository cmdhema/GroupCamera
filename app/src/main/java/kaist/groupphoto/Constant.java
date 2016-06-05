package kaist.groupphoto;

import android.os.Environment;

/**
 * Created by kjwook on 2016. 5. 31..
 */
public class Constant {

    public static final int MODE_FULL = 10;
    public static final int MODE_EYE_DETECTION = 11;
    public static final int MODE_COMPOSITE = 12;
    public static final int MODE_NONE = 13;

    public static final String PHOTO_DIR = Environment.getExternalStorageDirectory().getPath() + "/DCIM/GroupPhoto/";

    public static final int SELECT_AREA_LEFT = 100;
    public static final int SELECT_AREA_RIGHT = 101;

    public static final int REQUEST_COMPOSITE = 200;
    public static final int REQUEST_GALLERY=201;
}
