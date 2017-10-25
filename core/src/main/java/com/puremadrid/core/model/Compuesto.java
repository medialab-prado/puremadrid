package com.puremadrid.core.model;

import com.google.gson.Gson;
import com.puremadrid.core.model.ApiResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Delga on 15/11/2016.
 */

public enum Compuesto{
   SO2(1),
   CO(6),
   NO(7),
   NO2(8),
   PM2_5(9),
   PM10(10),
   NOX(12),
   O3(14),
   TOL(20),
   BEN(30),
   EBE(35),
   MXY(37),
   PXY(38),
   OXY(39),
   TCH(42),
   CH4(43),
   NMHC(44);

    private final int id;

    Compuesto(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static List<Compuesto> measuredCompuestos() {
        return new ArrayList<Compuesto>(){{
            add(Compuesto.CO);
            add(Compuesto.BEN);
            add(Compuesto.PM2_5);
            add(Compuesto.PM10);
            add(Compuesto.TOL);
            add(Compuesto.NO2);
            add(Compuesto.O3);
            add(Compuesto.SO2);
        }};
    }

    public static boolean isMeasureUsed(int measuredParameter) {
        Compuesto compuesto = Compuesto.withId(measuredParameter);
        return measuredCompuestos().contains(compuesto);
    }

    public static Compuesto withId(int propertyCompuesto) {
        for (Compuesto compuesto : Compuesto.values()){
            if (compuesto.getId() == propertyCompuesto){
                return compuesto;
            }
        }
        return null;
    }

    public static Compuesto withName(String propertyCompuesto) {
        for (Compuesto compuesto : measuredCompuestos()){
            if (compuesto.name().equals(propertyCompuesto)){
                return compuesto;
            }
        }
        return null;
    }

}
