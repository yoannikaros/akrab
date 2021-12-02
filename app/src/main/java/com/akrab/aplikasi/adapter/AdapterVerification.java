package com.akrab.aplikasi.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.akrab.aplikasi.R;
import com.akrab.aplikasi.model.ModelVerification;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterVerification extends RecyclerView.Adapter<AdapterVerification.MyHolder>{

    final Context context;
    final List<ModelVerification> userList;

    public AdapterVerification(Context context, List<ModelVerification> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       View view = LayoutInflater.from(context).inflate(R.layout.verification_view, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {

        FirebaseDatabase.getInstance().getReference("Users").child(userList.get(position).getuID()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                holder.name.setText(Objects.requireNonNull(snapshot.child("name").getValue()).toString());
                holder.username.setText(Objects.requireNonNull(snapshot.child("username").getValue()).toString());
                if (!Objects.requireNonNull(snapshot.child("photo").getValue()).toString().isEmpty()){
                    Picasso.get().load(Objects.requireNonNull(snapshot.child("photo").getValue()).toString()).into(holder.dp);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.fName.setText(userList.get(position).getName());
        holder.fUsername.setText(userList.get(position).getUsername());
        holder.known.setText(userList.get(position).getKnown());
        holder.govt.setText(userList.get(position).getLink());

        holder.reject.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("Verification").child(userList.get(position).getvId()).getRef().removeValue();
            Snackbar.make(v, "Rejected", Snackbar.LENGTH_LONG).show();
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.height = 0;
            holder.itemView.setLayoutParams(params);
        });

        holder.accept.setOnClickListener(v -> {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("verified", "yes");
            FirebaseDatabase.getInstance().getReference("Users").child(userList.get(position).getuID()).updateChildren(hashMap);
            FirebaseDatabase.getInstance().getReference().child("Verification").child(userList.get(position).getvId()).getRef().removeValue();
            Snackbar.make(v, "Accepted", Snackbar.LENGTH_LONG).show();
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.height = 0;
            holder.itemView.setLayoutParams(params);
        });

    }


    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder{

        final CircleImageView dp;
        final TextView name;
        final TextView username;
        final TextView fName;
        final TextView fUsername;
        final TextView known;
        final TextView govt;
        final Button accept;
        final Button reject;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            dp = itemView.findViewById(R.id.dp);
            name = itemView.findViewById(R.id.name);
            username = itemView.findViewById(R.id.username);
            fName = itemView.findViewById(R.id.fName);
            fUsername = itemView.findViewById(R.id.fUsername);
            known = itemView.findViewById(R.id.known);
            govt = itemView.findViewById(R.id.govt);
            accept = itemView.findViewById(R.id.accept);
            reject = itemView.findViewById(R.id.reject);

        }

    }
}
