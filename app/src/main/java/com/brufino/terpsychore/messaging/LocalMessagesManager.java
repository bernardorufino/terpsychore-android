package com.brufino.terpsychore.messaging;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.activities.SessionActivity;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;
import com.brufino.terpsychore.util.CoreUtils;
import com.brufino.terpsychore.util.ViewUtils;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;

public class LocalMessagesManager {

    private static final int INBOX_STYLE_LINE_LIMIT = 5;
    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final List<MessagePresenter> MESSAGE_PRESENTERS = ImmutableList.<MessagePresenter>of(
            new ChatMessagePresenter());

    private static class InstanceHolder {
        private static final LocalMessagesManager INSTANCE = new LocalMessagesManager();
    }

    public static LocalMessagesManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private Map<String, List<JsonObject>> mMessages = new HashMap<>();

    private LocalMessagesManager() {
        /* Prevents outside instantiation */
    }

    /* TODO: Order by date! */
    private Comparator<JsonObject> mMessageComparator = Ordering
            .natural()
            .onResultOf(new Function<JsonObject, Integer>() {
                @Override
                public Integer apply(JsonObject message) {
                    return message.get("id").getAsInt();
                }
            });

    public synchronized void addMessage(Context context, int sessionId, JsonObject message) {
        if (selectPresenterForMessage(message) == null) {
            return;
        }
        List<JsonObject> messages = getMessagesBySessionId(sessionId);
        messages.add(message);
        Collections.sort(messages, mMessageComparator);
        JsonArray messagesJson = CoreUtils.jsonObjectListToJsonArray(messages);
        context.getSharedPreferences(SharedPreferencesDefs.Messaging.FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(getSessionKey(sessionId), messagesJson.toString())
                .apply();
    }

    public synchronized List<JsonObject> getMessages(Context context, int sessionId) {
        String sessionKey = getSessionKey(sessionId);
        List<JsonObject> messages = mMessages.get(sessionKey);
        if (messages == null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    SharedPreferencesDefs.Messaging.FILE,
                    Context.MODE_PRIVATE);
            String messagesString = sharedPreferences.getString(sessionKey, null);
            messages = CoreUtils.jsonArrayToJsonObjectList((messagesString == null)
                    ? new JsonArray()
                    : JSON_PARSER.parse(messagesString).getAsJsonArray());
            mMessages.put(sessionKey, messages);
        }
        return messages;
    }

    public synchronized Collection<Integer> getSessionIds() {
        List<Integer> sessionIds = new ArrayList<>();
        for (String sessionKey : mMessages.keySet()) {
            sessionIds.add(getSessionId(sessionKey));
        }
        return sessionIds;
    }

