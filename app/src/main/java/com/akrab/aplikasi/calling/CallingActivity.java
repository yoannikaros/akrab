package com.akrab.aplikasi.calling;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.chat.ChatActivity;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

@SuppressWarnings("ALL")
public class CallingActivity extends AppCompatActivity {

    String his;
    String room;
    String call;
    String type;
    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

         mp = MediaPlayer.create(this, R.raw.calling);
        mp.start();

         //Room
         room = getIntent().getStringExtra("room");
         his = getIntent().getStringExtra("to");
         call  = getIntent().getStringExtra("call");

        //GetRoom
        FirebaseDatabase.getInstance().getReference().child("calling").child(room).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //Type
                 type = snapshot.child("type").getValue().toString();
                if (type.equals("ans")){
                    mp.stop();

                    if (snapshot.child("call").getValue().toString().equals("video")){
                        //go to call
                        Intent intent = new Intent(CallingActivity.this, VideoChatViewActivity.class);
                        intent.putExtra("room", room);
                        startActivity(intent);
                        finish();
                    }else {
                        //go to call
                        Intent intent = new Intent(CallingActivity.this, VoiceChatViewActivity.class);
                        intent.putExtra("room", room);
                        startActivity(intent);
                        finish();
                    }


                }
                if (type.equals("dec")){
                    mp.stop();
                   Toast.makeText(CallingActivity.this, "Declined", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CallingActivity.this, ChatActivity.class);
                    intent.putExtra("hisUID", his);
                    intent.putExtra("type", "create");
                    startActivity(intent);
                    finish();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //User
        FirebaseDatabase.getInstance().getReference().child("Users").child(his).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //Name
                TextView name = findViewById(R.id.name);
                name.setText(snapshot.child("name").getValue().toString());

                //DP
                CircleImageView dp = findViewById(R.id.dp);
                if (!snapshot.child("photo").getValue().toString().isEmpty())  Picasso.get().load(snapshot.child("photo").getValue().toString()).into(dp);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        findViewById(R.id.end).setOnClickListener(v -> {
            mp.stop();
            Toast.makeText(CallingActivity.this, "Canceled", Toast.LENGTH_SHORT).show();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("type", "cancel");
            FirebaseDatabase.getInstance().getReference().child("calling").child(room).updateChildren(hashMap);
            Intent intent = new Intent(CallingActivity.this, ChatActivity.class);
            intent.putExtra("hisUID", his);
            intent.putExtra("type", "create");
            startActivity(intent);
            finish();
        });

        new Handler().postDelayed(() -> {
            mp.stop();
            if (!type.equals("ans")){
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("type", "cancel");
                FirebaseDatabase.getInstance().getReference().child("calling").child(room).updateChildren(hashMap);
                Intent intent = new Intent(CallingActivity.this, ChatActivity.class);
                intent.putExtra("hisUID", his);
                intent.putExtra("type", "create");
                startActivity(intent);
                finish();
            }

        },30000);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!type.equals("ans")){
            mp.stop();
            Toast.makeText(CallingActivity.this, "Canceled", Toast.LENGTH_SHORT).show();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("type", "cancel");
            FirebaseDatabase.getInstance().getReference().child("calling").child(room).updateChildren(hashMap);
            Intent intent = new Intent(CallingActivity.this, ChatActivity.class);
            intent.putExtra("hisUID", his);
            intent.putExtra("type", "create");
            startActivity(intent);
            finish();
        }
    }
}