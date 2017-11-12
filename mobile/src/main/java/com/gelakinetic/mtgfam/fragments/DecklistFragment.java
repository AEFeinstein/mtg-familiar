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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.DecklistDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import org.apache.commons.collections4.comparators.ComparatorChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/**
 * This fragment shows a deck, and allows you to add to and modify it.
 */
public class DecklistFragment extends FamiliarListFragment {

    /* UI Elements */
    public TextView mDeckName;
    public TextView mDeckCards;

    /* Decklist and adapters */
    public ArrayList<CompressedDecklistInfo> mCompressedDecklist;
    private ComparatorChain<CompressedDecklistInfo> mDecklistChain;

    public static final String AUTOSAVE_NAME = "autosave";
    public String mCurrentDeck = "";
    public static final String DECK_EXTENSION = ".deck";

    /**
     * Create the view, pull out UI elements, and set up the listener for the "add cards" button.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in
     *                           the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should
     *                           be attached to. The fragment should not add the view itself, but
     *                           this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return The view to be displayed.
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final View myFragmentView =
                inflater.inflate(R.layout.decklist_frag, container, false);
        assert myFragmentView != null;

        final TextView.OnEditorActionListener addCardListener =
                new TextView.OnEditorActionListener() {

                    @Override
                    public boolean onEditorAction(final TextView textView,
                                                  final int actionId,
                                                  final KeyEvent event) {

                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            addCardToDeck(false);
                            return true;
                        }
                        return false;

                    }

                };

        /* Set up the decklist and adapter, it will be read in onResume() */
        mCompressedDecklist = new ArrayList<>();

        /* Call to set up our shared UI elements */
        initializeMembers(
                myFragmentView,
                new int[]{R.id.cardlist},
                new CardDataAdapter[]{new CardDataAdapter(mCompressedDecklist)},
                addCardListener,
                R.menu.decklist_select_menu);

