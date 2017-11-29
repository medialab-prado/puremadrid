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

package com.albaitdevs.puremadrid.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.activities.MainActivity;
import com.albaitdevs.puremadrid.data.PureMadridDbHelper;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;

/**
 * Implementation of App Widget functionality.
 */
public class PureMadridWidget extends AppWidgetProvider {


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Get data
        ApiMedicion currentPollution = PureMadridDbHelper.getLastMeasureNO2(context);
        com.puremadrid.core.model.ApiMedicion.Escenario esceneToday = com.puremadrid.core.model.ApiMedicion.Escenario.valueOf(currentPollution.getEscenarioStateToday());
        com.puremadrid.core.model.ApiMedicion.Escenario esceneTomorrow = com.puremadrid.core.model.ApiMedicion.Escenario.valueOf(currentPollution.getEscenarioStateTomorrow());
        if (currentPollution != null){
            esceneToday = com.puremadrid.core.model.ApiMedicion.Escenario.valueOf(currentPollution.getEscenarioStateToday());
            esceneTomorrow = com.puremadrid.core.model.ApiMedicion.Escenario.valueOf(currentPollution.getEscenarioStateTomorrow());
        }


        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pure_madrid_widget);

        int colorToday = R.color.blue;
        String textToday = context.getString(R.string.scenario_none_title);
        if (esceneToday.ordinal() > com.puremadrid.core.model.ApiMedicion.Escenario.ESCENARIO3.ordinal()) {
            colorToday = R.color.red;
            textToday = context.getString(R.string.scenario4_title_lower);
        } else if (esceneToday.ordinal() > com.puremadrid.core.model.ApiMedicion.Escenario.ESCENARIO2.ordinal()){
            colorToday = R.color.red;
            textToday = context.getString(R.string.scenario3_title_lower);
        } else if (esceneToday.ordinal() > com.puremadrid.core.model.ApiMedicion.Escenario.ESCENARIO1.ordinal()){
            colorToday = R.color.orange;
            textToday = context.getString(R.string.scenario2_title_lower);
        } else if (esceneToday.ordinal() > com.puremadrid.core.model.ApiMedicion.Escenario.NONE.ordinal()){
            colorToday = R.color.yellow;
            textToday = context.getString(R.string.scenario1_title_lower);
        }

        int colorTomorrow = R.color.blue;
        String textTomorrow = context.getString(R.string.scenario_none_title);
        if (esceneTomorrow.ordinal() > com.puremadrid.core.model.ApiMedicion.Escenario.ESCENARIO3.ordinal()) {
            colorTomorrow = R.color.red;
            textTomorrow = context.getString(R.string.scenario4_title_lower);
        } else if (esceneTomorrow.ordinal() > com.puremadrid.core.model.ApiMedicion.Escenario.ESCENARIO2.ordinal()){
            colorTomorrow = R.color.red;
            textTomorrow = context.getString(R.string.scenario3_title_lower);
        } else if (esceneTomorrow.ordinal() > com.puremadrid.core.model.ApiMedicion.Escenario.ESCENARIO1.ordinal()){
            colorTomorrow = R.color.orange;
            textTomorrow = context.getString(R.string.scenario2_title_lower);
        } else if (esceneTomorrow.ordinal() > com.puremadrid.core.model.ApiMedicion.Escenario.NONE.ordinal()){
            colorTomorrow = R.color.yellow;
            textTomorrow = context.getString(R.string.scenario1_title_lower);
        }

        // Set views
        views.setTextViewText(R.id.appwidget_today_text, context.getString(R.string.hoy) + ":\n" + textToday);
        views.setTextViewText(R.id.appwidget_tomorrow_text, context.getString(R.string.manana) + ":\n" + textTomorrow);
        views.setInt(R.id.appwidget_today_text, "setBackgroundResource", colorToday);
        views.setInt(R.id.appwidget_tomorrow_text, "setBackgroundResource", colorTomorrow);

        // On click intent
        Intent intent = new Intent(context,MainActivity.class);
        intent.putExtra(MainActivity.KEY_OPENED_FROM_WIDGET, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.appwidget_root, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

