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

import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarListFragment;
import com.gelakinetic.mtgfam.fragments.TradeFragment;

import java.util.ArrayList;

public abstract class CardDataViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {

    protected final CardDataAdapter mAdapter;
    private final FamiliarListFragment mFragment;
    public TextView mCardName;
    private int mActionMenuResId;

    private boolean isSwipeable = true;

    public CardDataViewHolder(ViewGroup view, @LayoutRes final int layoutRowId, CardDataAdapter adapter, FamiliarListFragment fragment, @MenuRes int actionMenuResId) {
        // The inflated view is set to itemView
        super(LayoutInflater.from(view.getContext()).inflate(layoutRowId, view, false));
        mCardName = itemView.findViewById(R.id.card_name);
        mAdapter = adapter;
        mFragment = fragment;
        mActionMenuResId = actionMenuResId;
    }

    boolean getIsSwipeable() {
        return isSwipeable;
    }

    public void setIsSwipeable(final boolean isSwipeable) {
        this.isSwipeable = isSwipeable;
    }

    abstract public void onClickNotSelectMode(View view, int position);

    @Override
    public void onClick(View view) {
        int position = getAdapterPosition();
        if (RecyclerView.NO_POSITION != position) {
            if (mAdapter.isInSelectMode()) {
                if (itemView.isSelected()) {
                    // Unselect the item
                    mAdapter.setItemSelected(itemView, position, false, true);

                    int numSelected = 0;
                    for (CardDataAdapter adapter : mFragment.mCardDataAdapters) {
                        numSelected += adapter.getNumSelectedItems();
                    }
                    // If there are no more items
                    if (numSelected < 1) {
                        // Finish select mode
                        mFragment.mActionMode.finish();
                        mAdapter.setInSelectMode(false);
                    }
                } else {
                    // Select the item
                    mAdapter.setItemSelected(itemView, position, true, true);
                }
            } else {
                onClickNotSelectMode(view, position);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int position = getAdapterPosition();
        if (RecyclerView.NO_POSITION != position) {
            if (!mAdapter.isInSelectMode()) {
                // Start select mode
                mFragment.mActionMode = mFragment.getFamiliarActivity().startSupportActionMode(new ActionMode.Callback() {

                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater inflater = mode.getMenuInflater();
                        inflater.inflate(mActionMenuResId, menu); // action_mode_menu or decklist_select_menu
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        switch (item.getItemId()) {
                            // All lists have this one
                            case R.id.deck_delete_selected: {
                                mFragment.adaptersDeleteSelectedItems();
                                mode.finish();
                                if (mFragment.shouldShowPrice()) {
                                    mFragment.updateTotalPrices(TradeFragment.BOTH);
                                }
                                return true;
                            }
                            // Only for the decklist
                            case R.id.deck_import_selected: {
                                ArrayList<DecklistHelpers.CompressedDecklistInfo> selectedItems =
                                        ((DecklistFragment.DecklistDataAdapter) mFragment.getCardDataAdapter(0)).getSelectedItems();
                                for (DecklistHelpers.CompressedDecklistInfo info : selectedItems) {
                                    WishlistHelpers.addItemToWishlist(mFragment.getContext(),
                                            info.convertToWishlist());
                                }
                                mode.finish();
                                return true;
                            }
                            default: {
                                return false;
                            }
                        }
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        mFragment.adaptersDeselectAll();
                    }
                });

                for (CardDataAdapter adapter : mFragment.mCardDataAdapters) {
                    adapter.setInSelectMode(true);
                }

                // Then select the item
                mAdapter.setItemSelected(itemView, position, true, true);

                // This click was handled
                return true;
            }
        }
        // The click was not handled
        return false;
    }
}