package com.akrab.aplikasi.menu;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.akrab.aplikasi.NightMode;
import com.akrab.aplikasi.R;

import java.util.Objects;

public class EditEmailActivity extends AppCompatActivity {

    NightMode sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState()){
            setTheme(R.style.DarkTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_email);

        findViewById(R.id.imageView).setOnClickListener(v -> onBackPressed());

        EditText email = findViewById(R.id.email);
        EditText pass = findViewById(R.id.pass);

        findViewById(R.id.login).setOnClickListener(v -> {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            if (pass.getText().toString().isEmpty()){  findViewById(R.id.progressBar).setVisibility(View.GONE);
                Snackbar.make(v, "Enter your password", Snackbar.LENGTH_SHORT).show();
            }else if (email.getText().toString().isEmpty()){  findViewById(R.id.progressBar).setVisibility(View.GONE);
                Snackbar.make(v, "Enter your new email", Snackbar.LENGTH_SHORT).show();
            }else {
                AuthCredential authCredential = EmailAuthProvider.getCredential(Objects.requireNonNull(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail()), pass.getText().toString());
                FirebaseAuth.getInstance().getCurrentUser().reauthenticate(authCredential).addOnSuccessListener(aVoid -> {
                    FirebaseAuth.getInstance().getCurrentUser().updateEmail(email.getText().toString());
                    Snackbar.make(v, "Email Changed", Snackbar.LENGTH_SHORT).show();
                    pass.setText("");
                    email.setText("");
                }).addOnFailureListener(e -> Snackbar.make(v, Objects.requireNonNull(e.getMessage()), Snackbar.LENGTH_SHORT).show());
            }
        });

    }
}