package com.example.shareinfo.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shareinfo.R;
import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;
import com.example.shareinfo.data.TopicReport;
import com.example.shareinfo.data.market.MarketSnapshot;
import com.example.shareinfo.sentiment.SentimentSummary;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.robinhood.spark.SparkView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A summary card for the topic followed by its posts. */
class ReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_POST = 1;

    interface FollowHandler {
        boolean isFollowed(TopicReport report);

        /** Toggles following and returns the new state. */
        boolean toggleFollow(TopicReport report);
    }

    private final FollowHandler followHandler;
    private TopicReport report;

    ReportAdapter(FollowHandler followHandler) {
        this.followHandler = followHandler;
    }

    void setReport(TopicReport report) {
        this.report = report;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return report == null ? 0 : 1 + report.posts.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_report_header, parent, false));
        }
        return new PostHolder(inflater.inflate(R.layout.item_post, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind(report, followHandler);
        } else {
            ((PostHolder) holder).bind(report, report.posts.get(position - 1));
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView topicName;
        private final TextView topicSubtitle;
        private final MaterialButton followButton;
        private final TextView sentimentIndex;
        private final TextView sentimentLabel;
        private final View barPositive;
        private final View barNeutral;
        private final View barNegative;
        private final TextView sentimentCounts;
        private final TextView sentimentMeta;
        private final View priceSection;
        private final TextView price;
        private final TextView priceChange;
        private final SparkView priceChart;
        private final View trendSection;
        private final SparkView trendChart;
        private final LinearLayout sourceRows;
        private final TextView failedSources;
        private final TextView postsHeader;

        HeaderHolder(View view) {
            super(view);
            topicName = view.findViewById(R.id.topicName);
            topicSubtitle = view.findViewById(R.id.topicSubtitle);
            followButton = view.findViewById(R.id.followButton);
            sentimentIndex = view.findViewById(R.id.sentimentIndex);
            sentimentLabel = view.findViewById(R.id.sentimentLabel);
            barPositive = view.findViewById(R.id.barPositive);
            barNeutral = view.findViewById(R.id.barNeutral);
            barNegative = view.findViewById(R.id.barNegative);
            sentimentCounts = view.findViewById(R.id.sentimentCounts);
            sentimentMeta = view.findViewById(R.id.sentimentMeta);
            priceSection = view.findViewById(R.id.priceSection);
            price = view.findViewById(R.id.price);
            priceChange = view.findViewById(R.id.priceChange);
            priceChart = view.findViewById(R.id.priceChart);
            trendSection = view.findViewById(R.id.trendSection);
            trendChart = view.findViewById(R.id.trendChart);
            sourceRows = view.findViewById(R.id.sourceRows);
            failedSources = view.findViewById(R.id.failedSources);
            postsHeader = view.findViewById(R.id.postsHeader);
            // Clip the colored segments to the bar's rounded track.
            view.findViewById(R.id.distributionBar).setClipToOutline(true);
        }

        void bind(TopicReport report, FollowHandler followHandler) {
            Context context = itemView.getContext();
            SentimentSummary summary = report.summary;

            topicName.setText(report.topic.name);
            topicSubtitle.setText(Formatters.subtitle(context, report.topic));
            bindFollowButton(followHandler.isFollowed(report));
            followButton.setOnClickListener(v -> bindFollowButton(followHandler.toggleFollow(report)));

            int color = Formatters.sentimentColor(context, summary.score);
            sentimentIndex.setText(String.valueOf(summary.index()));
            sentimentIndex.setTextColor(color);
            sentimentLabel.setText(Formatters.label(context, summary.label, report.topic));
            sentimentLabel.setTextColor(color);

            setWeight(barPositive, summary.positiveCount);
            setWeight(barNeutral, summary.neutralCount);
            setWeight(barNegative, summary.negativeCount);
            sentimentCounts.setText(context.getString(R.string.sentiment_counts,
                    summary.positiveCount, Formatters.positiveWord(context, report.topic),
                    summary.neutralCount,
                    summary.negativeCount, Formatters.negativeWord(context, report.topic)));
            sentimentMeta.setText(context.getString(R.string.sentiment_meta,
                    Formatters.confidence(context, summary.confidence), summary.totalCount(),
                    Formatters.relativeTime(report.fetchedAtMillis)));

            bindMarket(context, report.market);
            bindTrend(context, summary);
            bindSources(context, report);
            postsHeader.setText(report.posts.isEmpty() ? R.string.no_posts : R.string.posts_header);
        }

        private void bindFollowButton(boolean followed) {
            Context context = itemView.getContext();
            followButton.setText(followed ? R.string.following : R.string.follow);
            followButton.setIcon(ContextCompat.getDrawable(context,
                    followed ? R.drawable.ic_baseline_star : R.drawable.ic_star_border));
        }

        private void bindMarket(Context context, MarketSnapshot market) {
            if (market == null || market.closes.length < 2) {
                priceSection.setVisibility(View.GONE);
                return;
            }
            priceSection.setVisibility(View.VISIBLE);
            price.setText(Formatters.price(market.price, market.currency));
            priceChange.setText(Formatters.percentChange(market.changePercent));
            priceChange.setTextColor(Formatters.sentimentColor(context, market.changePercent));

            float[] values = new float[market.closes.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = (float) market.closes[i];
            }
            priceChart.setLineColor(Formatters.sentimentColor(context, values[values.length - 1] - values[0]));
            priceChart.setAdapter(new ValuesSparkAdapter(values, null));
        }

        private void bindTrend(Context context, SentimentSummary summary) {
            if (summary.trend.size() < 2) {
                trendSection.setVisibility(View.GONE);
                return;
            }
            trendSection.setVisibility(View.VISIBLE);
            float[] values = new float[summary.trend.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = (float) summary.trend.get(i).mean;
            }
            trendChart.setLineColor(Formatters.sentimentColor(context, summary.score));
            trendChart.setBaseLineColor(MaterialColors.getColor(trendChart, com.google.android.material.R.attr.colorOutlineVariant));
            // The neutral line keeps the scale honest when every day leans the same way.
            trendChart.setAdapter(new ValuesSparkAdapter(values, 0f));
        }

        private void bindSources(Context context, TopicReport report) {
            sourceRows.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);
            for (Map.Entry<Source, SentimentSummary.SourceBreakdown> entry : report.summary.bySource.entrySet()) {
                SentimentSummary.SourceBreakdown breakdown = entry.getValue();
                View row = inflater.inflate(R.layout.item_source_row, sourceRows, false);
                ((TextView) row.findViewById(R.id.sourceName)).setText(entry.getKey().displayName);
                ((TextView) row.findViewById(R.id.sourceCount)).setText(context.getString(R.string.source_row, breakdown.count));
                TextView score = row.findViewById(R.id.sourceScore);
                score.setText(Formatters.score(breakdown.mean));
                score.setTextColor(Formatters.sentimentColor(context, breakdown.mean));
                sourceRows.addView(row);
            }

            List<String> failures = new ArrayList<>();
            for (Map.Entry<Source, String> entry : report.failedSources.entrySet()) {
                failures.add(context.getString(R.string.source_failed, entry.getKey().displayName, entry.getValue()));
            }
            if (report.marketError != null) {
                failures.add(context.getString(R.string.source_failed, context.getString(R.string.price_source), report.marketError));
            }
            failedSources.setVisibility(failures.isEmpty() ? View.GONE : View.VISIBLE);
            failedSources.setText(String.join("\n", failures));
        }

        private static void setWeight(View view, int weight) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            params.weight = weight;
            view.setLayoutParams(params);
            view.setVisibility(weight > 0 ? View.VISIBLE : View.GONE);
        }
    }

    static class PostHolder extends RecyclerView.ViewHolder {
        private final TextView sourceBadge;
        private final TextView author;
        private final TextView time;
        private final TextView text;
        private final TextView sentimentBadge;
        private final TextView authorTag;
        private final TextView engagement;

        PostHolder(View view) {
            super(view);
            sourceBadge = view.findViewById(R.id.sourceBadge);
            author = view.findViewById(R.id.author);
            time = view.findViewById(R.id.time);
            text = view.findViewById(R.id.postText);
            sentimentBadge = view.findViewById(R.id.sentimentBadge);
            authorTag = view.findViewById(R.id.authorTag);
            engagement = view.findViewById(R.id.engagement);
        }

        void bind(TopicReport report, Post post) {
            Context context = itemView.getContext();
            sourceBadge.setText(post.source.displayName);
            Formatters.tintBadge(sourceBadge, Formatters.sourceColor(context, post.source));
            author.setText(post.author);
            time.setText(Formatters.relativeTime(post.createdAtMillis));
            // Social posts often pad lines with blank space; keep cards compact.
            text.setText(post.text.replaceAll("\\n\\s*\\n+", "\n"));

            sentimentBadge.setText(Formatters.postLabel(context, post.sentiment, report.topic)
                    + " " + Formatters.score(post.sentiment));
            Formatters.tintBadge(sentimentBadge, Formatters.sentimentColor(context, post.sentiment));

            if (post.authorLabel != 0) {
                authorTag.setVisibility(View.VISIBLE);
                authorTag.setText(context.getString(R.string.author_tagged,
                        context.getString(post.authorLabel > 0 ? R.string.label_bullish : R.string.label_bearish)));
            } else {
                authorTag.setVisibility(View.GONE);
            }

            List<String> stats = new ArrayList<>();
            if (post.likes > 0) {
                stats.add("♥ " + post.likes);
            }
            if (post.replies > 0) {
                stats.add("↩ " + post.replies);
            }
            if (post.reposts > 0) {
                stats.add("⟳ " + post.reposts);
            }
            engagement.setText(String.join("  ", stats));

            itemView.setOnClickListener(post.url == null ? null : v -> open(context, post.url));
            itemView.setClickable(post.url != null);
        }

        private static void open(Context context, String url) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (ActivityNotFoundException ignored) {
                // No browser installed.
            }
        }
    }
}
