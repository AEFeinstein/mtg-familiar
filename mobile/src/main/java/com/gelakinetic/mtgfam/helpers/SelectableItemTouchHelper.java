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

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback;

/**
 * Used for the callback for the ItemTouchHelper when swiping items.
 */
public class SelectableItemTouchHelper extends SimpleCallback {

    private final SelectableItemAdapter adapter;

    public SelectableItemTouchHelper(final SelectableItemAdapter adapter, final int swipeDirs) {
        super(0, swipeDirs);
        this.adapter = adapter;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(ViewHolder viewHolder, int direction) {
        adapter.onItemDismissed(viewHolder.getAdapterPosition());
    }

    @Override
    public int getSwipeDirs(RecyclerView parent, ViewHolder holder) {
        if (holder instanceof SelectableItemViewHolder) {
            if (!((SelectableItemViewHolder) holder).getIsSwipeable()) {
                return 0;
            }
        }
        return super.getSwipeDirs(parent, holder);
    }

    @Override
    public void onChildDraw(
            Canvas canvas,
            RecyclerView recyclerView,
            ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive) {

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            final float alpha = 1.0f - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
            viewHolder.itemView.setAlpha(alpha);
            viewHolder.itemView.setTranslationX(dX);
        } else {
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState,
                    isCurrentlyActive);
        }

    }

}
