package kaist.groupphoto;

import android.os.Environment;

/**
 * Created by kjwook on 2016. 5. 31..
 */
public class Constant {

    public static final int MODE_FULL = 0;
    public static final int MODE_EYE_DETECTION = 1;
    public static final int MODE_COMPOSITE = 2;
    public static final int MODE_NONE = 3;

    public enum CEModeType {
        AUTO(0), MANUAL(1);
        private final int value;

        private CEModeType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    public static final String PHOTO_DIR = Environment.getExternalStorageDirectory().getPath() + "/DCIM/GroupPhoto/";

    public static final int SELECT_AREA_LEFT = 100;
    public static final int SELECT_AREA_RIGHT = 101;

    public static final int REQUEST_COMPOSITE = 200;
    public static final int REQUEST_GALLERY=201;
}
