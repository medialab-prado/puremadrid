package com.puremadrid.api.core;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.puremadrid.api.utils.EmailUtils;
import com.puremadrid.core.model.Compuesto;
import com.puremadrid.core.model.Medicion;
import com.puremadrid.core.model.Station;
import com.puremadrid.core.utils.GlobalUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Logger;

import static com.puremadrid.api.MainServlet.isPureMadrid;
import static com.puremadrid.core.model.Compuesto.*;

/**
 * Created by jdelgado on 15/11/2016.
 */

public class Parser {

    private static final Logger mLogger = Logger.getLogger(Parser.class.getName());

    public Parser() throws MalformedURLException {
    }

    public static void main(String[] args) {
        // Parse local
    }

    public static List<Medicion> parseFromHorarios(Calendar lastValidMeasure, InputStream stream) throws IOException {

        int lastTime = lastValidMeasure.get(Calendar.HOUR_OF_DAY);
        int lastDay = lastValidMeasure.get(Calendar.DATE);
        int lastMonth = lastValidMeasure.get(Calendar.MONTH)+1;
        int lastYear = lastValidMeasure.get(Calendar.YEAR);

        BufferedReader in = new BufferedReader(new InputStreamReader(stream));

        mLogger.info("Last Date: Year " + lastYear + " : month " + lastMonth + " : day " + lastDay + " : hour " + lastTime);

        //TABLE MAP: HORA, ESTACION, VALOR
        String line;
        Table<Calendar,String,Object> tableNO2 = HashBasedTable.create();
        Table<Calendar,String,Object> tableSO2 = HashBasedTable.create();
        Table<Calendar,String,Object> tableCO = HashBasedTable.create();
        Table<Calendar,String,Object> tableO3 = HashBasedTable.create();
        Table<Calendar,String,Object> tableTOL = HashBasedTable.create();

        Table<Calendar,String,Object> tableBEN = HashBasedTable.create();
        Table<Calendar,String,Object> tablePM25 = HashBasedTable.create();
        Table<Calendar,String,Object> tablePM10 = HashBasedTable.create();

        while ((line = in.readLine()) != null) {

            StringTokenizer tokenizer = new StringTokenizer(line, ",");
            // Estas tres van multiplicadas tantas veces como parametros
            int province = Integer.parseInt(tokenizer.nextToken()); // 28 for Comunidad de Madrid (ALL)
            int city = Integer.parseInt(tokenizer.nextToken()); // 79 for Municipio de Madrid (ALL)
            int stationNumber = Integer.parseInt(tokenizer.nextToken());
            //
            int measuredParameter = Integer.parseInt(tokenizer.nextToken());
            int measuredTechnique = Integer.parseInt(tokenizer.nextToken());

            if (!isMeasureUsed(measuredParameter)){
                continue;
            }

            //
            int periodo = Integer.parseInt(tokenizer.nextToken()); // Si es 4 es diario
            // FECHA REAL
            int anyo = Integer.parseInt(tokenizer.nextToken());
            int mes = Integer.parseInt(tokenizer.nextToken());
            int dia = Integer.parseInt(tokenizer.nextToken());
            //
            for (int i = 1; i <= 24; i++) {

                Number value = parseValue(measuredParameter, tokenizer.nextToken());
                boolean valid = tokenizer.nextToken().equals("V"); // V o N, valido o no
                if (valid) {
                    Calendar currentCalendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
                    currentCalendar.set(Calendar.YEAR,anyo);
                    currentCalendar.set(Calendar.MONTH,mes-1);
                    currentCalendar.set(Calendar.DATE,dia);
                    currentCalendar.set(Calendar.HOUR_OF_DAY,i-1);
                    currentCalendar.set(Calendar.MINUTE,0);
                    currentCalendar.set(Calendar.SECOND,0);
                    currentCalendar.set(Calendar.MILLISECOND,0);
                    currentCalendar.add(Calendar.HOUR_OF_DAY,1);

//                    mLogger.info("Hora anterior: " + dateFormat.format(lastValidMeasure.getTime()));
//                    mLogger.info("Hora analizada: " + dateFormat.format(currentCalendar.getTime()));
                    if (currentCalendar.getTimeInMillis() > lastValidMeasure.getTimeInMillis()) {
//                        mLogger.info("Insertando");
                        Compuesto compuesto = Compuesto.withId(measuredParameter);
                        switch (compuesto){
                            case NO2:
                                tableNO2.put(currentCalendar, "estacion_" + GlobalUtils.intTwoDigits(stationNumber), value);
                                break;
                            case PM2_5:
                                tablePM25.put(currentCalendar, "estacion_" + GlobalUtils.intTwoDigits(stationNumber), value);
                                break;
                            case PM10:
                                tablePM10.put(currentCalendar, "estacion_" + GlobalUtils.intTwoDigits(stationNumber), value);
                                break;
                            case O3:
                                tableO3.put(currentCalendar, "estacion_" + GlobalUtils.intTwoDigits(stationNumber), value);
                                break;
                            case CO:
                                tableCO.put(currentCalendar, "estacion_" + GlobalUtils.intTwoDigits(stationNumber), value);
                                break;
                            case SO2:
                                tableSO2.put(currentCalendar, "estacion_" + GlobalUtils.intTwoDigits(stationNumber), value);
                                break;
                            case TOL:
                                tableTOL.put(currentCalendar, "estacion_" + GlobalUtils.intTwoDigits(stationNumber), value);
                                break;
                            case BEN:
                                tableBEN.put(currentCalendar, "estacion_" + GlobalUtils.intTwoDigits(stationNumber), value);
                                break;

                        }
                    } else {
//                        mLogger.info("NO nsertando");
                        // continue
                    }
                } else {
                    // continue
                }
            }
        }
        List<Medicion> mediciones = formatData(
                tableNO2, tableSO2, tableCO, tableO3,
                tableTOL, tableBEN, tablePM25, tablePM10);
        return mediciones;
    }



