package com.example.testingchat;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final String TAG = "MainActivity";

    private BluetoothAdapter bluetoothAdapter;
    private TGDevice tgDevice;
    private TextView textViewAttention;
    private TextView textViewMeditation;
    private TextView textViewBlink;
    private TextView textViewRawData;
    private TextView textViewDelta;
    private TextView textViewTheta;
    private TextView textViewLowAlpha;
    private TextView textViewHighAlpha;
    private TextView textViewLowBeta;
    private TextView textViewHighBeta;
    private TextView textViewLowGamma;
    private TextView textViewHighGamma;
    private Button buttonRecord;

    private boolean isRecording = false;
    private List<String[]> recordedData = new ArrayList<>();
    private Handler recordHandler = new Handler();
    private Runnable recordRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewAttention = findViewById(R.id.textViewAttention);
        textViewMeditation = findViewById(R.id.textViewMeditation);
        textViewBlink = findViewById(R.id.textViewBlink);
        textViewRawData = findViewById(R.id.textViewRawData);
        textViewDelta = findViewById(R.id.textViewDelta);
        textViewTheta = findViewById(R.id.textViewTheta);
        textViewLowAlpha = findViewById(R.id.textViewLowAlpha);
        textViewHighAlpha = findViewById(R.id.textViewHighAlpha);
        textViewLowBeta = findViewById(R.id.textViewLowBeta);
        textViewHighBeta = findViewById(R.id.textViewHighBeta);
        textViewLowGamma = findViewById(R.id.textViewLowGamma);
        textViewHighGamma = findViewById(R.id.textViewHighGamma);

        buttonRecord = findViewById(R.id.buttonRecord);

        buttonRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.e(TAG, "Device doesn't support Bluetooth");
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        // Request necessary permissions
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions();
        } else {
            initializeBluetooth();
        }
    }

    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
        }, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeBluetooth();
            } else {
                Toast.makeText(this, "Permissions are required to use this app", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeBluetooth() {
        // Enable Bluetooth if it's not enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            initializeTGDevice();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Bluetooth enabled");
                initializeTGDevice();
            } else {
                Log.e(TAG, "User denied request to enable Bluetooth");
                Toast.makeText(this, "Bluetooth must be enabled to use this app", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeTGDevice() {
        try {
            tgDevice = new TGDevice(bluetoothAdapter, handler);
            if (tgDevice.getState() != TGDevice.STATE_CONNECTED) {
                Log.d(TAG, "Attempting to connect to TGDevice");
                tgDevice.connect(true);
            } else {
                Log.d(TAG, "TGDevice already connected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TGDevice", e);
            Toast.makeText(this, "Error initializing TGDevice: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TGDevice.MSG_STATE_CHANGE:
                    switch (msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            Log.d(TAG, "State: Idle");
                            break;
                        case TGDevice.STATE_CONNECTING:
                            Log.d(TAG, "State: Connecting");
                            break;
                        case TGDevice.STATE_CONNECTED:
                            Log.d(TAG, "State: Connected");
                            tgDevice.start();
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                            Log.d(TAG, "State: Not Found");
                            break;
                        case TGDevice.STATE_NOT_PAIRED:
                            Log.d(TAG, "State: Not Paired");
                            break;
                        case TGDevice.STATE_DISCONNECTED:
                            Log.d(TAG, "State: Disconnected");
                            break;
                    }
                    break;
                case TGDevice.MSG_MEDITATION:
                    int meditation = msg.arg1;
                    runOnUiThread(() -> textViewMeditation.setText("Meditation: " + meditation));
                    break;
                case TGDevice.MSG_BLINK:
                    int blink = msg.arg1;
                    runOnUiThread(() -> textViewBlink.setText("Blink: " + blink));
                    break;
                case TGDevice.MSG_RAW_DATA:
                    int rawData = msg.arg1;
                    runOnUiThread(() -> textViewRawData.setText("Raw Data: " + rawData));
                    break;
                case TGDevice.MSG_EEG_POWER:
                    TGEegPower ep = (TGEegPower) msg.obj;
                    runOnUiThread(() -> {
                        textViewDelta.setText("Delta: " + ep.delta);
                        textViewTheta.setText("Theta: " + ep.theta);
                        textViewLowAlpha.setText("Low Alpha: " + ep.lowAlpha);
                        textViewHighAlpha.setText("High Alpha: " + ep.highAlpha);
                        textViewLowBeta.setText("Low Beta: " + ep.lowBeta);
                        textViewHighBeta.setText("High Beta: " + ep.highBeta);
                        textViewLowGamma.setText("Low Gamma: " + ep.lowGamma);
                        textViewHighGamma.setText("High Gamma: " + ep.midGamma);
                    });
                    if (isRecording) {
                        recordEEGData(ep);
                    }
                    break;
                default:
                    Log.d(TAG, "Unhandled message: " + msg.what);
                    break;
            }
        }
    };

    private void startRecording() {
        isRecording = true;
        buttonRecord.setText("Stop Recording");
        recordedData.clear();
        recordRunnable = new Runnable() {
            @Override
            public void run() {
                stopRecording();
                saveDataToCSV();
                startRecording();
            }
        };
        recordHandler.postDelayed(recordRunnable, 30000); // Record every 30 seconds
    }

    private void stopRecording() {
        isRecording = false;
        buttonRecord.setText("Start Recording");
        recordHandler.removeCallbacks(recordRunnable);
    }

    private void recordEEGData(TGEegPower ep) {
        String[] data = {
                String.valueOf(ep.delta),
                String.valueOf(ep.theta),
                String.valueOf(ep.lowAlpha),
                String.valueOf(ep.highAlpha),
                String.valueOf(ep.lowBeta),
                String.valueOf(ep.highBeta),
                String.valueOf(ep.lowGamma),
                String.valueOf(ep.midGamma)
        };
        recordedData.add(data);
    }

    private void saveDataToCSV() {
        String fileName = "EEGData_" + System.currentTimeMillis() + ".csv";
        try (FileWriter writer = new FileWriter(getExternalFilesDir(null) + "/" + fileName)) {
            writer.append("Delta,Theta,Low Alpha,High Alpha,Low Beta,High Beta,Low Gamma,High Gamma\n");
            for (String[] data : recordedData) {
                writer.append(String.join(",", data)).append("\n");
            }
            Toast.makeText(this, "Data saved to " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error saving data to CSV", e);
            Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
