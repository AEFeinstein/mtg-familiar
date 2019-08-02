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

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;

import java.util.Objects;

public class CompletionView extends ATokenTextView {

    public CompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAllowDuplicates = false;
        this.preventFreeFormText(true);
        this.setTokenClickStyle(TokenClickStyle.Delete);
        this.performBestGuess(true);
        this.allowCollapse(true);
    }

    @Override
    protected View getViewForObject(String set) {
        LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        TextView view = (TextView) Objects.requireNonNull(l).inflate(R.layout.set_token, (ViewGroup) getParent(), false);
        view.setText(set);

        return view;
    }
}
