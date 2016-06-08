package kaist.groupphoto.composite;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import kaist.groupphoto.Constant;
import kaist.groupphoto.R;
import kaist.groupphoto.listener.CompositeListener;

public class CompositeSelectActivity extends Activity {

    private ImageView imageView;
    private LineView lineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_composite_select);

        imageView = (ImageView) findViewById(R.id.iv_image);
        lineView = (LineView) findViewById(R.id.view_line);

        lineView.setOnCompositeListener(compositeListener);
        String imagePath="";
        Bitmap image = null;
        if ( getIntent().getStringExtra("name") != null ) {
            imagePath = getIntent().getStringExtra("name");
            image = BitmapFactory.decodeFile(imagePath);
        }
        else if ( getIntent().getStringExtra("path") != null ) {
            imagePath = getIntent().getStringExtra("path");
            image = BitmapFactory.decodeFile(imagePath+".jpg");
        }

        imageView.setImageBitmap(image);


    }

    CompositeListener compositeListener = new CompositeListener() {

        @Override
        public void areaSelectDone(int direction, float xPoint) {
//            compositeImage(direction, xPoint);
            Intent intent = new Intent();
            intent.putExtra("direction", direction);
            intent.putExtra("xPoint", xPoint);

            setResult(Constant.REQUEST_COMPOSITE, intent);
            finish();
        }
    };

}
