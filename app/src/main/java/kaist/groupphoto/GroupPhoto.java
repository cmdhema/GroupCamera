package kaist.groupphoto;

import android.util.SparseArray;

import com.google.android.gms.vision.face.Face;

/**
 * Created by kjwook on 2016. 5. 18..
 */
public class GroupPhoto {
    private byte[] data;
    private int eyesNum;
    private String filePath;
    private SparseArray<Face> faces;

    public SparseArray<Face> getFaces() {
        return faces;
    }

    public void setFaces(SparseArray<Face> faces) {
        this.faces = faces;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getEyesNum() {
        return eyesNum;
    }

    public void setEyesNum(int eyesNum) {
        this.eyesNum = eyesNum;
    }
}
