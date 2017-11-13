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

package com.puremadrid.core.model;

import com.google.gson.Gson;

/**
 * Created by Delga on 10/12/2016.
 */
public class NotificationBody {

    public enum Version{
        PUREMADRID_PROD,
        ADMIN
    }

    public final ApiResponse.NotificationData data;
    private final String to;

    public NotificationBody(String date, boolean warningStatus, String currentStatus, String validStatus, String maxStatus,  String escenarioToday, String escenarioTomorrow, String to, String flags) {
        this.data = new ApiResponse.NotificationData(date, warningStatus, currentStatus, validStatus, maxStatus, escenarioToday, escenarioTomorrow, flags);
        this.to = to;
    }

    public String buildJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public ApiResponse.NotificationData getData() {
        return data;
    }

    public String getTo() {
        return to;
    }

}