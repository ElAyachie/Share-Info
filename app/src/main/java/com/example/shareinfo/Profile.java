package com.example.shareinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Profile#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Profile extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    Spinner stockSymbolsSpin;
    ArrayAdapter<String> stockSymbolsListAdapter;
    static ListView stockFavoritesView;
    static DeleteStockCustomAdapter deleteStockAdapter;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Profile() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Profile.
     */
    // TODO: Rename and change types and number of parameters
    public static Profile newInstance(String param1, String param2) {
        Profile fragment = new Profile();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View mainView = inflater.inflate(R.layout.fragment_profile, container, false);
        Context context = getContext();

        // Set up the list view.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String favoriteStocksSymbols = preferences.getString("favoriteStocksSymbols", "");
        String favoriteStocksNames = preferences.getString("favoriteStocksNames", "");
        stockFavoritesView = mainView.findViewById(R.id.stockFavoritesView);
        List<String> trendingSymbolsList = Arrays.asList(favoriteStocksSymbols.split(","));
        List<String> trendingNamesList = Arrays.asList(favoriteStocksNames.split(","));
        // Set the adapters.
        deleteStockAdapter = new DeleteStockCustomAdapter(context, trendingSymbolsList, trendingNamesList);
        stockFavoritesView.setAdapter(deleteStockAdapter);

        // Inflate the layout for this fragment
        return mainView;
    }

    public static void updateListView(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String favoriteStocksSymbols = preferences.getString("favoriteStocksSymbols", "");
        String favoriteStocksNames = preferences.getString("favoriteStocksNames", "");
        List<String> trendingSymbolsList = Arrays.asList(favoriteStocksSymbols.split(","));
        List<String> trendingNamesList = Arrays.asList(favoriteStocksNames.split(","));
        // Set the adapters.
        deleteStockAdapter = new DeleteStockCustomAdapter(context, trendingSymbolsList, trendingNamesList);
        stockFavoritesView.setAdapter(deleteStockAdapter);
        deleteStockAdapter.notifyDataSetChanged();
    }
}