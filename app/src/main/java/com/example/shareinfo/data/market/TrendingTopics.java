package com.example.shareinfo.data.market;

import com.example.shareinfo.data.Http;
import com.example.shareinfo.data.Json;
import com.example.shareinfo.data.Topic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What people are talking about right now. StockTwits ranks tickers by social activity, which
 * suits this app better than ranking by price moves. CoinGecko adds trending coins, and Yahoo's
 * trending tickers are the fallback when StockTwits is unavailable.
 */
public class TrendingTopics {
    private static final int MAX_STOCKS = 12;
    private static final int MAX_COINS = 4;
    // CoinGecko's trending list is full of brand-new micro caps; keep established coins.
    private static final int MAX_COIN_RANK = 300;

    private final YahooFinance yahoo;

    public TrendingTopics(YahooFinance yahoo) {
        this.yahoo = yahoo;
    }

    public List<Topic> load() throws IOException {
        Map<String, Topic> topics = new LinkedHashMap<>();
        IOException error = null;
        try {
            for (Topic topic : parseStockTwits(Http.get("https://api.stocktwits.com/api/2/trending/symbols.json"))) {
                topics.put(topic.id(), topic);
            }
        } catch (IOException e) {
            error = e;
        } catch (JSONException e) {
            error = new IOException("Unexpected StockTwits trending response", e);
        }
        if (topics.isEmpty()) {
            try {
                for (String symbol : yahoo.trendingSymbols()) {
                    Topic topic = symbol.endsWith("-USD")
                            ? Topic.crypto(symbol.substring(0, symbol.length() - 4), symbol)
                            : Topic.stock(symbol, symbol);
                    topics.put(topic.id(), topic);
                }
            } catch (IOException e) {
                error = e;
            }
        }
        try {
            for (Topic topic : parseCoinGecko(Http.get("https://api.coingecko.com/api/v3/search/trending"))) {
                topics.putIfAbsent(topic.id(), topic);
            }
        } catch (IOException | JSONException ignored) {
            // Coins are a bonus; the stock list is enough on its own.
        }
        if (topics.isEmpty() && error != null) {
            throw error;
        }
        return new ArrayList<>(topics.values());
    }

    static List<Topic> parseStockTwits(String body) throws JSONException {
        JSONArray symbols = new JSONObject(body).getJSONArray("symbols");
        List<Topic> topics = new ArrayList<>();
        for (int i = 0; i < symbols.length() && topics.size() < MAX_STOCKS; i++) {
            JSONObject symbol = symbols.getJSONObject(i);
            String ticker = Json.string(symbol, "symbol", "");
            String title = Json.string(symbol, "title", ticker);
            switch (symbol.optString("instrument_class").toLowerCase(Locale.ROOT)) {
                case "crypto":
                    String base = ticker.endsWith(".X") ? ticker.substring(0, ticker.length() - 2) : ticker;
                    topics.add(Topic.crypto(base, title));
                    break;
                case "exchangetradedfund":
                    topics.add(Topic.fund(ticker, title));
                    break;
                case "stock":
                case "depositoryreceipt":
                    topics.add(Topic.stock(ticker, title));
                    break;
                default:
                    // Currencies and indexes.
                    break;
            }
        }
        return topics;
    }

    static List<Topic> parseCoinGecko(String body) throws JSONException {
        JSONArray coins = new JSONObject(body).getJSONArray("coins");
        List<Topic> topics = new ArrayList<>();
        for (int i = 0; i < coins.length() && topics.size() < MAX_COINS; i++) {
            JSONObject item = coins.getJSONObject(i).getJSONObject("item");
            int rank = item.optInt("market_cap_rank", Integer.MAX_VALUE);
            if (rank <= 0 || rank > MAX_COIN_RANK) {
                continue;
            }
            topics.add(Topic.crypto(item.getString("symbol"), Json.string(item, "name", item.getString("symbol"))));
        }
        return topics;
    }
}
