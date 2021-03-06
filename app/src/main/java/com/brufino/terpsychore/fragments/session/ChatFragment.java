package com.brufino.terpsychore.fragments.session;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.lib.ApiCallback;
import com.brufino.terpsychore.lib.EmojiFontTextWatcher;
import com.brufino.terpsychore.lib.EmojisIndex;
import com.brufino.terpsychore.lib.EmoticonsHitCounter;
import com.brufino.terpsychore.messaging.FirebaseMessagingServiceImpl;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.MessagesApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.util.CoreUtils;
import com.brufino.terpsychore.util.FontUtils;
import com.brufino.terpsychore.util.ViewUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

public class ChatFragment extends Fragment {

    private static final long REPEAT_EMOTICON_DELAY_IN_MS = 350;
    private static final long REPEAT_EMOTICON_INTERVAL_IN_MS = 120;

    private static Set<String> SUPPORTED_MESSAGE_TYPES = Sets.newHashSet("chat_message", "session_message");
    private static int ACTION_BUTTON_OPEN_REACTIONS_ICON = R.drawable.ic_bubble_chart_white_40dp;
    private static int ACTION_BUTTON_CLOSE_REACTIONS_ICON = R.drawable.ic_close_white_40dp;
    private static int ACTION_BUTTON_SEND_MESSAGE_ICON = R.drawable.ic_send_white_40dp;

    private EditText vInput;
    private ImageButton vActionButton;
    private RelativeLayout vReactionsContainer;
    private RecyclerView vMessagesList;

