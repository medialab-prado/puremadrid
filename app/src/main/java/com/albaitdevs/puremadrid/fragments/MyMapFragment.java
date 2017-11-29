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

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.activities.MainActivity;
import com.albaitdevs.puremadrid.data.DataBaseLoader;
import com.albaitdevs.puremadrid.data.PureMadridContract;
import com.albaitdevs.puremadrid.downloaders.GetLastStatusAsync;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.gson.Gson;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;
import com.puremadrid.api.pureMadridApi.model.JsonMap;
import com.puremadrid.core.model.Compuesto;
import com.puremadrid.core.model.Station;
import com.puremadrid.core.utils.GlobalUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.albaitdevs.puremadrid.data.DataBaseLoader.LOADER_LAST_MEASURE;
import static com.puremadrid.core.model.Compuesto.*;

public class MyMapFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener, GoogleMap.OnMyLocationButtonClickListener, GetLastStatusAsync.ApiListener, DatePickerFragment.OnDateTimeSetListener, DataBaseLoader.DataBaseLoaderCallbacks {

    private static final String KEY_MAPVIEW_SAVE_INSTANCE = "mapViewSaveState";
    private static final float BASE_ZOOM = 12.0f;
    private static final String KEY_SHOW_HEATMAP = "key_show_heatmap";
    private static final String KEY_SHOW_STATIONS = "key_show_stations";
    private MainActivity mNavigationCallback;
    private ApiMedicion currentPollution;
    @BindView(R.id.map_progressabar) ProgressBar progressBar;
    @BindView(R.id.fab_open_calendar)  FloatingActionButton openMapButton;
    @BindView(R.id.fab_layers)  FloatingActionButton fabLayer;
    @BindView(R.id.coordinator_map) CoordinatorLayout coordinatorLayout;
    private DataBaseLoader mCallbacks;

    //Needed to handle Google Play Services Location
    public static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private static GoogleApiClient googleApiClient;

    private GoogleMap map;
    private MapView mMapView;
    private View view;

