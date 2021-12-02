package com.akrab.aplikasi.reel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
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
import com.akrab.aplikasi.adapter.AdapterCommentReel;
import com.akrab.aplikasi.model.ModelCommentReel;
import com.akrab.aplikasi.model.ModelUser;
import com.akrab.aplikasi.notifications.Data;
import com.akrab.aplikasi.notifications.Sender;
import com.akrab.aplikasi.notifications.Token;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

@SuppressWarnings("ALL")
public class ReelCommentActivity extends AppCompatActivity {

    String position;
    String reelId;
    String type;

    //Comments
    List<ModelCommentReel> commentsList;
    AdapterCommentReel adapterComments;
    RecyclerView recyclerView;

    private RequestQueue requestQueue;
    private boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_party_chat);

        requestQueue = Volley.newRequestQueue(ReelCommentActivity.this);

        recyclerView = findViewById(R.id.chat_rv);

        position = getIntent().getStringExtra("item");

        reelId = getIntent().getStringExtra("id");

        type = getIntent().getStringExtra("type");

        findViewById(R.id.imageView).setOnClickListener(v -> {
            if (type.equals("view")){

                Intent intent3 = new Intent(ReelCommentActivity.this, ViewReelActivity.class);
                intent3.putExtra("id", reelId);
                startActivity(intent3);
                finish();

            }else {
                Intent intent = new Intent(ReelCommentActivity.this, ReelActivity.class);
                intent.putExtra("position", position);
                intent.putExtra("type", type);
                startActivity(intent);
                finish();
            }
        });

        //Send
        EditText editText = findViewById(R.id.editText);
        findViewById(R.id.message_send).setOnClickListener(v -> {

            if (editText.getText().toString().isEmpty()){
                Snackbar.make(v, "Type a comment", Snackbar.LENGTH_SHORT).show();
            }else {
                String timeStamp = ""+System.currentTimeMillis();
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("cId",  timeStamp);
                hashMap.put("comment", editText.getText().toString());
                hashMap.put("timestamp",  timeStamp);
                hashMap.put("id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                hashMap.put("pId", reelId);
                FirebaseDatabase.getInstance().getReference("Reels").child(reelId).child("Comment").child(timeStamp).setValue(hashMap);
                Snackbar.make(v, "Comment sent", Snackbar.LENGTH_SHORT).show();
                addToHisNotification(getIntent().getStringExtra("his"), "Commented on your reel");
                notify = true;
                FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ModelUser user = snapshot.getValue(ModelUser.class);
                        if (notify){
                            sendNotification(getIntent().getStringExtra("his"), Objects.requireNonNull(user).getName(), "commented on your post");
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
                editText.setText("");
            }

        });

        commentsList = new ArrayList<>();
        loadComments();

    }

    private void loadComments() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        commentsList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference("Reels").child(reelId).child("Comment").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentsList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelCommentReel modelComments = ds.getValue(ModelCommentReel.class);
                    commentsList.add(modelComments);
                    adapterComments = new AdapterCommentReel(getApplicationContext(), commentsList);
                    recyclerView.setAdapter(adapterComments);
                    adapterComments.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (type.equals("view")){

            Intent intent3 = new Intent(ReelCommentActivity.this, ViewReelActivity.class);
            intent3.putExtra("id", reelId);
            startActivity(intent3);
            finish();

        }else {
            Intent intent = new Intent(ReelCommentActivity.this, ReelActivity.class);
            intent.putExtra("position", position);
            intent.putExtra("type", type);
            startActivity(intent);
            finish();
        }
    }

    private void addToHisNotification(String hisUid, String message){
        String timestamp = ""+System.currentTimeMillis();
        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", reelId);
        hashMap.put("type", "reel");
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", message);
        hashMap.put("sUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
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
                    Data data = new Data(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(), name + " " + message, "New Message", hisId, "profile", R.drawable.logo);
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