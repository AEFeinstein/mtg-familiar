package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.IndividualSetInfo;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.WishlistComparatorCmc;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.WishlistComparatorColor;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.WishlistComparatorName;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.WishlistComparatorPrice;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.WishlistComparatorSet;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

/**
 * This class displays a wishlist of cards, details about the cards, their prices, and the sum of their prices
 */
public class WishlistFragment extends FamiliarFragment {

    /* Dialog constants */
    private static final int DIALOG_UPDATE_CARD = 1;
    private static final int DIALOG_PRICE_SETTING = 2;
    private static final int DIALOG_CONFIRMATION = 3;
    private static final int DIALOG_SORT = 4;
    private static final int DIALOG_SHARE = 5;

    /* Price setting constants */
    private static final int LOW_PRICE = 0;
    private static final int AVG_PRICE = 1;
    private static final int HIGH_PRICE = 2;

    /* Sort type constants */
    private static final int SORT_TYPE_NONE = 0;
    private int wishlistSortType = SORT_TYPE_NONE;  //Type to sort list by (e.g. Name)
    private static final int SORT_TYPE_CMC = 1;
    private static final int SORT_TYPE_COLOR = 2;
    private static final int SORT_TYPE_NAME = 3;
    private static final int SORT_TYPE_PRICE = 4;
    private static final int SORT_TYPE_SET = 5;
    private static final int ASCENDING = 0;
    private static final int DESCENDING = 1;
    /* Preferences */
    private int mPriceSetting;
    private boolean mShowCardInfo;
    private boolean mShowIndividualPrices;
    private boolean mShowTotalWishlistPrice;
    private int wishlistSortOrder;  //ASCENDING v DESCENDING

    /* UI Elements */
    private AutoCompleteTextView mNameField;
    private EditText mNumberField;
    private TextView mTotalPriceField;
    private CheckBox mFoilCheckBox;
    private int mPriceFetchRequests = 0;

