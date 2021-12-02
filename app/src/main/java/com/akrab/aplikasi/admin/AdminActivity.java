package com.akrab.aplikasi.admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.model.ModelUser;
import com.akrab.aplikasi.notifications.Data;
import com.akrab.aplikasi.notifications.Sender;
import com.akrab.aplikasi.notifications.Token;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

public class AdminActivity extends AppCompatActivity {

    private RequestQueue requestQueue;
    private boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        requestQueue = Volley.newRequestQueue(AdminActivity.this);

        findViewById(R.id.imageView).setOnClickListener(v -> onBackPressed());

        //Id
        TextView userNo = findViewById(R.id.userNo);
        TextView postNo = findViewById(R.id.postNo);
        TextView groupsNo = findViewById(R.id.groupsNo);
        TextView reelNo = findViewById(R.id.reelNo);
        TextView onlineNo = findViewById(R.id.onlineNo);
        TextView sellNo = findViewById(R.id.sellNo);
        TextView liveNo = findViewById(R.id.liveNo);
        TextView podcastNo = findViewById(R.id.podcastNo);
        TextView partyNo = findViewById(R.id.partyNo);
        TextView meetNO = findViewById(R.id.meetNO);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch aSwitch = findViewById(R.id.adSwitch);

        FirebaseDatabase.getInstance().getReference("Ads").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                aSwitch.setChecked(Objects.requireNonNull(snapshot.child("type").getValue()).toString().equals("on"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        aSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            HashMap<String, Object> hashMap = new HashMap<>();
            if (isChecked){
                hashMap.put("type", "on");
            }else {
                hashMap.put("type", "off");
            }
            FirebaseDatabase.getInstance().getReference("Ads").updateChildren(hashMap);
        });

        findViewById(R.id.reportedUser).setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, ReportedUserActivity.class)));
        findViewById(R.id.reportGroup).setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, ReportedGroupsActivity.class)));
        findViewById(R.id.reportedPost).setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, ReportedPostActivity.class)));
        findViewById(R.id.reportReels).setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, ReportedReelsActivity.class)));
        findViewById(R.id.verification).setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, VerificationRequestActivity.class)));
        findViewById(R.id.warnUser).setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, WarnUserActivity.class)));
        findViewById(R.id.warnGroup).setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, WarnGroupActivity.class)));

        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userNo.setText(String.valueOf(snapshot.child("Users").getChildrenCount()));
                postNo.setText(String.valueOf(snapshot.child("Posts").getChildrenCount()));
                groupsNo.setText(String.valueOf(snapshot.child("Groups").getChildrenCount()));
                reelNo.setText(String.valueOf(snapshot.child("Reels").getChildrenCount()));
                sellNo.setText(String.valueOf(snapshot.child("Product").getChildrenCount()));
                liveNo.setText(String.valueOf(snapshot.child("Live").getChildrenCount()));
                podcastNo.setText(String.valueOf(snapshot.child("Podcast").getChildrenCount()));
                partyNo.setText(String.valueOf(snapshot.child("Party").getChildrenCount()));
                meetNO.setText(String.valueOf(snapshot.child("Chats").getChildrenCount()));

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        FirebaseDatabase.getInstance().getReference("Users").orderByChild("status").equalTo("online").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                onlineNo.setText(String.valueOf(snapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //announcement
        findViewById(R.id.announcement).setOnClickListener(v -> findViewById(R.id.extra).setVisibility(View.VISIBLE));

        findViewById(R.id.imageView4).setOnClickListener(v -> findViewById(R.id.extra).setVisibility(View.GONE));

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
                            FirebaseDatabase.getInstance().getReference("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()){
                                        sendNotification(ds.getKey(), Objects.requireNonNull(user).getName(), email.getText().toString());
                                        addToHisNotification(ds.getKey(), email.getText().toString());
                                        Toast.makeText(AdminActivity.this, "Sent", Toast.LENGTH_SHORT).show();
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
                    Data data = new Data(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(), name + " : " + message, "Announcement", hisId, "profile", R.drawable.logo);
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