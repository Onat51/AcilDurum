package com.example.emergencyapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.emergencyapp.services.EmergencyListenerService;
import com.example.emergencyapp.services.LocationService;
import com.example.emergencyapp.utils.PreferenceManager;

/**
 * Cihaz açıldığında veya uygulama güncellendiğinde servisleri yeniden başlatır.
 * AndroidManifest'te RECEIVE_BOOT_COMPLETED izni gereklidir.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (!action.equals(Intent.ACTION_BOOT_COMPLETED)
                && !action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)
                && !action.equals("android.intent.action.QUICKBOOT_POWERON")) {
            return;
        }

        PreferenceManager prefManager = new PreferenceManager(context);
        if (!prefManager.isSetupComplete()) {
            Log.d(TAG, "Setup tamamlanmamış, servisler başlatılmadı");
            return;
        }

        Log.d(TAG, "Boot tamamlandı, servisler başlatılıyor: " + action);

        startService(context, EmergencyListenerService.class);
        startService(context, LocationService.class);
    }

    private void startService(Context context, Class<?> serviceClass) {
        try {
            Intent intent = new Intent(context, serviceClass);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent);
            } else {
                context.startService(intent);
            }
            Log.d(TAG, serviceClass.getSimpleName() + " başlatıldı");
        } catch (Exception e) {
            Log.e(TAG, "Servis başlatılamadı: " + serviceClass.getSimpleName(), e);
        }
    }
}
