package com.coderming.mystockhawk;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coderming.mystockhawk.data.QuoteColumns;
import com.coderming.mystockhawk.data.QuoteProvider;
import com.coderming.mystockhawk.service.StockIntentService;
import com.coderming.mystockhawk.service.StockTaskService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>  {
    private String LOG_TAG = MainActivity.class.getSimpleName();

    static final int CURSOR_LOADER_ID = 0;
    static final int DETAIL_LOADER_ID = 2;
    static final String SELECTED_SYMBOL = "SelectedSymbol";

    public static final String TAG_PERIODIC = "periodic";
    public static final String TAG_INIT = "init";
    public static final String TAG_ADD = "add";

    private static final long SYNC_INTERVAL = 3600L;            // in msec?
    private static final long SYNC_FLEXTIME = 10L;             // in msec?

    long flex = 10L;

    String mSelectedSymbol;
    PeriodicTask mPeriodicTask;

    private RecyclerView mRecyclerView;
    private CursorRecyclerViewAdapter mAdapter;

    private Intent mServiceIntent;
    private FloatingActionButton mFab;
    private BottomSheetBehavior mBottomSheetBehavior;
    private Loader<Cursor> mDetailLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Utils.setQuoteServerStatus(this, Utils.QUOTE_STATUS_NONE);

        mServiceIntent = new Intent(this, StockIntentService.class);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        loadBackdrop();

        if ((savedInstanceState != null) && savedInstanceState.containsKey(SELECTED_SYMBOL)) {
            mSelectedSymbol = savedInstanceState.getString(SELECTED_SYMBOL);
        } else {
            mSelectedSymbol = null;
        }
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_quote);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new CursorRecyclerViewAdapter(this, mSymbolSelectionListener);
        mRecyclerView.setAdapter(mAdapter);

        View bottomSheet = findViewById( R.id.detail_sheet );
        assert (bottomSheet != null);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        View btview = findViewById(R.id.btn_collapse);
        assert (btview!= null);
        btview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeDetailPane();
            }
        });
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        assert (mFab != null);
        setupFab(mFab);
        scheduleTask();

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this );
    }

    private  CursorRecyclerViewAdapter.QuoteAdapterOnClickHandler mSymbolSelectionListener =
        new CursorRecyclerViewAdapter.QuoteAdapterOnClickHandler() {
            @Override
            public void onClick(String symbol, CursorRecyclerViewAdapter.QuoteViewHolder vh) {
                mSelectedSymbol = symbol;
                Bundle args = new Bundle();
                args.putString(SELECTED_SYMBOL, symbol);
                if (mDetailLoader == null) {
                    getLoaderManager().initLoader(DETAIL_LOADER_ID, args, MainActivity.this);
                } else {
                    getLoaderManager().restartLoader(DETAIL_LOADER_ID, args, MainActivity.this);
                }
            }

        };
    private void removeDetailPane() {
        Log.v(LOG_TAG, "++ removeDetailPane");
        if ((mBottomSheetBehavior != null)&&(BottomSheetBehavior.STATE_EXPANDED == mBottomSheetBehavior.getState())) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(LOG_TAG, "++ onReceive received intent");
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork =cm.getActiveNetworkInfo();
            if (activeNetwork != null) {
                boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;  // not in use
                scheduleTask();
            } else {
                mFab.setVisibility(View.INVISIBLE);
            }
        }
    };
    private void setupFab(FloatingActionButton fab) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                removeDetailPane();
                final Context context = v.getContext();
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                final AlertDialog dlg = new AlertDialog.Builder(context, R.style.MyAlertDialogStyle)
                        .setTitle("Symbol Search").setMessage("Enter a stock symbol:")
                        .setView(input).setPositiveButton("OK", null).setNegativeButton("Cancel", null)
                        .create();
                dlg.setOnShowListener(new DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button b = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String symbol = input.getText().toString().toUpperCase();
                                if (isNewSymbol(v, symbol)) {
                                    dlg.dismiss();
                                } else {
                                    input.setText("");
                                }
                            }
                        });
                    }
                });
                dlg.show();
            }
        });
    }

    private void loadBackdrop() {
        View view = findViewById(R.id.backdrop);
        if (view != null) {
            final ImageView imageView = (ImageView) view;
            Glide.with(this).load(R.drawable.stock_image).centerCrop().into(imageView);
        }
    }
    private void scheduleTask() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.v(LOG_TAG, "+-+-+-scheduleTask no GPS, resultCode="+Integer.toString(resultCode));
        } else {
            Log.v(LOG_TAG, "+-+-+-scheduleTask !!! has GPS");
        }
        if (Utils.isNetworkAvailable(this)) {
            mFab.show();
            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            if (mPeriodicTask == null) {
                mPeriodicTask = new PeriodicTask.Builder()
                        .setService(StockTaskService.class)
                        .setPeriod(SYNC_INTERVAL)
                        .setFlex(SYNC_FLEXTIME)
                        .setTag(TAG_PERIODIC)
                        .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                        .setRequiresCharging(false)
                        .build();
                // Schedule task with tag "periodic." This ensure that only the stocks present in the DB are updated
                GcmNetworkManager.getInstance(this).schedule(mPeriodicTask);
                mServiceIntent.putExtra("tag", TAG_INIT);
                startService(mServiceIntent);
                Log.v(LOG_TAG, "GMS: scheduled task and initiate first");
            }
            return;
        }
        mFab.hide();
    }
    private boolean isNewSymbol(View view, String symbol) {
        Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                new String[] { symbol }, null);
        if (c.getCount() > 0) {
            Toast.makeText(view.getContext(), "This stock is already saved!", Toast.LENGTH_LONG).show();
            return false;
        } else {
            if (Utils.isNetworkAvailable(this)) {
                mServiceIntent.putExtra("tag", TAG_ADD);
                mServiceIntent.putExtra("symbol", symbol);
                startService(mServiceIntent);
                Snackbar.make(view, "getting quote for " + symbol + "...", Snackbar.LENGTH_SHORT);
            }
            return true;
        }
    }

    private void updateEmptyView() {
        int mid = Utils.getErrorStatusMessageId(this);
        TextView tv = (TextView)findViewById(R.id.recyclerview_empty);
        assert (tv != null);
        if (mAdapter.getItemCount() > 0) {
            tv.setVisibility(View.INVISIBLE);
            if (mid != -1) {
                Toast.makeText(this, getString(mid), Toast.LENGTH_LONG).show();
            }
        } else {
            if (mid == -1) {
                mid = R.string.empty_quote_list;
            }
            String textStr = getString(mid);
            tv.setText(textStr);
            tv.setContentDescription(textStr);
            tv.setVisibility(View.VISIBLE);
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedSymbol != null)
            outState.putString(SELECTED_SYMBOL, mSelectedSymbol);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static final String[] sQuoteProjection =new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP};
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (CURSOR_LOADER_ID == id ) {
            // This narrows the return to only the stocks that are most current.
            return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI, sQuoteProjection,
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);
        } else if ((DETAIL_LOADER_ID == id) && (args != null)) {
            String symbol = args.getString(SELECTED_SYMBOL);
            Uri uri = QuoteProvider.Quotes.withSymbol(symbol);
            Log.v(LOG_TAG, String.format("+++ onCreateLoader, id=%d, uri=%s", id, uri.toString()));
            mDetailLoader = new CursorLoader(this, uri, DetailProjection,
                    QuoteColumns.SYMBOL + "=?", new String[]{symbol}, null);
            return mDetailLoader;

        } else {
            return null;
        }
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (CURSOR_LOADER_ID == loader.getId()) {
            mAdapter.swapCursor(data);
            updateEmptyView();
            if (data.getCount() > 0) {
                mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        Cursor cursor = mAdapter.getCursor();
                        final int symbolColumn = cursor.getColumnIndex(QuoteColumns.SYMBOL);
                        int count = cursor.getCount();
                        int pos = RecyclerView.NO_POSITION;
                        if (mSelectedSymbol != null) {
                            for (int i = 0; i < count; i++) {
                                cursor.moveToPosition(i);
                                if (cursor.getString(symbolColumn) == mSelectedSymbol) {
                                    pos = i;
                                    break;
                                }
                            }
                        }
                        if (pos == RecyclerView.NO_POSITION) {
                            if (cursor.moveToFirst()) {
                                mSelectedSymbol = cursor.getString(symbolColumn);
                            }
                            pos = 0;
                        }
                        mRecyclerView.smoothScrollToPosition(pos);
                        return true;
                    }
                });
            }
        }  else if (DETAIL_LOADER_ID == loader.getId()) {
            if (data.moveToFirst()) {
                int colNum = 0;
                String symbol = data.getString(colNum++);       // symbol
                TextView tv = (TextView) findViewById(R.id.detail_symbol);
                tv.setText(symbol);
                tv.setContentDescription(symbol);
                String name = data.getString(colNum++);         // name
                tv = (TextView) findViewById(R.id.detail_title);
                tv.setText(name);
                tv.setContentDescription(name);
//                str = Integer.toString(data.getInt(colNum++));    // volume is int
//                tv = (TextView) findViewById(R.id.volume_data);
//                tv.setText(str);
//                tv.setContentDescription(str);

                for (int rid : tvColMap) {
                    View view = findViewById(rid);
                    if (view != null) {
                        Float val = data.getFloat(colNum++);
                        if (val != null) {
                            tv = (TextView) view;
                            tv.setText(Float.toString(val));
                            tv.setContentDescription(Float.toString(val));
                        }
                    }
                }
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }
    private static final String[] DetailProjection = new String[] {
            QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.VOLUME,
            QuoteColumns.DAYSLOW, QuoteColumns.DAYSHIGH,
            QuoteColumns.YEARLOW, QuoteColumns.YEARHIGH,
            QuoteColumns.FIFTYDAYMOVINGAVERAGE, QuoteColumns.TWOHUNDREDDAYMOVINGAVERAGE};
    private static final int[] tvColMap = new int[] {       //R.id.tv, idx is colNum of cursor, start 2
            R.id.days_low_data, R.id.days_high_data, R.id.year_low_data,
            R.id.year_high_data, R.id.fifty_ma_data, R.id.two_hundred_ma_data};
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
