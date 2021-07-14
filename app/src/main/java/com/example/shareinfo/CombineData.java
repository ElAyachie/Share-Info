package com.example.shareinfo;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;

public class CombineData {
    public static void CombineAndSaveData(Context context, String stockSymbol) {
        try {
            // Creating the twitter json object from the json file.
            String twitterFilePath1 = context.getFilesDir().getAbsolutePath() + "/stock_information/TwitterSearchData_"+ stockSymbol +".json";
            FileInputStream twitterFile1 = new FileInputStream(twitterFilePath1);
            int twitterFileSize1 = twitterFile1.available();
            byte[] twitterRawData1 = new byte[twitterFileSize1];
            int result = twitterFile1.read(twitterRawData1);
            twitterFile1.close();
            JSONObject twitterJSONData1 = new JSONObject(new String(twitterRawData1, "UTF-8"));
            // Creating the twitter trending json object from the json file.
            String twitterFilePath2 = context.getFilesDir().getAbsolutePath() + "/stock_information/TwitterSearchData_"+ stockSymbol +"2.json";
            FileInputStream twitterFile2 = new FileInputStream(twitterFilePath2);
            int twitterFileSize2 = twitterFile2.available();
            byte[] twitterRawData2 = new byte[twitterFileSize2];
            result = twitterFile2.read(twitterRawData2);
            twitterFile2.close();
            JSONObject twitterJSONData2 = new JSONObject(new String(twitterRawData2, "UTF-8"));

            // Creating the google news json object from the json file.
            String googleNewsFilePath1 = context.getFilesDir().getAbsolutePath() + "/stock_information/GoogleNewsData_"+ stockSymbol +".json";
            FileInputStream googleNewsFile1 = new FileInputStream(googleNewsFilePath1);
            int googleNewsFileSize1 = googleNewsFile1.available();
            byte[] googleNewsRawData1 = new byte[googleNewsFileSize1];
            result = googleNewsFile1.read(googleNewsRawData1);
            googleNewsFile1.close();
            JSONObject googleNewsJSONData1 = new JSONObject(new String(googleNewsRawData1, "UTF-8"));
            // Creating the google news json object from the json file.
            String googleNewsFilePath2 = context.getFilesDir().getAbsolutePath() + "/stock_information/GoogleNewsData_"+ stockSymbol +"2.json";
            FileInputStream googleNewsFile2 = new FileInputStream(googleNewsFilePath2);
            int googleNewsFileSize2 = googleNewsFile2.available();
            byte[] googleNewsRawData2 = new byte[googleNewsFileSize2];
            result = googleNewsFile2.read(googleNewsRawData2);
            googleNewsFile2.close();
            JSONObject googleNewsJSONData2 = new JSONObject(new String(googleNewsRawData2, "UTF-8"));

            // Creating the reddit json object from the json file.
            String redditFilePath1 = context.getFilesDir().getAbsolutePath() + "/stock_information/RedditSearchData_"+ stockSymbol +".json";
            FileInputStream redditSearchFile1 = new FileInputStream(redditFilePath1);
            int redditSearchFileSize1 = redditSearchFile1.available();
            byte[] redditSearchRawData1 = new byte[redditSearchFileSize1];
            result = redditSearchFile1.read(redditSearchRawData1);
            redditSearchFile1.close();
            JSONObject redditSearchJSONData1 = new JSONObject(new String(redditSearchRawData1, "UTF-8"));
            // Creating the reddit json object from the json file.
            String redditFilePath2 = context.getFilesDir().getAbsolutePath() + "/stock_information/RedditSearchData_"+ stockSymbol +"2.json";
            FileInputStream redditSearchFile2 = new FileInputStream(redditFilePath2);
            int redditSearchFileSize2 = redditSearchFile2.available();
            byte[] redditSearchRawData2 = new byte[redditSearchFileSize2];
            result = redditSearchFile2.read(redditSearchRawData2);
            redditSearchFile2.close();
            JSONObject redditSearchJSONData2 = new JSONObject(new String(redditSearchRawData2, "UTF-8"));

            // Creating the json object for combined news and social media.
            JSONObject combinedStockData = new JSONObject();
            JSONArray stockDataArray = new JSONArray();
            int id = 0;

            // Add all the Twitter information to the combined json file.
            JSONArray twitterJSONDataArray1 = twitterJSONData1.getJSONArray("data");
            JSONArray twitterJSONUserDataArray = twitterJSONData1.getJSONObject("includes").getJSONArray("users");
            for (int i = 0; i < twitterJSONDataArray1.length(); i++) {
                JSONObject data = new JSONObject();
                JSONObject jsonObject = twitterJSONDataArray1.getJSONObject(i);
                // Make sure that the tweet is not a retweet and is in english.
                if (!jsonObject.getString("text").contains("RT") && jsonObject.getString("lang").equals("en")) {
                    JSONObject userJSONObject = twitterJSONUserDataArray.getJSONObject(i);
                    JSONObject metricsJSON = jsonObject.getJSONObject("public_metrics");
                    data.put("id", id);
                    data.put("media_source", "twitter");
                    data.put("user_or_network", userJSONObject.getString("username"));
                    data.put("content", jsonObject.getString("text"));
                    data.put("interactions", metricsJSON.getInt("like_count"));
                    data.put("date_created", jsonObject.getString("created_at"));
                    data.put("link", "NA");
                    stockDataArray.put(data);
                    id += 1;
                }
            }

            JSONArray twitterJSONDataArray2 = twitterJSONData2.getJSONArray("data");
            twitterJSONUserDataArray = twitterJSONData2.getJSONObject("includes").getJSONArray("users");
            for (int i = 0; i < twitterJSONDataArray2.length(); i++) {
                JSONObject data = new JSONObject();
                JSONObject jsonObject = twitterJSONDataArray2.getJSONObject(i);
                // Make sure that the tweet is not a retweet and is in english.
                if (!jsonObject.getString("text").contains("RT") && jsonObject.getString("lang").equals("en")) {
                    JSONObject userJSONObject = twitterJSONUserDataArray.getJSONObject(i);
                    JSONObject metricsJSON = jsonObject.getJSONObject("public_metrics");
                    data.put("id", id);
                    data.put("media_source", "twitter");
                    data.put("user_or_network", userJSONObject.getString("username"));
                    data.put("content", jsonObject.getString("text"));
                    data.put("interactions", metricsJSON.getInt("like_count"));
                    data.put("date_created", jsonObject.getString("created_at"));
                    data.put("link", "NA");
                    stockDataArray.put(data);
                    id += 1;
                }
            }

            // Add all the Reddit information to the combined json array.
            JSONArray redditJSONDataArray1 = redditSearchJSONData1.getJSONArray("data");

            for (int i = 0; i < redditJSONDataArray1.length(); i++) {
                JSONObject data = new JSONObject();
                JSONObject jsonObject = redditJSONDataArray1.getJSONObject(i);
                // Make sure that the comment has some interactions
                if (jsonObject.has("sentiment") && !(jsonObject.getDouble("sentiment") == 0)) {
                    data.put("id", id);
                    data.put("media_source", "reddit");
                    data.put("user_or_network", jsonObject.getString("id"));
                    data.put("content", jsonObject.getString("body"));
                    data.put("interactions", jsonObject.getDouble("sentiment"));
                    data.put("date_created", jsonObject.getString("created_utc"));
                    data.put("link", "NA");
                    stockDataArray.put(data);
                    id += 1;
                }
            }

            // Add all the Reddit information to the combined json array.
            JSONArray redditJSONDataArray2 = redditSearchJSONData2.getJSONArray("data");

            for (int i = 0; i < redditJSONDataArray2.length(); i++) {
                JSONObject data = new JSONObject();
                JSONObject jsonObject = redditJSONDataArray2.getJSONObject(i);
                // Make sure that the comment has some interactions
                if (jsonObject.has("sentiment") && !(jsonObject.getDouble("sentiment") == 0)) {
                    data.put("id", id);
                    data.put("media_source", "reddit");
                    data.put("user_or_network", jsonObject.getString("id"));
                    data.put("content", jsonObject.getString("body"));
                    data.put("interactions", jsonObject.getDouble("sentiment"));
                    data.put("date_created", jsonObject.getString("created_utc"));
                    data.put("link", "permalink");
                    stockDataArray.put(data);
                    id += 1;
                }
            }


            // Add all the Google News information to the combined json file.
            JSONArray googleNewsJSONDataArray1 = googleNewsJSONData1.getJSONArray("articles");

            for (int i = 0; i < googleNewsJSONDataArray1.length(); i++) {
                JSONObject data = new JSONObject();
                JSONObject jsonObject = googleNewsJSONDataArray1.getJSONObject(i);
                JSONObject sourceJSON = jsonObject.getJSONObject("source");
                data.put("id", id);
                data.put("media_source", "Google News");
                data.put("user_or_network", sourceJSON.getString("title"));
                data.put("content", jsonObject.getString("title"));
                data.put("interactions", "NA");
                data.put("date_created", jsonObject.getString("published_date"));
                data.put("link", jsonObject.getString("link"));
                stockDataArray.put(data);
                id += 1;
            }

            // Add all the Google News information to the combined json file.
            JSONArray googleNewsJSONDataArray2 = googleNewsJSONData2.getJSONArray("articles");

            for (int i = 0; i < googleNewsJSONDataArray2.length(); i++) {
                JSONObject data = new JSONObject();
                JSONObject jsonObject = googleNewsJSONDataArray2.getJSONObject(i);
                JSONObject sourceJSON = jsonObject.getJSONObject("source");
                data.put("id", id);
                data.put("media_source", "Google News");
                data.put("user_or_network", sourceJSON.getString("title"));
                data.put("content", jsonObject.getString("title"));
                data.put("interactions", "NA");
                data.put("date_created", jsonObject.getString("published_date"));
                data.put("link", jsonObject.getString("link"));
                stockDataArray.put(data);
                id += 1;
            }

            combinedStockData.put("data", stockDataArray);
            String filePath = context.getFilesDir().getAbsolutePath() + "/stock_information";
            String combinedDataFileName = "CombinedData_"+ stockSymbol +".json";
            File combinedDataFile = new File(filePath, combinedDataFileName);
            FileOutputStream stream = new FileOutputStream(combinedDataFile);
            stream.write(combinedStockData.toString().getBytes());
            stream.write("\n".getBytes());
            stream.close();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
}
