package com.example.emergencyapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.emergencyapp.FirebaseManager;
import com.example.emergencyapp.R;
import com.example.emergencyapp.models.User;
import com.example.emergencyapp.utils.PreferenceManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;

/**
 * Giriş ekranı.
 * — Google ile giriş
 * — E-posta & şifre ile giriş / kayıt
 * — Şifre sıfırlama
 *
 * Giriş başarılı olduğunda:
 *   - Firebase Realtime DB'de users/{uid} oluşturulur / güncellenir
 *   - PreferenceManager'a kullanıcı bilgileri yazılır
 *   - MainActivity'e yönlendirilir
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText etEmail, etPassword, etDisplayName;
    private Button btnEmailLogin, btnEmailRegister, btnGoogleLogin;
    private TextView tvToggleMode, tvForgotPassword;
    private LinearLayout layoutDisplayName;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private PreferenceManager prefManager;

    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        prefManager = new PreferenceManager(this);

        // Zaten giriş yapılmışsa direkt ana ekrana git
        if (mAuth.getCurrentUser() != null && prefManager.isSetupComplete()) {
            goToMain();
            return;
        }

        initViews();
        setupGoogleSignIn();  // initViews sonrası — btnGoogleLogin inflate edilmiş olur
        requestPermissions();
    }

    private void setupGoogleSignIn() {
        // Web Client ID'yi google-services.json'dan al.
        // Bu dosya projeye eklenmemişse veya SHA-1 tanımlanmamışsa Google girişi çalışmaz.
        // Firebase Console → Project Settings → Web API Key bölümünden alınır.
        String webClientId = getWebClientId();

        if (webClientId == null) {
            // google-services.json eksik ya da web_client_id tanımsız —
            // Google butonu gizlenir, sadece email girişi açık kalır.
            if (btnGoogleLogin != null) btnGoogleLogin.setVisibility(android.view.View.GONE);
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    /**
     * google-services.json plugin'i tarafından üretilen R.string.default_web_client_id'yi
     * reflection ile güvenli şekilde okur. Dosya eksikse null döner.
     */
    private String getWebClientId() {
        try {
            int resId = R.string.default_web_client_id;
            return getString(resId);
        } catch (android.content.res.Resources.NotFoundException e) {
            android.util.Log.w(TAG,
                    "default_web_client_id bulunamadı. google-services.json ekli değil " +
                            "ya da Firebase Console'da SHA-1 tanımlanmamış. " +
                            "Google girişi devre dışı bırakıldı.");
            return null;
        }
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etDisplayName = findViewById(R.id.etDisplayName);
        btnEmailLogin = findViewById(R.id.btnEmailLogin);
        btnEmailRegister = findViewById(R.id.btnEmailRegister);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        layoutDisplayName = findViewById(R.id.layoutDisplayName);
        progressBar = findViewById(R.id.progressBar);

        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        btnEmailLogin.setOnClickListener(v -> handleEmailLogin());
        btnEmailRegister.setOnClickListener(v -> handleEmailRegister());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        tvToggleMode.setOnClickListener(v -> toggleMode());

        updateModeUI();
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        updateModeUI();
    }

    private void updateModeUI() {
        if (isRegisterMode) {
            layoutDisplayName.setVisibility(View.VISIBLE);
            btnEmailLogin.setVisibility(View.GONE);
            btnEmailRegister.setVisibility(View.VISIBLE);
            tvToggleMode.setText("Zaten hesabın var mı? Giriş yap");
            tvForgotPassword.setVisibility(View.GONE);
        } else {
            layoutDisplayName.setVisibility(View.GONE);
            btnEmailLogin.setVisibility(View.VISIBLE);
            btnEmailRegister.setVisibility(View.GONE);
            tvToggleMode.setText("Hesabın yok mu? Kayıt ol");
            tvForgotPassword.setVisibility(View.VISIBLE);
        }
    }

    // ─── Google Sign-In ────────────────────────────────────────────────────────

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                showError("Google girişi başarısız: " + e.getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                FirebaseUser fbUser = mAuth.getCurrentUser();
                if (fbUser != null) {
                    saveUserToDatabase(fbUser.getUid(),
                            fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "Kullanıcı",
                            fbUser.getEmail(), "google");
                }
            } else {
                showError("Google kimlik doğrulaması başarısız");
            }
        });
    }

    // ─── Email / Şifre ────────────────────────────────────────────────────────

    private void handleEmailLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateEmailPassword(email, password)) return;

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                FirebaseUser fbUser = mAuth.getCurrentUser();
                if (fbUser != null) {
                    // Oturumu yenile — token güncelle
                    updateFcmAndProceed(fbUser.getUid(),
                            fbUser.getDisplayName() != null ? fbUser.getDisplayName() : email.split("@")[0],
                            email, "email");
                }
            } else {
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "Giriş başarısız";
                showError(msg);
            }
        });
    }

    private void handleEmailRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String displayName = etDisplayName.getText().toString().trim();

        if (TextUtils.isEmpty(displayName)) {
            etDisplayName.setError("İsminizi girin");
            return;
        }
        if (!validateEmailPassword(email, password)) return;

        showLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                FirebaseUser fbUser = mAuth.getCurrentUser();
                if (fbUser != null) {
                    // Display name güncelle
                    com.google.firebase.auth.UserProfileChangeRequest profileUpdates =
                            new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName).build();
                    fbUser.updateProfile(profileUpdates);
                    saveUserToDatabase(fbUser.getUid(), displayName, email, "email");
                }
            } else {
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "Kayıt başarısız";
                showError(msg);
            }
        });
    }

    private boolean validateEmailPassword(String email, String password) {
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Geçerli bir e-posta girin");
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError("Şifre en az 6 karakter olmalı");
            return false;
        }
        return true;
    }

    // ─── Şifre Sıfırlama ──────────────────────────────────────────────────────

    private void showForgotPasswordDialog() {
        EditText emailInput = new EditText(this);
        emailInput.setHint("E-posta adresiniz");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        String prefEmail = etEmail.getText().toString().trim();
        if (!prefEmail.isEmpty()) emailInput.setText(prefEmail);

        new AlertDialog.Builder(this)
                .setTitle("Şifre Sıfırlama")
                .setMessage("E-postanıza sıfırlama bağlantısı gönderilecek.")
                .setView(emailInput)
                .setPositiveButton("Gönder", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "E-posta girin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Sıfırlama e-postası gönderildi", Toast.LENGTH_LONG).show();
                        } else {
                            showError("E-posta gönderilemedi");
                        }
                    });
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    // ─── Kayıt & Yönlendirme ──────────────────────────────────────────────────

    private void saveUserToDatabase(String uid, String displayName, String email, String authProvider) {
        showLoading(true);
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String fcmToken = task.isSuccessful() ? task.getResult() : "";

            // DB'ye yaz / güncelle
            User user = new User(uid, displayName, email, authProvider, fcmToken);
            FirebaseManager.getInstance().getDatabase()
                    .child("users").child(uid).setValue(user)
                    .addOnCompleteListener(dbTask -> {
                        showLoading(false);
                        if (dbTask.isSuccessful()) {
                            // Prefs kaydet
                            prefManager.setUserId(uid);
                            prefManager.setUserName(displayName);
                            prefManager.setFcmToken(fcmToken);
                            prefManager.setSetupComplete(true);
                            FirebaseMessaging.getInstance().subscribeToTopic("emergency");
                            goToMain();
                        } else {
                            showError("Kullanıcı kaydedilemedi");
                        }
                    });
        });
    }

    private void updateFcmAndProceed(String uid, String displayName, String email, String authProvider) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String fcmToken = task.isSuccessful() ? task.getResult() : "";
            FirebaseManager.getInstance().updateUserFcmToken(uid, fcmToken);
            prefManager.setUserId(uid);
            prefManager.setUserName(displayName);
            prefManager.setFcmToken(fcmToken);
            prefManager.setSetupComplete(true);
            goToMain();
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // ─── İzinler ──────────────────────────────────────────────────────────────

    private void requestPermissions() {
        java.util.List<String> needed = new java.util.ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!needed.isEmpty())
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnEmailLogin.setEnabled(!show);
        btnEmailRegister.setEnabled(!show);
        btnGoogleLogin.setEnabled(!show);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}