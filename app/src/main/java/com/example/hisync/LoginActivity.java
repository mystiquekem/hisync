package com.example.bandpractice.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.bandpractice.R;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private CredentialManager credentialManager;
    private CallbackManager facebookCallbackManager;

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnSignIn, btnGoogleSignIn, btnFacebookSignIn;
    private TextView tvForgotPassword, tvSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        facebookCallbackManager = CallbackManager.Factory.create();

        // Already logged in → skip to main
        if (auth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        initViews();
        setupClickListeners();
        setupFacebookCallback();
    }

    private void initViews() {
        tilEmail          = findViewById(R.id.tilEmail);
        tilPassword       = findViewById(R.id.tilPassword);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        btnSignIn         = findViewById(R.id.btnSignIn);
        btnGoogleSignIn   = findViewById(R.id.btnGoogleSignIn);
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
        tvForgotPassword  = findViewById(R.id.tvForgotPassword);
        tvSignUp          = findViewById(R.id.tvSignUp);
    }

    // ── Click Listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(v -> {
            String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            if (validateInputs(email, password)) signInWithEmail(email, password);
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        btnFacebookSignIn.setOnClickListener(v -> signInWithFacebook());

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (email.isEmpty()) tilEmail.setError("Enter your email first");
            else sendPasswordReset(email);
        });

        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    // ── Email / Password ──────────────────────────────────────────────────────

    private boolean validateInputs(String email, String password) {
        boolean valid = true;
        tilEmail.setError(null);
        tilPassword.setError(null);

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email");
            valid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            valid = false;
        }
        return valid;
    }

    private void signInWithEmail(String email, String password) {
        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) navigateToMain();
                    else showError(task.getException() != null
                            ? task.getException().getMessage() : "Sign-in failed");
                });
    }

    private void sendPasswordReset(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        Toast.makeText(this, "Reset email sent to " + email, Toast.LENGTH_LONG).show();
                    else
                        showError(task.getException() != null
                                ? task.getException().getMessage() : "Failed to send reset email");
                });
    }

    // ── Google Sign-In (Credential Manager — replaces deprecated GoogleSignIn) ─

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)   // show all Google accounts on device
                .setServerClientId(getString(R.string.default_web_client_id)) // from google-services.json
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        setLoading(true);
        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        runOnUiThread(() -> handleGoogleCredential(response));
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showError("Google sign-in failed: " + e.getMessage());
                        });
                    }
                }
        );
    }

    private void handleGoogleCredential(GetCredentialResponse response) {
        if (response.getCredential() instanceof CustomCredential) {
            CustomCredential custom = (CustomCredential) response.getCredential();
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(custom.getType())) {
                GoogleIdTokenCredential googleCredential =
                        GoogleIdTokenCredential.createFrom(custom.getData());
                firebaseAuthWithGoogle(googleCredential.getIdToken());
                return;
            }
        }
        setLoading(false);
        showError("Unexpected credential type");
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) navigateToMain();
                    else showError(task.getException() != null
                            ? task.getException().getMessage() : "Google auth failed");
                });
    }

    // ── Facebook (SDK v17+ — no onActivityResult needed) ─────────────────────

    private void setupFacebookCallback() {
        LoginManager.getInstance().registerCallback(
                facebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        firebaseAuthWithFacebook(loginResult.getAccessToken().getToken());
                    }
                    @Override
                    public void onCancel() {
                        showError("Facebook sign-in cancelled");
                    }
                    @Override
                    public void onError(FacebookException error) {
                        showError("Facebook error: " + error.getMessage());
                    }
                }
        );
    }

    private void signInWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(
                this,
                facebookCallbackManager,
                Arrays.asList("email", "public_profile")
        );
    }

    private void firebaseAuthWithFacebook(String token) {
        setLoading(true);
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) navigateToMain();
                    else showError(task.getException() != null
                            ? task.getException().getMessage() : "Facebook auth failed");
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        btnSignIn.setEnabled(!loading);
        btnGoogleSignIn.setEnabled(!loading);
        btnFacebookSignIn.setEnabled(!loading);
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}