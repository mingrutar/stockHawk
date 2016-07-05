package com.coderming.mystockhawk.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.coderming.mystockhawk.MainActivity;
import com.coderming.mystockhawk.R;
import com.coderming.mystockhawk.data.QuoteColumns;
import com.coderming.mystockhawk.data.QuoteProvider;

/**
 * Created by linna on 7/3/2016.
 */
public class QuoteWidgetIntentService extends IntentService  {
    private static final String LOG_TAG = QuoteWidgetIntentService.class.getSimpleName();
    public QuoteWidgetIntentService() {
        super(LOG_TAG);
    }

    private static int[][] widget_ids = new int[][] {
            {R.id.widget_stock_symbol1, R.id.widget_bid_price1, R.id.widget_change1},
            {R.id.widget_stock_symbol2, R.id.widget_bid_price2, R.id.widget_change2},
            {R.id.widget_stock_symbol3, R.id.widget_bid_price3, R.id.widget_change3},
            {R.id.widget_stock_symbol4, R.id.widget_bid_price4, R.id.widget_change4} };
    private static final int MAX_LINE = widget_ids.length;

    public static final String[] sQuoteProjection =new String[]{
            QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE, QuoteColumns.ISUP, QuoteColumns.PERCENT_CHANGE};

    @Override
    protected void onHandleIntent(Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                QuoteWidgetProvider2.class));

        Cursor data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                sQuoteProjection,
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
        if (data == null) {
            Log.v(LOG_TAG, String.format("+-+- #appWidgetId=%d, #date=null", appWidgetIds.length));
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            Log.v(LOG_TAG, String.format("+-+- #appWidgetId=%d, #date=0", appWidgetIds.length));
            return;
        }
        Log.v(LOG_TAG, String.format("+-+- #appWidgetId=%d, #date=%d", appWidgetIds.length, data.getCount()));
        for (int appWidgetId : appWidgetIds) {
            int layoutId;
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_quotes2);

            int limit = Math.min(data.getCount(), MAX_LINE);
            for (int i = 0; i < limit; i++) {
                Log.v(LOG_TAG, String.format("+-+- appWidgetId=%d, i=%d", appWidgetId, i));
                if (i > 0)
                    data.moveToPosition(i);
                else
                    data.moveToFirst();
                views.setTextViewText(widget_ids[i][0], data.getString(0));     //symbol
                views.setTextViewText(widget_ids[i][1], data.getString(1));     //bid
                int colorId = (data.getInt(2) == 1) ? R.color.up_text : R.color.down_text;
                @ColorInt int color = (Build.VERSION.SDK_INT < 23) ? getResources().getColor(colorId)
                        : ContextCompat.getColor(getBaseContext(), colorId);
                views.setTextColor(widget_ids[i][2], color);
                views.setTextViewText(widget_ids[i][2], data.getString(3));     // chg%
            }

            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}

