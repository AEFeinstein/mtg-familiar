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

import android.text.Editable;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDataAdapter;
import com.gelakinetic.mtgfam.helpers.CardDataTouchHelper;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

/**
 * This class is for extension by any Fragment that has a custom list of cards at it's base that
 * allows modification.
 */
public abstract class FamiliarListFragment extends FamiliarFragment {

    /* Pricing */
    static final String PRICE_FORMAT = "$%.02f";

    /* UI Elements */
    private AutoCompleteTextView mNameField;
    private EditText mNumberOfField;
    private CheckBox mCheckboxFoil;
    private boolean mCheckboxFoilLocked = false;
    private final ArrayList<TextView> mTotalPriceFields = new ArrayList<>();
    private final ArrayList<View> mTotalPriceDividers = new ArrayList<>();
    private ActionMode mActionMode;

    /* Data adapters */
    private final ArrayList<CardDataAdapter> mCardDataAdapters = new ArrayList<>();
    private int mActionMenuResId;

    /**
     * Initializes common members. Must be called in onCreate
     *
     * @param fragmentView    the view of the fragment calling this method
     * @param recyclerViewIds the resource IDs of all the recycler views to be managed
     * @param adapters        the adapters for all the recycler views. Must have the same number of
     *                        elements as recyclerViewIds
     * @param priceViewIds    the resource IDs for all the total price views to be managed
     * @param priceDividerIds the resource IDs for all the total price dividers to be managed
     * @param actionMenuResId the resource ID for the action menu inflated when selecting items
     * @param addCardListener the listener to attach to mNameField, or null
     */
    void initializeMembers(View fragmentView, @LayoutRes int[] recyclerViewIds,
                           CardDataAdapter[] adapters, @IdRes int[] priceViewIds,
                           @IdRes int[] priceDividerIds, @MenuRes int actionMenuResId,
                           TextView.OnEditorActionListener addCardListener) {

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
        mNameField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // Set up the number of field
        mNumberOfField = fragmentView.findViewById(R.id.number_input);
        clearCardNumberInput();
        if (null != addCardListener) {
            mNumberOfField.setOnEditorActionListener(addCardListener);
        }

        // Set up the recycler views and adapters
        mCardDataAdapters.clear();
        for (int i = 0; i < recyclerViewIds.length; i++) {
            RecyclerView recyclerView = fragmentView.findViewById(recyclerViewIds[i]);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapters[i]);

            ItemTouchHelper.SimpleCallback callback =
                    new CardDataTouchHelper(adapters[i]);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(recyclerView);

            mCardDataAdapters.add(adapters[i]);
        }

