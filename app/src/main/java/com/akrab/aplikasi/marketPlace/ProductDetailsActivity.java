package com.akrab.aplikasi.marketPlace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.akrab.aplikasi.MediaViewActivity;
import com.akrab.aplikasi.NightMode;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.chat.ChatActivity;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProductDetailsActivity extends AppCompatActivity {

    String pId;

    NightMode sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState()){
            setTheme(R.style.DarkTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        pId = getIntent().getStringExtra("pId");

        //Id
        ImageView cover = findViewById(R.id.cover);
        TextView price = findViewById(R.id.price);
        TextView title = findViewById(R.id.title);
        TextView des = findViewById(R.id.des);
        TextView type = findViewById(R.id.type);
        TextView location = findViewById(R.id.location);
        TextView user = findViewById(R.id.user);
        CircleImageView dp  = findViewById(R.id.dp);

        findViewById(R.id.back).setOnClickListener(v -> onBackPressed());

        FirebaseDatabase.getInstance().getReference("Product").child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Picasso.get().load(Objects.requireNonNull(snapshot.child("photo").getValue()).toString()).into(cover);
                price.setText(Objects.requireNonNull(snapshot.child("price").getValue()).toString());
                title.setText(Objects.requireNonNull(snapshot.child("title").getValue()).toString());
                des.setText(Objects.requireNonNull(snapshot.child("des").getValue()).toString());
                type.setText(Objects.requireNonNull(snapshot.child("type").getValue()).toString());
                location.setText(Objects.requireNonNull(snapshot.child("location").getValue()).toString());
                FirebaseDatabase.getInstance().getReference("Users").child(Objects.requireNonNull(snapshot.child("id").getValue()).toString()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        user.setText(Objects.requireNonNull(snapshot.child("name").getValue()).toString());
                        if (!Objects.requireNonNull(snapshot.child("photo").getValue()).toString().isEmpty()){
                            Picasso.get().load(Objects.requireNonNull(snapshot.child("photo").getValue()).toString()).into(dp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                cover.setOnClickListener(v -> {
                    Intent intent = new Intent(ProductDetailsActivity.this, MediaViewActivity.class);
                    intent.putExtra("type", "image");
                    intent.putExtra("uri", Objects.requireNonNull(snapshot.child("photo").getValue()).toString());
                    startActivity(intent);
                });

                findViewById(R.id.message).setOnClickListener(v -> {
                    Intent intent = new Intent(ProductDetailsActivity.this, ChatActivity.class);
                    intent.putExtra("hisUID", Objects.requireNonNull(snapshot.child("id").getValue()).toString());
                    startActivity(intent);
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }
}