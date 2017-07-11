package com.gelakinetic.mtgfam.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.TradeDialogFragment;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

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
 * This class manages trades between two users. Trades can be saved and loaded
 */
public class TradeFragment extends FamiliarFragment {

    /* Price Constants */
    public static final int LOW_PRICE = 0;
    public static final int AVG_PRICE = 1;
    public static final int HIGH_PRICE = 2;
    public static final int FOIL_PRICE = 3;

    /* Side Constants */
    public static final int LEFT = 0;
    public static final int BOTH = 2;
    public static final String TRADE_EXTENSION = ".trade";
    private static final int RIGHT = 1;
    /* Save file constants */
    private static final String AUTOSAVE_NAME = "autosave";

    /* Lists and adapters */
    public TradeListAdapter mLeftAdapter;
    public ArrayList<MtgCard> mLeftList;
    public TradeListAdapter mRightAdapter;
    public ArrayList<MtgCard> mRightList;
    public CheckBox mCheckboxFoil;
    private int mOrderAddedIndex;
    /* Settings */
    public int mPriceSetting;
    public String mCurrentTrade = "";
    /* Trade information */
    private TextView mTotalPriceLeft;
    private TextView mTotalPriceRight;
    /* UI Elements */
    private AutoCompleteTextView mNameEditText;
    private EditText mNumberEditText;
    private int mPriceFetchRequests = 0;
    private boolean mCheckboxFoilLocked = false;

    /**
     * Initialize the view and set up the button actions
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The inflated view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /* Inflate the view, pull out UI elements */
        View myFragmentView = inflater.inflate(R.layout.trader_frag, container, false);
        assert myFragmentView != null;
        mNameEditText = (AutoCompleteTextView) myFragmentView.findViewById(R.id.namesearch);
        mNumberEditText = (EditText) myFragmentView.findViewById(R.id.numberInput);
        mCheckboxFoil = (CheckBox) myFragmentView.findViewById(R.id.trader_foil);
        mTotalPriceRight = (TextView) myFragmentView.findViewById(R.id.priceTextRight);
        mTotalPriceLeft = (TextView) myFragmentView.findViewById(R.id.priceTextLeft);

