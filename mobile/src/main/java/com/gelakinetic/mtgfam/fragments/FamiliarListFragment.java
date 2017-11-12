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

package com.gelakinetic.mtgfam.fragments;

import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.design.widget.Snackbar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SelectableItemAdapter;
import com.gelakinetic.mtgfam.helpers.SelectableItemTouchHelper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.util.ArrayList;

/**
 * This class is for extension by any Fragment that has a custom list of cards at it's base that
 * allows modification.
 */
public abstract class FamiliarListFragment extends FamiliarFragment {

    /* Preferences */
    public int mPriceSetting;

    /* Pricing Constants */
    public static final int LOW_PRICE = 0;
    public static final int AVG_PRICE = 1;
    public static final int HIGH_PRICE = 2;
    public static final int FOIL_PRICE = 3;

    /* UI Elements */
    private AutoCompleteTextView mNameField;
    private EditText mNumberOfField;
    public CheckBox mCheckboxFoil;
    TextView mTotalPriceField;

    int mPriceFetchRequests = 0;

    private ArrayList<CardDataAdapter> mCardDataAdapters = new ArrayList<>();

    boolean mCheckboxFoilLocked = false;

    ActionMode mActionMode;
    ActionMode.Callback mActionModeCallback;

    /**
     * Initializes common members. Must be called in onCreate
     *
     * @param fragmentView    the view of the fragment calling this method
     * @param recyclerViewIds the resource IDs of all the recycler views to be managed
     * @param adapters        the adapters for all the recycler views. Must have the same number of
     *                        elements as recyclerViewIds
     * @param addCardListener the listener to attach to mNameField, or null
     */
    void initializeMembers(View fragmentView, int[] recyclerViewIds, CardDataAdapter[] adapters, TextView.OnEditorActionListener addCardListener) {

        // Set up the name field
        mNameField = fragmentView.findViewById(R.id.name_search);
        /* Set up the autocomplete adapter, and default number */
        mNameField.setAdapter(
                new AutocompleteCursorAdapter(this,
                        new String[]{CardDbAdapter.KEY_NAME},
                        new int[]{R.id.text1}, mNameField,
                        false)
        );
        if (null != addCardListener) {
            mNameField.setOnEditorActionListener(addCardListener);
        }

        // Set up the number of field
        mNumberOfField = fragmentView.findViewById(R.id.number_input);
        clearCardNumberInput();
        if (null != addCardListener) {
            mNumberOfField.setOnEditorActionListener(addCardListener);
        }

        // Set up the recycler views and adapters
        for (int i = 0; i < recyclerViewIds.length; i++) {
            RecyclerView recyclerView = fragmentView.findViewById(recyclerViewIds[i]);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapters[i]);

            ItemTouchHelper.SimpleCallback callback =
                    new SelectableItemTouchHelper(adapters[i], ItemTouchHelper.LEFT);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(recyclerView);

            mCardDataAdapters.add(adapters[i]);
        }

        // Then set up the checkbox
        mCheckboxFoil = fragmentView.findViewById(R.id.list_foil);
        mCheckboxFoil.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {

                /* Lock the checkbox on long click */
                mCheckboxFoilLocked = true;
                mCheckboxFoil.setChecked(true);
                return true;

            }

        });
        mCheckboxFoil.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (!isChecked) {
                    /* Unlock the checkbox when the user unchecks it */
                    mCheckboxFoilLocked = false;
                }

            }

        });

        // And hide the camera button
        fragmentView.findViewById(R.id.camera_button).setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        for (CardDataAdapter adapter : mCardDataAdapters) {
            adapter.removePendingNow();
        }
    }

    public CardDataAdapter getCardDataAdapter(int adapterIdx) {
        return mCardDataAdapters.get(adapterIdx);
    }

    void adaptersDeleteSelectedItems() {
        for (CardDataAdapter adapter : mCardDataAdapters) {
            adapter.deleteSelectedItems();
        }
    }

    void adaptersDeselectAll() {
        for (CardDataAdapter adapter : mCardDataAdapters) {
            adapter.deselectAll();
        }
    }

    /**
     * @return The current text in mNameField
     */
    Editable getCardNameInput() {
        return mNameField.getText();
    }

    /**
     * Clears mNameField
     */
    public void clearCardNameInput() {
        mNameField.setText("");
    }

    /**
     * @return The current text in mNumberOfField
     */
    Editable getCardNumberInput() {
        return mNumberOfField.getText();
    }

    /**
     * Clears mNameField
     */
    public void clearCardNumberInput() {
        mNumberOfField.setText("1");
    }

    /**
     * Specific implementation for list-based Familiar Fragments.
     *
     * @param <T>  type that is stored in the ArrayList
     * @param <VH> ViewHolder that is used by the adapter
     */
    public abstract class CardDataAdapter<T extends MtgCard, VH extends CardDataAdapter.ViewHolder>
            extends SelectableItemAdapter<T, VH> {

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

        public CardDataAdapter(ArrayList<T> values) {
            super(values, PreferenceAdapter.getUndoTimeout(getContext()));
        }

        @Override
        public boolean pendingRemoval(final int position) {
            if (super.pendingRemoval(position)) {
                Snackbar undoBar = Snackbar.make(
                        getFamiliarActivity().findViewById(R.id.fragment_container),
                        getString(R.string.cardlist_item_deleted) + " " + getItemName(position),
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

        public void onUndoDelete(final int position) {
            // Do nothing by default.
        }

        public abstract String getItemName(final int position);

        abstract class ViewHolder extends SelectableItemAdapter.ViewHolder {

            final TextView mCardName;

            ViewHolder(ViewGroup view, @LayoutRes final int layoutRowId) {
                // The inflated view is set to itemView
                super(LayoutInflater.from(view.getContext()).inflate(layoutRowId, view, false));
                mCardName = itemView.findViewById(R.id.card_name);
            }

            @Override
            public void onClick(View view) {
                if (isInSelectMode()) {
                    int position = getAdapterPosition();
                    if (itemView.isSelected()) {
                        // Unselect the item
                        setItemSelected(itemView, position, false, true);

                        // If there are no more items
                        if (getNumSelectedItems() < 1) {
                            // Finish select mode
                            mActionMode.finish();
                            setInSelectMode(false);
                        }
                    } else {
                        // Select the item
                        setItemSelected(itemView, position, true, true);
                    }
                }
            }

            @Override
            public boolean onLongClick(View view) {
                if (!isInSelectMode()) {
                    // Start select mode
                    mActionMode = getFamiliarActivity().startSupportActionMode(mActionModeCallback);
                    setInSelectMode(true);

                    // Then select the item
                    setItemSelected(itemView, getAdapterPosition(), true, true);

                    // This click was handled
                    return true;
                }

                // The click was not handled
                return false;
            }
        }

    }

}
