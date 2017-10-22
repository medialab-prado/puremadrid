package com.albaitdevs.puremadrid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;
import com.puremadrid.api.pureMadridApi.model.JsonMap;
import com.puremadrid.core.model.Station;
import com.puremadrid.core.utils.GlobalUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
import static com.albaitdevs.puremadrid.data.PureMadridContract.PollutionEntry._ID;

public class PureMadridDbHelper extends SQLiteOpenHelper {

    public static final String databaseDateFormat = "yyyy-MM-dd HH:mm";

    private static final String DATABASE_NAME = "pollution.db";

    private static final int VERSION = 1;
    public static String ESTACION_PREFIX = "estacion_";

    PureMadridDbHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create tasks table (careful to follow SQL formatting rules)
        final String CREATE_TABLE = "CREATE TABLE "  + PureMadridContract.PollutionEntry.TABLE_NAME + " (" +
                PureMadridContract.PollutionEntry._ID                + " INTEGER PRIMARY KEY, " +
                PureMadridContract.PollutionEntry.COLUMN_AVISO_LEVEL_NOW + " TEXT NOT NULL, " +
                COLUMN_AVISO_MAX_LEVEL_TODAY + " TEXT NOT NULL, " +
                PureMadridContract.PollutionEntry.COLUMN_AVISO_STATE + " TEXT NOT NULL, " +
                PureMadridContract.PollutionEntry.COLUMN_PARTICLE + " TEXT NOT NULL, " +
                PureMadridContract.PollutionEntry.COLUMN_SCENARIO_MANUAL_TOMORROW + " TEXT NOT NULL, " +
                PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TOMORROW + " TEXT NOT NULL, " +
                PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TODAY    + " TEXT NOT NULL, " +
                PureMadridContract.PollutionEntry.COLUMN_DATE    + " TEXT NOT NULL UNIQUE " +
                createStationColumns() +
                ");";

