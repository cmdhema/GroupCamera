/*
 * Copyright 2014-2016 Media for Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kaist.groupphoto.controls;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


import kaist.groupphoto.CameraActivity;
import kaist.groupphoto.R;
import kaist.groupphoto.listener.SpinnerSelectListener;

public class CameraSettingsPopup {

    private SpinnerSelectListener listener;

    private Context context;
    private static int mCameraModeSelectedItem, mCEModeSelectedItem;
    private Spinner mCEModeSpinner, mCameraModeSpinner;
    private String mSelectedCameraMode, mSelectedCEMode;
    CameraActivity cameraActivity;

    public void setOnSpinnerSelectListener(SpinnerSelectListener listener) {
        this.listener = listener;
    }

    public CameraSettingsPopup(Context context) {
        this.context = context;
        cameraActivity = new CameraActivity();
    }

    public void show() {
        showCameraModeDialog();
    }

    private void showCameraModeDialog() {

        LayoutInflater inflater = LayoutInflater.from(context);
        View alertLayout = inflater.inflate(R.layout.popup_camera_settings, null);

        mCameraModeSpinner = (Spinner) alertLayout.findViewById(R.id.cam_mode_settings);
        mCEModeSpinner = (Spinner) alertLayout.findViewById(R.id.ce_mode_settings);

        String[] cameraModeArray = {"Full", "Closed Eye Detection", "Composite Picture"};
        String[] ceModeArray = {"Auto", "Manual"}; /*getResources().getStringArray(R.array.cemode_array);*/

        ArrayAdapter<String> mCameraModeAdapter;
        mCameraModeAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, cameraModeArray);
        mCameraModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter mCEModeAdapter;
        mCEModeAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, ceModeArray);
        mCEModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mCameraModeSpinner.setAdapter(mCameraModeAdapter);
        mCameraModeSpinner.setSelection(mCameraModeSelectedItem);

        mCEModeSpinner.setAdapter(mCEModeAdapter);
        mCEModeSpinner.setSelection(mCEModeSelectedItem);

        mCameraModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                //mCameraModeSpinner.setSelection(mCameraModeSpinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mCEModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Camera Mode Settings");
        alert.setView(alertLayout);
        alert.setCancelable(false);
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, "Cancel clicked", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                listener.onItemSelect(mCameraModeSpinner.getSelectedItemPosition());
            }
        });

        AlertDialog dialog = alert.create();
        dialog.show();
    }


}
