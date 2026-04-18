package com.example.emergencyapp.workers;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.emergencyapp.services.EmergencyListenerService;
import com.example.emergencyapp.services.LocationService;
import com.example.emergencyapp.utils.PreferenceManager;

/**
 * WorkManager tarafından periyodik olarak çalıştırılır.
 * EmergencyListenerService ve LocationService'in hayatta olup olmadığını kontrol eder;
 * ölmüşse yeniden başlatır. Bu, üretici OEM optimizasyonlarına (Xiaomi, Huawei, Samsung)
 * karşı son savunma hattıdır.
 */
public class ServiceWatchdogWorker extends Worker {
    private static final String TAG = "ServiceWatchdog";

    public ServiceWatchdogWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        PreferenceManager prefManager = new PreferenceManager(context);

        if (!prefManager.isSetupComplete()) {
            return Result.success();
        }

        Log.d(TAG, "Watchdog çalışıyor — servis durumu kontrol ediliyor");

        if (!EmergencyListenerService.isRunning()) {
            Log.w(TAG, "EmergencyListenerService ölü — yeniden başlatılıyor");
            startService(context, EmergencyListenerService.class);
        }

        if (!LocationService.isRunning()) {
            Log.w(TAG, "LocationService ölü — yeniden başlatılıyor");
            startService(context, LocationService.class);
        }

        return Result.success();
    }

    private void startService(Context context, Class<?> serviceClass) {
        try {
            Intent intent = new Intent(context, serviceClass);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Servis başlatılamadı: " + serviceClass.getSimpleName(), e);
        }
    }
}