        db.execSQL(CREATE_TABLE);
    }

    private String createStationColumns() {
        String result = "";
        Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
        for (Station station : stations){
            result += ", " + PureMadridContract.PollutionEntry.COLUMN_BASE_STATION + String.format("%02d", station.getId())  + " INTEGER NOT NULL";
        }
        return result;
    }

    /**
     * This method discards the old table of data and calls onCreate to recreate a new one.
     * This only occurs when the version number for this database (DATABASE_VERSION) is incremented.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PureMadridContract.PollutionEntry.TABLE_NAME);
        onCreate(db);
    }

    public static void addMeasure(Context context, ApiMedicion result){
        if (result == null){
            return;
        }

        // If there is a result
        ContentValues contentValues = new ContentValues();
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_AVISO_LEVEL_NOW, result.getAviso());
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_AVISO_MAX_LEVEL_TODAY, result.getAvisoMaxToday());
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_AVISO_STATE, result.getAvisoState());
        String particle =  result.getCompuesto();
        if (particle == null){
            // If info is not returned by the Api, set default
            particle = "NO2";
        }
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_PARTICLE, particle);
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_SCENARIO_MANUAL_TOMORROW, result.getEscenarioManualTomorrow());
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TOMORROW, result.getEscenarioStateTomorrow());
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TODAY, result.getEscenarioStateToday());

        // Date
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendar.setTimeInMillis(result.getMeasuredAt());
        SimpleDateFormat formatter = new SimpleDateFormat(PureMadridDbHelper.databaseDateFormat); //Or whatever format fits best your needs.
        String dateStr = formatter.format(calendar.getTime());
        // Put data
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_DATE, dateStr);

        //Stations
        Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
        for (Station station : stations){
            String stationId = PureMadridDbHelper.ESTACION_PREFIX + String.format("%02d", station.getId());
            Object valueObject = result.getNo2().get(stationId);
            int stationValue;
            if (valueObject == null) {
                stationValue = -1;
            } else if (valueObject instanceof BigDecimal){
                stationValue = ((BigDecimal) valueObject).intValueExact();
            } else {
                stationValue = (int) valueObject;
            }
            contentValues.put(PureMadridContract.PollutionEntry.COLUMN_BASE_STATION + String.format("%02d", station.getId()), stationValue);
        }

        // Delete
        context.getContentResolver().insert(PureMadridContract.PollutionEntry.CONTENT_URI, contentValues);
    }

    public static ApiMedicion getLastMeasureNO2(Context context){
        Cursor cursor = context.getContentResolver().query(PureMadridContract.PollutionEntry.CONTENT_URI,
                PureMadridDbHelper.getAllColumns(),
                PureMadridContract.PollutionEntry.COLUMN_PARTICLE + " =?",
                new String[]{"NO2"},
                PureMadridContract.PollutionEntry.COLUMN_DATE + " DESC LIMIT 1");

        ApiMedicion medicion = null;
        if (cursor.moveToFirst()){
            medicion = new ApiMedicion();
            medicion.setAviso(cursor.getString(cursor.getColumnIndex(COLUMN_AVISO_LEVEL_NOW)));
            medicion.setAvisoMaxToday(cursor.getString(cursor.getColumnIndex(COLUMN_AVISO_MAX_LEVEL_TODAY)));
            medicion.setAvisoState(cursor.getString(cursor.getColumnIndex(COLUMN_AVISO_STATE)));
            medicion.setCompuesto(cursor.getString(cursor.getColumnIndex(COLUMN_PARTICLE)));
            medicion.setEscenarioManualTomorrow(cursor.getString(cursor.getColumnIndex(COLUMN_SCENARIO_MANUAL_TOMORROW)));
            medicion.setEscenarioStateTomorrow(cursor.getString(cursor.getColumnIndex(COLUMN_SCENARIO_TOMORROW)));
            medicion.setEscenarioStateToday(cursor.getString(cursor.getColumnIndex(COLUMN_SCENARIO_TODAY)));

            // Date
            String dateString = cursor.getString(cursor.getColumnIndex(COLUMN_DATE));
            SimpleDateFormat formatter = new SimpleDateFormat(PureMadridDbHelper.databaseDateFormat);
            Date date = null;
            try {
                date = formatter.parse(dateString);
                medicion.setMeasuredAt(date.getTime());
            } catch (ParseException e) {
                medicion.setMeasuredAt(0L);
            }

            // Add stations
            JsonMap valuesMap = new JsonMap();
            Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
            for (Station station : stations){
                String stationId = COLUMN_BASE_STATION + String.format("%02d", station.getId());
                int value = cursor.getInt(cursor.getColumnIndex(stationId));
                valuesMap.put(stationId,value);
            }
            medicion.setNo2(valuesMap);

        }

        cursor.close();
        return medicion;

    }

    public static void updateLastMeasure(Context context, ApiMedicion newMedicion){
        Cursor cursor = context.getContentResolver().query(PureMadridContract.PollutionEntry.CONTENT_URI,
                PureMadridDbHelper.getAllColumns(),
                null,
                null,
                PureMadridContract.PollutionEntry.COLUMN_DATE + " DESC LIMIT 1");

        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndex(_ID));

            ContentValues contentValues = new ContentValues();
            contentValues.put(PureMadridContract.PollutionEntry.COLUMN_AVISO_LEVEL_NOW, newMedicion.getAviso());
            contentValues.put(PureMadridContract.PollutionEntry.COLUMN_AVISO_MAX_LEVEL_TODAY, newMedicion.getAvisoMaxToday());
            contentValues.put(PureMadridContract.PollutionEntry.COLUMN_AVISO_STATE, newMedicion.getAvisoState());
            contentValues.put(PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TOMORROW, newMedicion.getEscenarioStateTomorrow());
            contentValues.put(PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TODAY, newMedicion.getEscenarioStateToday());
            //
            int result = context.getContentResolver().update(PureMadridContract.PollutionEntry.CONTENT_URI,
                    contentValues,
                    "_ID = ?",
                    new String[]{Long.toString(id)}
            );

        }
        cursor.close();
    }


    public static String[] getAllColumns() {
        List<String> columns = new ArrayList<>();
        columns.add(_ID);
        columns.add(COLUMN_AVISO_LEVEL_NOW);
        columns.add(COLUMN_AVISO_MAX_LEVEL_TODAY);
        columns.add(COLUMN_AVISO_STATE);
        columns.add(COLUMN_PARTICLE);
        columns.add(COLUMN_SCENARIO_MANUAL_TOMORROW);
        columns.add(COLUMN_SCENARIO_TOMORROW);
        columns.add(COLUMN_SCENARIO_TODAY);
        columns.add(COLUMN_DATE);

        // Add stations
        Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
        for (Station station : stations){
            String stationId = PureMadridContract.PollutionEntry.COLUMN_BASE_STATION + String.format("%02d", station.getId());
            columns.add(stationId);
        }

        //
        return columns.toArray(new String[0]);

    }
}
