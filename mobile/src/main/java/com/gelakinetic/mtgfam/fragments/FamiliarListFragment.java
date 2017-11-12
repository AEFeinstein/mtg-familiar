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
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.design.widget.Snackbar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SelectableItemAdapter;
import com.gelakinetic.mtgfam.helpers.SelectableItemTouchHelper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.util.ArrayList;

/**
 * This class is for extension by any Fragment that has a custom list of cards at it's base that
 * allows modification.
 */
public abstract class FamiliarListFragment extends FamiliarFragment {

    /* Pricing */
    public static final int LOW_PRICE = 0;
    public static final int AVG_PRICE = 1;
    public static final int HIGH_PRICE = 2;
    public static final int FOIL_PRICE = 3;
    public int mPriceSetting;
    int mPriceFetchRequests = 0;
    static final String PRICE_FORMAT = "$%.02f";

    /* UI Elements */
    private AutoCompleteTextView mNameField;
    private EditText mNumberOfField;
    private CheckBox mCheckboxFoil;
    private boolean mCheckboxFoilLocked = false;
    private ArrayList<TextView> mTotalPriceFields = new ArrayList<>();
    private ArrayList<View> mTotalPriceDividers = new ArrayList<>();
    private int mActionMenuResId;
    private ActionMode mActionMode;

    /* Data adapters */
    private ArrayList<CardDataAdapter> mCardDataAdapters = new ArrayList<>();

    /**
     * Initializes common members. Must be called in onCreate
     *
     * @param fragmentView    the view of the fragment calling this method
     * @param recyclerViewIds the resource IDs of all the recycler views to be managed
     * @param adapters        the adapters for all the recycler views. Must have the same number of
     *                        elements as recyclerViewIds
     * @param priceViewIds    the resource IDs for all the total price views to be managed
     * @param priceDividerIds the resource IDs for all the total price dividers to be managed
     * @param addCardListener the listener to attach to mNameField, or null
     * @param ActionMenuResId the action menu to inflate
     */
    void initializeMembers(View fragmentView, @LayoutRes int[] recyclerViewIds,
                           CardDataAdapter[] adapters, @IdRes int[] priceViewIds,
                           @IdRes int[] priceDividerIds,
                           TextView.OnEditorActionListener addCardListener,
                           @MenuRes final int ActionMenuResId) {

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

        // Set which menu to inflate for the action menu
        mActionMenuResId = ActionMenuResId;

        // Set up total price views
        for (int resId : priceViewIds) {
            mTotalPriceFields.add((TextView) fragmentView.findViewById(resId));
        }
        if (null != priceDividerIds) {
            for (int resId : priceDividerIds) {
                mTotalPriceDividers.add(fragmentView.findViewById(resId));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        for (CardDataAdapter adapter : mCardDataAdapters) {
            adapter.removePendingNow();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        /* Show the total price, if desired */
        if (shouldShowPrice()) {
            for (TextView priceView : mTotalPriceFields) {
                priceView.setVisibility(View.VISIBLE);
            }
            for (View view : mTotalPriceDividers) {
                view.setVisibility(View.VISIBLE);
            }
        } else {
            for (TextView priceView : mTotalPriceFields) {
                priceView.setVisibility(View.GONE);
            }
            for (View view : mTotalPriceDividers) {
                view.setVisibility(View.VISIBLE);
            }
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
     * @return true if the foil checkbox is checked, false otherwise
     */
    boolean checkboxFoilIsChecked() {
        return mCheckboxFoil.isChecked();
    }

    /**
     * Unchecks the foil checkbox if it isn't locked
     */
    public void uncheckFoilCheckbox() {
        if (!mCheckboxFoilLocked) {
            mCheckboxFoil.setChecked(false);
        }
    }

    /**
     * Sets the total price text and color for the given field
     *
     * @param priceText The text to set
     * @param color     The color to write the text in
     * @param side      An index to the view to update
     */
    void setTotalPrice(String priceText, Integer color, int side) {
        mTotalPriceFields.get(side).setText(priceText);
        if (null != color) {
            mTotalPriceFields.get(side).setTextColor(color);
        }
    }

    /**
     * Updates the total prices shown for the lists
     *
     * @param side The side to update (only valid for trade)
     */
    abstract void updateTotalPrices(int side);

    /**
     * @return true if the total price should be shown, false otherwise
     */
    abstract boolean shouldShowPrice();

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

        CardDataAdapter(ArrayList<T> values) {
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

        public abstract String getItemName(final int position);

        abstract class ViewHolder extends SelectableItemAdapter.ViewHolder {

            protected TextView mCardName;

            ViewHolder(ViewGroup view, @LayoutRes final int layoutRowId) {
                // The inflated view is set to itemView
                super(LayoutInflater.from(view.getContext()).inflate(layoutRowId, view, false));
                mCardName = itemView.findViewById(R.id.card_name);
            }

            abstract void onClickNotSelectMode(View view);

            @Override
            public void onClick(View view) {
                if (isInSelectMode()) {
                    int position = getAdapterPosition();
                    if (itemView.isSelected()) {
                        // Unselect the item
                        setItemSelected(itemView, position, false, true);

                        int numSelected = 0;
                        for (CardDataAdapter adapter : mCardDataAdapters) {
                            numSelected += adapter.getNumSelectedItems();
                        }
                        // If there are no more items
                        if (numSelected < 1) {
                            // Finish select mode
                            mActionMode.finish();
                            setInSelectMode(false);
                        }
                    } else {
                        // Select the item
                        setItemSelected(itemView, position, true, true);
                    }
                } else {
                    onClickNotSelectMode(view);
                }
            }

            @Override
            public boolean onLongClick(View view) {
                if (!isInSelectMode()) {
                    // Start select mode
                    mActionMode = getFamiliarActivity().startSupportActionMode(new ActionMode.Callback() {

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
                                    adaptersDeleteSelectedItems();
                                    mode.finish();
                                    if (shouldShowPrice()) {
                                        updateTotalPrices(TradeFragment.BOTH);
                                    }
                                    return true;
                                }
                                // Only for the decklist
                                case R.id.deck_import_selected: {
                                    ArrayList<DecklistHelpers.CompressedDecklistInfo> selectedItems =
                                            ((DecklistFragment.CardDataAdapter) getCardDataAdapter(0)).getSelectedItems();
                                    for (DecklistHelpers.CompressedDecklistInfo info : selectedItems) {
                                        WishlistHelpers.addItemToWishlist(getContext(),
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
                            adaptersDeselectAll();
                        }
                    });

                    for (CardDataAdapter adapter : mCardDataAdapters) {
                        adapter.setInSelectMode(true);
                    }

                    // Then select the item
                    setItemSelected(itemView, getAdapterPosition(), true, true);

                    // This click was handled
                    return true;
                }

                // The click was not handled
                return false;
            }
        }

        @Override
        public void onItemDismissed(final int position) {
            super.onItemDismissed(position);
            if (shouldShowPrice()) {
                updateTotalPrices(TradeFragment.BOTH);
            }
        }

        void onUndoDelete(final int position) {
            if (shouldShowPrice()) {
                updateTotalPrices(TradeFragment.BOTH);
            }
        }
    }
}
