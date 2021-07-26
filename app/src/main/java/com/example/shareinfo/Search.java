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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Search#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Search extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    Spinner stockSymbolsSpin;
    ArrayAdapter<String> stockSymbolsListAdapter;
    ListView stockTrendingView;
    AddStockCustomAdapter addStockAdapter;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Search() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment search.
     */
    // TODO: Rename and change types and number of parameters
    public static Search newInstance(String param1, String param2) {
        Search fragment = new Search();
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

        View mainView = inflater.inflate(R.layout.fragment_search, container, false);
        Context context = getContext();

        // Set up the stock symbol spinner.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String trendingSymbols = preferences.getString("stockTrendingSymbols", "");
        String trendingNames = preferences.getString("stockTrendingNames", "");
        stockTrendingView = mainView.findViewById(R.id.stockFavoritesView);
        List<String> trendingSymbolsList = Arrays.asList(trendingSymbols.split(","));
        List<String> trendingNamesList = Arrays.asList(trendingNames.split(","));
        // Set the adapters.
        addStockAdapter = new AddStockCustomAdapter(context, trendingSymbolsList, trendingNamesList);
        stockTrendingView.setAdapter(addStockAdapter);

        // Inflate the layout for this fragment
        return mainView;
    }
}