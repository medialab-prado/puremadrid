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

package com.puremadrid.api.utils;

import com.puremadrid.core.model.Compuesto;
import com.puremadrid.api.services.GetNewData;
import com.puremadrid.core.model.NotificationBody;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import static com.puremadrid.api.ApiKeys.GCM_API_KEY_PROD;
import static com.puremadrid.api.ApiKeys.TOPIC_SCENARIO;

/**
 * Created by Delga on 10/12/2016.
 */

public class NotificationUtils {

    private static final Logger mLogger = Logger.getLogger(NotificationUtils.class.getName());

    public static boolean sendNotification(boolean warningEstado, String currentEstado, String validEstado, String maxEstado, String escenarioToday, String escenarioTomorrow, String service) {

        String basicAuth = null;
        String to = null;

        basicAuth = GCM_API_KEY_PROD;
        to = TOPIC_SCENARIO;

        // Si STATE es alguno de debug, es numero de estaciones en warning
        // Si es REAL, entonces es el nivel del escenario

        NotificationBody body = new NotificationBody( "date", warningEstado, currentEstado, validEstado, maxEstado, escenarioToday, escenarioTomorrow, to, service);
        mLogger.info("Sending notification: " + body.buildJson().toString());

        try {
            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestProperty ("Authorization", basicAuth);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setDoOutput(true);

            byte[] outputInBytes = body.buildJson().getBytes("UTF-8");
            OutputStream os = urlConnection.getOutputStream();
            os.write( outputInBytes );
            os.close();

            int code = urlConnection.getResponseCode();

            if (code == 200) {
                mLogger.info("Notification sent: " + code + " : " + urlConnection.getResponseMessage());
            } else {
                mLogger.warning("Notification NOT SENT: " + code + " : " + urlConnection.getResponseMessage());
            }
            return true;

        } catch (IOException e){
            mLogger.warning("Error sending Notification");
            return false;
        }

    }
}
