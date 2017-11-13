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

package com.albaitdevs.puremadrid.data;

import android.net.Uri;
import android.provider.BaseColumns;


public class PureMadridContract {

    public static final String AUTHORITY = "com.javierdelgado.puremadrid.dataton";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String PATH_POLLUTION = "pollution";

    public static final class PollutionEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_POLLUTION).build();

        public static final String TABLE_NAME = "no2_levels";

        public static final String COLUMN_AVISO_LEVEL_NOW = "aviso_level_now";                  // Aviso_instant
        public static final String COLUMN_AVISO_MAX_LEVEL_TODAY = "aviso_max_level_today";      // Max seen today
        public static final String COLUMN_AVISO_STATE = "aviso_state";                          // State for today - Assigned yesterday;
        public static final String COLUMN_PARTICLE = "particle";                                // Compuesto - Name of the particle
        public static final String COLUMN_SCENARIO_MANUAL_TOMORROW = "scenario_manual_tomorrow";// Escenario_manual_tomorrow
        public static final String COLUMN_SCENARIO_TOMORROW = "scenario_state_tomorrow";        // Escenario_state
        public static final String COLUMN_SCENARIO_TODAY = "scenario_state_today";              // Escenario_state_today
        public static final String COLUMN_DATE = "scenario_date";                               // Measure_date  +  Measure_time

        // Several columns
        public static final String COLUMN_BASE_STATION = "estacion_";            // estacion_

    }
}
