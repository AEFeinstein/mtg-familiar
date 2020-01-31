/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.model.Comparison;

public class ComparisonSpinner extends androidx.appcompat.widget.AppCompatSpinner {

    public ComparisonSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        ComparisonAdapter adapter = new ComparisonAdapter();
        this.setAdapter(adapter);
    }

    private class ComparisonAdapter extends ArrayAdapter<Comparison> {
        ComparisonAdapter() {
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
