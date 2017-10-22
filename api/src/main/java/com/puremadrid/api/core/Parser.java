package com.puremadrid.api.core;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.puremadrid.api.utils.EmailUtils;
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
        Map<String,Table<Calendar,String,Integer>> valueArray = new HashMap<>();
        Table<Calendar,String,Integer> tableMap = HashBasedTable.create();
        while ((line = in.readLine()) != null) {

            StringTokenizer tokenizer = new StringTokenizer(line, ",");
            // Estas tres van multiplicadas tantas veces como parametros
            int province = Integer.parseInt(tokenizer.nextToken()); // 28 for Comunidad de Madrid (ALL)
            int city = Integer.parseInt(tokenizer.nextToken()); // 79 for Municipio de Madrid (ALL)
            int stationNumber = Integer.parseInt(tokenizer.nextToken());
            //
            int measuredParameter = Integer.parseInt(tokenizer.nextToken());
            int measuredTechnique = Integer.parseInt(tokenizer.nextToken());

            //Evaluar solo NO2
            if (measuredParameter != 8) {
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

                int value = Integer.parseInt(tokenizer.nextToken()); // Valor del elemento medido
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
                        tableMap.put(currentCalendar, "estacion_" + GlobalUtils.intTwoDigits(stationNumber), value);
                    } else {
//                        mLogger.info("NO nsertando");
                        // continue
                    }
                } else {
                    // continue
                }
            }
        }
        List<Medicion> mediciones = formatData(tableMap, 8); // ONLY NO2
        return mediciones;
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
        Table<Calendar, String, Integer> tableMap = HashBasedTable.create();
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

        List<Medicion> formattedData = formatData(tableMap, 8); // ONLY NO2
        return formattedData;
    }




    private static List<Medicion> formatData(Table<Calendar, String, Integer> tableMap, int compuesto) {
        List<Medicion> mediciones = new ArrayList<>();
        //

        Calendar savedTime = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        for(Calendar row : tableMap.rowKeySet()) {
            Medicion medicion = new Medicion(isPureMadrid());
            medicion.setCompuesto(compuesto); // NO2
            medicion.setSavedAtHour((Calendar) savedTime.clone());
            medicion.setMeasuredAt(row);
            //
            for(String column : tableMap.columnKeySet()) {
                if (tableMap.contains(row,column)) {
                    medicion.add(column,tableMap.get(row,column));
                }
            }
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



}