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

import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarListFragment;
import com.gelakinetic.mtgfam.fragments.TradeFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Specific implementation for list-based Familiar Fragments.
 *
 * @param <T>  type that is stored in the ArrayList
 * @param <VH> ViewHolder that is used by the adapter
 */
public abstract class CardDataAdapter<T extends MtgCard, VH extends CardDataViewHolder>
        extends RecyclerView.Adapter<VH> {


    private final List<T> items;

    private boolean inSelectMode;

    private final Handler handler;
    private final SparseArray<Runnable> pendingRunnables;

    private final int pendingTimeout;
    private final FamiliarListFragment mFragment;

    public CardDataAdapter(ArrayList<T> values, FamiliarListFragment fragment) {
        items = values;
        handler = new Handler();
        inSelectMode = false;
        pendingRunnables = new SparseArray<>();
        pendingTimeout = PreferenceAdapter.getUndoTimeout(fragment.getContext());

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

    public boolean pendingRemoval(final int position) {
        if (pendingRunnables.indexOfKey(position) < 0) {
            notifyItemChanged(position);
            Runnable pendingRunnable = new Runnable() {
                @Override
                public void run() {
                    remove(position);
                }
            };
            handler.postDelayed(pendingRunnable, pendingTimeout);
            pendingRunnables.put(position, pendingRunnable);
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

    public void onItemDismissed(final int position) {
        pendingRemoval(position);
        notifyItemChanged(position);
        if (mFragment.shouldShowPrice()) {
            mFragment.updateTotalPrices(TradeFragment.BOTH);
        }
    }

    void onUndoDelete(final int position) {
        if (mFragment.shouldShowPrice()) {
            mFragment.updateTotalPrices(TradeFragment.BOTH);
        }
    }


    /**
     * Properly go about removing an item from the list.
     *
     * @param position where the item to remove is
     */
    public void remove(final int position) {

        try {
            final T item = items.get(position);
            if (pendingRunnables.indexOfKey(position) > -1) {
                pendingRunnables.remove(position);
            }
            if (items.contains(item)) {
                items.remove(position);
                notifyItemRemoved(position);
            }
        } catch (IndexOutOfBoundsException oob) {
            /* Happens from time to time, shouldn't worry about it */
        }

    }

    /**
     * Execute any pending Runnables NOW. This generally means we are moving away from this
     * fragment.
     */
    public void removePendingNow() {
        for (int i = 0; i < pendingRunnables.size(); i++) {
            Runnable runnable = pendingRunnables.valueAt(i);
            handler.removeCallbacks(runnable);
            runnable.run();
        }
        pendingRunnables.clear();
    }

    protected void removePending(Runnable pendingRemovalRunnable) {
        handler.removeCallbacks(pendingRemovalRunnable);
    }

    /**
     * If we are in select mode.
     *
     * @return inSelectMode
     */
    public boolean isInSelectMode() {
        return inSelectMode;
    }

    /**
     * Set the select mode.
     *
     * @param inSelectMode if we are in select mode or not
     */
    public void setInSelectMode(final boolean inSelectMode) {
        this.inSelectMode = inSelectMode;
    }

    public ArrayList<T> getSelectedItems() {

        ArrayList<T> selectedItemsLocal = new ArrayList<>();
        for (T item : items) {
            if (item.isSelected()) {
                selectedItemsLocal.add(item);
            }
        }
        return selectedItemsLocal;

    }

    public void deleteSelectedItems() {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).isSelected()) {
                items.remove(i);
            }
        }
    }

    public boolean isItemPendingRemoval(final int position) {
        return pendingRunnables.indexOfKey(position) > -1;
    }

    public void deselectAll() {

        for (T item : items) {
            item.setSelected(false);
        }
        setInSelectMode(false);
        notifyDataSetChanged();

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    protected void setItemSelected(View view, int position, boolean selected, boolean shouldNotify) {
        view.setSelected(selected);
        if (selected) {
            items.get(position).setSelected(true);
        } else {
            items.get(position).setSelected(false);
        }
        view.invalidate();

        // Notify of any changes
        if (shouldNotify) {
            notifyDataSetChanged();
        }
    }

    protected int getNumSelectedItems() {
        int numSelected = 0;
        for (T item : items) {
            if (item.isSelected()) {
                numSelected++;
            }
        }
        return numSelected;
    }

    protected boolean isItemSelected(int position) {
        return items.get(position).isSelected();
    }

    public T getItem(int position) {
        return items.get(position);
    }

    protected int itemsIndexOf(T item) {
        return items.indexOf(item);
    }

    protected Runnable getAndRemovePendingRunnable(int position) {
        Runnable runnable = pendingRunnables.get(position);
        pendingRunnables.remove(position);
        return runnable;
    }

    protected int getPendingTimeout() {
        return pendingTimeout;
    }
}