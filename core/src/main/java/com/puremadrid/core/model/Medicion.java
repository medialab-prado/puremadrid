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
