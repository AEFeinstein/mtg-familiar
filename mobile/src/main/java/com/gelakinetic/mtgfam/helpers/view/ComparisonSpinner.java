package com.gelakinetic.mtgfam.helpers.view;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.model.Comparison;

public class ComparisonSpinner extends android.support.v7.widget.AppCompatSpinner {

    public ComparisonSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        ComparisonAdapter adapter = new ComparisonAdapter();
        this.setAdapter(adapter);
    }

    private class ComparisonAdapter extends ArrayAdapter<Comparison> {
        public ComparisonAdapter() {
            super(ComparisonSpinner.this.getContext(), R.layout.list_item_1, Comparison.values());
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final TextView view = (TextView) super.getView(position, convertView, parent);
            final Comparison comparison = this.getItem(position);
            assert comparison != null;
            String value = comparison.getShortDescription();
            view.setText(value);
            return view;
        }

        @NonNull
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            final Comparison comparison = this.getItem(position);
            assert comparison != null;
            String value = comparison.getShortDescription() + " " + ComparisonSpinner.this.
                    getContext().getString(comparison.getLongDescriptionRes());
            view.setText(value);
            return view;
        }
    }
}
