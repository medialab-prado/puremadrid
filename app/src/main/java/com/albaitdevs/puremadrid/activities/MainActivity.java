package com.albaitdevs.puremadrid.activities;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.albaitdevs.puremadrid.downloaders.DownloadFilesUtils;
import com.albaitdevs.puremadrid.downloaders.GetLastStatusAsync;
import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.data.DataBaseLoader;
import com.albaitdevs.puremadrid.fragments.FeedbackFragment;
import com.albaitdevs.puremadrid.fragments.InfoFragment;
import com.albaitdevs.puremadrid.fragments.PredictionFragment;
import com.albaitdevs.puremadrid.fragments.ProtocolosFragment;
import com.albaitdevs.puremadrid.fragments.MainFragment;
import com.albaitdevs.puremadrid.fragments.MyMapFragment;
import com.albaitdevs.puremadrid.receivers.MyFirebaseInstanceIDService;
import com.albaitdevs.puremadrid.sync.JobSchedulerHelper;
import com.albaitdevs.puremadrid.utils.DeviceInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.MapsInitializer;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;
import com.puremadrid.core.model.ApiMedicion.Escenario;
import com.puremadrid.core.utils.GlobalUtils;

import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.puremadrid.core.model.ApiMedicion.Estado;
import com.puremadrid.core.model.ApiResponse.NotificationData;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.puremadrid.core.utils.GlobalUtils.getJsonFromUrl;

public class MainActivity extends Activity implements NavigationView.OnNavigationItemSelectedListener, GetLastStatusAsync.ApiListener, DataBaseLoader.DataBaseLoaderCallbacks {

    private static final String KEY_CURRENT_NAVIGATION_ITEM = "key_current_navigation_item";
    private static final String KEY_CURRENT_VIEWS_COLOR = "key_current_color";
    private static final String KEY_CURRENT_VIEWS_COLOR_DARK = "key_current_color_dark";

    // Instances and constants
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static final int POSITION_MAIN = 0;
    public static final int POSITION_MAP = 1;
    public static final int POSITION_PREDICTION = 2;
    public static final int POSITION_PROTOCOLO = 3;
    public static final int POSITION_LAST_AVISO = 4;
    public static final int POSITION_INFO = 5;
    public static final int POSITION_FEEDBACK = 6;

    public static final int LOADER_LAST_MEASURE = 123;
    public static final String KEY_OPENED_FROM_NOTIFICATION = "opened_from_noti";
    public static final String KEY_OPENED_FROM_WIDGET = "opened_from_widget";

    // State
    private static boolean wasGooglePlayServicesDialogClosed = false;
    private static boolean wasUpdateDialogClosed = false;


