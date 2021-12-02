package com.akrab.aplikasi.adapter;

import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
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
import com.akrab.aplikasi.model.ModelReel;
import com.akrab.aplikasi.model.ModelUser;
import com.akrab.aplikasi.notifications.Data;
import com.akrab.aplikasi.notifications.Sender;
import com.akrab.aplikasi.notifications.Token;
import com.akrab.aplikasi.profile.UserProfileActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

@SuppressWarnings("ALL")
public class AdapterReportReelView extends RecyclerView.Adapter<AdapterReportReelView.AdapterReelHolder>{

    private final List<ModelReel> modelReels;
    private RequestQueue requestQueue;
    private boolean notify = false;

    public AdapterReportReelView(List<ModelReel> modelReels) {
        this.modelReels = modelReels;
    }

    @NonNull
    @Override
    public AdapterReelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AdapterReelHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.reel_post_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterReelHolder holder, int position) {
        holder.setVideoData(modelReels.get(position));
    }


    @Override
    public int getItemCount() {
        return modelReels.size();
    }

    class AdapterReelHolder extends RecyclerView.ViewHolder{

        final ImageView video;
        final TextView views;

        public AdapterReelHolder(@NonNull View itemView) {
            super(itemView);

            video = itemView.findViewById(R.id.image);
            views = itemView.findViewById(R.id.views);

        }

        void setVideoData(ModelReel modelReel){

            //Views
            FirebaseDatabase.getInstance().getReference("ReelViews").child(modelReel.getpId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()){
                        views.setVisibility(View.VISIBLE);
                        views.setText(String.valueOf(snapshot.getChildrenCount()));
                    }else {
                        views.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            //Video
            Glide.with(itemView.getContext()).asBitmap().load(modelReel.getVideo()).thumbnail(0.1f).into(video);

            //Click

            video.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(itemView.getContext(), v, Gravity.END);


                popupMenu.getMenu().add(Menu.NONE,1,0, "Send warning to user");
                popupMenu.getMenu().add(Menu.NONE,2,0, "Remove from report");
                popupMenu.getMenu().add(Menu.NONE,3,0, "View user profile");
                popupMenu.getMenu().add(Menu.NONE,4,0, "Delete reel");

                popupMenu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();

                    if (id == 1) {
                        FirebaseDatabase.getInstance().getReference("warn").child("user").child( modelReel.getId()).setValue(true);
                        //Notification
                        String timestamp = ""+System.currentTimeMillis();
                        HashMap<Object, String> hashMap = new HashMap<>();
                        hashMap.put("pId", "");
                        hashMap.put("timestamp", timestamp);
                        hashMap.put("pUid",  modelReel.getId());
                        hashMap.put("notification", "You have got a warning by the admin");
                        hashMap.put("sUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        FirebaseDatabase.getInstance().getReference("Users").child( modelReel.getId()).child("Notifications").child(timestamp).setValue(hashMap);
                        FirebaseDatabase.getInstance().getReference("Users").child( modelReel.getId()).child("Count").child(timestamp).setValue(true);
                        notify = true;
                        FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                ModelUser user = snapshot.getValue(ModelUser.class);
                                if (notify){
                                    sendNotification( modelReel.getId(), Objects.requireNonNull(user).getName(), "You have got a warning by the admin");
                                }
                                notify = false;
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                        Snackbar.make(v, "Warning sent", Snackbar.LENGTH_LONG).show();
                    }

                    if (id == 2) {
                        FirebaseDatabase.getInstance().getReference().child("ReportReel").child(modelReel.getpId()).getRef().removeValue();
                        Snackbar.make(v, "Removed", Snackbar.LENGTH_LONG).show();
                        ViewGroup.LayoutParams params = itemView.getLayoutParams();
                        params.height = 0;
                        itemView.setLayoutParams(params);
                    }

                    if (id == 3) {
                        Intent intent = new Intent(itemView.getContext(), UserProfileActivity.class);
                        intent.putExtra("hisUID", modelReel.getId());
                        itemView.getContext().startActivity(intent);
                    }

                    if (id == 4){
                        FirebaseDatabase.getInstance().getReference("ReelViews").child(modelReel.getpId()).getRef().removeValue();
                        Snackbar.make(v, "Removed", Snackbar.LENGTH_LONG).show();
                        ViewGroup.LayoutParams params = itemView.getLayoutParams();
                        params.height = 0;
                        itemView.setLayoutParams(params);
                    }

                    return false;
                });
                popupMenu.show();
            });

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
                    Data data = new Data(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(), name + " " + message, "Warning", hisId, "profile", R.drawable.logo);
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
            }
        });
    }

}
