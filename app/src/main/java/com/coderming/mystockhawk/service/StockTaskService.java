package com.coderming.mystockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.coderming.mystockhawk.MainActivity;
import com.coderming.mystockhawk.Utils;
import com.coderming.mystockhawk.data.QuoteColumns;
import com.coderming.mystockhawk.data.QuoteProvider;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by linna on 7/1/2016.
 */
public class StockTaskService extends GcmTaskService {
    private String LOG_TAG = StockTaskService.class.getSimpleName();

    public static final String ACTION_DATA_UPDATED = "com.coderming.mystockhawk.service.ACTION_DATA_UPDATED";
    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService(){}

    public StockTaskService(Context context){
        mContext = context;
    }
    String fetchData(String url) throws IOException{
        Log.v(LOG_TAG, "fetchData: uri="+url);
        Request request = new Request.Builder()
                .url(url)
                .build();
        if (Utils.isNetworkAvailable(mContext)) {
            Response response = client.newCall(request).execute();
            String respStr = response.body().string();
            Log.v(LOG_TAG, "fetchData: resp="+respStr);
            return respStr;
        } else {
            Utils.setQuoteServerStatus(mContext, Utils.QUOTE_STATUS_NETWORK_DOWN);
            Log.v(LOG_TAG, "network is unavailable");
            return null;
        }
    }

    @Override
    public int onRunTask(TaskParams params){
        Cursor initQueryCursor;
        if (mContext == null){
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try{
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Utils.setQuoteServerStatus(mContext, Utils.QUOTE_STATUS_ENCODING_ERROR);
            Log.e(LOG_TAG, "onRunTask caught exception; "+e.getMessage(), e);
        }
        if (params.getTag().equals(MainActivity.TAG_INIT) || params.getTag().equals(MainActivity.TAG_PERIODIC)){
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
                    null, null);
//            if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
//                // Init task. Populates DB with quotes for the symbols seen below
//                try {
//                    urlStringBuilder.append(URLEncoder.encode("\"AAPL\",\"GOOG\")", "UTF-8"));
////                  URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
//                } catch (UnsupportedEncodingException e) {
//                    Utils.setQuoteServerStatus(mContext, Utils.QUOTE_STATUS_ENCODING_ERROR);
//                    Log.e(LOG_TAG, "onRunTask caught exception; "+e.getMessage(), e);
//                }
//            } else if (initQueryCursor != null){
            if ((initQueryCursor.getCount() > 0) && (initQueryCursor != null)){
                    DatabaseUtils.dumpCursor(initQueryCursor);
                    initQueryCursor.moveToFirst();
                    for (int i = 0; i < initQueryCursor.getCount(); i++){
                        if (i>0) {
                            mStoredSymbols.append(",");
                        }
                        mStoredSymbols.append("\""+
                                initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\"");
                        initQueryCursor.moveToNext();
                    }
                    mStoredSymbols.append( ")");
                    try {
                        urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        Utils.setQuoteServerStatus(mContext, Utils.QUOTE_STATUS_ENCODING_ERROR);
                        Log.e(LOG_TAG, "onRunTask caught exception; "+e.getMessage(), e);
                    }
            } else {
                return GcmNetworkManager.RESULT_SUCCESS;
            }
        } else if (params.getTag().equals("add")){
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
            } catch (UnsupportedEncodingException e){
                Utils.setQuoteServerStatus(mContext, Utils.QUOTE_STATUS_ENCODING_ERROR);
                Log.e(LOG_TAG, "onRunTask caught exception; "+e.getMessage(), e);
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null){
            urlString = urlStringBuilder.toString();
            try{
                getResponse = fetchData(urlString);
                if (getResponse != null) {
                    result = GcmNetworkManager.RESULT_SUCCESS;
                    try {
                        ContentValues contentValues = new ContentValues();
                        // update ISCURRENT to 0 (false) so new data is current
                        if (isUpdate) {
                            contentValues.put(QuoteColumns.ISCURRENT, 0);
                            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                    null, null);
                        }
                        mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                                Utils.quoteJsonToContentVals(getResponse));
                        updateWidgets();
                    } catch  (JSONException je) {
// seems it is normal               Utils.setQuoteServerStatus(mContext, Utils.QUOTE_STATUS_SERVER_INVALID);
                        Log.e(LOG_TAG, "quoteJsonToContentVals caught Exception: " + je);
                    } catch (RemoteException | OperationApplicationException e) {
                        Utils.setQuoteServerStatus(mContext, Utils.QUOTE_STATUS_SERVER_INVALID);
                        Log.e(LOG_TAG, "Error applying batch insert", e);
                    }
                } else {
                    result = GcmNetworkManager.RESULT_FAILURE;
                }
            } catch (IOException e){
                Utils.setQuoteServerStatus(mContext, Utils.QUOTE_STATUS_SERVER_DOWN);
                Log.e(LOG_TAG, "onRunTask failed: "+e.getMessage(), e);
            } catch (Exception ex) {
                Utils.setQuoteServerStatus(mContext, Utils.QUOTE_STATUS_BAD_SYMBOL);
                Log.i(LOG_TAG, "onRunTask failed "+ex.getMessage(), ex);
            }
        }

        return result;
    }
    private void updateWidgets() {
        // Setting the package ensures that only components in our app will receive the broadcast
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                .setPackage(mContext.getPackageName());
        mContext.sendBroadcast(dataUpdatedIntent);
    }}

