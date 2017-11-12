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

package com.gelakinetic.mtgfam.helpers;

/**
 * Created by Adam on 11/12/2017.
 */


import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * ViewHolder implementing the needed listeners and defining swipeable and its get/setters.
 */
public abstract class SelectableItemViewHolder
        extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {

    private boolean isSwipeable = true;

    public SelectableItemViewHolder(View view) {
        super(view);
    }

    boolean getIsSwipeable() {
        return isSwipeable;
    }

    public void setIsSwipeable(final boolean isSwipeable) {
        this.isSwipeable = isSwipeable;
    }

}