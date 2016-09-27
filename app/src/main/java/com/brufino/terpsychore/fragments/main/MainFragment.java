package com.brufino.terpsychore.fragments.main;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.brufino.terpsychore.activities.MainActivity;

import static com.google.common.base.Preconditions.checkState;

public abstract class MainFragment extends Fragment {

    private FloatingActionButton vFab;
    private MainActivity mMainActivity;

    public FloatingActionButton onCreateFab(LayoutInflater inflater, ViewGroup parent) {
        return null;
    }

    protected FloatingActionButton getFab() {
        if (vFab == null) {
            vFab = onCreateFab(LayoutInflater.from(getContext()), mMainActivity.getFabParent());
            if (vFab != null) {
                vFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onFabClick(vFab);
                    }
                });
            }
        }
        return vFab;
    }

    protected void onFabClick(FloatingActionButton fab) {
        /* Override */
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        checkState(getActivity() instanceof MainActivity, "MainFragment can only be attached to MainActivity");
        mMainActivity = (MainActivity) getActivity();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMainActivity.setFab(getFab());
    }
}
