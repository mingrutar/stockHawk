package com.coderming.mystockhawk;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import com.coderming.mystockhawk.data.QuoteColumns;
import com.coderming.mystockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Created by linna on 7/1/2016.
 */
public class Utils {
    private static String LOG_TAG = Utils.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({QUOTE_STATUS_NONE, QUOTE_STATUS_OK, QUOTE_STATUS_SERVER_DOWN, QUOTE_STATUS_NETWORK_DOWN,QUOTE_STATUS_SERVER_INVALID
            ,QUOTE_STATUS_SERVER_UNKNOWN, QUOTE_STATUS_ENCODING_ERROR, QUOTE_STATUS_BAD_SYMBOL } )
    public @interface QuoteServerStatus {}
    public static final int QUOTE_STATUS_NONE = 1;
    public static final int QUOTE_STATUS_OK = 0;
    public static final int QUOTE_STATUS_SERVER_DOWN = -1;       //test: http://google.com/ping? (empty .length==0)
    public static final int QUOTE_STATUS_NETWORK_DOWN = -2;
    public static final int QUOTE_STATUS_SERVER_INVALID = -3;    //test: http://google.com/?
    public static final int QUOTE_STATUS_SERVER_UNKNOWN = -4;
    public static final int QUOTE_STATUS_ENCODING_ERROR = -10;
    public static final int QUOTE_STATUS_BAD_SYMBOL = -11;

    public static boolean showPercent = true;

    public static final String TAG_CHANGE = "Change";
    public static final String TAG_SYMBOL = "symbol";
    public static final String TAG_BID = "Bid";
    public static final String TAG_CHANGEINPERCENT = "ChangeinPercent";
    public static final String TAG_DAYSLOW = "DaysLow";
    public static final String TAG_DAYSHIGH = "DaysHigh";
    public static final String TAG_YEARLOW = "YearLow";
    public static final String TAG_YEARHIGH = "YearHigh";
    public static final String TAG_FIFTYDAYMOVINGAVERAGE = "FiftydayMovingAverage";
    public static final String TAG_TWOHUNDREDDAYMOVINGAVERAGE = "TwoHundreddayMovingAverage";
    public static final String TAG_VOLUME = "Volume";
    public static final String TAG_NAME = "Name";

    public static final String FORMATTER_PRICE = "%.2f";
    public static final String FORMATTER_POS_CHANGE = "+%.2f%% (%.2f)";
    public static final String FORMATTER_NEG_CHANGE = "-%.2f%% (%.2f)";

