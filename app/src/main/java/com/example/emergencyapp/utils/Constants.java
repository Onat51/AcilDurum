package com.example.emergencyapp.utils;

public class Constants {
    // Emergency Types
    public static final int EMERGENCY_KIDNAPPING = 1;
    public static final int EMERGENCY_BEING_FOLLOWED = 2;
    public static final int EMERGENCY_AMBULANCE = 3;
    public static final int EMERGENCY_COME_HERE = 4;
    public static final int EMERGENCY_FULL = 5;

    // Firebase Paths
    public static final String PATH_USERS = "users";
    public static final String PATH_EMERGENCIES = "emergencies";
    public static final String PATH_MESSAGES = "messages";
    public static final String PATH_LOCATIONS = "locations";
    public static final String PATH_HINTS = "hints";
    public static final String PATH_ACTIVE_EMERGENCY = "active_emergency";
    public static final String PATH_APP_INFO = "app_info";
    public static final String PATH_USER_GROUPS = "user_groups";

    // Preferences
    public static final String PREF_NAME = "emergency_prefs";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_SETUP_COMPLETE = "setup_complete";
    public static final String PREF_FCM_TOKEN = "fcm_token";
    public static final String PREF_IS_ADMIN = "is_admin";
    public static final String PREF_SELECTED_USERS = "selected_users";

    // Notification
    public static final String CHANNEL_ID = "emergency_channel";
    public static final String CHANNEL_MESSAGE_ID = "message_channel";
    public static final String CHANNEL_SERVICE_ID = "service_channel";
    public static final int NOTIFICATION_ID = 1001;
    public static final int LOCATION_NOTIFICATION_ID = 1002;
    public static final int MESSAGE_NOTIFICATION_ID = 1003;

    // Timing
    public static final long LONG_PRESS_DURATION = 5000;
    public static final long ALARM_SHORT_DURATION = 3000;
    public static final long ALARM_LONG_DURATION = 10000;
    public static final long DATA_RETENTION_DAYS = 30;

    // Swipe threshold
    public static final float SWIPE_THRESHOLD = 150f;

    // Yönetici Ayarları
    public static final String ADMIN_PASSWORD = "Admin.onat?12141925!";

    // Uygulama Versiyon
    public static final int APP_VERSION_CODE = 3;
    public static final String APP_VERSION_NAME = "1.5.0";

    // Google Drive Linki
    public static final String GOOGLE_DRIVE_LINK = "https://drive.google.com/uc?export=download&id=DOSYA_ID_BURAYA";

    // İsim renkleri
    public static final int[] NAME_COLORS = {
            0xFF4FC3F7, // Açık Mavi
            0xFFFF8A65, // Turuncu
            0xFF81C784, // Yeşil
            0xFFBA68C8, // Mor
            0xFFFFD54F, // Sarı
            0xFFE57373, // Kırmızı
            0xFF4DD0E1, // Cyan
            0xFFF06292, // Pembe
            0xFFAED581, // Açık Yeşil
            0xFF7986CB, // İndigo
            0xFFFFB74D, // Amber
            0xFF9575CD  // Deep Purple
    };
}