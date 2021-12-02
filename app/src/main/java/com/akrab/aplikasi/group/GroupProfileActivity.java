package com.akrab.aplikasi.group;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.akrab.aplikasi.GetTimeAgo;
import com.akrab.aplikasi.MainActivity;
import com.akrab.aplikasi.NightMode;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.adapter.AdapterGroupPost;
import com.akrab.aplikasi.calling.RingingActivity;
import com.akrab.aplikasi.groupVoiceCall.RingingGroupVoiceActivity;
import com.akrab.aplikasi.model.ModelPostGroup;
import com.akrab.aplikasi.model.ModelUser;
import com.akrab.aplikasi.notifications.Data;
import com.akrab.aplikasi.notifications.Sender;
import com.akrab.aplikasi.notifications.Token;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

@SuppressWarnings("ALL")
public class GroupProfileActivity extends AppCompatActivity {

    String groupId,myGroupRole;
    LinearLayout link_layout;

    RecyclerView post;
    AdapterGroupPost adapterPost;
    List<ModelPostGroup> modelPosts;

    //Bottom
    BottomSheetDialog more_options;
    LinearLayout members,add,announcement,mEdit,mLeave,delete,addPost,report,requestJoin;

    private RequestQueue requestQueue;
    private boolean notify = false;

    NightMode sharedPref;
    boolean sendRequest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState()){
            setTheme(R.style.DarkTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_profile);

        requestQueue = Volley.newRequestQueue(GroupProfileActivity.this);

        groupId = getIntent().getStringExtra("group");
        String type = getIntent().getStringExtra("type");

        //Post
        post = findViewById(R.id.post);
        post.setLayoutManager(new LinearLayoutManager(GroupProfileActivity.this));
        modelPosts = new ArrayList<>();
        getAllPost();

        //Back
        findViewById(R.id.back).setOnClickListener(v -> {
            if (type.equals("create")){
                Intent intent = new Intent(GroupProfileActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }else {
                onBackPressed();
            }
        });

        //PostIntent
        findViewById(R.id.create_post).setOnClickListener(v -> {
            Intent intent = new Intent(GroupProfileActivity.this, CreateGroupPostActivity.class);
            intent.putExtra("group", groupId);
            startActivity(intent);
            finish();
        });

        FirebaseDatabase.getInstance().getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                CircleImageView circleImageView = findViewById(R.id.circleImageView);
                if (!Objects.requireNonNull(snapshot.child("photo").getValue()).toString().isEmpty()){
                    Picasso.get().load(Objects.requireNonNull(snapshot.child("photo").getValue()).toString()).into(circleImageView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        findViewById(R.id.more).setOnClickListener(v -> more_options.show());
        findViewById(R.id.menu).setOnClickListener(v -> more_options.show());

        //Id
        TextView topName = findViewById(R.id.topName);
        TextView name = findViewById(R.id.name);
        TextView bio = findViewById(R.id.bio);
        TextView username = findViewById(R.id.username);
        TextView link = findViewById(R.id.link);
        TextView created = findViewById(R.id.location);
        CircleImageView dp = findViewById(R.id.dp);
        ImageView cover = findViewById(R.id.cover);
        TextView members = findViewById(R.id.members);
        TextView posts = findViewById(R.id.posts);
        link_layout = findViewById(R.id.link_layout);

        //Buttons
        Button edit = findViewById(R.id.edit);
        Button request = findViewById(R.id.request);
        Button cancel = findViewById(R.id.cancel);
        Button leave = findViewById(R.id.leave);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    if (ds.child("Participants").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).exists()){
                        for (DataSnapshot dataSnapshot1 : ds.child("Voice").getChildren()){
                            if (Objects.requireNonNull(dataSnapshot1.child("type").getValue()).toString().equals("calling")){

                                if (!Objects.requireNonNull(dataSnapshot1.child("from").getValue()).toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                    if (!dataSnapshot1.child("end").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                        if (!dataSnapshot1.child("ans").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                            Intent intent = new Intent(getApplicationContext(), RingingGroupVoiceActivity.class);
                                            intent.putExtra("room", Objects.requireNonNull(dataSnapshot1.child("room").getValue()).toString());
                                            intent.putExtra("group", Objects.requireNonNull(ds.child("groupId").getValue()).toString());
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

        //Call
        Query query = FirebaseDatabase.getInstance().getReference().child("calling").orderByChild("to").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot ds : snapshot.getChildren()){
                        if (Objects.requireNonNull(ds.child("type").getValue()).toString().equals("calling")){
                            Intent intent = new Intent(getApplicationContext(), RingingActivity.class);
                            intent.putExtra("room", Objects.requireNonNull(ds.child("room").getValue()).toString());
                            intent.putExtra("from", Objects.requireNonNull(ds.child("from").getValue()).toString());
                            intent.putExtra("call", Objects.requireNonNull(ds.child("call").getValue()).toString());
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
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                topName.setText(Objects.requireNonNull(snapshot.child("gUsername").getValue()).toString());

                name.setText(Objects.requireNonNull(snapshot.child("gName").getValue()).toString());

                bio.setText(Objects.requireNonNull(snapshot.child("gBio").getValue()).toString());

                username.setText(Objects.requireNonNull(snapshot.child("gUsername").getValue()).toString());

                link.setText(Objects.requireNonNull(snapshot.child("gLink").getValue()).toString());

                long lastTime = Long.parseLong(Objects.requireNonNull(snapshot.child("timestamp").getValue()).toString());

                //Visibility
                if (bio.getText().length()>0){
                    bio.setVisibility(View.VISIBLE);
                }

                if (link.getText().length()>0){
                    link_layout.setVisibility(View.VISIBLE);
                }else{
                    link_layout.setVisibility(View.GONE);
                }

                FirebaseDatabase.getInstance().getReference().child("Users").child(Objects.requireNonNull(snapshot.child("createdBy").getValue()).toString()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        created.setText("Created by " + Objects.requireNonNull(snapshot.child("name").getValue()).toString() + " - " + GetTimeAgo.getTimeAgo(lastTime));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                if (!Objects.requireNonNull(snapshot.child("gIcon").getValue()).toString().isEmpty()) {
                    Picasso.get().load(Objects.requireNonNull(snapshot.child("gIcon").getValue()).toString()).into(dp);
                }

                //Cover
                FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Cover").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                       if (snapshot.exists()){
                           if (!Objects.requireNonNull(snapshot.child("cover").getValue()).toString().isEmpty()){
                               Picasso.get().load(Objects.requireNonNull(snapshot.child("cover").getValue()).toString()).into(cover);
                           }
                       }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                //Participants
                FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                       if (snapshot.exists()){
                           members.setText(String.valueOf(snapshot.getChildrenCount()));
                       }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                //Posts
                FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Posts").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            posts.setText(String.valueOf(snapshot.getChildrenCount()));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                findViewById(R.id.progressBar).setVisibility(View.GONE);

                //Buttons
                FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Participants").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()){
                                    myGroupRole = ""+snapshot.child("role").getValue();
                                }else {
                                    myGroupRole = "visitor";
                                }
                                checkUserType();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });


                //EditProfile
                edit.setOnClickListener(v -> {
                    Intent intent = new Intent(GroupProfileActivity.this, EditGroupActivity.class);
                    intent.putExtra("group", groupId);
                    startActivity(intent);
                    finish();
                });

                //Leave
                leave.setOnClickListener(v -> FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Participants").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot1) {
                                if (snapshot1.exists()){
                                    snapshot1.getRef().removeValue();
                                    checkUserType();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        }));

                //Request
                request.setOnClickListener(v -> {
                     sendRequest = true;
                    if (sendRequest){
                        HashMap<String, String> hashMap1 = new HashMap<>();
                        hashMap1.put("id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Request").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(hashMap1);
                          sendRequest = false;
                        checkUserType();
                        Snackbar.make(v, "Request sent", Snackbar.LENGTH_LONG).show();
                    }
                });

                //Cancel
                cancel.setOnClickListener(v -> FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Request").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            snapshot.getRef().removeValue();
                            checkUserType();
                            Snackbar.make(v, "Request sent", Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                }));

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



    }

    private void checkUserType() {
        //Admin & Creator
        switch (myGroupRole) {
            case "admin":
            case "creator":
                findViewById(R.id.edit).setVisibility(View.VISIBLE);
                findViewById(R.id.leave).setVisibility(View.GONE);
                findViewById(R.id.request).setVisibility(View.GONE);
                findViewById(R.id.cancel).setVisibility(View.GONE);
                break;
            case "participant":
                findViewById(R.id.leave).setVisibility(View.VISIBLE);
                findViewById(R.id.edit).setVisibility(View.GONE);
                findViewById(R.id.request).setVisibility(View.GONE);
                findViewById(R.id.cancel).setVisibility(View.GONE);
                break;
            case "visitor":
                checkRequest();
                break;
        }
        options();
    }


    private void checkRequest() {

        FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Request").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    if (snapshot.hasChild(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())){
                        findViewById(R.id.cancel).setVisibility(View.VISIBLE);
                        findViewById(R.id.edit).setVisibility(View.GONE);
                        findViewById(R.id.request).setVisibility(View.GONE);
                        findViewById(R.id.leave).setVisibility(View.GONE);
                    }else {
                        findViewById(R.id.request).setVisibility(View.VISIBLE);
                        findViewById(R.id.edit).setVisibility(View.GONE);
                        findViewById(R.id.cancel).setVisibility(View.GONE);
                        findViewById(R.id.leave).setVisibility(View.GONE);
                    }
                }else {
                    findViewById(R.id.request).setVisibility(View.VISIBLE);
                    findViewById(R.id.edit).setVisibility(View.GONE);
                    findViewById(R.id.cancel).setVisibility(View.GONE);
                    findViewById(R.id.leave).setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void getAllPost() {
        FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                modelPosts.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelPostGroup modelPost = ds.getValue(ModelPostGroup.class);
                    modelPosts.add(modelPost);
                    adapterPost = new AdapterGroupPost(GroupProfileActivity.this, modelPosts);
                    post.setAdapter(adapterPost);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    if (adapterPost.getItemCount() == 0){
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        post.setVisibility(View.GONE);
                        findViewById(R.id.nothing).setVisibility(View.VISIBLE);
                    }else {
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        post.setVisibility(View.VISIBLE);
                        findViewById(R.id.nothing).setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void options() {
        if (more_options == null){
            @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.group_more, null);

            members = view.findViewById(R.id.members);
            add = view.findViewById(R.id.add);
            announcement = view.findViewById(R.id.announcement);
            mEdit = view.findViewById(R.id.edit);
            mLeave = view.findViewById(R.id.leave);
            delete = view.findViewById(R.id.delete);
            addPost = view.findViewById(R.id.addPost);
            report = view.findViewById(R.id.report);
            requestJoin = view.findViewById(R.id.requestJoin);

            //Admin & Creator
            switch (myGroupRole) {
                case "admin":
                case "creator":
                    mLeave.setVisibility(View.GONE);
                    break;
                case "participant":
                    delete.setVisibility(View.GONE);
                    announcement.setVisibility(View.GONE);
                    mEdit.setVisibility(View.GONE);
                    add.setVisibility(View.GONE);
                    requestJoin.setVisibility(View.GONE);
                    break;
                case "visitor":
                    delete.setVisibility(View.GONE);
                    announcement.setVisibility(View.GONE);
                    mEdit.setVisibility(View.GONE);
                    mLeave.setVisibility(View.GONE);
                    add.setVisibility(View.GONE);
                    addPost.setVisibility(View.GONE);
                    requestJoin.setVisibility(View.GONE);
                    break;
            }

            requestJoin.setOnClickListener(v -> {
                more_options.cancel();
                Intent intent = new Intent(GroupProfileActivity.this, JoinRequestActivity.class);
                intent.putExtra("group", groupId);
                startActivity(intent);
            });

            announcement.setOnClickListener(v -> {
                more_options.cancel();
                findViewById(R.id.extra).setVisibility(View.VISIBLE);
            });

            findViewById(R.id.imageView4).setOnClickListener(v -> {
                more_options.cancel();
                findViewById(R.id.extra).setVisibility(View.GONE);
            });

            EditText email = findViewById(R.id.email);
            findViewById(R.id.login).setOnClickListener(v -> {
                if (email.getText().toString().isEmpty()){
                    Snackbar.make(v, "Enter a message", Snackbar.LENGTH_SHORT).show();
                }else {
                    notify = true;
                    FirebaseDatabase.getInstance().getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            ModelUser user = snapshot.getValue(ModelUser.class);
                            if (notify){
                                FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot ds : snapshot.getChildren()){
                                            Toast.makeText(GroupProfileActivity.this, "Sent", Toast.LENGTH_SHORT).show();
                                            sendNotification(ds.getKey(), Objects.requireNonNull(user).getName(), email.getText().toString());
                                            addToHisNotification(ds.getKey(), email.getText().toString());
                                            email.setText("");
                                            findViewById(R.id.extra).setVisibility(View.GONE);
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

            members.setOnClickListener(v -> {
                more_options.cancel();
                Intent intent = new Intent(GroupProfileActivity.this, GroupMembersActivity.class);
                intent.putExtra("group", groupId);
                startActivity(intent);
                finish();
            });
            add.setOnClickListener(v -> {
                more_options.cancel();
                Intent intent = new Intent(GroupProfileActivity.this, AddGroupActivity.class);
                intent.putExtra("group", groupId);
                startActivity(intent);
                finish();
            });
            mEdit.setOnClickListener(v -> {
                more_options.cancel();
                Intent intent = new Intent(GroupProfileActivity.this, EditGroupActivity.class);
                intent.putExtra("group", groupId);
                startActivity(intent);
                finish();
            });
            mLeave.setOnClickListener(v -> FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Participants").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot1) {
                            if (snapshot1.exists()){
                                more_options.cancel();
                                snapshot1.getRef().removeValue();
                                checkUserType();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    }));

            mLeave.setOnClickListener(v -> {
                more_options.cancel();
                AlertDialog.Builder builder = new AlertDialog.Builder(GroupProfileActivity.this);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to leave this group ?");
                builder.setPositiveButton("Delete", (dialog, which) -> FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).child("Participants").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot1) {
                                if (snapshot1.exists()){
                                    snapshot1.getRef().removeValue();
                                    checkUserType();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        })).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.create().show();
            });


            delete.setOnClickListener(v -> {
                more_options.cancel();
                AlertDialog.Builder builder = new AlertDialog.Builder(GroupProfileActivity.this);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to delete this group ?");
                builder.setPositiveButton("Delete", (dialog, which) -> {

                    FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId).getRef().removeValue();
                    Intent intent = new Intent(GroupProfileActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();

                }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.create().show();
            });

            addPost.setOnClickListener(v -> {
                more_options.cancel();
                Intent intent = new Intent(GroupProfileActivity.this, CreateGroupPostActivity.class);
                intent.putExtra("group", groupId);
                startActivity(intent);
                finish();
            });

            report.setOnClickListener(v -> {
                more_options.cancel();
                FirebaseDatabase.getInstance().getReference().child("GroupsReport").child(groupId).child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).setValue(true);
                Snackbar.make(v, "Reported", Snackbar.LENGTH_LONG).show();
            });

            more_options = new BottomSheetDialog(this);
            more_options.setContentView(view);
        }
    }

    private void addToHisNotification(String hisUid, String message){
        String timestamp = ""+System.currentTimeMillis();
        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", "");
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", message);
        hashMap.put("sUid", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
        FirebaseDatabase.getInstance().getReference("Users").child(hisUid).child("Notifications").child(timestamp).setValue(hashMap);
        FirebaseDatabase.getInstance().getReference("Users").child(hisUid).child("Count").child(timestamp).setValue(true);
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