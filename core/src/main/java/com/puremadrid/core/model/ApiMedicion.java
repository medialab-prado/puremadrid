package com.puremadrid.core.model;

import com.google.gson.Gson;
import com.puremadrid.core.utils.GlobalUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Created by Delga on 15/11/2016.
 */

public class ApiMedicion {

    protected static final Logger mLogger = Logger.getLogger(ApiMedicion.class.getName());
    protected boolean isPureMadrid;

    public enum Estado{
        NONE,
        PREAVISO,
        AVISO,
        ALERTA;
    }

    public enum Escenario{
        NONE,
        ESCENARIO1,
        ESCENARIO2,
        ESCENARIO3,
        ESCENARIO3_MIN,
        ESCENARIO4
    }

    protected static final int MIN_DEBUG_WARNING_150 = 150; // 150
    protected static final int MIN_DEBUG_WARNING_180 = 180; // 180
    protected static int MIN_VALUE_PREAVISO = 180; //180;
    protected static int MIN_VALUE_AVISO = 200; //200;
    protected static int MIN_VALUE_ALERTA = 400; //400;

    protected static final int ESTACIONES_PREAVISO = 2;
    protected static final int ESTACIONES_AVISO = 2;
    protected static final int ESTACIONES_ALERTA = 3;
    protected static final int ESTACIONES_ALERTA_ZONA_4 = 2;

    protected static final int HORAS_PREAVISO = 2;
    protected static final int HORAS_AVISO = 2;
    protected static final int HORAS_ALERTA = 3;

    /**
     *  Format:  "estacion_XX" -> value
     */
    Map<String,Integer> no2values = new HashMap<>();
    protected String compuesto;
    protected Calendar savedAtHour;
    protected Calendar measuredAt;
    protected String aviso;
    protected String avisoState;
    protected String avisoMaxToday;

    protected String escenarioStateToday;
    protected String escenarioStateTomorrow;
    protected String escenarioManualTomorrow;
    protected boolean hasAllValues;

    public ApiMedicion(Date measuredAt, String currentState, String avisoState, String avisoMaxToday, String escenarioToday, String escenarioTomorrow, String escenarioTomorrowManual, boolean isPureMadrid, Map<String,Integer> measures) {
        this(measuredAt, currentState, avisoState, avisoMaxToday, escenarioToday, escenarioTomorrow, escenarioTomorrowManual, isPureMadrid);
        if (measures != null){
            no2values = measures;
        }
    }

    public ApiMedicion(Date measuredAt, String currentState, String avisoState, String avisoMaxToday, String escenarioToday, String escenarioTomorrow, String escenarioTomorrowManual, boolean isPureMadrid) {
        this.isPureMadrid = isPureMadrid;
        MIN_VALUE_PREAVISO = isPureMadrid ? 180 : 140; //180;
        MIN_VALUE_AVISO = isPureMadrid ? 200 : 180; //200;
        MIN_VALUE_ALERTA = isPureMadrid ? 400 : 300; //400;

        this.aviso = currentState;
        this.avisoState = avisoState;
        this.avisoMaxToday = avisoMaxToday;
        this.escenarioStateToday = escenarioToday;
        this.escenarioStateTomorrow = escenarioTomorrow;
        this.escenarioManualTomorrow = escenarioTomorrowManual;

        if (measuredAt == null){
            this.measuredAt = null;
        } else {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            calendar.setTime(measuredAt);
            this.measuredAt = calendar;
        }
    }

    public ApiMedicion(boolean isPureMadrid){
        this.isPureMadrid = isPureMadrid;
        MIN_VALUE_PREAVISO = isPureMadrid ? 180 : 180; //180;
        MIN_VALUE_AVISO = isPureMadrid ? 200 : 200; //200;
        MIN_VALUE_ALERTA = isPureMadrid ? 400 : 400; //400;
        //
        avisoMaxToday = Estado.NONE.name();
        avisoState = Estado.NONE.name();
        aviso = Estado.NONE.name();
        escenarioStateToday = Escenario.NONE.name();
        escenarioStateTomorrow = Escenario.NONE.name();

    }

    public String getAviso() {
        return aviso;
    }

    public void setAviso(String aviso) {
        this.aviso = aviso;
    }

    public void add(String column, Integer integer) {
        no2values.put(column,integer);
    }

    public void setSavedAtHour(Calendar savedAtHour) {
        this.savedAtHour = savedAtHour;
    }

    public void setMeasuredAt(Calendar measuredAt) {
        this.measuredAt = measuredAt;
    }

    public String getCompuesto() {
        return compuesto;
    }

    public Calendar getSavedAtHour() {
        return savedAtHour;
    }

    public long getMeasuredAt() {
        return measuredAt.getTimeInMillis();
    }

    public String getAvisoMaxToday() {
        return avisoMaxToday;
    }

