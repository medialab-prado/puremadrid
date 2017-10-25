package com.puremadrid.core.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Created by Delga on 15/11/2016.
 */

public class Medicion extends ApiMedicion implements Comparable<Medicion>{


    public Medicion(Date measuredAt, String currentState, String avisoState, String avisoMaxToday, String escenarioToday, String escenarioTomorrow, String escenarioTomorrowManual, boolean isPureMadrid, Map<String,Object> measures) {
        this(measuredAt,currentState,avisoState,avisoMaxToday,escenarioToday,escenarioTomorrow,escenarioTomorrowManual, isPureMadrid);
        if (measures != null){
            no2values = measures;
        }
    }

    public Medicion(Date measuredAt, String currentState, String avisoState, String avisoMaxToday, String escenarioToday, String escenarioTomorrow, String escenarioTomorrowManual, boolean isPureMadrid) {
        super(measuredAt,currentState,avisoState,avisoMaxToday,escenarioToday,escenarioTomorrow,escenarioTomorrowManual,isPureMadrid);

    }

    public Medicion(boolean isPureMadrid){
        super(isPureMadrid);
    }

    @Override
    public int compareTo(Medicion other) {
        Long result = getMeasuredAt() - other.getMeasuredAt();
        return result.intValue();
    }

}
