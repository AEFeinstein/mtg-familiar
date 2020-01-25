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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarListFragment;

public abstract class CardDataViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {

    private final CardDataAdapter mAdapter;
    private final FamiliarListFragment mFragment;
    private final TextView mCardName;
    private boolean isSwipeable = true;

    /**
     * Constructor
     *
     * @param view        The root ViewGroup
     * @param layoutRowId The layout to inflate for this ViewHolder
     * @param adapter     The adapter which displays this ViewHolder
     * @param fragment    The fragment which contains the list which contains this ViewHolder
     */
    protected CardDataViewHolder(ViewGroup view, @LayoutRes final int layoutRowId, CardDataAdapter adapter,
                                 FamiliarListFragment fragment) {
        // The inflated view is set to itemView
        super(LayoutInflater.from(view.getContext()).inflate(layoutRowId, view, false));
        mCardName = itemView.findViewById(R.id.card_name);
        mAdapter = adapter;
        mFragment = fragment;
    }

    /**
     * @return true if this ViewHolder is swipeable, false otherwise
     */
    boolean getIsSwipeable() {
        return isSwipeable;
    }

    /**
     * Sets if this ViewHolder is swipeable or not
     *
     * @param isSwipeable true if it may be swiped, false otherwise
     */
    public void setIsSwipeable(final boolean isSwipeable) {
        this.isSwipeable = isSwipeable;
    }

    /**
     * This function will be called if this ViewHolder is clicked while the adapter is not in
     * select mode
     *
     * @param view     The View that was clicked
     * @param position The position of the View that was clicked in the adapter
     */
    protected abstract void onClickNotSelectMode(View view, int position);

    /**
     * @return The card name for the object displayed in this ViewHolder, from the TextView
     */
    protected String getCardName() {
        return mCardName.getText().toString();
    }

    /**
     * Sets the card name in the TextView for this ViewHolder
     *
     * @param name The card name to set
     */
    public void setCardName(String name) {
        mCardName.setText(name);
    }

    /**
     * When the ViewHolder is clicked, if the adapter is in select mode, it will either select or
     * unselect the ViewHolder. If all ViewHolders are unselected, select mode will be exited.
     * If the adapter isn't in select mode, it will call onClickNotSelectMode()
     *
     * @param view The view that was clicked
     */
    @Override
    public void onClick(View view) {
        int position = getAdapterPosition();
        if (RecyclerView.NO_POSITION != position) {
            if (mAdapter.isInSelectMode()) {
                if (itemView.isSelected()) {
                    // Unselect the item
                    mAdapter.setItemSelected(itemView, position, false, true);

                    // If there are no more items
                    if (mFragment.adaptersGetAllSelected() < 1) {
                        // Finish select mode
                        mFragment.finishActionMode();
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

    /**
     * When the ViewHolder is long clicked, it will start the parent adapter's select mode and
     * select the long clicked ViewHolder
     *
     * @param view The View that was long clicked
     * @return true if this long click was handled, false otherwise
     */
    @Override
    public boolean onLongClick(View view) {
        int position = getAdapterPosition();
        if (RecyclerView.NO_POSITION != position) {
            if (!mAdapter.isInSelectMode()) {
                // Start select mode
                mFragment.startActionMode();

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