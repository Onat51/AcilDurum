package com.example.emergencyapp.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UpdateChecker {
    private static final String TAG = "UpdateChecker";

    public interface UpdateCheckListener {
        void onCheckComplete(boolean updateAvailable);
    }

    public static void checkForUpdate(Activity activity, UpdateCheckListener listener) {
        DatabaseReference appInfoRef = FirebaseDatabase.getInstance()
                .getReference(Constants.PATH_APP_INFO);

        appInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer latestVersion = snapshot.child("versionCode").getValue(Integer.class);
                    String latestVersionName = snapshot.child("versionName").getValue(String.class);
                    String updateMessage = snapshot.child("updateMessage").getValue(String.class);
                    Boolean forceUpdate = snapshot.child("forceUpdate").getValue(Boolean.class);
                    String downloadLink = snapshot.child("downloadLink").getValue(String.class);

                    if (latestVersion != null && latestVersion > Constants.APP_VERSION_CODE) {
                        Log.d(TAG, "Güncelleme mevcut: " + latestVersionName);
                        showUpdateDialog(activity, latestVersionName, updateMessage,
                                forceUpdate != null && forceUpdate,
                                downloadLink != null ? downloadLink : Constants.GOOGLE_DRIVE_LINK);

                        if (listener != null) {
                            listener.onCheckComplete(true);
                        }
                    } else {
                        Log.d(TAG, "Uygulama güncel");
                        if (listener != null) {
                            listener.onCheckComplete(false);
                        }
                    }
                } else {
                    Log.d(TAG, "Versiyon bilgisi bulunamadı");
                    if (listener != null) {
                        listener.onCheckComplete(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Güncelleme kontrolü hatası: " + error.getMessage());
                if (listener != null) {
                    listener.onCheckComplete(false);
                }
            }
        });
    }

    private static void showUpdateDialog(Activity activity, String versionName,
                                         String message, boolean forceUpdate, String downloadLink) {
        if (activity == null || activity.isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("🔄 Güncelleme Mevcut!");

        String dialogMessage = "Yeni versiyon: " + versionName + "\n\n";
        if (message != null && !message.isEmpty()) {
            dialogMessage += message;
        } else {
            dialogMessage += "Yeni bir güncelleme mevcut. Lütfen uygulamayı güncelleyin.";
        }

        builder.setMessage(dialogMessage);
        builder.setCancelable(!forceUpdate);

        builder.setPositiveButton("Güncelle", (dialog, which) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink));
                activity.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Link açılamadı: " + e.getMessage());
            }
        });

        if (!forceUpdate) {
            builder.setNegativeButton("Sonra", (dialog, which) -> dialog.dismiss());
        }

        try {
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Dialog gösterilemedi: " + e.getMessage());
        }
    }

    // Yönetici için versiyon güncelleme
    public static void updateVersionInfo(String versionName, int versionCode,
                                         String updateMessage, boolean forceUpdate,
                                         String downloadLink, OnCompleteListener listener) {
        DatabaseReference appInfoRef = FirebaseDatabase.getInstance()
                .getReference(Constants.PATH_APP_INFO);

        appInfoRef.child("versionCode").setValue(versionCode);
        appInfoRef.child("versionName").setValue(versionName);
        appInfoRef.child("updateMessage").setValue(updateMessage);
        appInfoRef.child("forceUpdate").setValue(forceUpdate);
        appInfoRef.child("downloadLink").setValue(downloadLink);
        appInfoRef.child("updatedAt").setValue(System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}