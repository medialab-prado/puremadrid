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
import java.util.TimeZone;

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

    public static final int LOADER_LAST_MEASURE = 123;

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
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {

        if (loaderId == LOADER_LAST_MEASURE) {

            ApiMedicion lastMeasure = PureMadridDbHelper.getLastMeasureNO2(mActivity);
            if (lastMeasure == null){
                mCallbacks.onDBFinished(null);
                return null;
            }

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            calendar.setTimeInMillis(lastMeasure.getMeasuredAt());
            SimpleDateFormat formatter = new SimpleDateFormat(PureMadridDbHelper.databaseDateFormat); //Or whatever format fits best your needs.
            String dateStr = formatter.format(calendar.getTime());

            return new CursorLoader(mActivity,
                    PureMadridContract.PollutionEntry.CONTENT_URI,
                    PureMadridDbHelper.getAllColumns(),
                    PureMadridContract.PollutionEntry.COLUMN_DATE + " = ? ",
                    new String[]{dateStr},
                    null);


        } else {
            mCallbacks.onDBFinished(null);
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        ApiMedicion medicion = new ApiMedicion();

        while (cursor.moveToNext()){

            Compuesto compuesto = Compuesto.withName(cursor.getString(cursor.getColumnIndex(COLUMN_PARTICLE)));

            if (compuesto == Compuesto.NO2) {
                medicion.setAviso(cursor.getString(cursor.getColumnIndex(COLUMN_AVISO_LEVEL_NOW)));
                medicion.setAvisoMaxToday(cursor.getString(cursor.getColumnIndex(COLUMN_AVISO_MAX_LEVEL_TODAY)));
                medicion.setAvisoState(cursor.getString(cursor.getColumnIndex(COLUMN_AVISO_STATE)));
                medicion.setEscenarioManualTomorrow(cursor.getString(cursor.getColumnIndex(COLUMN_SCENARIO_MANUAL_TOMORROW)));
                medicion.setEscenarioStateTomorrow(cursor.getString(cursor.getColumnIndex(COLUMN_SCENARIO_TOMORROW)));
                medicion.setEscenarioStateToday(cursor.getString(cursor.getColumnIndex(COLUMN_SCENARIO_TODAY)));
            }
            // Date
            String dateString = cursor.getString(cursor.getColumnIndex(COLUMN_DATE));
            SimpleDateFormat formatter = new SimpleDateFormat(PureMadridDbHelper.databaseDateFormat);
            try {
                Date date = formatter.parse(dateString);
                medicion.setMeasuredAt(date.getTime());
            } catch (ParseException e) {

            }

            // Add stations
            JsonMap valuesMap = new JsonMap();
            Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
            for (Station station : stations){
                String stationId = COLUMN_BASE_STATION + String.format("%02d", station.getId());
                double value = cursor.getDouble(cursor.getColumnIndex(stationId));
                valuesMap.put(stationId,value);
            }

            switch (compuesto){
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
