package com.brufino.terpsychore.messaging;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;

/* TODO: Support Nougat (Android 7) style messaging notifications (See NotificationCompat.MessagingStyle) */
public class FirebaseMessagingServiceImpl extends FirebaseMessagingService {

    public static final String MESSAGE_RECEIVED =FirebaseMessagingServiceImpl.class.getCanonicalName() + ".MESSAGE_RECEIVED";
    public static final String EXTRA_KEY_SESSION_ID = "sessionId";
    public static final String EXTRA_KEY_MESSAGE_TYPE = "messageType";
    public static final String EXTRA_KEY_MESSAGE = "message";

    private static final JsonParser JSON_PARSER = new JsonParser();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Context context = getApplicationContext();

        Log.d("VFY", "Firebase message received");
        Log.d("VFY", "  from = " + remoteMessage.getFrom());
        Log.d("VFY", "  data size = " + remoteMessage.getData().size());
        for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
            Log.d("VFY", "    " + entry.getKey() + " = " + entry.getValue());
        }

        if (remoteMessage.getData() == null || remoteMessage.getData().isEmpty()) {
            Log.w("VFY", "  [WARNING] empty message!");
            return;
        }

        Map<String, String> data = remoteMessage.getData();

        if (!data.containsKey("type") || !data.containsKey("session_id")) {
            Log.e("VFY", "Message data doesn't contain type or session_id");
            return;
        }

        String messageType = data.get("type");
        int sessionId = Integer.parseInt(data.get("session_id"));
        String messageString = data.containsKey("message") ? data.get("message") : null;

        // Send broadcast
        Intent broadcastIntent = new Intent(MESSAGE_RECEIVED);
        broadcastIntent.putExtra(EXTRA_KEY_MESSAGE_TYPE, messageType);
        broadcastIntent.putExtra(EXTRA_KEY_SESSION_ID, sessionId);
        if (messageString != null) {
            broadcastIntent.putExtra(EXTRA_KEY_MESSAGE, messageString);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

        // Create notification
        if (messageString != null) {
            JsonObject message = JSON_PARSER.parse(messageString).getAsJsonObject();
            LocalMessagesManager messagesManager = LocalMessagesManager.getInstance();
            messagesManager.addMessage(context, sessionId, message);
            messagesManager.updateNotification(context, sessionId);
        }
    }
}
