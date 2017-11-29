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

/**
 * Created by Delga on 26/11/2016.
 */

public class Station {
    public static final int ZONE_1 = 1;
    public static final int ZONE_2 = 2;
    public static final int ZONE_3 = 3;
    public static final int ZONE_4 = 4;
    public static final int ZONE_5 = 5;

    String nombre;
    int id;
    float latitud_decimal;
    float longitud_decimal;
    int zona;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getLatitud_decimal() {
        return latitud_decimal;
    }

    public void setLatitud_decimal(float latitud_decimal) {
        this.latitud_decimal = latitud_decimal;
    }

    public float getLongitud_decimal() {
        return longitud_decimal;
    }

    public void setLongitud_decimal(float longitud_decimal) {
        this.longitud_decimal = longitud_decimal;
    }

    public int getZona() {
        return zona;
    }

    public void setZona(int zona) {
        this.zona = zona;
    }
}
