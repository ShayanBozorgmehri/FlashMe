package com.boredofnothing.flashcard.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.boredofnothing.flashcard.databinding.ListViewItemBinding;

import java.util.ArrayList;
import java.util.List;

public class ListViewAdapter extends BaseAdapter implements Filterable {
    private List<ListViewItem> itemList;
    private List<ListViewItem> itemListFull;
    private LayoutInflater inflater;

    public ListViewAdapter(List<ListViewItem> itemList) {
        this.itemList = itemList;
        itemListFull = new ArrayList<>(itemList);
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public ListViewItem getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<ListViewItem> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(itemListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (ListViewItem item : itemListFull) {
                    if (item.getText1().toLowerCase().contains(filterPattern)
                        || item.getText2().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            itemList = (List) results.values;
            notifyDataSetChanged();
        }
    };

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {

        if (inflater == null) {
            inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        ListViewItemBinding listViewItemBinding = ListViewItemBinding.inflate(inflater);
        listViewItemBinding.englishTextView.setText((itemList.get(position).getText1()));
        listViewItemBinding.swedishTextView.setText((itemList.get(position).getText2()));

        // tag will be used to fetch the position of the document
        listViewItemBinding.englishTextView.setTag(position);

        return listViewItemBinding.getRoot();
    }
}