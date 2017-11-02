package com.puremadrid.api;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.puremadrid.core.model.ApiMedicion;
import com.puremadrid.core.model.Compuesto;
import com.puremadrid.core.model.Medicion;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import static com.puremadrid.api.MainServlet.*;

/**
 * An endpoint class we are exposing
 */
@Api(
        name = "pureMadridApi",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "api.puremadrid.com",
                ownerName = "api.puremadrid.com",
                packagePath = ""
        )
)
public class PureMadridApi {

    private static final Logger mLogger = Logger.getLogger(PureMadridApi.class.getName());

    @ApiMethod(path = "getLastStatus", name = "getLastStatus", httpMethod = ApiMethod.HttpMethod.GET)
    public ApiMedicion getLastStatus(){ //@Named("amount") Integer amount) {

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        // Prepare
        Query query = new Query(ENTITY_TYPE_MEDIDAS).addSort(PROPERTY_MEASURE_DATE, Query.SortDirection.DESCENDING);
        PreparedQuery pq = datastore.prepare(query);
        List<Entity> resultList = pq.asList(FetchOptions.Builder.withLimit(1));
        Date lastDate = null;
        if (resultList.size() < 1){
            mLogger.severe("No results in DB");
            return null;
        } else {
            lastDate = (Date) resultList.get(0).getProperty(PROPERTY_MEASURE_DATE);
        }
        query = new Query(ENTITY_TYPE_MEDIDAS).setFilter(new Query.FilterPredicate(PROPERTY_MEASURE_DATE, Query.FilterOperator.EQUAL, lastDate));
        pq = datastore.prepare(query);
        resultList = pq.asList(FetchOptions.Builder.withDefaults());

        if (resultList.size() < 1){
            mLogger.severe("No results in DB");
            return null;
        }

        ApiMedicion medicion = new Medicion(MainServlet.isPureMadrid());

        for (Entity entity : resultList){

            Map<String, Object> properties = entity.getProperties();
            Map<String, Object> mapMeditions = new HashMap<>();

            String propertyCompuesto = (String) entity.getProperty(PROPERTY_COMPUESTO);
            Compuesto compuesto = Compuesto.withName(propertyCompuesto);

            medicion.setMeasuredAt((Date) entity.getProperty(PROPERTY_MEASURE_DATE));
            medicion.setSavedAtHour((Date) entity.getProperty(PROPERTY_SAVED_AT));
            if (entity.getProperty(PROPERTY_COMPUESTO).equals(Compuesto.NO2)){
                medicion.setAviso((String) entity.getProperty(PROPERTY_AVISO));
                medicion.setAvisoState((String) entity.getProperty(PROPERTY_AVISO_STATE));
                medicion.setAvisoMaxToday((String) entity.getProperty(PREPERTY_AVISO_MAX_TODAY));
                medicion.setEscenarioStateToday((String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TODAY));
                medicion.setEscenarioStateTomorrow((String)  entity.getProperty(PROPERTY_ESCENARIO_STATE_TOMORROW));
            }

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if(entry.getKey().contains(PROPERTY_ESTACION_BEGINS)){
                    try {
                        mapMeditions.put(entry.getKey(), parseValue(entry.getValue()));
                    } catch (Exception e){
                        mLogger.warning("Error parsing data from Datastore: " + entry.getValue());
                    }
                }
            }
            medicion.put(compuesto,mapMeditions);

        }

        mLogger.info("Returning " + resultList.size() + " items");

        return medicion;

    }

    private Object parseValue(Object value) {
        if (value instanceof Long){
            Long longValue = (Long) value;
            return longValue.intValue();
        } else if (value instanceof Double){
            Double doubleValue = (Double) value;
            return doubleValue.floatValue();
        }
        return -1;
    }

    @ApiMethod(path = "getStatusAt", name = "getStatusAt", httpMethod = ApiMethod.HttpMethod.GET)
    public ApiMedicion getStatusAt(@Named("amount") Date date) {

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));

//         Fix offset
        if (date.getTime() < 1505341800L * 1000){
            date.setTime(date.getTime() - 3600L * 1000);
        }
        date.setTime(date.getTime());

        calendar.setTime(date);
        calendar.setTimeZone(TimeZone.getTimeZone("CET"));



        // Prepare
        Query.FilterPredicate filter = new Query.FilterPredicate(PROPERTY_MEASURE_DATE, Query.FilterOperator.EQUAL, date);
        Query query = new Query(ENTITY_TYPE_MEDIDAS).setFilter(filter);

        // Query
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = datastore.prepare(query);
        List<Entity> resultList = pq.asList(FetchOptions.Builder.withDefaults());

        ApiMedicion medicion = new Medicion(MainServlet.isPureMadrid());
        for (Entity entity : resultList){

            Map<String, Object> properties = entity.getProperties();
            Map<String, Object> mapMeditions = new HashMap<>();

            String propertyCompuesto = (String) entity.getProperty(PROPERTY_COMPUESTO);
            Compuesto compuesto = Compuesto.withName(propertyCompuesto);

            medicion.setMeasuredAt((Date) entity.getProperty(PROPERTY_MEASURE_DATE));
            medicion.setSavedAtHour((Date) entity.getProperty(PROPERTY_SAVED_AT));
            if (entity.getProperty(PROPERTY_COMPUESTO).equals(Compuesto.NO2)){
                medicion.setAviso((String) entity.getProperty(PROPERTY_AVISO));
                medicion.setAvisoState((String) entity.getProperty(PROPERTY_AVISO_STATE));
                medicion.setAvisoMaxToday((String) entity.getProperty(PREPERTY_AVISO_MAX_TODAY));
                medicion.setEscenarioStateToday((String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TODAY));
                medicion.setEscenarioStateTomorrow((String)  entity.getProperty(PROPERTY_ESCENARIO_STATE_TOMORROW));
            }

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if(entry.getKey().contains(PROPERTY_ESTACION_BEGINS)){
                    try {
                        mapMeditions.put(entry.getKey(), parseValue(entry.getValue()));
                    } catch (Exception e){
                        mLogger.warning("Error parsing data from Datastore: " + entry.getValue());
                    }
                }
            }
            medicion.put(compuesto,mapMeditions);

        }

        return medicion;
    }

}
