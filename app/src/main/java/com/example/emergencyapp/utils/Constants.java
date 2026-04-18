package com.example.emergencyapp.utils;

public class Constants {
    // Emergency Types
    public static final int EMERGENCY_KIDNAPPING    = 1;
    public static final int EMERGENCY_BEING_FOLLOWED = 2;
    public static final int EMERGENCY_AMBULANCE     = 3;
    public static final int EMERGENCY_COME_HERE     = 4;
    public static final int EMERGENCY_FULL          = 5;
    public static final int EMERGENCY_CUSTOM        = 99;

    // Firebase Paths
    public static final String PATH_USERS             = "users";
    public static final String PATH_EMERGENCIES       = "emergencies";
    public static final String PATH_MESSAGES          = "messages";
    public static final String PATH_LOCATIONS         = "locations";
    public static final String PATH_HINTS             = "hints";
    public static final String PATH_ACTIVE_EMERGENCY  = "active_emergency";
    public static final String PATH_APP_INFO          = "app_info";
    public static final String PATH_USER_GROUPS       = "user_groups";
    public static final String PATH_CUSTOM_CODES      = "custom_emergency_codes";
    public static final String PATH_LOCATION_HISTORY  = "location_history";
    public static final String PATH_REMOTE_COMMANDS   = "remote_commands";
    public static final String PATH_ACKNOWLEDGEMENTS  = "acknowledgements";  // ← YENİ

    // Preferences
    public static final String PREF_NAME           = "emergency_prefs";
    public static final String PREF_USER_ID        = "user_id";
    public static final String PREF_USER_NAME      = "user_name";
    public static final String PREF_SETUP_COMPLETE = "setup_complete";
    public static final String PREF_FCM_TOKEN      = "fcm_token";
    public static final String PREF_IS_ADMIN       = "is_admin";
    public static final String PREF_SELECTED_USERS = "selected_users";
    public static final String PREF_CONTACTS       = "contacts";

    // Notification channels
    public static final String CHANNEL_ID         = "emergency_channel";
    public static final String CHANNEL_MESSAGE_ID = "message_channel";
    public static final String CHANNEL_SERVICE_ID = "service_channel";

    // Notification IDs
    public static final int NOTIFICATION_ID          = 1001;
    public static final int LOCATION_NOTIFICATION_ID = 1002;
    public static final int MESSAGE_NOTIFICATION_ID  = 1003;

    // Timing
    public static final long LONG_PRESS_DURATION      = 5000;
    public static final long ALARM_SHORT_DURATION     = 3000;
    public static final long ALARM_LONG_DURATION      = 10000;
    public static final long DATA_RETENTION_DAYS      = 30;
    public static final long LOCATION_HISTORY_HOURS   = 24;
    public static final long LOCATION_HISTORY_INTERVAL = 30000;

    // UI
    public static final float SWIPE_THRESHOLD = 150f;

    // App Version
    public static final int    APP_VERSION_CODE = 5;
    public static final String APP_VERSION_NAME = "2.5.0";

    // Google Drive (indirme linki)
    public static final String GOOGLE_DRIVE_LINK =
            "https://drive.google.com/uc?export=download&id=DOSYA_ID_BURAYA";

    // Admin şifresi BURADAN KALDIRILDI.
    // Firebase'de admin_settings/password_hash altında bcrypt hash olarak saklanmalı.
    // AdminActivity buna göre güncellenmiştir.

    // Name colors (chat renkleri)
    public static final int[] NAME_COLORS = {
            0xFF4FC3F7, 0xFFFF8A65, 0xFF81C784, 0xFFBA68C8,
            0xFFFFD54F, 0xFFE57373, 0xFF4DD0E1, 0xFFF06292,
            0xFFAED581, 0xFF7986CB, 0xFFFFB74D, 0xFF9575CD
    };
}
