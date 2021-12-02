package com.akrab.aplikasi.menu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.akrab.aplikasi.marketPlace.MarketPlaceActivity;
import com.akrab.aplikasi.NightMode;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.admin.AdminActivity;
import com.akrab.aplikasi.faceFilters.FaceFilters;
import com.akrab.aplikasi.group.GroupFragment;
import com.akrab.aplikasi.live.StartLiveActivity;
import com.akrab.aplikasi.meeting.MeetingActivity;
import com.akrab.aplikasi.notifications.NotificationScreen;
import com.akrab.aplikasi.profile.EditProfileActivity;
import com.akrab.aplikasi.reel.ReelActivity;
import com.akrab.aplikasi.search.LocationActivity;
import com.akrab.aplikasi.search.SearchActivity;
import com.akrab.aplikasi.send.ImageEditingActivity;
import com.akrab.aplikasi.send.VideoEditingActivity;
import com.akrab.aplikasi.watchParty.StartWatchPartyActivity;
import com.akrab.aplikasi.welcome.IntroLast;

import java.util.HashMap;
import java.util.Objects;

@SuppressWarnings("ALL")
public class MenuActivity extends AppCompatActivity {

    NightMode sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState()){
            setTheme(R.style.DarkTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        findViewById(R.id.imageView).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.market).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, MarketPlaceActivity.class)));

        findViewById(R.id.reel).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, ReelActivity.class)));

        findViewById(R.id.group).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, GroupFragment.class)));

        findViewById(R.id.party).setOnClickListener(v -> {

            Query q = FirebaseDatabase.getInstance().getReference().child("Party").orderByChild("from").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
            q.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()){
                        if (ds.child("from").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                            ds.getRef().removeValue();
                            startActivity(new Intent(MenuActivity.this, StartWatchPartyActivity.class));
                        }else {
                            startActivity(new Intent(MenuActivity.this, StartWatchPartyActivity.class));
                        }
                    }
                    startActivity(new Intent(MenuActivity.this, StartWatchPartyActivity.class));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        });

        findViewById(R.id.meeting).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, MeetingActivity.class)));

        findViewById(R.id.podcast).setOnClickListener(v -> {

            String room = String.valueOf(System.currentTimeMillis());
            Query query = FirebaseDatabase.getInstance().getReference().child("Podcast").orderByChild("userid").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()){
                        snapshot.getRef().removeValue();
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("room", room);
                        hashMap.put("userid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Podcast");
                        reference.child(room).setValue(hashMap).addOnCompleteListener(task -> {
                            Intent i = new Intent(MenuActivity.this, StartLiveActivity.class);
                            i.putExtra("name", room);
                            i.putExtra("type", "host");
                            startActivity(i);
                        });
                    }else {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("room", room);
                        hashMap.put("userid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Podcast");
                        reference.child(room).setValue(hashMap).addOnCompleteListener(task -> {
                            Intent i = new Intent(MenuActivity.this, StartLiveActivity.class);
                            i.putExtra("name", room);
                            i.putExtra("type", "host");
                            startActivity(i);
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        });

        findViewById(R.id.saved).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, SavedActivity.class)));

        findViewById(R.id.live).setOnClickListener(v -> {

            String room = String.valueOf(System.currentTimeMillis());
            Query query = FirebaseDatabase.getInstance().getReference().child("Live").orderByChild("userid").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()){
                        snapshot.getRef().removeValue();
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("room", room);
                        hashMap.put("userid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Live");
                        reference.child(room).setValue(hashMap).addOnCompleteListener(task -> {
                            Intent i = new Intent(MenuActivity.this, StartLiveActivity.class);
                            i.putExtra("name", room);
                            i.putExtra("type", "host");
                            startActivity(i);
                        });
                    }else {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("room", room);
                        hashMap.put("userid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Live");
                        reference.child(room).setValue(hashMap).addOnCompleteListener(task -> {
                            Intent i = new Intent(MenuActivity.this, StartLiveActivity.class);
                            i.putExtra("name", room);
                            i.putExtra("type", "host");
                            startActivity(i);
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        });

        findViewById(R.id.notification).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, NotificationScreen.class)));

        findViewById(R.id.near).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, LocationActivity.class)));

        findViewById(R.id.camera).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, FaceFilters.class)));

        findViewById(R.id.camera).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, FaceFilters.class)));

        findViewById(R.id.editImage).setOnClickListener(v -> pickImage());

        findViewById(R.id.editVideo).setOnClickListener(v -> pickVideo());

        findViewById(R.id.search).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, SearchActivity.class)));

        findViewById(R.id.search).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, SearchActivity.class)));

        findViewById(R.id.verify).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, VerificationActivity.class)));

        findViewById(R.id.logOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MenuActivity.this, IntroLast.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.email).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, EditEmailActivity.class)));

        findViewById(R.id.pass).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, EditPasswordActivity.class)));

        findViewById(R.id.profile).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, EditProfileActivity.class)));

        findViewById(R.id.phone).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, EditNumberActivity.class)));

        findViewById(R.id.policy).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, PrivacyActivity.class)));


        FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("phone").getValue().toString().isEmpty()){
                    findViewById(R.id.email).setVisibility(View.VISIBLE);
                    findViewById(R.id.pass).setVisibility(View.VISIBLE);
                    findViewById(R.id.phone).setVisibility(View.GONE);
                }else {
                    findViewById(R.id.email).setVisibility(View.GONE);
                    findViewById(R.id.pass).setVisibility(View.GONE);
                    findViewById(R.id.phone).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch aSwitch = findViewById(R.id.nightSwitch);
        if (sharedPref.loadNightModeState()){
            aSwitch.setChecked(true);
        }
        aSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPref.setNightModeState(isChecked);
            restartApp();
        });

        findViewById(R.id.invite).setOnClickListener(v -> {
            String shareBody = "Akrab - Sosial tanpa batas" + " Download now on play store \nhttps://play.google.com/store/apps/details?id=com.akrab.aplikasi";
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/*");
            intent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
            intent.putExtra(Intent.EXTRA_TEXT,shareBody);
            startActivity(Intent.createChooser(intent, "Share Via"));
        });

        //Admin
        FirebaseDatabase.getInstance().getReference("Admin").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    findViewById(R.id.admin).setVisibility(View.VISIBLE);
                }else {
                    findViewById(R.id.admin).setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        findViewById(R.id.admin).setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, AdminActivity.class)));

    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, 1);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK && requestCode == 1 && data != null){
            Uri video_uri = Objects.requireNonNull(data).getData();
            Intent intent = new Intent(MenuActivity.this, VideoEditingActivity.class);
            intent.putExtra("uri", video_uri.toString());
            startActivity(intent);
        }
        if (resultCode == RESULT_OK && requestCode == 2 && data != null){
            Uri dp_uri = Objects.requireNonNull(data).getData();
            Intent intent = new Intent(MenuActivity.this, ImageEditingActivity.class);
            intent.putExtra("uri", dp_uri.toString());
            startActivity(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void restartApp() {
        Intent i = getBaseContext().getPackageManager().
                getLaunchIntentForPackage(getBaseContext().getPackageName());
        Objects.requireNonNull(i).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

}