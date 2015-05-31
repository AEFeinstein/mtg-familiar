package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
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

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class manages trades between two users. Trades can be saved and loaded
 */
public class TradeFragment extends FamiliarFragment {

    /* Price Constants */
    private static final int LOW_PRICE = 0;
    private static final int AVG_PRICE = 1;
    private static final int HIGH_PRICE = 2;
    private static final int FOIL_PRICE = 3;

    /* Side Constants */
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTH = 2;

    /* Dialog Constants */
    private static final int DIALOG_UPDATE_CARD = 1;
    private static final int DIALOG_PRICE_SETTING = 2;
    private static final int DIALOG_SAVE_TRADE = 3;
    private static final int DIALOG_LOAD_TRADE = 4;
    private static final int DIALOG_DELETE_TRADE = 5;
    private static final int DIALOG_CONFIRMATION = 6;
    private static final int DIALOG_CHANGE_SET = 7;

    /* Save file constants */
    private static final String AUTOSAVE_NAME = "autosave";
    private static final String TRADE_EXTENSION = ".trade";

    /* Trade information */
    private TextView mTotalPriceLeft;
    private TradeListAdapter mLeftAdapter;
    private ArrayList<MtgCard> mLeftList;
    private TextView mTotalPriceRight;
    private TradeListAdapter mRightAdapter;
    private ArrayList<MtgCard> mRightList;

    /* UI Elements */
    private AutoCompleteTextView mNameEditText;
    private EditText mNumberEditText;
    private CheckBox mCheckboxFoil;
    private int mPriceFetchRequests = 0;

