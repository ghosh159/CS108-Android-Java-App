package com.csl.cs108ademoapp.fragments;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.csl.cs108library4a.ReaderDevice;

import com.csl.cs108ademoapp.CustomDrawableView;
import com.csl.cs108ademoapp.CustomMediaPlayer;
import com.csl.cs108ademoapp.InventoryRfidTask;
import com.csl.cs108ademoapp.SelectTag;
import com.csl.cs108ademoapp.MainActivity;
import com.csl.cs108ademoapp.R;
import com.csl.cs108library4a.Cs108Library4A;
import com.csl.cs108library4a.ReaderDevice;

public class InventoryRfidSearchFragment extends CommonFragment implements SensorEventListener {
    double dBuV_dBm_constant = MainActivity.csLibrary4A.dBuV_dBm_constant;
    final int labelMin = -90;
    final int labelMax = -10;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;


    SelectTag selectTag;
    private SensorManager sensorManager;
    private ProgressBar geigerProgress;
    private CheckBox checkBoxGeigerTone;
    private SeekBar seekGeiger;
    private Spinner memoryBankSpinner;
    private EditText editTextRWSelectOffset, editTextGeigerAntennaPower;
    private TextView geigerThresholdView;
    private TextView geigerTagRssiView;
    private TextView geigerTagGotView;
    private TextView geigerRunTime, geigerVoltageLevelView;
    private TextView rfidYieldView;
    private TextView rfidRateView;
    private Button button;

