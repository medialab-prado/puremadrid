package com.albaitdevs.puremadrid.data;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;

import com.google.gson.Gson;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;
import com.puremadrid.api.pureMadridApi.model.JsonMap;
import com.puremadrid.core.model.Compuesto;
import com.puremadrid.core.model.Station;
import com.puremadrid.core.utils.GlobalUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry.COLUMN_AVISO_LEVEL_NOW;
import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry.COLUMN_AVISO_MAX_LEVEL_TODAY;
import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry.COLUMN_AVISO_STATE;
import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry.COLUMN_BASE_STATION;
import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry.COLUMN_DATE;
import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry.COLUMN_PARTICLE;
import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry.COLUMN_SCENARIO_MANUAL_TOMORROW;
import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TODAY;
import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TOMORROW;

/**
 * Created by jdelgado on 25/08/2017.
 */

public class DataBaseLoader implements LoaderManager.LoaderCallbacks<Cursor>{

    private Activity mActivity;
    private DataBaseLoaderCallbacks mCallbacks;

    public interface DataBaseLoaderCallbacks{
        void onDBFinished(ApiMedicion medicion);
    }

    public DataBaseLoader(Activity activity, DataBaseLoaderCallbacks callbacks){
        mActivity = activity;
        mCallbacks = callbacks;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(mActivity,
                PureMadridContract.PollutionEntry.CONTENT_URI,
                PureMadridDbHelper.getAllColumns(),
                PureMadridContract.PollutionEntry.COLUMN_PARTICLE + " = ? ",
                new String[]{Compuesto.NO2.name()},
                PureMadridContract.PollutionEntry.COLUMN_DATE + " DESC LIMIT 1");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        ApiMedicion medicion = null;
        if (cursor.moveToFirst()){
            medicion = new ApiMedicion();
            medicion.setAviso(cursor.getString(cursor.getColumnIndex(COLUMN_AVISO_LEVEL_NOW)));
            medicion.setAvisoMaxToday(cursor.getString(cursor.getColumnIndex(COLUMN_AVISO_MAX_LEVEL_TODAY)));
            medicion.setAvisoState(cursor.getString(cursor.getColumnIndex(COLUMN_AVISO_STATE)));
            medicion.setEscenarioManualTomorrow(cursor.getString(cursor.getColumnIndex(COLUMN_SCENARIO_MANUAL_TOMORROW)));
            medicion.setEscenarioStateTomorrow(cursor.getString(cursor.getColumnIndex(COLUMN_SCENARIO_TOMORROW)));
            medicion.setEscenarioStateToday(cursor.getString(cursor.getColumnIndex(COLUMN_SCENARIO_TODAY)));

            // Date
            String dateString = cursor.getString(cursor.getColumnIndex(COLUMN_DATE));
            SimpleDateFormat formatter = new SimpleDateFormat(PureMadridDbHelper.databaseDateFormat);
            Date date = null;

            try {
                date = formatter.parse(dateString);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(date.getTime());
            } catch (ParseException e) {

            }

            // Add stations
            JsonMap valuesMap = new JsonMap();
            Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
            for (Station station : stations){
                String stationId = COLUMN_BASE_STATION + String.format("%02d", station.getId());
                int value = cursor.getInt(cursor.getColumnIndex(stationId));
                valuesMap.put(stationId,value);
            }

            switch (Compuesto.withName(cursor.getString(cursor.getColumnIndex(COLUMN_PARTICLE)))){
                case NO2: medicion.setNo2(valuesMap); break;
                case CO: medicion.setCoValues(valuesMap); break;
                case SO2: medicion.setSo2values(valuesMap); break;
                case O3: medicion.setO3values(valuesMap); break;
                case TOL: medicion.setTolValues(valuesMap); break;
                case BEN: medicion.setBenValues(valuesMap); break;
                case PM2_5: medicion.setPm25values(valuesMap); break;
                case PM10: medicion.setPm10values(valuesMap); break;
            }
        }
        mCallbacks.onDBFinished(medicion);
        cursor.close();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
