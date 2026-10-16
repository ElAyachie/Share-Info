# Share Info

## Introduction

### Problem statements 
The stock market can be daunting to newcomers who don't know where to look for information.  With so many different social media and news media outlets it is impossible to take it all in. Existing solutions do a bad job displaying relevant information to the user and giving them an indication of how it correlates to the stock’s performance. This paper proposes a solution that shows the user trending stocks in the stock market and how they correlate with social media and news media.

### Proposed solution 
The purpose of Share Info was to create an online hub for the stock market where users can view online articles and social media posts for stocks and cryptocurrency. We wanted users to view a live feed of congregated information from online spaces like Twitter, Reddit, and the news media. They’d be able to see live market figures and how they correspond to these online spaces. We wanted to allow new users who are interested in but unfamiliar with the stock market to help them get started. With many different sources, users would be able to make their inferences on stocks/crypto that they may or may not want to buy.

### Novelty 
The popular stock trading application Robinhood displays articles to the user that pertain to a stock. To see this information users, have to search for a stock on the application and can scroll down to view information from news articles about the stock. Users can click on these articles to be taken to a page where they can read the article. While Robinhood does a great job displaying news articles about the stock, our application takes in information from news articles and social media posts that talk about the stock. Share Info incorporates information from Twitter and Reddit. Share Info also intends to sort information based on an algorithm designed to try and find the posts or articles that are most relevant.

## Functions
1. **Trending** – the tickers people are talking about most right now (StockTwits trending list, plus trending coins from CoinGecko), each with a sentiment report.
2. **Favorites** – sentiment reports for everything you follow.
3. **Search** – find a stock, fund or cryptocurrency by name or ticker, or track *any* free-text topic such as "pokemon cards".
4. **Following** – manage followed topics, and read how the score is calculated.

Each report shows a 0–100 sentiment score, the bullish/neutral/bearish split, a confidence level, a 7-day sentiment trend, a per-source breakdown, the price over the last month (for tickers) and the latest posts. Tap a post to open it.

## Data sources
None of these need an API key.

| Data | Source | Notes |
| --- | --- | --- |
| Social posts | [StockTwits](https://stocktwits.com) | Tickers only. Authors can tag posts Bullish/Bearish, which is blended into the score. |
| Social posts | [Bluesky](https://bsky.app) search | Replaces Twitter/X, whose API is no longer free. |
| Social posts | Reddit search feed | Replaces SocialGrep (shut down). Rate limited per IP, so it is best effort. |
| Headlines | Google News RSS (past 7 days) | Replaces Mediastack. |
| Prices, ticker search | Yahoo Finance chart/search endpoints | Replaces the RapidAPI wrapper. |
| Trending coins | CoinGecko | |

If a source fails, the report is built from the others and shows which source was unavailable.

## How sentiment is scored
- Each post is scored from −1 to +1 with a Java port of [VADER](https://github.com/cjhutto/vaderSentiment) (MIT; its license ships in `app/src/main/assets/VADER_LICENSE.txt`), a model built for social media text. It handles negation, intensifiers, ALL CAPS, exclamation marks and "but" clauses. The port matches the reference Python implementation on the cases in `app/src/test/resources/vader_reference.json`.
- `app/src/main/assets/market_lexicon.txt` adapts VADER to market talk. It adds slang ("bullish", "rekt", "bagholder"), emoji (🚀 📉) and phrases ("all-time high", "dead cat bounce"). It also neutralizes words that VADER reads as emotional but that are neutral in finance ("shares", "interest", "trust"). Cashtags, links and the topic's own name are ignored, so "Best Buy" doesn't make every post about it positive.
- A topic's score is a weighted average. Recent posts count more (48-hour half-life), and engagement counts logarithmically, so one viral post can't dominate. The average is pulled toward neutral when there are few posts. Confidence comes from the effective sample size.

## Project layout
- `data/` – topics, posts, HTTP, the report builder and repository, `sources/` (one class per site) and `market/` (Yahoo Finance, trending).
- `sentiment/` – the VADER port, the market layer and aggregation.
- `ui/` – fragments and adapters.

Run the unit tests with `./gradlew testDebugUnitTest`. They use API responses captured in `app/src/test/resources/fixtures`.

### Deployment
[https://developer.android.com/studio/run](https://developer.android.com/studio/run)

## Demo video
https://www.youtube.com/watch?v=iK_hcvb4ENI&ab_channel=MohamedOulal

## Contributors
- Mohamed Oulal, oulalm@wit.edu, Team Lead, Full stack
- Caleb Franco, francoj2@wit.edu, Front End
