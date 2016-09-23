package com.brufino.terpsychore.view.trackview;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import com.brufino.terpsychore.lib.DynamicAdapter;

import java.util.*;

public class MusicPickerList extends RelativeLayout {

    public static final int ITEMS_PER_REQUEST = 20;
    /* TODO: Forcing 0 bc of loading spinner on last item */
    public static final int TRIGGER_MARGIN = 5;

    private final List<Item> mItems = new LinkedList<>();
    private RecyclerView vList;
    private LinearLayoutManager mLayoutManager;
    private Adapter<?> mAdapter;
    private boolean mLoading;
    private ProgressBar vLoading;

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
    }

    public <T> void setAdapter(Adapter<T> adapter) {
        mAdapter = adapter;
        mAdapter.setMusicPickerList(this);
        vList.setAdapter(mAdapter);
    }

    private Collection<MusicPickerListItemHolder> mLoadingViewHolders = new HashSet<>();

    public void setLoading(boolean loading) {
        mLoading = loading;
        if (loading) {
            if (vList.getChildCount() == 0) {
                vLoading.setVisibility(View.VISIBLE);
            } // else it's handled in mAdapter.onBindViewHolder()
        } else {
            vLoading.setVisibility(View.GONE);
            for (MusicPickerListItemHolder viewHolder : mLoadingViewHolders) {
                viewHolder.setLoading(false);
            }
            mLoadingViewHolders.clear();
        }
    }

    public abstract static class Adapter<T> extends DynamicAdapter<Item, MusicPickerListItemHolder> {

        private MusicPickerList vMusicPickerList;

        public Adapter() {
            super(ITEMS_PER_REQUEST, TRIGGER_MARGIN);
        }

        private void setMusicPickerList(MusicPickerList musicPickerList) {
            vMusicPickerList = musicPickerList;
        }

        public abstract Item transform(T item);

        protected void addItemsPreTransform(Collection<? extends T> items) {
            vMusicPickerList.setLoading(false);
            List<Item> itemsPostTransform = new ArrayList<>(items.size());
            for (T itemPreTransform : items) {
                Item item = transform(itemPreTransform);
                itemsPostTransform.add(item);
            }
            addItems(itemsPostTransform);
        }

        @Override
        protected void loadItems(int offset, int limit) {
            vMusicPickerList.setLoading(true);
        }

        protected void reportError() {
            vMusicPickerList.setLoading(false);
            super.reportError();
        }

        @Override
        public MusicPickerListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_music_picker_list, parent, false);
            return new MusicPickerListItemHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MusicPickerListItemHolder holder, int position, Item item) {
            // boolean loading = vMusicPickerList.mLoading && position == getItemCount() - 1;
            // String comingFrom = holder.vDescription.getText().toString();
            // String prefix = "[" + position + " / " + (getItemCount() - 1) + "] ";
            // if (loading) prefix += "[LOADING] ";
            // Log.d("VFY", prefix + item.description + " (from: " + comingFrom + ")");

            holder.bind(item);
            holder.setLoading(vMusicPickerList.mLoading && position == getItemCount() - 1);
            vMusicPickerList.mLoadingViewHolders.add(holder);
        }
    }

    private static class MusicPickerListItemHolder extends RecyclerView.ViewHolder {

        private final ImageView vImage;
        private final TextView vTitle;
        private final TextView vDescription;
        private final ProgressBar vLoading;

        public MusicPickerListItemHolder(View itemView) {
            super(itemView);
            vImage = (ImageView) itemView.findViewById(R.id.item_music_picker_image);
            vTitle = (TextView) itemView.findViewById(R.id.item_music_picker_title);
            vDescription = (TextView) itemView.findViewById(R.id.item_music_picker_description);
            vLoading = (ProgressBar) itemView.findViewById(R.id.item_music_picker_loading);
        }

        public void bind(Item item) {
            vImage.setImageDrawable(item.image);
            vTitle.setText(item.title);
            vDescription.setText(item.description);
        }

        public void setLoading(boolean loading) {
            vLoading.setVisibility((loading) ? View.VISIBLE : View.GONE);
        }
    }

    public static class Item {

        public String title;
        public String description;
        public Drawable image;

        public Item(String title, String description, Drawable image) {
            this.title = title;
            this.description = description;
            this.image = image;
        }
    }
}
