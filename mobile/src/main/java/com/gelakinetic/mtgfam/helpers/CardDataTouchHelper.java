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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * Used for the callback for the ItemTouchHelper when swiping items.
 */
public class CardDataTouchHelper extends SimpleCallback {

    private final CardDataAdapter mAdapter;

    /**
     * Call the super constructor and save the mAdapter for this callback
     *
     * @param adapter The adapter for this helper, will be called if an item is swiped
     */
    public CardDataTouchHelper(final CardDataAdapter adapter) {
        super(0, ItemTouchHelper.LEFT);
        this.mAdapter = adapter;
    }

    /**
     * Do nothing when an item is moved
     *
     * @param recyclerView The RecyclerView to which ItemTouchHelper is attached to.
     * @param viewHolder   The ViewHolder which is being dragged by the user.
     * @param target       The ViewHolder over which the currently active item is being dragged.
     * @return false, because we do nothing
     */
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder, @NonNull ViewHolder target) {
        return false;
    }

    /**
     * Called when an item is swiped. Swiped items should be removed
     *
     * @param viewHolder The ViewHolder which has been swiped by the user.
     * @param direction  The direction to which the ViewHolder is swiped. It will be LEFT, the only
     *                   direction registered by the constructor
     */
    @Override
    public void onSwiped(@NonNull ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (RecyclerView.NO_POSITION != position) {
            mAdapter.swipeRemoveItem(position);
        }
    }

    /**
     * Return the swipeable directions for a given ViewHolder. May be 0 if the ViewHolder isn't
     * swipeable
     *
     * @param parent The RecyclerView to which the ItemTouchHelper is attached to.
     * @param holder The RecyclerView for which the swipe direction is queried.
     * @return 0 if the view is not swipeable, otherwise super
     */
    @Override
    public int getSwipeDirs(@NonNull RecyclerView parent, @NonNull ViewHolder holder) {
        if (holder instanceof CardDataViewHolder) {
            if (!((CardDataViewHolder) holder).getIsSwipeable()) {
                return 0;
            }
        }
        return super.getSwipeDirs(parent, holder);
    }
}
