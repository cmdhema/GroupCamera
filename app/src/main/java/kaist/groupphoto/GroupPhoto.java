package kaist.groupphoto;

/**
 * Created by kjwook on 2016. 5. 18..
 */
public class GroupPhoto {
    private byte[] data;
    private int eyesNum;
    private String filePath;

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
