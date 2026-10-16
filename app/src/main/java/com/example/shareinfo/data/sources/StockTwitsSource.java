package com.example.shareinfo.data.sources;

import com.example.shareinfo.data.Http;
import com.example.shareinfo.data.Json;
import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;
import com.example.shareinfo.data.Topic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * StockTwits symbol streams. No API key is needed, and authors can tag each message as Bullish
 * or Bearish, which is the most direct sentiment signal of any source.
 */
public class StockTwitsSource implements PostSource {
    // Messages that tag this many tickers are almost always spam or watchlist dumps.
    private static final int MAX_SYMBOLS_PER_MESSAGE = 4;

    @Override
    public Source source() {
        return Source.STOCKTWITS;
    }

    @Override
    public boolean supports(Topic topic) {
        return topic.isMarket();
    }

    @Override
    public List<Post> fetch(Topic topic) throws IOException {
        String url = "https://api.stocktwits.com/api/2/streams/symbol/" + Http.encode(streamSymbol(topic)) + ".json";
        try {
            return parse(Http.get(url));
        } catch (JSONException e) {
            throw new IOException("Unexpected StockTwits response", e);
        }
    }

    static String streamSymbol(Topic topic) {
        // StockTwits suffixes crypto tickers with ".X" (BTC.X).
        return topic.type == Topic.Type.CRYPTO ? topic.symbol + ".X" : topic.symbol;
    }

    static List<Post> parse(String body) throws JSONException {
        JSONArray messages = new JSONObject(body).getJSONArray("messages");
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.getJSONObject(i);
            JSONArray symbols = message.optJSONArray("symbols");
            if (symbols != null && symbols.length() > MAX_SYMBOLS_PER_MESSAGE) {
                continue;
            }
            JSONObject user = message.optJSONObject("user");
            String username = Json.string(user, "username", "");
            String id = String.valueOf(message.optLong("id"));

            int label = 0;
            JSONObject entities = message.optJSONObject("entities");
            JSONObject sentiment = entities != null ? entities.optJSONObject("sentiment") : null;
            if (sentiment != null) {
                String basic = Json.string(sentiment, "basic", "");
                if ("Bullish".equalsIgnoreCase(basic)) {
                    label = 1;
                } else if ("Bearish".equalsIgnoreCase(basic)) {
                    label = -1;
                }
            }
            JSONObject likes = message.optJSONObject("likes");
            JSONObject conversation = message.optJSONObject("conversation");
            JSONObject reshares = message.optJSONObject("reshares");

            posts.add(new Post(Source.STOCKTWITS, id, username,
                    Text.decodeEntities(Json.string(message, "body", "")),
                    "https://stocktwits.com/" + username + "/message/" + id,
                    Text.parseIso(Json.string(message, "created_at", "")),
                    likes != null ? likes.optInt("total") : 0,
                    conversation != null ? conversation.optInt("replies") : 0,
                    reshares != null ? reshares.optInt("reshared_count") : 0,
                    label));
        }
        return posts;
    }
}
