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

package com.puremadrid.api;

import com.google.apphosting.api.ApiProxy;
import com.puremadrid.core.model.ApiResponse;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.appengine.api.utils.SystemProperty.Environment.Value.Development;
import static com.google.appengine.api.utils.SystemProperty.Environment.Value.Production;
import static com.google.appengine.api.utils.SystemProperty.environment;
import static com.puremadrid.api.ApiKeys.ACCESS_TOKEN;
import static com.puremadrid.api.ApiKeys.ACCES_TOKEN_USERPASS;

/**
 * Created by Delga on 17/11/2016.
 */

public class MainServlet extends HttpServlet {

    protected static final String PROPERTY_AVISO = "Aviso_instant";
    protected static final String PROPERTY_AVISO_STATE = "Aviso_state";
    protected static final String PREPERTY_AVISO_MAX_TODAY = "Aviso_max_today";
    protected static final String PROPERTY_ESCENARIO_STATE_TODAY = "Escenario_state_today";
    protected static final String PROPERTY_ESCENARIO_STATE_TOMORROW = "Escenario_state_tomorrow";
    protected static final String PROPERTY_SAVED_AT = "Saved_at";
    protected static final String PROPERTY_COMPUESTO = "Compuesto";
    protected static final String PROPERTY_MEASURE_DATE = "Measure_date";
    protected static final String PROPERTY_MEASURE_TIME = "Measure_time";
    protected static final String PROPERTY_ESTACION_BEGINS = "estacion_";
    protected static final String PROPERTY_ESCENARIO_STATE_TOMORROW_MANUAL = "Escenario_manual_tomorrow";

    protected static final String ENTITY_TYPE_MEDIDAS = "Medidas";
    protected static final String ENTITY_TYPE_FICHEROS = "Files";

    private static final Logger mLogger = Logger.getLogger(MainServlet.class.getName());




    public static boolean isPureMadrid() {
        return environment.value() == Production
                && ApiProxy.getCurrentEnvironment().getAppId().contains("pure-madrid")
                && !ApiProxy.getCurrentEnvironment().getAppId().contains("pure-madrid-testing")
                || isDevelopment();
    }

    private static boolean isDevelopment() {
        return environment.value() == Development;
    }

    protected static boolean checkSenderAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        if(isDevelopment()) {
            return true;
        }

//         Check if request is secure
        if (!req.isSecure()){
            if (req.getMethod().equals("GET")) {
                if (req.getQueryString() == null) {
                    permanentRedirect(resp, req.getRequestURL().toString().replace("http", "https"));
                } else {
                    permanentRedirect(resp, req.getRequestURL().toString().replace("http", "https") + "?" + req.getQueryString());
                }
            } else if (req.getMethod().equals("POST")) {
                ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_400_GENERIC,ApiResponse.Errors.ERROR_INSECURE);
                resp.setContentType("application/json; charset=utf-8");
                resp.getWriter().print(apiResponse.buildJson());
                resp.setStatus(ApiResponse.HTTP_400_GENERIC);
            }
            return false;
        }

        // Detectar autenticacion
        String authorization = req.getHeader("Authorization");
        String calledFromCron = req.getHeader("X-Appengine-Cron");
        String ipAddress = req.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = req.getRemoteAddr();
        }

        if ((ipAddress.equals("0.1.0.1") && calledFromCron.equals("true"))
                || (authorization != null
                    && (authorization.equals(ACCES_TOKEN_USERPASS)))
                ){
            return true;
        } else {
            mLogger.severe("IP or Authorization FAILED");
            ApiResponse apiResponse = new ApiResponse(ApiResponse.HTTP_401_NOT_AUTHORIZED,ApiResponse.Errors.ERROR_FORBIDDEN);
            //
            resp.setStatus(ApiResponse.HTTP_401_NOT_AUTHORIZED);
            resp.addHeader("WWW-Authenticate", "Basic realm=\"" + Calendar.getInstance(TimeZone.getTimeZone("CET")).getTimeInMillis() + "\"");
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print(apiResponse.buildJson());
            return false;
        }

    }



    private static void permanentRedirect(HttpServletResponse response, String url){
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", url );
        response.setHeader("Connection", "close" );
    }


}
