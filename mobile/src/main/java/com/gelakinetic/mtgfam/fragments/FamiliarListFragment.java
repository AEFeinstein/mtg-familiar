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

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDataAdapter;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.CardDataTouchHelper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

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
    static final String PRICE_FORMAT = "$%.02f";

    private int mPriceFetchRequests = 0;

    /* UI Elements */
    private AutoCompleteTextView mNameField;
    private EditText mNumberOfField;
    private CheckBox mCheckboxFoil;
    private boolean mCheckboxFoilLocked = false;
    private ArrayList<TextView> mTotalPriceFields = new ArrayList<>();
    private ArrayList<View> mTotalPriceDividers = new ArrayList<>();
    public ActionMode mActionMode;

    /* Data adapters */
    public ArrayList<CardDataAdapter> mCardDataAdapters = new ArrayList<>();

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
     */
    void initializeMembers(View fragmentView, @LayoutRes int[] recyclerViewIds,
                           CardDataAdapter[] adapters, @IdRes int[] priceViewIds,
                           @IdRes int[] priceDividerIds,
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
                    new CardDataTouchHelper(adapters[i], ItemTouchHelper.LEFT);
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

        // Set up total price views
        mTotalPriceFields.clear();
        for (int resId : priceViewIds) {
            mTotalPriceFields.add((TextView) fragmentView.findViewById(resId));
        }
        mTotalPriceDividers.clear();
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

        if (null == mNameField) {
            throw new IllegalStateException("A class extending FamiliarListFragment must call initializeMembers()");
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

    public CardDataAdapter getCardDataAdapter(int adapterIdx) {
        return mCardDataAdapters.get(adapterIdx);
    }

    public void adaptersDeleteSelectedItems() {
        for (CardDataAdapter adapter : mCardDataAdapters) {
            adapter.deleteSelectedItems();
        }
    }

    public void adaptersDeselectAll() {
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
     * Load the price for a given card. This handles all the spice stuff
     *
     * @param data A card to load price info for
     */
    public void loadPrice(final MtgCard data) {

        /* If the priceInfo is already loaded, don't bother performing a query */
        if (data.priceInfo != null) {
            if (data.foil) {
                data.price = (int) (data.priceInfo.mFoilAverage * 100);
            } else {
                switch (getPriceSetting()) {
                    case LOW_PRICE: {
                        data.price = (int) (data.priceInfo.mLow * 100);
                        break;
                    }
                    default:
                    case AVG_PRICE: {
                        data.price = (int) (data.priceInfo.mAverage * 100);
                        break;
                    }
                    case HIGH_PRICE: {
                        data.price = (int) (data.priceInfo.mHigh * 100);
                        break;
                    }
                    case FOIL_PRICE: {
                        data.price = (int) (data.priceInfo.mFoilAverage * 100);
                        break;
                    }
                }
            }
        } else {
            PriceFetchRequest priceRequest = new PriceFetchRequest(data.mName, data.setCode, data.mNumber, -1, getActivity());
            mPriceFetchRequests++;
            getFamiliarActivity().setLoading();
            getFamiliarActivity().mSpiceManager.execute(priceRequest, data.mName + "-" +
                    data.setCode, DurationInMillis.ONE_DAY, new RequestListener<PriceInfo>() {

                /**
                 * Loading the price for this card failed and threw a spiceException
                 *
                 * @param spiceException The exception thrown when trying to load this card's price
                 */
                @Override
                public void onRequestFailure(SpiceException spiceException) {
                /* because this can return when the fragment is in the background */
                    if (FamiliarListFragment.this.isAdded()) {
                        onCardPriceLookupFailure(data, spiceException);
                        mPriceFetchRequests--;
                        if (mPriceFetchRequests == 0) {
                            getFamiliarActivity().clearLoading();
                        }
                        for (CardDataAdapter adapter : mCardDataAdapters) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                }

                /**
                 * Loading the price for this card succeeded. Set it.
                 *
                 * @param result The price for this card
                 */
                @Override
                public void onRequestSuccess(final PriceInfo result) {
                    /* Sanity check */
                    if (result == null) {
                        data.priceInfo = null;
                    } else {
                        /* Set the PriceInfo object */
                        data.priceInfo = result;

                        /* Only reset the price to the downloaded one if the old price isn't custom */
                        if (!data.customPrice) {
                            if (data.foil) {
                                data.price = (int) (result.mFoilAverage * 100);
                            } else {
                                switch (getPriceSetting()) {
                                    case LOW_PRICE: {
                                        data.price = (int) (result.mLow * 100);
                                        break;
                                    }
                                    default:
                                    case AVG_PRICE: {
                                        data.price = (int) (result.mAverage * 100);
                                        break;
                                    }
                                    case HIGH_PRICE: {
                                        data.price = (int) (result.mHigh * 100);
                                        break;
                                    }
                                    case FOIL_PRICE: {
                                        data.price = (int) (result.mFoilAverage * 100);
                                        break;
                                    }
                                }
                            }
                        }
                        /* Clear the message */
                        data.message = null;
                    }

                    /* because this can return when the fragment is in the background */
                    if (FamiliarListFragment.this.isAdded()) {
                        onCardPriceLookupSuccess(data, result);
                        mPriceFetchRequests--;
                        if (mPriceFetchRequests == 0) {
                            getFamiliarActivity().clearLoading();
                        }
                        for (CardDataAdapter adapter : mCardDataAdapters) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            });
        }
    }

    /**
     * Called when a price load fails. Should contain fragment-specific code
     *
     * @param data           The card for which the price lookup failed
     * @param spiceException The exception that occured
     */
    protected abstract void onCardPriceLookupFailure(MtgCard data, SpiceException spiceException);

    /**
     * Called when a price load succeeds. Should contain fragment-specific code
     *
     * @param data   The card for which the price lookup succeeded
     * @param result The price information
     */
    protected abstract void onCardPriceLookupSuccess(MtgCard data, PriceInfo result);

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
    public abstract int getPriceSetting();

    /**
     * @param priceSetting The price setting to write to preferences
     */
    public abstract void setPriceSetting(int priceSetting);
}
