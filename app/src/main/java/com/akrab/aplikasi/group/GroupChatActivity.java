package com.akrab.aplikasi.group;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.devlomi.record_view.OnRecordListener;
import com.devlomi.record_view.RecordButton;
import com.devlomi.record_view.RecordView;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.iceteck.silicompressorr.SiliCompressor;
import com.akrab.aplikasi.MainActivity;
import com.akrab.aplikasi.NightMode;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.Stickers;
import com.akrab.aplikasi.adapter.AdapterGroupActiveUsers;
import com.akrab.aplikasi.adapter.AdapterGroupChat;
import com.akrab.aplikasi.calling.RingingActivity;
import com.akrab.aplikasi.faceFilters.FaceFilters;
import com.akrab.aplikasi.groupVideoCall.CallingGroupVideoActivity;
import com.akrab.aplikasi.groupVoiceCall.CallingGroupVoiceActivity;
import com.akrab.aplikasi.groupVoiceCall.RingingGroupVoiceActivity;
import com.akrab.aplikasi.meeting.MeetingActivity;
import com.akrab.aplikasi.model.ModelGroupChat;
import com.akrab.aplikasi.model.ModelUser;
import com.akrab.aplikasi.notifications.Data;
import com.akrab.aplikasi.notifications.Sender;
import com.akrab.aplikasi.notifications.Token;
import com.akrab.aplikasi.watchParty.StartWatchPartyActivity;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

@SuppressWarnings("ALL")
public class GroupChatActivity extends AppCompatActivity implements View.OnClickListener{

    //String
    String mName;
    private static String groupId;
    public static String getGroupId() {
        return groupId;
    }
    public GroupChatActivity(){

    }

    boolean isShown = false;
    public static final String fileName = "recorded.3gp";
    final String file = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + fileName;

    //Bottom
    BottomSheetDialog post_more;
    LinearLayout image,video,audio,watch_party,camera,document,location,recorder,meeting,stickers;

    //Permission
    private static final int IMAGE_PICK_CODE = 1000;
    private static final int VIDEO_PICK_CODE = 1002;
    private static final int AUDIO_PICK_CODE = 1003;
    private static final int DOC_PICK_CODE = 1004;
    private static final int PERMISSION_CODE = 1001;
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final int PERMISSION_REQ_CODE = 1 << 3;
    private final String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //ID
    LinearLayout main;
    MediaRecorder mediaRecorder;
    private ArrayList<ModelGroupChat> groupChats;
    private AdapterGroupChat adapterGroupChat;
    RecyclerView recyclerView;
    String type;
    private ArrayList<ModelUser> userArrayList;
    private AdapterGroupActiveUsers adapterParticipants;
    RecyclerView onlineList;

    private RequestQueue requestQueue;
    private boolean notify = false;

