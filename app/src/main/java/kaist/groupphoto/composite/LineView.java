package kaist.groupphoto.composite;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import kaist.groupphoto.Constant;
import kaist.groupphoto.listener.CompositeListener;

/**
 * Created by kjwook on 2016. 6. 3..
 */
public class LineView extends View implements View.OnTouchListener {
    private static final String TAG = "GroupPhoto:LineView";
    private CompositeListener listener;

    private Context mContext;

    private Paint mPaint;

    private float linePointX;
    private boolean isLineSelected;
    private boolean isAreaSelected;

    private float selectedLinePointX;
    private int area;

    public LineView(Context context) {
        super(context);
        mContext = context;
        mPaint = new Paint();
        this.setOnTouchListener(this);
        initPaint();
    }

    public LineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mContext = context;
        this.setOnTouchListener(this);
        initPaint();
    }

    private void initPaint() {

        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(3);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if ( !isLineSelected )
            canvas.drawLine(linePointX, 0, linePointX, this.getHeight(), mPaint);
        else if ( isLineSelected )
            canvas.drawLine(selectedLinePointX, 0, selectedLinePointX, this.getHeight(), mPaint);
        else if ( !isLineSelected && !isAreaSelected ) {
            canvas.drawLine(0, 0, linePointX, this.getHeight(), mPaint);
        }

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        linePointX = motionEvent.getX();
        Log.i(TAG, "xPoint : " + linePointX);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if ( isLineSelected )
                showAreaSelectDialog();
            else
                showLineSelectDialog();
        }
        invalidate();

        return true;
    }

    private void showLineSelectDialog() {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        isLineSelected = true;
                        selectedLinePointX = linePointX;
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        isLineSelected = false;
                        selectedLinePointX = 0;
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("Line selected?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show()
                .setCancelable(false);
    }

    private void showAreaSelectDialog(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        isAreaSelected = true;
                        if ( selectedLinePointX > linePointX)
                            area = Constant.SELECT_AREA_LEFT;
                        else
                            area = Constant.SELECT_AREA_RIGHT;

                        listener.areaSelectDone(area, selectedLinePointX);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        isAreaSelected = false;
                        isLineSelected = false;
                        linePointX = 0;
                        invalidate();
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("Area selected?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show()
                .setCancelable(false);
    }


    public void setOnCompositeListener(CompositeListener listener ) {
        this.listener = listener;
    }
}
