package com.brufino.terpsychore.view.trackview;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment;
import com.brufino.terpsychore.lib.DynamicAdapter;
import com.brufino.terpsychore.lib.LoadingListIndicator;
import com.brufino.terpsychore.util.ViewUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MusicPickerList extends RelativeLayout {

    public static final int ITEMS_PER_REQUEST = 20;
    public static final int TRIGGER_MARGIN = 5;

    private final List<Item> mItems = new LinkedList<>();
    private RecyclerView vList;
    private LinearLayoutManager mLayoutManager;
    private Adapter<?> mAdapter;

    private ProgressBar vLoading;
    private LoadingListIndicator<MusicPickerListItemHolder> mLoadingIndicator;

    public MusicPickerList(Context context) {
        super(context);
        initializeView();
    }

    public MusicPickerList(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    public MusicPickerList(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView();
    }

    private void initializeView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_music_picker_list, this);
        vList = (RecyclerView) findViewById(R.id.music_picker_list_list);
        mLayoutManager = new LinearLayoutManager(getContext());
        vList.setLayoutManager(mLayoutManager);
        vLoading = (ProgressBar) findViewById(R.id.music_picker_list_loading);

        mLoadingIndicator = new LoadingListIndicator<>(vList, vLoading);
    }

    public <T> void setAdapter(Adapter<T> adapter) {
        mAdapter = adapter;
        mAdapter.setLoadingIndicator(mLoadingIndicator);
        vList.setAdapter(mAdapter);
    }

    public abstract static class Adapter<T> extends DynamicAdapter<Item, MusicPickerListItemHolder> {

        private LoadingListIndicator<MusicPickerListItemHolder> mLoadingIndicator;

        public Adapter() {
            super(ITEMS_PER_REQUEST, TRIGGER_MARGIN);
        }

        private void setLoadingIndicator(LoadingListIndicator<MusicPickerListItemHolder> loadingIndicator) {
            mLoadingIndicator = loadingIndicator;
        }

        public abstract Item transform(T item);

        protected void addItemsPreTransform(Collection<? extends T> items) {
            mLoadingIndicator.setLoading(false);
            List<Item> itemsPostTransform = new ArrayList<>(items.size());
            for (T itemPreTransform : items) {
                Item item = transform(itemPreTransform);
                itemsPostTransform.add(item);
            }
            addItems(itemsPostTransform);
        }

        @Override
        protected void loadItems(int offset, int limit) {
            mLoadingIndicator.setLoading(true);
        }

        protected void reportError() {
            mLoadingIndicator.setLoading(false);
            super.reportError();
        }

        @Override
        public MusicPickerListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_music_picker_list, parent, false);
            return new MusicPickerListItemHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final MusicPickerListItemHolder holder, int position, final Item item) {
            holder.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(holder, holder.getAdapterPosition(), item);
                    holder.bind(item);
                }
            });
            holder.bind(item);
            mLoadingIndicator.onBindViewHolder(holder, position, this);
        }

        protected abstract void onItemClick(MusicPickerListItemHolder holder, int position, Item item);
    }

    public static class MusicPickerListItemHolder extends RecyclerView.ViewHolder implements LoadingListIndicator.Item {

        private final ViewGroup vContainer;
        private final ImageView vImage;
        private final TextView vTitle;
        private final TextView vDescription;
        private final ProgressBar vLoading;
        private final ImageView vRemoveIcon;
        private final ImageView vTypeIcon;

        private final Context mContext;

        public MusicPickerListItemHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            vContainer = (ViewGroup) itemView.findViewById(R.id.item_music_picker_container);
            vImage = (ImageView) itemView.findViewById(R.id.item_music_picker_image);
            vTitle = (TextView) itemView.findViewById(R.id.item_music_picker_title);
            vDescription = (TextView) itemView.findViewById(R.id.item_music_picker_description);
            vLoading = (ProgressBar) itemView.findViewById(R.id.item_music_picker_loading);
            vRemoveIcon = (ImageView) itemView.findViewById(R.id.item_music_picker_remove);
            vTypeIcon = (ImageView) itemView.findViewById(R.id.item_music_picker_type);
        }

        public void bind(Item item) {
            vTitle.setText(item.title);
            vDescription.setText(item.description);
            Picasso.with(mContext)
                    .load(item.imageUrl)
                    .into(vImage);
            setSelected(item.selected);
            switch (item.type) {
                case PLAYLISTS:
                    vTypeIcon.setImageResource(R.drawable.ic_queue_music_white_24dp);
                    break;
                case ALBUM_SONGS:
                case PLAYLIST_SONGS:
                case SONGS:
                    vTypeIcon.setImageResource(R.drawable.ic_play_circle_filled_white_24dp);
                    break;
                case ALBUMS:
                    vTypeIcon.setImageResource(R.drawable.ic_album_white_24dp);
                    break;

            }
        }

        public void setSelected(boolean selected) {
            if (selected) {
                vTypeIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.colorPrimary));
                vRemoveIcon.setVisibility(View.GONE);
                vTitle.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
                ViewUtils.setBackground(vContainer, null);
                vContainer.setBackgroundColor(
                        ContextCompat.getColor(mContext, R.color.selectedBg));
            } else {
                vTypeIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.textSecondary));
                vRemoveIcon.setVisibility(View.GONE);
                vTitle.setTextColor(ContextCompat.getColor(mContext, R.color.text));
                ViewUtils.setBackgroundResource(vContainer, R.drawable.item_bg);
            }
        }

        public void setLoading(boolean loading) {
            vLoading.setVisibility((loading) ? View.VISIBLE : View.GONE);
        }

        public void setOnClickListener(View.OnClickListener listener) {
            vContainer.setOnClickListener(listener);
        }
    }

    public static class Item {

        public String title;
        public String description;
        public String imageUrl;
        public boolean selected = false;
        public Object data = null;
        public MusicPickerListFragment.ContentType type;

        public Item(String title, String description, String imageUrl) {
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
        }
    }
}