    private int mSessionId = -1;
    private List<TextView> mReactionTextViews = new ArrayList<>();
    private ChatButtonAction mButtonAction;
    private ChatMessagesAdapter mMessagesAdapter;
    private MessagesApi mMessagesApi;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    public void setSessionId(int sessionId) {
        mSessionId = sessionId;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        vInput = (EditText) getView().findViewById(R.id.chat_input);
        vInput.addTextChangedListener(mInputTextWatcher);
        vActionButton = (ImageButton) getView().findViewById(R.id.chat_action_button);
        vActionButton.setImageResource(ACTION_BUTTON_OPEN_REACTIONS_ICON);
        vActionButton.setOnClickListener(mOnActionButtonClickListener);
        vReactionsContainer = (RelativeLayout) getView().findViewById(R.id.chat_reactions_container);
        vMessagesList = (RecyclerView) getView().findViewById(R.id.chat_messages_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
        vMessagesList.setLayoutManager(layoutManager);

        mMessagesApi = ApiUtils.createApi(getContext(), MessagesApi.class);
        checkState(mSessionId != -1, "Provide session id with setSessionId()");
        mMessagesAdapter = new ChatMessagesAdapter(getContext(), mSessionId);
        vMessagesList.setAdapter(mMessagesAdapter);
        mMessagesAdapter.firstLoad();

        mButtonAction = ChatButtonAction.OPEN_REACTIONS;
        ViewUtils.addOnFirstGlobalLayoutListener(
                vReactionsContainer,
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        initializeReactions();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(FirebaseMessagingServiceImpl.MESSAGE_RECEIVED));
        if (!mMessagesAdapter.isLoading()) {
            mMessagesAdapter.loadNewMessages();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(FirebaseMessagingServiceImpl.EXTRA_KEY_MESSAGE_TYPE);
            int sessionId = intent.getIntExtra(FirebaseMessagingServiceImpl.EXTRA_KEY_SESSION_ID, -1);
            if (SUPPORTED_MESSAGE_TYPES.contains(type) && mSessionId == sessionId && mSessionId != -1) {
                mMessagesAdapter.loadNewMessages();
            }
        }
    };

    private void initializeReactions() {
        EmoticonsHitCounter emoticonsHitCounter = EmoticonsHitCounter.load(getContext());
        List<String> emoticons = emoticonsHitCounter.getTopDefaultSize();
        List<View> views = new ArrayList<>(emoticons.size());
        int size = -1;
        for (String emoticon : emoticons) {
            ViewGroup reactionWrapper = (ViewGroup) LayoutInflater.from(getContext())
                    .inflate(R.layout.chat_reaction, vReactionsContainer, false);
            TextView reactionTextView = (TextView) reactionWrapper.findViewById(R.id.chat_reaction_icon);
            mReactionTextViews.add(reactionTextView);
            FontUtils.setTextWithEmojis(reactionTextView, emoticon);
            size = reactionWrapper.getLayoutParams().width;
            views.add(reactionWrapper);
            reactionTextView.setOnTouchListener(mOnReactionTouchListener);
        }
        checkState(size > 0);

        int centerX = vReactionsContainer.getMeasuredWidth() / 2;
        int centerY = vReactionsContainer.getMeasuredHeight() / 2;
        int padding = ViewUtils.dpToPx(getResources(), 0);
        int radius = (int) (0.5 + vActionButton.getWidth() / 2.0 + size / 2.0 + padding);
        double offset = 3 * Math.PI / 180;
        ViewUtils.disposeViewsInArc(
                vReactionsContainer,
                centerX, centerY, radius,
                Math.PI / 2 - offset, Math.PI + offset,
                views);
    }

    private void updateReactions() {
        EmoticonsHitCounter emoticonsHitCounter = EmoticonsHitCounter.load(getContext());
        int i = 0;
        for (String emoticon : emoticonsHitCounter.getTopDefaultSize()){
            FontUtils.setTextWithEmojis(mReactionTextViews.get(i), emoticon);
            i++;
        }
    }

    private volatile View mReactionButtonDown = null;

    private View.OnTouchListener mOnReactionTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final TextView reactionView = (TextView) v;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mReactionButtonDown = reactionView;
                vInput.getText().append(reactionView.getText());
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mReactionButtonDown == reactionView) {
                            vInput.getText().append(reactionView.getText());
                            new Handler().postDelayed(this, REPEAT_EMOTICON_INTERVAL_IN_MS);
                        }
                    }
                }, REPEAT_EMOTICON_DELAY_IN_MS);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mReactionButtonDown = null;
                updateActionButtonStateBasedOnInput();
                return true;
            }
            return false;
        }
    };

    private TextWatcher mInputTextWatcher = new EmojiFontTextWatcher() {

        @Override
        protected Context getContext() {
            return ChatFragment.this.getContext();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateActionButtonStateBasedOnInput();
        }
    };

    private void updateActionButtonStateBasedOnInput() {
        if (mReactionButtonDown != null) {
            return;
        }
        if (vInput.getText().toString().isEmpty()) {
            mButtonAction = ChatButtonAction.OPEN_REACTIONS;
            vActionButton.setImageResource(ACTION_BUTTON_OPEN_REACTIONS_ICON);
            vReactionsContainer.setVisibility(View.INVISIBLE);
        } else {
            mButtonAction = ChatButtonAction.SEND_MESSAGE;
            vActionButton.setImageResource(ACTION_BUTTON_SEND_MESSAGE_ICON);
            vReactionsContainer.setVisibility(View.INVISIBLE);
        }
    }

    private Callback<JsonObject> mOnMessagePosted = new ApiCallback<JsonObject>() {
        @Override
        public void onSuccess(Call<JsonObject> call, Response<JsonObject> response) {
            mMessagesAdapter.loadNewMessages();
        }
        @Override
        public void onFailure(Call<JsonObject> call, Throwable t) {
            Log.e("VFY", "Error posting message", t);
            Toast.makeText(getContext(), "Error sending message", Toast.LENGTH_SHORT).show();
        }
    };

    private View.OnClickListener mOnActionButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (mButtonAction) {
                case SEND_MESSAGE:
                    String content = vInput.getText().toString().trim();
                    if (content.isEmpty()) {
                        return;
                    }
                    updateEmoticonHitCounter(content);
                    String userId = ActivityUtils.getUserId(getContext());
                    ApiUtils.postMessage(
                            getContext(),
                            mSessionId,
                            "chat_message",
                            new ImmutableMap.Builder<String, String>()
                                    .put("content", content)
                                    .put("user_id", userId)
                                    .build(),
                            mOnMessagePosted);
                    vInput.setText("");
                    break;
                case OPEN_REACTIONS:
                    if (vReactionsContainer.isShown()) {
                        vReactionsContainer.setVisibility(View.INVISIBLE);
                        vActionButton.setImageResource(ACTION_BUTTON_OPEN_REACTIONS_ICON);
                    } else {
                        updateReactions();
                        vReactionsContainer.setVisibility(View.VISIBLE);
                        vActionButton.setImageResource(ACTION_BUTTON_CLOSE_REACTIONS_ICON);
                    }
                    break;
            }
        }
    };

    private void updateEmoticonHitCounter(String message) {
        EmoticonsHitCounter counter = EmoticonsHitCounter.load(getContext());
        Set<Integer> emoticons = new HashSet<>(); // 1 instance per message
        for (int codePoint : CoreUtils.codePoints(message)) {
            if (EmojisIndex.isEmoji(codePoint) && !emoticons.contains(codePoint)) {
                String emoticon = String.valueOf(Character.toChars(codePoint));
                counter.hit(emoticon);
                emoticons.add(codePoint);
            }
        }
        counter.saveInBackground();
    }

    private static enum ChatButtonAction {
        SEND_MESSAGE, OPEN_REACTIONS
    }
}
