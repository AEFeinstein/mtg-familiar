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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.TradeDialogFragment;
import com.gelakinetic.mtgfam.helpers.CardDataAdapter;
import com.gelakinetic.mtgfam.helpers.CardDataViewHolder;
import com.gelakinetic.mtgfam.helpers.ExpansionImageHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * This class manages trades between two users. Trades can be saved and loaded.
 */
public class TradeFragment extends FamiliarListFragment {

    /* Side constants */
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int BOTH = 2;

    public static final String TRADE_EXTENSION = ".trade";
    private static final String AUTOSAVE_NAME = "autosave";

    /* Left List and Company */
    public final List<MtgCard> mListLeft = Collections.synchronizedList(new ArrayList<>());

    /* Right List and Company */
    public final List<MtgCard> mListRight = Collections.synchronizedList(new ArrayList<>());

    public String mCurrentTrade = AUTOSAVE_NAME;

    private int mOrderAddedIdx = 0;
    private TextView mTradeNameView;
    private TextView mTradePriceDifference;

    /**
     * Initialize the view and set up the button actions.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in
     *                           the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should
     *                           be attached to. The fragment should not add the view itself, but
     *                           this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return The inflated view
     */
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        /* Inflate the view, pull out the UI elements */
        View myFragmentView = inflater.inflate(R.layout.trader_frag, container, false);

        assert myFragmentView != null;

        synchronized (mListRight) {
            synchronized (mListLeft) {
                CardDataAdapter listAdapterLeft = new TradeDataAdapter(mListLeft, LEFT);
                CardDataAdapter listAdapterRight = new TradeDataAdapter(mListRight, RIGHT);

                /* Call to set up our shared UI elements */
                initializeMembers(
                        myFragmentView,
                        new int[]{R.id.tradeListLeft, R.id.tradeListRight},
                        new CardDataAdapter[]{listAdapterLeft, listAdapterRight},
                        new int[]{R.id.priceTextLeft, R.id.priceTextRight},
                        new int[]{R.id.trade_price_divider_left, R.id.trade_price_divider_right}, R.menu.action_mode_menu,
                        null);
            }
        }

        /* Click listeners to add cards */
        myFragmentView.findViewById(R.id.addCardLeft).setOnClickListener(
                v -> addCardToTrade(LEFT));

        myFragmentView.findViewById(R.id.addCardRight).setOnClickListener(
                v -> addCardToTrade(RIGHT));

        mTradeNameView = myFragmentView.findViewById(R.id.trade_name);
        mTradePriceDifference = myFragmentView.findViewById(R.id.trade_price_diff);

