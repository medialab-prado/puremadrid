/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.albaitdevs.puremadrid.sync;

import android.os.AsyncTask;
import android.util.Log;

import com.albaitdevs.puremadrid.downloaders.GetLastStatusAsync;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;

public class GetLastStatusJobService extends JobService implements GetLastStatusAsync.ApiListener {

    private AsyncTask mBackgroundTask;
    private JobParameters mJobParameters;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.d("JOB","Executing...");
        this.mJobParameters = jobParameters;
        mBackgroundTask = new GetLastStatusAsync(this,this,null).execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mBackgroundTask != null){
            mBackgroundTask.cancel(true);
        }
        return true;
    }

    @Override
    public void onApiFinished(ApiMedicion result) {
        jobFinished(mJobParameters, false);
        Log.d("JOB","Job executed");
    }
}