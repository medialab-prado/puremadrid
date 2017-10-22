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
