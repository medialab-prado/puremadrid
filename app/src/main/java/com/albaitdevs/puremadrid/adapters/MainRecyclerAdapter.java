package com.albaitdevs.puremadrid.adapters;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.albaitdevs.puremadrid.R;
import com.albaitdevs.puremadrid.downloaders.DownloadFilesUtils;
import com.albaitdevs.puremadrid.downloaders.GetLastStatusAsync;
import com.albaitdevs.puremadrid.fragments.MyMapFragment;
import com.puremadrid.api.pureMadridApi.model.ApiMedicion;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainRecyclerAdapter extends RecyclerView.Adapter<MainRecyclerAdapter.ViewHolder> implements GetLastStatusAsync.ApiListener {

    private final Activity mActivity;
    ApiMedicion currentMeasure;

    public MainRecyclerAdapter(Activity activity, ApiMedicion measure) {
        mActivity = activity;
        currentMeasure = measure;
    }

    public void setData(ApiMedicion measure){
        currentMeasure = measure;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.image_card) ImageView imageView;
        @BindView(R.id.info_text) TextView textView;
        @BindView(R.id.item_progressbar) ProgressBar progressBar;

        public ViewHolder(RelativeLayout v) {
            super(v);
            ButterKnife.bind(this,itemView);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int scenario = -1;
            switch (getAdapterPosition()) {
                case 0:
                    if (currentMeasure != null) {
                        switch (currentMeasure.getEscenarioStateToday()) {
                            case "NONE":
                                scenario = 0;
                                break;
                            case "ESCENARIO1":
                                scenario = 1;
                                break;
                            case "ESCENARIO2":
                                scenario = 2;
                                break;
                            case "ESCENARIO3":
                                scenario = 3;
                                break;
                            case "ESCENARIO4":
                                scenario = 4;
                                break;
                        }
                    }
                    showDialogScenario(scenario,true);
                    break;
                case 1:
                    if (currentMeasure != null) {
                        switch (currentMeasure.getEscenarioStateTomorrow()) {
                            case "NONE":
                                scenario = 0;
                                break;
                            case "ESCENARIO1":
                                scenario = 1;
                                break;
                            case "ESCENARIO2":
                                scenario = 2;
                                break;
                            case "ESCENARIO3":
                                scenario = 3;
                                break;
                            case "ESCENARIO4":
                                scenario = 4;
                                break;
                        }
                    }
                    showDialogScenario(scenario,false);
                    break;
                case 2:
                    FragmentManager fragmentManager = mActivity.getFragmentManager();
                    String fragment_tag = MyMapFragment.class.getName();
                    Fragment fragment = fragmentManager.findFragmentByTag(fragment_tag);
                    if (fragment == null) {
                        fragment = new MyMapFragment();
                    }
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.content_frame, fragment, fragment_tag);
                    transaction.addToBackStack(null);
                    transaction.commit();
                    break;
                case 3:
                    DownloadFilesUtils.downloadPdf(mActivity, mActivity, DownloadFilesUtils.EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE_BOLETIN_DIARIO);
            }
        }

        public void bind(int position) {
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            String text = "";
            int color = 0;

            switch (position) {
                case 0:
                    progressBar.setVisibility(View.VISIBLE);
                    // View setup
                    layoutParams.height = (int) itemView.getContext().getResources().getDimension(R.dimen.escenarios_height);
                    imageView.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.skyline_item));
                    // Set text and background color
                    color = mActivity.getResources().getColor(R.color.blue);
                    text = mActivity.getString(R.string.loading_data);
                    if (currentMeasure != null) {
                        progressBar.setVisibility(View.GONE);
                        text = mActivity.getString(R.string.scenario_activaded_today_is);
                        switch (currentMeasure.getEscenarioStateToday()) {
                            case "ESCENARIO1":
                                text += " 1";
                                color = mActivity.getResources().getColor(R.color.yellow);
                                break;
                            case "ESCENARIO2":
                                text += " 2";
                                color = mActivity.getResources().getColor(R.color.orange);
                                break;
                            case "ESCENARIO3":
                                text += " 3";
                                color = mActivity.getResources().getColor(R.color.red);
                                break;
                            case "ESCENARIO4":
                                text += " 4";
                                color = mActivity.getResources().getColor(R.color.red);
                                break;
                            case "NONE":
                                text = mActivity.getString(R.string.scenario0_today_title);
                        }
                    }
                    imageView.setBackgroundColor(color);
                    textView.setText(text);
                    break;
                case 1:
                    progressBar.setVisibility(View.VISIBLE);
                    layoutParams.height = (int) itemView.getContext().getResources().getDimension(R.dimen.escenarios_height);
                    imageView.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.skyline_item));

                    // Text
                    color = mActivity.getResources().getColor(R.color.blue);
                    text = mActivity.getResources().getString(R.string.loading_data);
                    if (currentMeasure != null) {
                        progressBar.setVisibility(View.GONE);
                        text = mActivity.getString(R.string.scenario_activaded_tomorrow_is);
                        switch (currentMeasure.getEscenarioStateTomorrow()) {
                            case "ESCENARIO1":
                                text += " 1";
                                color = mActivity.getResources().getColor(R.color.yellow);
                                break;
                            case "ESCENARIO2":
                                text += " 2";
                                color = mActivity.getResources().getColor(R.color.orange);
                                break;
                            case "ESCENARIO3":
                                text += " 3";
                                color = mActivity.getResources().getColor(R.color.red);
                                break;
                            case "ESCENARIO4":
                                text += " 4";
                                color = mActivity.getResources().getColor(R.color.red);
                                break;
                            case "NONE":
                                text = mActivity.getString(R.string.scenario0_tomorrow_title);
                        }
                    }
                    imageView.setBackgroundColor(color);
                    textView.setText(text);
                    break;
                case 2:
                    layoutParams.height = (int) itemView.getContext().getResources().getDimension(R.dimen.main_last_items_height);
                    text = mActivity.getString(R.string.goto_map);
                    textView.setText(text);
                    Picasso.with(mActivity).load(R.drawable.main_map_image).into(imageView);
                    progressBar.setVisibility(View.GONE);
                    break;
                case 3:
                    layoutParams.height = (int) itemView.getContext().getResources().getDimension(R.dimen.main_last_items_height);
                    text = mActivity.getString(R.string.menuitem_last_announcement);
                    textView.setText(text);
                    Picasso.with(mActivity).load(R.drawable.main_moreinfo_image).into(imageView);
                    progressBar.setVisibility(View.GONE);
                    break;
            }
        }
    }

    private void showDialogScenario(final int scenario, final boolean today) {

        String title = mActivity.getString(R.string.no_data_title);
        String message = mActivity.getString(R.string.no_data_content);

        switch (scenario){
            case 0:
                title = mActivity.getString(R.string.scenario_none_title);
                message = mActivity.getString(R.string.scenario_none_content);
                break;
            case 1:
                title = mActivity.getString(R.string.scenario1_title);
                message = mActivity.getString(R.string.scenario1_content);
                break;
            case 2:
                title = mActivity.getString(R.string.scenario2_title);
                message = mActivity.getString(R.string.scenario2_content);
                break;
            case 3:
                title = mActivity.getString(R.string.scenario3_title);
                message = mActivity.getString(R.string.scenario3_content);
                break;
            case 4:
                title = mActivity.getString(R.string.scenario4_title);
                message = mActivity.getString(R.string.scenario4_content);
                break;
        }

        //Create alert
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage(message)
                .setTitle(title);
        builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        if (scenario >= 0) {
            builder.setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    String text;
                    if (today) {
                        text = mActivity.getString(R.string.scenario0_today_title);
                        if (scenario > 0) {
                            text = mActivity.getString(R.string.scenario_activaded_today_is)
                                    + " " + scenario;
                        }
                    } else {
                        text = mActivity.getString(R.string.scenario0_tomorrow_title);
                        if (scenario > 0) {
                            text = mActivity.getString(R.string.scenario_activaded_tomorrow_is)
                                    + " " + scenario;
                        }
                    }
                    text += "\n" + mActivity.getString(R.string.puremadrid_play_url);

                    // Intent
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, text);
                    mActivity.startActivity(Intent.createChooser(intent, mActivity.getString(R.string.share)));
                }
            });
        } else {
            builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    // Intent
                    new GetLastStatusAsync(mActivity,MainRecyclerAdapter.this,null).execute();
                }
            });
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }


    @Override
    public MainRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_status, parent, false);

        // set the view's size, margins, paddings and layout parameters
        ViewHolder viewHolder = new ViewHolder((RelativeLayout) v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MainRecyclerAdapter.ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public void onApiFinished(ApiMedicion result) {
        if (result != null) {
            notifyItemChanged(0);
            notifyItemChanged(1);
        } else {

        }
    }

    /**
     * The item count relies only in database
     * @return
     */
    @Override
    public int getItemCount() {
        return 4;
    }

}