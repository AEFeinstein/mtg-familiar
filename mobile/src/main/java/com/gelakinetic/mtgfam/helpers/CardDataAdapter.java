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

import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarListFragment;
import com.gelakinetic.mtgfam.fragments.TradeFragment;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

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

    private final FamiliarListFragment mFragment;

    protected final List<T> items;
    private final ArrayList<T> undoBuffer;

    private boolean inSelectMode;

    /**
     * Default constructor
     *
     * @param values   The values which will back this adapter
     * @param fragment The fragment this adapter will be shown in
     */
    protected CardDataAdapter(List<T> values, FamiliarListFragment fragment) {
        items = values;
        inSelectMode = false;
        undoBuffer = new ArrayList<>();

        mFragment = fragment;
    }

    /**
     * This method updates the contents of the RecyclerView.ViewHolder.itemView to reflect the item
     * at the given position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item
     *                 at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    @CallSuper
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (!isInSelectMode()) {
            /* Sometimes an item will be selected after we exit select mode */
            setItemSelected(holder.itemView, position, false, false);
        } else {
            synchronized (items) {
                setItemSelected(holder.itemView, position, items.get(position).isSelected(), false);
            }
        }
    }

    /**
     * @return The number of items in the list behind this adapter
     */
    @Override
    public int getItemCount() {
        synchronized (items) {
            return items.size();
        }
    }

    /**
     * Returns the item at the given position
     *
     * @param position The position to get
     * @return The item at that position
     */
    @Nullable
    protected T getItem(int position) {
        synchronized (items) {
            if (position < items.size()) {
                return items.get(position);
            }
        }
        return null;
    }

    /**
     * Called when an item is swiped away. This will remove the item, set up the "Undo" snackbar
     * and notify the fragment
     *
     * @param position The position of the item that was removed
     */
    void swipeRemoveItem(final int position) {
        // Remove the item from the list and add it to a temporary array
        String removedName;
        synchronized (items) {
            removedName = items.get(position).getName();
            undoBuffer.add(items.remove(position));
        }
        onItemRemoved();

        SnackbarWrapper.makeAndShowText(mFragment.getFamiliarActivity(), mFragment.getString(R.string.cardlist_item_deleted) + " " + removedName,
                PreferenceAdapter.getUndoTimeout(mFragment.getContext()), R.string.cardlist_undo,
                new View.OnClickListener() {
                    /**
                     * When "Undo" is clicked, readd the removed items to the underlying list,
                     * remove them from the undo list, and notify the adapter that it was changed
                     *
                     * @param v unused, the view that was clicked
                     */
                    @Override
                    public void onClick(View v) {
                        undoDelete();
                    }
                },
                new Snackbar.Callback() {
                    /**
                     * When the snackbar is dismissed, depending on how it was dismissed, either
                     * clear the undo buffer of all items and notify the adapter, or ignore it
                     *
                     * @param transientBottomBar The transient bottom bar which has been dismissed.
                     * @param event The event which caused the dismissal. One of either:
                     *              DISMISS_EVENT_SWIPE, DISMISS_EVENT_ACTION, DISMISS_EVENT_TIMEOUT,
                     *              DISMISS_EVENT_MANUAL or DISMISS_EVENT_CONSECUTIVE.
                     */
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        switch (event) {
                            case BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_SWIPE:
                            case BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT: {
                                finalizeDelete();
                                break;
                            }
                            case BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_MANUAL:
                            case BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION:
                            case BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE: {
                                // Snackbar was dismissed by action click, handled above or
                                // Hidden by a new snackbar, ignore it
                                break;
                            }
                        }
                    }
                });
    }

    /**
     * This function is called if the prior action was undone, and items are readded back to the
     * list. It may be overridden to do fragment-specific stuff.
     */
    @CallSuper
    protected void onItemReadded() {
        notifyDataSetChanged();

        // And update all prices if necessary
        if (mFragment.shouldShowPrice()) {
            mFragment.updateTotalPrices(TradeFragment.BOTH);
        }
    }

    /**
     * This function is called if an item was swiped away. It may be overridden to do
     * fragment-specific stuff.
     */
    @CallSuper
    protected void onItemRemoved() {
        // Tell the adapter the item was removed
        notifyDataSetChanged();

        // Update the price if necessary
        if (mFragment.shouldShowPrice()) {
            mFragment.updateTotalPrices(TradeFragment.BOTH);
        }
    }

    /**
     * This function is called when the undo option times out and the removed item is gone forever.
     * It may be overridden to do fragment-specific stuff.
     */
    protected void onItemRemovedFinal() {

    }

    /**
     * If we are in select mode.
     *
     * @return inSelectMode
     */
    boolean isInSelectMode() {
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

    /**
     * @return An ArrayList of all the currently selected items.
     */
    public ArrayList<T> getSelectedItems() {

        ArrayList<T> selectedItemsLocal = new ArrayList<>();
        synchronized (items) {
            for (T item : items) {
                if (item.isSelected()) {
                    selectedItemsLocal.add(item);
                }
            }
        }
        return selectedItemsLocal;

    }

    /**
     * Deletes all currently selected items, starting from the back to avoid
     * ConcurrentModificationExceptions or skipping over items. The deleted items are stored in
     * the undo buffer in case the user wants them back before the timeout
     */
    public void deleteSelectedItemsWithUndo() {
        synchronized (items) {
            for (int i = items.size() - 1; i >= 0; i--) {
                if (items.get(i).isSelected()) {
                    undoBuffer.add(items.get(i));
                    items.remove(i);
                }
            }
        }
        this.onItemRemoved();
    }

    /**
     * Deselects all items
     */
    public void deselectAll() {
        synchronized (items) {
            for (T item : items) {
                item.setSelected(false);
            }
        }
        setInSelectMode(false);
        notifyDataSetChanged();
    }

    /**
     * Sets a given item as selected or not
     *
     * @param view         The view which was selected
     * @param position     The position of the item which was selected
     * @param selected     true if the item was selected, false if it was not
     * @param shouldNotify true to notify the system to redraw the list, false if it's handled
     *                     elsewhere
     */
    void setItemSelected(View view, int position, boolean selected, boolean shouldNotify) {
        view.setSelected(selected);
        view.invalidate();
        synchronized (items) {
            items.get(position).setSelected(selected);
        }

        // Notify of any changes
        if (shouldNotify) {
            notifyDataSetChanged();
        }
    }

    /**
     * @return The number of currently selected items
     */
    public int getNumSelectedItems() {
        int numSelected = 0;
        synchronized (items) {
            for (T item : items) {
                if (item.isSelected()) {
                    numSelected++;
                }
            }
        }
        return numSelected;
    }

    /**
     * Undoes the last deleted action by readding the deleted cards to the list and removing them
     * from the undo buffer. Also notifies the adapter that items were readded
     */
    public void undoDelete() {
        // The user clicked Undo. Add the items back, then clear them from
        // the undo buffer
        synchronized (items) {
            items.addAll(undoBuffer);
        }
        undoBuffer.clear();
        // Notify the adapter, this may have fragment specific code
        onItemReadded();
    }

    /**
     * Finalizes the last delete action by clearing the undo buffer and notifying the fragment that
     * the delete is permanent
     */
    public void finalizeDelete() {
        // Snackbar timed out or was dismissed by the user, so wipe the
        // undoBuffer forever
        undoBuffer.clear();
        onItemRemovedFinal();
    }
}