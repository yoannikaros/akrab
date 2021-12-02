package com.akrab.aplikasi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.iceteck.silicompressorr.SiliCompressor;
import com.akrab.aplikasi.marketPlace.PostProductActivity;
import com.akrab.aplikasi.calling.RingingActivity;
import com.akrab.aplikasi.faceFilters.FaceFilters;
import com.akrab.aplikasi.fragment.ChatFragment;
import com.akrab.aplikasi.fragment.HomeFragment;
import com.akrab.aplikasi.fragment.ProfileFragment;
import com.akrab.aplikasi.groupVideoCall.RingingGroupVideoActivity;
import com.akrab.aplikasi.groupVoiceCall.RingingGroupVoiceActivity;
import com.akrab.aplikasi.live.StartLiveActivity;
import com.akrab.aplikasi.meeting.MeetingActivity;
import com.akrab.aplikasi.notifications.Token;
import com.akrab.aplikasi.podcast.StartPodcastActivity;
import com.akrab.aplikasi.post.CreatePostActivity;
import com.akrab.aplikasi.reel.ReelActivity;
import com.akrab.aplikasi.reel.VideoEditActivity;
import com.akrab.aplikasi.story.AddStoryActivity;
import com.akrab.aplikasi.watchParty.StartWatchPartyActivity;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Objects;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    BottomNavigationView bottomNavigationView;
    Fragment selectedFragment = null;

    //Bottom
    BottomSheetDialog more;
    LinearLayout post,reel,party,camera,meeting,live,podcast,sell,stories;

    //Permission
    private static final int VIDEO_PICK_CODE = 1002;
    private static final int PERMISSION_CODE = 1001;

    NightMode sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState()){
            setTheme(R.style.DarkTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navigationSelected);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new HomeFragment()).commit();

        getGroupCall();
        getGroupCallVideo();
        addPost();
        deleteStory();

        updateToken(FirebaseInstanceId.getInstance().getToken());

        //Call
        Query query = FirebaseDatabase.getInstance().getReference().child("calling").orderByChild("to").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot ds : snapshot.getChildren()){
                        if (ds.child("type").getValue().toString().equals("calling")){
                            Intent intent = new Intent(MainActivity.this, RingingActivity.class);
                            intent.putExtra("room", ds.child("room").getValue().toString());
                            intent.putExtra("from", ds.child("from").getValue().toString());
                            intent.putExtra("call", ds.child("call").getValue().toString());
                            startActivity(intent);
                            finish();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void deleteStory() {
        FirebaseDatabase.getInstance().getReference("Story").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long timecurrent = System.currentTimeMillis();
                for (DataSnapshot ds : snapshot.getChildren()){
                    FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("High").child(ds.child("storyid").getValue().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) {

                                FirebaseDatabase.getInstance().getReference().child("Chats").orderByChild("msg").equalTo(ds.child("storyid").getValue().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (!snapshot.exists()){
                                            if (timecurrent > Long.parseLong(ds.child("timeend").getValue().toString())){
                                                FirebaseStorage.getInstance().getReferenceFromUrl(ds.child("imageUri").getValue().toString()).delete();
                                                ds.getRef().removeValue();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getGroupCallVideo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    if (ds.child("Participants").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).exists()){
                        for (DataSnapshot dataSnapshot1 : ds.child("Video").getChildren()){
                            if (dataSnapshot1.child("type").getValue().toString().equals("calling")){

                                if (!dataSnapshot1.child("from").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                    if (!dataSnapshot1.child("end").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                        if (!dataSnapshot1.child("ans").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                            Intent intent = new Intent(MainActivity.this, RingingGroupVideoActivity.class);
                                            intent.putExtra("room", dataSnapshot1.child("room").getValue().toString());
                                            intent.putExtra("group", ds.child("groupId").getValue().toString());
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @SuppressLint("NonConstantResourceId")
    private final BottomNavigationView.OnNavigationItemSelectedListener navigationSelected =
            item -> {
                switch (item.getItemId()){
                    case R.id.nav_home:
                        selectedFragment = new HomeFragment();

                        break;
                    case R.id.nav_add:
                        more.show();
                        break;
                    case R.id.nav_reels:

                        Intent intent = new Intent(MainActivity.this, ReelActivity.class);
                        startActivity(intent);

                        break;
                    case R.id.nav_chat:
                        selectedFragment = new ChatFragment();
                        break;
                    case R.id.nav_user:
                        selectedFragment = new ProfileFragment();
                        break;
                }
                if (selectedFragment != null){
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
                }
                return true;
            };

    private void getGroupCall() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    if (ds.child("Participants").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).exists()){
                        for (DataSnapshot dataSnapshot1 : ds.child("Voice").getChildren()){
                            if (dataSnapshot1.child("type").getValue().toString().equals("calling")){
                                if (!dataSnapshot1.child("from").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                    if (!dataSnapshot1.child("end").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                        if (!dataSnapshot1.child("ans").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                            Intent intent = new Intent(MainActivity.this, RingingGroupVoiceActivity.class);
                                            intent.putExtra("room", dataSnapshot1.child("room").getValue().toString());
                                            intent.putExtra("group", ds.child("groupId").getValue().toString());
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addPost() {
        if (more == null){
            @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.add_bottom, null);
            post = view.findViewById(R.id.post);
            post.setOnClickListener(this);
            reel = view.findViewById(R.id.reel);
            reel.setOnClickListener(this);
            party = view.findViewById(R.id.party);
            party.setOnClickListener(this);
            meeting = view.findViewById(R.id.meeting);
            meeting.setOnClickListener(this);
            live = view.findViewById(R.id.live);
            live.setOnClickListener(this);
            podcast = view.findViewById(R.id.podcast);
            podcast.setOnClickListener(this);
            sell = view.findViewById(R.id.sell);
            sell.setOnClickListener(this);
            stories = view.findViewById(R.id.stories);
            stories.setOnClickListener(this);
            camera = view.findViewById(R.id.camera);
            camera.setOnClickListener(this);
            more = new BottomSheetDialog(this);
            more.setContentView(view);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.post:
                more.cancel();
                startActivity(new Intent(MainActivity.this, CreatePostActivity.class));
                break;
            case R.id.reel:
                more.cancel();
               selectReel();
                break;
            case R.id.party:
                more.cancel();
                startActivity(new Intent(MainActivity.this, StartWatchPartyActivity.class));
                break;
            case R.id.meeting:
                more.cancel();
                startActivity(new Intent(MainActivity.this, MeetingActivity.class));
                break;
            case R.id.live:
                more.cancel();
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
                                Intent i = new Intent(MainActivity.this, StartLiveActivity.class);
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
                                Intent i = new Intent(MainActivity.this, StartLiveActivity.class);
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
                break;
            case R.id.podcast:
                more.cancel();
                createPod();
                break;
            case R.id.sell:
                more.cancel();
                startActivity(new Intent(MainActivity.this, PostProductActivity.class));
                break;
            case R.id.stories:
                more.cancel();
                startActivity(new Intent(MainActivity.this, AddStoryActivity.class));
                break;
            case R.id.camera:
                more.cancel();
                startActivity(new Intent(MainActivity.this, FaceFilters.class));
                break;
        }
    }

    private void createPod() {
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
                        Intent i = new Intent(MainActivity.this, StartPodcastActivity.class);
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
                        Intent i = new Intent(MainActivity.this, StartPodcastActivity.class);
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
    }

    private void selectReel() {

        //Check Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED){
                String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(permissions, PERMISSION_CODE);
            }
            else {
                pickVideo();
            }
        }
        else {
            pickVideo();
        }

    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, VIDEO_PICK_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(R.id.main), "Storage permission allowed", Snackbar.LENGTH_LONG).show();
                pickVideo();
            } else {
                Snackbar.make(findViewById(R.id.main), "Storage permission is required", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK && requestCode == VIDEO_PICK_CODE && data != null){
            Uri video_uri = Objects.requireNonNull(data).getData();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(getApplicationContext(), video_uri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMilli = Long.parseLong(time);
            retriever.release();

            if (timeInMilli > 60000){
                Snackbar.make(findViewById(R.id.main), "Video must be of 1 minutes or less", Snackbar.LENGTH_LONG).show();
            }else {
                Snackbar.make(findViewById(R.id.main), "Please wait...", Snackbar.LENGTH_LONG).show();
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                new CompressVideo().execute("false",video_uri.toString(),file.getPath());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("StaticFieldLeak")
    private class CompressVideo extends AsyncTask<String,String,String> {

        @Override
        protected String doInBackground(String... strings) {
            String videoPath = null;
            try {
                Uri mUri = Uri.parse(strings[1]);
                videoPath = SiliCompressor.with(MainActivity.this)
                        .compressVideo(mUri,strings[2]);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return videoPath;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            File file = new File(s);
            Uri videoUri = Uri.fromFile(file);
            sendVideo(videoUri);
        }
    }

    private void sendVideo(Uri videoUri) {
        Intent intent = new Intent(MainActivity.this, VideoEditActivity.class);
        intent.putExtra("uri", videoUri.toString());
        startActivity(intent);
    }

    private void updateToken(String token){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(mToken);
    }


}