    /* The wishlist and adapter */
    private ArrayList<CompressedWishlistInfo> mCompressedWishlist;
    private WishlistArrayAdapter mWishlistAdapter;
    private View mTotalPriceDivider;

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
        mNameField.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME}, new int[]{R.id.text1}, mNameField));
        mNameField.setOnEditorActionListener(addCardListener);

		/* Default the number of cards field */
        mNumberField = (EditText) myFragmentView.findViewById(R.id.number_input);
        mNumberField.setText("1");
        mNumberField.setOnEditorActionListener(addCardListener);

		/* Grab other elements */
        mTotalPriceField = (TextView) myFragmentView.findViewById(R.id.priceText);
        mTotalPriceDivider = myFragmentView.findViewById(R.id.divider_total_price);
        mFoilCheckBox = (CheckBox) myFragmentView.findViewById(R.id.wishlistFoil);
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
                showDialog(DIALOG_UPDATE_CARD, mCompressedWishlist.get(position).mCard.name);
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
            card.name = name;
            card.foil = mFoilCheckBox.isChecked();
            card.numberOf = Integer.parseInt(numberOf);
            card.message = getString(R.string.wishlist_loading);

			/* Get some extra information from the database */
            Cursor cardCursor = CardDbAdapter.fetchCardByName(card.name, CardDbAdapter.allData, database);
            if (cardCursor.getCount() == 0) {
                ToastWrapper.makeText(WishlistFragment.this.getActivity(), getString(R.string.toast_no_card),
                        ToastWrapper.LENGTH_LONG).show();
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }
            card.type = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_TYPE));
            card.rarity = (char) cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_RARITY));
            card.manaCost = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_MANACOST));
            card.power = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_POWER));
            card.toughness = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
            card.loyalty = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
            card.ability = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_ABILITY));
            card.flavor = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
            card.number = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
            card.setCode = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
            card.setName = CardDbAdapter.getSetNameFromCode(card.setCode, database);
            card.cmc = cardCursor.getInt((cardCursor.getColumnIndex(CardDbAdapter.KEY_CMC)));
            card.color = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_COLOR));
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
            loadPrice(card.name, card.setCode, card.number);

            /* Sort the wishlist */
            sortWishlist();

			/* Save the wishlist */
            WishlistHelpers.WriteCompressedWishlist(getActivity(), mCompressedWishlist);

			/* Clean up for the next add */
            mNumberField.setText("1");
            mNameField.setText("");
            mFoilCheckBox.setChecked(false);

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
        try {
			/* Translate the set code to tcg name, of course it's not saved */
            for (MtgCard card : wishlist) {
                card.setName = CardDbAdapter.getSetNameFromCode(card.setCode, database);
            }

			/* Clear the wishlist, or just the card that changed */
            if (changedCardName == null) {
                mCompressedWishlist.clear();
            } else {
                for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                    if (cwi.mCard.name.equals(changedCardName)) {
                        cwi.clearCompressedInfo();
                    }
                }
            }

			/* Compress the whole wishlist, or just the card that changed */
            for (MtgCard card : wishlist) {
                if (changedCardName == null || changedCardName.equals(card.name)) {
					/* This works because both MtgCard's and CompressedWishlistInfo's .equals() can compare each
					 * other */
                    if (!mCompressedWishlist.contains(card)) {
                        mCompressedWishlist.add(new CompressedWishlistInfo(card));
                    } else {
                        mCompressedWishlist.get(mCompressedWishlist.indexOf(card)).add(card);
                    }
					/* Look up the new price */
                    if (mShowIndividualPrices || mShowTotalWishlistPrice) {
                        loadPrice(card.name, card.setCode, card.number);
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
                showDialog(DIALOG_CONFIRMATION, null);
                return true;
            case R.id.wishlist_menu_settings:
				/* Show a dialog to change which price (low/avg/high) is used */
                showDialog(DIALOG_PRICE_SETTING, null);
                return true;
            case R.id.wishlist_menu_sort:
				/* Show a dialog to change the sort criteria the list uses */
                showDialog(DIALOG_SORT, null);
                return true;
            case R.id.wishlist_menu_share:
				/* Launch a dialog to share the plaintext wishlist */
                showDialog(DIALOG_SHARE, null);
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
        final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

            @NotNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                setShowsDialog(true);
                switch (id) {
                    case DIALOG_UPDATE_CARD: {
                        Dialog dialog = WishlistHelpers.getDialog(cardName, WishlistFragment.this, true);
                        if (dialog == null) {
                            handleFamiliarDbException(false);
                            return DontShowDialog();
                        }
                        return dialog;
                    }
                    case DIALOG_PRICE_SETTING: {
                        return new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(R.string.trader_pricing_dialog_title)
                                .setSingleChoiceItems(new String[]{getString(R.string.trader_Low),
                                                getString(R.string.trader_Average), getString(R.string.trader_High)},
                                        mPriceSetting,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (mPriceSetting != which) {
                                                    mPriceSetting = which;
                                                    getFamiliarActivity().mPreferenceAdapter.setTradePrice(
                                                            String.valueOf(mPriceSetting));
                                                    mWishlistAdapter.notifyDataSetChanged();
                                                    sumTotalPrice();
                                                }
                                                dialog.dismiss();
                                            }
                                        }
                                )
                                .create();
                    }
                    case DIALOG_CONFIRMATION: {
                        return new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(R.string.wishlist_empty_dialog_title)
                                .setMessage(R.string.wishlist_empty_dialog_text)
                                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        WishlistHelpers.ResetCards(WishlistFragment.this.getActivity());
                                        mCompressedWishlist.clear();
                                        mWishlistAdapter.notifyDataSetChanged();
                                        sumTotalPrice();
                                        dialog.dismiss();
										/* Clear input too */
                                        mNameField.setText("");
                                        mNumberField.setText("1");
                                        mFoilCheckBox.setChecked(false);
                                    }
                                })
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setCancelable(true).create();

                    }
                    case DIALOG_SORT: {
                        return new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(R.string.wishlist_sort_by)
                                .setSingleChoiceItems(R.array.wishlist_sort_type, wishlistSortType, null)
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setNeutralButton(R.string.wishlist_ascending, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        wishlistSortOrder = ASCENDING;
                                        ListView lw = ((AlertDialog) dialog).getListView();
                                        wishlistSortType = lw.getCheckedItemPosition();
                                        sortWishlist();
                                    }
                                })
                                .setPositiveButton(R.string.wishlist_descending, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        wishlistSortOrder = DESCENDING;
                                        ListView lw = ((AlertDialog) dialog).getListView();
                                        wishlistSortType = lw.getCheckedItemPosition();
                                        sortWishlist();
                                    }
                                })
                                .setCancelable(true).create();
                    }
                    case DIALOG_SHARE: {
                        /* Use a more generic send text intent. It can also do emails */
                        return new AlertDialogPro.Builder(getFamiliarActivity())
                                .setTitle(R.string.wishlist_export_title)
                                .setMessage(R.string.wishlist_export_text)
                                .setPositiveButton(R.string.dialog_yes,
                                        new AlertDialog.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent sendIntent = new Intent();
                                                sendIntent.setAction(Intent.ACTION_SEND);
                                                sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.wishlist_share_title);
                                                sendIntent.putExtra(Intent.EXTRA_TEXT, WishlistHelpers.GetSharableWishlist(mCompressedWishlist, getActivity(), true));
                                                sendIntent.setType("text/plain");

                                                try {
                                                    startActivity(Intent.createChooser(sendIntent, getString(R.string.wishlist_share)));
                                                } catch (android.content.ActivityNotFoundException ex) {
                                                    ToastWrapper.makeText(getActivity(), getString(R.string.error_no_email_client), ToastWrapper.LENGTH_SHORT).show();
                                                }

                                            }
                                        })
                                .setNegativeButton(R.string.dialog_no,
                                        new AlertDialog.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent sendIntent = new Intent();
                                                sendIntent.setAction(Intent.ACTION_SEND);
                                                sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.wishlist_share_title);
                                                sendIntent.putExtra(Intent.EXTRA_TEXT, WishlistHelpers.GetSharableWishlist(mCompressedWishlist, getActivity(), false));
                                                sendIntent.setType("text/plain");

                                                try {
                                                    startActivity(Intent.createChooser(sendIntent, getString(R.string.wishlist_share)));
                                                } catch (android.content.ActivityNotFoundException ex) {
                                                    ToastWrapper.makeText(getActivity(), getString(R.string.error_no_email_client), ToastWrapper.LENGTH_SHORT).show();
                                                }
                                            }
                                        })
                                .create();
                    }
                    default: {
                        savedInstanceState.putInt("id", id);
                        return super.onCreateDialog(savedInstanceState);
                    }
                }
            }
        };
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
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
                        if (cwi.mCard.name.equals(mCardName)) {
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
                        if (cwi.mCard.name.equals(mCardName)) {
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
                    mWishlistAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * Add together the price of all the cards in the wishlist and display it
     */
    private void sumTotalPrice() {
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
            mTotalPriceField.setText(String.format("$%.02f", totalPrice));
        }
    }

    /**
     * Sorts the wishlist based on wishlistSortType and wishlistSortOrder
     */
    private void sortWishlist() {
		/* If no sort type specified, return */
        if (wishlistSortType != SORT_TYPE_NONE) {
            if (wishlistSortOrder == ASCENDING) {
                switch (wishlistSortType) {
                    case SORT_TYPE_CMC:
                        Collections.sort(mCompressedWishlist, new WishlistComparatorCmc());
                        break;
                    case SORT_TYPE_COLOR:
                        Collections.sort(mCompressedWishlist, new WishlistComparatorColor());
                        break;
                    case SORT_TYPE_NAME:
                        Collections.sort(mCompressedWishlist, new WishlistComparatorName());
                        break;
                    case SORT_TYPE_PRICE:
                        Collections.sort(mCompressedWishlist, new WishlistComparatorPrice(mPriceSetting));
                        break;
                    case SORT_TYPE_SET:
                        Collections.sort(mCompressedWishlist, new WishlistComparatorSet());
                        break;
                }
            } else {
                switch (wishlistSortType) {
                    case SORT_TYPE_CMC:
                        Collections.sort(mCompressedWishlist, Collections.reverseOrder(new WishlistComparatorCmc()));
                        break;
                    case SORT_TYPE_COLOR:
                        Collections.sort(mCompressedWishlist, Collections.reverseOrder(new WishlistComparatorColor()));
                        break;
                    case SORT_TYPE_NAME:
                        Collections.sort(mCompressedWishlist, Collections.reverseOrder(new WishlistComparatorName()));
                        break;
                    case SORT_TYPE_PRICE:
                        Collections.sort(mCompressedWishlist, Collections.reverseOrder(new WishlistComparatorPrice(mPriceSetting)));
                        break;
                    case SORT_TYPE_SET:
                        Collections.sort(mCompressedWishlist, Collections.reverseOrder(new WishlistComparatorSet()));
                        break;
                }
            }
            mWishlistAdapter.notifyDataSetChanged();
        }
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
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

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
            ((TextView) convertView.findViewById(R.id.cardname)).setText(info.mCard.name);

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
                ((TextView) convertView.findViewById(R.id.cardtype)).setText(info.mCard.type);
                ((TextView) convertView.findViewById(R.id.cardcost))
                        .setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.manaCost, imgGetter));
                ((TextView) convertView.findViewById(R.id.cardability))
                        .setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.ability, imgGetter));

				/* Show the power, toughness, or loyalty if the card has it */
                convertView.findViewById(R.id.cardt).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardp).setVisibility(View.GONE);
                convertView.findViewById(R.id.cardslash).setVisibility(View.GONE);
                try {
					/* Figure out what power the card has, including special ones */
                    float p = info.mCard.power;
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
                    float t = info.mCard.toughness;
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
                float loyalty = info.mCard.loyalty;
                if (loyalty != -1 && loyalty != CardDbAdapter.NO_ONE_CARES) {
                    if (loyalty == (int) loyalty) {
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
                ((TextView) setRow.findViewById(R.id.wishlistRowSet)).setText(isi.mSet);
                ((TextView) setRow.findViewById(R.id.wishlistRowSet)).setTextColor(getResources()
                        .getColor(getResourceIdFromAttr(color)));

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
                            priceText.setText(String.format("%dx $%.02f", isi.mNumberOf, isi.mPrice.mFoilAverage));
                            priceText.setTextColor(getResources().getColor(
                                    getResourceIdFromAttr(R.attr.color_text)));
                        } else {
                            priceText.setText(String.format("%dx %s", isi.mNumberOf, isi.mMessage));
                            priceText.setTextColor(getResources().getColor(R.color.material_red_500));
                        }
                    } else {
                        boolean priceFound = false;
                        if (isi.mPrice != null) {
                            switch (mPriceSetting) {
                                case LOW_PRICE:
                                    if (isi.mPrice.mLow != 0) {
                                        priceText.setText(String.format("%dx $%.02f", isi.mNumberOf,
                                                isi.mPrice.mLow));
                                        priceFound = true;
                                    }
                                    break;
                                default:
                                case AVG_PRICE:
                                    if (isi.mPrice.mAverage != 0) {
                                        priceText.setText(String.format("%dx $%.02f", isi.mNumberOf,
                                                isi.mPrice.mAverage));
                                        priceFound = true;
                                    }
                                    break;
                                case HIGH_PRICE:
                                    if (isi.mPrice.mHigh != 0) {
                                        priceText.setText(String.format("%dx $%.02f", isi.mNumberOf,
                                                isi.mPrice.mHigh));
                                        priceFound = true;
                                    }
                                    break;
                            }
                            priceText.setTextColor(getResources().getColor(
                                    getResourceIdFromAttr(R.attr.color_text)
                            ));
                        }
                        if (!priceFound) {
                            priceText.setText(String.format("%dx %s", isi.mNumberOf, isi.mMessage));
                            priceText.setTextColor(getResources().getColor(R.color.material_red_500));
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
