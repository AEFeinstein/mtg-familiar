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

import android.support.annotation.CallSuper;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarListFragment;
import com.gelakinetic.mtgfam.fragments.TradeFragment;

import java.util.ArrayList;

/**
 * Specific implementation for list-based Familiar Fragments.
 *
 * @param <T>  type that is stored in the ArrayList
 * @param <VH> ViewHolder that is used by the adapter
 */
public abstract class CardDataAdapter<T extends MtgCard, VH extends SelectableItemViewHolder>
        extends SelectableItemAdapter<T, VH> {

    private final FamiliarListFragment mFragment;

    public CardDataAdapter(ArrayList<T> values, FamiliarListFragment fragment) {
        super(values, PreferenceAdapter.getUndoTimeout(fragment.getContext()));
        mFragment = fragment;
    }

    @Override
    @CallSuper
    public void onBindViewHolder(VH holder, int position) {
        if (!isInSelectMode()) {
                /* Sometimes an item will be selected after we exit select mode */
            setItemSelected(holder.itemView, position, false, false);
        } else {
            setItemSelected(holder.itemView, position, getItem(position).isSelected(), false);
        }
    }

    @Override
    public boolean pendingRemoval(final int position) {
        if (super.pendingRemoval(position)) {
            Snackbar undoBar = Snackbar.make(
                    mFragment.getFamiliarActivity().findViewById(R.id.fragment_container),
                    mFragment.getString(R.string.cardlist_item_deleted) + " " + getItemName(position),
                    getPendingTimeout()
            );
            undoBar.setAction(R.string.cardlist_undo, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Runnable pendingRemovalRunnable = getAndRemovePendingRunnable(position);
                    onUndoDelete(position);
                    if (pendingRemovalRunnable != null) {
                        removePending(pendingRemovalRunnable);
                    }
                    notifyItemChanged(position);
                }
            });
            undoBar.show();
            return true;
        }
        return false;
    }

    public abstract String getItemName(final int position);

    @Override
    public void onItemDismissed(final int position) {
        super.onItemDismissed(position);
        if (mFragment.shouldShowPrice()) {
            mFragment.updateTotalPrices(TradeFragment.BOTH);
        }
    }

    void onUndoDelete(final int position) {
        if (mFragment.shouldShowPrice()) {
            mFragment.updateTotalPrices(TradeFragment.BOTH);
        }
    }
}