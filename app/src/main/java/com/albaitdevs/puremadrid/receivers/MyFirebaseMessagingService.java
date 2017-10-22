package com.albaitdevs.puremadrid.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.albaitdevs.puremadrid.BuildConfig;
import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.activities.MainActivity;
import com.albaitdevs.puremadrid.data.PureMadridDbHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;
import com.puremadrid.core.model.ApiMedicion.Escenario;
import com.puremadrid.core.model.ApiMedicion.Estado;
import com.puremadrid.core.model.ApiResponse.NotificationData;
import com.puremadrid.core.model.Constants;

/**
 * Created by Delga on 27/11/2016.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_IS_TODAY = "isToday";
    private static final String KEY_LEVEL_COLOR = "levelColor";
    private static final String KEY_SEND_NOTI = "showAlways";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        String from = remoteMessage.getFrom();

        boolean acceptTopicTesting = BuildConfig.DEBUG && from.startsWith("/topics/testing");

        if (remoteMessage.getData().size() > 0) {
            Log.d("Notis", "Payload" + remoteMessage.getData());
        }

        if (from.startsWith("/topics/global") || from.startsWith("/topics/scenarios")) {

            try {
    //            Log.d("NOTIFICATION", "From: " + remoteMessage.getFrom());

                // Check if message contains a data payload.
    //            if (remoteMessage.getData().size() > 0) {
    //                Log.d("NOTIFICATION", "Message data payload: " + remoteMessage.getData());
    //            }

                // Check if message contains a notification payload.
    //            if (remoteMessage.getNotification() != null) {
    //                Log.d("NOTIFICATION", "Message Notification Body: " + remoteMessage.getNotification().getBody());
    //            }

                // Get new values
                NotificationData data = new NotificationData(remoteMessage.getData());
                Estado newCurrentEstado = Estado.valueOf(data.getCurrentStatus());
                Estado newValidEstado = Estado.valueOf(data.getValidStatus());
                Estado newMaxEstado = Estado.valueOf(data.getMaxStatus());
                Escenario newEscenarioToday= Escenario.valueOf(data.getEscenarioToday());
                Escenario newEscenarioTomorrow = Escenario.valueOf(data.getEscenarioTomorrow());

                // Save data
                ApiMedicion lastMedicion = PureMadridDbHelper.getLastMeasureNO2(getApplicationContext());
                Estado oldCurrentEstado = Estado.valueOf(lastMedicion.getAvisoState());
                Estado oldValidEstado = Estado.valueOf(lastMedicion.getAviso());
                Estado oldMaxEstado = Estado.valueOf(lastMedicion.getAvisoMaxToday());
                Escenario oldEscenarioToday= Escenario.valueOf(lastMedicion.getEscenarioStateToday());
                Escenario oldEscenarioTomorrow = Escenario.valueOf(lastMedicion.getEscenarioStateTomorrow());

                // STORE
                lastMedicion.setEscenarioStateTomorrow(newEscenarioTomorrow.name());
                lastMedicion.setEscenarioStateToday(newEscenarioToday.name());
                lastMedicion.setAviso(newCurrentEstado.name());
                lastMedicion.setAvisoState(newValidEstado.name());
                lastMedicion.setAvisoMaxToday(newMaxEstado.name());
                PureMadridDbHelper.updateLastMeasure(getApplicationContext(),lastMedicion);

                if (data.getFlags().equals(Constants.FLAG_SET_ALWAYS_TOMORROW)
                        || data.getFlags().equals(Constants.FLAG_SET_BOTH)){
                    sendNotiTomorrow(newEscenarioTomorrow);
                }

                if (data.getFlags().equals(Constants.FLAG_SET_ALWAYS_TODAY)
                        || data.getFlags().equals(Constants.FLAG_SET_BOTH)){
                    sendNotiToday(newEscenarioToday);
                }

                // Valores elevados
                if (newMaxEstado.ordinal() > oldMaxEstado.ordinal()){

                }
                if (newValidEstado.ordinal() > oldValidEstado.ordinal()){

                }
                if (newCurrentEstado.ordinal() > oldCurrentEstado.ordinal()){

                }

            } catch (Exception e){
                e.printStackTrace();
            }

        } else if (from.startsWith("/topics/custom") || acceptTopicTesting){
            String title = remoteMessage.getData().get(KEY_TITLE);
            String message = remoteMessage.getData().get(KEY_MESSAGE);
            boolean isToday = remoteMessage.getData().get(KEY_IS_TODAY).equals("true");
            boolean sendNoti = remoteMessage.getData().get(KEY_SEND_NOTI).equals("true");
            int levelColor = Integer.parseInt(remoteMessage.getData().get(KEY_LEVEL_COLOR));
            //
            int color = prepareColor(levelColor);
            int notiCode = isToday ? 1 : 0;
            //
            if (sendNoti) {
                sendNotification(notiCode, title, message, color);
            }
        }
    }

    private int prepareColor(int levelColor) {
        int color = getResources().getColor(R.color.colorPrimary);
        switch (levelColor){
            case 0:
                break;
            case 1:
                color = getResources().getColor(R.color.yellow);
                break;
            case 2:
                color = getResources().getColor(R.color.orange);
                break;
            case 3:
                color = getResources().getColor(R.color.red);
                break;
            case 4:
                color = getResources().getColor(R.color.red);
                break;
        }
        return color;
    }

    private void sendNotiToday(Escenario newEscenarioToday) {
        String message = getString(R.string.scenario_activaded_today_is_noti);
        String title = "";
        int color = getResources().getColor(R.color.colorPrimary);

        switch (newEscenarioToday){
            case NONE:
                title = getString(R.string.scenario_deactivated_title_today);
                message = getString(R.string.scenario_deactivated_message);
                break;
            case ESCENARIO1:
                title = getString(R.string.scenario1_title_lower) + " " + getString(R.string.activado_hoy);
                message += " 1";
                color = getResources().getColor(R.color.yellow);
                break;
            case ESCENARIO2:
                title = getString(R.string.scenario2_title_lower) + " " + getString(R.string.activado_hoy);
                message += " 2";
                color = getResources().getColor(R.color.orange);
                break;
            case ESCENARIO3:
                title = getString(R.string.scenario3_title_lower) + " " + getString(R.string.activado_hoy);
                message += " 3";
                color = getResources().getColor(R.color.red);
                break;
            case ESCENARIO4:
                title = getString(R.string.scenario4_title_lower) + " " + getString(R.string.activado_hoy);
                message += " 4";
                color = getResources().getColor(R.color.red);
                break;
        }
        sendNotification(1, title, message, color);
    }

    private void sendNotiTomorrow(Escenario newEscenarioTomorrow) {
        String message = getString(R.string.scenario_activaded_tomorrow_is_noti);
        String title = "";
        int color = getResources().getColor(R.color.colorPrimary);

        switch (newEscenarioTomorrow){
            case NONE:
                title = getString(R.string.scenario_deactivated_title_tomorrow);
                message = getString(R.string.scenario_deactivated_message_tomorrow);
                break;
            case ESCENARIO1:
                title = getString(R.string.scenario1_title_lower) + " " + getString(R.string.activado_manana);
                message += " 1";
                color = getResources().getColor(R.color.yellow);
                break;
            case ESCENARIO2:
                title = getString(R.string.scenario2_title_lower) + " " + getString(R.string.activado_manana);
                message += " 2";
                color = getResources().getColor(R.color.orange);
                break;
            case ESCENARIO3:
                title = getString(R.string.scenario3_title_lower) + " " + getString(R.string.activado_manana);
                message += " 3";
                color = getResources().getColor(R.color.red);
                break;
            case ESCENARIO4:
                title = getString(R.string.scenario4_title_lower) + " " + getString(R.string.activado_manana);
                message += " 4";
                color = getResources().getColor(R.color.red);
                break;
        }
        sendNotification(0, title, message, color);
    }

    private void sendNotification(int id, String title, String message, int color) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.KEY_OPENED_FROM_NOTIFICATION, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,"")
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setColor(color)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setAutoCancel(true)
                ;

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSound(defaultSoundUri);

        notificationBuilder.setVibrate(new long[]{1000, 1000});

        notificationBuilder.setLights(color, 1000, 1000);


        Notification notification = notificationBuilder.build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(id, notification);
    }
}
