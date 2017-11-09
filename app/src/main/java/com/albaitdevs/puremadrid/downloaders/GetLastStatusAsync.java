package com.albaitdevs.puremadrid.downloaders;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.data.PureMadridDbHelper;
import com.albaitdevs.puremadrid.sync.GetLastStatusJobService;
import com.albaitdevs.puremadrid.widget.PureMadridWidget;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.util.DateTime;
import com.puremadrid.api.pureMadridApi.PureMadridApi;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;

import java.io.IOException;
import java.util.Date;

 public class GetLastStatusAsync  extends AsyncTask<Object, Object, ApiMedicion> {


     private static final int MAX_RETRIES = 10;
     private int retries = 0;

     public interface ApiListener{
        void onApiFinished(ApiMedicion result);
    }

    private PureMadridApi myApiService = null;
    private ApiListener listener;
    private Context mContext;
    private Date date;
     private boolean isFromBackgound = false;

    public GetLastStatusAsync(Context context, ApiListener listener, Date date){
        this.listener = listener;
        this.mContext = context;
        this.date = date;
        if (listener instanceof GetLastStatusJobService){
            isFromBackgound = true;
        }
    }

    @Override
    protected void onPreExecute() {
        if (listener == null){
            throw new RuntimeException("Interface ApiListener not implemented");
        }
    }

    @Override
    protected ApiMedicion doInBackground(Object... params) {
        ApiMedicion result = getLastStatusRequest(isFromBackgound);
        while (result == null && isFromBackgound && retries < MAX_RETRIES){
            retries++;
            result = getLastStatusRequest(isFromBackgound);
        }

        if (result != null) {
            // Update database in background thread
            PureMadridDbHelper.addMeasure(mContext, result);

            // Update Widget
            Intent intent = new Intent(mContext, PureMadridWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            ComponentName thisWidget = new ComponentName(mContext, PureMadridWidget.class);
            int[] appWidgetIds = AppWidgetManager.getInstance(mContext).getAppWidgetIds(thisWidget);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            mContext.sendBroadcast(intent);

        }
        return result;
    }

     private ApiMedicion getLastStatusRequest(boolean isFromBackgound) {
         ApiMedicion result = null;
         try {
             if (myApiService == null) {  // Only do this once
                 PureMadridApi.Builder builder = new PureMadridApi.Builder(AndroidHttp.newCompatibleTransport(),
                         new AndroidJsonFactory(), null)
                         // options for running against local devappserver
                         // - 10.0.2.2 is localhost's IP address in Android emulator
                         // - turn off compression date running against local devappserver
                         // .setRootUrl("http://10.0.2.2:8080/_ah/api/")
                         .setRootUrl(mContext.getString(R.string.pure_madrid_api_url))
                         .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                             @Override
                             public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                                 abstractGoogleClientRequest.setDisableGZipContent(true);
                             }
                         });

                 myApiService = builder.build();
             }

             if (date == null) {
                 result = myApiService.getLastStatus().execute();
             } else {
                 result = myApiService.getStatusAt(new DateTime(date)).execute();
             }
         } catch (IOException e) {
             return null;
         }
         return result;
     }

    @Override
    protected void onPostExecute(ApiMedicion result) {
        listener.onApiFinished(result);
    }

}
