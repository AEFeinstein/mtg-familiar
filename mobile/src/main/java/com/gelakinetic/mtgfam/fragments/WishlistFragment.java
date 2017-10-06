package com.gelakinetic.mtgfam.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.WishlistDialogFragment;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.SelectableItemTouchHelper;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/**
 * This class displays a wishlist of cards, details about the cards, their prices, and the sum of their prices
 */
public class WishlistFragment extends FamiliarListFragment {

    /* Preferences */
    private boolean mShowCardInfo;
    private boolean mShowIndividualPrices;
    private boolean mShowTotalWishlistPrice;

    /* The wishlist and adapter */
    public ArrayList<CompressedWishlistInfo> mCompressedWishlist;
    private View mTotalPriceDivider;
    private int mOrderAddedIdx = 0;

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

        /* Make sure to initialize shared members */
        initializeMembers(myFragmentView);

        /* set the autocomplete for card names */
        mNameField.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME}, new int[]{R.id.text1}, mNameField, false));
        mNameField.setOnEditorActionListener(addCardListener);

        /* Default the number of cards field */
        mNumberOfField.setText("1");
        mNumberOfField.setOnEditorActionListener(addCardListener);

        /* Grab other elements */
        mTotalPriceField = myFragmentView.findViewById(R.id.priceText);
        mTotalPriceDivider = myFragmentView.findViewById(R.id.divider_total_price);
        mListView.setLayoutManager(new LinearLayoutManager(getContext()));

        myFragmentView.findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCardToWishlist();
            }
        });

        /* Set up the wishlist and adapter, it will be read in onResume() */
        mCompressedWishlist = new ArrayList<>();
        mListAdapter = new CardDataAdapter(mCompressedWishlist);
        mListView.setAdapter(mListAdapter);

        ItemTouchHelper.SimpleCallback callback =
                new SelectableItemTouchHelper(mListAdapter, ItemTouchHelper.LEFT);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(mListView);

        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.action_mode_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.deck_delete_selected: {
                        mListAdapter.deleteSelectedItems();
                        mActionMode.finish();
                        return true;
                    }
                    default: {
                        return false;
                    }
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mListAdapter.deselectAll();
            }
        };

        myFragmentView.findViewById(R.id.camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFamiliarActivity().startTutorCardsSearch();
            }
        });
        myFragmentView.findViewById(R.id.camera_button).setVisibility(View.GONE);

        setUpCheckBoxClickListeners();

        return myFragmentView;
    }

    @Override
    public void onPause() {
        super.onPause();
        /* unsort, then save the wishlist */
        sortWishlist(SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_ASC);
        WishlistHelpers.WriteCompressedWishlist(getActivity(), mCompressedWishlist);
    }

    /**
     * This function takes care of adding a card to the wishlist from this fragment. It makes sure that fields are
     * not null or have bad information.
     */
    private void addCardToWishlist() {
        /* Do not allow empty fields */
        String name = String.valueOf(mNameField.getText());
        if (name == null || name.equals("")) {
            return;
        }
        String numberOf = (String.valueOf(mNumberOfField.getText()));
        if (numberOf == null || numberOf.equals("")) {
            return;
        }

        MtgCard card = CardHelpers.makeMtgCard(getContext(), name, null, mCheckboxFoil.isChecked(), Integer.parseInt(numberOf));
        CompressedWishlistInfo wrapped = new CompressedWishlistInfo(card, 0);

        /* Add it to the wishlist, either as a new CompressedWishlistInfo, or to an existing one */
        if (mCompressedWishlist.contains(wrapped)) {
            CompressedWishlistInfo cwi = mCompressedWishlist.get(mCompressedWishlist.indexOf(wrapped));
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
            mCompressedWishlist.add(new CompressedWishlistInfo(card, mOrderAddedIdx++));
        }

        /* load the price */
        loadPrice(card.mName, card.setCode, card.mNumber);

        /* Sort the wishlist */
        sortWishlist(PreferenceAdapter.getWishlistSortOrder(getContext()));

        /* Clean up for the next add */
        mNumberOfField.setText("1");
        mNameField.setText("");
        /* Only unselect the checkbox if it isn't locked */
        if (!mCheckboxFoilLocked) {
            mCheckboxFoil.setChecked(false);
        }
        /* Redraw the new wishlist with the new card */
        mListAdapter.notifyDataSetChanged();

        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * Read the preferences, show or hide the total price, read and compress the wishlist, and load prices
     */
    @Override
    public void onResume() {
        super.onResume();

        /* Get the relevant preferences */
        mPriceSetting = Integer.parseInt(PreferenceAdapter.getTradePrice(getContext()));
        mShowIndividualPrices = PreferenceAdapter.getShowIndividualWishlistPrices(getContext());
        mShowTotalWishlistPrice = PreferenceAdapter.getShowTotalWishlistPrice(getContext());
        mShowCardInfo = PreferenceAdapter.getVerboseWishlist(getContext());

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
        mListAdapter.notifyDataSetChanged();
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
        try {
            SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
            boolean cardNumberFixed = false;
            /* Translate the set code to tcg name, of course it's not saved */
            for (MtgCard card : wishlist) {
                card.setName = CardDbAdapter.getSetNameFromCode(card.setCode, database);

                /* If the number is empty because of a prior bug, get it from the database */
                if (card.mNumber.equals("")) {
                    Cursor numberCursor = CardDbAdapter.fetchCardByName(card.mName, Arrays.asList(CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_CODE), false, database);
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
                    if (cwi.mName.equals(changedCardName)) {
                        cwi.clearCompressedInfo();
                    }
                }
            }

            /* Compress the whole wishlist, or just the card that changed */
            for (MtgCard card : wishlist) {
                if (changedCardName == null || changedCardName.equals(card.mName)) {
                    /* This works because both MtgCard's and CompressedWishlistInfo's .equals() can compare each
                     * other */
                    CompressedWishlistInfo wrapped = new CompressedWishlistInfo(card, 0);
                    if (mCompressedWishlist.contains(wrapped)) {
                        mCompressedWishlist.get(mCompressedWishlist.indexOf(wrapped)).add(card);
                    } else {
                        mCompressedWishlist.add(new CompressedWishlistInfo(card, mOrderAddedIdx++));
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
                sortWishlist(SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_ASC);
                WishlistHelpers.WriteCompressedWishlist(getContext(), mCompressedWishlist);
                sortWishlist(PreferenceAdapter.getWishlistSortOrder(getContext()));
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
        mListAdapter.notifyDataSetChanged();
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
                    PreferenceAdapter.getWishlistSortOrder(getContext()));
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
                        if (cwi.mName.equals(mCardName)) {
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
                    mListAdapter.notifyDataSetChanged();
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
                        if (cwi.mName.equals(mCardName)) {
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
                    sortWishlist(PreferenceAdapter.getWishlistSortOrder(getContext()));
                    mListAdapter.notifyDataSetChanged();
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
                                default:
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
        PreferenceAdapter.setWishlistSortOrder(getContext(), orderByStr);
        sortWishlist(orderByStr);
    }

    /**
     * Sorts the wishlist based on mWishlistSortType and mWishlistSortOrder
     */
    private void sortWishlist(String orderByStr) {
        Collections.sort(mCompressedWishlist,
                new WishlistHelpers.WishlistComparator(orderByStr, mPriceSetting));
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * The adapter that drives the wish list
     */
    public class CardDataAdapter
            extends FamiliarListFragment
            .CardDataAdapter<CompressedWishlistInfo, CardDataAdapter.ViewHolder> {

        CardDataAdapter(ArrayList<CompressedWishlistInfo> values) {
            super(values);
        }

        @Override
        public String getItemName(int position) {
            return items.get(position).mName;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            /* Get all the wishlist info for this entry */
            final CompressedWishlistInfo info = items.get(position);

            if (!isInSelectMode()) {
                /* Sometimes an item will be selected after we exit select mode */
                holder.itemView.setSelected(false);
            }

            if (isItemPendingRemoval(position)) {
                holder.itemView.findViewById(R.id.card_row_full).setVisibility(View.GONE);
            } else {

                /* Make sure you can see the item */
                holder.itemView.findViewById(R.id.card_row_full).setVisibility(View.VISIBLE);

                /* Clear out the old items in the view */
                holder.mWishlistSets.removeAllViews();

                /* Set the card name, always */
                holder.mCardName.setText(info.mName);

                /* Show or hide full card information */
                holder.itemView.findViewById(R.id.cardset).setVisibility(View.GONE);
                if (mShowCardInfo) {
                    Html.ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getActivity());
                    /* make sure everything is showing */
                    holder.mCardCost.setVisibility(View.VISIBLE);
                    holder.mCardType.setVisibility(View.VISIBLE);
                    holder.mCardText.setVisibility(View.VISIBLE);
                    /* Show the power, toughness, or loyalty if the card has it */
                    holder.mCardPower.setVisibility(View.GONE);
                    holder.mCardSlash.setVisibility(View.GONE);
                    holder.mCardToughness.setVisibility(View.GONE);
                    /* Set the type, cost, and ability */
                    holder.mCardType.setText(info.mType);
                    holder.mCardCost.setText(ImageGetterHelper.formatStringWithGlyphs(info.mManaCost, imgGetter));
                    holder.mCardText.setText(ImageGetterHelper.formatStringWithGlyphs(info.mText, imgGetter));
                    try {
                        String power = CardHelpers.adaptCardPT(info.mPower);
                        String toughness = CardHelpers.adaptCardPT(info.mToughness);
                        holder.mCardPower.setText(power);
                        holder.mCardToughness.setText(toughness);
                        holder.mCardPower.setVisibility(View.VISIBLE);
                        holder.mCardSlash.setVisibility(View.VISIBLE);
                        holder.mCardToughness.setVisibility(View.VISIBLE);
                    } catch (NumberFormatException nfe) {
                        /* eat it */
                    }

                    /* Show the loyalty, if the card has any (traitor...) */
                    float loyalty = info.mLoyalty;
                    if (loyalty != -1 && loyalty != CardDbAdapter.NO_ONE_CARES) {
                        if (loyalty == CardDbAdapter.X) {
                            holder.mCardToughness.setText("X");
                        } else if (loyalty == (int) loyalty) {
                            holder.mCardToughness.setText(Integer.toString((int) loyalty));
                        } else {
                            holder.mCardToughness.setText(Float.toString(loyalty));
                        }
                        holder.mCardToughness.setVisibility(View.VISIBLE);
                    }
                } else {
                    /* hide all the extra fields */
                    holder.mCardCost.setVisibility(View.GONE);
                    holder.mCardType.setVisibility(View.GONE);
                    holder.mCardText.setVisibility(View.GONE);
                    holder.mCardPower.setVisibility(View.GONE);
                    holder.mCardSlash.setVisibility(View.GONE);
                    holder.mCardToughness.setVisibility(View.GONE);
                }

                /* Rarity is displayed on the expansion lines */
                holder.itemView.findViewById(R.id.rarity).setVisibility(View.GONE);

                /* List all the sets and wishlist values for this card */
                for (IndividualSetInfo isi : info.mInfo) {
                    /* inflate a new row */
                    View setRow = getActivity().getLayoutInflater().inflate(R.layout.wishlist_cardset_row, (ViewGroup) holder.itemView.getParent(), false);
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
                    TextView priceText = setRow.findViewById(R.id.wishlistRowPrice);
                    if (mShowIndividualPrices) {
                        if (isi.mIsFoil) {
                            if (isi.mPrice == null || isi.mPrice.mFoilAverage == 0) {
                                priceText.setText(String.format(Locale.US, "%dx %s", isi.mNumberOf, isi.mMessage));
                                priceText.setTextColor(ContextCompat.getColor(getContext(), R.color.material_red_500));
                            } else {
                                priceText.setText(String.format(Locale.US, "%dx $%.02f", isi.mNumberOf, isi.mPrice.mFoilAverage));
                            }
                        } else {
                            boolean priceFound = false;
                            if (isi.mPrice != null) {
                                switch (mPriceSetting) {
                                    case LOW_PRICE:
                                        if (isi.mPrice.mLow != 0) {
                                            priceText.setText(String.format(Locale.US, "%dx $%.02f", isi.mNumberOf, isi.mPrice.mLow));
                                            priceFound = true;
                                        }
                                        break;
                                    case AVG_PRICE:
                                        if (isi.mPrice.mAverage != 0) {
                                            priceText.setText(String.format(Locale.US, "%dx $%.02f", isi.mNumberOf, isi.mPrice.mAverage));
                                            priceFound = true;
                                        }
                                        break;
                                    case HIGH_PRICE:
                                        if (isi.mPrice.mHigh != 0) {
                                            priceText.setText(String.format(Locale.US, "%dx $%.02f", isi.mNumberOf, isi.mPrice.mHigh));
                                            priceFound = true;
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                priceText.setTextColor(ContextCompat.getColor(getContext(),
                                        getResourceIdFromAttr(R.attr.color_text)));
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
                    holder.mWishlistSets.addView(setRow);
                }
            }
        }

        class ViewHolder extends FamiliarListFragment.CardDataAdapter.ViewHolder {

            /* Card Information */
            final TextView mCardType;
            final TextView mCardText;
            final TextView mCardPower;
            final TextView mCardSlash;
            final TextView mCardToughness;
            final TextView mCardCost;

            /* For adding individual wishlist sets */
            final LinearLayout mWishlistSets;

            ViewHolder(ViewGroup view) {
                super(view, R.layout.result_list_card_row);

                mCardType = itemView.findViewById(R.id.cardtype);
                mCardText = itemView.findViewById(R.id.cardability);
                mCardPower = itemView.findViewById(R.id.cardp);
                mCardSlash = itemView.findViewById(R.id.cardslash);
                mCardToughness = itemView.findViewById(R.id.cardt);
                mCardCost = itemView.findViewById(R.id.cardcost);
                mWishlistSets = itemView.findViewById(R.id.wishlist_sets);
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View view) {
                if (!isInSelectMode()) {
                    showDialog(WishlistDialogFragment.DIALOG_UPDATE_CARD,
                            mCardName.getText().toString());
                }
                super.onClick(view);
            }

        }

    }

}
