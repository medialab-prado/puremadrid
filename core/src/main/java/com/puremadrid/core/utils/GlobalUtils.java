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

package com.puremadrid.core.utils;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Delga on 10/12/2016.
 */

public class GlobalUtils {

    public static JSONObject getJsonFromUrl(String requestUri) {
        JSONObject jsonResponse = null;

        // HTTP Connection and get json
        HttpURLConnection urlConnection;
        try {
            URL url = new URL(requestUri);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String read = GlobalUtils.readIt(in);
            jsonResponse = new JSONObject(read);

        } catch (Exception e) {
            return null;
        }
        return jsonResponse;
    }

    /**
     * Reads an InputStream and converts it to a String.
     *
     * Works for the remote jsons
     *
     * @param stream
     * @return
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    public static String readIt(InputStream stream) throws IOException {
        int len = 5000;
        Reader reader = new InputStreamReader(stream, "iso-8859-1");
        char[] buffer = new char[len];
        int length = reader.read(buffer);
        return new String(buffer).substring(0,length);
    }

    /**
     * Returns the InputStream is as a String
     * Used to load raw json into String
     *
     * Works for the local versions
     *
     * @param is: InputStream to parseFromHorarios
     * @return result String
     */
    public static String getString(InputStream is){
        String text;
        int size = 0;

        try {
            size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            text = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        return text;
    }


    /**
     * KEEP
     *
     * @return
     */
    public static InputStream getInputStream(String name) {
        return GlobalUtils.class.getClassLoader().getResourceAsStream(name);
    }

    public static String intTwoDigits(int stationNumber) {
        if (stationNumber < 10){
            return "0" + Integer.toString(stationNumber);
        } else {
            return Integer.toString(stationNumber);
        }
    }

    public static String stringHour(int hour) {
        if (hour < 10){
            return "0" + Integer.toString(hour) + ":00";
        } else {
            return Integer.toString(hour)+ ":00";
        }
    }
}