    private CharSequence previousSubtitle;
    private int mCurrentDrawerItem = 0;
    @BindView (R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView (R.id.main_toolbar) Toolbar mToolbar;
    @BindView (R.id.navigation_view) NavigationView mNavigationView;
    @Nullable @BindView (android.R.id.statusBarBackground) View mStatusBar;

    private int mCurrentColor;
    private int mCurrentColorDark;

    private ApiMedicion mCurrentPollution;
    private ValueAnimator mColorAnimationToolbar;
    private ValueAnimator mColorAnimationStatusBar;
    private int mStatusColorTo;
    private int mColorTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Get tracker
        if (savedInstanceState != null) {
            mCurrentDrawerItem = savedInstanceState.getInt(KEY_CURRENT_NAVIGATION_ITEM);
            mCurrentColor = savedInstanceState.getInt(KEY_CURRENT_VIEWS_COLOR);
            mCurrentColorDark = savedInstanceState.getInt(KEY_CURRENT_VIEWS_COLOR_DARK);
            mToolbar.setBackgroundColor(mCurrentColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(mCurrentColorDark);
            }
        } else {
            mCurrentColor = getResources().getColor(R.color.blue);
            mCurrentColorDark = getResources().getColor(R.color.blue_dark);
        }
        mColorTo = mCurrentColor;
        mStatusColorTo = mCurrentColorDark;

        // Start Job Scheduler
        JobSchedulerHelper.getNewLevelsSetup(this);

        // Drawer Toggle
        mToolbar.inflateMenu(R.menu.toolbar_menu);
        mToolbar.setTitle(R.string.app_name);
        mNavigationView.setNavigationItemSelectedListener(this);
        mToolbar.setSubtitle(R.string.menuitem_estado);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.open_drawer_action, R.string.close_drawer_action) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                mToolbar.setSubtitle(previousSubtitle);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                previousSubtitle = mToolbar.getSubtitle();
                mToolbar.setSubtitle(R.string.menu);
            }
        };
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        replaceFragment(mCurrentDrawerItem);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentEstado = prefs.getString(NotificationData.currentEstadoName,Escenario.NONE.name());
        String validEstado = prefs.getString(NotificationData.validEstadoName,Escenario.NONE.name());
        String maxEstado = prefs.getString(NotificationData.maxEstadoName,Escenario.NONE.name());
        String escenarioToday = prefs.getString(NotificationData.escenarioNameToday,Escenario.NONE.name());
        String escenarioTomorrow = prefs.getString(NotificationData.escenarioNameTomorrow,Escenario.NONE.name());

        Escenario esceneToday = Escenario.valueOf(escenarioToday);
        Escenario esceneTomorrow = Escenario.valueOf(escenarioTomorrow);
        Estado state = Estado.valueOf(currentEstado);


    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        item.setChecked(true);
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawers();
        }

        //Find the item position
        int position = -2;
        for (int i = 0; i < mNavigationView.getMenu().size(); i++) {
            if (item == mNavigationView.getMenu().getItem(i)) {
                position = i;
            }
        }

        if (position != mCurrentDrawerItem) {
            replaceFragment(position);
        } else {
            mDrawerLayout.closeDrawers();
        }

        return true;
    }

    private void clearBackStack(FragmentManager fragmentManager ){
        while (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate();
        }
    }

    /**
     *
     *
     * @param position is the new fragment
     */
    private void replaceFragment(int position) {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = null;
        String fragment_tag = "";

        //Delete not initialized state
        if (position == -1) {
            position = 0;
        }

        switch (position) {
            case POSITION_MAIN:
                fragment_tag = MainFragment.class.getName();
                fragment = fragmentManager.findFragmentByTag(fragment_tag);
                if (fragment == null) {
                    fragment = new MainFragment();
                }
                break;
            case POSITION_MAP:
                fragment_tag = MyMapFragment.class.getName();
                fragment = fragmentManager.findFragmentByTag(fragment_tag);
                if (fragment == null) {
                    fragment = new MyMapFragment();
                }
                break;
            case POSITION_PREDICTION:
                fragment_tag = PredictionFragment.class.getName();
                fragment = fragmentManager.findFragmentByTag(fragment_tag);
                if (fragment == null) {
                    fragment = new PredictionFragment();
                }
                break;
            case POSITION_PROTOCOLO:
                fragment_tag = ProtocolosFragment.class.getName();
                fragment = fragmentManager.findFragmentByTag(fragment_tag);
                if (fragment == null) {
                    fragment = new ProtocolosFragment();
                }
                break;
            case POSITION_LAST_AVISO:
                DownloadFilesUtils.downloadPdf(this,this, DownloadFilesUtils.EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_BOLETIN_DIARIO);
                return;
            case POSITION_INFO:
                fragment_tag = InfoFragment.class.getName();
                fragment = fragmentManager.findFragmentByTag(fragment_tag);
                if (fragment == null) {
                    fragment = new InfoFragment();
                }
                break;
            case POSITION_FEEDBACK:
                fragment_tag = FeedbackFragment.class.getName();
                fragment = fragmentManager.findFragmentByTag(fragment_tag);
                if (fragment == null) {
                    fragment = new FeedbackFragment();
                }
                break;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content_frame, fragment, fragment_tag);
        if (position != 0 && fragmentManager.getBackStackEntryCount() == 0) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
        mCurrentDrawerItem = position;

    }

    public void onItemChanged(int position) {
        switch (position) {
            case POSITION_MAIN:
                mToolbar.setSubtitle(R.string.menuitem_estado);
                break;
            case POSITION_MAP:
                mToolbar.setSubtitle(R.string.menuitem_mapa);
                break;
            case POSITION_PREDICTION:
                mToolbar.setSubtitle(R.string.menuitem_prediction);
                break;
            case POSITION_PROTOCOLO:
                mToolbar.setSubtitle(R.string.menuitem_protocolos);
                break;
            case POSITION_LAST_AVISO:
                mToolbar.setSubtitle(R.string.menuitem_last_announcement);
                break;
            case POSITION_INFO:
                mToolbar.setSubtitle(R.string.menuitem_info);
                break;
            case POSITION_FEEDBACK:
                mToolbar.setSubtitle(R.string.menuitem_feedback);
                break;
        }
        previousSubtitle = mToolbar.getSubtitle();
        mNavigationView.getMenu().getItem(position).setChecked(true);
        mCurrentDrawerItem = position;
    }

    @Override
    public void onDBFinished(ApiMedicion medicion) {
        mCurrentPollution = medicion;
        animateViews(mCurrentPollution);
        MainFragment fragment = (MainFragment) getFragmentManager().findFragmentByTag(MainFragment.class.getName());
        if (fragment != null && fragment.isAdded()){
            fragment.updateData(medicion);
        }
        new GetLastStatusAsync(this,this,null).execute();
    }

    @Override
    public void onApiFinished(ApiMedicion result) {
        if (result != null) {
            mCurrentPollution = result;
            animateViews(result);
            MainFragment fragment = (MainFragment) getFragmentManager().findFragmentByTag(MainFragment.class.getName());
            if (fragment != null && fragment.isAdded()){
                fragment.updateData(result);
            }
        } else {

        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_NAVIGATION_ITEM,mCurrentDrawerItem);
        outState.putInt(KEY_CURRENT_VIEWS_COLOR,mColorTo);
        outState.putInt(KEY_CURRENT_VIEWS_COLOR_DARK,mStatusColorTo);
    }

    private void animateViews(ApiMedicion currentPollution) {
        if (currentPollution != null){
            Escenario esceneToday = Escenario.valueOf(currentPollution.getEscenarioStateToday());

            int colorFrom = mCurrentColor;
            mColorTo = getResources().getColor(R.color.blue);
            int statusColorFrom = mCurrentColorDark;
            mStatusColorTo = getResources().getColor(R.color.blue_dark);
            //
            if (esceneToday.ordinal() > Escenario.ESCENARIO2.ordinal()){
                mColorTo = getResources().getColor(R.color.red);
                mStatusColorTo = getResources().getColor(R.color.red_dark);
            } else if (esceneToday.ordinal() > Escenario.ESCENARIO1.ordinal()){
                mColorTo = getResources().getColor(R.color.orange);
                mStatusColorTo = getResources().getColor(R.color.orange_dark);
            } else if (esceneToday.ordinal() > Escenario.NONE.ordinal()){
                mColorTo = getResources().getColor(R.color.yellow);
                mStatusColorTo = getResources().getColor(R.color.yellow_dark);
            }

            //
            if (mColorAnimationToolbar != null) {
                mColorAnimationToolbar.cancel();
            }
            mColorAnimationToolbar = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, mColorTo);
            mColorAnimationToolbar.setDuration(4000); // milliseconds
            mColorAnimationToolbar.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    mCurrentColor = (int) animator.getAnimatedValue();
                    mToolbar.setBackgroundColor(mCurrentColor);
                    mNavigationView.getHeaderView(0).setBackgroundColor(mCurrentColor);
                }
            });
            mColorAnimationToolbar.start();

            //
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mColorAnimationStatusBar != null) {
                    mColorAnimationStatusBar.cancel();
                }
                mColorAnimationStatusBar = ValueAnimator.ofObject(new ArgbEvaluator(), statusColorFrom, mStatusColorTo);
                mColorAnimationStatusBar.setDuration(4000); // milliseconds
                mColorAnimationStatusBar.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        mCurrentColorDark = (int) animator.getAnimatedValue();
                        getWindow().setStatusBarColor(mCurrentColorDark);
                    }
                });
                mColorAnimationStatusBar.start();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawer(Gravity.START);
        } else {
            // TODO: To be improved:
            try {
                super.onBackPressed();
            } catch (IllegalStateException e){
                Fragment fragment = getFragmentManager().findFragmentByTag(MainFragment.class.getName());
                if (fragment == null) {
                    fragment = new MainFragment();
                }
                if (!fragment.isAdded()) {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.content_frame, fragment, MainFragment.class.getName());
                    transaction.commit();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Animation
        // Load data if database is empty
        // Load last measure
        DataBaseLoader callbacks = new DataBaseLoader(this,this);
        getLoaderManager().restartLoader(LOADER_LAST_MEASURE, null, callbacks);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPlayServices(true)) {
            // Start IntentService to register this application with GCM.
            Intent registrationIntent = new Intent(this, MyFirebaseInstanceIDService.class);
            startService(registrationIntent);
        }

        // Check last version
        new GetLatestUpdatedVersions().execute();

        if (mCurrentPollution != null) {
            animateViews(mCurrentPollution);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getBooleanExtra(KEY_OPENED_FROM_NOTIFICATION,false)){
            intent.putExtra(KEY_OPENED_FROM_NOTIFICATION,false);
            clearBackStack(getFragmentManager());
            replaceFragment(POSITION_MAIN);

        } else if (intent.getBooleanExtra(KEY_OPENED_FROM_WIDGET,false)){
            intent.putExtra(KEY_OPENED_FROM_WIDGET,false);
            clearBackStack(getFragmentManager());
            replaceFragment(POSITION_MAIN);
        }

    }

    public void shareApp(MenuItem item) {

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.text_share_app));
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    public void openOfficialDocs(View v){
        DownloadFilesUtils.downloadPdf(this,this, DownloadFilesUtils.EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_REGLAMENTO);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getLoaderManager().destroyLoader(LOADER_LAST_MEASURE);
    }

    /**
     * Handle new versions of the app
     * <p/>
     * Async task
     */
    private class GetLatestUpdatedVersions extends AsyncTask<String, String, Integer> {

        @Override
        protected Integer doInBackground(String... params) {

            //Get Json from URL. HTTP Connection
            String url = getResources().getString(R.string.cloud_puremadrid_json);
            JSONObject jsonResponse = getJsonFromUrl(url);

            //Get variables from json
            if (jsonResponse != null) {
                try {
                    String fileDateStr = jsonResponse.getString("date");
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                    //Remote versions
                    Date remoteFileDate = formatter.parse(fileDateStr);
                    int remoteLastMajorVersion = Integer.parseInt(jsonResponse.getString("lastMajorVersion"));
                    int remoteLastMinorVersion = Integer.parseInt(jsonResponse.getString("lastMinorVersion"));
                    int remoteJsonModel = Integer.parseInt(jsonResponse.getString("jsonModel"));
                    int remoteFileVersion = Integer.parseInt(jsonResponse.getString("version"));

                    //Local versions-----------------------------------
                    InputStream file = getResources().openRawResource(R.raw.pure_madrid_main);
                    String localVersionFile = GlobalUtils.getString(file);
                    JSONObject jsonVersionLocal = new JSONObject(localVersionFile);
                    String localfileDateStr = jsonVersionLocal.getString("date");

                    //TRUE LOCALS
                    Date localFileDate = formatter.parse(localfileDateStr);
                    int localLastMajorVersion = Integer.parseInt(jsonVersionLocal.getString("lastMajorVersion"));
                    int localLastMinorVersion = Integer.parseInt(jsonVersionLocal.getString("lastMinorVersion"));
                    int localJsonModel = Integer.parseInt(jsonVersionLocal.getString("jsonModel"));
                    int localFileVersion = Integer.parseInt(jsonVersionLocal.getString("version"));

                    //Check main file version
                    //If there is not a new version, do nothing
                    if (remoteFileDate.after(localFileDate)) {
                        if (remoteLastMajorVersion > DeviceInfo.getVersionCode(getApplicationContext())) {

                            //Request release notes
                            //Get Json from URL. HTTP Connection
                            String requestUri = getResources().getString(R.string.cloud_pure_madrid_release_notes);
                            JSONObject releaseNotesJson = getJsonFromUrl(requestUri);

                            //Treat json
                            String field = "release" + remoteLastMajorVersion;
                            String releaseNotes = releaseNotesJson.getString(field);

                            //Show popup
                            handleNewVersionInGooglePlay(releaseNotes);
                        }
                    }

                } catch (Exception e) {
                    Log.d("APP_VERSION","Error getting new Version");
                    return -1;
                }

            }
            return 0;
        }

        protected void onPostExecute(String result) {

        }

    } // end CallAPI

    /**
     * Show alert to download new version of the app
     */
    private void handleNewVersionInGooglePlay(String releaseNotes) {
        if (!wasUpdateDialogClosed) {

            //Create alert
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.update_dialog_message) + "\n\n" + releaseNotes)
                    .setTitle(R.string.update_dialog_title);
            builder.setPositiveButton(R.string.update_dialog_iragoogleplay, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    //Start activity
                    String url = getResources().getString(R.string.link_googleplay_from_update_dialog);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton(R.string.update_dialog_ahorano, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    // User cancelled the dialog
                    wasUpdateDialogClosed = true;
                }
            });

            //Show alert in UI Thread
            runOnUiThread(new Runnable() {
                public void run() {
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                // User cancelled the dialog
                                wasUpdateDialogClosed = true;
                                //Send event

                            }
                            return false;
                        }
                    });
                    try {
                        alertDialog.show();
                    } catch (Exception e) {

                    }

                }
            });
        }
    }


    //
    private boolean checkPlayServices(boolean showDialog) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode) && showDialog) {
                showGooglePlayServicesErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                Log.i("GooglePlayServices", "This device is not supported.");
            }
            return false;
        }
        return true;
    }
    /**
     * Create and show error dialog if Google Play Services are not installed
     *
     * @param context
     * @param resultCode
     * @param playServicesResolutionRequest
     */
    private void showGooglePlayServicesErrorDialog(final Context context, final int resultCode, int playServicesResolutionRequest) {

        if (!wasGooglePlayServicesDialogClosed) {
            //Create alert
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.google_play_services_error_message));
            builder.setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //Start activity
                    PendingIntent pendingIntent = GoogleApiAvailability.getInstance().getErrorResolutionPendingIntent(context, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.setNegativeButton(R.string.update_dialog_ahorano, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    wasGooglePlayServicesDialogClosed = true;
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        // User cancelled the dialog
                        wasGooglePlayServicesDialogClosed = true;
                    }
                    return false;
                }
            });
            alertDialog.show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case DownloadFilesUtils.EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_BOLETIN_DIARIO:
            case DownloadFilesUtils.EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_REGLAMENTO:
            case DownloadFilesUtils.EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_MORE_INFORMATION:

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    DownloadFilesUtils.downloadPdf(this,this,requestCode);
                } else {
                    DownloadFilesUtils.showNotDownloadedToast(this, "" + requestCode);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }










    /**
     * EXPERIMENTS ========================================================
     */





