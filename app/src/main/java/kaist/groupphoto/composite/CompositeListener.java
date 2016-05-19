package kaist.groupphoto.composite;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kjwook on 2016. 5. 19..
 */
public interface CompositeListener {
    void cropDone(List<Point> list);
}
