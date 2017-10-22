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

import com.puremadrid.api.MainServlet;
import com.puremadrid.api.core.Parser;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import com.google.common.collect.Table;
import com.puremadrid.core.model.ApiMedicion;
import com.puremadrid.core.model.ApiResponse;
import com.puremadrid.core.model.Constants;
import com.puremadrid.core.model.Medicion;
import com.puremadrid.core.utils.GlobalUtils;
import com.puremadrid.api.utils.EmailUtils;
import com.puremadrid.api.utils.NotificationUtils;

import static com.puremadrid.api.core.Parser.parseFromMissingDay;
import static com.puremadrid.core.model.ApiMedicion.Escenario.*;
import static com.puremadrid.core.model.ApiMedicion.Estado.*;
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
            computeAvisoStatus(formattedData, true);
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




    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        if (!checkSenderAuth(req, resp)){
            return;
        }

        try {
            // Store
            List<Medicion> formattedData = parseFromMissingDay(req.getReader());
            computeAvisoStatus(formattedData, false);
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

    private void computeAvisoStatus(List<Medicion> formattedData, boolean withAssertions) {
        //
        int dbCount = 50;
        ArrayList<Medicion> analyzing = getLastStatus(dbCount);
        Collections.reverse(analyzing);
        int foundDbCount = analyzing.size();
        Collections.sort(formattedData);
        analyzing.addAll(formattedData);

        //
        String lastScenarioToday = Medicion.Escenario.NONE.name();
        String lastScenarioTomorrow = Medicion.Escenario.NONE.name();
        String lastAviso = Medicion.Estado.NONE.name();
        String lastValidAviso = Medicion.Estado.NONE.name();
        String lastMaxAviso = Medicion.Estado.NONE.name();

        String flag = Constants.FLAGS_NONE;
        mLogger.config("Found in DB = " + foundDbCount + ": LAST IN DB: ");// + analyzing.get(foundDbCount).getMeasuredAtAsCalendar() + ":" + analyzing.get(foundDbCount).getAviso());
        for (int i=foundDbCount;i<analyzing.size();i++){
            if (i-2 < 0){
                mLogger.config("STATE: Not enouth data. Skipping this Medicion.");
                continue;
            }
            Medicion currentMedicion = analyzing.get(i);
            Medicion prevMedicion = analyzing.get(i-1);
            Medicion prevTwoMedicion = analyzing.get(i-2);

            //ASSERT IF THE PREVIOUS VALUE IS PREVIOUS HOUR
            Calendar tempCalendar = (Calendar) currentMedicion.getMeasuredAtAsCalendar().clone();
            Calendar tempCalendar2 = (Calendar) currentMedicion.getMeasuredAtAsCalendar().clone();
            tempCalendar.add(Calendar.HOUR_OF_DAY,-1);
            tempCalendar2.add(Calendar.HOUR_OF_DAY,-2);
            if (withAssertions) {
                if (prevMedicion.getMeasuredAtAsCalendar() == null || prevTwoMedicion.getMeasuredAtAsCalendar() == null) {
                    mLogger.warning("prevMedicion OR prevTwoMedition Measure is NULL");
                    // TODO: Salir
                } else if (tempCalendar.compareTo(prevMedicion.getMeasuredAtAsCalendar()) != 0
                        || tempCalendar2.compareTo(prevTwoMedicion.getMeasuredAtAsCalendar()) != 0) {
                    mLogger.warning("Dates are not contiguous: " + tempCalendar.get(Calendar.DATE) + " - " + tempCalendar.get(Calendar.HOUR_OF_DAY));
                    // TODO: Salir
                }
            }

            // COMPUTE AVISOS =======================================================
            Medicion.Estado aviso_result = Medicion.Estado.NONE;
            Medicion.Estado currentAviso = Medicion.Estado.valueOf(currentMedicion.getAviso());
            switch (currentAviso){
                case NONE:
                    break;
                case PREAVISO:
                    if (Medicion.Estado.valueOf(prevMedicion.getAviso()).ordinal() >= PREAVISO.ordinal()){
                        aviso_result = Medicion.Estado.PREAVISO;
                    }
                    break;
                case AVISO:
                    if (Medicion.Estado.valueOf(prevMedicion.getAviso()).ordinal() >= AVISO.ordinal()){
                        aviso_result = AVISO;
                    } else if (Medicion.Estado.valueOf(prevMedicion.getAviso()) == PREAVISO){
                        aviso_result = Medicion.Estado.PREAVISO;
                    }
                    break;
                case ALERTA:
                    if (Medicion.Estado.valueOf(prevMedicion.getAviso()).ordinal() >= ALERTA.ordinal()
                        && Medicion.Estado.valueOf(prevTwoMedicion.getAviso()).ordinal() >= ALERTA.ordinal()){
                        aviso_result = ALERTA;
                    } else if (Medicion.Estado.valueOf(prevMedicion.getAviso()).ordinal() >= AVISO.ordinal()){
                        aviso_result = AVISO;
                    } else if (Medicion.Estado.valueOf(prevMedicion.getAviso()).ordinal() >= PREAVISO.ordinal()) {
                        aviso_result = Medicion.Estado.PREAVISO;
                    }
                default:
            }
            mLogger.config("Setting Aviso State = " + aviso_result);
            currentMedicion.setAvisoState(aviso_result.name());

            String aviso_max_today = null;
            try {
                // COMPUTE HIHGEST ALERTA FOR TODAY =============================
                aviso_max_today = currentMedicion.getAvisoState();
                if (currentMedicion.getMeasuredAtAsCalendar().get(Calendar.HOUR_OF_DAY) != REFERENCE_HOUR
                        && (aviso_max_today == null || Medicion.Estado.valueOf(prevMedicion.getAvisoMaxToday()).ordinal() > Medicion.Estado.valueOf(aviso_max_today).ordinal())) {
                    aviso_max_today = prevMedicion.getAvisoMaxToday();
                }
                currentMedicion.setAvisoMaxToday(aviso_max_today);
            } catch (Exception e){
                e.printStackTrace();
                continue;
            }
            mLogger.config("Setting aviso max today: " + aviso_max_today);

            // COMPUTE ESCENARIO TODAY AND TOMORROW
            ApiMedicion.Escenario escenarioToday = Medicion.Escenario.NONE;
            ApiMedicion.Escenario escenarioTomorrow = Medicion.Escenario.NONE;
            ApiMedicion.Escenario escenarioManualTomorrow = Medicion.Escenario.NONE;;

            try {
                mLogger.config("Scenario. Analyzing time: "  + currentMedicion.getMeasuredAtAsCalendar().get(Calendar.HOUR_OF_DAY));
                int hoursDiff = currentMedicion.getMeasuredAtAsCalendar().get(Calendar.HOUR_OF_DAY)+ HOURS_ADD_TO_HOUR;

                // I DONT NEED TO CHECK OLDER VALUES FOR THIS
                // ESCENARIO TOMORROW PRIORITY
                try {
                    if (currentMedicion.getMeasuredAtAsCalendar().get(Calendar.HOUR_OF_DAY) != REFERENCE_HOUR) {
                        escenarioManualTomorrow = Medicion.Escenario.valueOf(prevMedicion.getEscenarioManualTomorrow());
                    }
                } catch (Exception e) {
                    mLogger.config("Error setting scenario manual tomorrow");
                }
                currentMedicion.setEscenarioManualTomorrow(escenarioManualTomorrow.name());
                mLogger.config("Setting Escenario manual tomorrow: " + escenarioManualTomorrow.name());


                // Assert dates
                Calendar tempsCalendarMinusOne = (Calendar) analyzing.get(i).getMeasuredAtAsCalendar().clone();
                Calendar tempsCalendarMinusTwo = (Calendar) analyzing.get(i).getMeasuredAtAsCalendar().clone();

                if (i- hoursDiff < 0){
                    mLogger.warning("SCENARIO: Not enough data to analyze scenario for yesterday");
                    continue;
                }
                boolean availableTwoDaysAgo = true;
                if (i- hoursDiff - 24 < 0){
                    mLogger.warning("SCENARIO: Not enough data to analyze scenario for two days ago");
                    availableTwoDaysAgo = false;
                }


                Calendar tempsCalendar2 = (Calendar) analyzing.get(i- hoursDiff).getMeasuredAtAsCalendar().clone();
                Calendar tempsCalendar3 = (Calendar) analyzing.get(i- hoursDiff - 24).getMeasuredAtAsCalendar().clone();
                tempsCalendarMinusOne.add(Calendar.HOUR_OF_DAY, - hoursDiff);
                tempsCalendarMinusTwo.add(Calendar.HOUR_OF_DAY, - hoursDiff - 24);
                if(withAssertions
                        && (tempsCalendar2.compareTo(tempsCalendarMinusOne) != 0
                        || tempsCalendar3.compareTo(tempsCalendarMinusTwo) != 0 )){
                    mLogger.warning("Dates are not contiguous: day " + tempsCalendarMinusOne.get(Calendar.DATE) + " - hour " + tempsCalendarMinusOne.get(Calendar.HOUR_OF_DAY));
                }

                String avisoMaxToday = analyzing.get(i).getAvisoMaxToday();
                String avisoMaxYesterday = analyzing.get(i - hoursDiff).getAvisoMaxToday();

                // COMPUTE ESCENARIO TODAY
                Calendar currentMedicionTime = Calendar.getInstance(TimeZone.getTimeZone("CET"));
                currentMedicionTime.setTimeInMillis(currentMedicion.getMeasuredAt());
                if (currentMedicionTime.get(Calendar.HOUR_OF_DAY) == HOUR_OF_REFERENCE){
                    escenarioToday = Medicion.Escenario.valueOf(prevMedicion.getEscenarioStateTomorrow());
                    escenarioTomorrow = ApiMedicion.Escenario.NONE;
                    flag = Constants.FLAG_SET_ALWAYS_TODAY;
                } else {
                    escenarioToday = Medicion.Escenario.valueOf(prevMedicion.getEscenarioStateToday());
                    escenarioTomorrow = Medicion.Escenario.valueOf(prevMedicion.getEscenarioStateTomorrow());
                    flag = Constants.FLAG_DUMMY;
                }

                currentMedicion.setEscenarioStateToday(escenarioToday.name());
                currentMedicion.setEscenarioStateTomorrow(escenarioTomorrow.name());

                mLogger.config("Setting Escenario today: " + escenarioToday.name());
                mLogger.config("Setting Escenario today: " + escenarioTomorrow.name());

                // MANUAL---------------------------------------------------------------
                // IF MORE THAN 12 THEN ESCENARIO 1 ALWAYS
                // IF LESS THAN 12 THEN WHICHEVER ESCENARIO
                ApiMedicion.Escenario escenarioEmail = ApiMedicion.Escenario.NONE;
                if (escenarioManualTomorrow.ordinal() >= ESCENARIO1.ordinal()){
//                    escenarioTomorrow = escenarioManualTomorrow;
                    mLogger.config("Setting Escenario tomorrow from manual: " + escenarioTomorrow.name());
                }
                else if (Medicion.Estado.valueOf(avisoMaxToday).ordinal() >= ESCENARIO1.ordinal()
                        && analyzing.get(i).getMeasuredAtAsCalendar().get(Calendar.DATE) > 12){
                    // ESTO ES EL ESCENARIO 1 AUTOMATICO
//                    escenarioTomorrow = ESCENARIO1;
                    escenarioEmail = ESCENARIO1;
                    EmailUtils.sendEmail("ESCENARIO 1 DISPARADO AUTOMATICO, ESTAR ATENTO","PUES ESO");
                } else {
                    // ESTO ES EL SETEAR EL RESTO DE ESCENARIOS, NO APLICA
                    switch (Medicion.Estado.valueOf(avisoMaxToday)) {
                        case PREAVISO:
                            if (Medicion.Estado.valueOf(avisoMaxYesterday).ordinal() >= PREAVISO.ordinal()) {
//                                escenarioTomorrow = Medicion.Escenario.ESCENARIO2;
                                escenarioEmail = ESCENARIO2;
                            } else {
//                                escenarioTomorrow = ESCENARIO1;
                                escenarioEmail = ESCENARIO1;
                            }
                            break;
                        case AVISO:
                            if (Medicion.Estado.valueOf(avisoMaxYesterday).ordinal() >= AVISO.ordinal()) {
                                if (availableTwoDaysAgo) {
                                    String avisoMaxTwoAgo = analyzing.get(i - hoursDiff - 24).getAvisoMaxToday();
                                    if (Medicion.Estado.valueOf(avisoMaxTwoAgo).ordinal() >= AVISO.ordinal()) {
//                                        escenarioTomorrow = Medicion.Escenario.ESCENARIO4;
                                        escenarioEmail = ESCENARIO4;
                                    } else {
//                                        escenarioTomorrow = Medicion.Escenario.ESCENARIO3;
                                        escenarioEmail = ESCENARIO3;
                                    }
                                } else {
                                    // Data from 2 days ago is not available
//                                    escenarioTomorrow = Medicion.Escenario.ESCENARIO3_MIN;
                                    escenarioEmail = ESCENARIO3_MIN;
                                }
                            } else {
//                                escenarioTomorrow = Medicion.Escenario.ESCENARIO2;
                                escenarioEmail = ESCENARIO2;
                            }
                            break;
                        case ALERTA:
//                            escenarioTomorrow = Medicion.Escenario.ESCENARIO4;
                            escenarioEmail = ESCENARIO4;
                    }

                }
                //                currentMedicion.setEscenarioStateTomorrow(escenarioTomorrow.name());
                // END OF MANUAL---------------------------------------------------------------


                // ESTA LINEA ES LA QUE SETEA EL ESTADO DE MAÑANA-------------------------------
                // ------------------------------------------------------------------------
//                currentMedicion.setEscenarioStateTomorrow(escenarioTomorrow.name());
//                mLogger.config("Setting Escenario tomorrow: " + escenarioTomorrow.name());

                if (escenarioEmail.ordinal() > ApiMedicion.Escenario.NONE.ordinal()) {
                    EmailUtils.sendEmail("Automatic Escenario: " + escenarioEmail.name(), "Este escenario se hubiera seteado automaticamente: " + escenarioEmail.name());
                }

            } catch (Exception e){
                e.printStackTrace();
                mLogger.warning("Not storing scenario: i, i-hour or i-48 is null");
                continue;
            }

            lastAviso = currentAviso.name();
            lastValidAviso = aviso_result.name();
            lastMaxAviso = aviso_max_today;
            lastScenarioTomorrow = escenarioTomorrow.name();
            lastScenarioToday = escenarioToday.name();

        }

        boolean warningEstado = false;
        NotificationUtils.sendNotification(warningEstado, lastAviso, lastValidAviso, lastMaxAviso, lastScenarioToday, lastScenarioTomorrow, flag);
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
            Date measuredDate=medicion.getMeasuredAtAsCalendar().getTime();
            //
            Calendar savedAt = medicion.getSavedAtHour();
            // Build task
            Entity entity = new Entity(ENTITY_TYPE_MEDIDAS, savedAt.getTimeInMillis());
            entity.setIndexedProperty(PROPERTY_COMPUESTO, medicion.getCompuesto());
            entity.setIndexedProperty(PROPERTY_AVISO,medicion.getAviso());
            entity.setIndexedProperty(PROPERTY_AVISO_STATE,medicion.getAvisoState());
            entity.setIndexedProperty(PREPERTY_AVISO_MAX_TODAY,medicion.getAvisoMaxToday());
            entity.setIndexedProperty(PROPERTY_ESCENARIO_STATE_TODAY,medicion.getEscenarioStateToday());
            entity.setIndexedProperty(PROPERTY_ESCENARIO_STATE_TOMORROW,medicion.getEscenarioStateTomorrow());
            entity.setIndexedProperty(PROPERTY_ESCENARIO_STATE_TOMORROW_MANUAL,medicion.getEscenarioManualTomorrow());
            entity.setIndexedProperty(PROPERTY_SAVED_AT, savedDate);
            entity.setIndexedProperty(PROPERTY_MEASURE_DATE, measuredDate);
            entity.setIndexedProperty(PROPERTY_MEASURE_TIME,stringHour(medicion.getMeasuredAtAsCalendar().get(Calendar.HOUR_OF_DAY)));
            // Build stations
            for ( Map.Entry<String, Integer> entry : medicion.getNO2().entrySet()) {
                entity.setUnindexedProperty(entry.getKey(),entry.getValue());
            }
            // Store
            batch.add(entity);
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
        // Poner Key estacion + hora_muestra
        Calendar calendarTwoAgo = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendarTwoAgo.add(Calendar.DATE, -2);
        Date dateTwoAgo = calendarTwoAgo.getTime();

        // Prepare
        Query.Filter keyFilter = new Query.FilterPredicate(PROPERTY_MEASURE_DATE, Query.FilterOperator.GREATER_THAN, dateTwoAgo);
        Query query = new Query(ENTITY_TYPE_MEDIDAS).setFilter(keyFilter).addSort(PROPERTY_MEASURE_DATE, Query.SortDirection.DESCENDING);
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
        Query query = new Query(ENTITY_TYPE_MEDIDAS).setFilter(keyFilter).addSort(PROPERTY_MEASURE_DATE, Query.SortDirection.DESCENDING);
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
