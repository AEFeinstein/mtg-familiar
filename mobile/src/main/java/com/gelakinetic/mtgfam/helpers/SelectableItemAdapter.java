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
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter that holds whatever, swipes to delete with a undo timeout, and multi-select of items.
 *
 * @param <T>  type of what is held in the ArrayList
 * @param <VH> the ViewHolder that is used for the adapter
 */
public abstract class SelectableItemAdapter<T, VH extends SelectableItemAdapter.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    private final List<T> items;

    private boolean inSelectMode;
    private final SparseBooleanArray selectedItems;

    private final Handler handler;
    private final SparseArray<Runnable> pendingRunnables;

    private final int pendingTimeout;

    public SelectableItemAdapter(ArrayList<T> values, final int millisPending) {
        items = values;
        selectedItems = new SparseBooleanArray();
        handler = new Handler();
        inSelectMode = false;
        pendingRunnables = new SparseArray<>();
        pendingTimeout = millisPending;
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
     * Where things go before they get removed.
     *
     * @param position where the item to be removed is
     */
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
            return true;
        }
        return false;
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
        for (int i = 0; i < this.selectedItems.size(); i++) {
            if (this.selectedItems.valueAt(i)) {
                selectedItemsLocal.add(items.get(this.selectedItems.keyAt(i)));
            }
        }
        return selectedItemsLocal;

    }

    public void deleteSelectedItems() {
        ArrayList<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i)) {
                toRemove.add(selectedItems.keyAt(i));
            }
        }
        Collections.sort(toRemove);
        while (!toRemove.isEmpty()) {
            int lastIdx = toRemove.size() - 1;
            remove(toRemove.get(lastIdx));
            toRemove.remove(lastIdx);
        }
    }

    public boolean isItemPendingRemoval(final int position) {
        return pendingRunnables.indexOfKey(position) > -1;
    }

    public void deselectAll() {

        selectedItems.clear();
        setInSelectMode(false);
        notifyDataSetChanged();

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void onItemDismissed(final int position) {
        pendingRemoval(position);
        notifyItemChanged(position);
    }

    protected void setItemSelected(View view, int position, boolean selected, boolean shouldNotify) {
        view.setSelected(selected);
        if (selected) {
            selectedItems.put(position, true);
        } else {
            selectedItems.delete(position);
        }
        view.invalidate();

        // Notify of any changes
        if(shouldNotify) {
            notifyDataSetChanged();
        }
    }

    protected int getNumSelectedItems() {
        return selectedItems.size();
    }

    protected boolean isItemSelected(int position) {
        return selectedItems.get(position, false);
    }

    protected T getItem(int position) {
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

    /**
     * ViewHolder implementing the needed listeners and defining swipeable and its get/setters.
     */
    public abstract class ViewHolder
            extends RecyclerView.ViewHolder
            implements OnClickListener, OnLongClickListener {

        private boolean isSwipeable = true;

        public ViewHolder(View view) {
            super(view);
        }

        boolean getIsSwipeable() {
            return isSwipeable;
        }

        public void setIsSwipeable(final boolean isSwipeable) {
            this.isSwipeable = isSwipeable;
        }

    }
}
