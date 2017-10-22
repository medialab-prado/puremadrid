package com.puremadrid.core.model;

import com.google.gson.Gson;

import java.util.Map;

/**
 * Created by Delga on 28/11/2016.
 */

public class ApiResponse {

    public enum Errors{
        ERROR_OK(0,MESSAGE_EDITED_CORRECTLY),
        ERROR_TIMEOUT_IN_MEDIOAMBIENTE(1,MESSAGE_TIMEOUT),
        ERROR_MEDIOAMBIENTE_INPUT_NULL(2,MESSAGE_INPUT_NULL),
        ERROR_MANUAL_PARSE(3,MESSAGE_MANUAL_PARSE_ERROR),
        ERROR_FETCHING_REMOTE_URL(4,MESSAGE_ERROR_FETCHING_URL),
        ERROR_FORBIDDEN(5,MESSAGE_FORBIDDEN),
//        ERROR_WRONG_METHOD(6,MESSAGE_WRONG_METHOD)
        ERROR_INSECURE(7,MESSAGE_INSECURE),
        ERROR_PREDICTION_DOES_NOT_EXIST(8,MESSAGE_PREDICTION_DOES_NOT_EXIST),
        ERROR_SENDING_EMAIL(8,MESSAGE_ERROR_SENDING_EMAIL),
        ERROR_WRONG_PARAMETER(9, MESSAGE_WRONG_PARAMETER);

        private int error_code;
        private String message;

        Errors(int code, String message) {
            this.message = message;
            this.error_code = code;
        }

        public String getMessage(){
            return message;
        }

        public int getError_code(){
            return error_code;
        }
    }

    public static final int HTTP_OK = 200;
    public static final int HTTP_400_GENERIC = 400;
    public static final int HTTP_401_NOT_AUTHORIZED = 401;
    public static final int HTTP_403_FORBIDDEN = 403;
//    public static final int HTTP_405_MUST_BE_POST = 405;
    public static final int HTTP_ERROR = 500;
    public static final int HTTP_ERROR_NOT_UPDATED = 599;

    @Deprecated
    public static final int ERROR_OK = 0;
    @Deprecated
    public static final int ERROR_TIMEOUT_IN_MEDIOAMBIENTE = 1;
    @Deprecated
    public static final int ERROR_MEDIOAMBIENTE_INPUT_NULL = 2;
    @Deprecated
    public static final int ERROR_MANUAL_PARSE = 3;
    @Deprecated
    public static final int ERROR_FETCHING_REMOTE_URL = 4;

    public static final String MESSAGE_INPUT_NULL = "Data input stream is null";
    public static final String MESSAGE_TIMEOUT = "Timeout fetching data from remote url";
    public static final String MESSAGE_UPDATED_ROWS = "Rows were updated";
    public static final String MESSAGE_NO_NEW_DATA = "No new data";
    public static final String MESSAGE_MANUAL_PARSE_ERROR = "Parse exception";
    public static final String MESSAGE_MANUAL_PARSE_OK = "Parsed manually correctly";
    public static final String MESSAGE_ERROR_FETCHING_URL = "Wrong remote URL";
    public static final String MESSAGE_FORBIDDEN = "Client not authorized";
    public static final String MESSAGE_WRONG_METHOD = "Wrong method";
    public static final String MESSAGE_INSECURE= "Request is not secure";
    public static final String SENT_PREDICTIONS_CORRECTLY = "Predictions sent correctly";
    public static final String SENT_MORE_INFO_CORRECTLY = "More information sent correctly";
    public static final String MESSAGE_PREDICTION_DOES_NOT_EXIST = "The prediction does not exist";
    public static final String MESSAGE_ERROR_SENDING_EMAIL = "There was an error sending the email";
    public static final String MESSAGE_WRONG_PARAMETER = "Wrong parameter";
    public static final String MESSAGE_EDITED_CORRECTLY = "Edited correctly";


    // ======= FIELDS

    private int status;
    private int error;
    private String message;
    private Object response;

    @Deprecated
    public ApiResponse(int status, int error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ApiResponse(int status, Errors error) {
        this.status = status;
        this.error = error.getError_code();
        this.message = error.getMessage();
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String buildJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * Created by Delga on 15/12/2016.
     */

    public static class NotificationData {
        @Deprecated
        public static final String warningName = "warning";
        public static final String currentEstadoName = "currentStatus";
        public static final String validEstadoName = "validStatus";
        public static final String maxEstadoName = "maxStatus";
        public static final String escenarioNameToday = "escenarioToday";
        public static final String dateName = "date";
        public static final String escenarioNameTomorrow = "escenarioTomorrow";
        public static final String flagsName = "flags";

        @Deprecated
        private boolean warningStatus;
        private String currentStatus;
        private String validStatus;
        private String maxStatus;
        private String escenarioTomorrow;
        private String escenarioToday;
        private String date;
        private String flags;

        public NotificationData(String date, boolean warningStatus, String currentStatus, String validStatus, String maxStatus, String escenarioToday, String escenarioTomorrow, String flags) {
            this.warningStatus = warningStatus;
            this.currentStatus = currentStatus;
            this.date = date;
            this.escenarioTomorrow = escenarioTomorrow;
            this.escenarioToday = escenarioToday;
            this.validStatus = validStatus;
            this.maxStatus = maxStatus;
            this.flags = flags;
        }

        public NotificationData(Map<String, String> map){
            this.warningStatus = Boolean.parseBoolean(map.get(warningName));
            this.currentStatus = map.get(currentEstadoName);
            this.validStatus = map.get(validEstadoName);
            this.maxStatus = map.get(maxEstadoName);
            this.escenarioToday = map.get(escenarioNameToday);
            this.escenarioTomorrow = map.get(escenarioNameTomorrow);
            this.flags = map.get(flagsName);
            this.date = map.get(dateName);
        }

        public String getCurrentStatus() {
            return currentStatus;
        }

        @Deprecated
        public boolean getWarningStatus() {
            return warningStatus;
        }

        public String getDate() {
            return date;
        }

        public String getEscenarioTomorrow() {
            return escenarioTomorrow;
        }

        public String getEscenarioToday() {
            return escenarioToday;
        }

        public String getValidStatus() {
            return validStatus;
        }

        public String getMaxStatus() {
                return maxStatus;
            }

        public String getFlags() {
            return flags;
        }

        public String buildJson() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }
}
