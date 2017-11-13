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

package com.puremadrid.api.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.repackaged.com.google.datastore.v1.PropertyFilter;
import com.puremadrid.api.MainServlet;
import com.puremadrid.api.core.Parser;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import com.puremadrid.core.model.ApiMedicion;
import com.puremadrid.core.model.ApiResponse;
import com.puremadrid.core.model.Compuesto;
import com.puremadrid.core.model.Constants;
import com.puremadrid.core.model.Medicion;
import com.puremadrid.core.utils.GlobalUtils;
import com.puremadrid.api.utils.EmailUtils;
import com.puremadrid.api.utils.NotificationUtils;

import static com.puremadrid.api.core.Parser.parseFromMissingDay;
import static com.puremadrid.core.model.ApiMedicion.Escenario.*;
import static com.puremadrid.core.model.ApiMedicion.Estado.*;
import static com.puremadrid.core.model.Compuesto.NO2;
import static com.puremadrid.core.utils.GlobalUtils.stringHour;

/**
 * Created by Delga on 17/11/2016.
 */

public class GetNewData extends MainServlet {

    private static final int HOUR_OF_REFERENCE = 0;
    private static final int HOURS_ADD_TO_HOUR = 1;
    private static final int REFERENCE_HOUR = 0;

    static String official_raw = "http://www.mambiente.munimadrid.es/opendata/horario.txt";

    private static final Logger mLogger = Logger.getLogger(GetNewData.class.getName());


    private boolean mEmailSent = false;



    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        mEmailSent = false;

        if (!checkSenderAuth(req, resp)){
            return;
        }

        Date lastSavedTime = getLastSavedTime();
        Calendar calendarSavedTime = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendarSavedTime.setTime(lastSavedTime);
        calendarSavedTime.setTimeZone(TimeZone.getTimeZone("CET"));
        mLogger.info("Last saved time is: " + calendarSavedTime.get(Calendar.HOUR_OF_DAY));

