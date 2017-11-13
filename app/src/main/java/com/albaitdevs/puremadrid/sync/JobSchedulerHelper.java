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
package com.albaitdevs.puremadrid.sync;

import android.content.Context;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

import java.util.concurrent.TimeUnit;

public class JobSchedulerHelper {

    private static final int REMINDER_INTERVAL_MINUTES = 30;
    private static final int REMINDER_INTERVAL_SECONDS = (int) (TimeUnit.MINUTES.toSeconds(REMINDER_INTERVAL_MINUTES));
    private static final int SYNC_FLEXTIME_MINUTES = REMINDER_INTERVAL_MINUTES + 5;
    private static final int SYNC_FLEXTIME_SECONDS = (int) (TimeUnit.MINUTES.toSeconds(SYNC_FLEXTIME_MINUTES));

    private static final String SCHEDULER_JOB_TAG = "tag_get_last_status";

    private static boolean sInitialized;

    synchronized public static void getNewLevelsSetup(@NonNull final Context context) {

        if (sInitialized){
            return;
        }

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        /* Create the Job to periodically create reminders to drink water */
        Job constraintReminderJob = dispatcher.newJobBuilder()
                /* The Service that will be used to write to preferences */
                .setService(GetLastStatusJobService.class)
                /* Set the tag used to identify this Job. */
                .setTag(SCHEDULER_JOB_TAG)
                /* Live forever */
                .setLifetime(Lifetime.FOREVER)
                /* We want these reminders to continuously happen, so we tell this Job to recur. */
                .setRecurring(true)
                /* Trigger */
                .setTrigger(Trigger.executionWindow(
                        REMINDER_INTERVAL_SECONDS,
                        SYNC_FLEXTIME_SECONDS))
                /*
                 * If a Job with the tag with provided already exists, this new job will replace
                 * the old one.
                 */
                .setReplaceCurrent(true)
                /* Once the Job is ready, call the builder's build method to return the Job */
                .build();

        /* Schedule the Job with the dispatcher */
        dispatcher.schedule(constraintReminderJob);

        /* The job has been initialized */
        sInitialized = true;
    }

}
