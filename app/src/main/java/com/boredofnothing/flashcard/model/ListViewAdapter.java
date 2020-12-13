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
    private List<ListViewItem> originalList;
    private List<ListViewItem> filteredList;
    private LayoutInflater inflater;

    public ListViewAdapter(List<ListViewItem> originalList) {
        this.originalList = originalList;
        filteredList = new ArrayList<>(originalList);
    }

    @Override
    public int getCount() {
        return originalList.size();
    }

    @Override
    public ListViewItem getItem(int position) {
        return originalList.get(position);
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
                filteredList.addAll(ListViewAdapter.this.filteredList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (ListViewItem item : ListViewAdapter.this.filteredList) {
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
            originalList = (List) results.values;
            notifyDataSetChanged();
        }
    };

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {

        if (inflater == null) {
            inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        ListViewItemBinding listViewItemBinding = ListViewItemBinding.inflate(inflater);
        listViewItemBinding.englishTextView.setText((originalList.get(position).getText1()));
        listViewItemBinding.swedishTextView.setText((originalList.get(position).getText2()));

        // tag will be used to fetch the position of the document for on click
        String tag = originalList.get(position).getText1() + "_" + originalList.get(position).getText2();
        listViewItemBinding.englishTextView.setTag(tag);
        listViewItemBinding.swedishTextView.setTag(tag);

        return listViewItemBinding.getRoot();
    }
}