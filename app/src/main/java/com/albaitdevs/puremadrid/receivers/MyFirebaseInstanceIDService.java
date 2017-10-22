package com.albaitdevs.puremadrid.receivers;

import android.util.Log;

import com.albaitdevs.puremadrid.BuildConfig;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

/**
 * Created by Delga on 27/11/2016.
 */
public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TOPIC_GLOBAL = "global";
    private static final String TOPIC_ESCENARIOS = "scenarios";
    private static final String TOPIC_TESTING = "testing";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("TOKEN", "Refreshed token: " + refreshedToken);

        try {
            subscribeTopics(refreshedToken,TOPIC_GLOBAL);
            subscribeTopics(refreshedToken,TOPIC_ESCENARIOS);
            if (BuildConfig.DEBUG) {
                subscribeTopics(refreshedToken, TOPIC_TESTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
//        sendRegistrationToServer(refreshedToken);

    }

    private void subscribeTopics(String token, String topic) throws IOException {
        FirebaseMessaging.getInstance().subscribeToTopic(topic);
    }
}
