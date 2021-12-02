package com.akrab.aplikasi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.akrab.aplikasi.chat.ChatActivity;
import com.akrab.aplikasi.group.GroupChatActivity;
import com.akrab.aplikasi.model.ModelUser;
import com.akrab.aplikasi.notifications.Data;
import com.akrab.aplikasi.notifications.Sender;
import com.akrab.aplikasi.notifications.Token;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

public class SendStickerActivity extends AppCompatActivity {

    private RequestQueue requestQueue;
    private boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestQueue = Volley.newRequestQueue(SendStickerActivity.this);

        String type = getIntent().getStringExtra("type");
        String id = getIntent().getStringExtra("id");
        String uri = getIntent().getStringExtra("uri");

        if (type.equals("user")){

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("sender", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
            hashMap.put("receiver", id);
            hashMap.put("msg",  uri);
            hashMap.put("isSeen", false);
            hashMap.put("timestamp", ""+System.currentTimeMillis());
            hashMap.put("type", "gif");
            FirebaseDatabase.getInstance().getReference().child("Chats").push().setValue(hashMap);
            notify = true;
            FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ModelUser user = snapshot.getValue(ModelUser.class);
                    if (notify){
                        sendNotification(id, Objects.requireNonNull(user).getName(), "has sent a sticker");
                    }
                    notify = false;

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
            Intent intent = new Intent(SendStickerActivity.this, ChatActivity.class);
            intent.putExtra("hisUID", id);
            intent.putExtra("type", "create");
            startActivity(intent);
            finish();


        }else if (type.equals("group")){

            String stamp = ""+System.currentTimeMillis();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("sender", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
            hashMap.put("msg", uri);
            hashMap.put("type", "gif");
            hashMap.put("timestamp", stamp);

            FirebaseDatabase.getInstance().getReference("Groups").child(id).child("Message").child(stamp)
                    .setValue(hashMap);

            notify = true;
            FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ModelUser user = snapshot.getValue(ModelUser.class);
                    if (notify){
                        FirebaseDatabase.getInstance().getReference("Groups").child(id).child("Participants").addValueEventListener(new ValueEventListener() {
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

            Intent intent = new Intent(SendStickerActivity.this, GroupChatActivity.class);
            intent.putExtra("group", id);
            intent.putExtra("type", "");
            startActivity(intent);
            finish();

        }

    }

    private void sendNotification(final String hisId, final String name,final String message){
        DatabaseReference allToken = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allToken.orderByKey().equalTo(hisId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(), name + " : " + message, "New Message", hisId, "chat", R.drawable.logo);
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