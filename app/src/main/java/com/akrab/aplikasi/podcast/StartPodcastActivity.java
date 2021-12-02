package com.akrab.aplikasi.podcast;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc.Constants;

public class StartPodcastActivity extends AppCompatActivity {

    int channelProfile;
    public static final String channelMessage = "com.akrab.aplikasi.CHANNEL";
    public static final String profileMessage = "com.akrab.aplikasi.PROFILE";
    String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int MY_PERMISSIONS_REQUEST_CAMERA = 0;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        type = getIntent().getStringExtra("type");

        channelProfile = Constants.CLIENT_ROLE_BROADCASTER;
        String channelName = getIntent().getStringExtra("name");
        Intent intent = new Intent(this, PodcastActivity.class);
        intent.putExtra(channelMessage, channelName);
        intent.putExtra(profileMessage, channelProfile);
        intent.putExtra("type", type);
        startActivity(intent);

    }
}