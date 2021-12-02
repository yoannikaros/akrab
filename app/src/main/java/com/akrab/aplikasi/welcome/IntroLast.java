package com.akrab.aplikasi.welcome;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.akrab.aplikasi.R;
import com.akrab.aplikasi.emailAuth.LoginActivity;
import com.akrab.aplikasi.phoneAuth.GenerateOTPActivity;

public class IntroLast extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_last);

        Button email = findViewById(R.id.next);
        Button phone = findViewById(R.id.phone);

        email.setOnClickListener(v -> startActivity( new Intent(getApplicationContext(), LoginActivity.class )));

        phone.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), GenerateOTPActivity.class )));

    }
}