    private TileOverlay mOverlay = null;
    private HeatmapTileProvider mProvider = null;
    private static final double TILE_RADIUS_BASE = 1.47;
    private int mRadiusZoom = (int) BASE_ZOOM;
    private boolean showHeatMap = true;
    private boolean showStations = true;

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
    public void onCreate(Bundle savedInstanceState) {
        final Bundle mapViewSavedInstanceState = savedInstanceState != null ? savedInstanceState.getBundle(KEY_MAPVIEW_SAVE_INSTANCE) : null;
        super.onCreate(mapViewSavedInstanceState);

        // Load last measure
        mCallbacks = new DataBaseLoader(getActivity(),this);
        if (mMapView != null) {
            mMapView.onCreate(savedInstanceState);
        }

        if (savedInstanceState != null) {
            showHeatMap = savedInstanceState.getBoolean(KEY_SHOW_HEATMAP);
            showStations = savedInstanceState.getBoolean(KEY_SHOW_STATIONS);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mNavigationCallback.onItemChanged(MainActivity.POSITION_MAP);
        getLoaderManager().restartLoader(LOADER_LAST_MEASURE, null, mCallbacks);

        if (mMapView != null) {
            mMapView.onStart();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_map, container, false);
            ButterKnife.bind(this,view);
        }

        // Load Map
        mMapView = view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();
        mMapView.getMapAsync(this);

        //Open Calendar Action
        openMapButton.setOnClickListener(this);
        fabLayer.setOnClickListener(this);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(savedInstanceState);
        }
        savedInstanceState.putBoolean(KEY_SHOW_HEATMAP,showHeatMap);
        savedInstanceState.putBoolean(KEY_SHOW_STATIONS,showStations);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
        getLoaderManager().destroyLoader(LOADER_LAST_MEASURE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap map) {

        this.map = map;

        CameraUpdate cu;
        LatLng madridCenter = new LatLng(40.4169335, -3.7083759);
        final float[] zoom = {BASE_ZOOM};
        cu = CameraUpdateFactory.newLatLngZoom(madridCenter, zoom[0]);
        map.moveCamera(cu);
        map.getUiSettings().setMapToolbarEnabled(false);

        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {


            @Override
            public void onCameraChange(CameraPosition pos) {
                if (mProvider != null && mOverlay != null) {
                    int currentZoom = (int) Math.floor(pos.zoom);
                    if (currentZoom != mRadiusZoom) {
                        mRadiusZoom = currentZoom;
                        int radius = (int) Math.floor(Math.pow(TILE_RADIUS_BASE, mRadiusZoom));
                        mProvider.setRadius(radius);
                        mOverlay.clearTileCache();
                    }
                }
            }
        });

        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View v = getActivity().getLayoutInflater().inflate(R.layout.map_marker, null);
                TextView info= v.findViewById(R.id.marker_info);
                info.setText(marker.getSnippet());
                TextView title= v.findViewById(R.id.marker_title);
                title.setText(marker.getTitle());
                return v;
            }
        });
        
        //Check permissions
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            map.setOnMyLocationButtonClickListener(this);
            map.setMyLocationEnabled(true);
        } else {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                //Explain the permission
                Snackbar snack = Snackbar.make(coordinatorLayout,
                        getResources().getString(R.string.permission_ratioanale_snack),
                        Snackbar.LENGTH_LONG)
                        .setAction(getResources().getString(R.string.permission_snack_ok), this);
                snack.show();

            } else {
                // No explanation needed, we can request the permission.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
            }
        }

    }

    private void loadHeatMap() {
        List<LatLng> list = new ArrayList<>();
        List<WeightedLatLng> weightedStations = new ArrayList<>();

        // Get the data: latitude/longitude positions of police stations.
        Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
        for (Station station : stations) {
            String stationId = PureMadridContract.PollutionEntry.COLUMN_BASE_STATION + String.format("%02d", station.getId());

            LatLng latLng = new LatLng(station.getLatitud_decimal(), station.getLongitud_decimal());
            list.add(latLng);
            //
            if (currentPollution == null){
                return;
            }

            Object valueObject = currentPollution.getNo2().get(stationId);
            double stationValue;
            if (valueObject == null) {
                stationValue = 0;
            } else if (valueObject instanceof BigDecimal){
                stationValue = ((BigDecimal) valueObject).doubleValue();
            } else if (valueObject instanceof Double){
                stationValue = (double) valueObject;
            } else {
                stationValue = -1;
            }
            if (stationValue < 0){
                stationValue = 0;
            }

            stationValue /= 400; // Supposing 400 as MAX
            stationValue *= 0.8f; // Setting max

            WeightedLatLng weightedLatLng = new WeightedLatLng(latLng,stationValue);
            weightedStations.add(weightedLatLng);
        }

        // Set gradient
        int[] colors = {
                Color.rgb(79, 195, 247), // blue
                Color.rgb(255, 235, 59), // yellow
                Color.rgb(255, 152, 0), // orange
                Color.rgb(244, 67, 54)    // red
        };
        float[] startPoints = {
                0.2f, 0.8f, 0.9f, 1.0f
        };
        Gradient gradient = new Gradient(colors, startPoints);

        // Create a heat map tile provider, passing it the latlngs of the police stations.
        int radius = (int) Math.floor(Math.pow(TILE_RADIUS_BASE, mRadiusZoom));
        mProvider = new HeatmapTileProvider.Builder()
                .weightedData(weightedStations)
                .gradient(gradient)
                .opacity(0.6f)
                .build();
        mProvider.setRadius(radius);
        if (mOverlay != null) {
            mOverlay.clearTileCache();
        }
        // Add a tile overlay to the map, using the heat map tile provider.
        mOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }

    private void loadMarkers() {
        Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
        for (Station station : stations) {
            LatLng latLng = new LatLng(station.getLatitud_decimal(), station.getLongitud_decimal());
            String snippet = "";
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
            if (currentPollution != null) {
                String stationId = PureMadridContract.PollutionEntry.COLUMN_BASE_STATION + String.format("%02d", station.getId());

                double no2Value = getValues(NO2,stationId);
                if (no2Value > 0) {
                    snippet += NO2.name() + ": " + no2Value + " µg/m3";
                }
                snippet += addStringValue(CO,stationId);
                snippet += addStringValue(O3,stationId);
                snippet += addStringValue(SO2,stationId);
                snippet += addStringValue(PM2_5,stationId);
                snippet += addStringValue(PM10,stationId);
                snippet += addStringValue(BEN,stationId);
                snippet += addStringValue(TOL,stationId);

                //
                if (no2Value > 400) {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                } else if (no2Value > 200) {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
                } else if (no2Value > 150) {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
                }
                if (snippet.equals("")) {
                    snippet = getString(R.string.no_data_title);
                }

            }
            map.addMarker(
                    new MarkerOptions()
                            .icon(icon)
                            .title(station.getNombre())
                            .snippet(snippet)
                            .position(latLng));
        }
    }

    private String addStringValue(Compuesto compuesto, String stationId) {
        double value = getValues(compuesto, stationId);
        String unit = "µg/m3";
        if (compuesto == CO){
            unit = "mg/m3";
        }
        if (value > 0) {
            return "\n" + compuesto.name() + ": " + value + " " + unit;
        }
        return "";
    }

    /**
     * On click for AppcompatButtons
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //Permissions from snackbar
            case R.id.snackbar_action:
                showRationalePermissionAlertDialog();
                break;

            case R.id.fab_open_calendar:
                //Date Picker Button
                DatePickerFragment newFragment = new DatePickerFragment();
                newFragment.setListener(this);
                newFragment.show(getFragmentManager(), "PickerFragmentDialog");
                break;

            case R.id.fab_layers:
                //Date Picker Button
                openOptionsDialog();
                break;
        }
    }

    private void openOptionsDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.map_options));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        builder.setMultiChoiceItems(new String[]{
                        getString(R.string.checkbox_heatmap),
                        getString(R.string.checkbox_stations)},
                new boolean[]{showHeatMap, showStations},
                new DialogInterface.OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                switch (which){
                    case 0: showHeatMap = isChecked; break;
                    case 1: showStations = isChecked; break;
                }
                loadItems();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showRationalePermissionAlertDialog() {
        //Create alert
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.need_permission_location_message)
                .setTitle(R.string.need_permission_title);
        builder.setPositiveButton(R.string.give_permission, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                Toast.makeText(getActivity(),R.string.not_showing_location,Toast.LENGTH_LONG).show();            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }


    /**
     * Handle permissions response
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    map.setOnMyLocationButtonClickListener(this);
                    map.setMyLocationEnabled(true);

                } else {
                    // permission denied

                    Toast.makeText(getActivity(),R.string.not_showing_location,Toast.LENGTH_LONG).show();
                }
                break;


        }
    }

    /**
     * Handles Google Play Services Permissions
     *
     * @return
     */
    @Override
    public boolean onMyLocationButtonClick() {
        handleLocationPermissionForGooglePlayServices();
        return false;
    }


    private void handleLocationPermissionForGooglePlayServices() {

        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        //**************************
        builder.setAlwaysShow(true);
        //**************************

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {

                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });

    }

    /**
     * Handle action after receiving the resquest location intent
     *
     * @param resultCode
     */
    public void onRequestLocationResult(int resultCode) {

        switch (resultCode) {
            case Activity.RESULT_OK:
                //Ask for new versions
                new CenterMapInLocation().execute();

                break;
            case Activity.RESULT_CANCELED:

                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Handle each result by its own fragment
        switch (requestCode) {
            case MyMapFragment.REQUEST_CHECK_SETTINGS:
                onRequestLocationResult(resultCode);
                break;
        }
    }

    @Override
    public void onApiFinished(ApiMedicion result) {
        // ProgressBar
        progressBar.setVisibility(View.INVISIBLE);

        //Load data if database is empty
        currentPollution = result;
        if (result == null){
            Toast.makeText(getActivity(),getString(R.string.no_data_for_date),Toast.LENGTH_LONG).show();
        } else {
            if (map != null) {
                loadItems();
            }
        }
    }

    private void loadItems() {
        map.clear();
        if (showHeatMap) {
            loadHeatMap();
        }
        if (showStations) {
            loadMarkers();
        }

    }

    public double getValues(Compuesto compuesto, String stationId) {
        JsonMap values;
        switch (compuesto){
            case NO2: values = currentPollution.getNo2(); break;
            case CO: values = currentPollution.getCoValues(); break;
            case O3: values = currentPollution.getO3values(); break;
            case BEN: values = currentPollution.getBenValues(); break;
            case TOL: values = currentPollution.getTolValues(); break;
            case SO2: values = currentPollution.getSo2values(); break;
            case PM2_5: values = currentPollution.getPm25values(); break;
            case PM10: values = currentPollution.getPm10values(); break;
            default: return -1;
        }
        // Get value from station
        if (values == null){
            return -1;
        }
        Object valueObject = values.get(stationId);
        double stationValue;
        if (valueObject == null) {
            stationValue = -1;
        } else if (valueObject instanceof BigDecimal){
            stationValue = ((BigDecimal) valueObject).doubleValue();
        } else if (valueObject instanceof Double){
            stationValue = (Double) valueObject;
        } else {
            stationValue = (int) valueObject;
        }
        return stationValue;
    }

    /**
     * Wait until location is availbale AND not block UI
     */
    private class CenterMapInLocation extends AsyncTask<String, String, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            android.location.Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            int times = 0;
            while (location == null && times < 20){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                }
                location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                times++;
            }
            if (location != null) {
                final CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                        .zoom(14.0f).build();
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(position));
                    }
                });
            }
            return null;
        }
    }

    @Override
    public void onDBFinished(ApiMedicion medicion) {
        currentPollution = medicion;
        if (map != null) {
             loadItems();
        }
    }

    @Override
    public void onDateTimeSet(Calendar timeSet) {

        // Request
        progressBar.setVisibility(View.VISIBLE);
        new GetLastStatusAsync(getActivity(),this, timeSet.getTime()).execute();
    }
}
