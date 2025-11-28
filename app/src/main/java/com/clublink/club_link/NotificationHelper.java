package com.clublink.club_link;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class NotificationHelper {

    private static final String PREFS_NAME = "club_link_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    public interface PermissionCallback {
        void onGranted();
        void onDenied();
    }

    public static void initNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            FCMService.CHANNEL_ID,
                            "Announcements",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );
            channel.setDescription("Notifications for new announcements");

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void subscribeToAnnouncements() {
        FirebaseMessaging.getInstance()
                .subscribeToTopic("announcements")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        // You can log success/failure if you want
                    }
                });
    }

    public static void cacheFcmToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    public static String getCachedFcmToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * For Android 13+ POST_NOTIFICATIONS permission.
     * You’ll call this from an Activity.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static ActivityResultLauncher<String> setupPermissionRequestLauncher(
            Activity activity,
            PermissionCallback callback
    ) {
        ActivityResultLauncher<String> launcher =
                ((MainActivity) activity).registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if (isGranted) {
                                subscribeToAnnouncements();
                                callback.onGranted();
                            } else {
                                callback.onDenied();
                            }
                        }
                );
        return launcher;
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        int result = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
        );
        return result == PackageManager.PERMISSION_GRANTED;
    }
}
