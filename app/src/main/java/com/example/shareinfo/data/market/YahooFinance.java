package com.example.shareinfo.data.market;

import com.example.shareinfo.data.Http;
import com.example.shareinfo.data.Json;
import com.example.shareinfo.data.Topic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Yahoo Finance's public chart and search endpoints. These replace the RapidAPI wrapper the app
 * used before, so no API key is needed.
 */
public class YahooFinance {
    private static final String HOST = "https://query1.finance.yahoo.com";

    public MarketSnapshot chart(String marketSymbol) throws IOException {
        String url = HOST + "/v8/finance/chart/" + Http.encode(marketSymbol) + "?range=1mo&interval=1d";
        try {
            return parseChart(Http.get(url));
        } catch (JSONException e) {
            throw new IOException("Unexpected Yahoo chart response", e);
        }
    }

    public List<Topic> search(String query) throws IOException {
        String url = HOST + "/v1/finance/search?q=" + Http.encode(query) + "&quotesCount=10&newsCount=0";
        try {
            return parseSearch(Http.get(url));
        } catch (JSONException e) {
            throw new IOException("Unexpected Yahoo search response", e);
        }
    }

    public List<String> trendingSymbols() throws IOException {
        try {
            return parseTrending(Http.get(HOST + "/v1/finance/trending/US?count=15"));
        } catch (JSONException e) {
            throw new IOException("Unexpected Yahoo trending response", e);
        }
    }

    static MarketSnapshot parseChart(String body) throws JSONException {
        JSONObject result = new JSONObject(body).getJSONObject("chart").getJSONArray("result").getJSONObject(0);
        JSONObject meta = result.getJSONObject("meta");
        JSONArray rawCloses = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
                .optJSONArray("close");

        List<Double> closes = new ArrayList<>();
        if (rawCloses != null) {
            for (int i = 0; i < rawCloses.length(); i++) {
                // Days without trades come back as null.
                if (!rawCloses.isNull(i)) {
                    closes.add(rawCloses.getDouble(i));
                }
            }
        }
        double[] closeArray = new double[closes.size()];
        for (int i = 0; i < closeArray.length; i++) {
            closeArray[i] = closes.get(i);
        }

        double lastClose = closeArray.length > 0 ? closeArray[closeArray.length - 1] : 0;
        double price = meta.optDouble("regularMarketPrice", lastClose);
        double changePercent;
        if (meta.has("regularMarketChangePercent")) {
            changePercent = meta.getDouble("regularMarketChangePercent");
        } else if (closeArray.length > 1 && closeArray[closeArray.length - 2] != 0) {
            double previous = closeArray[closeArray.length - 2];
            changePercent = (price - previous) / previous * 100;
        } else {
            changePercent = 0;
        }
        String name = Json.string(meta, "shortName", Json.string(meta, "longName", Json.string(meta, "symbol", "")));
        return new MarketSnapshot(Json.string(meta, "symbol", ""), name, Json.string(meta, "currency", "USD"),
                price, changePercent, closeArray);
    }

    static List<Topic> parseSearch(String body) throws JSONException {
        JSONArray quotes = new JSONObject(body).optJSONArray("quotes");
        List<Topic> topics = new ArrayList<>();
        if (quotes == null) {
            return topics;
        }
        for (int i = 0; i < quotes.length(); i++) {
            JSONObject quote = quotes.getJSONObject(i);
            String symbol = Json.string(quote, "symbol", "");
            String name = Json.string(quote, "longname", Json.string(quote, "shortname", symbol));
            if (symbol.isEmpty()) {
                continue;
            }
            switch (quote.optString("quoteType").toUpperCase(Locale.ROOT)) {
                case "EQUITY":
                    topics.add(Topic.stock(symbol, name));
                    break;
                case "ETF":
                case "MUTUALFUND":
                    topics.add(Topic.fund(symbol, name));
                    break;
                case "CRYPTOCURRENCY":
                    // Only USD pairs; "BTC-EUR" is the same conversation as "BTC-USD".
                    if (symbol.endsWith("-USD")) {
                        topics.add(Topic.crypto(symbol.substring(0, symbol.length() - 4), name));
                    }
                    break;
                default:
                    // Futures, options, currencies and indexes have no social stream of their own.
                    break;
            }
        }
        return topics;
    }

    static List<String> parseTrending(String body) throws JSONException {
        JSONArray quotes = new JSONObject(body).getJSONObject("finance").getJSONArray("result")
                .getJSONObject(0).getJSONArray("quotes");
        List<String> symbols = new ArrayList<>();
        for (int i = 0; i < quotes.length(); i++) {
            String symbol = Json.string(quotes.getJSONObject(i), "symbol", "");
            // Skip indexes (^GSPC) and futures (ES=F).
            if (!symbol.isEmpty() && !symbol.startsWith("^") && !symbol.contains("=")) {
                symbols.add(symbol);
            }
        }
        return symbols;
    }
}