    public static ArrayList quoteJsonToContentVals(String JSON)
                    throws JSONException {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        jsonObject = new JSONObject(JSON);
        if (jsonObject != null && jsonObject.length() != 0){
            jsonObject = jsonObject.getJSONObject("query");
            int count = Integer.parseInt(jsonObject.getString("count"));
            if (count == 1){
                jsonObject = jsonObject.getJSONObject("results")
                        .getJSONObject("quote");
                ContentProviderOperation cpo = buildBatchOperation(jsonObject);
                if (cpo != null)
                    batchOperations.add(cpo);
            } else{
                resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                if (resultsArray != null && resultsArray.length() != 0){
                    for (int i = 0; i < resultsArray.length(); i++){
                        jsonObject = resultsArray.getJSONObject(i);
                        ContentProviderOperation cpo = buildBatchOperation(jsonObject);
                        if (cpo != null)
                            batchOperations.add(cpo);
                    }
                }
            }
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice){
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange){
        String weight = change.substring(0,1);
        String ampersand = "";
        if (isPercentChange){
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    private static boolean isNull(String str) {
        return ("null" == str) || (null == str);
    }
    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString(TAG_CHANGE);
            String strSym = jsonObject.getString(TAG_SYMBOL);
            String strBid = jsonObject.getString(TAG_BID);
            String strChange = jsonObject.getString(TAG_CHANGEINPERCENT);
            if (isNull(change) ||isNull(strSym) || isNull(strBid) || isNull(strChange)) {
                return null;
            } else {
                builder.withValue(QuoteColumns.SYMBOL, strSym).
                        withValue(QuoteColumns.BIDPRICE, truncateBidPrice(strBid))
                        .withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(strChange, true))
                        .withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            }
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-'){
                builder.withValue(QuoteColumns.ISUP, 0);
            }else{
                builder.withValue(QuoteColumns.ISUP, 1);
            }
            String strDayL = jsonObject.getString(TAG_DAYSLOW);
            String strDayH = jsonObject.getString(TAG_DAYSHIGH);
            String strYearL = jsonObject.getString(TAG_YEARLOW);
            String strYearH = jsonObject.getString(TAG_YEARHIGH);
            String strVol = jsonObject.getString(TAG_VOLUME);
            String strName = jsonObject.getString(TAG_NAME);
            if (isNull(strDayL) || isNull(strDayH) || isNull(strYearL) ||
                    isNull(strYearH) || isNull(strVol) || isNull(strName) ) {
                return null;
            } else {
                builder.withValue(QuoteColumns.DAYSLOW, Float.parseFloat(strDayL))
                    .withValue(QuoteColumns.DAYSHIGH, Float.parseFloat(strDayH))
                    .withValue(QuoteColumns.YEARLOW, Float.parseFloat(strYearL))
                    .withValue(QuoteColumns.YEARHIGH, Float.parseFloat(strYearH));
           }
            String str =  jsonObject.getString(TAG_FIFTYDAYMOVINGAVERAGE);
            if (isNull(str))
                builder.withValue(QuoteColumns.FIFTYDAYMOVINGAVERAGE, Float.parseFloat(str));
            str = jsonObject.getString(TAG_TWOHUNDREDDAYMOVINGAVERAGE);
            if (isNull(str))
                builder.withValue(QuoteColumns.TWOHUNDREDDAYMOVINGAVERAGE, Float.parseFloat(str));
            builder.withValue(QuoteColumns.VOLUME, Integer.parseInt(strVol)).withValue(QuoteColumns.NAME, strName);
        } catch (JSONException e){
            e.printStackTrace();
            Log.i(LOG_TAG, "ContentProviderOperation: "+e.getMessage(), e);
        } catch (Exception ex) {
            Log.i(LOG_TAG, "ContentProviderOperation: "+ex.getMessage(), ex);
        }
        return builder.build();
    }
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return ((activeNetwork!=null) && activeNetwork.isConnectedOrConnecting());
    }

    @SuppressWarnings("ResourceType")
    public static @QuoteServerStatus
    int getQuoteServerStatus(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int ret = prefs.getInt(context.getString(R.string.quote_server_status_key),
                QUOTE_STATUS_SERVER_UNKNOWN);
        return ret;
    }
    public static void setQuoteServerStatus(Context context, @QuoteServerStatus int status ) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(context.getString(R.string.quote_server_status_key), status);
        editor.apply();
    }
    public static boolean isFatalStatus(Context context) {
        @Utils.QuoteServerStatus int status = getQuoteServerStatus(context);
        return (status < 0 ) && (status > 10) ;
    }
    public static int getErrorStatusMessageId( Context context) {
        int message = -1;
        @Utils.QuoteServerStatus int status = getQuoteServerStatus(context);
        if (status != QUOTE_STATUS_NONE) {
            switch (status) {
                case QUOTE_STATUS_OK:         //test: http://google.com/ping? (empty .length==0)
                    message = R.string.empty_quote_list;
                    break;
                case QUOTE_STATUS_SERVER_DOWN:         //test: http://google.com/ping? (empty .length==0)
                    message = R.string.empty_quote_list_server_down;
                    break;
                case QUOTE_STATUS_NETWORK_DOWN:
                    message = R.string.empty_quote_list_no_network;
                    break;
                case QUOTE_STATUS_SERVER_UNKNOWN:
                    message = R.string.empty_quote_list_server_error;
                    break;
                case QUOTE_STATUS_SERVER_INVALID:                //test: http://google.com/?  bad symbol
                    message = R.string.empty_quote_list_server_invalid;
                    break;
                case QUOTE_STATUS_ENCODING_ERROR:
                    message = R.string.empty_encoding_failure;
                    break;
                case QUOTE_STATUS_BAD_SYMBOL:
                    message = R.string.empty_invalid_symbol;
                    break;
                default:
                    if (!isNetworkAvailable(context)) {
                        message = R.string.empty_quote_list_no_network;
                    } else {
                        message = R.string.empty_quote_list;
                    }
            }
            setQuoteServerStatus(context, QUOTE_STATUS_NONE);
        }
        return message;
    }
    public static String getPriceStr(Context context, String priceStr) {
        float priceF = Float.parseFloat(priceStr);

        return String.format(FORMATTER_PRICE, priceF);
    }
    private static float getFloat(String str) {
        int iS = 0, iE = str.length()-1;
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {
                iS = i;
                if (!Character.isDigit(str.charAt(iE))) {
                    str = str.substring(iS, iE);
                } else
                    str = str.substring(iS);
                break;
            }
        }
        return (str .length() > 0) ? Float.parseFloat(str) : 0f;
    }
    public static String getChangeStr(Context context, boolean isUp, String percent, String change) {
        float percentF = getFloat(percent.trim());
        float changeF = getFloat(change);
        String fmt = isUp ? FORMATTER_POS_CHANGE : FORMATTER_NEG_CHANGE;
//        String fmt = isUp ? context.getString(R.string.format_change) : context.getString(R.string.neg_format_change);
        return String.format(fmt, percentF, changeF);
    }
    public static final void setPriceText(Context context, String changeStr, boolean isUp, TextView mTextView) {
        int colorId = isUp ? R.color.up_text : R.color.down_text;
        @ColorInt
        int color = (Build.VERSION.SDK_INT < 23) ? context.getResources().getColor(colorId)
                : ContextCompat.getColor(context, colorId);
        mTextView.setTextColor(color);
        mTextView.setText(changeStr);
        mTextView.setContentDescription(changeStr);
    }
}
