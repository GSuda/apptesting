package com.example.testingchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class Login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Ensure this is the correct layout file

        EditText username = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        Button loginbtn = findViewById(R.id.login_button);

        loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputUsername = username.getText().toString();
                String inputPassword = password.getText().toString();

                if (inputUsername.equals("nadia") && inputPassword.equals("1234")) {
                    // correct
                    Toast.makeText(Login.this, "LOGIN SUCCESSFUL", Toast.LENGTH_SHORT).show();
                    // Navigate to MainActivity
                    Intent intent = new Intent(Login.this, HomePage.class);
                    startActivity(intent);
                    finish(); // Optional: close the login activity
                } else {
                    // incorrect
                    Toast.makeText(Login.this, "LOGIN FAILED !!!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
