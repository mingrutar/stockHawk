package com.coderming.mystockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.coderming.mystockhawk.MainActivity;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by linna on 7/1/2016.
 */
public class StockIntentService extends IntentService {

    public StockIntentService(){
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override protected void onHandleIntent(Intent intent) {
        Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();
        if (intent.getStringExtra("tag").equals(MainActivity.TAG_ADD)){
            args.putString("symbol", intent.getStringExtra("symbol"));
        }
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        int ret = stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
    }
}
