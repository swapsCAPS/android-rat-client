package com.safe.myapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class SafeLauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // start our service
        Intent i = new Intent(this, SafeService.class);
        startService(i);

        // start their MainActivity
        i = new Intent(this, ChangeMeToMainActivity.class);
        startActivity(i);

        // close this activity
        finish();
    }
}
