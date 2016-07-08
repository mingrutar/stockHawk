package com.coderming.mystockhawk;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coderming.mystockhawk.data.QuoteColumns;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by linna on 7/1/2016.
 */
public class CursorRecyclerViewAdapter
        extends RecyclerView.Adapter<CursorRecyclerViewAdapter.QuoteViewHolder>{
    private static final String LOG_TAG = CursorRecyclerViewAdapter.class.getSimpleName();

    private Context mContext;
    private Cursor mCursor;
    private boolean dataIsValid;
    private int rowIdColumn;
    private DataSetObserver mDataSetObserver;
    private QuoteAdapterOnClickHandler mClickHandler;

    public static interface QuoteAdapterOnClickHandler {
        void onClick(String symbol, QuoteViewHolder vh);
    }
    public class QuoteViewHolder extends RecyclerView.ViewHolder
        implements OnClickListener {
        @BindView(R.id.stock_symbol) TextView mSymbol;
        @BindView(R.id.bid_price) TextView mBidPrice;
        @BindView(R.id.change) TextView mChange;

        public QuoteViewHolder(View itemView){
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            int symbolColumnIndex = mCursor.getColumnIndex(QuoteColumns.SYMBOL);
            mClickHandler.onClick(mCursor.getString(symbolColumnIndex), this);
        }
    }

    public CursorRecyclerViewAdapter(Context context, QuoteAdapterOnClickHandler clickHandler){
        mClickHandler = clickHandler;
        mContext = context;
        dataIsValid = false;
        rowIdColumn = dataIsValid ? mCursor.getColumnIndex("_id") : -1;
        mDataSetObserver = new NotifyingDataSetObserver();
        if (dataIsValid){
            mCursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    public Cursor getCursor(){
        return mCursor;
    }

    @Override
    public int getItemCount(){
        if (dataIsValid && mCursor != null){
            return mCursor.getCount();
        }
        return 0;
    }

    @Override public long getItemId(int position) {
        if (dataIsValid && mCursor != null && mCursor.moveToPosition(position)){
            return mCursor.getLong(rowIdColumn);
        }
        return 0;
    }

    @Override public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    @Override
    public QuoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_quote, parent, false);
        itemView.setFocusable(true);
        return new QuoteViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(QuoteViewHolder viewHolder, int position) {
        if (!dataIsValid){
            throw new IllegalStateException("This should only be called when Cursor is valid");
        }
        mCursor.moveToPosition(position);
        String str = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
        viewHolder.mSymbol.setText(str);
        viewHolder.mSymbol.setContentDescription(str);
        String priceStr = Utils.getPriceStr(mContext, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE)));
        viewHolder.mBidPrice.setText(priceStr);
        viewHolder.mBidPrice.setContentDescription(priceStr);
        boolean isUp = (mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.ISUP)) == 1);
        String changeStr = Utils.getChangeStr(mContext, isUp,  mCursor.getString(mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE))
                , mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE)) ) ;
        Utils.setPriceText(mContext, changeStr, isUp, viewHolder.mChange);
    }

    public Cursor swapCursor(Cursor newCursor){
        if (newCursor == mCursor){
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null && mDataSetObserver != null){
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;
        if (mCursor != null){
            if (mDataSetObserver != null){
                mCursor.registerDataSetObserver(mDataSetObserver);
            }
            rowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            dataIsValid = true;
            notifyDataSetChanged();
        }else{
            rowIdColumn = -1;
            dataIsValid = false;
            notifyDataSetChanged();
        }
        return oldCursor;
    }

    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override public void onChanged() {
            super.onChanged();
            dataIsValid = true;
            notifyDataSetChanged();
        }

        @Override public void onInvalidated() {
            super.onInvalidated();
            dataIsValid = false;
            notifyDataSetChanged();
        }
    }
}
