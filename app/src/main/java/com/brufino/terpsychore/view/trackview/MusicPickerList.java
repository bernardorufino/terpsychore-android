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
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.brufino.terpsychore.R;
import com.google.common.base.Function;

import java.util.LinkedList;
import java.util.List;

public class MusicPickerList<T> extends RelativeLayout {

    private final List<Item> mItems = new LinkedList<>();
    private RecyclerView vList;
    private LinearLayoutManager mLayoutManager;
    private Function<T, Item> mTransform;
    private MusicPickerListAdapter mAdapter;

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
        mAdapter = new MusicPickerListAdapter(mItems);
        vList.setLayoutManager(mLayoutManager);
        vList.setAdapter(mAdapter);
    }

    public void setTransform(Function<T, Item> transform) {
        mTransform = transform;
    }

    public void setList(List<T> list) {
        mItems.clear();
        for (T item : list) {
            Item transformedItem = mTransform.apply(item);
            mItems.add(transformedItem);
        }
        mAdapter.notifyDataSetChanged();
    }

    private static class MusicPickerListAdapter extends RecyclerView.Adapter<MusicPickerListItemHolder> {

        private List<Item> mBackingList;

        public MusicPickerListAdapter(List<Item> backingList) {
            mBackingList = backingList;
        }

        @Override
        public MusicPickerListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_music_picker_list, parent, false);
            return new MusicPickerListItemHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MusicPickerListItemHolder holder, int position) {
            Item item = mBackingList.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return mBackingList.size();
        }
    }

    private static class MusicPickerListItemHolder extends RecyclerView.ViewHolder {

        private final ImageView vImage;
        private final TextView vTitle;
        private final TextView vDescription;

        public MusicPickerListItemHolder(View itemView) {
            super(itemView);
            vImage = (ImageView) itemView.findViewById(R.id.item_music_picker_image);
            vTitle = (TextView) itemView.findViewById(R.id.item_music_picker_title);
            vDescription = (TextView) itemView.findViewById(R.id.item_music_picker_description);
        }

        public void bind(Item item) {
            vImage.setImageDrawable(item.image);
            vTitle.setText(item.title);
            vDescription.setText(item.description);
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
