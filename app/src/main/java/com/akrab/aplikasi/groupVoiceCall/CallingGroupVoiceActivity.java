package com.akrab.aplikasi.groupVoiceCall;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.group.GroupChatActivity;
import com.akrab.aplikasi.groupVoiceCall.openacall.model.ConstantApp;
import com.akrab.aplikasi.groupVoiceCall.openacall.ui.BaseActivity;
import com.akrab.aplikasi.groupVoiceCall.openacall.ui.VoiceCallGroupActivity;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

@SuppressWarnings("ALL")
public class CallingGroupVoiceActivity extends BaseActivity {

    String groupId,room;
    boolean isAnswered = false;
    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling_group_voice);

        mp = MediaPlayer.create(this, R.raw.calling);
        mp.start();

        groupId = getIntent().getStringExtra("group");
        room = getIntent().getStringExtra("room");

        //Query
        FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Voice").child(room).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("ans").exists()){
                    mp.stop();
                    isAnswered = true;
                    vSettings().mChannelName = room;
                    Intent i = new Intent(CallingGroupVoiceActivity.this, VoiceCallGroupActivity.class);
                    i.putExtra(ConstantApp.ACTION_KEY_CHANNEL_NAME, room);
                    i.putExtra("group", groupId);
                    startActivity(i);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //GroupInfo
        FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                TextView name = findViewById(R.id.name);
                name.setText(snapshot.child("gName").getValue().toString());

                //DP
                CircleImageView dp = findViewById(R.id.dp);
                if (!snapshot.child("gIcon").getValue().toString().isEmpty())  Picasso.get().load(snapshot.child("gIcon").getValue().toString()).into(dp);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        findViewById(R.id.end).setOnClickListener(v -> {
            mp.stop();
            Toast.makeText(CallingGroupVoiceActivity.this, "Canceled", Toast.LENGTH_SHORT).show();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("type", "cancel");
            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Voice").child(room)
                    .updateChildren(hashMap);
            Intent intent = new Intent(CallingGroupVoiceActivity.this, GroupChatActivity.class);
            intent.putExtra("group", groupId);
            intent.putExtra("type", "");
            startActivity(intent);
            finish();
        });

        new Handler().postDelayed(() -> {
            if (!isAnswered){
                Toast.makeText(CallingGroupVoiceActivity.this, "Not answered", Toast.LENGTH_SHORT).show();
                mp.stop();
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("type", "cancel");
                FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Voice").child(room)
                        .updateChildren(hashMap);
                Intent intent = new Intent(CallingGroupVoiceActivity.this, GroupChatActivity.class);
                intent.putExtra("group", groupId);
                intent.putExtra("type", "");
                startActivity(intent);
                finish();
            }

        },30000);

    }

    @Override
    protected void initUIandEvent() {

    }

    @Override
    protected void deInitUIandEvent() {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!isAnswered){
            mp.stop();
           Toast.makeText(CallingGroupVoiceActivity.this, "Canceled", Toast.LENGTH_SHORT).show();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("type", "cancel");
            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Voice").child(room)
                    .updateChildren(hashMap);
            Intent intent = new Intent(CallingGroupVoiceActivity.this, GroupChatActivity.class);
            intent.putExtra("group", groupId);
            intent.putExtra("type", "");
            startActivity(intent);
            finish();
        }
    }
}