        /* Set the autocomplete adapter, default number */
        mNameEditText.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME},
                new int[]{R.id.text1}, mNameEditText, false));
        mNumberEditText.setText("1");

        /* Initialize the left list */
        mLeftList = new ArrayList<>();
        mLeftAdapter = new TradeListAdapter(this.getActivity(), mLeftList);
        ListView lvTradeLeft = (ListView) myFragmentView.findViewById(R.id.tradeListLeft);
        lvTradeLeft.setAdapter(mLeftAdapter);
        lvTradeLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                showDialog(TradeDialogFragment.DIALOG_UPDATE_CARD, LEFT, arg2);
            }
        });

        /* Initialize the right list */
        mRightList = new ArrayList<>();
        mRightAdapter = new TradeListAdapter(this.getActivity(), mRightList);
        ListView lvTradeRight = (ListView) myFragmentView.findViewById(R.id.tradeListRight);
        lvTradeRight.setAdapter(mRightAdapter);
        lvTradeRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                showDialog(TradeDialogFragment.DIALOG_UPDATE_CARD, RIGHT, arg2);
            }
        });

        /* Set the buttons to add cards to the left or right */
        myFragmentView.findViewById(R.id.addCardLeft).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addCardToTrade(LEFT);
            }
        });
        myFragmentView.findViewById(R.id.addCardRight).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addCardToTrade(RIGHT);
            }
        });

        lvTradeLeft.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                /* Remove the card */
                mLeftList.remove(position);
                /* Redraw this side */
                mLeftAdapter.notifyDataSetChanged();
                UpdateTotalPrices(LEFT);
                return true;
            }
        });

        lvTradeRight.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                /* Remove the card */
                mRightList.remove(position);
                /* Redraw this side */
                mRightAdapter.notifyDataSetChanged();
                UpdateTotalPrices(RIGHT);
                return true;
            }
        });

        myFragmentView.findViewById(R.id.camera_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getFamiliarActivity().startTutorCardsSearch();
            }
        });
        myFragmentView.findViewById(R.id.camera_button).setVisibility(View.GONE);

        mCheckboxFoil.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                /* Lock the checkbox when the user long clicks it */
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

        /* Return the view */
        return myFragmentView;
    }

    /**
     * This helper method adds a card to a side of the wishlist from the user input
     *
     * @param side RIGHT or LEFT, depending on which side to add the card to
     */
    private void addCardToTrade(int side) {
        /* Make sure there is something to add */
        if (mNameEditText.getText() == null || mNumberEditText.getText() == null) {
            return;
        }

        /* Get the card info from the UI */
        String cardName, setCode, setName, cardNumber, color;
        int cmc;
        cardName = mNameEditText.getText().toString();
        String numberOfFromField = mNumberEditText.getText().toString();
        boolean foil = mCheckboxFoil.isChecked();

        /* Make sure it isn't the empty string */
        if (cardName.equals("") || numberOfFromField.equals("")) {
            return;
        }

        /* Parse the int after the "" check */
        int numberOf = Integer.parseInt(numberOfFromField);

        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
        try {
            String CARDNAME = "cardnameColumn";
            /* Get the rest of the relevant card info from the database */
            Cursor cardCursor = CardDbAdapter.fetchCardByName(cardName, new String[]{
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_CMC, /* For sorting */
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_COLOR, /* For sorting */
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NAME + " as " + CARDNAME, /* Don't trust the user */
                    CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME}, true, database);

            /* Make sure there was a database hit */
            if (cardCursor.getCount() == 0) {
                ToastWrapper.makeText(TradeFragment.this.getActivity(), getString(R.string.toast_no_card), ToastWrapper.LENGTH_LONG)
                        .show();
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }

            /* Read the information from the cursor, check if the card can be foil */
            cardName = cardCursor.getString(cardCursor.getColumnIndex(CARDNAME)); /* The set name and card name keys overlap each other, so just get the second to last column */
            setCode = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
            setName = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
            cardNumber = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
            if (foil && !CardDbAdapter.canBeFoil(setCode, database)) {
                foil = false;
            }
            cmc = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_CMC));
            color = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_COLOR));

            /* Clean up */
            cardCursor.close();

            /* Create the card, add it to a list, start a price fetch */
            MtgCard data = new MtgCard(cardName, setName, setCode, numberOf, getString(R.string.wishlist_loading),
                    cardNumber, foil, color, cmc);
            data.setIndex(mOrderAddedIndex++);
            switch (side) {
                case LEFT: {
                    mLeftList.add(0, data);
                    mLeftAdapter.notifyDataSetChanged();
                    loadPrice(data, mLeftAdapter);
                    break;
                }
                case RIGHT: {
                    mRightList.add(0, data);
                    mRightAdapter.notifyDataSetChanged();
                    loadPrice(data, mRightAdapter);
                    break;
                }
            }

            /* Return the input fields to defaults */
            mNameEditText.setText("");
            mNumberEditText.setText("1");
            /* Only clear the checkbox if it is unlocked */
            if (!mCheckboxFoilLocked) {
                mCheckboxFoil.setChecked(false);
            }

        } catch (FamiliarDbException e) {
            /* Something went wrong, but it's not worth quitting */
            handleFamiliarDbException(false);
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

        /* Sort the newly added card */
        sortTrades(getFamiliarActivity().mPreferenceAdapter.getTradeSortOrder());
    }

    /**
     * When the fragment resumes, get the price preference again and attempt to load the autosave trade
     */
    @Override
    public void onResume() {
        super.onResume();
        mPriceSetting = Integer.parseInt(getFamiliarActivity().mPreferenceAdapter.getTradePrice());
        /* Try to load the autosave trade, the function will handle FileNotFoundException */
        LoadTrade(AUTOSAVE_NAME + TRADE_EXTENSION);
    }

    /**
     * When the fragment pauses, save the trade and cancel all pending price requests
     */
    @Override
    public void onPause() {
        super.onPause();
        SaveTrade(AUTOSAVE_NAME + TRADE_EXTENSION);
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
     * Sort the trades
     */
    private void sortTrades(String sortOrder) {
        /* If no sort type specified, return */
        TradeComparator tradeComparator = new TradeComparator(sortOrder);
        Collections.sort(mLeftList, tradeComparator);
        Collections.sort(mRightList, tradeComparator);
        mLeftAdapter.notifyDataSetChanged();
        mRightAdapter.notifyDataSetChanged();
    }

    /**
     * Save the current trade to the given filename
     *
     * @param tradeName The name of the trade, to be used as a file name
     */
    public void SaveTrade(String tradeName) {
        FileOutputStream fos;

        /* Revert to added-order before saving */
        sortTrades(SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_ASC);
        try {
            /* MODE_PRIVATE will create the file (or replace a file of the same name) */
            fos = this.getActivity().openFileOutput(tradeName, Context.MODE_PRIVATE);

            for (MtgCard cd : mLeftList) {
                fos.write(cd.toTradeString(LEFT).getBytes());
            }
            for (MtgCard cd : mRightList) {
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
    public void LoadTrade(String tradeName) {
        try {
            /* Clear the current lists */
            mLeftList.clear();
            mRightList.clear();

            /* Read each card, line by line, load prices along the way */
            BufferedReader br = new BufferedReader(new InputStreamReader(this.getActivity().openFileInput(tradeName)));
            String line;
            mOrderAddedIndex = 0;
            while ((line = br.readLine()) != null) {
                MtgCard card = MtgCard.fromTradeString(line, getActivity());

                if (card.setName == null) {
                    handleFamiliarDbException(false);
                    return;
                }
                if (card.mSide == LEFT) {
                    card.setIndex(mOrderAddedIndex++);
                    mLeftList.add(card);
                    if (!card.customPrice) {
                        loadPrice(card, mLeftAdapter);
                    }
                } else if (card.mSide == RIGHT) {
                    card.setIndex(mOrderAddedIndex++);
                    mRightList.add(card);
                    if (!card.customPrice) {
                        loadPrice(card, mRightAdapter);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            /* Do nothing, the autosave doesn't exist */
        } catch (IOException e) {
            ToastWrapper.makeText(this.getActivity(), e.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
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
                for (MtgCard data : mLeftList) {
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
                for (MtgCard data : mRightList) {
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
     * Handle an ActionBar item click
     *
     * @param item the item clicked
     * @return true if the click was acted on
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle item selection */
        switch (item.getItemId()) {
            case R.id.trader_menu_clear:
                showDialog(TradeDialogFragment.DIALOG_CONFIRMATION, 0, 0);
                return true;
            case R.id.trader_menu_settings:
                showDialog(TradeDialogFragment.DIALOG_PRICE_SETTING, 0, 0);
                return true;
            case R.id.trader_menu_save:
                showDialog(TradeDialogFragment.DIALOG_SAVE_TRADE, 0, 0);
                return true;
            case R.id.trader_menu_load:
                showDialog(TradeDialogFragment.DIALOG_LOAD_TRADE, 0, 0);
                return true;
            case R.id.trader_menu_delete:
                showDialog(TradeDialogFragment.DIALOG_DELETE_TRADE, 0, 0);
                return true;
            case R.id.trader_menu_sort:
                /* Show a dialog to change the sort criteria the list uses */
                showDialog(TradeDialogFragment.DIALOG_SORT, 0, 0);
                return true;
            case R.id.trader_menu_share:
                shareTrade();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Build a plaintext trade and share it
     */
    private void shareTrade() {

        StringBuilder sb = new StringBuilder();

        /* Add all the cards to the StringBuilder from the left, tallying the price */
        int totalPrice = 0;
        for (MtgCard card : mLeftList) {
            totalPrice += card.toTradeShareString(sb, getString(R.string.wishlist_foil));
        }
        sb.append(String.format(Locale.US, "$%d.%02d\n", totalPrice / 100, totalPrice % 100));

        /* Simple divider */
        sb.append("--------\n");

        /* Add all the cards to the StringBuilder from the right, tallying the price */
        totalPrice = 0;
        for (MtgCard card : mRightList) {
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

    /**
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.trader_menu, menu);
    }

    /**
     * Request the price of a card asynchronously from the network. Save all the returned prices
     *
     * @param data    The card to fetch a price for
     * @param adapter The adapter to notify when a price is downloaded
     */
    public void loadPrice(final MtgCard data, final TradeListAdapter adapter) {
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
                            if (mPriceFetchRequests == 0 && TradeFragment.this.isAdded()) {
                                getFamiliarActivity().clearLoading();
                            }
                            sortTrades(getFamiliarActivity().mPreferenceAdapter.getTradeSortOrder());
                        }
                    }
            );
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
     * Receive the result from the card image search, then fill in the name edit text on the
     * UI thread
     *
     * @param multiverseId The multiverseId of the card the query returned
     */
    @Override
    public void receiveTutorCardsResult(long multiverseId) {
        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false)
                .openDatabase(false);
        try {
            Cursor card = CardDbAdapter.fetchCardByMultiverseId(multiverseId, new String[]{
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NAME}, database);
            final String name = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NAME));
            getFamiliarActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mNameEditText.setText(name);
                }
            });
            card.close();
        } catch (FamiliarDbException e) {
            e.printStackTrace();
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * This inner class helps to display card information from an ArrayList<> in a ListView
     */
    public class TradeListAdapter extends ArrayAdapter<MtgCard> {

        private final ArrayList<MtgCard> items;

        /**
         * Constructor
         *
         * @param context A context to pass to super
         * @param mItems  The list of items to display
         */
        public TradeListAdapter(Context context, ArrayList<MtgCard> mItems) {
            super(context, R.layout.trader_row, mItems);
            this.items = mItems;
        }

        /**
         * This either inflates or recycles a view for a given row, and populates it with card information
         *
         * @param position    The position of the row, corresponds to an entry in the ArrayList
         * @param convertView The view to recycle, or null if we have to inflate a new one
         * @param parent      The parent ViewGroup
         * @return The view to display for this row
         */
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            /* if the supplied view is null, inflate a new one */
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.trader_row, null, false);
            }
            /* Get the data from the ArrayList */
            MtgCard data = items.get(position);
            if (data != null) {
                assert convertView != null;

                /* Set the name, set number, and foil indicators */
                ((TextView) convertView.findViewById(R.id.traderRowName)).setText(data.mName);
                ((TextView) convertView.findViewById(R.id.traderRowSet)).setText(data.setName);
                ((TextView) convertView.findViewById(R.id.traderNumber)).setText(data.hasPrice() ?
                        data.numberOf + "x" : "");
                convertView.findViewById(R.id.traderRowFoil).setVisibility((data.foil ? View.VISIBLE : View.GONE));

                /* Set the price, and the color depending on custom status */
                TextView priceField = (TextView) convertView.findViewById(R.id.traderRowPrice);
                priceField.setText(data.hasPrice() ? data.getPriceString() : data.message);
                if (data.hasPrice()) {
                    if (data.customPrice) {
                        priceField.setTextColor(ContextCompat.getColor(getContext(), R.color.material_green_500));
                    } else {
                        priceField.setTextColor(ContextCompat.getColor(getContext(),
                                getResourceIdFromAttr(R.attr.color_text)
                        ));
                    }
                } else {
                    priceField.setTextColor(ContextCompat.getColor(getContext(), R.color.material_red_500));
                }
            }
            return convertView;
        }
    }

    private static class TradeComparator implements Comparator<MtgCard> {

        final ArrayList<SortOrderDialogFragment.SortOption> options = new ArrayList<>();

        /**
         * Constructor. It parses an "order by" string into search options. The first options have
         * higher priority
         *
         * @param orderByStr   The string to parse. It uses SQLite syntax: "KEY asc,KEY2 desc" etc
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
                        case SortOrderDialogFragment.KEY_ORDER: {
                            retVal = SortOrderDialogFragment.compareInt(card1.getIndex(), card2.getIndex());
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
}
