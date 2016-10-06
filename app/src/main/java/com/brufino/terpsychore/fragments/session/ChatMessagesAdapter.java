package com.brufino.terpsychore.fragments.session;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.lib.ApiCallback;
import com.brufino.terpsychore.lib.DynamicAdapter;
import com.brufino.terpsychore.lib.LoadingListIndicator;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.MessagesApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.util.CoreUtils;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Response;

import java.util.List;
import java.util.Objects;

public class ChatMessagesAdapter extends DynamicAdapter<JsonObject, ChatMessagesAdapter.MessageViewHolder> {

    private static final List<String> TYPES = Lists.newArrayList(
            "current_user_message",
            "user_message",
            "session_message");

    private final MessagesApi mMessagesApi;
    private final int mSessionId;
    private final Context mContext;
    private LoadingListIndicator<MessageViewHolder> mLoadingIndicator;

    public ChatMessagesAdapter(Context context, int sessionId) {
        super(20, 5);
        mContext = context;
        mSessionId = sessionId;
        mMessagesApi =  ApiUtils.createApi(MessagesApi.class);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mLoadingIndicator = new LoadingListIndicator<>(recyclerView);
    }

    @Override
    protected void loadItems(int offset, int limit) {
        mLoadingIndicator.setLoading(true);
        mMessagesApi.getMessages(mSessionId, offset, limit).enqueue(new ApiCallback<JsonArray>() {
            @Override
            public void onSuccess(Call<JsonArray> call, Response<JsonArray> response) {
                mLoadingIndicator.setLoading(false);
                List<JsonObject> items = toMessageList(response.body());
                addItems(items);
                annotateMessages();
            }
            @Override
            public void onFailure(Call<JsonArray> call, Throwable t, Response<JsonArray> response) {
                mLoadingIndicator.setLoading(false);
                Log.e("VFY", "Error loading chat", t);
                Toast.makeText(mContext, "Error loading chat", Toast.LENGTH_SHORT).show();
                reportError();
            }
        });
    }

