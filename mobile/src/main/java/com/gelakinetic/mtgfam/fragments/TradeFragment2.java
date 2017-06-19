package com.gelakinetic.mtgfam.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.TradeDialogFragment;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * Created by xvicarious on 6/13/17.
 */

public class TradeFragment2 extends FamiliarListFragment {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int BOTH = 2;

    public static final String TRADE_EXTENSION = ".trade";
    public static final String AUTOSAVE_NAME = "autosave";

    private ArrayList<MtgCard> mListLeft;
    private RecyclerView mListViewLeft;
    private CardDataAdapter mListAdapterLeft;

    private ArrayList<MtgCard> mListRight;
    private RecyclerView mListViewRight;
    private CardDataAdapter mListAdapterRight;

    private TextView mTotalPriceLeft;
    private TextView mTotalPriceRight;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View myFragmentView = inflater.inflate(R.layout.trader_frag_2, container, false);

        assert myFragmentView != null;
        mNameField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.name_search);
        mNumberOfField = (EditText) myFragmentView.findViewById(R.id.number_input);
        mCheckboxFoil = (CheckBox) myFragmentView.findViewById(R.id.list_foil);

        mNameField.setAdapter(
                new AutocompleteCursorAdapter(this,
                        new String[]{CardDbAdapter.KEY_NAME},
                        new int[]{R.id.text1}, mNameField,
                        false)
        );

        mListLeft = new ArrayList<>();
        mListAdapterLeft = new CardDataAdapter(mListLeft);
        mListViewLeft = (RecyclerView) myFragmentView.findViewById(R.id.tradeListLeft);
        mListViewLeft.setAdapter(mListAdapterLeft);
        mListViewLeft.setLayoutManager(new LinearLayoutManager(getContext()));

        mListRight = new ArrayList<>();
        mListAdapterRight = new CardDataAdapter(mListRight);
        mListViewRight = (RecyclerView) myFragmentView.findViewById(R.id.tradeListRight);
        mListViewRight.setAdapter(mListAdapterRight);
        mListViewRight.setLayoutManager(new LinearLayoutManager(getContext()));

        mTotalPriceLeft = (TextView) myFragmentView.findViewById(R.id.priceTextLeft);
        mTotalPriceRight = (TextView) myFragmentView.findViewById(R.id.priceTextRight);

        /* Temporary? Just to appease FamiliarListFragment's onPause */
        mListAdapter = mListAdapterLeft;

        myFragmentView.findViewById(R.id.addCardLeft).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addCardToTrade(LEFT);
            }

        });

        myFragmentView.findViewById(R.id.addCardRight).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addCardToTrade(RIGHT);
            }

        });

        setUpCheckBoxClickListeners();

        // todo: swipe-to-delete

        mActionModeCallback = new ActionMode.Callback() {

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.decklist_select_menu, menu); // todo: make an actual menu
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return false; // todo: menu items
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

                mListAdapterLeft.unselectAll();
                mListAdapterRight.unselectAll();

            }

        };

        return myFragmentView;
    }

    private void addCardToTrade(final int side) {

        if (mNameField.getText() == null || mNumberOfField.getText() == null) {
            return;
        }

        final String cardName = mNameField.getText().toString();
        final int numberOf = Integer.parseInt(mNumberOfField.getText().toString());
        final boolean isFoil = mCheckboxFoil.isChecked();

        final MtgCard card = CardHelpers.makeMtgCard(getContext(), cardName, isFoil, numberOf);

        switch (side) {
            case LEFT: {
                mListLeft.add(0, card);
                mListAdapterLeft.notifyItemInserted(0);
                loadPrice(card, mListAdapterLeft);
                break;
            }
            case RIGHT: {
                mListRight.add(0, card);
                mListAdapterRight.notifyItemInserted(0);
                loadPrice(card, mListAdapterRight);
                break;
            }
        }

        mNameField.setText("");
        mNumberOfField.setText("1");

        if (!mCheckboxFoilLocked) {
            mCheckboxFoil.setChecked(false);
        }

        sortTrades(getFamiliarActivity().mPreferenceAdapter.getTradeSortOrder());

    }

    /**
     * Remove any showing dialogs, and show the requested one
     *
     * @param id                The ID of the dialog to show
     * @param sideForDialog     If this is for a specific card, this is the side of the trade the card lives in.
     * @param positionForDialog If this is for a specific card, this is the position of the card in the list.
     */
    public void showDialog(final int id, final int sideForDialog, final int positionForDialog) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        /* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        if (id == TradeDialogFragment.DIALOG_SORT) {
            SortOrderDialogFragment newFragment = new SortOrderDialogFragment();
            Bundle args = new Bundle();
            args.putString(SortOrderDialogFragment.SAVED_SORT_ORDER,
                    getFamiliarActivity().mPreferenceAdapter.getTradeSortOrder());
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
     * Save the current trade to the given filename
     *
     * @param tradeName The name of the trade, to be used as a file name
     */
    public void saveTrade(String tradeName) {
        FileOutputStream fos;

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
            ToastWrapper.makeText(this.getActivity(), R.string.trader_toast_save_error, ToastWrapper.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            ToastWrapper.makeText(this.getActivity(), R.string.trader_toast_invalid_chars, ToastWrapper.LENGTH_LONG).show();
        }
    }

    /**
     * Load a a trade from the given filename
     *
     * @param tradeName The name of the trade to load
     */
    public void loadTrade(String tradeName) {
        try {
            /* Clear the current lists */
            mListLeft.clear();
            mListRight.clear();

            /* Read each card, line by line, load prices along the way */
            BufferedReader br = new BufferedReader(new InputStreamReader(this.getActivity().openFileInput(tradeName)));
            String line;
            while ((line = br.readLine()) != null) {
                MtgCard card = MtgCard.fromTradeString(line, getActivity());

                if (card.setName == null) {
                    handleFamiliarDbException(false);
                    return;
                }
                if (card.mSide == LEFT) {
                    mListLeft.add(card);
                    if (!card.customPrice) {
                        loadPrice(card, mListAdapterLeft);
                    }
                } else if (card.mSide == RIGHT) {
                    mListRight.add(card);
                    if (!card.customPrice) {
                        loadPrice(card, mListAdapterRight);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            /* Do nothing, the autosave doesn't exist */
        } catch (IOException e) {
            ToastWrapper.makeText(this.getActivity(), e.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        }
    }


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
     * Build a plaintext trade and share it
     */
    private void shareTrade() {

        StringBuilder sb = new StringBuilder();

        /* Add all the cards to the StringBuilder from the left, tallying the price */
        int totalPrice = 0;
        for (MtgCard card : mListLeft) {
            totalPrice += card.toTradeShareString(sb, getString(R.string.wishlist_foil));
        }
        sb.append(String.format(Locale.US, "$%d.%02d\n", totalPrice / 100, totalPrice % 100));

        /* Simple divider */
        sb.append("--------\n");

        /* Add all the cards to the StringBuilder from the right, tallying the price */
        totalPrice = 0;
        for (MtgCard card : mListRight) {
            totalPrice += card.toTradeShareString(sb, getString(R.string.wishlist_foil));
        }
        sb.append(String.format(Locale.US, "$%d.%02d", totalPrice / 100, totalPrice % 100));

        /* Send the Intent on it's merry way */
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.trade_share_title);
        sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        sendIntent.setType("text/plain");

        try {
            startActivity(Intent.createChooser(sendIntent, getString(R.string.trader_share)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastWrapper.makeText(getActivity(), getString(R.string.error_no_email_client),
                    ToastWrapper.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.trader_menu, menu);

    }

    @Override
    public void onResume() {

        super.onResume();
        mPriceSetting = Integer.parseInt(getFamiliarActivity().mPreferenceAdapter.getTradePrice());
        loadTrade(AUTOSAVE_NAME + TRADE_EXTENSION);

    }

    @Override
    public void onPause() {

        super.onPause();
        mListAdapterRight.removePendingNow();
        mListAdapterLeft.removePendingNow();
        saveTrade(AUTOSAVE_NAME + TRADE_EXTENSION);

    }

    /**
     * Request the price of a card asynchronously from the network. Save all the returned prices
     *
     * @param data    The card to fetch a price for
     * @param adapter The adapter to notify when a price is downloaded
     */
    public void loadPrice(final MtgCard data, final CardDataAdapter adapter) {
        /* If the priceInfo is already loaded, don't bother performing a query */
        if (data.priceInfo != null) {
            if (data.foil) {
                data.price = (int) (data.priceInfo.mFoilAverage * 100);
            } else {
                switch (mPriceSetting) {
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
            /* priceInfo is null, perform a query */
            PriceFetchRequest priceRequest = new PriceFetchRequest(data.mName, data.setCode, data.mNumber, -1, getActivity());
            mPriceFetchRequests++;
            getFamiliarActivity().setLoading();
            getFamiliarActivity().mSpiceManager.execute(priceRequest, data.mName + "-" + data.setCode,
                    DurationInMillis.ONE_DAY, new RequestListener<PriceInfo>() {
                        /**
                         * This is called when the lookup fails. Set the error message and notify the adapter
                         *
                         * @param spiceException The exception thrown when trying to download the price
                         */
                        @Override
                        public void onRequestFailure(SpiceException spiceException) {
                            if (TradeFragment2.this.isAdded()) {
                                data.message = spiceException.getLocalizedMessage();
                                data.priceInfo = null;
                                adapter.notifyDataSetChanged();
                                mPriceFetchRequests--;
                                if (mPriceFetchRequests == 0) {
                                    getFamiliarActivity().clearLoading();
                                }
                            }
                        }

                        /**
                         * This is called when the lookup succeeds. Save all the prices and set the current price
                         *
                         * @param result The PriceInfo object with the low, average, high, and foil prices
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
                                        switch (mPriceSetting) {
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
                            /* Notify the adapter and update total prices */
                            UpdateTotalPrices(BOTH);
                            adapter.notifyDataSetChanged();
                            mPriceFetchRequests--;
                            if (mPriceFetchRequests == 0 && TradeFragment2.this.isAdded()) {
                                getFamiliarActivity().clearLoading();
                            }
                            sortTrades(getFamiliarActivity().mPreferenceAdapter.getTradeSortOrder());
                        }
                    }
            );
        }
    }

    /**
     * This function iterates through the cards in the given list and sums together their prices
     *
     * @param side RIGHT, LEFT, or BOTH, depending on the side to update
     */
    public void UpdateTotalPrices(int side) {
        if (this.isAdded()) {
            if (side == LEFT || side == BOTH) {
                int totalPrice = 0;
                int totalCards = 0;
                boolean hasBadValues = false;
                /* Iterate through the list and either sum the price or mark it as "bad," (incomplete) */
                for (MtgCard data : mListLeft) {
                    if (data.hasPrice()) {
                        totalCards += data.numberOf;
                        totalPrice += data.numberOf * data.price;
                    } else {
                        hasBadValues = true;
                    }
                }

                /* Set the color whether all values are loaded, and write the text */
                int color = hasBadValues ?
                        ContextCompat.getColor(getContext(), R.color.material_red_500) :
                        ContextCompat.getColor(getContext(),
                                getResourceIdFromAttr(R.attr.color_text));
                mTotalPriceLeft.setText(String.format(Locale.US, "$%d.%02d", totalPrice / 100, totalPrice % 100)
                        + " (" + totalCards + ")");
                mTotalPriceLeft.setTextColor(color);
            }
            if (side == RIGHT || side == BOTH) {
                int totalPrice = 0;
                int totalCards = 0;
                boolean hasBadValues = false;
                /* Iterate through the list and either sum the price or mark it as "bad," (incomplete) */
                for (MtgCard data : mListRight) {
                    if (data.hasPrice()) {
                        totalCards += data.numberOf;
                        totalPrice += data.numberOf * data.price;
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
                mTotalPriceRight.setText(String.format(Locale.US, "$%d.%02d", totalPrice / 100, totalPrice % 100)
                        + " (" + totalCards + ")");
                mTotalPriceRight.setTextColor(color);
            }
        }
    }

    /**
     * Called when the sorting dialog closes. Sort the trades with the new order
     *
     * @param orderByStr The sort order string
     */
    @Override
    public void receiveSortOrder(String orderByStr) {
        getFamiliarActivity().mPreferenceAdapter.setTradeSortOrder(orderByStr);
        sortTrades(orderByStr);
    }

    /**
     * Sort the trades
     */
    private void sortTrades(String sortOrder) {
        /* If no sort type specified, return */
        TradeComparator tradeComparator = new TradeComparator(sortOrder);
        Collections.sort(mListLeft, tradeComparator);
        Collections.sort(mListRight, tradeComparator);
        mListAdapterLeft.notifyDataSetChanged();
        mListAdapterRight.notifyDataSetChanged();
    }

    private static class TradeComparator implements Comparator<MtgCard> {

        final ArrayList<SortOrderDialogFragment.SortOption> options = new ArrayList<>();

        /**
         * Constructor. It parses an "order by" string into search options. The first options have
         * higher priority
         *
         * @param orderByStr The string to parse. It uses SQLite syntax: "KEY asc,KEY2 desc" etc
         */
        TradeComparator(String orderByStr) {
            int idx = 0;
            for (String option : orderByStr.split(",")) {
                String key = option.split(" ")[0];
                boolean ascending = option.split(" ")[1].equalsIgnoreCase(SortOrderDialogFragment.SQL_ASC);
                options.add(new SortOrderDialogFragment.SortOption(null, ascending, key, idx++));
            }
        }

        /**
         * Compare two MtgCard objects based on all the search options in descending priority
         *
         * @param card1 One card to compare
         * @param card2 The other card to compare
         * @return an integer < 0 if card1 is less than card2, 0 if they are equal, and > 0 if card1 is greater than card2.
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
                    }
                } catch (NullPointerException e) {
                    retVal = 0;
                }

                /* Adjust for ascending / descending */
                if (!option.getAscending()) {
                    retVal = -retVal;
                }

                /* If these two entries aren't equal, return. Otherwise continue and compare the
                 * next value
                 */
                if (retVal != 0) {
                    return retVal;
                }
            }

            /* Guess they're totally equal */
            return retVal;
        }

    }

    public class CardDataAdapter extends FamiliarListFragment.CardDataAdapter<MtgCard> {

        CardDataAdapter(ArrayList<MtgCard> values) {
            super(values);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new ViewHolder(viewGroup);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

            final MtgCard item = mItems.get(position);

            final ViewHolder holder = (ViewHolder) viewHolder;

            if (mItemsPendingRemoval.contains(item)) {
                holder.itemView.findViewById(R.id.trade_row).setVisibility(View.GONE);
            } else {
                holder.itemView.findViewById(R.id.trade_row).setVisibility(View.VISIBLE);
                holder.mCardName.setText(item.mName);
                holder.mCardSet.setText(item.setName);
                holder.mCardNumberOf.setText(item.hasPrice() ? item.numberOf + "x" : "");
                holder.mCardFoil.setVisibility(item.foil ? View.VISIBLE : View.GONE);
                if (item.hasPrice()) {
                    if (item.customPrice) {
                        holder.mCardPrice.setTextColor(ContextCompat.getColor(getContext(), R.color.material_green_500));
                    } else {
                        holder.mCardPrice.setTextColor(ContextCompat.getColor(getContext(), getResourceIdFromAttr(R.attr.color_text)));
                    }
                } else {
                    holder.mCardPrice.setTextColor(ContextCompat.getColor(getContext(), R.color.material_red_500));
                }
            }

        }

        class ViewHolder extends FamiliarListFragment.CardDataAdapter.ViewHolder {

            private TextView mCardSet;
            private TextView mCardNumberOf;
            private ImageView mCardFoil;
            private TextView mCardPrice;

            ViewHolder(ViewGroup view) {

                super(view, R.layout.trader_row);

                mCardSet = (TextView) itemView.findViewById(R.id.traderRowSet);
                mCardNumberOf = (TextView) itemView.findViewById(R.id.traderNumber);
                mCardFoil = (ImageView) itemView.findViewById(R.id.traderRowFoil);
                mCardPrice = (TextView) itemView.findViewById(R.id.traderRowPrice);

                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);

            }

        }

    }

}
