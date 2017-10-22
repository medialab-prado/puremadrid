package com.albaitdevs.puremadrid.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.activities.MainActivity;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.TimeZone;

import static com.puremadrid.core.utils.GlobalUtils.intTwoDigits;
import static java.util.Calendar.DATE;


public class PredictionFragment extends Fragment {
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
    public void onResume(){
        super.onResume();

        mNavigationCallback.onItemChanged(MainActivity.POSITION_PREDICTION);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Set the text view as the activity layout
        View view = inflater.inflate(R.layout.fragment_prediction, container, false);
        this.view = view;

        // Get date
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        int yearOne = calendar.get(Calendar.YEAR);
        int monthOne = calendar.get(Calendar.MONTH) + 1;
        int dayOne = calendar.get(DATE);

        calendar.add(DATE,1);
        int yearTwo = calendar.get(Calendar.YEAR);
        int monthTwo = calendar.get(Calendar.MONTH) + 1;
        int dayTwo = calendar.get(DATE);

        calendar.add(DATE,-2);
        int yearYesterday = calendar.get(Calendar.YEAR);
        int monthYesterday = calendar.get(Calendar.MONTH) + 1;
        int dayYesteday = calendar.get(DATE);

        ImageView predicionToday = view.findViewById(R.id.prediction_today);
        ImageView predicionTomorrow = view.findViewById(R.id.prediction_tomorrow);

        String urlToday = getResources().getString(R.string.base_prediction_url) + yearOne + intTwoDigits(monthOne) + intTwoDigits(dayOne) + ".png";
        String urlTomorrow = getResources().getString(R.string.base_prediction_url) + yearTwo + intTwoDigits(monthTwo) + intTwoDigits(dayTwo) + ".png";
        String urlYesterday = getResources().getString(R.string.base_prediction_url) + yearYesterday + intTwoDigits(monthYesterday) + intTwoDigits(dayYesteday) + ".png";

        //Set texts
        TextView predicionTodayText = view.findViewById(R.id.prediction_today_text);
        TextView predicionTomorrowText = view.findViewById(R.id.prediction_tomorrow_text);

        if (calendar.get(Calendar.HOUR_OF_DAY) > 11) {
            Picasso.with(getActivity())
                    .load(urlToday)
                    .into(predicionToday);

            Picasso.with(getActivity())
                    .load(urlTomorrow)
                    .into(predicionTomorrow);

            predicionTodayText.setText(getString(R.string.predicion_text_start) + " " + dayOne +"/" + intTwoDigits(monthOne) + "/" + yearOne + " es:");
            predicionTomorrowText.setText(getString(R.string.predicion_text_start) + " " + dayTwo + "/" + intTwoDigits(monthTwo) + "/" + yearTwo + " es:");

        } else {
            Picasso.with(getActivity())
                    .load(urlYesterday)
                    .into(predicionToday);

            Picasso.with(getActivity())
                    .load(urlToday)
                    .into(predicionTomorrow);

            predicionTodayText.setText(getString(R.string.predicion_text_start) + " " + dayYesteday + "/" + intTwoDigits(monthYesterday) + "/" + dayYesteday + " es:");
            predicionTomorrowText.setText(getString(R.string.predicion_text_start) + " " + dayOne + "/" + intTwoDigits(monthOne) + "/" + yearOne + " es:");

        }



        return view;
    }

}
