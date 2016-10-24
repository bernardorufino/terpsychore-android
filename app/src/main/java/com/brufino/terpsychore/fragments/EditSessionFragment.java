package com.brufino.terpsychore.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.lib.EmojiFontTextWatcher;

public class EditSessionFragment extends DialogFragment {

    private OnSessionEditedListener mListener = null;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText name = (EditText) view.findViewById(R.id.session_name);
        /* TODO: EmojiFontTextWatcher not working */
        name.addTextChangedListener(new EmojiFontTextWatcher(getContext()));
        name.requestFocus();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener == null) return;
                        EditText name = (EditText) ((AlertDialog) dialog).findViewById(R.id.session_name);
                        mListener.onComplete(true, name.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener == null) return;
                        mListener.onComplete(false, null);
                    }
                })
                .setTitle("Session Name")
                .setView(R.layout.dialog_edit_session)
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    public void setOnSessionEditedListener(OnSessionEditedListener listener) {
        mListener = listener;
    }

    public static interface OnSessionEditedListener {
        public void onComplete(boolean success, String sessionName);
    }
}