    private static Number parseValue(int measuredParameter, String readValue) {
        Compuesto compuesto = Compuesto.withId(measuredParameter);
        switch (compuesto) {
            case NO2:
            case PM2_5:
            case PM10:
            case SO2:
            case O3:
                int intValue = Integer.parseInt(readValue);
                return intValue;
            case CO:
            case TOL:
            case BEN:
                float floatValue = Float.parseFloat(readValue);
                return floatValue;
        }
        return -1;
    }

    public static List<Medicion> parseFromMissingDay(BufferedReader in) throws IOException, ParseException {

        String line0 = in.readLine();
        StringTokenizer tokenizer = new StringTokenizer(line0, ",");
        String compuesto = tokenizer.nextToken(); // Keep format
        String fecha = tokenizer.nextToken();

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
        Date date = formatter.parse(fecha);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendar.setTime(date);

        in.readLine(); // Linea vacia, keep format
        //
        Table<Calendar, String, Object> tableMap = HashBasedTable.create();
        Station[] stations = new Gson().fromJson(GlobalUtils.getString(GlobalUtils.getInputStream("stations.json")), Station[].class);
        int itemNumber = 0;
        String line = null;
        while ((line = in.readLine()) != null) {
            tokenizer = new StringTokenizer(line, ",");
            String station = tokenizer.nextToken();
            //
            for (int i = 1; i <= 24; i++) {
                Calendar measuredAt = Calendar.getInstance(TimeZone.getTimeZone("CET"));
                measuredAt.set(Calendar.YEAR,calendar.get(Calendar.YEAR));
                measuredAt.set(Calendar.MONTH,calendar.get(Calendar.MONTH));
                measuredAt.set(Calendar.DATE,calendar.get(Calendar.DATE));
                measuredAt.set(Calendar.HOUR_OF_DAY,i-1);
                measuredAt.set(Calendar.MINUTE,0);
                measuredAt.set(Calendar.SECOND,0);
                measuredAt.set(Calendar.MILLISECOND,0);
                measuredAt.add(Calendar.HOUR_OF_DAY,1);
                int value = 0;
                try {
                    value = Integer.parseInt(tokenizer.nextToken());
                } catch (NumberFormatException e){

                }
                tableMap.put(measuredAt, "estacion_" + GlobalUtils.intTwoDigits(stations[itemNumber].getId()), value);
            }
            itemNumber++;
        }

        List<Medicion> formattedData = formatData(tableMap, null, null, null, null, null, null, null); // ONLY NO2
        return formattedData;
    }




    private static List<Medicion> formatData(
            Table<Calendar, String, Object> tableNO2,
            Table<Calendar, String, Object> tableSO2,
            Table<Calendar, String, Object> tableCO,
            Table<Calendar, String, Object> tableO3,
            Table<Calendar, String, Object> tableTOL,
            Table<Calendar, String, Object> tableBEN,
            Table<Calendar, String, Object> tablePM25,
            Table<Calendar, String, Object> tablePM10) {

        List<Medicion> mediciones = new ArrayList<>();
        //

        Calendar savedTime = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        for(int i = 0 ; i < tableNO2.rowKeySet().size(); i++) {

            List<Calendar> listNO2 = new ArrayList<>(tableNO2.rowKeySet());
            Calendar time = listNO2.get(i);

            Medicion medicion = new Medicion(isPureMadrid());
            medicion.setMeasuredAt(time.getTime());
            medicion.setSavedAtHour(Calendar.getInstance().getTime());

            // Parse each Compuesto
            medicion.put(NO2,formatValues(time,tableNO2));
            medicion.put(CO,formatValues(time,tableCO));
            medicion.put(SO2,formatValues(time,tableSO2));
            medicion.put(BEN,formatValues(time,tableBEN));
            medicion.put(O3,formatValues(time,tableO3));
            medicion.put(PM10,formatValues(time,tablePM10));
            medicion.put(PM2_5,formatValues(time,tablePM25));
            medicion.put(TOL,formatValues(time,tableTOL));

            Map<String, String> emailMap = medicion.computeAlertas();
            if (emailMap.containsKey("subject")) {
                mLogger.info("Must send email with subject: " + emailMap.get("subject"));
            }
            if (emailMap != null && emailMap.size() >= 0 && emailMap.containsKey("title")){
                EmailUtils.sendEmail(emailMap.get("title"),emailMap.get("subject"));
            }
            //
            mediciones.add(medicion);
            savedTime.add(Calendar.MILLISECOND,1);
        }
        return mediciones;
    }

    private static Map<String, Object> formatValues(Calendar time, Table<Calendar, String, Object> tableNO2) {
        Map<String,Object> values = new HashMap<>();
        for(String column : tableNO2.columnKeySet()) {
            if (tableNO2.contains(time,column)) {
                Object value = tableNO2.get(time,column);
                values.put(column,value);
            }
        }
        return values;
    }


}