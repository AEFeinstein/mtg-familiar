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
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.TradeDialogFragment;
import com.gelakinetic.mtgfam.helpers.CardDataAdapter;
import com.gelakinetic.mtgfam.helpers.CardDataViewHolder;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.octo.android.robospice.persistence.exception.SpiceException;

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
import java.util.Locale;

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
    public ArrayList<MtgCard> mListLeft;

    /* Right List and Company */
    public ArrayList<MtgCard> mListRight;

    public String mCurrentTrade = "";

    private int mOrderAddedIdx = 0;

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
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        /* Inflate the view, pull out the UI elements */
        View myFragmentView = inflater.inflate(R.layout.trader_frag, container, false);

        assert myFragmentView != null;

        mListLeft = new ArrayList<>();
        CardDataAdapter listAdapterLeft = new TradeDataAdapter(mListLeft, LEFT);

        mListRight = new ArrayList<>();
        CardDataAdapter listAdapterRight = new TradeDataAdapter(mListRight, RIGHT);

        /* Call to set up our shared UI elements */
        initializeMembers(
                myFragmentView,
                new int[]{R.id.tradeListLeft, R.id.tradeListRight},
                new CardDataAdapter[]{listAdapterLeft, listAdapterRight},
                new int[]{R.id.priceTextLeft, R.id.priceTextRight},
                new int[]{R.id.priceDividerLeft, R.id.priceDividerRight},
                null
        );

        /* Click listeners to add cards */
        myFragmentView.findViewById(R.id.addCardLeft).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        addCardToTrade(LEFT);
                    }

                });

        myFragmentView.findViewById(R.id.addCardRight).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        addCardToTrade(RIGHT);
                    }

                });

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
        final MtgCard card = CardHelpers.makeMtgCard(getContext(), cardName, null, isFoil, numberOf);

        if (card == null) {
            return;
        }

        card.setIndex(mOrderAddedIdx++);

        switch (side) {
            case LEFT: {
                mListLeft.add(0, card);
                getCardDataAdapter(LEFT).notifyItemInserted(0);
                loadPrice(card);
                break;
            }
            case RIGHT: {
                mListRight.add(0, card);
                getCardDataAdapter(RIGHT).notifyItemInserted(0);
                loadPrice(card);
                break;
            }
            default: {
                return;
            }
        }

        clearCardNameInput();
        clearCardNumberInput();

        uncheckFoilCheckbox();

        sortTrades(PreferenceAdapter.getTradeSortOrder(getContext()));

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
            fos = this.getActivity().openFileOutput(tradeName, Context.MODE_PRIVATE);

            for (MtgCard cd : mListLeft) {
                fos.write(cd.toTradeString(LEFT).getBytes());
            }
            for (MtgCard cd : mListRight) {
                fos.write(cd.toTradeString(RIGHT).getBytes());
            }

            fos.close();
        } catch (IOException e) {
            ToastWrapper.makeAndShowText(this.getActivity(), R.string.trader_toast_save_error,
                    ToastWrapper.LENGTH_LONG);
        } catch (IllegalArgumentException e) {
            ToastWrapper.makeAndShowText(this.getActivity(), R.string.trader_toast_invalid_chars,
                    ToastWrapper.LENGTH_LONG);
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
            /* Clear the current lists */
            mListLeft.clear();
            mListRight.clear();

            /* Read each card, line by line, load prices along the way */
            br = new BufferedReader(
                    new InputStreamReader(this.getActivity().openFileInput(tradeName))
            );
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    MtgCard card = MtgCard.fromTradeString(line, getActivity());
                    card.setIndex(mOrderAddedIdx++);

                    if (card.setName == null) {
                        handleFamiliarDbException(false);
                        return;
                    }
                    if (card.mSide == LEFT) {
                        mListLeft.add(card);
                        if (!card.customPrice) {
                            loadPrice(card);
                        }
                    } else if (card.mSide == RIGHT) {
                        mListRight.add(card);
                        if (!card.customPrice) {
                            loadPrice(card);
                        }
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    // This card line is junk, ignore it
                }
            }
        } catch (FileNotFoundException e) {
            /* Do nothing, the autosave doesn't exist */
        } catch (IOException e) {
            ToastWrapper.makeAndShowText(this.getActivity(), e.getLocalizedMessage(),
                    ToastWrapper.LENGTH_LONG);
        } finally {
            if (br != null) {
                IOUtils.closeQuietly(br);
            }
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
        switch (item.getItemId()) {
            case R.id.trader_menu_clear: {
                showDialog(TradeDialogFragment.DIALOG_CONFIRMATION, 0, 0);
                return true;
            }
            case R.id.trader_menu_settings: {
                showDialog(TradeDialogFragment.DIALOG_PRICE_SETTING, 0, 0);
                return true;
            }
            case R.id.trader_menu_save: {
                showDialog(TradeDialogFragment.DIALOG_SAVE_TRADE, 0, 0);
                return true;
            }
            case R.id.trader_menu_load: {
                showDialog(TradeDialogFragment.DIALOG_LOAD_TRADE, 0, 0);
                return true;
            }
            case R.id.trader_menu_delete: {
                showDialog(TradeDialogFragment.DIALOG_DELETE_TRADE, 0, 0);
                return true;
            }
            case R.id.trader_menu_sort: {
                /* show a dialog to change the sort criteria the list uses */
                showDialog(TradeDialogFragment.DIALOG_SORT, 0, 0);
                return true;
            }
            case R.id.trader_menu_share: {
                shareTrade();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Build a plaintext trade and share it.
     */
    private void shareTrade() {

        StringBuilder sb = new StringBuilder();

        /* Add all the cards to the StringBuilder from the left, tallying the price */
        float totalPrice = 0;
        for (MtgCard card : mListLeft) {
            totalPrice += (card.toTradeShareString(sb, getString(R.string.wishlist_foil)) / 100.0f);
        }
        sb.append(String.format(Locale.US, PRICE_FORMAT + "%n", totalPrice));

        /* Simple divider */
        sb.append("--------\n");

        /* Add all the cards to the StringBuilder from the right, tallying the price */
        totalPrice = 0;
        for (MtgCard card : mListRight) {
            totalPrice += (card.toTradeShareString(sb, getString(R.string.wishlist_foil)) / 100.0f);
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
            ToastWrapper.makeAndShowText(getActivity(), R.string.error_no_email_client,
                    ToastWrapper.LENGTH_SHORT);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

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
        loadTrade(AUTOSAVE_NAME + TRADE_EXTENSION);

    }

    /**
     * When the fragment pauses, save the trade and cancel all pending price requests.
     */
    @Override
    public void onPause() {

        super.onPause();
        saveTrade(AUTOSAVE_NAME + TRADE_EXTENSION);

    }

    @Override
    protected void onCardPriceLookupFailure(MtgCard data, SpiceException spiceException) {
        data.message = spiceException.getLocalizedMessage();
        data.priceInfo = null;
    }

    @Override
    protected void onCardPriceLookupSuccess(MtgCard data, PriceInfo result) {
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
     */
    public void updateTotalPrices(int side) {
        if (this.isAdded()) {
            if (side == LEFT || side == BOTH) {
                float totalPrice = 0;
                int totalCards = 0;
                boolean hasBadValues = false;
                /* Iterate through the list and either sum the price or mark it as
                   "bad," (incomplete) */
                for (MtgCard data : mListLeft) {
                    totalCards += data.numberOf;
                    if (data.hasPrice()) {
                        totalPrice += data.numberOf * (data.price / 100.0f);
                    } else {
                        hasBadValues = true;
                    }
                }

                /* Set the color whether all values are loaded, and write the text */
                int color = hasBadValues ?
                        ContextCompat.getColor(getContext(), R.color.material_red_500) :
                        ContextCompat.getColor(getContext(),
                                getResourceIdFromAttr(R.attr.color_text));
                final String leftPrice =
                        String.format(Locale.US, PRICE_FORMAT, totalPrice)
                                + " (" + totalCards + ")";
                setTotalPrice(leftPrice, color, TradeFragment.LEFT);
            }
            if (side == RIGHT || side == BOTH) {
                float totalPrice = 0;
                int totalCards = 0;
                boolean hasBadValues = false;
                /* Iterate through the list and either sum the price or mark it as "bad,"
                   (incomplete) */
                for (MtgCard data : mListRight) {
                    totalCards += data.numberOf;
                    if (data.hasPrice()) {
                        totalPrice += data.numberOf * (data.price / 100.0f);
                    } else {
                        hasBadValues = true;
                    }
                }

                /* Set the color whether all values are loaded, and write the text */
                int color = hasBadValues ?
                        ContextCompat.getColor(getContext(), R.color.material_red_500) :
                        ContextCompat.getColor(getContext(),
                                getResourceIdFromAttr(R.attr.color_text)
                        );
                final String rightPrice =
                        String.format(Locale.US, PRICE_FORMAT, totalPrice)
                                + " (" + totalCards + ")";
                setTotalPrice(rightPrice, color, TradeFragment.RIGHT);
            }
        }
    }

    @Override
    public boolean shouldShowPrice() {
        return true;
    }

    @Override
    public int getPriceSetting() {
        return Integer.parseInt(PreferenceAdapter.getTradePrice(getContext()));
    }

    @Override
    public void setPriceSetting(int priceSetting) {
        PreferenceAdapter.setTradePrice(getContext(), Integer.toString(priceSetting));
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
        Collections.sort(mListLeft, tradeComparator);
        Collections.sort(mListRight, tradeComparator);
        getCardDataAdapter(LEFT).notifyDataSetChanged();
        getCardDataAdapter(RIGHT).notifyDataSetChanged();
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
                            retVal = card1.mName.compareTo(card2.mName);
                            break;
                        }
                        case CardDbAdapter.KEY_COLOR: {
                            retVal = card1.mColor.compareTo(card2.mColor);
                            break;
                        }
                        case CardDbAdapter.KEY_SUPERTYPE: {
                            retVal = card1.mType.compareTo(card2.mType);
                            break;
                        }
                        case CardDbAdapter.KEY_CMC: {
                            retVal = card1.mCmc - card2.mCmc;
                            break;
                        }
                        case CardDbAdapter.KEY_POWER: {
                            retVal = Float.compare(card1.mPower, card2.mPower);
                            break;
                        }
                        case CardDbAdapter.KEY_TOUGHNESS: {
                            retVal = Float.compare(card1.mToughness, card2.mToughness);
                            break;
                        }
                        case CardDbAdapter.KEY_SET: {
                            retVal = card1.mExpansion.compareTo(card2.mExpansion);
                            break;
                        }
                        case SortOrderDialogFragment.KEY_PRICE: {
                            retVal = Double.compare(card1.price, card2.price);
                            break;
                        }
                        case SortOrderDialogFragment.KEY_ORDER: {
                            retVal = Double.compare(card1.getIndex(), card2.getIndex());
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
        private final TextView mCardNumberOf;
        private final ImageView mCardFoil;
        private final TextView mCardPrice;
        private int mSide;

        TradeViewHolder(ViewGroup view, int side) {

            super(view, R.layout.trader_row, TradeFragment.this.getCardDataAdapter(side), TradeFragment.this, R.menu.action_mode_menu);

            mCardSet = itemView.findViewById(R.id.traderRowSet);
            mCardNumberOf = itemView.findViewById(R.id.traderNumber);
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

        TradeDataAdapter(ArrayList<MtgCard> values, int side) {
            super(values, TradeFragment.this);
            this.side = side;
        }

        @Override
        public TradeViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new TradeViewHolder(viewGroup, side);
        }

        @Override
        protected void onItemReadded() {
            TradeComparator tradeComparator = new TradeComparator(PreferenceAdapter.getTradeSortOrder(getContext()));
            switch (side) {
                case LEFT: {
                    Collections.sort(mListLeft, tradeComparator);
                    break;
                }
                case RIGHT: {
                    Collections.sort(mListRight, tradeComparator);
                    break;
                }
            }
            super.onItemReadded();
        }

        @Override
        public void onBindViewHolder(TradeViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            final MtgCard item = getItem(position);

            holder.itemView.findViewById(R.id.trade_row).setVisibility(View.VISIBLE);
            holder.setCardName(item.mName);
            holder.mCardSet.setText(item.setName);
            holder.mCardNumberOf.setText(item.hasPrice() ? item.numberOf + "x" : "");
            holder.mCardFoil.setVisibility(item.foil ? View.VISIBLE : View.GONE);
            holder.mCardPrice.setText(item.hasPrice() ? item.getPriceString() : item.message);
            if (item.hasPrice()) {
                if (item.customPrice) {
                    holder.mCardPrice.setTextColor(ContextCompat.getColor(getContext(),
                            R.color.material_green_500));
                } else {
                    holder.mCardPrice.setTextColor(ContextCompat.getColor(getContext(),
                            getResourceIdFromAttr(R.attr.color_text)));
                }
            } else {
                holder.mCardPrice.setTextColor(ContextCompat.getColor(getContext(),
                        R.color.material_red_500));
            }
        }
    }
}
