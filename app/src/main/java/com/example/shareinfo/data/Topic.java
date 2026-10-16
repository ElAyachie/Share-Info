package com.example.shareinfo.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Something people talk about: a stock, a fund, a cryptocurrency, or any free text subject such
 * as "pokemon cards". Market topics carry a ticker, which unlocks price data and StockTwits.
 */
public final class Topic {
    public enum Type { STOCK, FUND, CRYPTO, OTHER }

    // Corporate suffixes that add noise to news and social searches ("Apple Inc." -> "Apple").
    private static final Pattern NAME_SUFFIX = Pattern.compile(
            "(?i)[\\s,]+(inc|incorporated|corp|corporation|co|company|ltd|limited|plc|llc|lp|n\\.?v|s\\.?a|ag|"
                    + "holdings?|group|- ?adr|adr|class [a-c]|usd|cad|eur|common stock|ordinary shares)\\.?$");

    public final Type type;
    /** Ticker without exchange decoration ("AAPL", "BTC"); null for OTHER. */
    public final String symbol;
    /** Human readable name used for searches ("Apple", "Bitcoin", "pokemon cards"). */
    public final String name;
    /** Symbol for Yahoo Finance price data ("AAPL", "BTC-USD"); null when there is none. */
    public final String marketSymbol;

    private Topic(Type type, String symbol, String name, String marketSymbol) {
        this.type = type;
        this.symbol = symbol;
        this.name = name;
        this.marketSymbol = marketSymbol;
    }

    public static Topic stock(String symbol, String rawName) {
        String ticker = symbol.toUpperCase(Locale.ROOT);
        return new Topic(Type.STOCK, ticker, cleanName(rawName, ticker), ticker);
    }

    public static Topic fund(String symbol, String rawName) {
        String ticker = symbol.toUpperCase(Locale.ROOT);
        return new Topic(Type.FUND, ticker, cleanName(rawName, ticker), ticker);
    }

    /** @param symbol base currency symbol such as "BTC" (not "BTC-USD"). */
    public static Topic crypto(String symbol, String rawName) {
        String ticker = symbol.toUpperCase(Locale.ROOT);
        return new Topic(Type.CRYPTO, ticker, cleanName(rawName, ticker), ticker + "-USD");
    }

    public static Topic custom(String query) {
        return new Topic(Type.OTHER, null, query.trim().replaceAll("\\s+", " "), null);
    }

    /** Stable identifier used for following and caching. */
    public String id() {
        return type.name() + ":" + (symbol != null ? symbol : name.toLowerCase(Locale.ROOT));
    }

    public boolean isMarket() {
        return type != Type.OTHER;
    }

    /** Label for chips and lists. */
    public String label() {
        return symbol != null ? symbol : name;
    }

    /** Query for news search engines. */
    public String newsQuery() {
        switch (type) {
            case STOCK:
                return "\"" + name + "\" stock";
            case FUND:
                return symbol + " ETF";
            case CRYPTO:
                return "\"" + name + "\" crypto";
            default:
                return name;
        }
    }

    /** Query for social networks, where cashtags are the most precise way to find a ticker. */
    public String socialQuery() {
        switch (type) {
            case STOCK:
            case FUND:
                return "$" + symbol;
            case CRYPTO:
                return name;
            default:
                return name;
        }
    }

    /** Words naming the topic itself, which should not count toward sentiment. */
    public List<String> ignoreTerms() {
        List<String> terms = new ArrayList<>();
        terms.add(name);
        if (symbol != null) {
            terms.add(symbol);
        }
        return terms;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("type", type.name());
            json.put("name", name);
            if (symbol != null) {
                json.put("symbol", symbol);
            }
            if (marketSymbol != null) {
                json.put("marketSymbol", marketSymbol);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return json;
    }

    public static Topic fromJson(JSONObject json) {
        Type type = Type.valueOf(json.optString("type", Type.OTHER.name()));
        String symbol = json.has("symbol") ? json.optString("symbol") : null;
        String marketSymbol = json.has("marketSymbol") ? json.optString("marketSymbol") : null;
        return new Topic(type, symbol, json.optString("name"), marketSymbol);
    }

    static String cleanName(String rawName, String fallback) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return fallback;
        }
        String name = rawName.trim().replaceAll("\\s+", " ");
        String previous;
        do {
            previous = name;
            name = NAME_SUFFIX.matcher(name).replaceAll("").trim();
        } while (!name.equals(previous) && !name.isEmpty());
        if (name.toLowerCase(Locale.ROOT).startsWith("the ")) {
            name = name.substring(4);
        }
        return name.isEmpty() ? fallback : name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Topic && ((Topic) other).id().equals(id());
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }

    @Override
    public String toString() {
        return id();
    }
}
