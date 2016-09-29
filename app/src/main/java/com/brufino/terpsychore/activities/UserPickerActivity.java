package com.brufino.terpsychore.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.lib.ApiCallback;
import com.brufino.terpsychore.lib.CircleTransformation;
import com.brufino.terpsychore.lib.TrialScheduler;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SearchApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.lib.SimpleTextWatcher;
import com.brufino.terpsychore.util.CoreUtils;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;
import retrofit2.Call;
import retrofit2.Response;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class UserPickerActivity extends Activity {

    public static final String RESULT_USER_IDS = "userIds";
    public static final int TYPE_TO_LOAD_INTERVAL_IN_MS = 500;

    private Toolbar vToolbar;
    private RecyclerView vUserList;
    private EditText vSearchText;
    private ProgressBar vLoading;
    private TextView vSelectionStatus;
    private ViewGroup vSelection;
    private FrameLayout vSelectionDone;

    private Map<String, JsonObject> mSelectedUserIds = new LinkedHashMap<>();
    private List<JsonObject> mUserList = new ArrayList<>();
    private AtomicInteger mPendingLoadRequests;
    private UserListAdapter mUserListAdapter;
    private LinearLayoutManager mUserListLayoutManager;
    private TrialScheduler mUsersLoader;
    private SearchApi mSearchApi;
    private String mUserId;
    private TextView vMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_picker);

        vToolbar = (Toolbar) findViewById(R.id.toolbar);
        vUserList = (RecyclerView) findViewById(R.id.user_picker_user_list);
        mUserListAdapter = new UserListAdapter();
        mUserListLayoutManager = new LinearLayoutManager(this);
        vUserList.setAdapter(mUserListAdapter);
        vUserList.setLayoutManager(mUserListLayoutManager);
        vSearchText = (EditText) findViewById(R.id.user_picker_search);
        vSearchText.addTextChangedListener(mSearchInputTextWatcher);
        vLoading = (ProgressBar) findViewById(R.id.user_picker_loading);
        vSelectionStatus = (TextView) findViewById(R.id.picker_selection_status);
        vSelection = (ViewGroup) findViewById(R.id.picker_selection);
        vSelectionDone = (FrameLayout) findViewById(R.id.picker_selection_done);
        vSelectionDone.setOnClickListener(mOnSelectionDoneClickListener);
        vMessage = (TextView) findViewById(R.id.user_picker_message);

        mUsersLoader = new UsersLoader(this);
        mPendingLoadRequests = new AtomicInteger(0);
        mSearchApi = ApiUtils.createApi(SearchApi.class);
        mUserId = ActivityUtils.getUserId(this);
    }

    private boolean isValidQuery(String query) {
        return query.trim().length() >= 3;
    }

    private TextWatcher mSearchInputTextWatcher = new SimpleTextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            setMessage(null, -1);
            if (isValidQuery(s.toString())) {
                vLoading.setVisibility(View.VISIBLE);
                mUsersLoader.tryExecute();
            } else {
                mUserList.clear();
                mUserListAdapter.notifyDataSetChanged();
            }
        }
    };

    private void setMessage(CharSequence message, int imageResId) {
        if (message == null) {
            vMessage.setVisibility(View.GONE);
            vMessage.setText("");
        } else {
            vMessage.setVisibility(View.VISIBLE);
            vMessage.setText(message);
            vMessage.setCompoundDrawablesWithIntrinsicBounds(0, imageResId, 0, 0);
        }
    }

    private void loadUsers() {
        /* TODO: Filter @, # and other non-alpha chars */
        final String query = vSearchText.getText().toString().trim().replaceFirst("^@", "");
        if (!isValidQuery(query)) {
            if (mPendingLoadRequests.intValue() == 0 && !mUsersLoader.isOutstanding()) {
                vLoading.setVisibility(View.GONE);
            }
            return;
        }
        int requests = mPendingLoadRequests.incrementAndGet();
        Log.d("VFY", UserPickerActivity.class.getSimpleName() + ": " + requests + " pending requests");
        mSearchApi.searchUsers(query, mUserId).enqueue(new ApiCallback<JsonObject>() {
            public void onFinish() {
                if (mPendingLoadRequests.decrementAndGet() == 0 && !mUsersLoader.isOutstanding()) {
                    vLoading.setVisibility(View.GONE);
                }
            }
            @Override
            public void onSuccess(Call<JsonObject> call, Response<JsonObject> response) {
                onFinish();
                JsonObject body = response.body();
                JsonArray results = body.get("results").getAsJsonArray();
                mUserList.clear();
                mUserList.addAll(CoreUtils.jsonArrayToJsonObjectList(results));
                mUserListAdapter.notifyDataSetChanged();

                if (mUserList.isEmpty()) {
                    String htmlMessage = getString(R.string.user_picker_user_not_found, getString(R.string.app_name));
                    setMessage(Html.fromHtml(htmlMessage), R.drawable.ic_sentiment_dissatisfied_white_48dp);
                } else {
                    setMessage(null, -1);
                }
                Log.d("VFY", UserPickerActivity.class.getSimpleName() +  ": results size = " + results.size());
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                onFinish();
                Log.e("VFY", "Error searching for users (query = '" + query + "')", t);
                Toast.makeText(UserPickerActivity.this, "Error searching for users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onUserItemClick(UserItemHolder holder, int position, JsonObject item) {
        String userId = item.get("id").getAsString();
        if (mSelectedUserIds.containsKey(userId)) {
            mSelectedUserIds.remove(userId);
        } else {
            mSelectedUserIds.put(userId, item);
        }
        if (mSelectedUserIds.isEmpty()) {
            vSelection.setVisibility(View.GONE);
        } else {
            vSelection.setVisibility(View.VISIBLE);
            vSelectionStatus.setText(mSelectedUserIds.size() + " friends selected");
        }
        // Will bind again and the view holder will style it accordingly
    }

    private View.OnClickListener mOnSelectionDoneClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mSelectedUserIds.isEmpty()) {
                setResult(RESULT_CANCELED);
            } else {
                Intent result = new Intent();
                ArrayList<String> userIds = Lists.newArrayList(mSelectedUserIds.keySet());
                result.putExtra(RESULT_USER_IDS, userIds);
                setResult(RESULT_OK, result);
            }
            finish();
        }
    };

    private static class UsersLoader extends TrialScheduler {

        private final WeakReference<UserPickerActivity> mActivityRef;

        public UsersLoader(UserPickerActivity activity) {
            super(new Handler(), TYPE_TO_LOAD_INTERVAL_IN_MS, TimeUnit.MILLISECONDS);
            mActivityRef = new WeakReference<>(activity);
        }
        @Override
        protected void doExecute() {
            UserPickerActivity activity = mActivityRef.get();
            if (activity != null) {
                activity.loadUsers();
            }
        }

    }

    private class UserListAdapter extends RecyclerView.Adapter<UserItemHolder> {

        @Override
        public UserItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_list, parent, false);
            return new UserItemHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final UserItemHolder holder, final int position) {
            final JsonObject item = mUserList.get(position);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onUserItemClick(holder, position, item);
                    holder.bind(item);
                }
            });
            holder.bind(item);
        }
        @Override
        public int getItemCount() {
            return mUserList.size();
        }

    }

    private class UserItemHolder extends RecyclerView.ViewHolder {

        private final TextView vTitle;
        private final TextView vDescription;
        private final ImageView vImage;
        private final ViewGroup vContainer;

        private final Context mContext;

        public UserItemHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            vTitle = (TextView) itemView.findViewById(R.id.item_user_title);
            vDescription = (TextView) itemView.findViewById(R.id.item_user_description);
            vImage = (ImageView) itemView.findViewById(R.id.item_user_image);
            vContainer = (ViewGroup) itemView.findViewById(R.id.item_user_container);
        }

        public void bind(JsonObject item) {
            String userId = item.get("id").getAsString();
            String username = item.get("username").getAsString();
            String spotifyId = item.get("spotify_id").getAsString();
            String displayName = item.get("display_name").getAsString();
            JsonElement imageUrlElement = item.get("image_url");

            if (!imageUrlElement.isJsonNull()) {
                Picasso.with(mContext)
                        .load(imageUrlElement.getAsString())
                        .placeholder(R.drawable.ic_account_circle_no_padding_gray_48dp)
                        .transform(new CircleTransformation())
                        .into(vImage);
                vImage.setImageTintList(null);
            } else {
                Picasso.with(mContext)
                        .load(R.drawable.ic_account_circle_no_padding_gray_48dp)
                        .into(vImage);
                vImage.setImageTintList(ActivityUtils.getColorList(mContext, R.color.textSecondary));
            }

            vTitle.setText(displayName);
            vDescription.setText("@" + username);

            if (mSelectedUserIds.containsKey(userId)) {
                vTitle.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
                vContainer.setBackground(null);
                vContainer.setBackgroundColor(ContextCompat.getColor(mContext, R.color.selectedBg));
            } else {
                vTitle.setTextColor(ContextCompat.getColor(mContext, R.color.text));
                vContainer.setBackground(ContextCompat.getDrawable(mContext, R.drawable.item_music_picker_bg));
            }
        }
    }
}
