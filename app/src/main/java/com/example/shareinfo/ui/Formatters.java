package com.example.shareinfo.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.example.shareinfo.R;
import com.example.shareinfo.data.Source;
import com.example.shareinfo.data.Topic;
import com.example.shareinfo.sentiment.SentimentAggregator;
import com.example.shareinfo.sentiment.SentimentSummary;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/** Turns model values into display text and colors. */
final class Formatters {
    private Formatters() {
    }

    /** Markets talk about bullish/bearish; everything else about positive/negative. */
    static String label(Context context, SentimentSummary.Label label, Topic topic) {
        boolean market = topic.isMarket();
        switch (label) {
            case VERY_POSITIVE:
                return context.getString(market ? R.string.label_very_bullish : R.string.label_very_positive);
            case POSITIVE:
                return context.getString(market ? R.string.label_bullish : R.string.label_positive);
            case SLIGHTLY_POSITIVE:
                return context.getString(market ? R.string.label_slightly_bullish : R.string.label_slightly_positive);
            case SLIGHTLY_NEGATIVE:
                return context.getString(market ? R.string.label_slightly_bearish : R.string.label_slightly_negative);
            case NEGATIVE:
                return context.getString(market ? R.string.label_bearish : R.string.label_negative);
            case VERY_NEGATIVE:
                return context.getString(market ? R.string.label_very_bearish : R.string.label_very_negative);
            default:
                return context.getString(R.string.label_neutral);
        }
    }

    static String postLabel(Context context, double score, Topic topic) {
        if (score > SentimentAggregator.NEUTRAL_BAND) {
            return context.getString(topic.isMarket() ? R.string.label_bullish : R.string.label_positive);
        }
        if (score < -SentimentAggregator.NEUTRAL_BAND) {
            return context.getString(topic.isMarket() ? R.string.label_bearish : R.string.label_negative);
        }
        return context.getString(R.string.label_neutral);
    }

    static String positiveWord(Context context, Topic topic) {
        return context.getString(topic.isMarket() ? R.string.word_bullish : R.string.word_positive);
    }

    static String negativeWord(Context context, Topic topic) {
        return context.getString(topic.isMarket() ? R.string.word_bearish : R.string.word_negative);
    }

    @ColorInt
    static int sentimentColor(Context context, double score) {
        int color = R.color.sentiment_neutral;
        if (score > SentimentAggregator.NEUTRAL_BAND) {
            color = R.color.sentiment_positive;
        } else if (score < -SentimentAggregator.NEUTRAL_BAND) {
            color = R.color.sentiment_negative;
        }
        return ContextCompat.getColor(context, color);
    }

    @ColorInt
    static int sourceColor(Context context, Source source) {
        int color;
        switch (source) {
            case STOCKTWITS:
                color = R.color.source_stocktwits;
                break;
            case BLUESKY:
                color = R.color.source_bluesky;
                break;
            case REDDIT:
                color = R.color.source_reddit;
                break;
            case YOUTUBE:
                color = R.color.source_youtube;
                break;
            default:
                color = R.color.source_news;
                break;
        }
        return ContextCompat.getColor(context, color);
    }

    /** Colored text on a faint pill of the same color; the theme sets how faint. */
    static void tintBadge(TextView badge, @ColorInt int color) {
        float alpha = ResourcesCompat.getFloat(badge.getResources(), R.dimen.badge_tint_alpha);
        badge.setTextColor(color);
        badge.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, Math.round(alpha * 255))));
    }

    static String score(double score) {
        if (Math.abs(score) < 0.005) {
            return "0.00"; // Avoid "-0.00".
        }
        return String.format(Locale.US, "%+.2f", score);
    }

    static String typeName(Context context, Topic topic) {
        switch (topic.type) {
            case STOCK:
                return context.getString(R.string.type_stock);
            case FUND:
                return context.getString(R.string.type_fund);
            case CRYPTO:
                return context.getString(R.string.type_crypto);
            default:
                return context.getString(R.string.type_other);
        }
    }

    /** "AAPL · Stock" for tickers, "Topic" for free text. */
    static String subtitle(Context context, Topic topic) {
        return topic.symbol != null ? topic.symbol + " · " + typeName(context, topic) : typeName(context, topic);
    }

    static String price(double price, String currencyCode) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        try {
            format.setCurrency(Currency.getInstance(currencyCode));
        } catch (IllegalArgumentException ignored) {
            // Unknown code; keep the locale's currency symbol.
        }
        // Small coins trade in fractions of a cent.
        int decimals = price >= 1 ? 2 : price >= 0.01 ? 4 : 6;
        format.setMinimumFractionDigits(decimals);
        format.setMaximumFractionDigits(decimals);
        return format.format(price);
    }

    static String percentChange(double percent) {
        return String.format(Locale.getDefault(), "%+.2f%% today", percent);
    }

    static CharSequence relativeTime(long millis) {
        if (millis <= 0) {
            return "";
        }
        return DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    static String confidence(Context context, SentimentSummary.Confidence confidence) {
        switch (confidence) {
            case HIGH:
                return context.getString(R.string.confidence_high);
            case MEDIUM:
                return context.getString(R.string.confidence_medium);
            case LOW:
                return context.getString(R.string.confidence_low);
            default:
                return context.getString(R.string.confidence_none);
        }
    }

    /** Short reason for an error, suitable after "Couldn't load data." */
    static String errorReason(Exception error) {
        if (error instanceof java.net.UnknownHostException) {
            return "You appear to be offline.";
        }
        String message = error.getMessage();
        return message != null && message.length() < 120 ? message : "Please try again.";
    }
}