    public void setAvisoMaxToday(String avisoMaxToday) {
        this.avisoMaxToday = avisoMaxToday;
    }

    public Map<String,Integer> getNO2() {
        return no2values;
    }

    public int getNO2Size() {
        return no2values.size();
    }

    public void setCompuesto(int measuredParameter) {
        switch (measuredParameter){
            case 8: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;º
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;
//            case 4: compuesto = "NO2"; break;

        }
    }

    public boolean hasAllValues() {
        return hasAllValues;
    }

    public String getAvisoState() {
        return avisoState;
    }

    public String getEscenarioStateTomorrow() {
        return escenarioStateTomorrow;
    }

    public void setAvisoState(String status) {
        avisoState = status;
    }

    public void setEscenarioStateTomorrow(String state) {
        escenarioStateTomorrow = state;
    }

    public String getEscenarioStateToday() {
        return escenarioStateToday;
    }

    public void setEscenarioStateToday(String state) {
        this.escenarioStateToday = state;
    }

    public String getEscenarioManualTomorrow() {
        return escenarioManualTomorrow;
    }

    public void setEscenarioManualTomorrow(String escenarioManualTomorrow) {
        this.escenarioManualTomorrow = escenarioManualTomorrow;
    }

    /**
     * Este es el unico metodo tocho que es para parsear de TableMap a Medicion
     */
    public Map<String, String> computeAlertas() {

        if (no2values == null || no2values.size() == 0){
            return null;
        }

        // MAP (ZONE -> STATIONS WITH HIGH LEVELS)
        Map<Integer,Integer> stationsWithPreAvisosByZone = new HashMap<>();
        Map<Integer,Integer> stationsWithAvisosByZone = new HashMap<>();
        Map<Integer,Integer> stationsWithAlertasByZone = new HashMap<>();
        // Get Json
        Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);

        //
        aviso = Estado.NONE.name();
        int warning150 = 0;
        int warning180 = 0;
        int maxValue = 0;
        for (Station station : stations){
            Integer value = no2values.get("estacion_" + GlobalUtils.intTwoDigits(station.getId()));
            if (value == null){
                continue;
            }
            if (value >= MIN_DEBUG_WARNING_150){
                warning150++;
            }
            if (value >= MIN_DEBUG_WARNING_180){
                warning180++;
            }
            if (value >= maxValue){
                maxValue = value;
            }
            if (value >= MIN_VALUE_PREAVISO){
                Integer oldValue = stationsWithPreAvisosByZone.get(station.getZona());
                if (oldValue == null){
                    stationsWithPreAvisosByZone.put(station.getZona(),1);
                } else {
                    stationsWithPreAvisosByZone.put(station.getZona(), ++oldValue);
                }
            }
            if (value >= MIN_VALUE_AVISO) {
                Integer oldValue = stationsWithAvisosByZone.get(station.getZona());
                if (oldValue == null) {
                    stationsWithAvisosByZone.put(station.getZona(), 1);
                } else {
                    stationsWithAvisosByZone.put(station.getZona(), ++oldValue);
                }
            }
            if (value >= MIN_VALUE_ALERTA){
                Integer oldValue = stationsWithAlertasByZone.get(station.getZona());
                if (oldValue == null){
                    stationsWithAlertasByZone.put(station.getZona(),1);
                } else {
                    stationsWithAlertasByZone.put(station.getZona(), ++oldValue);
                }
            }
        }

        for (Map.Entry<Integer, Integer> zone : stationsWithPreAvisosByZone.entrySet()){
            if (zone.getValue() >= ESTACIONES_PREAVISO){
                aviso = Estado.PREAVISO.name();
            }

        }
        for (Map.Entry<Integer, Integer> zone : stationsWithAvisosByZone.entrySet()){
            if (zone.getValue() >= ESTACIONES_AVISO){
                aviso = Estado.AVISO.name();
            }

        }
        for (Map.Entry<Integer, Integer> zone : stationsWithAlertasByZone.entrySet()){
            if (zone.getKey() == Station.ZONE_4){
                if (zone.getValue() >= ESTACIONES_ALERTA_ZONA_4) {
                    aviso = Estado.ALERTA.name();
                }
            } else {
                if (zone.getValue() >= ESTACIONES_ALERTA) {
                    aviso = Estado.ALERTA.name();
                }
            }

        }

        Map<String, String> returnEmailData = new HashMap<>();
        int hour = measuredAt.get(Calendar.HOUR_OF_DAY);
        if (isPureMadrid){
            if (warning180 > 0) {
                returnEmailData.put("title","Status");
                returnEmailData.put("subject","Number of stations passing 180 = " + warning180);
            }
        }

        hasAllValues = false;
        escenarioStateToday = Escenario.NONE.name();
        escenarioStateTomorrow = Escenario.NONE.name();
        return returnEmailData;
    }
}
