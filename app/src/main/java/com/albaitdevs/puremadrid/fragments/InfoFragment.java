/*
 * Copyright (C) 2017 Javier Delgado Aylagas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.albaitdevs.puremadrid.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.activities.MainActivity;


public class InfoFragment extends Fragment implements View.OnClickListener {
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
        mNavigationCallback.onItemChanged(MainActivity.POSITION_INFO);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Set the text view as the activity layout
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        this.view = view;

        //Set actions for buttons
        Button button_calaire = view.findViewById(R.id.link_calaire);
        Button button_rateus = view.findViewById(R.id.link_google_play);
        Button button_facebook = view.findViewById(R.id.link_facebook);

        button_calaire.setOnClickListener(this);
        button_rateus.setOnClickListener(this);
        button_facebook.setOnClickListener(this);

        TextView textAppDdisenada = view.findViewById(R.id.text_app_disenada);
        textAppDdisenada.setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }


    /**
     * Onclic Listener for buttons
     *
     * @param v
     */
    @Override
    public void onClick(View v) {

        //Create chooser
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        //Prepare info for every button
        String url = "";
        String action = "DEFAULT";
        switch (v.getId()){
            case R.id.link_calaire:
                url = getResources().getString(R.string.link_calaire_url);
                action = "Goto_urbanoSoria";
                break;
            case R.id.link_google_play:
                url = getResources().getString(R.string.link_googleplay_rateus_url);
                action = "Goto_googleplay_rateus";
                break;
            case R.id.link_facebook:
                url = getResources().getString(R.string.link_facebook_url);
                action = "Goto_facebook";
                break;
        }

        intent.setData(Uri.parse(url));


        if (v.getId() == R.id.link_facebook){

            try {
                int versionCode = getActivity().getPackageManager().getPackageInfo("com.facebook.katana", 0).versionCode;
                if (versionCode >= 3002850) {
                    //newer versions of fb app
                    url = "fb://facewebmodal/f?href=https://www.facebook.com/albaitsoria";
                } else {
                    //older versions of fb app
                    url = "fb://page/421900594676221";
                }
                intent.setData(Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                url = getResources().getString(R.string.link_facebook_url);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }

        } else{
            startActivity(intent);

        }
    }

}
