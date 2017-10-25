package com.albaitdevs.puremadrid.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Delga on 24/09/2015.
 */

public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    public interface OnDateTimeSetListener{
        void onDateTimeSet(Calendar timeSet);
    }
    private static int minute = -1;
    private static int hour = -1;
    private static int day = -1;
    private static int month = -1;
    private static int year = -1;
    private static boolean dateSet = false;

    private OnDateTimeSetListener listener;

    public void setListener(OnDateTimeSetListener listener){
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        if (!dateSet) {
            final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        }

        // Create a new instance of DatePickerDialog and return it
        final DatePickerDialog picker = new DatePickerDialog(getActivity(), this, year, month, day);

        // Max and Min Date
        picker.getDatePicker().setMaxDate(Calendar.getInstance(TimeZone.getTimeZone("CET")).getTimeInMillis());
        picker.getDatePicker().setMinDate(getMinDate());

        picker.setCanceledOnTouchOutside(true);
        picker.setCancelable(true);

        picker.setButton(DialogInterface.BUTTON_NEGATIVE, "Hoy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CET"));
                picker.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                onDateSet(null, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            }
        });

        return picker;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {

        dateSet = true;
        this.day = day;
        this.month = month;
        this.year = year;

        //Prepare event for difference between days
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendar.set(Calendar.YEAR,year);
        calendar.set(Calendar.MONTH,month);
        calendar.set(Calendar.DAY_OF_MONTH,day);

        onDateSet();
    }

    /**
     * Count days
     * @param d1
     * @param d2
     * @return
     */
    public int daysBetween(Date d1, Date d2){
        return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
    }

    public long getMinDate() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendar.set(Calendar.YEAR,2017);
        calendar.set(Calendar.MONTH,Calendar.OCTOBER);
        calendar.set(Calendar.DATE,26);
        return calendar.getTimeInMillis();
    }

    public void onDateSet() {
        final TimePickerDialog timePicker = new TimePickerDialog(getActivity(), this, hour, minute, true);

        timePicker.setCanceledOnTouchOutside(true);
        timePicker.setCancelable(true);

        timePicker.setButton(DialogInterface.BUTTON_NEGATIVE, "Ahora", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CET"));
                timePicker.updateTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
                onTimeSet(null, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
            }
        });
        timePicker.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        this.hour = hourOfDay;
        this.minute = minute;

        // Create calendar
        Calendar selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        selectedCalendar.set(Calendar.YEAR,year);
        selectedCalendar.set(Calendar.MONTH,month);
        selectedCalendar.set(Calendar.DAY_OF_MONTH,day);
        selectedCalendar.set(Calendar.HOUR_OF_DAY,hour);
        selectedCalendar.set(Calendar.MINUTE,1);
        selectedCalendar.set(Calendar.SECOND,0);
        selectedCalendar.set(Calendar.MILLISECOND,0);

        //
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        if (selectedCalendar.getTimeInMillis() > now.getTimeInMillis()){
            selectedCalendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        }
        if (listener != null) {
            listener.onDateTimeSet(selectedCalendar);
        } else {
            Log.w("DatePickerFragment","There is no listener");
        }
    }
}