    public synchronized void clearMessages(Context context, int sessionId) {
        String sessionKey = getSessionKey(sessionId);
        mMessages.put(sessionKey, new ArrayList<JsonObject>());
        context.getSharedPreferences(SharedPreferencesDefs.Messaging.FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(sessionKey, null)
                .apply();
    }

    private List<JsonObject> getMessagesBySessionId(int sessionId) {
        String sessionKey = getSessionKey(sessionId);
        List<JsonObject> messages = mMessages.get(sessionKey);
        if (messages == null) {
            messages = new ArrayList<>();
            mMessages.put(sessionKey, messages);
        }
        return messages;
    }

    private String getSessionKey(int sessionId) {
        return SharedPreferencesDefs.Messaging.KEY_SESSION_PREFIX + sessionId;
    }

    private int getSessionId(String sessionKey) {
        return Integer.parseInt(sessionKey.replace(SharedPreferencesDefs.Messaging.KEY_SESSION_PREFIX, ""));
    }

    public synchronized void updateNotification(Context context, int sessionId) {
        Notification notification = getNotificationForSession(context, sessionId);
        if (notification == null) {
            getNotificationManager(context).cancel(sessionId);
        } else {
            getNotificationManager(context).notify(sessionId, notification);
        }
    }

    private Notification getNotificationForSession(Context context, int sessionId) {
        List<JsonObject> messages = getMessages(context, sessionId);
        if (messages.isEmpty()) {
            return null;
        }
        int nMessages = messages.size();
        JsonObject lastMessage = messages.get(nMessages - 1);
        String sessionName = lastMessage.get("session").getAsJsonObject().get("name").getAsString();
        NotificationCompat.Builder notificationBuilder =
                getNotificationBuilderScaffold(context, sessionId, sessionName);
        if (nMessages == 1) {
            return notificationBuilder
                    .setContentText(selectPresenterForMessage(lastMessage).getNotificationLine(lastMessage))
                    .build();
        }
        String summary = nMessages + " unread " + ViewUtils.pluralWithS(nMessages, "message");
        NotificationCompat.InboxStyle inboxStyle = getNotificationInboxStyle(messages, summary);
        return notificationBuilder
                .setContentText(summary)
                .setNumber(nMessages)
                .setStyle(inboxStyle)
                .build();
    }

    private NotificationCompat.Builder getNotificationBuilderScaffold(
            Context context,
            int sessionId,
            String sessionName) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_logo)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentTitle(sessionName)
                .setAutoCancel(false)
                .setShowWhen(true);
        Intent actionIntent = new Intent(context, SessionActivity.class);
        actionIntent.putExtra(SessionActivity.SESSION_ID_EXTRA_KEY, sessionId);
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addParentStack(SessionActivity.class)
                .addNextIntent(actionIntent)
                .getPendingIntent(sessionId, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        return notificationBuilder;
    }

    private NotificationCompat.InboxStyle getNotificationInboxStyle(List<JsonObject> messages, String summary) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        for (JsonObject message : Iterables.limit(Lists.reverse(messages), INBOX_STYLE_LINE_LIMIT)) {
            MessagePresenter presenter = selectPresenterForMessage(message);
            if (presenter == null) {
                continue;
            }
            CharSequence line = presenter.getNotificationLine(message);
            inboxStyle.addLine(line);
        }
        if (messages.size() > INBOX_STYLE_LINE_LIMIT) {
            inboxStyle.addLine("...");
        }
        inboxStyle.setSummaryText(summary);
        return inboxStyle;
    }

    private MessagePresenter selectPresenterForMessage(JsonObject message) {
        String messageType = message.get("type").getAsString();
        for (MessagePresenter presenter : MESSAGE_PRESENTERS) {
            if (presenter.getSupportedTypes().contains(messageType)) {
                return presenter;
            }
        }
        return null;
    }

    public CharSequence getDescriptionForMessage(JsonObject message) {
        return selectPresenterForMessage(message).getDescription(message);
    }

    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static abstract class MessagePresenter {

        public abstract CharSequence getNotificationLine(JsonObject message);

        public NotificationCompat.MessagingStyle.Message getNotificationMessage(JsonObject message) {
            /* TODO: Implement Nougat (Android 7) message style notification */
            return null;
        }

        public CharSequence getDescription(JsonObject message) {
            return getNotificationLine(message);
        }

        public abstract Collection<String> getSupportedTypes();
    }

    public static class ChatMessagePresenter extends MessagePresenter {

        @Override
        public CharSequence getNotificationLine(JsonObject message) {
            String[] splitMessage = splitMessage(message);
            return splitMessage[0] + ": " + splitMessage[1];
        }

        @Override
        public CharSequence getDescription(JsonObject message) {
            String[] splitMessage = splitMessage(message);
            return Html.fromHtml("<b>" + splitMessage[0] + "</b>: " + splitMessage[1]);
        }

        private String[] splitMessage(JsonObject message) {
            JsonObject user = message.get("user").getAsJsonObject();
            String displayName = !user.get("display_name").isJsonNull()
                                 ? user.get("display_name").getAsString()
                                 : user.get("username").getAsString();
            displayName = ViewUtils.getFirstName(displayName);
            String content = message.get("content").getAsString();
            return new String[] {displayName, content};
        }

        @Override
        public Collection<String> getSupportedTypes() {
            return ImmutableList.of("chat_message");
        }
    }
}
