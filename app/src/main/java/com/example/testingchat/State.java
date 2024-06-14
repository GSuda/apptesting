package com.example.testingchat;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class State extends AppCompatActivity {

    private ImageView backArrow, profileImage, homeIcon, menuIcon;
    private TextView tabEyeMovement, tabEyeState, tabMoreTests, connectingText, eyeStatusText, eyeStateClosed, eyeStateOpen, eyeStateNo, eyeStateYes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state); // Update with the actual name of your layout file

        // Bind views to variables
        backArrow = findViewById(R.id.back_arrow);
        profileImage = findViewById(R.id.profile_image);
        homeIcon = findViewById(R.id.home_icon);


        tabEyeMovement = findViewById(R.id.tab_eye_movement);
        tabEyeState = findViewById(R.id.tab_eye_state);
        tabMoreTests = findViewById(R.id.tab_more_tests);

        connectingText = findViewById(R.id.connecting_text);
        eyeStatusText = findViewById(R.id.eye_status_text);
        eyeStateClosed = findViewById(R.id.eye_state_closed);
        eyeStateOpen = findViewById(R.id.eye_state_open);
        eyeStateNo = findViewById(R.id.eye_state_no);
        eyeStateYes = findViewById(R.id.eye_state_yes);

        // Set up click listeners
        backArrow.setOnClickListener(v -> onBackPressed());

        homeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to State activity when Eye State tab is clicked
                startActivity(new Intent(State.this, HomePage.class));
            }
        });


        menuIcon.setOnClickListener(v -> {
            // Handle menu icon click
            // Open menu or drawer
        });

        tabEyeMovement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to State activity when Eye State tab is clicked
                startActivity(new Intent(State.this, EyeMovement.class));
            }
        });

        tabMoreTests.setOnClickListener(v -> {
            // Handle more tests tab click
            // Intent to navigate to more tests activity
        });
    }
}