//    private void shortcutAdd(String name, int number) {
//        // Intent to be send, when shortcut is pressed by user ("launched")
//        Intent shortcutIntent = new Intent(getApplicationContext(), MainActivity.class);
//        shortcutIntent.setAction("ASDA");
//
//        // Create bitmap with number in it -> very default. You probably want to give it a more stylish look
//        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
//        Paint paint = new Paint();
//        paint.setColor(0xFF808080); // gray
//        paint.setTextAlign(Paint.Align.CENTER);
//        paint.setTextSize(50);
//        new Canvas(bitmap).drawText(""+number, 50, 50, paint);
////        ((ImageView) findViewById(android.R.id.icon)).setImageBitmap(bitmap);
//
//        // Decorate the shortcut
//        Intent addIntent = new Intent();
//        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
//        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
//        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
//
//        // Inform launcher to create shortcut
//        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
//        getApplicationContext().sendBroadcast(addIntent);
//    }
//
//    private void shortcutDel(String name) {
//        // Intent to be send, when shortcut is pressed by user ("launched")
//        Intent shortcutIntent = new Intent(getApplicationContext(), MainActivity.class);
//        shortcutIntent.setAction("ASDA");
//
//        // Decorate the shortcut
//        Intent delIntent = new Intent();
//        delIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
//        delIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
//
//        // Inform launcher to remove shortcut
//        delIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
//        getApplicationContext().sendBroadcast(delIntent);
//    }

}