    private boolean started = false;
    int thresholdValue = 0;
    RFIDLocalization rfidLocalization;
    private InventoryRfidTask geigerSearchTask;
    private Sensor rotationVectorSensor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState, true);
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        return inflater.inflate(R.layout.fragment_geiger_search, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Directly reference your CustomDrawableView
        CustomDrawableView directionIndicator = view.findViewById(R.id.directionIndicator);

        // Since directionIndicator is already an instance of CustomDrawableView,
        // you don't need to create a new instance or set it as a background.

        // Set any other initial settings for your drawable or other views here...
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        androidx.appcompat.app.ActionBar actionBar;
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setIcon(R.drawable.dl_loc);
        actionBar.setTitle(R.string.title_activity_geiger);

        selectTag = new SelectTag((Activity)getActivity ());
        TableRow tableRowProgressLabel;
        TextView textViewProgressLabelMin = (TextView) getActivity().findViewById(R.id.geigerProgressLabelMin);
        TextView textViewProgressLabelMid = (TextView) getActivity().findViewById(R.id.geigerProgressLabelMid);
        TextView textViewProgressLabelMax = (TextView) getActivity().findViewById(R.id.geigerProgressLabelMax);
        textViewProgressLabelMin.setText(String.format("%.0f", MainActivity.csLibrary4A.getRssiDisplaySetting() != 0 ? labelMin : labelMin + dBuV_dBm_constant));
        textViewProgressLabelMid.setText(String.format("%.0f", MainActivity.csLibrary4A.getRssiDisplaySetting() != 0 ? labelMin + (labelMax - labelMin) / 2 : labelMin + (labelMax - labelMin) / 2 + dBuV_dBm_constant));
        textViewProgressLabelMax.setText(String.format("%.0f", MainActivity.csLibrary4A.getRssiDisplaySetting() != 0 ? labelMax : labelMax + dBuV_dBm_constant));

        geigerProgress = (ProgressBar) getActivity().findViewById(R.id.geigerProgress);
        checkBoxGeigerTone = (CheckBox) getActivity().findViewById(R.id.geigerToneCheck);
        rfidLocalization = new RFIDLocalization();

        final ReaderDevice tagSelected = MainActivity.tagSelected;
        if (tagSelected != null) {
            if (tagSelected.getSelected() == true) {
                selectTag.editTextTagID.setText(tagSelected.getAddress());
            }
        }

        seekGeiger = (SeekBar) getActivity().findViewById(R.id.geigerSeek);
        seekGeiger.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar == seekGeiger && fromUser == true) {
                    thresholdValue = progress;
                    geigerThresholdView.setText(String.format("%.2f", MainActivity.csLibrary4A.getRssiDisplaySetting() == 0 ? thresholdValue : thresholdValue - dBuV_dBm_constant));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        memoryBankSpinner = (Spinner) getActivity().findViewById(R.id.selectMemoryBank);
        ArrayAdapter<CharSequence> memoryBankAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.read_memoryBank_options, R.layout.custom_spinner_layout);
        memoryBankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        memoryBankSpinner.setAdapter(memoryBankAdapter);
        memoryBankSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: //if EPC
                        if (tagSelected != null) selectTag.editTextTagID.setText(tagSelected.getAddress());
                        editTextRWSelectOffset.setText("32");
                        break;
                    case 1:
                        if (tagSelected != null) { if (tagSelected.getTid() != null) selectTag.editTextTagID.setText(tagSelected.getTid()); }
                        editTextRWSelectOffset.setText("0");
                        break;
                    case 2:
                        if (tagSelected != null) { if (tagSelected.getUser() != null) selectTag.editTextTagID.setText(tagSelected.getUser()); }
                        editTextRWSelectOffset.setText("0");
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        editTextRWSelectOffset = (EditText) getActivity().findViewById(R.id.selectMemoryOffset);

        TableRow tableRowSelectPassword = (TableRow) getActivity().findViewById(R.id.selectPasswordRow);
        tableRowSelectPassword.setVisibility(View.GONE);

        editTextGeigerAntennaPower = (EditText) getActivity().findViewById(R.id.selectAntennaPower);
        editTextGeigerAntennaPower.setText(String.valueOf(300));

        geigerThresholdView = (TextView) getActivity().findViewById(R.id.geigerThreshold);
        geigerTagRssiView = (TextView) getActivity().findViewById(R.id.geigerTagRssi);
        geigerTagRssiView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if (alertRssiUpdateTime < 0) return;
                double rssi = Double.parseDouble(geigerTagRssiView.getText().toString());
                if (MainActivity.csLibrary4A.getRssiDisplaySetting() != 0) rssi += dBuV_dBm_constant;

                double progressPos = geigerProgress.getMax() * ( rssi - labelMin - dBuV_dBm_constant) / (labelMax - labelMin);
                if (progressPos < 0) progressPos = 0;
                if (progressPos > geigerProgress.getMax()) progressPos = geigerProgress.getMax();
                geigerProgress.setProgress((int) (progressPos));

                alertRssiUpdateTime = System.currentTimeMillis(); alertRssi = rssi;
                if (DEBUG) MainActivity.csLibrary4A.appendToLog("afterTextChanged(): alerting = " + alerting + ", alertRssi = " + alertRssi);
                if (rssi > thresholdValue && checkBoxGeigerTone.isChecked()) {
                    if (alerting == false)  {
                        alerting = true;
        mHandler.removeCallbacks(mAlertRunnable);
        mHandler.post(mAlertRunnable);

        if (DEBUG) MainActivity.csLibrary4A.appendToLog("afterTextChanged(): mAlertRunnable starts");
    }
}
            }
                    });
        geigerRunTime = (TextView) getActivity().findViewById(R.id.geigerRunTime);
        geigerTagGotView = (TextView) getActivity().findViewById(R.id.geigerTagGot);
        geigerVoltageLevelView = (TextView) getActivity().findViewById(R.id.geigerVoltageLevel);
        rfidYieldView = (TextView) getActivity().findViewById(R.id.geigerYield);
        rfidRateView = (TextView) getActivity().findViewById(R.id.geigerRate);
        button = (Button) getActivity().findViewById(R.id.geigerStart);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopHandler(false);
            }
        });

        playerN = MainActivity.sharedObjects.playerL;
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

        setNotificationListener();
    }

    @Override
    public void onPause() {
        MainActivity.csLibrary4A.setNotificationListener(null);
        sensorManager.unregisterListener(this);

        super.onPause();
    }

    @Override
    public void onDestroy() {
        MainActivity.csLibrary4A.setNotificationListener(null);
        if (geigerSearchTask != null) {
            geigerSearchTask.taskCancelReason = InventoryRfidTask.TaskCancelRReason.DESTORY;
        }
        MainActivity.csLibrary4A.restoreAfterTagSelect();
        super.onDestroy();
    }

    public InventoryRfidSearchFragment() {
        super("InventoryRfidSearchFragment");


    }

    double alertRssi; boolean alerting = false; long alertRssiUpdateTime;
    CustomMediaPlayer playerN;
    private final Runnable mAlertRunnable = new Runnable() {
        @Override
        public void run() {
            boolean alerting1 = true;
            final int toneLength = 50;

            mHandler.removeCallbacks(mAlertRunnable);

            if (alertRssi < 20 || alertRssi < thresholdValue || checkBoxGeigerTone.isChecked() == false || alertRssiUpdateTime < 0 || System.currentTimeMillis() - alertRssiUpdateTime > 200) alerting1 = false;
            if (alerting1 == false) {
                playerN.pause(); alerting = false;
                if (DEBUG) MainActivity.csLibrary4A.appendToLog("mAlertRunnable(): ENDS with new alerting1 = " + alerting1 + ", alertRssi = " + alertRssi);
            } else if (playerN.isPlaying() == false) {
                if (DEBUG) MainActivity.csLibrary4A.appendToLog("mAlertRunnable(): TONE starts");
                mHandler.postDelayed(mAlertRunnable, toneLength);
                playerN.start();
            } else {
                int tonePause = 0;
                if (alertRssi >= 60) tonePause = toneLength;
                else if (alertRssi >= 50) tonePause = 250 - toneLength;
                else if (alertRssi >= 40) tonePause = 500 - toneLength;
                else if (alertRssi >= 30) tonePause = 1000 - toneLength;
                else if (alertRssi >= 20) tonePause = 2000 - toneLength;
                if (tonePause > 0) mHandler.postDelayed(mAlertRunnable, tonePause);
                if (tonePause <= 0 || alertRssi < 60) { playerN.pause(); if (DEBUG) MainActivity.csLibrary4A.appendToLog("Pause"); }
                if (DEBUG) MainActivity.csLibrary4A.appendToLog("mAlertRunnable(): START with new alerting1 = " + alerting1 + ", alertRssi = " + alertRssi);
                alerting = tonePause > 0 ? true : false;
            }
        }
    };

    void setNotificationListener() {
        MainActivity.csLibrary4A.setNotificationListener(new Cs108Library4A.NotificationListener() {
            @Override
            public void onChange() {
                startStopHandler(true);
            }
        });
    }

    void startStopHandler(boolean buttonTrigger) {
        boolean started = false;
        if (geigerSearchTask != null) {
            if (geigerSearchTask.getStatus() == AsyncTask.Status.RUNNING) started = true;
        }
        if (buttonTrigger == true &&
                ((started && MainActivity.csLibrary4A.getTriggerButtonStatus())
                        || (started == false && MainActivity.csLibrary4A.getTriggerButtonStatus() == false)))   return;
        if (started == false) {
            if (MainActivity.csLibrary4A.isBleConnected() == false) {
                Toast.makeText(MainActivity.mContext, R.string.toast_ble_not_connected, Toast.LENGTH_SHORT).show();
                return;
            } else if (MainActivity.csLibrary4A.isRfidFailure()) {
                Toast.makeText(MainActivity.mContext, "Rfid is disabled", Toast.LENGTH_SHORT).show();
                return;
            } else if (MainActivity.csLibrary4A.mrfidToWriteSize() != 0) {
                Toast.makeText(MainActivity.mContext, R.string.toast_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            startInventoryTask();
            alertRssiUpdateTime = 0;
        } else {
            if (buttonTrigger) geigerSearchTask.taskCancelReason = InventoryRfidTask.TaskCancelRReason.BUTTON_RELEASE;
            else geigerSearchTask.taskCancelReason = InventoryRfidTask.TaskCancelRReason.STOP;
            alertRssiUpdateTime = -1;
        }
    }

    void startInventoryTask() {
        started = true; boolean invalidRequest = false;
        int memorybank = memoryBankSpinner.getSelectedItemPosition();
        int powerLevel = Integer.valueOf(editTextGeigerAntennaPower.getText().toString());
        if (powerLevel < 0 || powerLevel > 330) invalidRequest = true;
        else if (MainActivity.csLibrary4A.setSelectedTag(selectTag.editTextTagID.getText().toString(), memorybank+1, powerLevel) == false) {
            invalidRequest = true;
        } else {
            MainActivity.csLibrary4A.startOperation(Cs108Library4A.OperationTypes.TAG_SEARCHING);
        }
        geigerSearchTask = new InventoryRfidTask(getContext(), -1,-1, 0, 0, 0, 0, invalidRequest, true,
                null, null, geigerTagRssiView, null,
                geigerRunTime, geigerTagGotView, geigerVoltageLevelView, null, button, rfidRateView);
        geigerSearchTask.execute();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
/*        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                Log.d("SensorData", "Accelerometer: x=" + event.values[0] + " y=" + event.values[1] + " z=" + event.values[2]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                Log.d("SensorData", "Gyroscope: x=" + event.values[0] + " y=" + event.values[1] + " z=" + event.values[2]);
                break;
   *//*         case Sensor.TYPE_MAGNETIC_FIELD:
                Log.d("SensorData", "Magnetometer: x=" + event.values[0] + " y=" + event.values[1] + " z=" + event.values[2]);
                break;*//*
        }*/
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            float[] orientationValues = new float[3];

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationValues);
            /*            double measuredRSSI = 0;*/
            float azimuth = orientationValues[0];
            float pitch = orientationValues[1];
            float roll = orientationValues[2];
            /*      measuredRSSI = Double.parseDouble(geigerTagRssiView.getText().toString());*/

            double[] result = rfidLocalization.update(alertRssi, rotationMatrix);
            Log.d("result", "estimated angle" + result[0] + "estimated distance" + result[1]);
            /*            Log.d("RotationVector", "Azimuth: " + azimuth + ", Pitch: " + pitch + ", Roll: " + roll);}*/
        }

    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
