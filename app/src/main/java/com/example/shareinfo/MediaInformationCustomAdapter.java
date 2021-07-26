package com.example.shareinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.example.shareinfo.R;

import java.util.ArrayList;

public class MediaInformationCustomAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> mediaList;
    private ArrayList<String> mediaContentList;
    private ArrayList<Integer> interactionsList;
    private ArrayList<Double> sentimentList;
    private ArrayList<String> userOrNetworkList;
    private ArrayList<String> dateList;
    private ArrayList<String> urlList;


    MediaInformationCustomAdapter(Context context, ArrayList<String> media, ArrayList<String> mediaContent, ArrayList<Integer> interactions, ArrayList<Double> sentiments, ArrayList<String> userOrNetwork, ArrayList<String> date, ArrayList<String> url) {
        this.context = context;
        this.mediaList = media;
        this.mediaContentList = mediaContent;
        this.interactionsList = interactions;
        this.sentimentList = sentiments;
        this.userOrNetworkList = userOrNetwork;
        this.dateList = date;
        this.urlList = url;
    }

    @Override
    public int getCount() {
        return mediaList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mediaList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.mediaList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        @SuppressLint("ViewHolder") View view = View.inflate(this.context, R.layout.media_list_view, null);
        TextView mediaText = view.findViewById(R.id.mediaSourceText);
        TextView mediaContentText = view.findViewById(R.id.mediaContentText);
        TextView interactionsText = view.findViewById(R.id.interactionsText);
        TextView sentimentText = view.findViewById(R.id.sentimentText);
        TextView userOrNetworkText = view.findViewById(R.id.userOrNetworkText);
        TextView dateText = view.findViewById(R.id.dateText);
        TextView urlText = view.findViewById(R.id.urlText);

        String mediaString = "Media Source: " + mediaList.get(position);
        String mediaContentString = "Media Content: " + mediaContentList.get(position);
        String interactionsString = "Interactions: " + interactionsList.get(position);
        String sentimentString = "Sentiment: " + sentimentList.get(position).toString();
        String userOrNetworkString = "User or network: " + userOrNetworkList.get(position);
        String dateString = "Date: " + dateList.get(position);
        String urlString = "Link: " + urlList.get(position);

        mediaText.setText(mediaString);
        mediaContentText.setText(mediaContentString);
        interactionsText.setText(interactionsString);
        sentimentText.setText(sentimentString);
        userOrNetworkText.setText(userOrNetworkString);
        dateText.setText(dateString);
        urlText.setText(urlString);

        return view;
    }
}