        // GET HORARIOS.TXT
        InputStream dataInputStream = null;
        try {
            URL url = new URL(official_raw);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(15000);
            dataInputStream = urlConnection.getInputStream();
        } catch (MalformedURLException e) {
            mLogger.severe(ApiResponse.MESSAGE_ERROR_FETCHING_URL);

            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_ERROR,ApiResponse.Errors.ERROR_FETCHING_REMOTE_URL);

            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());
            resp.setStatus(ApiResponse.HTTP_ERROR);
            return;
        } catch (SocketTimeoutException e) {
            mLogger.severe(ApiResponse.MESSAGE_TIMEOUT);
            if (GetNewData.isPureMadrid()) {
                EmailUtils.sendEmail("Horarios caido", "HORARIOS.TXT ESTA CAIDO");
            }
            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_ERROR,ApiResponse.Errors.ERROR_TIMEOUT_IN_MEDIOAMBIENTE);
            String jsonInString = apiResponse.buildJson();

            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(jsonInString);
            resp.setStatus(ApiResponse.HTTP_ERROR);
            return;
        }
        if (dataInputStream == null){
            mLogger.severe("Timeout fecthing data");

            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_ERROR,ApiResponse.Errors.ERROR_MEDIOAMBIENTE_INPUT_NULL);

            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());
            resp.setStatus(ApiResponse.HTTP_ERROR);
            return;
        }

        List<Medicion> formattedData = null;
        try {
            formattedData = Parser.parseFromHorarios(calendarSavedTime, dataInputStream);
        } catch (IOException e) {
            e.printStackTrace();
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            mLogger.info(cal.getTime() + "= Horarios.txt caido");
        }

        if (formattedData != null && formattedData.size() > 0) {
            updateToday(formattedData);
            saveToDataStore(formattedData);
            //
            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_OK,ApiResponse.ERROR_OK,ApiResponse.MESSAGE_UPDATED_ROWS);

            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());
        } else {
            mLogger.warning(ApiResponse.MESSAGE_NO_NEW_DATA);
            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_OK,ApiResponse.ERROR_OK,ApiResponse.MESSAGE_NO_NEW_DATA);
            //
            resp.setStatus(ApiResponse.HTTP_ERROR_NOT_UPDATED);
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());
        }
    }

    private void updateToday(List<Medicion> formattedData) {

        mLogger.info("UPDATING TODAY");
        int dbCount = 50;
        ArrayList<Medicion> analyzing = getLastStatus(dbCount);
        if (analyzing.size() < 1){
            return;
        }
        Collections.reverse(analyzing);
        int foundDbCount = analyzing.size();
        Collections.sort(formattedData);
        analyzing.addAll(formattedData);

        for (int i=foundDbCount;i<analyzing.size();i++) {

            Medicion currentMedicion = analyzing.get(i);
            Medicion prevMedicion = analyzing.get(i-1);

            ApiMedicion.Escenario escenarioToday = Medicion.Escenario.NONE;
            ApiMedicion.Escenario escenarioTomorrow = Medicion.Escenario.NONE;

            Calendar currentMedicionTime = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            currentMedicionTime.setTimeInMillis(currentMedicion.getMeasuredAt());

            if (currentMedicionTime.get(Calendar.HOUR_OF_DAY) == HOUR_OF_REFERENCE) {
                escenarioToday = Medicion.Escenario.valueOf(prevMedicion.getEscenarioStateTomorrow());
                escenarioTomorrow = ApiMedicion.Escenario.NONE;
            } else {
                escenarioToday = Medicion.Escenario.valueOf(prevMedicion.getEscenarioStateToday());
                escenarioTomorrow = Medicion.Escenario.valueOf(prevMedicion.getEscenarioStateTomorrow());
            }

            mLogger.info("Setting today: " + escenarioToday.name());
            mLogger.info("Setting tomorrow: " + escenarioTomorrow.name());

            currentMedicion.setEscenarioStateToday(escenarioToday.name());
            currentMedicion.setEscenarioStateTomorrow(escenarioTomorrow.name());
        }

    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        if (!checkSenderAuth(req, resp)){
            return;
        }

        try {
            // Store
            List<Medicion> formattedData = parseFromMissingDay(req.getReader());
            updateToday(formattedData);
            saveToDataStore(formattedData);

            // Save
//            mLogger.info("SAVING COMPLETE FILE");
//            saveFile(dataInputStream);

            // Response
            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_OK, ApiResponse.ERROR_OK, ApiResponse.MESSAGE_MANUAL_PARSE_OK);
            resp.setStatus(ApiResponse.HTTP_OK);
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());

        } catch (ParseException e) {
            e.printStackTrace();

            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_ERROR, ApiResponse.ERROR_MANUAL_PARSE, ApiResponse.MESSAGE_MANUAL_PARSE_ERROR);
            resp.setStatus(ApiResponse.HTTP_ERROR);
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());
        }

    }

    //TABLE MAP: HORA, ESTACION, VALOR
    private void saveToDataStore(List<Medicion> mediciones) {
        mLogger.info("Storing " + mediciones.size() + " hours");

        // Saved date
        Date savedDate = Calendar.getInstance(TimeZone.getTimeZone("CET")).getTime();

        // Load datastore
        List<Entity> batch = new ArrayList<>();
        for (Medicion medicion : mediciones){
            // Measured date
            long measuredAtMillis = medicion.getMeasuredAt();
            Date measuredAt = new Date();
            Calendar measuredAtCalendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            measuredAtCalendar.setTimeInMillis(measuredAtMillis);
            measuredAt.setTime(measuredAtMillis);
            //
            long savedAtMillis = medicion.getSavedAtHour();
            Date savedAt = new Date();
            savedAt.setTime(savedAtMillis);

            // Una entidad por cada compuesto
            for (Compuesto compuesto: Compuesto.measuredCompuestos()) {
                // Build entity
                Entity entity = new Entity(ENTITY_TYPE_MEDIDAS, measuredAt.getTime() + compuesto.getId());
                entity.setIndexedProperty(PROPERTY_COMPUESTO, compuesto.name());
                entity.setIndexedProperty(PROPERTY_AVISO, medicion.getAviso());
                entity.setIndexedProperty(PROPERTY_AVISO_STATE, medicion.getAvisoState());
                entity.setIndexedProperty(PREPERTY_AVISO_MAX_TODAY, medicion.getAvisoMaxToday());
                entity.setIndexedProperty(PROPERTY_ESCENARIO_STATE_TODAY, medicion.getEscenarioStateToday());
                entity.setIndexedProperty(PROPERTY_ESCENARIO_STATE_TOMORROW, medicion.getEscenarioStateTomorrow());
                entity.setIndexedProperty(PROPERTY_ESCENARIO_STATE_TOMORROW_MANUAL, medicion.getEscenarioManualTomorrow());
                entity.setIndexedProperty(PROPERTY_SAVED_AT, savedDate);
                entity.setIndexedProperty(PROPERTY_MEASURE_DATE, measuredAt);
                entity.setIndexedProperty(PROPERTY_MEASURE_TIME, stringHour(measuredAtCalendar.get(Calendar.HOUR_OF_DAY)));
                // Build stations
                Map<String, Object> valueMap = medicion.getCompuestoValues(compuesto);
                if (valueMap != null) {
                    for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                        entity.setUnindexedProperty(entry.getKey(), entry.getValue());
                    }
                }
                // Store
                batch.add(entity);
            }
        }
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(batch);

    }

    /**
     * Save Full File
     *
     * @param inputStream
     */
    private static void saveFile(InputStream inputStream) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        time.set(Calendar.HOUR_OF_DAY,0);
        time.set(Calendar.MINUTE,0);
        time.set(Calendar.SECOND,0);
        Date measuredAt = time.getTime();
        long millis = time.getTimeInMillis();
        //
        Entity entity = new Entity(ENTITY_TYPE_FICHEROS,millis);
        entity.setIndexedProperty("Date", measuredAt);
        entity.setUnindexedProperty("File", GlobalUtils.getString(inputStream));
        // Store
        datastore.put(entity);
    }

    private Date getLastSavedTime() {
        // Prepare
        Query.Filter no2Filter = new Query.FilterPredicate(PROPERTY_COMPUESTO, Query.FilterOperator.EQUAL, NO2.name());
        Query query = new Query(ENTITY_TYPE_MEDIDAS)
            .setFilter(no2Filter)
            .addSort(PROPERTY_MEASURE_DATE, Query.SortDirection.DESCENDING);

        // Query
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = datastore.prepare(query);
        // Get results
        List<com.google.appengine.api.datastore.Entity> resultList = pq.asList(FetchOptions.Builder.withLimit(1));
        Date result;
        if (resultList.size()!=0) {
            mLogger.info("Previous value restored from DB");
            result = (Date) resultList.get(0).getProperty(PROPERTY_MEASURE_DATE);
        } else {
            // No hay datos
            mLogger.info("No previous value, initializing");
            Calendar calendarNoDatos = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            calendarNoDatos.set(Calendar.HOUR_OF_DAY, 0);
            calendarNoDatos.add(Calendar.DATE,-2);
            result = calendarNoDatos.getTime();
        }
        return result;
    }

    private ArrayList<Medicion> getLastStatus(int amountData) {
        // Poner Key estacion + hora_muestra
        Calendar calendarTwoAgo = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendarTwoAgo.add(Calendar.DATE, -3);
        Date dateTwoAgo = calendarTwoAgo.getTime();

        // Prepare
        Query.Filter keyFilter = new Query.FilterPredicate(PROPERTY_MEASURE_DATE, Query.FilterOperator.GREATER_THAN, dateTwoAgo);
        Query.Filter no2Filter = new Query.FilterPredicate(PROPERTY_COMPUESTO, Query.FilterOperator.EQUAL, NO2.name());
        List<Query.Filter> filterList = new ArrayList<>();
        filterList.add(keyFilter);
        filterList.add(no2Filter);
        Query.Filter filter = new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filterList);
        Query query = new Query(ENTITY_TYPE_MEDIDAS)
                .setFilter(filter)
                .addSort(PROPERTY_MEASURE_DATE
                        , Query.SortDirection.DESCENDING);

        // Query
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = datastore.prepare(query);
        // Get results

        List<com.google.appengine.api.datastore.Entity> resultList = pq.asList(FetchOptions.Builder.withLimit(amountData));
        ArrayList<Medicion> result = new ArrayList<>();
        if (resultList.size()!=0) {
            mLogger.info("Watching " + resultList.size() + " last values of ESTADO");
            for (com.google.appengine.api.datastore.Entity item : resultList){
                result.add(new Medicion((Date) item.getProperty(PROPERTY_MEASURE_DATE), (String) item.getProperty(PROPERTY_AVISO), (String) item.getProperty(PROPERTY_AVISO_STATE), (String) item.getProperty(PREPERTY_AVISO_MAX_TODAY), (String) item.getProperty(PROPERTY_ESCENARIO_STATE_TODAY), (String) item.getProperty(PROPERTY_ESCENARIO_STATE_TOMORROW),  (String) item.getProperty(PROPERTY_ESCENARIO_STATE_TOMORROW_MANUAL) ,isPureMadrid()));
            }
        } else {
            // No hay datos
            mLogger.info("No previous values for ESTADO");
        }
        return result;
    }

    private void permanentRedirect(HttpServletResponse response, String url){
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", url );
        response.setHeader("Connection", "close" );
    }
}
