package com.gelakinetic.mtgfam.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.WishlistDialogFragment;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * This class displays a wishlist of cards, details about the cards, their prices, and the sum of their prices
 */
public class WishlistFragment extends FamiliarFragment {

    /* Price setting constants */
    public static final int LOW_PRICE = 0;
    public static final int AVG_PRICE = 1;
    public static final int HIGH_PRICE = 2;
    /* Preferences */
    public int mPriceSetting;
    private boolean mShowCardInfo;
    private boolean mShowIndividualPrices;
    private boolean mShowTotalWishlistPrice;

    /* UI Elements */
    public AutoCompleteTextView mNameField;
    public EditText mNumberField;
    private TextView mTotalPriceField;
    public CheckBox mCheckboxFoil;
    private int mPriceFetchRequests = 0;

    /* The wishlist and adapter */
    public ArrayList<CompressedWishlistInfo> mCompressedWishlist;
    public WishlistArrayAdapter mWishlistAdapter;
    private View mTotalPriceDivider;
    private boolean mCheckboxFoilLocked = false;

    /**
     * Create the view, pull out UI elements, and set up the listener for the "add cards" button
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The view to be displayed
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myFragmentView = inflater.inflate(R.layout.wishlist_frag, container, false);
        assert myFragmentView != null;

        TextView.OnEditorActionListener addCardListener = new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
                if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
                    addCardToWishlist();
                    return true;
                }
                return false;
            }
        };

        /* set the autocomplete for card names */
        mNameField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.name_search);
        mNameField.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME}, new int[]{R.id.text1}, mNameField, false));
        mNameField.setOnEditorActionListener(addCardListener);

        /* Default the number of cards field */
        mNumberField = (EditText) myFragmentView.findViewById(R.id.number_input);
        mNumberField.setText("1");
        mNumberField.setOnEditorActionListener(addCardListener);

        /* Grab other elements */
        mTotalPriceField = (TextView) myFragmentView.findViewById(R.id.priceText);
        mTotalPriceDivider = myFragmentView.findViewById(R.id.divider_total_price);
        mCheckboxFoil = (CheckBox) myFragmentView.findViewById(R.id.wishlistFoil);
        ListView listView = (ListView) myFragmentView.findViewById(R.id.wishlist);

        myFragmentView.findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCardToWishlist();
            }
        });

        /* Set up the wishlist and adapter, it will be read in onResume() */
        mCompressedWishlist = new ArrayList<>();
        mWishlistAdapter = new WishlistArrayAdapter(mCompressedWishlist);
        listView.setAdapter(mWishlistAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                /* Show the dialog for this particular card */
                showDialog(WishlistDialogFragment.DIALOG_UPDATE_CARD, mCompressedWishlist.get(position).mCard.mName);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                /* Remove the card */
                mCompressedWishlist.remove(position);
                /* Save the wishlist */
                WishlistHelpers.WriteCompressedWishlist(getActivity(), mCompressedWishlist);

                /* Redraw the new wishlist */
                mWishlistAdapter.notifyDataSetChanged();
                sumTotalPrice();
                return true;
            }
        });

        myFragmentView.findViewById(R.id.camera_button).setOnClickListener(new View.OnClickListener() {
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

        return myFragmentView;
    }

    /**
     * This function takes care of adding a card to the wishlist from this fragment. It makes sure that fields are
     * not null or have bad information.
     */
    private void addCardToWishlist() {
        /* Do not allow empty fields */
        String name = String.valueOf(mNameField.getText());
        String numberOf = (String.valueOf(mNumberField.getText()));
        if (name == null || name.equals("")) {
            return;
        }
        if (numberOf == null || numberOf.equals("")) {
            return;
        }

        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
        try {
            /* Make the new card */
            MtgCard card = new MtgCard();
            card.mName = name;
            card.foil = mCheckboxFoil.isChecked();
            card.numberOf = Integer.parseInt(numberOf);
            card.message = getString(R.string.wishlist_loading);

            /* Get some extra information from the database */
            Cursor cardCursor = CardDbAdapter.fetchCardByName(card.mName, CardDbAdapter.allCardDataKeys, true, database);
            if (cardCursor.getCount() == 0) {
                ToastWrapper.makeText(WishlistFragment.this.getActivity(), getString(R.string.toast_no_card),
                        ToastWrapper.LENGTH_LONG).show();
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }
            /* Don't rely on the user's mName, get it from the DB just to be sure */
            card.mName = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
            card.mType = CardDbAdapter.getTypeLine(cardCursor);
            card.mRarity = (char) cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_RARITY));
            card.mManaCost = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_MANACOST));
            card.mPower = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_POWER));
            card.mToughness = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
            card.mLoyalty = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
            card.mText = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_ABILITY));
            card.mFlavor = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
            card.mNumber = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
            card.setCode = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
            card.setName = CardDbAdapter.getSetNameFromCode(card.setCode, database);
            card.mCmc = cardCursor.getInt((cardCursor.getColumnIndex(CardDbAdapter.KEY_CMC)));
            card.mColor = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_COLOR));
            /* Override choice if the card can't be foil */
            if (!CardDbAdapter.canBeFoil(card.setCode, database)) {
                card.foil = false;
            }
            /* Clean up */
            cardCursor.close();

            /* Add it to the wishlist, either as a new CompressedWishlistInfo, or to an existing one */
            if (mCompressedWishlist.contains(card)) {
                CompressedWishlistInfo cwi = mCompressedWishlist.get(mCompressedWishlist.indexOf(card));
                boolean added = false;
                for (IndividualSetInfo isi : cwi.mInfo) {
                    if (isi.mSetCode.equals(card.setCode) && isi.mIsFoil.equals(card.foil)) {
                        added = true;
                        isi.mNumberOf++;
                    }
                }
                if (!added) {
                    cwi.add(card);
                }
            } else {
                mCompressedWishlist.add(new CompressedWishlistInfo(card));
            }

            /* load the price */
            loadPrice(card.mName, card.setCode, card.mNumber);

            /* Sort the wishlist */
            sortWishlist(getFamiliarActivity().mPreferenceAdapter.getWishlistSortOrder());

            /* Save the wishlist */
            WishlistHelpers.WriteCompressedWishlist(getActivity(), mCompressedWishlist);

            /* Clean up for the next add */
            mNumberField.setText("1");
            mNameField.setText("");
            /* Only unselect the checkbox if it isn't locked */
            if (!mCheckboxFoilLocked) {
                mCheckboxFoil.setChecked(false);
            }
            /* Redraw the new wishlist with the new card */
            mWishlistAdapter.notifyDataSetChanged();

        } catch (FamiliarDbException e) {
            handleFamiliarDbException(false);
        } catch (NumberFormatException e) {
            /* eat it */
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * Read the preferences, show or hide the total price, read and compress the wishlist, and load prices
     */
    @Override
    public void onResume() {
        super.onResume();

        /* Get the relevant preferences */
        mPriceSetting = Integer.parseInt(getFamiliarActivity().mPreferenceAdapter.getTradePrice());
        mShowIndividualPrices = getFamiliarActivity().mPreferenceAdapter.getShowIndividualWishlistPrices();
        mShowTotalWishlistPrice = getFamiliarActivity().mPreferenceAdapter.getShowTotalWishlistPrice();
        mShowCardInfo = getFamiliarActivity().mPreferenceAdapter.getVerboseWishlist();

        /* Clear, then read the wishlist. This is done in onResume() in case the user quick-searched for a card, and
         * added it to the wishlist from the CardViewFragment */
        mCompressedWishlist.clear();
        readAndCompressWishlist(null);

        /* Show the total price, if desired */
        if (mShowTotalWishlistPrice) {
            mTotalPriceField.setVisibility(View.VISIBLE);
            mTotalPriceDivider.setVisibility(View.VISIBLE);
        } else {
            mTotalPriceField.setVisibility(View.GONE);
            mTotalPriceDivider.setVisibility(View.GONE);
        }

        /* Tell the adapter to redraw */
        mWishlistAdapter.notifyDataSetChanged();
    }

    /**
     * Read in the wishlist from the file, and pack it into an ArrayList of CompressedWishlistInfo for display in the
     * ListView. This data structure stores one copy of the card itself, and a list of set-specific attributes like
     * set name, rarity, and price.
     *
     * @param changedCardName If the wishlist was changed by a dialog, this is the card which we should look at for
     *                        changes
     */
    private void readAndCompressWishlist(String changedCardName) {
        /* Read the wishlist */
        ArrayList<MtgCard> wishlist = WishlistHelpers.ReadWishlist(getActivity());
        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
        boolean cardNumberFixed = false;
        try {
            /* Translate the set code to tcg name, of course it's not saved */
            for (MtgCard card : wishlist) {
                card.setName = CardDbAdapter.getSetNameFromCode(card.setCode, database);

                /* If the number is empty because of a prior bug, get it from the database */
                if (card.mNumber.equals("")) {
                    Cursor numberCursor = CardDbAdapter.fetchCardByName(card.mName, new String[]{CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_CODE}, false, database);
                    numberCursor.moveToFirst();
                    while (!numberCursor.isAfterLast()) {
                        if (card.setCode.equals(numberCursor.getString(numberCursor.getColumnIndex(CardDbAdapter.KEY_CODE)))) {
                            card.mNumber = numberCursor.getString(numberCursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
                            cardNumberFixed = true;
                            break;
                        }
                        numberCursor.moveToNext();
                    }
                    numberCursor.close();
                }
            }

            /* Clear the wishlist, or just the card that changed */
            if (changedCardName == null) {
                mCompressedWishlist.clear();
            } else {
                for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                    if (cwi.mCard.mName.equals(changedCardName)) {
                        cwi.clearCompressedInfo();
                    }
                }
            }

            /* Compress the whole wishlist, or just the card that changed */
            for (MtgCard card : wishlist) {
                if (changedCardName == null || changedCardName.equals(card.mName)) {
                    /* This works because both MtgCard's and CompressedWishlistInfo's .equals() can compare each
                     * other */
                    if (!mCompressedWishlist.contains(card)) {
                        mCompressedWishlist.add(new CompressedWishlistInfo(card));
                    } else {
                        mCompressedWishlist.get(mCompressedWishlist.indexOf(card)).add(card);
                    }
                    /* Look up the new price */
                    if (mShowIndividualPrices || mShowTotalWishlistPrice) {
                        loadPrice(card.mName, card.setCode, card.mNumber);
                    }
                }
            }

            /* Check for wholly removed cards if one card was modified */
            if (changedCardName != null) {
                for (int i = 0; i < mCompressedWishlist.size(); i++) {
                    if (mCompressedWishlist.get(i).mInfo.size() == 0) {
                        mCompressedWishlist.remove(i);
                        i--;
                    }
                }
            }

            /* Fill extra card data from the database, for displaying full card info */
            CardDbAdapter.fillExtraWishlistData(mCompressedWishlist, database);

            if(cardNumberFixed) {
                WishlistHelpers.WriteCompressedWishlist(getContext(), mCompressedWishlist);
            }

        } catch (FamiliarDbException e) {
            handleFamiliarDbException(false);
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * This notifies the fragment when a change has been made from a card's dialog
     */
    @Override
    public void onWishlistChanged(String cardName) {
        readAndCompressWishlist(cardName);
        mWishlistAdapter.notifyDataSetChanged();
    }

    /**
     * Create the options menu
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.wishlist_menu, menu);
    }

    /**
     * Handle a click from the options menu
     *
     * @param item The item clicked
     * @return true if the click was handled, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.wishlist_menu_clear:
                /* Show a dialog to confirm clearing the wishlist */
                showDialog(WishlistDialogFragment.DIALOG_CONFIRMATION, null);
                return true;
            case R.id.wishlist_menu_settings:
                /* Show a dialog to change which price (low/avg/high) is used */
                showDialog(WishlistDialogFragment.DIALOG_PRICE_SETTING, null);
                return true;
            case R.id.wishlist_menu_sort:
                /* Show a dialog to change the sort criteria the list uses */
                showDialog(WishlistDialogFragment.DIALOG_SORT, null);
                return true;
            case R.id.wishlist_menu_share:
                /* Share the plaintext wishlist */
                /* Use a more generic send text intent. It can also do emails */
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.wishlist_share_title);
                sendIntent.putExtra(Intent.EXTRA_TEXT, WishlistHelpers.GetSharableWishlist(mCompressedWishlist, getActivity(),
                        mShowCardInfo, mShowIndividualPrices, mPriceSetting));
                sendIntent.setType("text/plain");

                try {
                    startActivity(Intent.createChooser(sendIntent, getString(R.string.wishlist_share)));
                } catch (android.content.ActivityNotFoundException ex) {
                    ToastWrapper.makeText(getActivity(), getString(R.string.error_no_email_client), ToastWrapper.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Remove any showing dialogs, and show the requested one
     *
     * @param id       the ID of the dialog to show
     * @param cardName The name of the card to use if this is a dialog to change wishlist counts
     */
    private void showDialog(final int id, final String cardName) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        /* If the fragment isn't visible (if desired being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        /* Create and show the dialog. */
        if (id == WishlistDialogFragment.DIALOG_SORT) {
            SortOrderDialogFragment newFragment = new SortOrderDialogFragment();
            Bundle args = new Bundle();
            args.putString(SortOrderDialogFragment.SAVED_SORT_ORDER,
                    getFamiliarActivity().mPreferenceAdapter.getWishlistSortOrder());
            newFragment.setArguments(args);
            newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
        } else {
            WishlistDialogFragment newFragment = new WishlistDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
            arguments.putString(WishlistDialogFragment.NAME_KEY, cardName);
            newFragment.setArguments(arguments);
            newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
        }
    }

    /**
     * Load the price for a given card. This handles all the spice stuff
     *
     * @param mCardName   The name of the card to find a price for
     * @param mSetCode    The set code of the card to find a price for
     * @param mCardNumber The collector's number
     */
    private void loadPrice(final String mCardName, final String mSetCode, String mCardNumber) {
        PriceFetchRequest priceRequest = new PriceFetchRequest(mCardName, mSetCode, mCardNumber, -1, getActivity());
        mPriceFetchRequests++;
        getFamiliarActivity().setLoading();
        getFamiliarActivity().mSpiceManager.execute(priceRequest, mCardName + "-" +
                mSetCode, DurationInMillis.ONE_DAY, new RequestListener<PriceInfo>() {

            /**
             * Loading the price for this card failed and threw a spiceException
             *
             * @param spiceException The exception thrown when trying to load this card's price
             */
            @Override
            public void onRequestFailure(SpiceException spiceException) {
                /* because this can return when the fragment is in the background */
                if (WishlistFragment.this.isAdded()) {
                    /* Find the compressed wishlist info for this card */
                    for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                        if (cwi.mCard.mName.equals(mCardName)) {
                            /* Find all foil and non foil compressed items with the same set code */
                            for (IndividualSetInfo isi : cwi.mInfo) {
                                if (isi.mSetCode.equals(mSetCode)) {
                                    /* Set the price as null and the message as the exception */
                                    isi.mMessage = spiceException.getLocalizedMessage();
                                    isi.mPrice = null;
                                }
                            }
                        }
                    }
                    mPriceFetchRequests--;
                    if (mPriceFetchRequests == 0) {
                        getFamiliarActivity().clearLoading();
                    }
                    mWishlistAdapter.notifyDataSetChanged();
                }
            }

            /**
             * Loading the price for this card succeeded. Set it.
             *
             * @param result The price for this card
             */
            @Override
            public void onRequestSuccess(final PriceInfo result) {
                /* because this can return when the fragment is in the background */
                if (WishlistFragment.this.isAdded()) {
                    /* Find the compressed wishlist info for this card */
                    for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                        if (cwi.mCard.mName.equals(mCardName)) {
                            /* Find all foil and non foil compressed items with the same set code */
                            for (IndividualSetInfo isi : cwi.mInfo) {
                                if (isi.mSetCode.equals(mSetCode)) {
                                    /* Set the whole price info object */
                                    if (result != null) {
                                        isi.mPrice = result;
                                    }
                                    /* The message will never be shown with a valid price, so set it as DNE */
                                    isi.mMessage = getString(R.string.card_view_price_not_found);
                                }
                            }
                        }
                        sumTotalPrice();
                    }
                    mPriceFetchRequests--;
                    if (mPriceFetchRequests == 0) {
                        getFamiliarActivity().clearLoading();
                    }
                    sortWishlist(getFamiliarActivity().mPreferenceAdapter.getWishlistSortOrder());
                    mWishlistAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * Add together the price of all the cards in the wishlist and display it
     */
    public void sumTotalPrice() {
        if (mShowTotalWishlistPrice) {
            float totalPrice = 0;

            for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                for (IndividualSetInfo isi : cwi.mInfo) {
                    if (isi.mPrice != null) {
                        if (isi.mIsFoil) {
                            totalPrice += (isi.mPrice.mFoilAverage * isi.mNumberOf);
                        } else {
                            switch (mPriceSetting) {
                                case LOW_PRICE:
                                    totalPrice += (isi.mPrice.mLow * isi.mNumberOf);
                                    break;
                                case AVG_PRICE:
                                    totalPrice += (isi.mPrice.mAverage * isi.mNumberOf);
                                    break;
                                case HIGH_PRICE:
                                    totalPrice += (isi.mPrice.mHigh * isi.mNumberOf);
                                    break;
                            }
                        }
                    }
                }
            }
            mTotalPriceField.setText(String.format(Locale.US, "$%.02f", totalPrice));
        }
    }

    /**
     * Called when the sorting dialog closes. Sort the wishlist with the new options
     *
     * @param orderByStr The sort order string
     */
    @Override
    public void receiveSortOrder(String orderByStr) {
        getFamiliarActivity().mPreferenceAdapter.setWishlistSortOrder(orderByStr);
        sortWishlist(orderByStr);
    }

    /**
     * Sorts the wishlist based on mWishlistSortType and mWishlistSortOrder
     */
    private void sortWishlist(String orderByStr) {
        Collections.sort(mCompressedWishlist, new WishlistHelpers.WishlistComparator(orderByStr, mPriceSetting));
        mWishlistAdapter.notifyDataSetChanged();
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
                    mNameField.setText(name);
                }
            });
            card.close();
        } catch (FamiliarDbException e) {
            e.printStackTrace();
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * This nested class is the adapter which populates the listView in the drawer menu. It handles both entries and
     * headers
     */
    public class WishlistArrayAdapter extends ArrayAdapter<CompressedWishlistInfo> {
        private final ArrayList<CompressedWishlistInfo> values;

        /**
         * Constructor. The context will be used to inflate views later. The array of values will be used to populate
         * the views
         *
         * @param values An array of DrawerEntries which will populate the list
         */
        public WishlistArrayAdapter(ArrayList<CompressedWishlistInfo> values) {
            super(getActivity(), R.layout.drawer_list_item, values);
            this.values = values;
        }

        /**
         * Called to get a view for an entry in the listView
         *
         * @param position    The position of the listView to populate
         * @param convertView The old view to reuse, if possible. Since the layouts for entries and headers are
         *                    different, this will be ignored
         * @param parent      The parent this view will eventually be attached to
         * @return The view for the data at this position
         */
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            /* Recycle the view if it isn't null, otherwise inflate it */
            LinearLayout wishlistSets;
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.result_list_card_row, parent, false);
                assert convertView != null;
                wishlistSets = ((LinearLayout) convertView.findViewById(R.id.wishlist_sets));
            } else {
                wishlistSets = ((LinearLayout) convertView.findViewById(R.id.wishlist_sets));
                /* clear any prior sets */
                wishlistSets.removeAllViews();
            }

            /* Get all the wishlist info for this entry */
            CompressedWishlistInfo info = values.get(position);

            /* Set the card name, always */
            ((TextView) convertView.findViewById(R.id.cardname)).setText(info.mCard.mName);

            /* Show or hide full card information */
            convertView.findViewById(R.id.cardset).setVisibility(View.GONE);
            if (mShowCardInfo) {
                Html.ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getActivity());

                /* make sure everything is showing */
                convertView.findViewById(R.id.cardcost).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.cardtype).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.cardability).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.cardp).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.cardslash).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.cardt).setVisibility(View.VISIBLE);

                /* Set the type, cost, and ability */
                ((TextView) convertView.findViewById(R.id.cardtype)).setText(info.mCard.mType);
                ((TextView) convertView.findViewById(R.id.cardcost))
                        .setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.mManaCost, imgGetter));
                ((TextView) convertView.findViewById(R.id.cardability))
                        .setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.mText, imgGetter));

                /* Show the power, toughness, or loyalty if the card has it */
                convertView.findViewById(R.id.cardt).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardp).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardslash).setVisibility(View.GONE);
                try {
                    /* Figure out what power the card has, including special ones */
                    float p = info.mCard.mPower;
                    if (p != CardDbAdapter.NO_ONE_CARES) {
                        String pow;
                        if (p == CardDbAdapter.STAR)
                            pow = "*";
                        else if (p == CardDbAdapter.ONE_PLUS_STAR)
                            pow = "1+*";
                        else if (p == CardDbAdapter.TWO_PLUS_STAR)
                            pow = "2+*";
                        else if (p == CardDbAdapter.SEVEN_MINUS_STAR)
                            pow = "7-*";
                        else if (p == CardDbAdapter.STAR_SQUARED)
                            pow = "*^2";
                        else if (p == CardDbAdapter.X)
                            pow = "X";
                        else {
                            if (p == (int) p) {
                                pow = Integer.valueOf((int) p).toString();
                            } else {
                                pow = Float.valueOf(p).toString();
                            }
                        }
                        ((TextView) convertView.findViewById(R.id.cardp)).setText(pow);

                        convertView.findViewById(R.id.cardt).setVisibility(View.VISIBLE);
                        convertView.findViewById(R.id.cardp).setVisibility(View.VISIBLE);
                        convertView.findViewById(R.id.cardslash).setVisibility(View.VISIBLE);
                    }
                } catch (NumberFormatException e) {
                    /* eat it */
                }
                try {
                    /* figure out what toughness the card has, including special ones */
                    float t = info.mCard.mToughness;
                    if (t != CardDbAdapter.NO_ONE_CARES) {
                        String tou;
                        if (t == CardDbAdapter.STAR)
                            tou = "*";
                        else if (t == CardDbAdapter.ONE_PLUS_STAR)
                            tou = "1+*";
                        else if (t == CardDbAdapter.TWO_PLUS_STAR)
                            tou = "2+*";
                        else if (t == CardDbAdapter.SEVEN_MINUS_STAR)
                            tou = "7-*";
                        else if (t == CardDbAdapter.STAR_SQUARED)
                            tou = "*^2";
                        else if (t == CardDbAdapter.X)
                            tou = "X";
                        else {
                            if (t == (int) t) {
                                tou = Integer.valueOf((int) t).toString();
                            } else {
                                tou = Float.valueOf(t).toString();
                            }
                        }
                        ((TextView) convertView.findViewById(R.id.cardt)).setText(tou);
                    }
                } catch (NumberFormatException e) {
                    /* eat it */
                }

                /* Show the loyalty, if the card has any (traitor...) */
                float loyalty = info.mCard.mLoyalty;
                if (loyalty != -1 && loyalty != CardDbAdapter.NO_ONE_CARES) {

                    if (loyalty == CardDbAdapter.X) {
                        ((TextView) convertView.findViewById(R.id.cardt)).setText("X");
                    } else if (loyalty == (int) loyalty) {
                        ((TextView) convertView.findViewById(R.id.cardt)).setText(Integer.toString((int) loyalty));
                    } else {
                        ((TextView) convertView.findViewById(R.id.cardt)).setText(Float.toString(loyalty));
                    }
                    convertView.findViewById(R.id.cardt).setVisibility(View.VISIBLE);
                    convertView.findViewById(R.id.cardp).setVisibility(View.GONE);
                    convertView.findViewById(R.id.cardslash).setVisibility(View.GONE);
                }
            } else {
                /* hide all the extra fields */
                convertView.findViewById(R.id.cardcost).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardtype).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardability).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardp).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardslash).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardp).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardslash).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardt).setVisibility(View.GONE);
            }
            /* Rarity is displayed on the expansion lines */
            convertView.findViewById(R.id.rarity).setVisibility(View.GONE);

            /* List all the sets and wishlist values for this card */
            for (IndividualSetInfo isi : info.mInfo) {
                /* inflate a row */
                View setRow = getActivity().getLayoutInflater().inflate(R.layout.wishlist_cardset_row, parent, false);
                assert setRow != null;

                /* Write the set name, color it with the rarity */
                int color;
                switch (isi.mRarity) {
                    case 'c':
                    case 'C':
                        color = R.attr.color_common;
                        break;
                    case 'u':
                    case 'U':
                        color = R.attr.color_uncommon;
                        break;
                    case 'r':
                    case 'R':
                        color = R.attr.color_rare;
                        break;
                    case 'm':
                    case 'M':
                        color = R.attr.color_mythic;
                        break;
                    case 't':
                    case 'T':
                        color = R.attr.color_timeshifted;
                        break;
                    default:
                        color = R.attr.color_text;
                        break;
                }
                String setAndRarity = isi.mSet + " (" + isi.mRarity + ")";
                ((TextView) setRow.findViewById(R.id.wishlistRowSet)).setText(setAndRarity);
                ((TextView) setRow.findViewById(R.id.wishlistRowSet)).setTextColor(
                        ContextCompat.getColor(getContext(), getResourceIdFromAttr(color)));

                /* Show or hide the foil indicator */
                if (isi.mIsFoil) {
                    setRow.findViewById(R.id.wishlistSetRowFoil).setVisibility(View.VISIBLE);
                } else {
                    setRow.findViewById(R.id.wishlistSetRowFoil).setVisibility(View.GONE);
                }

                /* Show individual prices and number of each card, or message if price does not exist, if desired */
                TextView priceText = ((TextView) setRow.findViewById(R.id.wishlistRowPrice));
                if (mShowIndividualPrices) {
                    if (isi.mIsFoil) {
                        if (isi.mPrice != null && isi.mPrice.mFoilAverage != 0) {
                            priceText.setText(String.format(Locale.US, "%dx $%.02f", isi.mNumberOf, isi.mPrice.mFoilAverage));
                            priceText.setTextColor(ContextCompat.getColor(getContext(),
                                    getResourceIdFromAttr(R.attr.color_text)));
                        } else {
                            priceText.setText(String.format(Locale.US, "%dx %s", isi.mNumberOf, isi.mMessage));
                            priceText.setTextColor(ContextCompat.getColor(getContext(), R.color.material_red_500));
                        }
                    } else {
                        boolean priceFound = false;
                        if (isi.mPrice != null) {
                            switch (mPriceSetting) {
                                case LOW_PRICE:
                                    if (isi.mPrice.mLow != 0) {
                                        priceText.setText(String.format(Locale.US, "%dx $%.02f", isi.mNumberOf,
                                                isi.mPrice.mLow));
                                        priceFound = true;
                                    }
                                    break;
                                default:
                                case AVG_PRICE:
                                    if (isi.mPrice.mAverage != 0) {
                                        priceText.setText(String.format(Locale.US, "%dx $%.02f", isi.mNumberOf,
                                                isi.mPrice.mAverage));
                                        priceFound = true;
                                    }
                                    break;
                                case HIGH_PRICE:
                                    if (isi.mPrice.mHigh != 0) {
                                        priceText.setText(String.format(Locale.US, "%dx $%.02f", isi.mNumberOf,
                                                isi.mPrice.mHigh));
                                        priceFound = true;
                                    }
                                    break;
                            }
                            priceText.setTextColor(ContextCompat.getColor(getContext(),
                                    getResourceIdFromAttr(R.attr.color_text)
                            ));
                        }
                        if (!priceFound) {
                            priceText.setText(String.format(Locale.US, "%dx %s", isi.mNumberOf, isi.mMessage));
                            priceText.setTextColor(ContextCompat.getColor(getContext(), R.color.material_red_500));
                        }
                    }
                } else {
                    /* Just show the number of */
                    priceText.setText("x" + isi.mNumberOf);
                }

                /* Add the view to the linear layout */
                wishlistSets.addView(setRow);
            }
            return convertView;
        }
    }
}