        return myFragmentView;
    }

    /**
     * This helper method adds a card to a side of the wishlist from the user input.
     *
     * @param side RIGHT or LEFT, depending on which side to add the card to
     */
    private void addCardToTrade(final int side) {

        if (getCardNameInput() == null || getCardNameInput().length() == 0 ||
                getCardNumberInput() == null || getCardNumberInput().length() == 0) {
            return;
        }

        final String cardName = getCardNameInput().toString();
        final int numberOf = Integer.parseInt(getCardNumberInput().toString());
        final boolean isFoil = checkboxFoilIsChecked();
        try {
            final MtgCard card = new MtgCard(getActivity(), cardName, null, isFoil, numberOf);

            card.setIndex(mOrderAddedIdx++);

            switch (side) {
                case LEFT: {
                    synchronized (mListLeft) {
                        mListLeft.add(0, card);
                    }
                    getCardDataAdapter(LEFT).notifyItemInserted(0);
                    loadPrice(card);
                    break;
                }
                case RIGHT: {
                    synchronized (mListRight) {
                        mListRight.add(0, card);
                    }
                    getCardDataAdapter(RIGHT).notifyItemInserted(0);
                    loadPrice(card);
                    break;
                }
                default: {
                    return;
                }
            }

            clearCardNameInput();

            /* Don't reset the count after adding a card. This makes adding consecutive 4-ofs easier */
            /* clearCardNumberInput(); */

            uncheckFoilCheckbox();

            sortTrades(PreferenceAdapter.getTradeSortOrder(getContext()));
        } catch (java.lang.InstantiationException e) {
            /* Eat it */
        }
    }

    /**
     * Remove any showing dialogs, and show the requested one.
     *
     * @param id                The ID of the dialog to show.
     * @param sideForDialog     If this is for a specific card, this is the side of the trade the
     *                          card lives in.
     * @param positionForDialog If this is for a specific card, this is the position of the card in
     *                          the list.
     */
    public void showDialog(final int id, final int sideForDialog, final int positionForDialog)
            throws IllegalStateException {

        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also
           want to remove any currently showing dialog, so make our own transaction and take care
           of that here. */

        /* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        if (id == TradeDialogFragment.DIALOG_SORT) {
            SortOrderDialogFragment newFragment = new SortOrderDialogFragment();
            Bundle args = new Bundle();
            args.putString(SortOrderDialogFragment.SAVED_SORT_ORDER,
                    PreferenceAdapter.getTradeSortOrder(getContext()));
            newFragment.setArguments(args);
            newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
        } else {
            /* Create and show the dialog. */
            TradeDialogFragment newFragment = new TradeDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
            arguments.putInt(TradeDialogFragment.ID_SIDE, sideForDialog);
            arguments.putInt(TradeDialogFragment.ID_POSITION, positionForDialog);
            newFragment.setArguments(arguments);
            newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
        }

    }

    /**
     * Save the current trade to the given filename.
     *
     * @param tradeName The name of the trade, to be used as a file name
     */
    public void saveTrade(String tradeName) {
        FileOutputStream fos;

        /* Revert to added-order before saving */
        sortTrades(SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_ASC);

        try {
            /* MODE_PRIVATE will create the file (or replace a file of the same name) */
            fos = Objects.requireNonNull(this.getActivity()).openFileOutput(tradeName, Context.MODE_PRIVATE);

            synchronized (mListLeft) {
                for (MtgCard cd : mListLeft) {
                    fos.write(cd.toTradeString(LEFT).getBytes());
                }
            }
            synchronized (mListRight) {
                for (MtgCard cd : mListRight) {
                    fos.write(cd.toTradeString(RIGHT).getBytes());
                }
            }

            fos.close();
        } catch (IOException e) {
            SnackbarWrapper.makeAndShowText(this.getActivity(), R.string.trader_toast_save_error,
                    SnackbarWrapper.LENGTH_LONG);
        } catch (IllegalArgumentException e) {
            SnackbarWrapper.makeAndShowText(this.getActivity(), R.string.trader_toast_invalid_chars,
                    SnackbarWrapper.LENGTH_LONG);
        }

        /* And resort to the expected order after saving */
        sortTrades(PreferenceAdapter.getTradeSortOrder(getContext()));
    }

    /**
     * Load a a trade from the given filename.
     *
     * @param tradeName The name of the trade to load
     */
    public void loadTrade(String tradeName) {
        BufferedReader br = null;
        try {
            synchronized (mListLeft) {
                synchronized (mListRight) {
                    /* Clear the current lists */
                    mListLeft.clear();
                    mListRight.clear();

                    /* Read each card, line by line, load prices along the way */
                    br = new BufferedReader(
                            new InputStreamReader(Objects.requireNonNull(this.getActivity()).openFileInput(tradeName))
                    );
                    String line;
                    while ((line = br.readLine()) != null) {
                        try {
                            MtgCard card = MtgCard.fromTradeString(line, getActivity());
                            if (null != card) {
                                card.setIndex(mOrderAddedIdx++);

                                if (card.mSide == LEFT) {
                                    mListLeft.add(card);
                                } else if (card.mSide == RIGHT) {
                                    mListRight.add(card);
                                }
                            }
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            // This card line is junk, ignore it
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            /* Do nothing, the autosave doesn't exist */
        } catch (IOException | IllegalArgumentException e) {
            SnackbarWrapper.makeAndShowText(this.getActivity(), e.getLocalizedMessage(),
                    SnackbarWrapper.LENGTH_LONG);
        } finally {
            if (br != null) {
                IOUtils.closeQuietly(br);
            }
        }

        // Now that all the file IO is done, hit the database twice, once for each side
        try {
            synchronized (mListLeft) {
                MtgCard.initCardListFromDb(getContext(), mListLeft);
                for (MtgCard card : mListLeft) {
                    if (!card.mIsCustomPrice) {
                        loadPrice(card);
                    }
                }
            }

            synchronized (mListRight) {
                MtgCard.initCardListFromDb(getContext(), mListRight);
                for (MtgCard card : mListRight) {
                    if (!card.mIsCustomPrice) {
                        loadPrice(card);
                    }
                }
            }
        } catch (FamiliarDbException fde) {
            handleFamiliarDbException(true);
        }
    }

    /**
     * Handle an ActionBar item click.
     *
     * @param item the item clicked
     * @return true if the click was acted on
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle item selection */
        if (item.getItemId() == R.id.trader_menu_clear) {
            showDialog(TradeDialogFragment.DIALOG_CONFIRMATION, 0, 0);
            return true;
        } else if (item.getItemId() == R.id.trader_menu_settings) {
            showDialog(TradeDialogFragment.DIALOG_PRICE_SETTING, 0, 0);
            return true;
        } else if (item.getItemId() == R.id.trader_menu_save_as) {
            showDialog(TradeDialogFragment.DIALOG_SAVE_TRADE_AS, 0, 0);
            return true;
        } else if (item.getItemId() == R.id.trader_menu_new) {
            showDialog(TradeDialogFragment.DIALOG_NEW_TRADE, 0, 0);
            return true;
        } else if (item.getItemId() == R.id.trader_menu_load) {
            showDialog(TradeDialogFragment.DIALOG_LOAD_TRADE, 0, 0);
            return true;
        } else if (item.getItemId() == R.id.trader_menu_delete) {
            showDialog(TradeDialogFragment.DIALOG_DELETE_TRADE, 0, 0);
            return true;
        } else if (item.getItemId() == R.id.trader_menu_sort) {
            /* show a dialog to change the sort criteria the list uses */
            showDialog(TradeDialogFragment.DIALOG_SORT, 0, 0);
            return true;
        } else if (item.getItemId() == R.id.trader_menu_share) {
            shareTrade();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Build a plaintext trade and share it.
     */
    private void shareTrade() {

        StringBuilder sb = new StringBuilder();

        /* Add all the cards to the StringBuilder from the left, tallying the price */
        float totalPrice = 0;
        synchronized (mListLeft) {
            for (MtgCard card : mListLeft) {
                totalPrice += (card.toTradeShareString(sb, getString(R.string.wishlist_foil)) / 100.0f);
            }
        }
        sb.append(String.format(Locale.US, PRICE_FORMAT + "%n", totalPrice));

        /* Simple divider */
        sb.append("--------\n");

        /* Add all the cards to the StringBuilder from the right, tallying the price */
        totalPrice = 0;
        synchronized (mListRight) {
            for (MtgCard card : mListRight) {
                totalPrice += (card.toTradeShareString(sb, getString(R.string.wishlist_foil)) / 100.0f);
            }
        }
        sb.append(String.format(Locale.US, PRICE_FORMAT, totalPrice));

        /* Send the Intent on it's merry way */
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.trade_share_title);
        sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        sendIntent.setType("text/plain");

        try {
            startActivity(Intent.createChooser(sendIntent, getString(R.string.trader_share)));
        } catch (android.content.ActivityNotFoundException ex) {
            SnackbarWrapper.makeAndShowText(getActivity(), R.string.error_no_email_client,
                    SnackbarWrapper.LENGTH_SHORT);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.trader_menu, menu);

    }

    /**
     * When the fragment resumes, get the price preference again and attempt to load the autosave
     * trade.
     */
    @Override
    public void onResume() {

        super.onResume();

        // Get the last loaded trade
        setTradeName(PreferenceAdapter.getLastLoadedTrade(getContext()));
        if (mCurrentTrade.isEmpty()) {
            // If it's empty, use autosave instead
            setTradeName(AUTOSAVE_NAME);
        }
        loadTrade(mCurrentTrade + TRADE_EXTENSION);
    }

    /**
     * Set the current trade name and display it
     *
     * @param tradeName the name to set
     */
    public void setTradeName(String tradeName) {
        mCurrentTrade = tradeName;
        mTradeNameView.setText(tradeName);
    }

    /**
     * When the fragment pauses, save the trade and cancel all pending price requests.
     */
    @Override
    public void onPause() {

        super.onPause();
        // If for some reason there is no trade name, use autosave
        if (mCurrentTrade.isEmpty()) {
            setTradeName(AUTOSAVE_NAME);
        }
        // Save the current name and trade
        PreferenceAdapter.setLastLoadedTrade(getContext(), mCurrentTrade);
        saveTrade(mCurrentTrade + TRADE_EXTENSION);
    }

    @Override
    protected void onCardPriceLookupFailure(MtgCard data, Throwable exception) {
        // Do nothing, wait until all prices are fetched
    }

    @Override
    protected void onCardPriceLookupSuccess(MtgCard data, MarketPriceInfo result) {
        // Do nothing, wait until all prices are fetched
    }

    @Override
    protected void onAllPriceLookupsFinished() {
        updateTotalPrices(BOTH);
        try {
            sortTrades(PreferenceAdapter.getTradeSortOrder(getContext()));
        } catch (NullPointerException e) {
            /* couldn't get the preference, so don't bother sorting */
        }
    }

    /**
     * This function iterates through the cards in the given list and sums together their prices.
     *
     * @param side RIGHT, LEFT, or BOTH, depending on the side to update
     *             Always update both sides, regardless
     */
    public void updateTotalPrices(int side) {
        if (this.isAdded()) {
            float leftPrice = 0;
            float rightPrice = 0;
            {
                // First do the left side
                int totalCards = 0;
                boolean hasBadValues = false;
                /* Iterate through the list and either sum the price or mark it as
                   "bad," (incomplete) */
                synchronized (mListLeft) {
                    for (MtgCard data : mListLeft) {
                        totalCards += data.mNumberOf;
                        if (data.hasPrice()) {
                            leftPrice += data.mNumberOf * (data.mPrice / 100.0f);
                        } else {
                            hasBadValues = true;
                        }
                    }
                }

                /* Set the color whether all values are loaded, and write the text */
                int color = hasBadValues ?
                        ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.material_red_500) :
                        ContextCompat.getColor(Objects.requireNonNull(getContext()),
                                getResourceIdFromAttr(R.attr.color_text));
                final String leftPriceStr =
                        String.format(Locale.US, PRICE_FORMAT, leftPrice)
                                + " (" + totalCards + ")";
                setTotalPrice(leftPriceStr, color, TradeFragment.LEFT);
            }
            {
                // Then do the right side
                int totalCards = 0;
                boolean hasBadValues = false;
                /* Iterate through the list and either sum the price or mark it as "bad,"
                   (incomplete) */
                synchronized (mListRight) {
                    for (MtgCard data : mListRight) {
                        totalCards += data.mNumberOf;
                        if (data.hasPrice()) {
                            rightPrice += data.mNumberOf * (data.mPrice / 100.0f);
                        } else {
                            hasBadValues = true;
                        }
                    }
                }

                /* Set the color whether all values are loaded, and write the text */
                int color = hasBadValues ?
                        ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.material_red_500) :
                        ContextCompat.getColor(Objects.requireNonNull(getContext()),
                                getResourceIdFromAttr(R.attr.color_text)
                        );
                final String rightPriceStr =
                        String.format(Locale.US, PRICE_FORMAT, rightPrice)
                                + " (" + totalCards + ")";
                setTotalPrice(rightPriceStr, color, TradeFragment.RIGHT);
            }

            // Display the difference
            float priceDiff = leftPrice - rightPrice;
            mTradePriceDifference.setText(String.format(Locale.getDefault(), "%c" + PRICE_FORMAT, (priceDiff < 0 ? '-' : '+'), Math.abs(priceDiff)));
            if (priceDiff < 0) {
                mTradePriceDifference.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()),
                        R.color.material_red_500));
            } else {
                mTradePriceDifference.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()),
                        R.color.material_green_500));
            }
        }
    }

    @Override
    public boolean shouldShowPrice() {
        return true;
    }

    @Override
    public MarketPriceInfo.PriceType getPriceSetting() {
        return PreferenceAdapter.getTradePrice(getContext());
    }

    @Override
    public void setPriceSetting(MarketPriceInfo.PriceType priceSetting) {
        PreferenceAdapter.setTradePrice(getContext(), priceSetting);
    }

    /**
     * Called when the sorting dialog closes. Sort the trades with the new order.
     *
     * @param orderByStr The sort order string
     */
    @Override
    public void receiveSortOrder(String orderByStr) {
        PreferenceAdapter.setTradeSortOrder(getContext(), orderByStr);
        sortTrades(orderByStr);
    }

    /**
     * Sort the trades.
     */
    private void sortTrades(String sortOrder) {
        /* If no sort type specified, return */
        TradeComparator tradeComparator = new TradeComparator(sortOrder);
        synchronized (mListLeft) {
            Collections.sort(mListLeft, tradeComparator);
        }
        synchronized (mListRight) {
            Collections.sort(mListRight, tradeComparator);
        }
        getCardDataAdapter(LEFT).notifyDataSetChanged();
        getCardDataAdapter(RIGHT).notifyDataSetChanged();
    }

    /**
     * Clear the current trade
     */
    public void clearTrade(boolean preserveName) {
        /* Clear the arrays and tell everything to update */
        if (!preserveName) {
            setTradeName(AUTOSAVE_NAME);
        }
        synchronized (mListRight) {
            mListRight.clear();
        }
        synchronized (mListLeft) {
            mListLeft.clear();
        }
        getCardDataAdapter(TradeFragment.RIGHT).notifyDataSetChanged();
        getCardDataAdapter(TradeFragment.LEFT).notifyDataSetChanged();
        updateTotalPrices(TradeFragment.BOTH);
        clearCardNameInput();
        clearCardNumberInput();
        uncheckFoilCheckbox();
    }

    private static class TradeComparator implements Comparator<MtgCard>, Serializable {

        final ArrayList<SortOrderDialogFragment.SortOption> options = new ArrayList<>();

        /**
         * Constructor. It parses an "order by" string into search options. The first options have
         * higher priority.
         *
         * @param orderByStr The string to parse. It uses SQLite syntax: "KEY asc,KEY2 desc" etc
         */
        TradeComparator(String orderByStr) {
            int idx = 0;
            for (String option : orderByStr.split(",")) {
                String key = option.split(" ")[0];
                boolean ascending =
                        option.split(" ")[1].equalsIgnoreCase(SortOrderDialogFragment.SQL_ASC);
                options.add(new SortOrderDialogFragment.SortOption(null, ascending, key, idx++));
            }
        }

        /**
         * Compare two MtgCard objects based on all the search options in descending priority.
         *
         * @param card1 One card to compare
         * @param card2 The other card to compare
         * @return an integer < 0 if card1 is less than card2, 0 if they are equal, and > 0 if card1
         * is greater than card2.
         */
        @Override
        public int compare(MtgCard card1, MtgCard card2) {

            int retVal = 0;
            /* Iterate over all the sort options, starting with the high priority ones */
            for (SortOrderDialogFragment.SortOption option : options) {
                try {
                    /* Compare the entries based on the key */
                    switch (option.getKey()) {
                        case CardDbAdapter.KEY_NAME: {
                            retVal = card1.getName().compareTo(card2.getName());
                            break;
                        }
                        case CardDbAdapter.KEY_COLOR: {
                            retVal = card1.getColor().compareTo(card2.getColor());
                            break;
                        }
                        case CardDbAdapter.KEY_SUPERTYPE: {
                            retVal = card1.getType().compareTo(card2.getType());
                            break;
                        }
                        case CardDbAdapter.KEY_CMC: {
                            retVal = Integer.compare(card1.getCmc(), card2.getCmc());
                            break;
                        }
                        case CardDbAdapter.KEY_POWER: {
                            retVal = Float.compare(card1.getPower(), card2.getPower());
                            break;
                        }
                        case CardDbAdapter.KEY_TOUGHNESS: {
                            retVal = Float.compare(card1.getToughness(), card2.getToughness());
                            break;
                        }
                        case CardDbAdapter.KEY_SET: {
                            retVal = card1.getExpansion().compareTo(card2.getExpansion());
                            break;
                        }
                        case SortOrderDialogFragment.KEY_PRICE: {
                            retVal = Double.compare(card1.mPrice, card2.mPrice);
                            break;
                        }
                        case SortOrderDialogFragment.KEY_ORDER: {
                            retVal = Double.compare(card1.getIndex(), card2.getIndex());
                            break;
                        }
                        case CardDbAdapter.KEY_RARITY: {
                            retVal = Character.compare(card1.getRarity(), card1.getRarity());
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                } catch (NullPointerException e) {
                    retVal = 0;
                }

                /* Adjust for ascending / descending */
                if (!option.getAscending()) {
                    retVal = -retVal;
                }

                /* If these two entries aren't equal, return. Otherwise continue and compare the
                 * next value.
                 */
                if (retVal != 0) {
                    return retVal;
                }
            }

            /* Guess they're totally equal */
            return retVal;
        }

    }

    class TradeViewHolder extends CardDataViewHolder {

        private final TextView mCardSet;
        private final ImageView mCardSetImage;
        private final ImageView mCardFoil;
        private final TextView mCardPrice;
        private final int mSide;

        TradeViewHolder(ViewGroup view, int side) {

            super(view, R.layout.trader_row, TradeFragment.this.getCardDataAdapter(side), TradeFragment.this);

            mCardSet = itemView.findViewById(R.id.traderRowSet);
            mCardSetImage = itemView.findViewById(R.id.traderRowSetImage);
            mCardFoil = itemView.findViewById(R.id.traderRowFoil);
            mCardPrice = itemView.findViewById(R.id.traderRowPrice);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            mSide = side;
        }

        @Override
        public void onClickNotSelectMode(View view, int position) {
            showDialog(
                    TradeDialogFragment.DIALOG_UPDATE_CARD,
                    mSide,
                    position
            );
        }
    }

    /**
     * Adapter to display the cards in each list.
     */
    public class TradeDataAdapter extends CardDataAdapter<MtgCard, TradeViewHolder> {

        private final int side;

        TradeDataAdapter(List<MtgCard> values, int side) {
            super(values, TradeFragment.this);
            this.side = side;
        }

        @NonNull
        @Override
        public TradeViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new TradeViewHolder(viewGroup, side);
        }

        @Override
        protected void onItemReadded() {
            TradeComparator tradeComparator = new TradeComparator(PreferenceAdapter.getTradeSortOrder(getContext()));
            synchronized (this.items) {
                Collections.sort(this.items, tradeComparator);
            }
            super.onItemReadded();
        }

        @Override
        public void onBindViewHolder(@NonNull TradeViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            final MtgCard item = getItem(position);

            holder.itemView.findViewById(R.id.trade_row).setVisibility(View.VISIBLE);
            holder.setCardName(Objects.requireNonNull(item).getName());
            holder.mCardSet.setText(item.getSetName());
            ExpansionImageHelper.loadExpansionImage(getContext(), item.getExpansion(), item.getRarity(), holder.mCardSetImage, null, ExpansionImageHelper.ExpansionImageSize.SMALL);
            holder.mCardFoil.setVisibility(item.mIsFoil ? View.VISIBLE : View.GONE);
            if (item.hasPrice()) {
                holder.mCardPrice.setText(String.format(Locale.getDefault(), "%dx %s", item.mNumberOf, item.getPriceString()));
                if (item.mIsCustomPrice) {
                    holder.mCardPrice.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()),
                            R.color.material_green_500));
                } else {
                    holder.mCardPrice.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()),
                            getResourceIdFromAttr(R.attr.color_text)));
                }
            } else {
                holder.mCardPrice.setText(String.format(Locale.getDefault(), "%dx %s", item.mNumberOf, item.mMessage));
                holder.mCardPrice.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()),
                        R.color.material_red_500));
            }
        }
    }
}
