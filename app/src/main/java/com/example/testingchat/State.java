package com.example.testingchat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.testingchat.network.ApiService;
import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class State extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final String TAG = "State";

    private BluetoothAdapter bluetoothAdapter;
    private TGDevice tgDevice;
    private TextView eyeStateYes;

    private boolean isRecording = false;
    private List<String[]> recordedData = new ArrayList<>();
    private Handler recordHandler = new Handler();
    private Runnable recordRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state);

        eyeStateYes = findViewById(R.id.eye_state_yes);
        Button eyeStateButton = findViewById(R.id.eye_state_button);

        eyeStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }
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
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, REQUEST_PERMISSIONS);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions granted");
                initializeBluetooth();
            } else {
                Log.e(TAG, "Permissions denied");
                Toast.makeText(this, "Permissions are required to use this app", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeBluetooth() {
        try {
            // Enable Bluetooth if it's not enabled
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Bluetooth connect permission not granted");
                    return;
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                initializeTGDevice();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during Bluetooth initialization", e);
            Toast.makeText(this, "Error during Bluetooth initialization: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                    handleStateChange(msg.arg1);
                    break;
                case TGDevice.MSG_MEDITATION:
                    int meditation = msg.arg1;
                    break;
                case TGDevice.MSG_BLINK:
                    int blink = msg.arg1;
                    break;
                case TGDevice.MSG_RAW_DATA:
                    int rawData = msg.arg1;
                    break;
                case TGDevice.MSG_EEG_POWER:
                    TGEegPower ep = (TGEegPower) msg.obj;
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
    private void handleStateChange(int state) {
        switch (state) {
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
                Log.e(TAG, "State: Not Found");
                break;
            case TGDevice.STATE_NOT_PAIRED:
                Log.e(TAG, "State: Not Paired");
                break;
            case TGDevice.STATE_DISCONNECTED:
                Log.e(TAG, "State: Disconnected");
                break;
            default:
                Log.d(TAG, "Unknown state: " + state);
                break;
        }
    }

    private void startRecording() {
        isRecording = true;
        eyeStateYes.setText("Recording data...");
        recordedData.clear();
        recordRunnable = new Runnable() {
            @Override
            public void run() {
                stopRecording();
                saveDataToCSV();
                uploadRecordedFile();
            }
        };
        recordHandler.postDelayed(recordRunnable, 5000); // Record for 3 seconds
    }

    private void stopRecording() {
        isRecording = false;
        eyeStateYes.setText("Recording stopped. Uploading data...");
        recordHandler.removeCallbacks(recordRunnable);
    }

    private void recordEEGData(TGEegPower ep) {
        String[] data = {
                String.valueOf(ep.midGamma),
                String.valueOf(ep.lowGamma),
                String.valueOf(ep.highBeta),
                String.valueOf(ep.lowBeta),
                String.valueOf(ep.highAlpha),
                String.valueOf(ep.lowAlpha),
                String.valueOf(ep.theta),
                String.valueOf(ep.delta),







        };
        recordedData.add(data);
    }

    private void saveDataToCSV() {
        String fileName = "file_recorded.csv";
        File file = new File(getExternalFilesDir(null), fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("MID_GAMMA,LOW_GAMMA,HIGH_ALPHA,LOW_BETA,HIGH_BETA,THETA,LOW_ALPHA,DELTA,EYE STATE{1:open 0:close}\n");
            for (String[] data : recordedData) {
                writer.append(String.join(",", data)).append("\n");
            }
            Toast.makeText(this, "Data saved to " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("State", "Error saving data to CSV", e);
            Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void uploadRecordedFile() {
        File file = new File(getExternalFilesDir(null), "file_recorded.csv");

        // Log the file path for debugging
        Log.d("State", "File path: " + file.getAbsolutePath());

        // Check if the file exists
        if (!file.exists()) {
            eyeStateYes.setText("File not found");
            Log.e("State", "File not found: " + file.getAbsolutePath());
            return;
        }

        // Prepare the file for upload
        RequestBody requestFile = RequestBody.create(MediaType.parse("text/csv"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.36:5000") // Replace with your actual server IP and port
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        // Make the API call
        Call<List<Integer>> call = apiService.uploadFile(body);
        call.enqueue(new Callback<List<Integer>>() {
            @Override
            public void onResponse(Call<List<Integer>> call, Response<List<Integer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Integer> predictions = response.body();
                    // Update the TextView based on the prediction
                    if (predictions.contains(1)) {
                        eyeStateYes.setText("Yes");
                    } else {
                        eyeStateYes.setText("No");
                    }
                } else {
                    eyeStateYes.setText("Error in response");
                }
            }

            @Override
            public void onFailure(Call<List<Integer>> call, Throwable t) {
                Log.e("State", "Error uploading file", t);
                eyeStateYes.setText("Upload failed: " + t.getMessage());
            }
        });
    }
}