package com.example.shareinfo.data.market;

import com.example.shareinfo.Fixtures;
import com.example.shareinfo.data.Topic;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MarketParserTest {

    @Test
    public void parsesYahooChart() throws Exception {
        MarketSnapshot snapshot = YahooFinance.parseChart(Fixtures.read("fixtures/yahoo_chart_aapl.json"));
        assertEquals("AAPL", snapshot.symbol);
        assertEquals("Apple Inc.", snapshot.name);
        assertEquals("USD", snapshot.currency);
        assertEquals(345.82, snapshot.price, 0.001);
        assertEquals(1.117, snapshot.changePercent, 0.001);
        assertEquals(23, snapshot.closes.length);
    }

    @Test
    public void searchKeepsTradableAssetsOnly() throws Exception {
        List<Topic> topics = YahooFinance.parseSearch(Fixtures.read("fixtures/yahoo_search_bitcoin.json"));
        Topic bitcoin = topics.get(0);
        assertEquals(Topic.Type.CRYPTO, bitcoin.type);
        assertEquals("BTC", bitcoin.symbol);
        assertEquals("Bitcoin", bitcoin.name);
        assertEquals("BTC-USD", bitcoin.marketSymbol);
        for (Topic topic : topics) {
            // Futures such as BTC=F are dropped.
            assertFalse(topic.symbol.contains("="));
        }
        assertTrue(topics.contains(Topic.fund("IBIT", "iShares Bitcoin Trust ETF")));
    }

    @Test
    public void parsesYahooTrending() throws Exception {
        List<String> symbols = YahooFinance.parseTrending(Fixtures.read("fixtures/yahoo_trending.json"));
        assertEquals("TSLA", symbols.get(0));
    }

    @Test
    public void parsesStockTwitsTrendingAndSkipsCurrencies() throws Exception {
        List<Topic> topics = TrendingTopics.parseStockTwits(Fixtures.read("fixtures/stocktwits_trending.json"));
        assertFalse(topics.isEmpty());
        assertTrue(topics.size() <= 12);
        assertEquals(Topic.stock("SLS", "SELLAS Life Sciences Group Inc"), topics.get(0));
        assertEquals("SELLAS Life Sciences", topics.get(0).name);
        for (Topic topic : topics) {
            assertFalse("GBPUSD".equals(topic.symbol));
        }
    }

    @Test
    public void keepsOnlyEstablishedTrendingCoins() throws Exception {
        List<Topic> coins = TrendingTopics.parseCoinGecko(Fixtures.read("fixtures/coingecko_trending.json"));
        assertFalse(coins.isEmpty());
        for (Topic coin : coins) {
            assertEquals(Topic.Type.CRYPTO, coin.type);
            assertFalse("DACITY".equals(coin.symbol)); // Rank 796.
        }
    }
}
