package com.coderming.mystockhawk;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.coderming.mystockhawk.data.QuoteColumns;
import com.coderming.mystockhawk.data.QuoteProvider;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class TicketActivityFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = TicketActivityFragment.class.getSimpleName();

    private static final int DETAIL_LOADER_ID = 2;
    public static final String SYMBOL_ARG = "SYMBOL_ARG";
    static final String sVolumeFormatter = "Volumn %s";
    static final String sDayRangeFormatter = "Day Range %.2f - %.2f";

    private static final String[] DetailProjection = new String[] {
            QuoteColumns.SYMBOL, QuoteColumns.VOLUME, QuoteColumns.BIDPRICE, QuoteColumns.ISUP,
            QuoteColumns.DAYSLOW, QuoteColumns.DAYSHIGH};

    @BindView(R.id.detail_symbol) TextView mSymbol;
    @BindView(R.id.detail_volume) TextView mVolume;
    @BindView(R.id.detail_price)  TextView mPrice;
    @BindView(R.id.detail_day_range)  TextView mDayRange;
    @BindView(R.id.stock_chart) ImageView mTicketGraph;
    @BindView(R.id.retrieve_value) EditText mEditText;
    Spinner mSpinner;

    private static final String sUrlFormatter =
            "http://chart.finance.yahoo.com/z?s=%s&t=%d%s&q=l&l=on&z=s&p=m50,m200";
    private static char[] sUnitOption = {'d', 'm', 'y'};
    private String[] UnitOptions = {"Day", "Month", "Year"};

    public TicketActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_ticket, container, false);

        Bundle args = getArguments();
        assert (args != null);
        final String ticket = args.getString(SYMBOL_ARG);

        ButterKnife.bind(this, rootView);
        mSpinner = (Spinner) rootView.findViewById(R.id.retrieve_unit);
        ArrayAdapter<String> adapter = new ArrayAdapter (getContext(), R.layout.spinner_item, UnitOptions);
        mSpinner.setAdapter(adapter);

        rootView.findViewById(R.id.retrieve_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ticket != null)
                    retrieveGraphPicasso(ticket, getValue(), getUnit());
                else {
                    Log.v(LOG_TAG, " no symbol has passed in");
                    Glide.with(getContext()).load(R.drawable.not_available).asBitmap().into(mTicketGraph);
                }
            }
        });
        if (ticket != null) {
            getLoaderManager().initLoader(DETAIL_LOADER_ID, args, this);
            retrieveGraphPicasso(ticket, getValue(), getUnit());
        }
        return rootView;
    }

    private int getValue() {
        int val = 7;
        try {
            val = Integer.parseInt(mEditText.getText().toString());
        } catch (Exception ex) {
            Log.i(LOG_TAG, "ignore the exception: " + ex.getMessage(), ex);
        }
        if (val < 1) {
            val = 7;
        }
        return val;
    }

    private char getUnit() {
        int pos = mSpinner.getSelectedItemPosition();
        return sUnitOption[pos];
    }
    public void updateData(String ticket) {
        Bundle args = new Bundle();
        args.putString(SYMBOL_ARG, ticket);
        getLoaderManager().restartLoader(DETAIL_LOADER_ID, args, this);
        retrieveGraphPicasso(ticket, getValue(), getUnit());
    }
    private void retrieveGraphPicasso(String ticket, int value, char unit) {
        String urlStr = String.format(sUrlFormatter, ticket, value, unit);
        try {
            Glide.with(getContext()).load(urlStr)
                    .asBitmap().error(R.drawable.not_available).into(mTicketGraph);
//        Picasso.with(getContext()).load(urlStr)
//                .placeholder(R.drawable.placehold)
//                .error(R.drawable.not_available)
//                .into(mTicketGraph);
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error retrieving symbol graphic" + urlStr, ex);
        }
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if ((DETAIL_LOADER_ID == id) && (args != null)) {
            String symbol = args.getString(SYMBOL_ARG);
            Uri uri = QuoteProvider.Quotes.withSymbol(symbol);
            Log.v(LOG_TAG, String.format("+++ onCreateLoader, id=%d, uri=%s", id, uri.toString()));
            return new CursorLoader(getContext(), uri, DetailProjection,
                    QuoteColumns.SYMBOL + "=? AND "+ QuoteColumns.ISCURRENT + " = ?",
                    new String[]{symbol, "1"}, null);
        } else {
            return null;
        }
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if ( (DETAIL_LOADER_ID == loader.getId()) && data.moveToFirst()) {
            int colNum = 0;
            String str = data.getString(colNum++);          // symbol
            mSymbol.setText(str);
            mSymbol.setContentDescription(str);
            str = Integer.toString( data.getInt(colNum++));   // volume
            mVolume.setText(String.format(sVolumeFormatter, str));
            mVolume.setContentDescription(str);
            str = data.getString(colNum++);                // price
            boolean isUp = (data.getInt(colNum++) == 1);
            Utils.setPriceText(getContext(), str, isUp, mPrice);
            float low = data.getFloat(colNum++);            // low
            float high = data.getFloat(colNum++);          // high
            mDayRange.setText(String.format(sDayRangeFormatter, low, high));
        }
    }
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}