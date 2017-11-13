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
