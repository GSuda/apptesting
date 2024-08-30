package com.example.testingchat;



import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.testingchat.network.ApiFix;
import com.example.testingchat.network.ApiD;
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


public class EyeMovement extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final String TAG = "State";

    private BluetoothAdapter bluetoothAdapter;
    private TGDevice tgDevice;
    private ImageView backArrow, profileIcon, eyeStateIcon, homeIcon, menuIcon ;
    private LinearLayout tabLayout, connectionStatusTable, statusTexts, bottomNavigation;
    private TextView eyeMovementTab, eyeStateTab, recordTab,eyeFIX,eyeMOVE,isMoving,up,down,left,right;
    private Button testButton,fixationbutton;
    private boolean isRecording = false;
    private String lastClickedButton = "";
    private List<String[]> recordedData = new ArrayList<>();
    private Handler recordHandler = new Handler();
    private Runnable recordRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eye_movement); // Update with the actual name of your layout file

        // Bind views to variables
        backArrow = findViewById(R.id.back_arrow);
        profileIcon = findViewById(R.id.profile_icon);
        eyeStateIcon = findViewById(R.id.eye_state_icon);



        isMoving = findViewById(R.id.isMoving);

        tabLayout = findViewById(R.id.tab_layout);
        connectionStatusTable = findViewById(R.id.connection_status_table);
        statusTexts = findViewById(R.id.status_texts);


        eyeMovementTab = (TextView) tabLayout.getChildAt(0);
        eyeStateTab = (TextView) tabLayout.getChildAt(1);
        recordTab = (TextView) tabLayout.getChildAt(2);

        testButton = findViewById(R.id.test_button);
        fixationbutton = findViewById(R.id.fixation_button);

        up = findViewById(R.id.up);
        down = findViewById(R.id.down);
        left = findViewById(R.id.left);
        right = findViewById(R.id.right);


        // Set up click listeners
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle back arrow click
                onBackPressed();
            }
        });

        profileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle profile icon click
                // Intent to navigate to profile activity
            }
        });

        eyeMovementTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle eye movement tab click
            }
        });

        eyeStateTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to State activity when Eye State tab is clicked
                startActivity(new Intent(EyeMovement.this, State.class));
            }
        });

        recordTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EyeMovement.this, MainActivity.class);
                startActivity(intent);

            }
        });

        fixationbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    lastClickedButton = "fixationButton";
                    stopRecording();
                } else {
                    startRecording();
                }
            }
        });

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    lastClickedButton = "testButton";
                    stopRecordingForTestButton();
                } else {
                    startRecording();
                }
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.e(TAG, "Device doesn't support Bluetooth");
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

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
        isMoving.setText("Recording data...");
        recordedData.clear();
    }

    private void stopRecording() {
        isRecording = false;
        isMoving.setText("....");

        if (lastClickedButton.equals("fixationButton")) {
            saveDataToCSV();
            uploadRecordedFile();
        } else if (lastClickedButton.equals("testButton")) {
            saveDataToCSV2();
            uploadRecordedFileToApiD();
        }
    }

    private void stopRecordingForTestButton() {
        isRecording = false;
        isMoving.setText("Recording stopped. Uploading data...");
        saveDataToCSV();
        uploadRecordedFileToApiD();
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
                String.valueOf(ep.midGamma),

        };
        recordedData.add(data);

        // Stop recording after collecting 5 rows
        if (recordedData.size() >= 5) {
            stopRecording();
        }
    }

    private void saveDataToCSV() {
        String fileName = "file_recorded.csv";
        File file = new File(getExternalFilesDir(null), fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("delta,theta,alphaLow,alphaHigh,betaLow,betaHigh,gammaLow,gammaMid,is_moving\n");
            for (String[] data : recordedData) {
                writer.append(String.join(",", data)).append("\n");
            }
            Toast.makeText(this, "Data saved to " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("State", "Error saving data to CSV", e);
            Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveDataToCSV2() {
        String fileName = "file_recorded.csv";
        File file = new File(getExternalFilesDir(null), fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("delta,theta,alphaLow,alphaHigh,betaLow,betaHigh,gammaLow,gammaMid,directions\n");
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
            eyeFIX.setText("File not found");
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

        ApiFix apiService = retrofit.create(ApiFix.class);

        // Make the API call
        Call<List<Integer>> call = apiService.uploadFile(body);
        call.enqueue(new Callback<List<Integer>>() {
            @Override
            public void onResponse(Call<List<Integer>> call, Response<List<Integer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Integer> predictions = response.body();
                    // Determine the most frequent prediction
                    int fixCount = 0;
                    int moveCount = 0;
                    for (int prediction : predictions) {
                        if (prediction == 0) {
                            fixCount++;
                        } else if (prediction == 1) {
                            moveCount++;
                        }
                    }
                    if (fixCount > moveCount) {
                        isMoving.setText("YES");
                        isMoving.setTextColor(Color.BLUE); // Set text color to green for "Yes"
                        // Set text color to red for "No"
                    } else {
                        isMoving.setText("NO");
                        isMoving.setTextColor(Color.BLUE);   // Set text color to red for "No"
                        // Set text color to green for "Yes"
                    }

                } else {
                    eyeFIX.setText("Error in response");
                }
            }

            @Override
            public void onFailure(Call<List<Integer>> call, Throwable t) {
                Log.e("State", "Error uploading file", t);
                eyeFIX.setText("Upload failed: " + t.getMessage());
            }
        });
    }

    private void uploadRecordedFileToApiD() {
        File file = new File(getExternalFilesDir(null), "file_recorded.csv");

        // Log the file path for debugging
        Log.d("State", "File path: " + file.getAbsolutePath());

        // Check if the file exists
        if (!file.exists()) {
            eyeFIX.setText("File not found");
            Log.e("State", "File not found: " + file.getAbsolutePath());
            return;
        }

        // Prepare the file for upload
        RequestBody requestFile = RequestBody.create(MediaType.parse("text/csv"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.39:5000") // Replace with your actual server IP and port
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiD apiService = retrofit.create(ApiD.class);

        // Make the API call
        Call<List<Integer>> call = apiService.uploadFile(body);
        call.enqueue(new Callback<List<Integer>>() {
            @Override
            public void onResponse(Call<List<Integer>> call, Response<List<Integer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Integer> predictions = response.body();
                    // Determine the most frequent prediction
                    int UPCount = 0;
                    int DOWNCount = 0;
                    int LEFTCount = 0;
                    int RIGHTCount = 0;

                    for (int prediction : predictions) {
                        if (prediction == 1) {
                            UPCount++;
                        } else if (prediction == 2) {
                            DOWNCount++;
                        } else if (prediction == 3) {
                            LEFTCount++;
                        } else if (prediction == 4) {
                            RIGHTCount++;
                        }
                    }

                    if (UPCount > DOWNCount && UPCount > LEFTCount && UPCount > RIGHTCount) {
                        up.setText("Yes");
                        down.setText("NO");
                        left.setText("NO");
                        right.setText("NO");
                        up.setTextColor(Color.GREEN);
                        down.setTextColor(Color.RED);
                        left.setTextColor(Color.RED);
                        right.setTextColor(Color.RED);
                        // Set text color to blue for "Y"
                    } else if (DOWNCount > UPCount && DOWNCount > LEFTCount && DOWNCount > RIGHTCount) {
                        up.setText("NO");
                        down.setText("YES");
                        left.setText("NO");
                        right.setText("NO");
                        up.setTextColor(Color.RED);
                        down.setTextColor(Color.GREEN);
                        left.setTextColor(Color.RED);
                        right.setTextColor(Color.RED); // Set text color to red for "D"
                    } else if (LEFTCount > UPCount && LEFTCount > DOWNCount && LEFTCount > RIGHTCount) {
                        up.setText("NO");
                        down.setText("NO");
                        left.setText("YES");
                        right.setText("NO");
                        up.setTextColor(Color.RED);
                        down.setTextColor(Color.RED);
                        left.setTextColor(Color.GREEN);
                        right.setTextColor(Color.RED);// Set text color to green for "L"
                    } else if (RIGHTCount > UPCount && RIGHTCount > DOWNCount && RIGHTCount > LEFTCount) {
                        up.setText("NO");
                        down.setText("NO");
                        left.setText("NO");
                        right.setText("YES");
                        up.setTextColor(Color.RED);
                        down.setTextColor(Color.RED);
                        left.setTextColor(Color.RED);
                        right.setTextColor(Color.GREEN);// Set text color to yellow for "R"
                    } else {
                        up.setText("Unknown");
                        up.setTextColor(Color.GRAY); // Set text color to gray for unknown state
                    }

                } else {
                    up.setText("Error in response");
                }
            }

            @Override
            public void onFailure(Call<List<Integer>> call, Throwable t) {
                Log.e("State", "Error uploading file", t);
                eyeFIX.setText("Upload failed: " + t.getMessage());
            }
        });
    }



}