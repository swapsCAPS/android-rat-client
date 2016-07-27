package com.safe.myapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SafeReceiver extends BroadcastReceiver {
    public SafeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, SafeService.class);
        context.startService(i);
    }
}