    NightMode sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState()){
            setTheme(R.style.DarkTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        requestQueue = Volley.newRequestQueue(GroupChatActivity.this);

        //GetID
        groupId = getIntent().getStringExtra("group");
         type = getIntent().getStringExtra("type");

        //Back
        findViewById(R.id.back).setOnClickListener(v -> {
            if (type.equals("create")){
                Intent intent = new Intent(GroupChatActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }else {
                onBackPressed();
            }
        });

        //Id
        main = findViewById(R.id.main);
        RecordView recordView = findViewById(R.id.record_view);
        RecordButton recordButton = findViewById(R.id.record_button);
        recyclerView = findViewById(R.id.chatList);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        onlineList = findViewById(R.id.onlineList);
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(getApplicationContext(),
                LinearLayoutManager.HORIZONTAL, false);
        onlineList.setLayoutManager(linearLayoutManager2);
        onlineList.setHasFixedSize(true);


        if (isShown){
            check();
        }

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
                                            Intent intent = new Intent(getApplicationContext(), RingingGroupVoiceActivity.class);
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

        //IMPORTANT
        recordButton.setRecordView(recordView);

        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                //Start Recording..
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                mediaRecorder.setOutputFile(file);

                startRecording();
            }

            @Override
            public void onCancel() {
                //On Swipe To Cancel
            }

            @Override
            public void onFinish(long recordTime) {
                //Stop Recording..
                stopRecording();
            }

            @Override
            public void onLessThanSecond() {
                //When the record time is less than One Second
                Snackbar.make(main, "Recording must be greater than one Second", Snackbar.LENGTH_LONG).show();
            }
        });

        //UserCall
        Query query = FirebaseDatabase.getInstance().getReference().child("calling").orderByChild("to").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot ds : snapshot.getChildren()){
                        if (ds.child("type").getValue().toString().equals("calling")){
                            Intent intent = new Intent(GroupChatActivity.this, RingingActivity.class);
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

        //GroupInfo
        FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                TextView username = findViewById(R.id.username);
                username.setText(snapshot.child("gUsername").getValue().toString());

                TextView name = findViewById(R.id.name);
                name.setText(snapshot.child("gName").getValue().toString());

                //DP
                CircleImageView dp = findViewById(R.id.dp);
                if (!snapshot.child("gIcon").getValue().toString().isEmpty())  Picasso.get().load(snapshot.child("gIcon").getValue().toString()).into(dp);

                //Click
                dp.setOnClickListener(v -> {
                    Intent intent = new Intent(GroupChatActivity.this, GroupProfileActivity.class);
                    intent.putExtra("group", groupId);
                    intent.putExtra("type", "");
                    startActivity(intent);
                });

                name.setOnClickListener(v -> {
                    Intent intent = new Intent(GroupChatActivity.this, GroupProfileActivity.class);
                    intent.putExtra("group", groupId);
                    intent.putExtra("type", "");
                    startActivity(intent);
                });

                username.setOnClickListener(v -> {
                    Intent intent = new Intent(GroupChatActivity.this, GroupProfileActivity.class);
                    intent.putExtra("group", groupId);
                    intent.putExtra("type", "");
                    startActivity(intent);
                });


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //UserInfo
        FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //Name
                mName = snapshot.child("name").getValue().toString();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //VideoCall
        findViewById(R.id.video_call).setOnClickListener(v -> {

            String room = ""+System.currentTimeMillis();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("from", FirebaseAuth.getInstance().getCurrentUser().getUid());
            hashMap.put("to", groupId);
            hashMap.put("room", room);
            hashMap.put("type", "calling");
            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Video").child(room)
                    .setValue(hashMap);

            String stamp = ""+System.currentTimeMillis();
            HashMap<String, Object> hashMap1 = new HashMap<>();
            hashMap1.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
            hashMap1.put("msg", mName + " has video called");
            hashMap1.put("type", "video_call");
            hashMap1.put("timestamp", stamp);

            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Message").child(stamp)
                    .setValue(hashMap1);

            Intent intent = new Intent(GroupChatActivity.this, CallingGroupVideoActivity.class);
            intent.putExtra("room", room);
            intent.putExtra("group", groupId);
            startActivity(intent);

        });

        //VoiceCall
        findViewById(R.id.audio_call).setOnClickListener(v -> {

            String room = ""+System.currentTimeMillis();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("from", FirebaseAuth.getInstance().getCurrentUser().getUid());
            hashMap.put("to", groupId);
            hashMap.put("room", room);
            hashMap.put("type", "calling");
            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Voice").child(room)
                    .setValue(hashMap);

            String stamp = ""+System.currentTimeMillis();
            HashMap<String, Object> hashMap1 = new HashMap<>();
            hashMap1.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
            hashMap1.put("msg", mName + " has voice called");
            hashMap1.put("type", "voice_call");
            hashMap1.put("timestamp", stamp);

            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Message").child(stamp)
                    .setValue(hashMap1);

            Intent intent = new Intent(GroupChatActivity.this, CallingGroupVoiceActivity.class);
            intent.putExtra("room", room);
            intent.putExtra("group", groupId);
            startActivity(intent);

        });


        //EditText
        EditText editText = findViewById(R.id.editText);

        //Typing
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                HashMap<String, Object> hashMap = new HashMap<>();
                if (count == 0){
                    hashMap.put("typingTo", "noOne");
                }else {
                    hashMap.put("typingTo", groupId);
                }
                FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(hashMap);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //Send
        findViewById(R.id.message_send).setOnClickListener(v -> {
            if (editText.getText().toString().isEmpty()){
                Snackbar.make(v,"Type a message", Snackbar.LENGTH_LONG).show();
            }else {

                String stamp = ""+System.currentTimeMillis();

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                hashMap.put("msg", editText.getText().toString());
                hashMap.put("type", "text");
                hashMap.put("timestamp", stamp);

                FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Message").child(stamp)
                        .setValue(hashMap);
                notify = true;
                FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ModelUser user = snapshot.getValue(ModelUser.class);
                        if (notify){
                            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()){
                                        sendNotification(ds.getKey(), Objects.requireNonNull(user).getName(), editText.getText().toString());
                                        editText.setText("");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

            }
        });

        //Bottom
        addAttachment();
        loadGroupMessage();
        loadMembers();
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        findViewById(R.id.add).setOnClickListener(v -> post_more.show());
    }

    private void loadGroupMessage() {
        groupChats = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(getGroupId()).child("Message")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        groupChats.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            ModelGroupChat modelGroupChat = ds.getValue(ModelGroupChat.class);
                            groupChats.add(modelGroupChat);
                        }
                        adapterGroupChat = new AdapterGroupChat(GroupChatActivity.this, groupChats);
                        recyclerView.setAdapter(adapterGroupChat);
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void startRecording() {
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        try {
            mediaRecorder.stop();
            mediaRecorder.release();

        } catch(RuntimeException stopException) {
            // handle cleanup here
        }
        sendRec();
    }

    private void sendRec() {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        Toast.makeText(this, "Please wait, Sending...", Toast.LENGTH_SHORT).show();
        Uri audio_uri = Uri.fromFile(new File(file));

        StorageReference storageReference = FirebaseStorage.getInstance().getReference("chat_audio/" + ""+System.currentTimeMillis());
        storageReference.putFile(audio_uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            Uri downloadUri = uriTask.getResult();
            if (uriTask.isSuccessful()){

                String stamp = ""+System.currentTimeMillis();

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                hashMap.put("msg",downloadUri.toString());
                hashMap.put("type", "audio");
                hashMap.put("timestamp", stamp);

                FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Message").child(stamp)
                        .setValue(hashMap);

                findViewById(R.id.progressBar).setVisibility(View.GONE);
                Snackbar.make(main, "Sent", Snackbar.LENGTH_LONG).show();
                notify = true;
                FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ModelUser user = snapshot.getValue(ModelUser.class);
                        if (notify){
                            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()){
                                        sendNotification(ds.getKey(), Objects.requireNonNull(user).getName(),"sent a voice note");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }
        });

    }

    private void addAttachment() {
        if (post_more == null){
            @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.chat_more, null);
            image = view.findViewById(R.id.image);
            image.setOnClickListener(this);
            video = view.findViewById(R.id.video);
            video.setOnClickListener(this);
            audio = view.findViewById(R.id.audio);
            audio.setOnClickListener(this);
            document = view.findViewById(R.id.document);
            document.setOnClickListener(this);
            location = view.findViewById(R.id.location);
            location.setOnClickListener(this);
            watch_party = view.findViewById(R.id.watch_party);
            watch_party.setOnClickListener(this);
            camera = view.findViewById(R.id.camera);
            camera.setOnClickListener(this);
            recorder = view.findViewById(R.id.recorder);
            recorder.setOnClickListener(this);
             meeting = view.findViewById(R.id.meeting);
            meeting.setOnClickListener(this);

            stickers = view.findViewById(R.id.stickers);
            stickers.setOnClickListener(this);

            post_more = new BottomSheetDialog(this);
            post_more.setContentView(view);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", ""+System.currentTimeMillis());
        FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", "online");
        FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(hashMap);
    }


    @Override
    protected void onStart() {
        super.onStart();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", "online");
        FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(hashMap);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", ""+System.currentTimeMillis());
        FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(hashMap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(main, "Storage permission allowed", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(main, "Storage permission is required", Snackbar.LENGTH_LONG).show();
            }
            if (requestCode == PERMISSION_REQ_CODE) {
                boolean granted = true;
                for (int result : grantResults) {
                    granted = (result == PackageManager.PERMISSION_GRANTED);
                    if (!granted) break;
                }

                if (granted) {
                } else {
                    Snackbar.make(main, "Permission is required", Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.image:

                post_more.cancel();

                //Check Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(permissions, PERMISSION_CODE);
                    }
                    else {
                        pickImage();
                    }
                }
                else {
                    pickImage();
                }

                break;
            case R.id.video:

                post_more.cancel();

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

                break;
            case R.id.audio:

                post_more.cancel();

                //Check Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(permissions, PERMISSION_CODE);
                    }
                    else {
                        pickAudio();
                    }
                }
                else {
                    pickAudio();
                }
                break;
            case  R.id.location:

                post_more.cancel();

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    findViewById(R.id.place_autocomplete_search_button).setVisibility(View.GONE);
                    findViewById(R.id.place_autocomplete_search_input).setVisibility(View.GONE);
                    startActivityForResult(builder.build(GroupChatActivity.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                break;

            case  R.id.document:

                post_more.cancel();

                //Check Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(permissions, PERMISSION_CODE);
                    }
                    else {
                        pickDoc();
                    }
                }
                else {
                    pickDoc();
                }

                break;

            case  R.id.recorder:

                post_more.cancel();

                check();

                if (isShown){
                    findViewById(R.id.mediaRecord).setVisibility(View.GONE);
                    isShown = false;
                }else {
                    findViewById(R.id.mediaRecord).setVisibility(View.VISIBLE);
                    isShown = true;
                }

                break;

            case  R.id.stickers:

                post_more.cancel();

                Intent s = new Intent(GroupChatActivity.this, Stickers.class);
                s.putExtra("type", "group");
                s.putExtra("id", groupId);
                startActivity(s);

                break;

            case R.id.meeting:
                post_more.cancel();
                startActivity(new Intent(GroupChatActivity.this, MeetingActivity.class));
                break;
            case R.id.watch_party:
                post_more.cancel();

                Query q = FirebaseDatabase.getInstance().getReference().child("Party").orderByChild("from").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
                q.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            if (ds.child("from").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                ds.getRef().removeValue();
                                startActivity(new Intent(GroupChatActivity.this, StartWatchPartyActivity.class));
                            }else {
                                startActivity(new Intent(GroupChatActivity.this, StartWatchPartyActivity.class));
                            }
                        }
                        startActivity(new Intent(GroupChatActivity.this, StartWatchPartyActivity.class));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                break;

            case R.id.camera:
                post_more.cancel();
                startActivity(new Intent(GroupChatActivity.this, FaceFilters.class));
                break;

        }
    }

    private void check() {
        boolean granted = true;
        for (String per : PERMISSIONS) {
            if (!permissionGranted(per)) {
                granted = false;
                break;
            }
        }

        if (granted) {

        } else {
            requestPermissions();
        }
    }

    private boolean permissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(
                this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_CODE);
    }


    @SuppressLint("ObsoleteSdkInt")
    private void pickDoc() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("*/*");
        startActivityForResult(intent, DOC_PICK_CODE);
    }

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("audio/*");
        startActivityForResult(intent, AUDIO_PICK_CODE);
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, VIDEO_PICK_CODE);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE && data != null){
            Uri dp_uri = Objects.requireNonNull(data).getData();
            sendImage(dp_uri);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            Snackbar.make(main, "Please wait, Sending...", Snackbar.LENGTH_LONG).show();

        }
        if(resultCode == RESULT_OK && requestCode == VIDEO_PICK_CODE && data != null){
            Uri video_uri = Objects.requireNonNull(data).getData();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(getApplicationContext(), video_uri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMilli = Long.parseLong(time);
            retriever.release();

            if (timeInMilli > 50000){
                Snackbar.make(main, "Video must be of 5 minutes or less", Snackbar.LENGTH_LONG).show();
            }else {
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                Snackbar.make(main, "Please wait, Sending...", Snackbar.LENGTH_LONG).show();
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                new CompressVideo().execute("false",video_uri.toString(),file.getPath());
            }
        }
        if (resultCode == RESULT_OK && requestCode == AUDIO_PICK_CODE && data != null){
            Uri audio_uri = Objects.requireNonNull(data).getData();
            sendAudio(audio_uri);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            Snackbar.make(main, "Please wait, Sending...", Snackbar.LENGTH_LONG).show();
        }
        if (resultCode == RESULT_OK && requestCode == DOC_PICK_CODE && data != null){
            Uri doc_uri = Objects.requireNonNull(data).getData();
            sendDoc(doc_uri);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            Snackbar.make(main, "Please wait, Sending...", Snackbar.LENGTH_LONG).show();
        }
        if (resultCode == RESULT_OK && requestCode == PLACE_PICKER_REQUEST && data != null){
            Place place = PlacePicker.getPlace(data, this);
            String latitude = String.valueOf(place.getLatLng().latitude);
            String longitude = String.valueOf(place.getLatLng().longitude);

            //Message
            String stamp = ""+System.currentTimeMillis();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
            hashMap.put("msg", stamp);
            hashMap.put("type", "location");
            hashMap.put("timestamp", stamp);

            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Message").child(stamp)
                    .setValue(hashMap);

            //Location
            HashMap<String, Object> hashMap2 = new HashMap<>();
            hashMap2.put("latitude", latitude);
            hashMap2.put("longitude", longitude);
            hashMap2.put("id", stamp);
            FirebaseDatabase.getInstance().getReference().child("Location").child(stamp).setValue(hashMap2);

            Snackbar.make(main, "Sent", Snackbar.LENGTH_LONG).show();
            notify = true;
            FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ModelUser user = snapshot.getValue(ModelUser.class);
                    if (notify){
                        FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()){
                                    sendNotification(ds.getKey(), Objects.requireNonNull(user).getName(),"sent location");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                    notify = false;
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });


        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendDoc(Uri doc_uri) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("chat_doc/" + ""+System.currentTimeMillis());
        storageReference.putFile(doc_uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            Uri downloadUri = uriTask.getResult();
            if (uriTask.isSuccessful()){
                String stamp = ""+System.currentTimeMillis();
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                hashMap.put("msg", downloadUri.toString());
                hashMap.put("type", "doc");
                hashMap.put("timestamp", stamp);

                FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Message").child(stamp)
                        .setValue(hashMap);

                findViewById(R.id.progressBar).setVisibility(View.GONE);
                Snackbar.make(main, "Sent", Snackbar.LENGTH_LONG).show();
                notify = true;
                FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ModelUser user = snapshot.getValue(ModelUser.class);
                        if (notify){
                            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()){
                                        sendNotification(ds.getKey(), Objects.requireNonNull(user).getName(),"sent a document");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

            }
        });
    }

    private void sendAudio(Uri audio_uri) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("chat_audio/" + ""+System.currentTimeMillis());
        storageReference.putFile(audio_uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            Uri downloadUri = uriTask.getResult();
            if (uriTask.isSuccessful()){

                String stamp = ""+System.currentTimeMillis();
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                hashMap.put("msg", downloadUri.toString());
                hashMap.put("type", "audio");
                hashMap.put("timestamp", stamp);

                FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Message").child(stamp)
                        .setValue(hashMap);

                findViewById(R.id.progressBar).setVisibility(View.GONE);
                Snackbar.make(main, "Sent", Snackbar.LENGTH_LONG).show();
                notify = true;
                FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ModelUser user = snapshot.getValue(ModelUser.class);
                        if (notify){
                            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()){
                                        sendNotification(ds.getKey(), Objects.requireNonNull(user).getName(),"sent a audio");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

            }
        });
    }


    @SuppressLint("StaticFieldLeak")
    private class CompressVideo extends AsyncTask<String,String,String> {

        @Override
        protected String doInBackground(String... strings) {
            String videoPath = null;
            try {
                Uri mUri = Uri.parse(strings[1]);
                videoPath = SiliCompressor.with(GroupChatActivity.this)
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

    private void sendVideo(Uri videoUri){
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("chat_video/" + ""+System.currentTimeMillis());
        storageReference.putFile(videoUri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            Uri downloadUri = uriTask.getResult();
            if (uriTask.isSuccessful()){

                String stamp = ""+System.currentTimeMillis();
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                hashMap.put("msg", downloadUri.toString());
                hashMap.put("type", "video");
                hashMap.put("timestamp", stamp);

                FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Message").child(stamp)
                        .setValue(hashMap);

                findViewById(R.id.progressBar).setVisibility(View.GONE);
                Snackbar.make(main, "Sent", Snackbar.LENGTH_LONG).show();
                notify = true;
                FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ModelUser user = snapshot.getValue(ModelUser.class);
                        if (notify){
                            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()){
                                        sendNotification(ds.getKey(), Objects.requireNonNull(user).getName(),"sent a video");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

            }
        });
    }


    private void sendImage(Uri dp_uri) {

        StorageReference storageReference = FirebaseStorage.getInstance().getReference("chat_photo/" + ""+System.currentTimeMillis());
        storageReference.putFile(dp_uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            Uri downloadUri = uriTask.getResult();
            if (uriTask.isSuccessful()){

                String stamp = ""+System.currentTimeMillis();
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                hashMap.put("msg", downloadUri.toString());
                hashMap.put("type", "image");
                hashMap.put("timestamp", stamp);

                FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Message").child(stamp)
                        .setValue(hashMap);

                findViewById(R.id.progressBar).setVisibility(View.GONE);
                Snackbar.make(main, "Sent", Snackbar.LENGTH_LONG).show();
                notify = true;
                FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ModelUser user = snapshot.getValue(ModelUser.class);
                        if (notify){
                            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()){
                                        sendNotification(ds.getKey(), Objects.requireNonNull(user).getName(),"sent a image");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

            }
        });

    }

    private void loadMembers() {
        userArrayList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    String id = ""+ds.child("id").getValue();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                    ref.orderByChild("id").equalTo(id).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            userArrayList.clear();
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                ModelUser modelUser = ds.getValue(ModelUser.class);
                                if (ds.child("status").getValue().toString().equals("online")){
                                    if (!ds.child("id").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                        userArrayList.add(modelUser);
                                    }
                                }
                            }
                            adapterParticipants = new AdapterGroupActiveUsers(GroupChatActivity.this, userArrayList);
                            onlineList.setAdapter(adapterParticipants);
                            adapterParticipants.notifyDataSetChanged();

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


    private void sendNotification(final String hisId, final String name,final String message){
        DatabaseReference allToken = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allToken.orderByKey().equalTo(hisId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(), name + " : " + message, "New Message", hisId, "profile", R.drawable.logo);
                    assert token != null;
                    Sender sender = new Sender(data, token.getToken());
                    try {
                        JSONObject jsonObject = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", jsonObject, response -> Timber.d("onResponse%s", response.toString()), error -> Timber.d("onResponse%s", error.toString())){
                            @Override
                            public Map<String, String> getHeaders() {
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type", "application/json");
                                headers.put("Authorization", "key=AAAAOFTLx4w:APA91bGfw35kL7m7F16UnlK6qtFjdJdp2MIPXIRBBbE5VpbjDXvc1F_rF80YerGzpDZotiHQRlGN_sn-KVcBvPHHvDQFl36YoHjLKKAGPUPXPKWvBx-LeZNGdjTZ0_liO14q-Hq1ylXq");
                                return headers;
                            }
                        };
                        requestQueue.add(jsonObjectRequest);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}