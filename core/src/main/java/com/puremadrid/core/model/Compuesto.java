package com.puremadrid.core.model;

import com.google.gson.Gson;
import com.puremadrid.core.model.ApiResponse;

/**
 * Created by Delga on 15/11/2016.
 */

public enum Compuesto{
   SO2,
   CO,
   NO,
   NO2,
   PM2_5,
   PM10,
   NOX,
   O3,
   TOL,
   BEN,
   EBE,
   MXY,
   PXY,
   OXY,
   TCH,
   NMHC;

    /**
     * Created by Delga on 10/12/2016.
     */

    public static class NotificationBody {

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

}
