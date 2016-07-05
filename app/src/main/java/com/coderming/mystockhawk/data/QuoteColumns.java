package com.coderming.mystockhawk.data;

import android.support.annotation.Nullable;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

public class QuoteColumns {
    @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
    public static final String _ID = "_id";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String SYMBOL = "symbol";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String PERCENT_CHANGE = "percent_change";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String CHANGE = "change";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String BIDPRICE = "bid_price";
    @DataType(DataType.Type.TEXT)
    public static final String CREATED = "created";
    @DataType(DataType.Type.INTEGER) @NotNull
    public static final String ISUP = "is_up";
    @DataType(DataType.Type.INTEGER) @NotNull
    public static final String ISCURRENT = "is_current";

    @DataType(DataType.Type.REAL) @NotNull
    public static final String DAYSLOW = "days_low";
    @DataType(DataType.Type.REAL) @NotNull
    public static final String DAYSHIGH = "days_high";
    @DataType(DataType.Type.REAL) @NotNull
    public static final String YEARLOW = "year_low";
    @DataType(DataType.Type.REAL) @NotNull
    public static final String YEARHIGH = "year_high";
    @DataType(DataType.Type.REAL) @Nullable
    public static final String FIFTYDAYMOVINGAVERAGE = "fiftyday_moving_average";
    @DataType(DataType.Type.REAL) @Nullable
    public static final String TWOHUNDREDDAYMOVINGAVERAGE = "two_hundredday_moving_average";
    @DataType(DataType.Type.INTEGER) @NotNull
    public static final String VOLUME = "volume";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String NAME = "name";

}