        myFragmentView.findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View view) {
                addCardToDeck(false);
            }

        });
        myFragmentView.findViewById(R.id.add_card_sideboard).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addCardToDeck(true);
                    }

                });

        /* Decklist information */
        mDeckName = myFragmentView.findViewById(R.id.decklistName);
        mDeckName.setText(R.string.decklist_unnamed_deck);
        mDeckCards = myFragmentView.findViewById(R.id.decklistCards);
        mDeckCards.setText(getResources().getQuantityString(R.plurals.decklist_cards_count, 0, 0));
        mTotalPriceField = myFragmentView.findViewById(R.id.decklistPrice);

        mDecklistChain = new ComparatorChain<>();
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSideboard());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSupertype(
                getResources().getStringArray(R.array.card_types_extra)
        ));
        mDecklistChain.addComparator(new CardHelpers.CardComparatorCMC());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorColor());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorName());

        return myFragmentView;
    }

    /**
     * This function takes care of adding a card to the decklist from this fragment. It makes sure
     * that fields are not null or have bad information.
     *
     * @param isSideboard if the card is in the sideboard
     */
    private void addCardToDeck(final boolean isSideboard) {

        /* Don't allow the fields to be empty */
        if (getCardNameInput() == null || getCardNameInput().length() == 0 ||
                getCardNumberInput() == null || getCardNumberInput().length() == 0) {
            return;
        }

        final String name = String.valueOf(getCardNameInput());
        final String numberOf = String.valueOf(getCardNumberInput());
        final MtgCard card = CardHelpers.makeMtgCard(getContext(), name, null,
                checkboxFoilIsChecked(), Integer.parseInt(numberOf));

        /* If for some reason the card was null, we can just leave */
        if (card == null) {
            return;
        }

        final CompressedDecklistInfo decklistInfo =
                new CompressedDecklistInfo(card, isSideboard);

        /* Add it to the decklist, either as a new CompressedDecklistInfo, or to an existing one */
        if (mCompressedDecklist.contains(decklistInfo)) {
            boolean added = false;
            final int firstIndex = mCompressedDecklist.indexOf(decklistInfo);
            final CompressedDecklistInfo firstCard =
                    mCompressedDecklist.get(firstIndex);
            for (int i = 0; i < firstCard.mInfo.size(); i++) {
                CardHelpers.IndividualSetInfo firstIsi = firstCard.mInfo.get(i);
                if (firstIsi.mSetCode.equals(card.setCode) && firstIsi.mIsFoil.equals(card.foil)) {
                    firstIsi.mNumberOf++;
                    added = true;
                    break;
                }
            }
            if (!added) {
                firstCard.add(card);
            }
        } else {
            mCompressedDecklist.add(new CompressedDecklistInfo(card, isSideboard));
        }

        /* The headers shouldn't (and can't) be sorted */
        clearHeaders();

        /* Load the card's price */
        if (shouldShowPrice()) {
            loadPrice(card.mName, card.setCode, card.mNumber);
        }

        /* Sort the decklist */
        Collections.sort(mCompressedDecklist, mDecklistChain);

        /* Save the decklist */
        DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist);

        /* Clean up for the next add */
        clearCardNumberInput();
        clearCardNameInput();

        /* Uncheck the foil box if it isn't locked */
        uncheckFoilCheckbox();

        /* Update the number of cards listed */
        mDeckCards.setText(getResources().getQuantityString(R.plurals.decklist_cards_count,
                ((CardDataAdapter) getCardDataAdapter(0)).getTotalCards(),
                ((CardDataAdapter) getCardDataAdapter(0)).getTotalCards()));

        /* Redraw the new decklist with the new card */
        setHeaderValues();
        getCardDataAdapter(0).notifyDataSetChanged();

    }

    /**
     * Total the cards, and set it. Get the setting for price. And load up the decklist.
     */
    @Override
    public void onResume() {

        super.onResume();
        mPriceSetting = Integer.parseInt(PreferenceAdapter.getDeckPrice(getContext()));
        mCompressedDecklist.clear();
        readAndCompressDecklist(null, mCurrentDeck);
        getCardDataAdapter(0).notifyDataSetChanged();
        mDeckCards.setText(getResources().getQuantityString(R.plurals.decklist_cards_count,
                ((CardDataAdapter) getCardDataAdapter(0)).getTotalCards(),
                ((CardDataAdapter) getCardDataAdapter(0)).getTotalCards()));
    }

    /**
     * Sets the deck name header, and returns the full name with extension.
     *
     * @param deckName name of the deck, null if autosave
     * @return the name of the deck with extension
     */
    private String getAndSetDeckName(final @Nullable String deckName) {

        String lDeckName;
        if (deckName == null || deckName.equals("") || deckName.equals(AUTOSAVE_NAME)) {
            lDeckName = AUTOSAVE_NAME;
            mDeckName.setText(R.string.decklist_unnamed_deck);
        } else {
            lDeckName = deckName;
            mDeckName.setText(lDeckName);
        }
        return lDeckName + DECK_EXTENSION;

    }

    /**
     * Clears the compressed decklist info.
     *
     * @param cardChanged the card that was changed, null to clear everything
     */
    private void clearCompressedInfo(final @Nullable String cardChanged) {

        if (cardChanged == null) {
            mCompressedDecklist.clear();
            return;
        }
        for (final CompressedDecklistInfo cdi : mCompressedDecklist) {
            if (!cdi.mName.isEmpty() && cdi.mName.equals(cardChanged)) {
                cdi.clearCompressedInfo();
            }
        }

    }

    /**
     * Read in the decklist from the file, and pack it into an ArrayList of CompressedDecklistInfo
     * for display in a ListView. This data structure stores one copy of the card itself, and a list
     * of set-specific attributes like the set name and rarity.
     *
     * @param changedCardName card that was changed inside the list
     * @param deckName        name of the deck that is loaded
     */
    public void readAndCompressDecklist(final String changedCardName, final String deckName) {

        final String lDeckName = getAndSetDeckName(deckName);

        /* Read the decklist */
        final ArrayList<Pair<MtgCard, Boolean>> decklist =
                DecklistHelpers.ReadDecklist(getActivity(), lDeckName);

        try {
            final SQLiteDatabase database =
                    DatabaseManager.getInstance(getActivity(), false)
                            .openDatabase(false);
            /* Clear the decklist, or just the card that changed */
            clearCompressedInfo(changedCardName);

            /* Compress the whole decklist, or just the card that changed */
            for (Pair<MtgCard, Boolean> card : decklist) {
                /* It's possible for empty cards to be saved, though I don't know how. Don't add them back */
                if (!card.first.mName.isEmpty()) {
                    /* Translate the set code to TCG name of course it's not saved */
                    card.first.setName = CardDbAdapter.getSetNameFromCode(card.first.setCode, database);
                    if (changedCardName == null || changedCardName.equals(card.first.mName)) {
                        CompressedDecklistInfo wrapped =
                                new CompressedDecklistInfo(card.first, card.second);
                        if (mCompressedDecklist.contains(wrapped)) {
                            mCompressedDecklist.get(mCompressedDecklist.indexOf(wrapped))
                                    .add(card.first);
                        } else {
                            mCompressedDecklist.add(wrapped);
                        }
                        if (shouldShowPrice()) {
                            loadPrice(card.first.mName, card.first.setCode, card.first.mNumber);
                        }
                    }
                }
            }
            /* check for wholly removed cards if one card was modified */
            if (changedCardName != null) {
                for (int i = 0; i < mCompressedDecklist.size(); i++) {
                    if (mCompressedDecklist.get(i).mInfo.size() == 0) {
                        mCompressedDecklist.remove(i);
                        i--;
                    }
                }
            }
            /* Fill extra card data from the database, for displaying full card info */
            CardDbAdapter.fillExtraWishlistData(mCompressedDecklist, database);
            Collections.sort(mCompressedDecklist, mDecklistChain);
            setHeaderValues();
            mDeckCards.setText(getResources().getQuantityString(R.plurals.decklist_cards_count,
                    ((CardDataAdapter) getCardDataAdapter(0)).getTotalCards(),
                    ((CardDataAdapter) getCardDataAdapter(0)).getTotalCards()));
        } catch (FamiliarDbException fde) {
            handleFamiliarDbException(false);
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * This notifies the fragment when a change has been made from a card's dialog.
     *
     * @param cardName the card that was changed
     */
    @Override
    public void onWishlistChanged(String cardName) {

        readAndCompressDecklist(cardName, mCurrentDeck);
        clearHeaders();
        Collections.sort(mCompressedDecklist, mDecklistChain);
        setHeaderValues();
        getCardDataAdapter(0).notifyDataSetChanged();

    }

    /**
     * Remove any showing dialogs, and show the requested one.
     *
     * @param id          the ID of the dialog to show
     * @param cardName    the name of the card to use if this is a dialog to change decklist counts
     * @param isSideboard if the card is in the sideboard
     * @throws IllegalStateException If something is done out of order
     */
    private void showDialog(final int id, final String cardName, final boolean isSideboard)
            throws IllegalStateException {

        if (!this.isVisible()) {
            return;
        }
        removeDialog(getFragmentManager());
        DecklistDialogFragment newFragment = new DecklistDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        arguments.putString(DecklistDialogFragment.NAME_KEY, cardName);
        arguments.putBoolean(DecklistDialogFragment.SIDE_KEY, isSideboard);
        newFragment.setArguments(arguments);
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);

    }

    /**
     * Handle an ActionBar item click.
     *
     * @param item the item clicked
     * @return true if the click was acted on
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        /* handle item selection */
        switch (item.getItemId()) {
            case R.id.deck_menu_save_as: {
                showDialog(DecklistDialogFragment.DIALOG_SAVE_DECK_AS, null, false);
                return true;
            }
            case R.id.deck_menu_load: {
                showDialog(DecklistDialogFragment.DIALOG_LOAD_DECK, null, false);
                return true;
            }
            case R.id.deck_menu_delete: {
                showDialog(DecklistDialogFragment.DIALOG_DELETE_DECK, null, false);
                return true;
            }
            case R.id.deck_menu_clear: {
                showDialog(DecklistDialogFragment.DIALOG_CONFIRMATION, null, false);
                return true;
            }
            case R.id.deck_menu_share: {
                /* Share plaintext decklist */
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.decklist_share_title);
                sendIntent.putExtra(Intent.EXTRA_TEXT, DecklistHelpers
                        .getSharableDecklist(mCompressedDecklist, getActivity()));
                sendIntent.setType("text/plain");
                try {
                    startActivity(Intent.createChooser(sendIntent,
                            getString(R.string.decklist_share)));
                } catch (ActivityNotFoundException anfe) {
                    ToastWrapper.makeAndShowText(getActivity(), R.string.error_no_email_client,
                            ToastWrapper.LENGTH_LONG);
                }
                return true;
            }
            case R.id.deck_menu_save: {
                final StringBuilder deckName = new StringBuilder(mCurrentDeck);
                if (deckName.toString().equals("")) {
                    deckName.append(DecklistFragment.AUTOSAVE_NAME);
                }
                final String savedToast = getString(R.string.decklist_saved_toast, deckName);
                deckName.append(DecklistFragment.DECK_EXTENSION);
                DecklistHelpers.WriteCompressedDecklist(getContext(), mCompressedDecklist,
                        deckName.toString());
                ToastWrapper.makeAndShowText(getActivity(), savedToast, ToastWrapper.LENGTH_SHORT);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }

    }

    /**
     * Removes all of the headers.
     */
    private void clearHeaders() {

        for (int i = 0; i < mCompressedDecklist.size(); i++) {
            if (mCompressedDecklist.get(i).header != null) { /* We found our header */
                /* Now remove it, and then back up a step */
                mCompressedDecklist.remove(i);
                getCardDataAdapter(0).notifyItemRemoved(i);
                i--;
            }
        }

    }

    /**
     * Inserts a header entry at the given position with the given text.
     *
     * @param position   where to put the header
     * @param headerText what the header says
     * @return true if the header is inserted, false if it isn't
     */
    private boolean insertHeaderAt(final int position, final String headerText) {

        final CompressedDecklistInfo header = new CompressedDecklistInfo(new MtgCard(), false);
        header.header = headerText;
        if (!mCompressedDecklist.contains(header)) {
            mCompressedDecklist.add(position, header);
            getCardDataAdapter(0).notifyItemInserted(position);
            return true;
        }
        return false;

    }

    /**
     * Inserts the headers for each type.
     */
    private void setHeaderValues() {

        final String[] cardTypes = getResources().getStringArray(R.array.card_types_extra);
        final String[] cardHeaders = getResources().getStringArray(R.array.decklist_card_headers);

        for (int i = 0; i < mCompressedDecklist.size(); i++) {
            for (int j = 0; j < cardTypes.length; j++) {
                final CompressedDecklistInfo cdi = mCompressedDecklist.get(i);
                if (!cdi.mName.equals("") /* We only want entries that have a card attached */
                        && (i == 0 || mCompressedDecklist.get(i - 1).header == null)
                        && ((CardDataAdapter) getCardDataAdapter(0)).getTotalNumberOfType(j) > 0) {
                    if (cdi.mIsSideboard /* it is in the sideboard */
                            /* The sideboard header isn't already there */
                            && !insertHeaderAt(i, cardHeaders[0])) {
                        break;
                    } else if (j < cardHeaders.length - 1 /* if j is in range */
                            /* the current card has the selected card type */
                            && cdi.mType.contains(cardTypes[j])
                            /* There isn't already a header */
                            && !insertHeaderAt(i, cardHeaders[j + 1])) {
                        break;
                    } else if (j >= cardHeaders.length - 1
                            && !insertHeaderAt(i, cardHeaders[cardHeaders.length - 1])) {
                        break;
                    }
                }
            }
        }

    }

    /**
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.decklist_menu, menu);

    }

    /**
     * Load the price for a given card. This handles all the spice stuff.
     *
     * @param mCardName   The name of the card to find a price for
     * @param mSetCode    The set code of the card to find a price for
     * @param mCardNumber The collector's number
     */
    private void loadPrice(final String mCardName, final String mSetCode, String mCardNumber) {

        PriceFetchRequest priceRequest =
                new PriceFetchRequest(mCardName, mSetCode, mCardNumber, -1, getActivity());
        mPriceFetchRequests++;
        getFamiliarActivity().setLoading();
        getFamiliarActivity().mSpiceManager.execute(priceRequest, mCardName + "-" +
                mSetCode, DurationInMillis.ONE_DAY, new RequestListener<PriceInfo>() {

            /**
             * Loading the price for this card failed and threw a spiceException.
             *
             `             * @param spiceException The exception thrown when trying to load this card's price
             */
            @Override
            public void onRequestFailure(SpiceException spiceException) {

                /* because this can return when the fragment is in the background */
                if (DecklistFragment.this.isAdded()) {
                    /* Find the compressed wishlist info for this card */
                    for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                        if (cdi.header == null && cdi.mName.equals(mCardName)) {
                            /* Find all foil and non foil compressed items with the same set code */
                            for (CardHelpers.IndividualSetInfo isi : cdi.mInfo) {
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
                    getCardDataAdapter(0).notifyDataSetChanged();
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
                if (DecklistFragment.this.isAdded()) {
                    /* Find the compressed wishlist info for this card */
                    for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                        if (cdi.header == null && cdi.mName.equals(mCardName)) {
                            /* Find all foil and non foil compressed items with the same set code */
                            for (CardHelpers.IndividualSetInfo isi : cdi.mInfo) {
                                if (isi.mSetCode.equals(mSetCode)) {
                                    /* Set the whole price info object */
                                    if (result != null) {
                                        isi.mPrice = result;
                                    }
                                    /* The message will never be shown with a valid price,
                                     * so set it as DNE */
                                    isi.mMessage = getString(R.string.card_view_price_not_found);
                                }
                            }
                        }
                        updateTotalPrices(0);
                    }
                    mPriceFetchRequests--;
                    if (mPriceFetchRequests == 0) {
                        getFamiliarActivity().clearLoading();
                    }
                }
            }

        });

    }

    /**
     * Add together the price of all the cards in the wishlist and display it.
     */
    void updateTotalPrices(int side) {

        /* default */
        float totalPrice = 0;

        for (CompressedDecklistInfo cdi : mCompressedDecklist) {
            if (cdi.header == null) {
                for (CardHelpers.IndividualSetInfo isi : cdi.mInfo) {
                    if (isi.mPrice != null) {
                        if (isi.mIsFoil) {
                            totalPrice += isi.mPrice.mFoilAverage * isi.mNumberOf;
                        } else {
                            switch (mPriceSetting) {
                                case LOW_PRICE:
                                    totalPrice += isi.mPrice.mLow * isi.mNumberOf;
                                    break;
                                case AVG_PRICE:
                                    totalPrice += isi.mPrice.mAverage * isi.mNumberOf;
                                    break;
                                case HIGH_PRICE:
                                    totalPrice += isi.mPrice.mHigh * isi.mNumberOf;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
        mTotalPriceField.setText(String.format(Locale.US, PRICE_FORMAT, totalPrice));

    }

    @Override
    boolean shouldShowPrice() {
        return PreferenceAdapter.getShowTotalDecklistPrice(getContext());
    }

    /**
     * The adapter that drives the deck list.
     */
    public class CardDataAdapter
            extends FamiliarListFragment.CardDataAdapter<CompressedDecklistInfo, CardDataAdapter.ViewHolder> {

        /**
         * Create the adapter.
         *
         * @param values the data set
         */
        CardDataAdapter(ArrayList<CompressedDecklistInfo> values) {
            super(values);
        }

        @Override
        public ViewHolder onCreateViewHolder(
                ViewGroup parent,
                int viewType) {
            return new ViewHolder(parent);
        }


        /**
         * On binding the view holder.
         *
         * @param holder   the holder being bound
         * @param position where the holder is
         */
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            final CompressedDecklistInfo info = getItem(position);

            if (isItemPendingRemoval(position)) {
                holder.itemView.findViewById(R.id.card_row_full).setVisibility(View.GONE);
            } else { /* if the item IS NOT pending removal */
                holder.itemView.findViewById(R.id.card_row_full).setVisibility(View.VISIBLE);
                if (info.header == null) {

                    /* Enable the on click listener */
                    holder.itemView.setOnClickListener(holder);
                    holder.itemView.setOnLongClickListener(holder);

                    /* Do the selection stuff */
                    if (isItemSelected(position)) {
                        setItemSelected(holder.itemView, position, true, false);
                        holder.mCardNumberOf.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_menu_done, 0, 0, 0);
                        holder.mCardNumberOf.setText("");
                    } else {
                        setItemSelected(holder.itemView, position, false, false);
                        holder.mCardNumberOf
                                .setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        holder.mCardNumberOf.setText(String.valueOf(info.getTotalNumber()));
                    }

                    /* set up the card's views */
                    holder.itemView.findViewById(R.id.card_row).setVisibility(View.VISIBLE);
                    View separator = holder.itemView.findViewById(R.id.decklistSeparator);
                    separator.setVisibility(View.GONE);
                    Html.ImageGetter imageGetter = ImageGetterHelper.GlyphGetter(getActivity());
                    holder.mCardName.setText(info.mName);
                    holder.mCardCost.setText(ImageGetterHelper
                            .formatStringWithGlyphs(info.mManaCost, imageGetter));
                    holder.setIsSwipeable(true);
                } else {
                    /* The header uses the same layout, just set it up */
                    holder.itemView.setOnClickListener(null);
                    holder.itemView.setOnLongClickListener(null);
                    final int typeIndex = Arrays.asList(
                            getResources().getStringArray(R.array.decklist_card_headers)
                    ).indexOf(info.header) - 1;
                    holder.itemView.findViewById(R.id.decklistSeparator)
                            .setVisibility(View.VISIBLE);
                    holder.itemView.findViewById(R.id.card_row).setVisibility(View.GONE);
                    ((TextView) holder.itemView.findViewById(R.id.decklistHeaderType))
                            .setText(info.header);
                    ((TextView) holder.itemView.findViewById(R.id.decklistHeaderNumber))
                            .setText(String.valueOf(getTotalNumberOfType(typeIndex)));
                    holder.setIsSwipeable(false);
                }
            }

        }

        /**
         * Get the number of cards of the type to display in the header.
         *
         * @param typeIndex the card type's index we are counting, -1 if it is the sideboard
         * @return the number of cards of the given type
         */
        int getTotalNumberOfType(final int typeIndex) {

            /* default */
            int totalCards = 0;
            String[] types = getResources().getStringArray(R.array.card_types_extra);

            for (int i = 0; i < getItemCount(); i++) {
                CompressedDecklistInfo cdi = getItem(i);
                if (cdi.header != null) {
                    continue;
                }
                final int position = itemsIndexOf(cdi);
                /* The card is NOT pending removal */
                if (!isItemPendingRemoval(position)
                        /* The type is not above -1 OR is not in the sideboard */
                        && (!(typeIndex > -1) || !cdi.mIsSideboard)
                        /* The type is above -1 OR the card is in the sideboard */
                        && (typeIndex > -1 || cdi.mIsSideboard)
                        /* The card is in the sideboard OR the card is the wanted type */
                        && (cdi.mIsSideboard || cdi.mType.contains(types[typeIndex]))) {
                    /* There of course are edge cases */
                    final boolean lookForEnchant = types[typeIndex > -1 ? typeIndex : 0]
                            .equals(types[5]);
                    final boolean isCreature = cdi.mType.contains(types[0]);
                    if (typeIndex > -1 /* Make sure we aren't working on the sideboard */
                            /* Are we looking for enchantments or is the object a creature? */
                            && (lookForEnchant || isCreature)
                            /* Are we looking for enchantments or are we looking for a land? */
                            && (lookForEnchant || types[typeIndex].contains(types[6]))
                            /* Is the current object a creature or is it an artifact? */
                            && (isCreature || cdi.mType.contains(types[4]))) {
                        continue; /* Skip right over to the next iteration */
                    }
                    totalCards += cdi.getTotalNumber();
                }
            }
            return totalCards;

        }

        /**
         * Get the total number of cards in this adapter.
         *
         * @return the total number of cards
         */
        int getTotalCards() {

            int totalCards = 0;
            for (int i = 0; i < getItemCount(); i++) {
                totalCards += getItem(i).getTotalNumber();
            }
            return totalCards;

        }

        @Override
        public boolean pendingRemoval(int position) {

            super.pendingRemoval(position);

            clearHeaders();
            setHeaderValues();

            return true;

        }

        @Override
        public String getItemName(int position) {
            return getItem(position).mName;
        }

        @Override
        public void remove(int position) {

            super.remove(position);

            mDeckCards.setText(getResources().getQuantityString(R.plurals.decklist_cards_count, getTotalCards(), getTotalCards()));
            clearHeaders();
            notifyItemRemoved(position);
            setHeaderValues();
            DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist);

        }

        class ViewHolder extends FamiliarListFragment.CardDataAdapter.ViewHolder {

            private final TextView mCardNumberOf;
            private final TextView mCardCost;

            ViewHolder(ViewGroup view) {

                super(view, R.layout.decklist_card_row);

                mCardNumberOf = itemView.findViewById(R.id.decklistRowNumber);
                mCardCost = itemView.findViewById(R.id.decklistRowCost);

            }

            @Override
            public void onClickNotSelectMode(View view) {
                /* if we aren't in select mode, open a dialog to edit this card */
                final CompressedDecklistInfo item = getItem(getAdapterPosition());
                showDialog(DecklistDialogFragment.DIALOG_UPDATE_CARD,
                        item.mName, item.mIsSideboard);
            }

        }

    }

}
