package com.gelakinetic.mtgfam.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.SelectableItemTouchHelper;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

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
    private static final int RIGHT = 1;
    public static final int BOTH = 2;

    public static final String TRADE_EXTENSION = ".trade";
    private static final String AUTOSAVE_NAME = "autosave";

    /* Left List and Company */
    public ArrayList<MtgCard> mListLeft;
    public CardDataAdapter mListAdapterLeft;

    /* Right List and Company */
    public ArrayList<MtgCard> mListRight;
    public CardDataAdapter mListAdapterRight;

    /* Total Price Views */
    private TextView mTotalPriceLeft;
    private TextView mTotalPriceRight;

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
        mNameField = myFragmentView.findViewById(R.id.name_search);
        mNumberOfField = myFragmentView.findViewById(R.id.number_input);
        mCheckboxFoil = myFragmentView.findViewById(R.id.list_foil);

        /* Set up the autocomplete adapter, and default number */
        mNameField.setAdapter(
                new AutocompleteCursorAdapter(this,
                        new String[]{CardDbAdapter.KEY_NAME},
                        new int[]{R.id.text1}, mNameField,
                        false)
        );
        mNumberOfField.setText("1");

        /* Initialize the left list and company */
        mListLeft = new ArrayList<>();
        mListAdapterLeft = new CardDataAdapter(mListLeft, LEFT);
        RecyclerView mListViewLeft = myFragmentView.findViewById(R.id.tradeListLeft);
        mListViewLeft.setAdapter(mListAdapterLeft);
        mListViewLeft.setLayoutManager(new LinearLayoutManager(getContext()));
        ItemTouchHelper.SimpleCallback leftCallback =
                new SelectableItemTouchHelper(mListAdapterLeft, ItemTouchHelper.LEFT);
        ItemTouchHelper leftItemTouchHelper = new ItemTouchHelper(leftCallback);
        leftItemTouchHelper.attachToRecyclerView(mListViewLeft);

        /* Initialize the right list and company */
        mListRight = new ArrayList<>();
        mListAdapterRight = new CardDataAdapter(mListRight, RIGHT);
        RecyclerView mListViewRight = myFragmentView.findViewById(R.id.tradeListRight);
        mListViewRight.setAdapter(mListAdapterRight);
        mListViewRight.setLayoutManager(new LinearLayoutManager(getContext()));
        ItemTouchHelper.SimpleCallback rightCallback =
                new SelectableItemTouchHelper(mListAdapterRight, ItemTouchHelper.LEFT);
        ItemTouchHelper rightItemTouchHelper = new ItemTouchHelper(rightCallback);
        rightItemTouchHelper.attachToRecyclerView(mListViewRight);

        /* Set up the adapters so they know each other */
        mListAdapterLeft.setOtherAdapter(mListAdapterRight);
        mListAdapterRight.setOtherAdapter(mListAdapterLeft);

        /* Total price fields */
        mTotalPriceLeft = myFragmentView.findViewById(R.id.priceTextLeft);
        mTotalPriceRight = myFragmentView.findViewById(R.id.priceTextRight);

        /* Temporary? Just to appease FamiliarListFragment's onPause */
        mListAdapter = mListAdapterLeft;

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

        setUpCheckBoxClickListeners();

        mActionModeCallback = new ActionMode.Callback() {

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.action_mode_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.deck_delete_selected: {
                        mListAdapterLeft.deleteSelectedItems();
                        mListAdapterRight.deleteSelectedItems();
                        mActionMode.finish();
                        return true;
                    }
                    default: {
                        return false;
                    }
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

                mListAdapterLeft.deselectAll();
                mListAdapterRight.deselectAll();

            }

        };

        myFragmentView.findViewById(R.id.camera_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TradeFragment.this.getFamiliarActivity().startTutorCardsSearch();
                    }
                }
        );
        myFragmentView.findViewById(R.id.camera_button).setVisibility(View.GONE);

        return myFragmentView;
    }

    /**
     * This helper method adds a card to a side of the wishlist from the user input.
     *
     * @param side RIGHT or LEFT, depending on which side to add the card to
     */
    private void addCardToTrade(final int side) {

        final String cardName = mNameField.getText().toString();
        final int numberOf = Integer.parseInt(mNumberOfField.getText().toString());
        final boolean isFoil = mCheckboxFoil.isChecked();
        final MtgCard card = CardHelpers.makeMtgCard(getContext(), cardName, null, isFoil, numberOf);

        if (mNameField.getText() == null || mNameField.getText().length() == 0 ||
                mNumberOfField.getText() == null || mNumberOfField.getText().length() == 0 ||
                card == null) {
            return;
        }

        card.setIndex(mOrderAddedIdx++);

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
            default: {
                return;
            }
        }

        mNameField.setText("");
        mNumberOfField.setText("1");

        if (!mCheckboxFoilLocked) {
            mCheckboxFoil.setChecked(false);
        }

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
            ToastWrapper.makeText(this.getActivity(), R.string.trader_toast_save_error,
                    ToastWrapper.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            ToastWrapper.makeText(this.getActivity(), R.string.trader_toast_invalid_chars,
                    ToastWrapper.LENGTH_LONG).show();
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
                            loadPrice(card, mListAdapterLeft);
                        }
                    } else if (card.mSide == RIGHT) {
                        mListRight.add(card);
                        if (!card.customPrice) {
                            loadPrice(card, mListAdapterRight);
                        }
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    // This card line is junk, ignore it
                }
            }
        } catch (FileNotFoundException e) {
            /* Do nothing, the autosave doesn't exist */
        } catch (IOException e) {
            ToastWrapper.makeText(this.getActivity(), e.getLocalizedMessage(),
                    ToastWrapper.LENGTH_LONG).show();
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
        int totalPrice = 0;
        for (MtgCard card : mListLeft) {
            totalPrice += card.toTradeShareString(sb, getString(R.string.wishlist_foil));
        }
        sb.append(String.format(Locale.US, "$%d.%02d%n", totalPrice / 100, totalPrice % 100));

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

    /**
     * When the fragment resumes, get the price preference again and attempt to load the autosave
     * trade.
     */
    @Override
    public void onResume() {

        super.onResume();
        mPriceSetting = Integer.parseInt(PreferenceAdapter.getTradePrice(getContext()));
        loadTrade(AUTOSAVE_NAME + TRADE_EXTENSION);

    }

    /**
     * When the fragment pauses, save the trade and cancel all pending price requests.
     */
    @Override
    public void onPause() {

        super.onPause();
        mListAdapterRight.removePendingNow();
        mListAdapterLeft.removePendingNow();
        saveTrade(AUTOSAVE_NAME + TRADE_EXTENSION);

    }

    /**
     * Request the price of a card asynchronously from the network. Save all the returned prices.
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
            PriceFetchRequest priceRequest = new PriceFetchRequest(data.mName, data.setCode,
                    data.mNumber, -1, getActivity());
            mPriceFetchRequests++;
            getFamiliarActivity().setLoading();
            getFamiliarActivity().mSpiceManager.execute(priceRequest,
                    data.mName + "-" + data.setCode, DurationInMillis.ONE_DAY,
                    new RequestListener<PriceInfo>() {

                        /**
                         * This is called when the lookup fails. Set the error message and notify
                         * the adapter.
                         *
                         * @param spiceException The exception thrown when trying to download the
                         *                       price
                         */
                        @Override
                        public void onRequestFailure(SpiceException spiceException) {
                            if (TradeFragment.this.isAdded()) {
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
                         * This is called when the lookup succeeds. Save all the prices and set the
                         * current price.
                         *
                         * @param result The PriceInfo object with the low, average, high, and foil
                         *               prices
                         */
                        @Override
                        public void onRequestSuccess(final PriceInfo result) {
                            /* Sanity check */
                            if (result == null) {
                                data.priceInfo = null;
                            } else {
                                /* Set the PriceInfo object */
                                data.priceInfo = result;

                                /* Only reset the price to the downloaded one if the old price
                                   isn't custom */
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
                            updateTotalPrices(BOTH);
                            adapter.notifyDataSetChanged();
                            mPriceFetchRequests--;
                            if (mPriceFetchRequests == 0 && TradeFragment.this.isAdded()) {
                                getFamiliarActivity().clearLoading();
                            }
                            try {
                                sortTrades(PreferenceAdapter.getTradeSortOrder(getContext()));
                            } catch (NullPointerException e) {
                                /* couldn't get the preference, so don't bother sorting */
                            }
                        }
                    }
            );
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
                int totalPrice = 0;
                int totalCards = 0;
                boolean hasBadValues = false;
                /* Iterate through the list and either sum the price or mark it as
                   "bad," (incomplete) */
                for (MtgCard data : mListLeft) {
                    if (!mListAdapterLeft.isItemPendingRemoval(mListLeft.indexOf(data))) {
                        if (data.hasPrice()) {
                            totalCards += data.numberOf;
                            totalPrice += data.numberOf * data.price;
                        } else {
                            hasBadValues = true;
                        }
                    }
                }

                /* Set the color whether all values are loaded, and write the text */
                int color = hasBadValues ?
                        ContextCompat.getColor(getContext(), R.color.material_red_500) :
                        ContextCompat.getColor(getContext(),
                                getResourceIdFromAttr(R.attr.color_text));
                final String leftPrice =
                        String.format(Locale.US, "%d.%02d", totalPrice / 100, totalPrice % 100)
                                + " (" + totalCards + ")";
                mTotalPriceLeft.setText(leftPrice);
                mTotalPriceLeft.setTextColor(color);
            }
            if (side == RIGHT || side == BOTH) {
                int totalPrice = 0;
                int totalCards = 0;
                boolean hasBadValues = false;
                /* Iterate through the list and either sum the price or mark it as "bad,"
                   (incomplete) */
                for (MtgCard data : mListRight) {
                    if (!mListAdapterRight.isItemPendingRemoval(mListRight.indexOf(data))) {
                        if (data.hasPrice()) {
                            totalCards += data.numberOf;
                            totalPrice += data.numberOf * data.price;
                        } else {
                            hasBadValues = true;
                        }
                    }
                }

                /* Set the color whether all values are loaded, and write the text */
                int color = hasBadValues ?
                        ContextCompat.getColor(getContext(), R.color.material_red_500) :
                        ContextCompat.getColor(getContext(),
                                getResourceIdFromAttr(R.attr.color_text)
                        );
                final String rightPrice =
                        String.format(Locale.US, "$%d.%02d", totalPrice / 100, totalPrice % 100)
                                + " (" + totalCards + ")";
                mTotalPriceRight.setText(rightPrice);
                mTotalPriceRight.setTextColor(color);
            }
        }
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
        mListAdapterLeft.notifyDataSetChanged();
        mListAdapterRight.notifyDataSetChanged();
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

    /**
     * Adapter to display the cards in each list.
     */
    public class CardDataAdapter
            extends FamiliarListFragment.CardDataAdapter<MtgCard, CardDataAdapter.ViewHolder> {

        private CardDataAdapter otherAdapter;

        private final int side;

        CardDataAdapter(ArrayList<MtgCard> values, int side) {
            super(values);
            this.side = side;
        }

        void setOtherAdapter(CardDataAdapter adapter) {
            otherAdapter = adapter;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new ViewHolder(viewGroup);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            final MtgCard item = items.get(position);

            if (isItemPendingRemoval(position)) {
                holder.itemView.findViewById(R.id.trade_row).setVisibility(View.GONE);
            } else {
                holder.itemView.setSelected(selectedItems.get(position, false));
                holder.itemView.findViewById(R.id.trade_row).setVisibility(View.VISIBLE);
                holder.mCardName.setText(item.mName);
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

        @Override
        public void onItemDismissed(final int position) {
            super.onItemDismissed(position);
            updateTotalPrices(BOTH);
        }

        @Override
        public void onUndoDelete(final int position) {
            updateTotalPrices(BOTH);
        }

        @Override
        public String getItemName(int position) {
            return items.get(position).mName;
        }

        class ViewHolder extends FamiliarListFragment.CardDataAdapter.ViewHolder {

            private final TextView mCardSet;
            private final TextView mCardNumberOf;
            private final ImageView mCardFoil;
            private final TextView mCardPrice;

            ViewHolder(ViewGroup view) {

                super(view, R.layout.trader_row);

                mCardSet = itemView.findViewById(R.id.traderRowSet);
                mCardNumberOf = itemView.findViewById(R.id.traderNumber);
                mCardFoil = itemView.findViewById(R.id.traderRowFoil);
                mCardPrice = itemView.findViewById(R.id.traderRowPrice);

                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);

            }

            @Override
            public void onClick(View view) {

                if (!isInSelectMode() && !otherAdapter.isInSelectMode()) {
                    showDialog(
                            TradeDialogFragment.DIALOG_UPDATE_CARD,
                            side,
                            getAdapterPosition()
                    );
                }

                super.onClick(view);

            }

            /**
             * If the other adapter is in select mode, don't enter select mode
             *
             * @param view view being clicked
             * @return onLongClick
             */
            @Override
            public boolean onLongClick(View view) {
                return !otherAdapter.isInSelectMode() && super.onLongClick(view);
            }
        }

    }

}