    public void loadNewItems() {
        int newerThanMessageId = mList.get(0).get("id").getAsInt();
        mMessagesApi.getNewMessages(mSessionId, newerThanMessageId).enqueue(new ApiCallback<JsonArray>() {
            @Override
            public void onSuccess(Call<JsonArray> call, Response<JsonArray> response) {
                List<JsonObject> items = toMessageList(response.body());
                mList.addAll(0, items);
                notifyDataSetChanged();
                annotateMessages();
            }
            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Log.e("VFY", "Error loading new messages", t);
                Toast.makeText(mContext, "Error loading new messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void annotateMessages() {
        String lastUserId = null;
        for (JsonObject message : Lists.reverse(mList)) {
            String type = message.get("type").getAsString();
            if (!message.get("user").isJsonNull()) {
                String userId = message.get("user").getAsJsonObject().get("id").getAsString();
                message.remove("first_of_user");
                if (!Objects.equals(userId, lastUserId)) {
                    lastUserId = userId;
                    message.addProperty("first_of_user", true);
                }
            }
        }
    }

    public List<JsonObject> toMessageList(JsonArray response) {
        String currentUserId = ActivityUtils.getUserId(mContext);
        List<JsonObject> items = CoreUtils.jsonArrayToJsonObjectList(response);
        for (JsonObject item : items) {
            if (item.get("type").getAsString().equals("user_message")) {
                String userId = item.get("user").getAsJsonObject().get("id").getAsString();
                if (currentUserId.equals(userId)) {
                    item.addProperty("type", "current_user_message");
                }
            }
        }
        return items;
    }

    @Override
    public int getItemViewType(int position) {
        JsonObject item = getItem(position);
        return TYPES.indexOf(item.get("type").getAsString());
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return MessageViewHolder.create(parent, TYPES.get(viewType));
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position, JsonObject item) {
        holder.bind(item);
        mLoadingIndicator.onBindViewHolder(holder, position, this);
    }

    public abstract static class MessageViewHolder extends RecyclerView.ViewHolder
            implements LoadingListIndicator.Item {

        public static MessageViewHolder create(ViewGroup parent, String type) {
            int layout;
            switch (type) {
                case "current_user_message": layout = CurrentUserMessageViewHolder.LAYOUT; break;
                case "user_message": layout = DifferentUserMessageViewHolder.LAYOUT; break;
                case "session_message": layout = SessionMessageViewHolder.LAYOUT; break;
                default: throw new AssertionError("Unknown view type");
            }
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View container = inflater.inflate(R.layout.item_chat_container, parent, false);
            ViewGroup content = (ViewGroup) container.findViewById(R.id.item_chat_content);
            inflater.inflate(layout, content, true);
            switch (type) {
                case "current_user_message": return new CurrentUserMessageViewHolder(container);
                case "user_message": return new DifferentUserMessageViewHolder(container);
                case "session_message": return new SessionMessageViewHolder(container);
                default: throw new AssertionError("Unknown view type");
            }
        }

        private final ProgressBar vLoading;

        public MessageViewHolder(View itemView) {
            super(itemView);
            vLoading = (ProgressBar) itemView.findViewById(R.id.item_chat_loading);
        }

        public void setLoading(boolean loading) {
            vLoading.setVisibility((loading) ? View.VISIBLE : View.GONE);
        }

        public abstract void bind(JsonObject item);
    }

    private static class UserMessageViewHolder extends MessageViewHolder {

        protected final ViewGroup vContainer;
        protected final RelativeLayout vBubble;
        protected final TextView vContent;

        public UserMessageViewHolder(View itemView) {
            super(itemView);
            vContainer = (ViewGroup) itemView.findViewById(R.id.item_chat_message_container);
            vBubble = (RelativeLayout) itemView.findViewById(R.id.item_chat_message_bubble);
            vContent = (TextView) itemView.findViewById(R.id.item_chat_message_content);
        }

        @Override
        public void bind(JsonObject item) {
            String content = item.get("content").getAsString();
            boolean firstOfUser = item.has("first_of_user");
            vContent.setText(content);
            int topMarginRes = (firstOfUser) ? R.dimen.user_message_first_top_margin : R.dimen.user_message_top_margin;
            int topMargin = vContainer.getResources().getDimensionPixelSize(topMarginRes);
            ((ViewGroup.MarginLayoutParams) vContainer.getLayoutParams()).topMargin = topMargin;
        }
    }

    private static class CurrentUserMessageViewHolder extends UserMessageViewHolder {
        public static final int LAYOUT = R.layout.item_chat_current_user_message;

        public CurrentUserMessageViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bind(JsonObject item) {
            super.bind(item);
            boolean firstOfUser = item.has("first_of_user");
            vBubble.setBackgroundResource((firstOfUser)
                    ? R.drawable.chat_current_user_message_first_bg
                    : R.drawable.chat_current_user_message_bg);
        }
    }

    private static class DifferentUserMessageViewHolder extends UserMessageViewHolder {
        public static final int LAYOUT = R.layout.item_chat_different_user_message;

        public DifferentUserMessageViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bind(JsonObject item) {
            super.bind(item);
            boolean firstOfUser = item.has("first_of_user");
            vBubble.setBackgroundResource((firstOfUser)
                    ? R.drawable.chat_user_message_first_bg
                    : R.drawable.chat_user_message_bg);
        }
    }

    private static class SessionMessageViewHolder extends MessageViewHolder {
        public static final int LAYOUT = R.layout.item_chat_session_message;

        private final TextView vContent;

        public SessionMessageViewHolder(View itemView) {
            super(itemView);
            vContent = (TextView) itemView.findViewById(R.id.item_chat_message_content);
        }

        @Override
        public void bind(JsonObject item) {
            String content = item.get("content").getAsString();
            vContent.setText(content);
        }
    }
}
