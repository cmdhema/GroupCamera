/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kaist.groupphoto;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.ArrayList;
import java.util.List;

import kaist.groupphoto.camera.GraphicOverlay;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    static int a = 0;
    private int landmarkCount = 0;

    private static final float FACE_POSITION_RADIUS = 30.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    ArrayList<Integer> faceFactor;
    private static final int COLOR_CHOICES[] = {
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.MAGENTA,
        Color.RED,
        Color.WHITE,
        Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private float mFaceHappiness;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);


        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        a+=1;
        landmarkCount = 0;
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);
        String s = "";
        List<Landmark> landmakrs = face.getLandmarks();
        if ( isContain(landmakrs, Landmark.RIGHT_EYE) && isContain(landmakrs, Landmark.LEFT_EYE)) {
            if ( !(isContain(landmakrs, Landmark.BOTTOM_MOUTH)) && (!isContain(landmakrs, Landmark.LEFT_MOUTH)||!isContain(landmakrs, Landmark.RIGHT_MOUTH) ))
                canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        }

        if ( isContain(landmakrs, Landmark.LEFT_MOUTH) && isContain(landmakrs, Landmark.LEFT_EYE) && isContain(landmakrs, Landmark.LEFT_CHEEK)) {
            if ( !(isContain(landmakrs, Landmark.RIGHT_CHEEK)) && !(isContain(face.getLandmarks(), Landmark.RIGHT_EYE)))
                canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        }

        if ( isContain(landmakrs, Landmark.RIGHT_MOUTH) && isContain(landmakrs, Landmark.RIGHT_EYE) && isContain(landmakrs, Landmark.RIGHT_CHEEK)) {
            if ( !(isContain(landmakrs, Landmark.LEFT_CHEEK)) && !(isContain(face.getLandmarks(), Landmark.LEFT_EYE)))
                canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        }




        for (Landmark landmark : face.getLandmarks()) {

//            if ( !( landmark.getType() == Landmark.LEFT_EYE && landmark.getType() == Landmark.RIGHT_EYE ))
//                canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);

            s += landmark.getType() +", ";
//            switch (landmark.getType()) {
//                case Landmark.BOTTOM_MOUTH:
//                    Log.i("FaceGraphic", "BOTTOM_MOUTH");
//                    landmarkCount++;
//                case Landmark.LEFT_MOUTH:
//                    Log.i("FaceGraphic", "LEFT_MOUTH");
//                    landmarkCount++;
//                case Landmark.RIGHT_MOUTH:
//                    Log.i("FaceGraphic", "RIGHT_MOUTH");
//                    landmarkCount++;
//                case Landmark.RIGHT_EYE:
//                    Log.i("FaceGraphic", "RIGHT_EYE");
//                    landmarkCount++;
//                case Landmark.LEFT_EYE:
//                    Log.i("FaceGraphic", "LEFT_EYE");
//                    landmarkCount++;
//                case Landmark.NOSE_BASE:
//                    Log.i("FaceGraphic", "NOSE_BASE");
//                    landmarkCount++;
//                default:
//                    break;
//            }
        }
        if ( a % 10 == 0)
            canvas.drawText(s, 30, 30,mIdPaint);

//        if ( landmarkCount < 5 )
//            canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
//        canvas.drawText("id: " + face.getLandmarks().size() +"개 입니다", x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);

//        canvas.drawText("right eye: " + String.format("%.2f", face.getIsRightEyeOpenProbability()), x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint);
//        canvas.drawText("left eye: " + String.format("%.2f", face.getIsLeftEyeOpenProbability()), x - ID_X_OFFSET*2, y - ID_Y_OFFSET*2, mIdPaint);

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

    }

    private boolean isContain(List<Landmark> list, int type) {
        for ( int i = 0; i < list.size(); i++ ) {
            if ( list.get(i).getType() == type)
                return true;
        }

        return false;
    }
}
