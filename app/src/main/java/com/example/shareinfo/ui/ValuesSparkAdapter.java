package com.example.shareinfo.ui;

import androidx.annotation.NonNull;

import com.robinhood.spark.SparkAdapter;

/** Feeds a fixed series of values to a SparkView. */
class ValuesSparkAdapter extends SparkAdapter {
    private final float[] values;
    private final Float baseLine;

    /** @param baseLine value to draw a reference line at, or null for none. */
    ValuesSparkAdapter(float[] values, Float baseLine) {
        this.values = values;
        this.baseLine = baseLine;
    }

    @Override
    public int getCount() {
        return values.length;
    }

    @NonNull
    @Override
    public Object getItem(int index) {
        return values[index];
    }

    @Override
    public float getY(int index) {
        return values[index];
    }

    @Override
    public boolean hasBaseLine() {
        return baseLine != null;
    }

    @Override
    public float getBaseLine() {
        return baseLine != null ? baseLine : super.getBaseLine();
    }
}
