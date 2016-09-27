package com.brufino.terpsychore.fragments.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.brufino.terpsychore.R;

public class MusicInboxFragment extends MainFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_inbox, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Music Inbox");
    }
}
