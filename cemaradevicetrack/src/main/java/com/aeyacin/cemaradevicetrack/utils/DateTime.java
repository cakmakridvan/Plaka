package com.aeyacin.cemaradevicetrack.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTime {

    private static Date currentTime;
    private static SimpleDateFormat sdf;
    private static String currentDateandTime;

    public static String getDate(){

        currentTime = Calendar.getInstance().getTime();
        sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
        currentDateandTime = sdf.format(currentTime);

        return currentDateandTime;
    }

}
