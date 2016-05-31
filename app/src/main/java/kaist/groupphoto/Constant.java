package kaist.groupphoto;

import android.os.Environment;

/**
 * Created by kjwook on 2016. 5. 31..
 */
public class Constant {

    public static final int MODE_FULL = 0;
    public static final int MODE_GROUP = 1;
    public static final int MODE_COMPOSITE = 2;
    public static final String PHOTO_DIR = Environment.getExternalStorageDirectory().getPath() + "/DCIM/GroupPhoto/";
}
