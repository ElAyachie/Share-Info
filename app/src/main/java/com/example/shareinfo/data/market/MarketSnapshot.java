package com.example.shareinfo.data.market;

/** Latest price and recent daily closes for a ticker. */
public class MarketSnapshot {
    public final String symbol;
    public final String name;
    public final String currency;
    public final double price;
    /** Percent change on the day, e.g. 1.25 for +1.25%. */
    public final double changePercent;
    /** Daily closing prices, oldest first. */
    public final double[] closes;

    public MarketSnapshot(String symbol, String name, String currency, double price, double changePercent, double[] closes) {
        this.symbol = symbol;
        this.name = name;
        this.currency = currency;
        this.price = price;
        this.changePercent = changePercent;
        this.closes = closes;
    }
}
