package com.example.shareinfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class GetTwitterData {
    private static String search(String searchString) throws IOException, URISyntaxException {
        String searchResponse = null;
        String query = "from:TwitterDev&tweet.fields=created_at&expansions=author_id&user.fields=created_at";
        String url = "https://api.twitter.com/2/tweets/search/recent?query=" + query;
        URL twitterStreamsUrl = new URL(url);
        HttpURLConnection twitterSearchConnection = (HttpURLConnection) twitterStreamsUrl.openConnection();
        twitterSearchConnection.setRequestProperty("Authorization", "AAAAAAAAAAAAAAAAAAAAANDdQgEAAAAAEZxD1p%2BpxRdfzYQBYAFnCfxgPhU%3D9uWLk6CFLfuZuFYXm4Bdzbd0IbOKFLclXueGwTEPPyT9N520iE");
        twitterSearchConnection.setRequestMethod("GET");
        InputStream inputStream = twitterSearchConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String twitterSearchData = bufferedReader.readLine();
        System.out.println(twitterSearchData);
        return twitterSearchData;
    }

}