    /* Settings */
    private int mPriceSetting;
    private String mCurrentTrade = "";

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
                new int[]{R.id.text1}, mNameEditText));
        mNumberEditText.setText("1");

		/* Initialize the left list */
        mLeftList = new ArrayList<>();
        mLeftAdapter = new TradeListAdapter(this.getActivity(), mLeftList);
        ListView lvTradeLeft = (ListView) myFragmentView.findViewById(R.id.tradeListLeft);
        lvTradeLeft.setAdapter(mLeftAdapter);
        lvTradeLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                showDialog(DIALOG_UPDATE_CARD, LEFT, arg2);
            }
        });

		/* Initialize the right list */
        mRightList = new ArrayList<>();
        mRightAdapter = new TradeListAdapter(this.getActivity(), mRightList);
        ListView lvTradeRight = (ListView) myFragmentView.findViewById(R.id.tradeListRight);
        lvTradeRight.setAdapter(mRightAdapter);
        lvTradeRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                showDialog(DIALOG_UPDATE_CARD, RIGHT, arg2);
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
        String cardName, setCode, setName, cardNumber;
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
            /* Get the rest of the relevant card info from the database */
            Cursor cardCursor = CardDbAdapter.fetchCardByName(cardName, new String[]{
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
                    CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME}, database);

			/* Make sure there was a database hit */
            if (cardCursor.getCount() == 0) {
                ToastWrapper.makeText(TradeFragment.this.getActivity(), getString(R.string.toast_no_card), ToastWrapper.LENGTH_LONG)
                        .show();
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }

			/* Read the information from the cursor, check if the card can be foil */
            setCode = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
            setName = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
            cardNumber = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
            if (foil && !CardDbAdapter.canBeFoil(setCode, database)) {
                foil = false;
            }

			/* Clean up */
            cardCursor.close();

			/* Create the card, add it to a list, start a price fetch */
            MtgCard data = new MtgCard(cardName, setName, setCode, numberOf, getString(R.string.wishlist_loading),
                    cardNumber, foil);
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
            mCheckboxFoil.setChecked(false);

        } catch (FamiliarDbException e) {
            /* Something went wrong, but it's not worth quitting */
            handleFamiliarDbException(false);
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
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
    private void showDialog(final int id, final int sideForDialog, final int positionForDialog) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

		/* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

		/* Create and show the dialog. */
        final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

            @NotNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                /* We're setting this to false if we return null, so we should reset it every time to be safe */
                setShowsDialog(true);
                switch (id) {
                    case DIALOG_UPDATE_CARD: {
						/* Get some final references */
                        final ArrayList<MtgCard> lSide = (sideForDialog == LEFT ? mLeftList : mRightList);
                        final TradeListAdapter aaSide = (sideForDialog == LEFT ? mLeftAdapter : mRightAdapter);
                        final boolean oldFoil = lSide.get(positionForDialog).foil;

						/* Inflate the view and pull out UI elements */
                        View view = LayoutInflater.from(getActivity()).inflate(R.layout.trader_card_click_dialog,
                                null, false);
                        assert view != null;
                        final CheckBox foilCheckbox = (CheckBox) view.findViewById(R.id.traderDialogFoil);
                        final EditText numberOf = (EditText) view.findViewById(R.id.traderDialogNumber);
                        final EditText priceText = (EditText) view.findViewById(R.id.traderDialogPrice);

						/* Set initial values */
                        String numberOfStr = String.valueOf(lSide.get(positionForDialog).numberOf);
                        numberOf.setText(numberOfStr);
                        numberOf.setSelection(numberOfStr.length());
                        foilCheckbox.setChecked(oldFoil);
                        String priceNumberStr = lSide.get(positionForDialog).hasPrice() ?
                                lSide.get(positionForDialog).getPriceString().substring(1) : "";
                        priceText.setText(priceNumberStr);
                        priceText.setSelection(priceNumberStr.length());

						/* Only show the foil checkbox if the card can be foil */
                        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                        try {
                            if (CardDbAdapter.canBeFoil(lSide.get(positionForDialog).setCode, database)) {
                                view.findViewById(R.id.checkbox_layout).setVisibility(View.VISIBLE);
                            } else {
                                view.findViewById(R.id.checkbox_layout).setVisibility(View.GONE);
                            }
                        } catch (FamiliarDbException e) {
							/* Err on the side of foil */
                            foilCheckbox.setVisibility(View.VISIBLE);
                        }
                        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

						/* when the user checks or un-checks the foil box, if the price isn't custom, set it */
                        foilCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                lSide.get(positionForDialog).foil = b;
                                if (!lSide.get(positionForDialog).customPrice) {
                                    loadPrice(lSide.get(positionForDialog), aaSide);
                                    priceText.setText(lSide.get(positionForDialog).hasPrice() ?
                                            lSide.get(positionForDialog).getPriceString().substring(1) : "");
                                }
                            }
                        });

						/* Set up the button to remove this card from the trade */
                        view.findViewById(R.id.traderDialogRemove).setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                lSide.remove(positionForDialog);
                                aaSide.notifyDataSetChanged();
                                UpdateTotalPrices(sideForDialog);
                                removeDialog(getFragmentManager());
                            }
                        });

						/* If this has a custom price, show the button to default the price */
                        view.findViewById(R.id.traderDialogResetPrice).setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                lSide.get(positionForDialog).customPrice = false;
								/* This loads the price if necessary, or uses cached info */
                                loadPrice(lSide.get(positionForDialog), aaSide);
                                int price = lSide.get(positionForDialog).price;
                                priceText.setText(String.format("%d.%02d", price / 100, price % 100));

                                aaSide.notifyDataSetChanged();
                                UpdateTotalPrices(sideForDialog);
                            }
                        });


						/* Set up the button to show info about this card */
                        view.findViewById(R.id.traderDialogInfo).setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                                try {
									/* Get the card ID, and send it to a new CardViewPagerFragment */
                                    Cursor cursor = CardDbAdapter.fetchCardByNameAndSet(lSide.get(positionForDialog).name,
                                            lSide.get(positionForDialog).setCode, new String[]{
                                                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID}, database
                                    );

                                    Bundle args = new Bundle();
                                    args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, new long[]{cursor.getLong(
                                            cursor.getColumnIndex(CardDbAdapter.KEY_ID))});
                                    args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);

                                    cursor.close();
                                    CardViewPagerFragment cvpFrag = new CardViewPagerFragment();
                                    TradeFragment.this.startNewFragment(cvpFrag, args);
                                } catch (FamiliarDbException e) {
                                    TradeFragment.this.handleFamiliarDbException(false);
                                }
                                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                            }
                        });

						/* Set up the button to change the set of this card */
                        view.findViewById(R.id.traderDialogChangeSet).setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                showDialog(DIALOG_CHANGE_SET, sideForDialog, positionForDialog);
                            }
                        });

                        return new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(lSide.get(positionForDialog).name)
                                .setView(view)
                                .setPositiveButton(R.string.dialog_done, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
										/* Grab a reference to the card */
                                        MtgCard data = lSide.get(positionForDialog);

                                        /* Assume non-custom price */
                                        data.customPrice = false;

										/* Set this card's foil option */
                                        data.foil = foilCheckbox.isChecked();

										/* validate number of cards text */
                                        if (numberOf.length() == 0) {
                                            data.numberOf = 1;
                                        } else {
											/* Set the numberOf */
                                            assert numberOf.getEditableText() != null;
                                            try {
                                                data.numberOf =
                                                        (Integer.parseInt(numberOf.getEditableText().toString()));
                                            } catch (NumberFormatException e) {
                                                data.numberOf = 1;
                                            }
                                        }

										/* validate the price text */
                                        assert priceText.getText() != null;
                                        String userInputPrice = priceText.getText().toString();

										/* If the input price is blank, set it to zero */
                                        if (userInputPrice.length() == 0) {
                                            data.customPrice = true;
                                            data.price = 0;
                                        } else {
											/* Attempt to parse the price */
                                            try {
                                                data.price = (int) (Double.parseDouble(userInputPrice) * 100);
                                            } catch (NumberFormatException e) {
                                                data.customPrice = true;
                                                data.price = 0;
                                            }
                                        }

										/* Check if the user hand-modified the price by comparing the current price
										 * to the cached price */
                                        int oldPrice;
                                        if (data.priceInfo != null) {
                                            if (data.foil) {
                                                oldPrice = (int) (data.priceInfo.mFoilAverage * 100);
                                            } else {
                                                switch (mPriceSetting) {
                                                    case LOW_PRICE: {
                                                        oldPrice = (int) (data.priceInfo.mLow * 100);
                                                        break;
                                                    }
                                                    default:
                                                    case AVG_PRICE: {
                                                        oldPrice = (int) (data.priceInfo.mAverage * 100);
                                                        break;
                                                    }
                                                    case HIGH_PRICE: {
                                                        oldPrice = (int) (data.priceInfo.mHigh * 100);
                                                        break;
                                                    }
                                                    case FOIL_PRICE: {
                                                        oldPrice = (int) (data.priceInfo.mFoilAverage * 100);
                                                        break;
                                                    }
                                                }
                                            }
                                            if (oldPrice != data.price) {
                                                data.customPrice = true;
                                            }
                                        } else {
                                            data.customPrice = true;
                                        }

										/* Notify things to update */
                                        aaSide.notifyDataSetChanged();
                                        UpdateTotalPrices(sideForDialog);
                                    }
                                })
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .create();
                    }
                    case DIALOG_CHANGE_SET: {
						/* Get the card */
                        MtgCard data = (sideForDialog == LEFT ?
                                mLeftList.get(positionForDialog) : mRightList.get(positionForDialog));

                        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                        try {
							/* Query the database for all versions of this card */
                            Cursor cards = CardDbAdapter.fetchCardByName(data.name, new String[]{
                                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
                                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                                    CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME}, database);
							/* Build set names and set codes */
                            Set<String> sets = new LinkedHashSet<>();
                            Set<String> setCodes = new LinkedHashSet<>();
                            while (!cards.isAfterLast()) {
                                if (sets.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME)))) {
                                    setCodes.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET)));
                                }
                                cards.moveToNext();
                            }
							/* clean up */
                            cards.close();
                            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

							/* Turn set names and set codes into arrays */
                            final String[] aSets = sets.toArray(new String[sets.size()]);
                            final String[] aSetCodes = setCodes.toArray(new String[setCodes.size()]);

							/* Build and return the dialog */
                            return new AlertDialogPro.Builder(getActivity())
                                    .setTitle(R.string.card_view_set_dialog_title)
                                    .setItems(aSets, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialogInterface, int item) {
											/* Figure out what we're updating */
                                            MtgCard data;
                                            TradeListAdapter adapter;
                                            if (sideForDialog == LEFT) {
                                                data = mLeftList.get(positionForDialog);
                                                adapter = mLeftAdapter;
                                            } else {
                                                data = mRightList.get(positionForDialog);
                                                adapter = mRightAdapter;
                                            }

											/* Change the card's information, and reload the price */
                                            data.setCode = (aSetCodes[item]);
                                            data.setName = (aSets[item]);
                                            data.message = (getString(R.string.wishlist_loading));
                                            data.priceInfo = null;

											/* See if the new set can be foil */
                                            SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                                            try {
                                                if (!CardDbAdapter.canBeFoil(data.setCode, database)) {
                                                    data.foil = false;
                                                }
                                            } catch (FamiliarDbException e) {
                                                data.foil = false;
                                            }
                                            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

											/* Reload and notify the adapter */
                                            loadPrice(data, adapter);
                                            adapter.notifyDataSetChanged();
                                        }
                                    })
                                    .create();
                        } catch (FamiliarDbException e) {
							/* Don't show the dialog, but pop a toast */
                            handleFamiliarDbException(true);
                            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                            return DontShowDialog();
                        }
                    }
                    case DIALOG_PRICE_SETTING: {
						/* Build the dialog with some choices */
                        return new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(R.string.trader_pricing_dialog_title)
                                .setSingleChoiceItems(new String[]{getString(R.string.trader_Low),
                                                getString(R.string.trader_Average),
                                                getString(R.string.trader_High)}, mPriceSetting,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (mPriceSetting != which) {
                                                    mPriceSetting = which;

													/* Update ALL the prices! */
                                                    for (MtgCard data : mLeftList) {
                                                        if (!data.customPrice) {
                                                            data.message = getString(R.string.wishlist_loading);
                                                            loadPrice(data, mLeftAdapter);
                                                        }
                                                    }
                                                    mLeftAdapter.notifyDataSetChanged();

                                                    for (MtgCard data : mRightList) {
                                                        if (!data.customPrice) {
                                                            data.message = getString(R.string.wishlist_loading);
                                                            loadPrice(data, mRightAdapter);
                                                        }
                                                    }
                                                    mRightAdapter.notifyDataSetChanged();

													/* And also update the preference */
                                                    getFamiliarActivity().mPreferenceAdapter.setTradePrice(
                                                            String.valueOf(mPriceSetting));

                                                    UpdateTotalPrices(BOTH);
                                                }
                                                dialog.dismiss();
                                            }
                                        }
                                ).create();
                    }
                    case DIALOG_SAVE_TRADE: {
						/* Inflate a view to type in the trade's name, and show it in an AlertDialog */
                        View textEntryView = getActivity().getLayoutInflater()
                                .inflate(R.layout.alert_dialog_text_entry,
                                        null, false);
                        assert textEntryView != null;
                        final EditText nameInput = (EditText) textEntryView.findViewById(R.id.text_entry);
                        nameInput.append(mCurrentTrade);
						/* Set the button to clear the text field */
                        textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                nameInput.setText("");
                            }
                        });

                        Dialog dialog = new AlertDialogPro.Builder(getActivity())
                                .setTitle(R.string.trader_save_dialog_title)
                                .setView(textEntryView)
                                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        if (nameInput.getText() == null) {
                                            return;
                                        }
                                        String tradeName = nameInput.getText().toString();

										/* Don't bother saving if there is no name */
                                        if (tradeName.length() == 0 || tradeName.equals("")) {
                                            return;
                                        }

                                        SaveTrade(tradeName + TRADE_EXTENSION);
                                        mCurrentTrade = tradeName;
                                    }
                                })
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                })
                                .create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                        return dialog;
                    }
                    case DIALOG_LOAD_TRADE: {
						/* Find all the trade files */
                        String[] files = this.getActivity().fileList();
                        ArrayList<String> validFiles = new ArrayList<>();
                        for (String fileName : files) {
                            if (fileName.endsWith(TRADE_EXTENSION)) {
                                validFiles.add(fileName.substring(0, fileName.indexOf(TRADE_EXTENSION)));
                            }
                        }

						/* If there are no files, don't show the dialog */
                        if (validFiles.size() == 0) {
                            ToastWrapper.makeText(this.getActivity(), R.string.trader_toast_no_trades, ToastWrapper.LENGTH_LONG)
                                    .show();
                            return DontShowDialog();
                        }

						/* Make an array of the trade file names */
                        final String[] tradeNames = new String[validFiles.size()];
                        validFiles.toArray(tradeNames);

                        return new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(R.string.trader_select_dialog_title)
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
										/* Canceled. */
                                        dialog.dismiss();
                                    }
                                })
                                .setItems(tradeNames, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface di, int which) {
										/* Load the trade, set the current trade name */
                                        LoadTrade(tradeNames[which] + TRADE_EXTENSION);
                                        mCurrentTrade = tradeNames[which];

										/* Alert things to update */
                                        mLeftAdapter.notifyDataSetChanged();
                                        mRightAdapter.notifyDataSetChanged();
                                        UpdateTotalPrices(BOTH);
                                    }
                                })
                                .create();
                    }
                    case DIALOG_DELETE_TRADE: {
						/* Find all the trade files */
                        String[] files = this.getActivity().fileList();
                        ArrayList<String> validFiles = new ArrayList<>();
                        for (String fileName : files) {
                            if (fileName.endsWith(TRADE_EXTENSION)) {
                                validFiles.add(fileName.substring(0, fileName.indexOf(TRADE_EXTENSION)));
                            }
                        }

						/* If there are no files, don't show the dialog */
                        if (validFiles.size() == 0) {
                            ToastWrapper.makeText(this.getActivity(), R.string.trader_toast_no_trades, ToastWrapper.LENGTH_LONG)
                                    .show();
                            return DontShowDialog();
                        }

						/* Make an array of the trade file names */
                        final String[] tradeNames = new String[validFiles.size()];
                        validFiles.toArray(tradeNames);

                        return new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(R.string.trader_delete_dialog_title)
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
										/* Canceled. */
                                        dialog.dismiss();
                                    }
                                })
                                .setItems(tradeNames, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface di, int which) {
                                        File toDelete = new File(getActivity().getFilesDir(), tradeNames[which] +
                                                TRADE_EXTENSION);
                                        if (!toDelete.delete()) {
                                            ToastWrapper.makeText(getActivity(), toDelete.getName() + " " +
                                                    getString(R.string.not_deleted), ToastWrapper.LENGTH_LONG).show();
                                        }
                                    }
                                })
                                .create();
                    }
                    case DIALOG_CONFIRMATION: {
                        return new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(R.string.trader_clear_dialog_title)
                                .setMessage(R.string.trader_clear_dialog_text)
                                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
										/* Clear the arrays and tell everything to update */
                                        mRightList.clear();
                                        mLeftList.clear();
                                        mRightAdapter.notifyDataSetChanged();
                                        mLeftAdapter.notifyDataSetChanged();
                                        UpdateTotalPrices(BOTH);
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
										/* Canceled */
                                        dialog.dismiss();
                                    }
                                })
                                .setCancelable(true)
                                .create();
                    }
                    default: {
                        return DontShowDialog();
                    }
                }
            }
        };
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Save the current trade to the given filename
     *
     * @param tradeName The name of the trade, to be used as a file name
     */
    private void SaveTrade(String tradeName) {
        FileOutputStream fos;

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
    private void LoadTrade(String tradeName) {
        try {
			/* Clear the current lists */
            mLeftList.clear();
            mRightList.clear();

			/* Read each card, line by line, load prices along the way */
            BufferedReader br = new BufferedReader(new InputStreamReader(this.getActivity().openFileInput(tradeName)));
            String line;
            while ((line = br.readLine()) != null) {
                MtgCard card = MtgCard.MtgCardFromTradeString(line, getActivity());

                if (card.setName == null) {
                    handleFamiliarDbException(false);
                    return;
                }
                if (card.mSide == LEFT) {
                    mLeftList.add(card);
                    if (!card.customPrice) {
                        loadPrice(card, mLeftAdapter);
                    }
                } else if (card.mSide == RIGHT) {
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
    private void UpdateTotalPrices(int side) {
        if (this.isAdded()) {
            if (side == LEFT || side == BOTH) {
                int totalPrice = 0;
                boolean hasBadValues = false;
			/* Iterate through the list and either sum the price or mark it as "bad," (incomplete) */
                for (MtgCard data : mLeftList) {
                    if (data.hasPrice()) {
                        totalPrice += data.numberOf * data.price;
                    } else {
                        hasBadValues = true;
                    }
                }

			/* Set the color whether all values are loaded, and write the text */
                int color = hasBadValues ?
                        this.getActivity().getResources().getColor(R.color.material_red_500) :
                        this.getActivity().getResources().getColor(
                                getResourceIdFromAttr(R.attr.color_text));
                mTotalPriceLeft.setText(String.format("$%d.%02d", totalPrice / 100, totalPrice % 100));
                mTotalPriceLeft.setTextColor(color);
            }
            if (side == RIGHT || side == BOTH) {
                int totalPrice = 0;
                boolean hasBadValues = false;
			/* Iterate through the list and either sum the price or mark it as "bad," (incomplete) */
                for (MtgCard data : mRightList) {
                    if (data.hasPrice()) {
                        totalPrice += data.numberOf * data.price;
                    } else {
                        hasBadValues = true;
                    }
                }

			/* Set the color whether all values are loaded, and write the text */
                int color = hasBadValues ?
                        this.getActivity().getResources().getColor(R.color.material_red_500) :
                        this.getActivity().getResources().getColor(
                                getResourceIdFromAttr(R.attr.color_text)
                        );
                mTotalPriceRight.setText(String.format("$%d.%02d", totalPrice / 100, totalPrice % 100));
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
                showDialog(DIALOG_CONFIRMATION, 0, 0);
                return true;
            case R.id.trader_menu_settings:
                showDialog(DIALOG_PRICE_SETTING, 0, 0);
                return true;
            case R.id.trader_menu_save:
                showDialog(DIALOG_SAVE_TRADE, 0, 0);
                return true;
            case R.id.trader_menu_load:
                showDialog(DIALOG_LOAD_TRADE, 0, 0);
                return true;
            case R.id.trader_menu_delete:
                showDialog(DIALOG_DELETE_TRADE, 0, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
    private void loadPrice(final MtgCard data, final TradeListAdapter adapter) {
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
            PriceFetchRequest priceRequest = new PriceFetchRequest(data.name, data.setCode, data.number, -1, getActivity());
            mPriceFetchRequests++;
            getFamiliarActivity().setLoading();
            getFamiliarActivity().mSpiceManager.execute(priceRequest, data.name + "-" + data.setCode,
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
                        }
                    }
            );
        }
    }

    /**
     * This inner class helps to display card information from an ArrayList<> in a ListView
     */
    private class TradeListAdapter extends ArrayAdapter<MtgCard> {

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
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
			/* if the supplied view is null, inflate a new one */
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.trader_row, null, false);
            }
			/* Get the data from the ArrayList */
            MtgCard data = items.get(position);
            if (data != null) {
                assert convertView != null;

				/* Set the name, set number, and foil indicators */
                ((TextView) convertView.findViewById(R.id.traderRowName)).setText(data.name);
                ((TextView) convertView.findViewById(R.id.traderRowSet)).setText(data.setName);
                ((TextView) convertView.findViewById(R.id.traderNumber)).setText(data.hasPrice() ?
                        data.numberOf + "x" : "");
                convertView.findViewById(R.id.traderRowFoil).setVisibility((data.foil ? View.VISIBLE : View.GONE));

				/* Set the price, and the color depending on custom status */
                TextView priceField = (TextView) convertView.findViewById(R.id.traderRowPrice);
                priceField.setText(data.hasPrice() ? data.getPriceString() : data.message);
                if (data.hasPrice()) {
                    if (data.customPrice) {
                        priceField.setTextColor(getActivity().getResources().getColor(R.color.material_green_500));
                    } else {
                        priceField.setTextColor(getActivity().getResources().getColor(
                                getResourceIdFromAttr(R.attr.color_text)
                        ));
                    }
                } else {
                    priceField.setTextColor(getActivity().getResources().getColor(R.color.material_red_500));
                }
            }
            return convertView;
        }
    }
}
