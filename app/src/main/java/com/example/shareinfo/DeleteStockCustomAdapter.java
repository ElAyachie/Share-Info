package com.example.shareinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;


import java.util.List;

public class DeleteStockCustomAdapter extends BaseAdapter {
    private Context context;
    private List<String> stockSymbolsList;
    private List<String> stockNamesList;

    DeleteStockCustomAdapter(Context context, List<String> stockSymbols, List<String> stockNames) {
        this.context = context;
        this.stockSymbolsList = stockSymbols;
        this.stockNamesList = stockNames;
    }

    @Override
    public int getCount() {
        return stockSymbolsList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.stockSymbolsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.stockSymbolsList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        @SuppressLint("ViewHolder") View view = View.inflate(this.context, R.layout.unfollow_stock_list_view, null);
        TextView stockSymbolText = view.findViewById(R.id.stockSymbolText);
        TextView stockNameText = view.findViewById(R.id.stockNameText);

        String stockSymbol = stockSymbolsList.get(position);
        String stockName = stockNamesList.get(position);
        stockSymbolText.setText(stockSymbol);
        stockNameText.setText(stockName);

        ImageButton removeStockButton = view.findViewById(R.id.removeStockButton);
        removeStockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Setup.RemoveFromFavorites(context, stockSymbol, stockName);
                Profile.updateListView(context);
            }
        });
        return view;
    }
}
