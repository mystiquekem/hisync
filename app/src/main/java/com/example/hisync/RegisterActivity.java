package com.example.hisync;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnRegister;
    private TextView tvSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        tilEmail    = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvSignIn    = findViewById(R.id.tvSignIn);

        btnRegister.setOnClickListener(v -> {
            String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            if (validate(email, password)) register(email, password);
        });

        tvSignIn.setOnClickListener(v -> finish());
    }

    private boolean validate(String email, String password) {
        boolean valid = true;
        tilEmail.setError(null);
        tilPassword.setError(null);
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email"); valid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Minimum 6 characters"); valid = false;
        }
        return valid;
    }

    private void register(String email, String password) {
        btnRegister.setEnabled(false);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();
                        // Write user doc so schedule can look up names
                        Map<String, Object> userDoc = new HashMap<>();
                        userDoc.put("displayName", email.split("@")[0]);
                        userDoc.put("email", email);
                        userDoc.put("role", "member"); // default role

                        db.collection("users").document(uid).set(userDoc)
                                .addOnCompleteListener(dbTask -> {
                                    startActivity(new Intent(this, MainActivity.class));
                                    finishAffinity();
                                });
                    } else {
                        tilEmail.setError(task.getException() != null
                                ? task.getException().getMessage() : "Registration failed");
                    }
                });
    }
}