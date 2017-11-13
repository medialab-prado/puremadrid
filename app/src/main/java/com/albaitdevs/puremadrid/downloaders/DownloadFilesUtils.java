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

package com.albaitdevs.puremadrid.downloaders;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.albaitdevs.puremadrid.R;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * Created by Delga_ on 09/09/2017.
 */

public class DownloadFilesUtils {

    public static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_REGLAMENTO = 124;
    public static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_BOLETIN_DIARIO = 123;
    public static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_MORE_INFORMATION = 125;

    public static void downloadPdf(Activity activity, Context context, int documento_permission) {

        String uriString;
        String fileName;
        switch (documento_permission){
            case EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_BOLETIN_DIARIO:
                uriString = activity.getString(R.string.url_boletin_diario);
                fileName = "Boletin diario";
                break;
            case EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_REGLAMENTO:
                uriString = activity.getString(R.string.url_official_docs);
                fileName = "Protocolo de contaminacion";
                break;
            case EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_MORE_INFORMATION:
                uriString = activity.getString(R.string.url_more_information);
                fileName = "Más información";
                break;
            default:
                showNotDownloadedToast(activity, "NO DOCUMENT");
                return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(uriString);
        intent.setDataAndType(uri, "application/pdf");
        if (intent.resolveActivity(context.getPackageManager()) != null){

            context.startActivity(intent);
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED){

                Toast.makeText(activity,R.string.downloading,Toast.LENGTH_LONG).show();

                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // to notify when download is complete
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                request.allowScanningByMediaScanner();// if you want to be available from media players
                DownloadManager manager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                manager.enqueue(request);

            } else {

                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    //Explain the permission
                    showAlertDialog(activity,documento_permission,fileName);

                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE },
                            documento_permission);
                }
            }
        }
    }

    private static void showAlertDialog(final Activity activity, final int documento_permission, final String fileName) {
        //Create alert
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.need_permission_storage_message)
                .setTitle(R.string.need_permission_title);
        builder.setPositiveButton(R.string.give_permission, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        documento_permission);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                DownloadFilesUtils.showNotDownloadedToast(activity, fileName);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    public static void showNotDownloadedToast(Activity activity, String fileName) {

        Toast.makeText(activity,R.string.pdf_not_downloaded,Toast.LENGTH_LONG).show();
    }
}
