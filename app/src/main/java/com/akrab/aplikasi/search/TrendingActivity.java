package com.akrab.aplikasi.search;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.akrab.aplikasi.NightMode;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.adapter.AdapterPost;
import com.akrab.aplikasi.calling.RingingActivity;
import com.akrab.aplikasi.groupVoiceCall.RingingGroupVoiceActivity;
import com.akrab.aplikasi.model.ModelPost;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TrendingActivity extends AppCompatActivity {

    String type = "";

    //Post
    AdapterPost adapterPost;
    List<ModelPost> modelPosts;
    RecyclerView post;

    //Post
    AdapterPost getAdapterPost;
    List<ModelPost> modelPostList;
    RecyclerView postView;

    private static final int TOTAL_ITEM_EACH_LOAD = 8;
    private int currentPage = 1;
    Button more;
    long initial;

    NightMode sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState()){
            setTheme(R.style.DarkTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trending);

        //Back
        findViewById(R.id.back).setOnClickListener(v -> onBackPressed());

        //Search
        findViewById(R.id.search).setOnClickListener(v1 -> startActivity(new Intent(TrendingActivity.this, SearchActivity.class)));

        more = findViewById(R.id.more);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    if (ds.child("Participants").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).exists()){
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
        Query query = FirebaseDatabase.getInstance().getReference().child("calling").orderByChild("to").equalTo(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot ds : snapshot.getChildren()){
                        if (Objects.requireNonNull(ds.child("type").getValue()).toString().equals("calling")){
                            Intent intent = new Intent(TrendingActivity.this, RingingActivity.class);
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

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        reference.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int i = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    i++;
                }
                initial = i;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Post
        post = findViewById(R.id.post);
        post.setLayoutManager(new LinearLayoutManager(TrendingActivity.this));
        modelPosts = new ArrayList<>();
        trending();
        findViewById(R.id.more).setOnClickListener(v -> loadMoreData());

        //Post
        postView = findViewById(R.id.postView);
        postView.setLayoutManager(new LinearLayoutManager(TrendingActivity.this));
        modelPostList = new ArrayList<>();

        //Type
        findViewById(R.id.music).setOnClickListener(v -> {
            type = "music";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.sports).setOnClickListener(v -> {
            type = "sports";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.memes).setOnClickListener(v -> {
            type = "memes";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.vines).setOnClickListener(v -> {
            type = "vines";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.tv).setOnClickListener(v -> {
            type = "movie";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.animals).setOnClickListener(v -> {
            type = "animals";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.diy).setOnClickListener(v -> {
            type = "diy";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.beauty).setOnClickListener(v -> {
            type = "beauty";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.art).setOnClickListener(v -> {
            type = "art";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.food).setOnClickListener(v -> {
            type = "food";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.style).setOnClickListener(v -> {
            type = "style";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });

        findViewById(R.id.decor).setOnClickListener(v -> {
            type = "decor";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.politics).setOnClickListener(v -> {
            type = "politics";
            post.setVisibility(View.GONE);
            postView.setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getPostsType();
        });
        findViewById(R.id.trending).setOnClickListener(v -> {
            type = "";
            post.setVisibility(View.VISIBLE);
            postView.setVisibility(View.GONE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            trending();
        });


    }

    private void loadMoreData() {
        currentPage++;
        trending();
    }

    private void trending() {

        FirebaseDatabase.getInstance().getReference("Posts").limitToFirst(currentPage*TOTAL_ITEM_EACH_LOAD)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        modelPosts.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            ModelPost modelPost = ds.getValue(ModelPost.class);
                            modelPosts.add(modelPost);
                            adapterPost = new AdapterPost(TrendingActivity.this, modelPosts);
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
                                if(adapterPost.getItemCount() == initial){
                                    more.setVisibility(View.GONE);
                                    currentPage--;
                                }else {
                                    more.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void getPostsType(){
        FirebaseDatabase.getInstance().getReference("Posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                modelPostList.clear();
               for (DataSnapshot ds : snapshot.getChildren()){
                   if (Objects.requireNonNull(ds.child("text").getValue()).toString().toLowerCase().contains(type)){
                       ModelPost modelPost = ds.getValue(ModelPost.class);
                       modelPostList.add(modelPost);
                       getAdapterPost = new AdapterPost(TrendingActivity.this, modelPostList);
                       postView.setAdapter(getAdapterPost);
                       findViewById(R.id.progressBar).setVisibility(View.GONE);
                       if (getAdapterPost.getItemCount() == 0){
                           findViewById(R.id.progressBar).setVisibility(View.GONE);
                           postView.setVisibility(View.GONE);
                           findViewById(R.id.nothing).setVisibility(View.VISIBLE);
                       }else {
                           findViewById(R.id.progressBar).setVisibility(View.GONE);
                           postView.setVisibility(View.VISIBLE);
                           findViewById(R.id.nothing).setVisibility(View.GONE);
                       }
                   }else {
                       findViewById(R.id.progressBar).setVisibility(View.GONE);
                       postView.setVisibility(View.GONE);
                       findViewById(R.id.nothing).setVisibility(View.VISIBLE);
                   }
               }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

}