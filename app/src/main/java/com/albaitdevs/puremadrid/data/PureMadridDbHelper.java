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

package com.albaitdevs.puremadrid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;
import com.puremadrid.api.pureMadridApi.model.JsonMap;
import com.puremadrid.core.model.Compuesto;
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

    private static final int VERSION = 2;
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
                PureMadridContract.PollutionEntry.COLUMN_DATE    + " TEXT NOT NULL " +
                createStationColumns() +
                ", UNIQUE ( " + PureMadridContract.PollutionEntry.COLUMN_DATE + " , " + PureMadridContract.PollutionEntry.COLUMN_PARTICLE + " ) ON CONFLICT REPLACE" +
                ");";

        db.execSQL(CREATE_TABLE);
    }

    private String createStationColumns() {
        String result = "";
        Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
        for (Station station : stations){
            result += ", " + PureMadridContract.PollutionEntry.COLUMN_BASE_STATION + String.format("%02d", station.getId())  + " REAL NOT NULL";
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

        for (Compuesto compuesto : Compuesto.measuredCompuestos()) {

            ContentValues contentValues = createContentValuesBasics(result);
            JsonMap valuesMap = getCompuesto(compuesto, result);
            if (valuesMap == null){
                continue;
            }

            //Stations
            Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
            for (Station station : stations) {
                String stationId = PureMadridDbHelper.ESTACION_PREFIX + String.format("%02d", station.getId());
                Object valueObject = valuesMap.get(stationId);
                if (valueObject == null) {
                    contentValues.put(PureMadridContract.PollutionEntry.COLUMN_BASE_STATION + String.format("%02d", station.getId()), -1);
                } else if (valueObject instanceof BigDecimal) {
                    BigDecimal bigDecimal = (BigDecimal) valueObject;
                    contentValues.put(PureMadridContract.PollutionEntry.COLUMN_BASE_STATION + String.format("%02d", station.getId()), bigDecimal.doubleValue());
                } else {
                    contentValues.put(PureMadridContract.PollutionEntry.COLUMN_BASE_STATION + String.format("%02d", station.getId()), -1);
                }
            }

            contentValues.put(PureMadridContract.PollutionEntry.COLUMN_PARTICLE, compuesto.name());

            // Insert
            context.getContentResolver().insert(PureMadridContract.PollutionEntry.CONTENT_URI, contentValues);

        }
    }

    private static ContentValues createContentValuesBasics(ApiMedicion result) {
        // If there is a result
        ContentValues contentValues = new ContentValues();
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_AVISO_LEVEL_NOW, result.getAviso());
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_AVISO_MAX_LEVEL_TODAY, result.getAvisoMaxToday());
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_AVISO_STATE, result.getAvisoState());
        if (result.getEscenarioManualTomorrow() != null) {
            contentValues.put(PureMadridContract.PollutionEntry.COLUMN_SCENARIO_MANUAL_TOMORROW, result.getEscenarioManualTomorrow());
        } else {
            contentValues.put(PureMadridContract.PollutionEntry.COLUMN_SCENARIO_MANUAL_TOMORROW, "null");
        }
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TOMORROW, result.getEscenarioStateTomorrow());
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_SCENARIO_TODAY, result.getEscenarioStateToday());

        // Date
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendar.setTimeInMillis(result.getMeasuredAt());
        SimpleDateFormat formatter = new SimpleDateFormat(PureMadridDbHelper.databaseDateFormat); //Or whatever format fits best your needs.
        String dateStr = formatter.format(calendar.getTime());
        // Put data
        contentValues.put(PureMadridContract.PollutionEntry.COLUMN_DATE, dateStr);
        return contentValues;
    }

    private static JsonMap getCompuesto(Compuesto compuesto, ApiMedicion medicion) {
        switch (compuesto){
            case CO: return medicion.getCoValues();
            case NO2: return medicion.getNo2();
            case SO2: return medicion.getSo2values();
            case O3: return medicion.getO3values();
            case TOL: return medicion.getTolValues();
            case BEN: return medicion.getBenValues();
            case PM2_5: return medicion.getPm25values();
            case PM10: return medicion.getPm10values();
        }
        return null;
    }

    public static ApiMedicion getLastMeasureNO2(Context context){
        Cursor cursor = context.getContentResolver().query(PureMadridContract.PollutionEntry.CONTENT_URI,
                PureMadridDbHelper.getAllColumns(),
                PureMadridContract.PollutionEntry.COLUMN_PARTICLE + " =?",
                new String[]{Compuesto.NO2.name()},
                PureMadridContract.PollutionEntry.COLUMN_DATE + " DESC LIMIT 1");

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
                calendar.setTime(date);
                medicion.setMeasuredAt(calendar.getTimeInMillis());
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
            medicion.setNo2(valuesMap);

        }

        cursor.close();
        return medicion;

    }

    public static void updateLastMeasure(Context context, ApiMedicion newMedicion){
        Cursor cursor = context.getContentResolver().query(PureMadridContract.PollutionEntry.CONTENT_URI,
                PureMadridDbHelper.getAllColumns(),
                PureMadridContract.PollutionEntry.COLUMN_PARTICLE + " = ?",
                new String[]{Compuesto.NO2.name()},
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
