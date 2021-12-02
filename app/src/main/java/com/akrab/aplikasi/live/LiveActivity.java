package com.akrab.aplikasi.live;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.akrab.aplikasi.MainActivity;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.adapter.AdapterLiveChat;
import com.akrab.aplikasi.model.ModelLiveChat;
import com.akrab.aplikasi.profile.UserProfileActivity;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

@SuppressWarnings("ALL")
public class LiveActivity extends AppCompatActivity {

    private RtcEngine mRtcEngine;
    private String channelName;
    String type;
    String userId;
    private int channelProfile;
    public static final String LOGIN_MESSAGE = "com.akrab.aplikasi.CHANNEL_LOGIN";
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserLeft();
                }
            });
        }

        @Override
        public void onUserMuteVideo(final int uid, final boolean muted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserVideoMuted(uid, muted);
                }
            });
        }
    };

    public void onLocalAudioMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.setImageResource(R.drawable.ic_mic);
        } else {
            iv.setSelected(true);
            iv.setImageResource(R.drawable.ic_mic_off);
        }

        mRtcEngine.muteLocalAudioStream(iv.isSelected());
    }

    private void onRemoteUserVideoMuted(int uid, boolean muted) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);

        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);

        Object tag = surfaceView.getTag();
        if (tag != null && (Integer) tag == uid) {
            surfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);
        }
    }

    EditText sendMessage;
    ImageView send;
    RecyclerView chat_rv;

    //Post
    AdapterLiveChat liveChat;
    List<ModelLiveChat> modelLives;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        Intent intent = getIntent();
        channelName = intent.getStringExtra(StartLiveActivity.channelMessage);
        channelProfile = intent.getIntExtra(StartLiveActivity.profileMessage, -1);

        if (channelProfile == -1) {

        }

         type = getIntent().getStringExtra("type");
        if (type.equals("audience")){
            findViewById(R.id.local_video_view_container).setVisibility(View.GONE);
            findViewById(R.id.camSwitch).setVisibility(View.GONE);
            findViewById(R.id.videoCam).setVisibility(View.GONE);
            findViewById(R.id.mic).setVisibility(View.GONE);
            findViewById(R.id.remote_video_view_container).setVisibility(View.VISIBLE);
        }else {
            findViewById(R.id.local_video_view_container).setVisibility(View.VISIBLE);
            findViewById(R.id.remote_video_view_container).setVisibility(View.GONE);
        }

        //UserInfo
        CircleImageView mDp = findViewById(R.id.mDp);
        TextView username = findViewById(R.id.username);
        ImageView verify = findViewById(R.id.verify);

        sendMessage = findViewById(R.id.sendMessage);
        send = findViewById(R.id.imageView2);
        chat_rv = findViewById(R.id.chat_rv);

        new Handler().postDelayed(() -> {
            FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                     userId = snapshot.child("userid").getValue().toString();
                    FirebaseDatabase.getInstance().getReference().child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String dp = snapshot.child("photo").getValue().toString();
                            String mUsername = snapshot.child("username").getValue().toString();
                            String mVerify = snapshot.child("verified").getValue().toString();
                            if (!dp.isEmpty()){
                                Picasso.get().load(dp).into(mDp);
                            }
                            username.setText(mUsername);
                            if (!mVerify.isEmpty()){
                                verify.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        },2000);

        if (type.equals("audience")){
            new Handler().postDelayed(() -> {
                FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()){
                            channelProfile = 0;
                            Intent intent6 = new Intent(LiveActivity.this, MainActivity.class);
                            intent6.putExtra("hisUID", userId);
                            intent6.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent6);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            },2000);
        }

        TextView number = findViewById(R.id.number);

        new Handler().postDelayed(() -> {
            FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).child("Users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    number.setText(String.valueOf(snapshot.getChildrenCount()));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        },2000);

        send.setOnClickListener(v -> {
            String msg = sendMessage.getText().toString();
            if (msg.isEmpty()){
                Snackbar.make(v, "Type a message", Snackbar.LENGTH_SHORT).show();
            }else {

                String timeStamp = ""+System.currentTimeMillis();
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("ChatId", timeStamp);
                hashMap.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
                hashMap.put("msg", msg);
                FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).child("Chats").child(timeStamp).setValue(hashMap);

                sendMessage.setText("");

            }
        });

        modelLives = new ArrayList<>();

        chat_rv.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        chat_rv.setLayoutManager(linearLayoutManager);

        readMessage();

    }

    private void readMessage() {
        DatabaseReference ref =  FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).child("Chats");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                modelLives.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelLiveChat modelLiveChat = ds.getValue(ModelLiveChat.class);
                    modelLives.add(modelLiveChat);
                }
                liveChat = new AdapterLiveChat(LiveActivity.this, modelLives);
                chat_rv.setAdapter(liveChat);
                liveChat.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void onLocalVideoMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.setImageResource(R.drawable.ic_video_call);
        } else {
            iv.setSelected(true);
            iv.setImageResource(R.drawable.ic_video_off);
        }

        mRtcEngine.muteLocalVideoStream(iv.isSelected());

        FrameLayout container = (FrameLayout) findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);
        surfaceView.setZOrderMediaOverlay(!iv.isSelected());
        surfaceView.setVisibility(iv.isSelected() ? View.GONE : View.VISIBLE);
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
    }

    private void onRemoteUserLeft() {
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);
        container.removeAllViews();
    }

    private void initAgoraEngineAndJoinChannel() {
        initalizeAgoraEngine();
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.setClientRole(channelProfile);
        setupVideoProfile();
        setupLocalVideo();
        joinChannel();
    }

    private void initalizeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.private_app_id), mRtcEventHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupVideoProfile() {
        mRtcEngine.enableVideo();

        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x480, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo() {
        FrameLayout container = (FrameLayout) findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0));
    }

    private void joinChannel() {
        mRtcEngine.joinChannel(null, channelName, "Optional Data", 0);
    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (channelProfile == Constants.CLIENT_ROLE_BROADCASTER){
            FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    snapshot.getRef().removeValue();
                    channelProfile = 0;
                    Intent intent6 = new Intent(LiveActivity.this, MainActivity.class);
                    intent6.putExtra("hisUID", userId);
                    intent6.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent6);
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }else {
            FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).removeValue();
            channelProfile = 0;
            Intent intent6 = new Intent(LiveActivity.this, MainActivity.class);
            intent6.putExtra("hisUID", userId);
            intent6.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent6);
            finish();

        }

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (channelProfile == Constants.CLIENT_ROLE_BROADCASTER){
            FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    snapshot.getRef().removeValue();
                    channelProfile = 0;
                    Intent intent6 = new Intent(LiveActivity.this, MainActivity.class);
                    intent6.putExtra("hisUID", userId);
                    intent6.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent6);
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }else {
            FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).removeValue();
            channelProfile = 0;
            Intent intent6 = new Intent(LiveActivity.this, UserProfileActivity.class);
            intent6.putExtra("hisUID", userId);
            intent6.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent6);
            finish();

        }

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;

    }

    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    public void onEndCallClicked(View view) {
       end();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initAgoraEngineAndJoinChannel();
    }

    private void end() {

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;

        if (type.equals("host")){
            FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    snapshot.getRef().removeValue();
                    channelProfile = 0;
                    Intent intent6 = new Intent(LiveActivity.this, MainActivity.class);
                    intent6.putExtra("hisUID", userId);
                    intent6.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent6);
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }else {
            FirebaseDatabase.getInstance().getReference().child("Live").child(channelName).child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).removeValue();
            channelProfile = 0;
            Intent intent6 = new Intent(LiveActivity.this, MainActivity.class);
            intent6.putExtra("hisUID", userId);
            intent6.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent6);
            finish();
        }

    }
}
