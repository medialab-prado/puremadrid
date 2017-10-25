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