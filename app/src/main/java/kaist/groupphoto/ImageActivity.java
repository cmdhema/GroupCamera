package kaist.groupphoto;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageActivity extends Activity {

    private ImageView iv;
    private PhotoViewAttacher attacher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        iv = (ImageView) findViewById(R.id.iv_image);

        String fileName = getIntent().getStringExtra("name");
        Bitmap bitmap = BitmapFactory.decodeFile(fileName);

        iv.setImageBitmap(bitmap);

        attacher = new PhotoViewAttacher(iv);
        attacher.update();

    }
}
