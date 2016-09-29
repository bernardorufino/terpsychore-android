package com.brufino.terpsychore.fragments.session;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.lib.SimpleTextWatcher;
import com.brufino.terpsychore.util.ViewUtils;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class ChatFragment extends Fragment {

    public static int ACTION_BUTTON_OPEN_REACTIONS_ICON = R.drawable.ic_bubble_chart_white_40dp;
    public static int ACTION_BUTTON_CLOSE_REACTIONS_ICON = R.drawable.ic_close_white_40dp;
    public static int ACTION_BUTTON_SEND_MESSAGE_ICON = R.drawable.ic_send_white_40dp;

    private EditText vInput;
    private ImageButton vActionButton;
    private RelativeLayout vReactionsContainer;
    private TextView vChatWindow;

    private ChatButtonAction mButtonAction;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
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
        vChatWindow = (TextView) getView().findViewById(R.id.chat_window);

        mButtonAction = ChatButtonAction.OPEN_REACTIONS;
        ViewUtils.addOnFirstGlobalLayoutListener(
                vReactionsContainer,
                mOnFirstGlobalLayoutListener);
    }

    private ViewTreeObserver.OnGlobalLayoutListener mOnFirstGlobalLayoutListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            initializeReactions();
        }
    };

    private int[] reactionResIds = {
            R.drawable.reaction_love,
            R.drawable.reaction_wow,
            R.drawable.reaction_sad,
            R.drawable.reaction_angry
    };

    private void initializeReactions() {
        List<View> views = new ArrayList<>(reactionResIds.length);
        int size = -1;
        for (int resId : reactionResIds) {
            // TODO: Inflater for this, really?
            ImageView view = (ImageView) LayoutInflater
                    .from(getContext())
                    .inflate(R.layout.chat_reaction_icon, vReactionsContainer, false);
            view.setImageResource(resId);
            size = view.getLayoutParams().width;
            views.add(view);
        }
        checkState(size > 0);

        int centerX = vReactionsContainer.getMeasuredWidth() / 2;
        int centerY = vReactionsContainer.getMeasuredHeight() / 2;
        int padding = ViewUtils.dpToPx(getResources(), 20);
        int radius = (int) (vActionButton.getWidth() / 2.0 + size / 2.0 + padding + 0.5);
        ViewUtils.disposeViewsInArc(
                vReactionsContainer,
                centerX, centerY, radius,
                Math.PI / 2, Math.PI,
                views);
    }

    private TextWatcher mInputTextWatcher = new SimpleTextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
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
    };

    private View.OnClickListener mOnActionButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (mButtonAction) {
                case SEND_MESSAGE:
                    String message = vInput.getText().toString();
                    vChatWindow.append("Me: " + message + System.lineSeparator());
                    vInput.setText("");
                    break;
                case OPEN_REACTIONS:
                    if (vReactionsContainer.isShown()) {
                        vReactionsContainer.setVisibility(View.INVISIBLE);
                        vActionButton.setImageResource(ACTION_BUTTON_OPEN_REACTIONS_ICON);
                    } else {
                        vReactionsContainer.setVisibility(View.VISIBLE);
                        vActionButton.setImageResource(ACTION_BUTTON_CLOSE_REACTIONS_ICON);
                    }
                    break;
            }
        }
    };

    private static enum ChatButtonAction {
        SEND_MESSAGE, OPEN_REACTIONS
    }
}
