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


import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.puremadrid.api.MainServlet;
import com.puremadrid.api.utils.NotificationUtils;
import com.puremadrid.core.model.ApiMedicion;
import com.puremadrid.core.model.ApiResponse;
import com.puremadrid.core.model.Constants;
import com.puremadrid.core.model.Medicion;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.puremadrid.core.model.Compuesto.NO2;

/**
 * Created by Delga on 16/12/2016.
 */

public class SetManualScenarioToday extends MainServlet {

    private static final Logger mLogger = Logger.getLogger(SetManualScenarioToday.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!checkSenderAuth(req, resp)){
            return;
        }

        if (!GetNewData.isPureMadrid()){
            mLogger.info("IS NOT PURE MADRID");
        }

        ApiMedicion.Escenario escenario = null;
        try {
            String pathInfo = req.getPathInfo();
            StringTokenizer tokenizer = new StringTokenizer(pathInfo, "/");
            String escenarioParam = tokenizer.nextToken();
            escenario = ApiMedicion.Escenario.valueOf(escenarioParam);
        } catch (Exception e){
            mLogger.severe("Wrong parameter");
            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_ERROR, ApiResponse.Errors.ERROR_WRONG_PARAMETER);
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());
            resp.setStatus(ApiResponse.HTTP_ERROR);
            return;
        }


        // Prepare
        int amount = 1;
        Query.Filter no2Filter = new Query.FilterPredicate(PROPERTY_COMPUESTO, Query.FilterOperator.EQUAL, NO2.name());
        Query query = new Query(ENTITY_TYPE_MEDIDAS)
                .setFilter(no2Filter)
                .addSort(PROPERTY_MEASURE_DATE, Query.SortDirection.DESCENDING);

        // Query
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = datastore.prepare(query);
        // Get results
        List<Entity> resultList = pq.asList(FetchOptions.Builder.withLimit(amount));
        Medicion result = null;
        if (resultList.size() == amount) {

            mLogger.info("Found last value");
            Entity entity = resultList.get(0);

            entity.setIndexedProperty(MainServlet.PROPERTY_ESCENARIO_STATE_TODAY, escenario.name());

            datastore.put(entity);

            boolean warningEstado = false;
            NotificationUtils.sendNotification(warningEstado, (String) entity.getProperty(PROPERTY_AVISO), (String)  entity.getProperty(PROPERTY_AVISO_STATE), (String)  entity.getProperty(PREPERTY_AVISO_MAX_TODAY), (String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TODAY), (String) entity.getProperty(PROPERTY_ESCENARIO_STATE_TOMORROW), Constants.FLAG_SET_ALWAYS_TODAY);

            mLogger.info("Updated Manual Scenario for Today");
            mLogger.info("Notification sent");

            ApiResponse apiResponse = new ApiResponse(ApiResponse.ERROR_OK, ApiResponse.Errors.ERROR_OK);
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());
            resp.setStatus(ApiResponse.ERROR_OK);

        } else {
            // No hay datos
            mLogger.severe("This is weird, there are no results");
            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_ERROR_NOT_UPDATED, ApiResponse.Errors.ERROR_PREDICTION_DOES_NOT_EXIST);
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());
            resp.setStatus(ApiResponse.HTTP_ERROR_NOT_UPDATED);
        }

    }
}
