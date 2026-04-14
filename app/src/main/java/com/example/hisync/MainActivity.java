package com.example.hisync;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Populate header
        TextView tvAvatar   = findViewById(R.id.tvAvatar);
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);

        String email = user.getEmail() != null ? user.getEmail() : "User";
        String displayName = user.getDisplayName();

        if (displayName != null && !displayName.isEmpty()) {
            tvUserName.setText(displayName);
            tvAvatar.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
        } else {
            tvUserName.setText(email.split("@")[0]);
            tvAvatar.setText(String.valueOf(email.charAt(0)).toUpperCase());
        }
        tvUserEmail.setText(email);

        // Sign out
        MaterialButton btnSignOut = findViewById(R.id.btnSignOut);
        btnSignOut.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Bottom navigation (stub — expand per screen later)
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            // Add fragment transactions here as you build each screen
            return true;
        });
    }
}