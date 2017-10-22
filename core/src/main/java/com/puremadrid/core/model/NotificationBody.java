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

    private final Data data;
    private final String to;

    private class Data {
        private final String currentStatus;
        private final String date;
        private final String escenario;
        private final String estado;

        private Data(String date, String currentStatus, String escenario, String estado) {
            this.currentStatus = currentStatus;
            this.date = date;
            this.escenario = escenario;
            this.estado = estado;
        }

        public String getCurrentStatus() {
            return currentStatus;
        }

        public String getDate() {
            return date;
        }

        public String getEscenario() {
            return escenario;
        }

        public String getEstado() {
            return estado;
        }

    }

    public  NotificationBody(String date, String currentStatus, String escenario, String estado, String to) {
        this.data = new Data(date, currentStatus, escenario, estado);
        this.to = to;
    }

    public String buildJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public Data getData() {
        return data;
    }

    public String getTo() {
        return to;
    }

}
