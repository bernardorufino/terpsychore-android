package com.brufino.terpsychore.messaging;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FirebaseMessagingServiceImpl extends FirebaseMessagingService {

    public static final String MESSAGE_RECEIVED =FirebaseMessagingServiceImpl.class.getCanonicalName() + ".MESSAGE_RECEIVED";
    public static final String EXTRA_KEY_SESSION_ID = "sessionId";
    public static final String EXTRA_KEY_MESSAGE_TYPE = "messageType";
    public static final String EXTRA_KEY_MESSAGE = "message";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
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

        Intent intent = new Intent(MESSAGE_RECEIVED);
        intent.putExtra(EXTRA_KEY_MESSAGE_TYPE, data.get("type"));
        intent.putExtra(EXTRA_KEY_SESSION_ID, Integer.parseInt(data.get("session_id")));
        if (data.containsKey("message")) {
            intent.putExtra(EXTRA_KEY_MESSAGE, data.get("message"));
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}