        // Then set up the checkbox
        mCheckboxFoil = fragmentView.findViewById(R.id.list_foil);
        mCheckboxFoil.setOnLongClickListener(view -> {

            /* Lock the checkbox on long click */
            mCheckboxFoilLocked = true;
            mCheckboxFoil.setChecked(true);
            return true;

        });
        mCheckboxFoil.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (!isChecked) {
                /* Unlock the checkbox when the user unchecks it */
                mCheckboxFoilLocked = false;
            }

        });

        // And hide the camera button
        fragmentView.findViewById(R.id.camera_button).setVisibility(View.GONE);

        // Set up total price views
        mTotalPriceFields.clear();
        for (int resId : priceViewIds) {
            mTotalPriceFields.add(fragmentView.findViewById(resId));
        }
        mTotalPriceDividers.clear();
        if (null != priceDividerIds) {
            for (int resId : priceDividerIds) {
                mTotalPriceDividers.add(fragmentView.findViewById(resId));
            }
        }

        mActionMenuResId = actionMenuResId;
    }

    /**
     * When a FamiliarListFragment resumes, throw an exception if it hasn't been initialized
     * properly and set the visibility for total price views
     */
    @Override
    public void onResume() {
        super.onResume();

        if (null == mNameField) {
            throw new IllegalStateException("A class extending FamiliarListFragment must call" +
                    "initializeMembers()");
        }

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
                view.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Returns the adapter at the given index.
     *
     * @param adapterIdx Index for the desired adapter. Usually 0 for single lists, but trades have
     *                   two adapters
     * @return The adapter at that index
     */
    public CardDataAdapter getCardDataAdapter(int adapterIdx) {
        return mCardDataAdapters.get(adapterIdx);
    }

    /**
     * Deselects all currently selected items from all the adapters
     */
    private void adaptersDeselectAll() {
        for (CardDataAdapter adapter : mCardDataAdapters) {
            adapter.deselectAll();
        }
    }

    /**
     * Sets all adapters in select mode
     */
    private void adaptersSetAllSelectedMode() {
        for (CardDataAdapter adapter : mCardDataAdapters) {
            adapter.setInSelectMode(true);
        }
    }

    /**
     * @return The number of items currently selected by all adapters
     */
    public int adaptersGetAllSelected() {
        int numSelected = 0;
        for (CardDataAdapter adapter : mCardDataAdapters) {
            numSelected += adapter.getNumSelectedItems();
        }
        return numSelected;
    }

    /**
     * Starts the action mode, i.e. select mode In this mode, multiple items in the list may be
     * selected for deletion, sharing, etc.
     */
    public void startActionMode() {

        mActionMode = getFamiliarActivity().startSupportActionMode(new ActionMode.Callback() {

            /**
             * Called when the action mode is created. Inflates the menu
             *
             * @param mode ActionMode being created
             * @param menu Menu used to populate action buttons
             * @return true because the menu was inflated
             */
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(mActionMenuResId, menu);
                return true;
            }

            /**
             * Called to refresh the menu whenever it's invalidated
             *
             * @param mode ActionMode being prepared
             * @param menu Menu used to populate action buttons
             * @return false because nothing changed
             */
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            /**
             * Called when an action item in a menu is clicked. This has to handle all of the
             * different menus inflated. Currently this is just R.menu.decklist_select_menu and
             * R.menu.action_mode_menu
             *
             * @param mode The current ActionMode
             * @param item The item that was clicked
             * @return true if this callback handled the event, false if the standard MenuItem
             * invocation should continue.
             */
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                // All lists have this one
                if (item.getItemId() == R.id.deck_delete_selected) {
                    // Remove all selected items, put them in the undo buffer
                    for (CardDataAdapter adapter : mCardDataAdapters) {
                        adapter.deleteSelectedItemsWithUndo();
                    }

                    // Make a snackbar to undo this delete
                    SnackbarWrapper.makeAndShowText(getFamiliarActivity(), "", PreferenceAdapter.getUndoTimeout(getContext()), R.string.cardlist_undo,
                            new View.OnClickListener() {
                                /**
                                 * When "Undo" is clicked, readd the removed items to the underlying list,
                                 * remove them from the undo list, and notify the adapter that it was changed
                                 *
                                 * @param v unused, the view that was clicked
                                 */
                                @Override
                                public void onClick(View v) {
                                    for (CardDataAdapter adapter : mCardDataAdapters) {
                                        adapter.undoDelete();
                                    }
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
                                            // Snackbar timed out or was dismissed by the user, so wipe the
                                            // undoBuffer forever
                                            for (CardDataAdapter adapter : mCardDataAdapters) {
                                                adapter.finalizeDelete();
                                            }
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

                    mode.finish();
                    if (shouldShowPrice()) {
                        updateTotalPrices(TradeFragment.BOTH);
                    }
                    return true;
                }
                // Only for the decklist
                else if (item.getItemId() == R.id.deck_import_selected) {
                    ArrayList<DecklistHelpers.CompressedDecklistInfo> selectedItems =
                            ((DecklistFragment.DecklistDataAdapter) getCardDataAdapter(0)).getSelectedItems();
                    for (DecklistHelpers.CompressedDecklistInfo info : selectedItems) {
                        WishlistHelpers.addItemToWishlist(getActivity(),
                                info.convertToWishlist());
                    }
                    mode.finish();
                    return true;
                } else {
                    return false;
                }
            }

            /**
             * Called when an action mode is about to be exited and destroyed.
             *
             * @param mode The ActionMode being destroyed
             */
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                adaptersDeselectAll();
            }
        });

        adaptersSetAllSelectedMode();
    }

    /**
     * Called to finish the action mode, i.e. select mode. Make sure that all adapters leave select
     * mode
     */
    public void finishActionMode() {
        mActionMode.finish();
        for (CardDataAdapter adapter : mCardDataAdapters) {
            adapter.setInSelectMode(false);
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
     * Load the price for a given card. This handles all the spice stuff
     *
     * @param data A card to load price info for
     */
    public void loadPrice(final MtgCard data) {

        /* If the priceInfo is already loaded, don't bother performing a query */
        if (data.mPriceInfo != null) {
            data.mPrice = (int) (data.mPriceInfo.getPrice(data.mIsFoil, getPriceSetting()).price * 100);
            data.mIsFoil = data.mPriceInfo.getPrice(data.mIsFoil, getPriceSetting()).isFoil;
        } else {
            try {
                getFamiliarActivity().mMarketPriceStore.fetchMarketPrice(data,
                        result -> {
                            // This is not run on the UI thread
                            /* Sanity check */
                            if (result == null) {
                                data.mPriceInfo = null;
                                data.mMessage = getString(R.string.card_view_price_not_found);
                            } else {
                                /* Set the PriceInfo object */
                                data.mPriceInfo = result;

                                /* Only reset the price to the downloaded one if the old price isn't custom */
                                if (!data.mIsCustomPrice) {
                                    data.mPrice = (int) (result.getPrice(data.mIsFoil, getPriceSetting()).price * 100);
                                    data.mIsFoil = result.getPrice(data.mIsFoil, getPriceSetting()).isFoil;
                                }
                                /* Clear the message */
                                data.mMessage = null;
                            }

                            /* because this can return when the fragment is in the background */
                            if (FamiliarListFragment.this.isAdded()) {
                                onCardPriceLookupSuccess(data, result);
                            }
                        },
                        throwable -> {
                            // This is not run on the UI thread
                            data.mPriceInfo = null;
                            data.mMessage = throwable.getLocalizedMessage();
                            if (null == data.mMessage) {
                                data.mMessage = throwable.getClass().toString();
                            }
                            if (FamiliarListFragment.this.isAdded()) {
                                onCardPriceLookupFailure(data, throwable);
                            }
                        },
                        () -> {
                            // This is run on the UI thread
                            if (FamiliarListFragment.this.isAdded()) {
                                onAllPriceLookupsFinished();
                            }
                        });
            } catch (java.lang.InstantiationException e) {
                onCardPriceLookupFailure(data, e);
            }
        }
    }

    /**
     * Called when a price load fails. Should contain fragment-specific code
     *
     * @param data      The card for which the price lookup failed
     * @param exception The exception that occured
     */
    protected abstract void onCardPriceLookupFailure(MtgCard data, Throwable exception);

    /**
     * Called when a price load succeeds. Should contain fragment-specific code
     *
     * @param data   The card for which the price lookup succeeded
     * @param result The price information
     */
    protected abstract void onCardPriceLookupSuccess(MtgCard data, MarketPriceInfo result);

    /**
     * Called on the UI thread when all price operations are finished
     */
    protected abstract void onAllPriceLookupsFinished();

    /**
     * Updates the total prices shown for the lists
     *
     * @param side The side to update (only valid for trade)
     */
    public abstract void updateTotalPrices(int side);

    /**
     * @return true if the total price should be shown, false otherwise
     */
    public abstract boolean shouldShowPrice();

    /**
     * @return the current price setting
     */
    protected abstract MarketPriceInfo.PriceType getPriceSetting();

    /**
     * @param priceSetting The price setting to write to preferences
     */
    public abstract void setPriceSetting(MarketPriceInfo.PriceType priceSetting);

    /**
     * Show an error snackbar when a file is saved with an empty name
     */
    public void showErrorSnackbarNoName() {
        SnackbarWrapper.makeAndShowText(this.getActivity(),
                getString(R.string.judges_corner_error) + " " + getString(R.string.life_counter_edit_name_dialog_title),
                SnackbarWrapper.LENGTH_LONG);
    }
}
