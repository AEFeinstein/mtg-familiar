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
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
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

    private final FamiliarListFragment mFragment;

    private final List<T> items;
    private final ArrayList<T> undoBuffer;

    private boolean inSelectMode;

    /**
     * TODO
     *
     * @param values
     * @param fragment
     */
    public CardDataAdapter(ArrayList<T> values, FamiliarListFragment fragment) {
        items = values;
        inSelectMode = false;
        undoBuffer = new ArrayList<>();

        mFragment = fragment;
    }

    /**
     * TODO
     *
     * @param holder
     * @param position
     */
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

    /**
     * TODO
     *
     * @param position
     * @return
     */
    @Nullable
    private String getItemName(final int position) {
        if (position < items.size()) {
            return items.get(position).mName;
        }
        return null;
    }

    /**
     * TODO
     *
     * @return
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * TODO
     *
     * @param position
     * @return
     */
    @Nullable
    public T getItem(int position) {
        if (position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    /**
     * TODO
     *
     * @param item
     * @return
     */
    protected int itemsIndexOf(T item) {
        return items.indexOf(item);
    }

    /**
     * TODO
     *
     * @param position
     */
    void swipeRemoveItem(final int position) {
        // Remove the item from the list and add it to a temporary array
        String removedName = getItemName(position);
        undoBuffer.add(items.remove(position));

        onItemRemoved(position);

        Snackbar.make(mFragment.getFamiliarActivity().findViewById(R.id.fragment_container),
                mFragment.getString(R.string.cardlist_item_deleted) + " " + removedName,
                PreferenceAdapter.getUndoTimeout(mFragment.getContext()))
                .setAction(R.string.cardlist_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // The user clicked Undo. Add the items back, then clear them from
                        // the undo buffer
                        items.addAll(undoBuffer);
                        undoBuffer.clear();
                        // Notify the adapter, this may have fragment specific code
                        onItemAdded();
                    }
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        switch (event) {
                            case Snackbar.Callback.DISMISS_EVENT_MANUAL:
                            case Snackbar.Callback.DISMISS_EVENT_SWIPE:
                            case Snackbar.Callback.DISMISS_EVENT_TIMEOUT: {
                                // Snackbar timed out or was dismissed by the user, so wipe the
                                // undoBuffer forever
                                undoBuffer.clear();
                                onItemRemovedFinal();
                                break;
                            }
                            case Snackbar.Callback.DISMISS_EVENT_ACTION:
                            case Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE: {
                                // Snackbar was dismissed by action click, handled above or
                                // Hidden by a new snackbar, ignore it
                                break;
                            }
                        }
                    }
                })
                .show();
    }

    /**
     * TODO
     */
    @CallSuper
    protected void onItemAdded() {
        notifyDataSetChanged();

        // And update all prices if necessary
        if (mFragment.shouldShowPrice()) {
            mFragment.updateTotalPrices(TradeFragment.BOTH);
        }
    }

    /**
     * TODO
     *
     * @param position
     */
    @CallSuper
    protected void onItemRemoved(int position) {
        // Tell the adapter the item was removed
        notifyItemRemoved(position);

        // Update the price if necessary
        if (mFragment.shouldShowPrice()) {
            mFragment.updateTotalPrices(TradeFragment.BOTH);
        }
    }

    /**
     * TODO
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
    void setInSelectMode(final boolean inSelectMode) {
        this.inSelectMode = inSelectMode;
    }

    /**
     * TODO
     *
     * @return
     */
    protected ArrayList<T> getSelectedItems() {

        ArrayList<T> selectedItemsLocal = new ArrayList<>();
        for (T item : items) {
            if (item.isSelected()) {
                selectedItemsLocal.add(item);
            }
        }
        return selectedItemsLocal;

    }

    /**
     * TODO
     */
    public void deleteSelectedItems() {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).isSelected()) {
                items.remove(i);
            }
        }
    }

    /**
     * TODO
     */
    public void deselectAll() {

        for (T item : items) {
            item.setSelected(false);
        }
        setInSelectMode(false);
        notifyDataSetChanged();

    }

    /**
     * TODO
     *
     * @param view
     * @param position
     * @param selected
     * @param shouldNotify
     */
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

    /**
     * TODO
     *
     * @param position
     * @return
     */
    protected boolean isItemSelected(int position) {
        return items.get(position).isSelected();
    }

    /**
     * TODO
     *
     * @return
     */
    int getNumSelectedItems() {
        int numSelected = 0;
        for (T item : items) {
            if (item.isSelected()) {
                numSelected++;
            }
        }
        return numSelected;
    }
}