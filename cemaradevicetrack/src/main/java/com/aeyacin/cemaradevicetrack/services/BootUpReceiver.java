package com.aeyacin.cemaradevicetrack.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.aeyacin.cemaradevicetrack.MainActivity;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        context.startActivity(new Intent(context,MainActivity.class));
    }
}
