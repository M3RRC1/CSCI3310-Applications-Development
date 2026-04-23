package com.example.location_based_social_media.Notifications;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService{
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();
            String postId = remoteMessage.getData().get("postId");

            NotificationHelper.show(getApplicationContext(), title, message, postId);
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d("FCM", "New token: " + token);
    }
}
