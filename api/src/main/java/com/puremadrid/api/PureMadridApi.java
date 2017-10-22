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

        int amount = 1;

//         Poner Key estacion + hora_muestra
        Calendar calendarTwoAgo = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendarTwoAgo.add(Calendar.DATE, -1);

        // Prepare
        Query query = new Query(ENTITY_TYPE_MEDIDAS).addSort(PROPERTY_MEASURE_DATE, Query.SortDirection.DESCENDING);
        // Query
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = datastore.prepare(query);
        // Get results
        List<Entity> resultList = pq.asList(FetchOptions.Builder.withLimit(amount));
        if (resultList.size() == amount) {

            mLogger.info("Found last value");
            Entity entity = resultList.get(0);

            Map<String, Object> properties = entity.getProperties();
            Map<String, Integer> mapMeditions = new HashMap<>();

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if(entry.getKey().contains(PROPERTY_ESTACION_BEGINS)){
                    try {
                        mapMeditions.put(entry.getKey(), ((Long) entry.getValue()).intValue());
                    } catch (Exception e){
                        mLogger.warning("Error parsing data from Datastore: " + entry.getValue());
                    }
                }
            }
            ApiMedicion medicion = new Medicion((Date) entity.getProperty(PROPERTY_MEASURE_DATE), (String) entity.getProperty(PROPERTY_AVISO), (String) entity.getProperty(PROPERTY_AVISO_STATE), (String) entity.getProperty(PREPERTY_AVISO_MAX_TODAY), (String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TODAY), (String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TOMORROW), (String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TOMORROW_MANUAL) ,MainServlet.isPureMadrid(),mapMeditions);

            mLogger.info("Returning One Medicion");

            return medicion;
        } else {
            // No hay datos
            mLogger.severe("This is weird, there are no results");
            return null;
        }
    }

    @ApiMethod(path = "getStatusAt", name = "getStatusAt", httpMethod = ApiMethod.HttpMethod.GET)
    public ApiMedicion getStatusAt(@Named("amount") Date date) {

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));

//         Fix offset
        if (date.getTime() < 1505341800L * 1000){
            date.setTime(date.getTime() - 3600L * 1000);
        }
        date.setTime(date.getTime() + 100L);

        calendar.setTime(date);
        calendar.setTimeZone(TimeZone.getTimeZone("CET"));



        int amount = 1;

        // Prepare
        Query.FilterPredicate filter = new Query.FilterPredicate(PROPERTY_MEASURE_DATE, Query.FilterOperator.LESS_THAN_OR_EQUAL, date);
        Query query = new Query(ENTITY_TYPE_MEDIDAS)
                .addSort(PROPERTY_MEASURE_DATE, Query.SortDirection.DESCENDING)
                .setFilter(filter);
        // Query
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = datastore.prepare(query);
        // Get results
        List<Entity> resultList = pq.asList(FetchOptions.Builder.withLimit(amount));
        if (resultList.size() == amount) {

            mLogger.info("Found last value");
            Entity entity = resultList.get(0);

            Map<String, Object> properties = entity.getProperties();
            Map<String, Integer> mapMeditions = new HashMap<>();

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if(entry.getKey().contains(PROPERTY_ESTACION_BEGINS)){
                    try {
                        mapMeditions.put(entry.getKey(), ((Long) entry.getValue()).intValue());
                    } catch (Exception e){
                        mLogger.warning("Error parsing data from Datastore: " + entry.getValue());
                    }
                }
            }
            ApiMedicion medicion = new Medicion((Date) entity.getProperty(PROPERTY_MEASURE_DATE), (String) entity.getProperty(PROPERTY_AVISO), (String) entity.getProperty(PROPERTY_AVISO_STATE), (String) entity.getProperty(PREPERTY_AVISO_MAX_TODAY), (String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TODAY), (String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TOMORROW), (String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TOMORROW_MANUAL) ,MainServlet.isPureMadrid(),mapMeditions);

            mLogger.info("Returning One Medicion");

            return medicion;
        } else {
            // No hay datos
            mLogger.severe("This is weird, there are no results");
            return null;
        }
    }

}
