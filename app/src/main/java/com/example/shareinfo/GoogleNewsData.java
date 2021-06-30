package com.example.shareinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.MODE_PRIVATE;

public class GoogleNewsData {
    public static void GetGoogleNewsDataForStock(Context context, String searchString) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String googleNewsData = "";
                try {
                    // GET Google News information from API
                    String rules = "&country=US&lang=en";
                    String url = "https://google-news1.p.rapidapi.com/search?q=" + searchString + rules;
                    URL googleNewsUrl = new URL(url);
                    HttpURLConnection googleNewsConnection = (HttpURLConnection) googleNewsUrl.openConnection();
                    googleNewsConnection.setRequestProperty("x-rapidapi-host", "google-news1.p.rapidapi.com");
                    googleNewsConnection.setRequestProperty("x-rapidapi-key", "132a0cb470mshf7b87dbc92557f1p1cf34bjsna1ca8152da1a");
                    googleNewsConnection.setRequestMethod("GET");
                    InputStream inputStream = googleNewsConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    googleNewsData = bufferedReader.readLine();
                    Log.d("Google News JSON Data", googleNewsData);

                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Save the JSON information to the file storage
                try {
                    // Saving Google News data in a JSON file
                    String filePath = context.getFilesDir().getAbsolutePath();
                    String googleNewsJSONFileName = "GoogleNewsData_" + searchString + ".json";
                    File googleNewsDataFile = new File(filePath, googleNewsJSONFileName);
                    FileOutputStream stream = new FileOutputStream(googleNewsDataFile);
                    stream.write(googleNewsData.getBytes());
                    stream.write("\n".getBytes());
                    stream.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // UI Thread work here
                        // Update the social feed panel to show the stock information.
                    }
                });
            }
        });
    }
}
