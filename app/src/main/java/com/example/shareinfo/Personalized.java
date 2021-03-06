package com.example.shareinfo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.robinhood.spark.SparkView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Personalized#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Personalized extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    ArrayList<String> stockSymbolsList;
    Spinner stockSymbolsSpin;
    ArrayAdapter<String> stockSymbolsListAdapter;
    ListView stockMediaInformationView;
    MediaInformationCustomAdapter stockMediaInformationAdapter;
    int waitTime;


    public Personalized() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Personalized.
     */
    // TODO: Rename and change types and number of parameters
    public static Personalized newInstance(String param1, String param2) {
        Personalized fragment = new Personalized();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        // Inflate the layout for this fragment

        View mainView = inflater.inflate(R.layout.fragment_personalized, container, false);
        Context context = getContext();
        mainView.findViewById(R.id.loadingPanel).setVisibility(View.GONE);

        // Set up the preferences object.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Length of time to wait for API to get information.
        waitTime = 7000;

        // Checking if the fragment was already made (this is a work around).
        // Checks if the fragment has been already made.
        // If the fragment has not been made we have to wait for the information to come in from the API, store it to local storage, then show it to the user.
        // Else the fragment has already been made we don't need to wait for the new data we can just get the information from local storage.
        if (!preferences.getBoolean("personalizedExists", false)) {
            mainView.findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
            //Setup.GetAllStockDataForFavorites(context);
            // Wait for all the information from the API to be saved and then combine all the values into one json file.
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stockMediaInformationView = mainView.findViewById(R.id.mediaListView);
                    String favoriteStocksSymbols = preferences.getString("favoriteStocksSymbols", "");
                    String favoriteStocksNames = preferences.getString("favoriteStocksNames", "");
                    // A way to count the amount of stock symbols in the list.
                    int numberOfElements = (favoriteStocksSymbols.length() - favoriteStocksSymbols.replace(",", "").length()) + 1;
                    List<String> favoriteStocksSymbolsList = Arrays.asList(favoriteStocksSymbols.split(",")).subList(0, numberOfElements);
                    List<String> favoriteStocksNamesList = Arrays.asList(favoriteStocksNames.split(",")).subList(0, numberOfElements);
                    stockSymbolsSpin = mainView.findViewById(R.id.stockSpinner);
                    stockSymbolsList = new ArrayList<>(favoriteStocksSymbolsList);
                    stockSymbolsListAdapter = new ArrayAdapter<>(context, R.layout.support_simple_spinner_dropdown_item, favoriteStocksNamesList);
                    stockSymbolsSpin = mainView.findViewById(R.id.stockSpinner);
                    stockSymbolsSpin.setAdapter(stockSymbolsListAdapter);
                    stockSymbolsSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            ArrayList<String> media = new ArrayList<>();
                            ArrayList<String> userOrNetwork = new ArrayList<>();
                            ArrayList<String> content = new ArrayList<>();
                            ArrayList<Integer> interactions = new ArrayList<>();
                            ArrayList<String> date = new ArrayList<>();
                            ArrayList<String> url = new ArrayList<>();
                            ArrayList<Double> sentiments = new ArrayList<>();

                            if ((stockSymbolsList.size() > 0)) {
                                JSONObject stockMediaJson = CombineData.GetMediaInformationForStockFavorites(context, favoriteStocksSymbolsList.get(position));
                                try {
                                    JSONArray dataObjectArray = stockMediaJson.getJSONArray("data");
                                    List<JSONObject> jsonArrayAsList = new ArrayList<JSONObject>();
                                    for (int i = 0; i < dataObjectArray.length(); i++)
                                        jsonArrayAsList.add(dataObjectArray.getJSONObject(i));

                                    // Sort the json objects by interactions.
                                    sortByInteractions(jsonArrayAsList);
                                    Collections.reverse(jsonArrayAsList);

                                    for (int i = 0; i < jsonArrayAsList.size(); i++) {
                                        JSONObject dataObject = jsonArrayAsList.get(i);
                                        media.add(dataObject.getString("media_source"));
                                        userOrNetwork.add(dataObject.getString("user_or_network"));
                                        content.add(dataObject.getString("content"));
                                        interactions.add(dataObject.getInt("interactions"));
                                        sentiments.add(dataObject.getDouble("sentiment"));
                                        date.add(dataObject.getString("date_created"));
                                        url.add(dataObject.getString("link"));
                                    }
                                    // Set the adapters.
                                    stockMediaInformationAdapter = new MediaInformationCustomAdapter(context, media, content, interactions, sentiments, userOrNetwork, date, url);
                                    stockMediaInformationView.setAdapter(stockMediaInformationAdapter);
                                    SparkView sparkView = mainView.findViewById(R.id.sparkview);

                                    DataAdapter adapter = new DataAdapter();
                                    sparkView.setAdapter(adapter);
                                    sparkView.setLineColor(Color.GREEN);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                        }

                    });


                    stockMediaInformationView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            TextView textView = view.findViewById(R.id.mediaContentText);
                            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                            View alertView = View.inflate(context, R.layout.content_popup, null);
                            final TextView mediaContentTextView = alertView.findViewById(R.id.mediaContentTextView);
                            String contentText = textView.getText().toString();
                            mediaContentTextView.setText(contentText);
                            mediaContentTextView.setMovementMethod(new ScrollingMovementMethod());
                            builder.setView(alertView);
                            final AlertDialog optionDialog = builder.show();
                        }
                    });

                    stockMediaInformationView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                            TextView textView = view.findViewById(R.id.urlText);
                            String urlString = textView.getText().toString();
                            urlString = urlString.replace("Link: ", "");
                            if (!urlString.equals("NA")) {
                                Intent httpIntent = new Intent(Intent.ACTION_VIEW);
                                httpIntent.setData(Uri.parse(urlString));

                                startActivity(httpIntent);
                            }
                            return true;
                        }
                    });
                    final SwipeRefreshLayout pullToRefresh = mainView.findViewById(R.id.pullToRefresh);

                    // Setup the scroll down on screen to refresh the information.
                    pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            SharedPreferences.Editor prefEditor = preferences.edit();
                            prefEditor.putBoolean("personalizedExists", false);
                            prefEditor.apply();
                            mainView.findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                            MainActivity.RefreshPersonalized(getParentFragmentManager());
                            pullToRefresh.setRefreshing(false);
                        }
                    });

                    // Removing the loading icon from the screen.
                    mainView.findViewById(R.id.loadingPanel).setVisibility(View.GONE);

                    // Setup the graph.
                    SparkView sparkView = mainView.findViewById(R.id.sparkview);
                    DataAdapter adapter = new DataAdapter();
                    sparkView.setAdapter(adapter);
                    sparkView.setLineColor(Color.GREEN);
                }
            }, waitTime);
        }
        else {
            stockMediaInformationView = mainView.findViewById(R.id.mediaListView);
            String favoriteStocksSymbols = preferences.getString("favoriteStocksSymbols", "");
            String favoriteStocksNames = preferences.getString("favoriteStocksNames", "");
            // A way to count the amount of stock symbols in the list.
            int numberOfElements = (favoriteStocksSymbols.length() - favoriteStocksSymbols.replace(",", "").length()) + 1;
            List<String> favoriteStocksSymbolsList = Arrays.asList(favoriteStocksSymbols.split(",")).subList(0, numberOfElements);
            List<String> favoriteStocksNamesList = Arrays.asList(favoriteStocksNames.split(",")).subList(0, numberOfElements);
            stockSymbolsSpin = mainView.findViewById(R.id.stockSpinner);
            stockSymbolsList = new ArrayList<>(favoriteStocksSymbolsList);
            stockSymbolsListAdapter = new ArrayAdapter<>(context, R.layout.support_simple_spinner_dropdown_item, favoriteStocksNamesList);
            stockSymbolsSpin = mainView.findViewById(R.id.stockSpinner);
            stockSymbolsSpin.setAdapter(stockSymbolsListAdapter);
            stockSymbolsSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    assert context != null;
                    ArrayList<String> media = new ArrayList<>();
                    ArrayList<String> userOrNetwork = new ArrayList<>();
                    ArrayList<String> content = new ArrayList<>();
                    ArrayList<Integer> interactions = new ArrayList<>();
                    ArrayList<String> date = new ArrayList<>();
                    ArrayList<String> url = new ArrayList<>();
                    ArrayList<Double> sentiments = new ArrayList<>();

                    if (stockSymbolsList.size() > 1) {
                        JSONObject stockMediaJson = CombineData.GetMediaInformationForStockFavorites(context, favoriteStocksSymbolsList.get(position));
                        try {
                            JSONArray dataObjectArray = stockMediaJson.getJSONArray("data");
                            List<JSONObject> jsonArrayAsList = new ArrayList<JSONObject>();
                            for (int i = 0; i < dataObjectArray.length(); i++)
                                jsonArrayAsList.add(dataObjectArray.getJSONObject(i));

                            // Sort the json objects by interactions.
                            sortByInteractions(jsonArrayAsList);
                            Collections.reverse(jsonArrayAsList);

                            for (int i = 0; i < jsonArrayAsList.size(); i++) {
                                JSONObject dataObject = jsonArrayAsList.get(i);
                                media.add(dataObject.getString("media_source"));
                                userOrNetwork.add(dataObject.getString("user_or_network"));
                                content.add(dataObject.getString("content"));
                                interactions.add(dataObject.getInt("interactions"));
                                sentiments.add(dataObject.getDouble("sentiment"));
                                date.add(dataObject.getString("date_created"));
                                url.add(dataObject.getString("link"));
                            }
                            // Set the adapters.
                            stockMediaInformationAdapter = new MediaInformationCustomAdapter(context, media, content, interactions, sentiments, userOrNetwork, date, url);
                            stockMediaInformationView.setAdapter(stockMediaInformationAdapter);
                            SparkView sparkView = mainView.findViewById(R.id.sparkview);

                            DataAdapter adapter = new DataAdapter();
                            sparkView.setAdapter(adapter);
                            sparkView.setLineColor(Color.GREEN);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }

            });


            stockMediaInformationView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TextView textView = view.findViewById(R.id.mediaContentText);
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    View alertView = View.inflate(context, R.layout.content_popup, null);
                    final TextView mediaContentTextView = alertView.findViewById(R.id.mediaContentTextView);
                    String contentText = textView.getText().toString();
                    mediaContentTextView.setText(contentText);
                    mediaContentTextView.setMovementMethod(new ScrollingMovementMethod());
                    builder.setView(alertView);
                    final AlertDialog optionDialog = builder.show();
                }
            });

            stockMediaInformationView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    TextView textView = view.findViewById(R.id.urlText);
                    String urlString = textView.getText().toString();
                    urlString = urlString.replace("Link: ", "");
                    if (!urlString.equals("NA")) {
                        Intent httpIntent = new Intent(Intent.ACTION_VIEW);
                        httpIntent.setData(Uri.parse(urlString));

                        startActivity(httpIntent);
                    }
                    return true;
                }
            });

            // Setup the scroll down on screen to refresh the information.
            final SwipeRefreshLayout pullToRefresh = mainView.findViewById(R.id.pullToRefresh);

                    pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            SharedPreferences.Editor prefEditor = preferences.edit();
                            prefEditor.putBoolean("personalizedExists", false);
                            prefEditor.apply();
                            MainActivity.RefreshPersonalized(getParentFragmentManager());
                            pullToRefresh.setRefreshing(false);
                        }
                    });
            // Remove the loading icon.
            mainView.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            // Setup the graph.
            SparkView sparkView = mainView.findViewById(R.id.sparkview);
            DataAdapter adapter = new DataAdapter();
            sparkView.setAdapter(adapter);
            sparkView.setLineColor(Color.GREEN);
        }
        return mainView;
    }


    // Sorts media information by interactions.
    public static void sortByInteractions (List<JSONObject> jsonArray) {
        Collections.sort(jsonArray, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject jsonObjectA, JSONObject jsonObjectB) {
                int compare = 0;
                try {
                    int keyA = jsonObjectA.getInt("interactions");
                    int keyB = jsonObjectB.getInt("interactions");
                    compare = Integer.compare(keyA, keyB);
                }
                catch(JSONException e) {
                    e.printStackTrace();
                }
                return compare;
            }
        });
    }

    // Sorts media information by interactions.
    public static void sortBySentiment (List<JSONObject> jsonArray) {
        Collections.sort(jsonArray, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject jsonObjectA, JSONObject jsonObjectB) {
                int compare = 0;
                try {
                    double keyA = jsonObjectA.getDouble("sentiment");
                    double keyB = jsonObjectB.getDouble("sentiment");
                    compare = Double.compare(keyA, keyB);
                }
                catch(JSONException e) {
                    e.printStackTrace();
                }
                return compare;
            }
         
        });
    }
}