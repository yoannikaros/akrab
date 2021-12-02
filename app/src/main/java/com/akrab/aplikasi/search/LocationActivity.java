package com.akrab.aplikasi.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.akrab.aplikasi.NightMode;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.adapter.AdapterUsers;
import com.akrab.aplikasi.model.ModelUser;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
public class LocationActivity extends AppCompatActivity {

    //User
    AdapterUsers adapterUsers;
    List<ModelUser> userList;
    RecyclerView users_rv;

    private static final int LOCATION_PICK_CODE = 1009;
    TextView location;

    NightMode sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState()){
            setTheme(R.style.DarkTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        //Back
        findViewById(R.id.back).setOnClickListener(v -> onBackPressed());

        //Search
        location = findViewById(R.id.location);
        findViewById(R.id.editText).setOnClickListener(v -> {
            Intent intent = new PlaceAutocomplete.IntentBuilder()
                    .accessToken("sk.eyJ1IjoiYWtyYWJuZXQiLCJhIjoiY2t1dGphbHh2MWM5dTJvcXYwOWZ3YWF6byJ9.N5DpusKKH6hYBgX8CVhp9w")
                    .placeOptions(PlaceOptions.builder()
                            .backgroundColor(Color.parseColor("#ffffff"))
                            .build(PlaceOptions.MODE_CARDS))
                    .build(this);
            startActivityForResult(intent, LOCATION_PICK_CODE);
        });

        //User
        users_rv = findViewById(R.id.users);
        users_rv.setLayoutManager(new LinearLayoutManager(LocationActivity.this));
        userList = new ArrayList<>();

        if (location.getText().toString().isEmpty()){
            findViewById(R.id.set).setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.GONE);
        }else {
            getAllUsers();
            findViewById(R.id.set).setVisibility(View.GONE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }

    }

    private void getAllUsers() {

        FirebaseDatabase.getInstance().getReference("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelUser modelUser = ds.getValue(ModelUser.class);
                    if (!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(modelUser.getId())) {
                        if (modelUser.getLocation().toLowerCase().contains(location.getText().toString().toLowerCase())) {
                            userList.add(modelUser);
                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                        }
                    }
                    adapterUsers = new AdapterUsers(LocationActivity.this, userList);
                    users_rv.setAdapter(adapterUsers);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    if (adapterUsers.getItemCount() == 0){
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        findViewById(R.id.users).setVisibility(View.GONE);
                        findViewById(R.id.nothing).setVisibility(View.VISIBLE);
                    }else {
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        findViewById(R.id.users).setVisibility(View.VISIBLE);
                        findViewById(R.id.nothing).setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Location
        if (resultCode == Activity.RESULT_OK && requestCode == LOCATION_PICK_CODE && data != null) {
            CarmenFeature feature = PlaceAutocomplete.getPlace(data);
            location.setText(feature.text());

            if (location.getText().toString().isEmpty()){
                findViewById(R.id.set).setVisibility(View.VISIBLE);
                findViewById(R.id.progressBar).setVisibility(View.GONE);
            }else {
                getAllUsers();
                findViewById(R.id.set).setVisibility(View.GONE);
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            }

        }
    }
}