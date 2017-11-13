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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.activities.MainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProtocolosFragment extends Fragment {

    @BindView(R.id.avisos_explanation) TextView avisosExplanation;
    @BindView(R.id.scenarios_explanation) TextView escenariosExplanation;
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
        mNavigationCallback.onItemChanged(MainActivity.POSITION_PROTOCOLO);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_protocolo, container, false);
        ButterKnife.bind(this,view);

        // Avisos details
        String avisosText = getString(R.string.preaviso_content) + "\n\n"
                + getString(R.string.aviso_content) + "\n\n"
                + getString(R.string.alerta_content);
        avisosExplanation.setText(avisosText);

        // Scenarios details
        String escenariosText = getString(R.string.scenario1_title) + "\n\n"
                + getString(R.string.scenario1_content) + "\n\n\n"
                + getString(R.string.scenario2_title) + "\n\n"
                + getString(R.string.scenario2_content) + "\n\n\n"
                + getString(R.string.scenario3_title) + "\n\n"
                + getString(R.string.scenario3_content) + "\n\n\n"
                + getString(R.string.scenario4_title) + "\n\n"
                + getString(R.string.scenario4_content);
        escenariosExplanation.setText(escenariosText);

        return view;
    }

}
