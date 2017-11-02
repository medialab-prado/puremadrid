package com.albaitdevs.puremadrid.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.activities.MainActivity;
import com.albaitdevs.puremadrid.adapters.MainRecyclerAdapter;
import com.albaitdevs.puremadrid.data.DataBaseLoader;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.albaitdevs.puremadrid.data.DataBaseLoader.LOADER_LAST_MEASURE;

public class MainFragment extends Fragment implements DataBaseLoader.DataBaseLoaderCallbacks {

    private MainRecyclerAdapter mAdapter;
    @BindView(R.id.recycler_main) RecyclerView mRecyclerView;
    private DataBaseLoader mCallbacks;
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
    public void onStart() {
        super.onStart();

        // Load last measure
        mCallbacks = new DataBaseLoader(getActivity(),this);
        getLoaderManager().restartLoader(LOADER_LAST_MEASURE, null, mCallbacks);

        mNavigationCallback.onItemChanged(MainActivity.POSITION_MAIN);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this,view);

        //Layout
        mRecyclerView.setHasFixedSize(false);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Set up adapter
        mAdapter = new MainRecyclerAdapter(getActivity(),null);
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        getLoaderManager().destroyLoader(LOADER_LAST_MEASURE);
    }

    public void updateData(ApiMedicion currentPollution) {
        mAdapter.setData(currentPollution);
    }

    @Override
    public void onDBFinished(ApiMedicion medicion) {
        updateData(medicion);
    }


}
