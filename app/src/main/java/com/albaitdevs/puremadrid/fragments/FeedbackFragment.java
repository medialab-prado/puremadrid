package com.albaitdevs.puremadrid.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.activities.MainActivity;


public class FeedbackFragment extends Fragment {
    private View view;
    private MainActivity mNavigationCallback;

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mNavigationCallback = (MainActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mNavigationCallback.onItemChanged(MainActivity.POSITION_FEEDBACK);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Set the text view as the activity layout
        View view =  inflater.inflate(R.layout.fragment_feedback, container, false);
        this.view = view;

        FloatingActionButton sendButton = view.findViewById(R.id.floating_button_send_feedback);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        LinearLayout linearLayout = view.findViewById(R.id.linearlayout_desc);
        linearLayout.setMinimumHeight((int) (1.6*((double) view.getResources().getDimension(R.dimen.abc_action_bar_default_height_material))));

        return view;
    }

    // Action buttons-------------------------------------------------
    private void sendMessage() {
        //Get message
        EditText editText = view.findViewById(R.id.feedback_edittext);

        //Create intent
        Intent intent = new Intent(android.content.Intent.ACTION_SENDTO);
        intent.setType("message/rfc822");
        intent.setData(Uri.parse(getResources().getString(R.string.feedback_email)));
        intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.feedback_subject));
        intent.putExtra(Intent.EXTRA_TEXT, editText.getText());
        intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.word_feedback)));

    }

}
