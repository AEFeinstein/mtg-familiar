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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.DecklistDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.helpers.CardDataAdapter;
import com.gelakinetic.mtgfam.helpers.CardDataViewHolder;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import org.apache.commons.collections4.comparators.ComparatorChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * This fragment shows a deck, and allows you to add to and modify it.
 */
public class DecklistFragment extends FamiliarListFragment {

    /* UI Elements */
    public TextView mDeckName;
    private TextView mDeckCards;

    /* Decklist and adapters */
    public final List<CompressedDecklistInfo> mCompressedDecklist = Collections.synchronizedList(new ArrayList<>());
    private ComparatorChain<CompressedDecklistInfo> mDecklistChain;

    public static final String AUTOSAVE_NAME = "autosave";
    public String mCurrentDeck = AUTOSAVE_NAME;
    public static final String DECK_EXTENSION = ".deck";

    private static final String FRAGMENT_TAG = "decklist";
    private static final String CURRENT_DECKLIST_TAG = "decklist_name";
    private LegalityCheckerTask mLegalityCheckerTask = null;

    public static final String[] LEGALITY_DIAOG_FROM = new String[]{"format", "status"};
    public static final int[] LEGALITY_DIALOG_TO = new int[]{R.id.format, R.id.status};
    public final List<HashMap<String, String>> legalityMap = new ArrayList<>();

    static class LegalityCheckerTask extends AsyncTask<DecklistFragment, Void, DecklistFragment> {

        @Override
        protected DecklistFragment doInBackground(DecklistFragment... decklistFragments) {
            DecklistFragment parentFrag = decklistFragments[0];

            parentFrag.legalityMap.clear();
            Cursor cFormats = null;
            FamiliarDbHandle handle = new FamiliarDbHandle();
            try {
                SQLiteDatabase database = DatabaseManager.openDatabase(parentFrag.getContext(), false, handle);
                cFormats = CardDbAdapter.fetchAllFormats(database);
                cFormats.moveToFirst();
                for (int i = 0; i < cFormats.getCount(); i++) {
                    boolean deckIsLegal = true;
                    String deckLegality;
                    String format =
                            cFormats.getString(cFormats.getColumnIndex(CardDbAdapter.KEY_NAME));
                    synchronized (parentFrag.mCompressedDecklist) {
                        for (CompressedDecklistInfo info : parentFrag.mCompressedDecklist) {
                            if (!info.getName().isEmpty()) { /* Skip the headers */
                                switch (CardDbAdapter.checkLegality(info.getName(), format, database)) {
                                    case CardDbAdapter.LEGAL: {
                                        if ((format.equalsIgnoreCase("Commander") ||
                                                format.equalsIgnoreCase("Brawl"))
                                                && info.getTotalNumber() > 1 && !info.getType().contains("Basic")) {
                                            deckIsLegal = false;
                                        }
                                        break;
                                    }
                                    case CardDbAdapter.RESTRICTED: {
                                        if (format.equalsIgnoreCase("Vintage")
                                                && info.getTotalNumber() > 1) {
                                            deckIsLegal = false;
                                        }
                                        break;
                                    }
                                    case CardDbAdapter.BANNED: {
                                        deckIsLegal = false;
                                        break;
                                    }
                                }
                                if (!deckIsLegal) {
                                    break;
                                }
                            }
                        }
                    }
                    int minCards = 60;
                    if (format.equals("Commander")) {
                        minCards = 100;
                    }
                    if (((DecklistDataAdapter) parentFrag.getCardDataAdapter(0)).getTotalCards() < minCards) {
                        deckIsLegal = false;
                    }
                    if (deckIsLegal) {
                        deckLegality = parentFrag.getString(R.string.card_view_legal);
                    } else {
                        deckLegality = parentFrag.getString(R.string.decklist_not_legal);
                    }
                    HashMap<String, String> map = new HashMap<>();
                    map.put(LEGALITY_DIAOG_FROM[0], format);
                    map.put(LEGALITY_DIAOG_FROM[1], deckLegality);
                    parentFrag.legalityMap.add(map);
                    cFormats.moveToNext();
                }
            } catch (SQLiteException | FamiliarDbException fdbe) {
                parentFrag.handleFamiliarDbException(false);
            } finally {
                if (null != cFormats) {
                    cFormats.close();
                }
                DatabaseManager.closeDatabase(parentFrag.getContext(), handle);
            }
            return parentFrag;
        }

        @Override
        protected void onPostExecute(DecklistFragment fragment) {
            fragment.getFamiliarActivity().clearLoading();
            fragment.showDialog(DecklistDialogFragment.DIALOG_GET_LEGALITY, null, false);
        }
    }

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final View myFragmentView =
                inflater.inflate(R.layout.decklist_frag, container, false);
        assert myFragmentView != null;

        final TextView.OnEditorActionListener addCardListener =
                (textView, actionId, event) -> {

                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        addCardToDeck(false);
                        return true;
                    }
                    return false;

                };

        /* Call to set up our shared UI elements */
        synchronized (mCompressedDecklist) {
            initializeMembers(
                    myFragmentView,
                    new int[]{R.id.cardlist},
                    new CardDataAdapter[]{new DecklistDataAdapter(mCompressedDecklist)},
                    new int[]{R.id.decklistPrice}, null, R.menu.decklist_select_menu,
                    addCardListener);
        }
        myFragmentView.findViewById(R.id.add_card).setOnClickListener(view -> addCardToDeck(false));
        myFragmentView.findViewById(R.id.add_card_sideboard).setOnClickListener(
                v -> addCardToDeck(true));

        /* Decklist information */
        mDeckName = myFragmentView.findViewById(R.id.decklistName);
        mDeckName.setText(R.string.decklist_unnamed_deck);
        mDeckCards = myFragmentView.findViewById(R.id.decklistCards);
        updateDeckCounts(true);

        mDecklistChain = new ComparatorChain<>();
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSideboard());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSupertype(
                getResources().getStringArray(R.array.card_types_extra)
        ));
        mDecklistChain.addComparator(new CardHelpers.CardComparatorCMC());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorColor());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorName());


        if (savedInstanceState != null) {
            mCurrentDeck = Objects.requireNonNull(savedInstanceState.getBundle(FRAGMENT_TAG)).getString(CURRENT_DECKLIST_TAG);
            readAndCompressDecklist(null, mCurrentDeck);
        }

        return myFragmentView;
    }

    /**
     * Update the card count next to the deck name
     *
     * @param shouldZero True if the value should be zero, false if it should check the actual deck
     */
    private void updateDeckCounts(boolean shouldZero) {
        if (shouldZero) {
            mDeckCards.setText(getResources().getQuantityString(R.plurals.decklist_cards_count, 0, 0, 0));
        } else {
            int[] counts = new int[2];
            ((DecklistDataAdapter) getCardDataAdapter(0)).getDeckCardCounts(counts);
            mDeckCards.setText(getResources().getQuantityString(R.plurals.decklist_cards_count,
                    counts[0] + counts[1],
                    counts[0], counts[1]));
        }
    }

    /**
     * Create a bundle with information to recreate this exact state
     *
     * @return A bundle with the current decklist
     */
    private Bundle saveState() {
        Bundle state = new Bundle();
        state.putString(CURRENT_DECKLIST_TAG, mCurrentDeck);
        return state;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBundle(FRAGMENT_TAG, saveState());
        super.onSaveInstanceState(outState);
    }

    /**
     * Save the current deck when the fragment is paused
     */
    @Override
    public void onPause() {
        super.onPause();
        if (null != mLegalityCheckerTask) {
            mLegalityCheckerTask.cancel(true);
            getFamiliarActivity().clearLoading();
        }
        PreferenceAdapter.setLastLoadedDecklist(getContext(), mCurrentDeck);
        synchronized (mCompressedDecklist) {
            DecklistHelpers.WriteCompressedDecklist(this.getActivity(), mCompressedDecklist, getCurrentDeckName());
        }
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

        ArrayList<String> nonFoilSets;
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            SQLiteDatabase database = DatabaseManager.openDatabase(getContext(), false, handle);
            nonFoilSets = CardDbAdapter.getNonFoilSets(database);
        } catch (SQLiteException | FamiliarDbException | IllegalStateException ignored) {
            nonFoilSets = new ArrayList<>();
        } finally {
            DatabaseManager.closeDatabase(getContext(), handle);
        }

        final String name = String.valueOf(getCardNameInput());
        final String numberOf = String.valueOf(getCardNumberInput());
        try {
            final MtgCard card = new MtgCard(getActivity(), name, null,
                    checkboxFoilIsChecked(), Integer.parseInt(numberOf));

            final CompressedDecklistInfo decklistInfo =
                    new CompressedDecklistInfo(card, isSideboard);

            synchronized (mCompressedDecklist) {
                /* Add it to the decklist, either as a new CompressedDecklistInfo, or to an existing one */
                if (mCompressedDecklist.contains(decklistInfo)) {
                    boolean added = false;
                    final int firstIndex = mCompressedDecklist.indexOf(decklistInfo);
                    final CompressedDecklistInfo firstCard =
                            mCompressedDecklist.get(firstIndex);
                    for (int i = 0; i < firstCard.mInfo.size(); i++) {
                        CardHelpers.IndividualSetInfo firstIsi = firstCard.mInfo.get(i);
                        if (firstIsi.mSetCode.equals(card.getExpansion()) &&
                                (firstIsi.mIsFoil.equals(card.mIsFoil) || nonFoilSets.contains(firstIsi.mSetCode))) {
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
                    loadPrice(card);
                }

                /* Sort the decklist */
                Collections.sort(mCompressedDecklist, mDecklistChain);

                /* Save the decklist */
                DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist, getCurrentDeckName());
            }

            /* Clean up for the next add */
            clearCardNameInput();

            /* Don't reset the count after adding a card. This makes adding consecutive 4-ofs easier */
            /* clearCardNumberInput(); */

            /* Uncheck the foil box if it isn't locked */
            uncheckFoilCheckbox();

            /* Update the number of cards listed */
            updateDeckCounts(false);

            /* Redraw the new decklist with the new card */
            setHeaderValues();
            getCardDataAdapter(0).notifyDataSetChanged();
        } catch (java.lang.InstantiationException e) {
            /* Eat it */
        }

    }

    /**
     * Total the cards, and set it. Get the setting for price. And load up the decklist.
     */
    @Override
    public void onResume() {

        super.onResume();
        synchronized (mCompressedDecklist) {
            mCompressedDecklist.clear();
        }
        mCurrentDeck = PreferenceAdapter.getLastLoadedDecklist(getContext());
        readAndCompressDecklist(null, mCurrentDeck);
        getCardDataAdapter(0).notifyDataSetChanged();
        updateDeckCounts(false);
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

        synchronized (mCompressedDecklist) {
            if (cardChanged == null) {
                mCompressedDecklist.clear();
                return;
            }
            for (final CompressedDecklistInfo cdi : mCompressedDecklist) {
                if (!cdi.getName().isEmpty() && cdi.getName().equals(cardChanged)) {
                    cdi.clearCompressedInfo();
                }
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
        synchronized (mCompressedDecklist) {
            try {
                final String lDeckName = getAndSetDeckName(deckName);

                /* Read the decklist */
                final ArrayList<MtgCard> decklist =
                        DecklistHelpers.ReadDecklist(getActivity(), lDeckName, true);

                /* Clear the decklist, or just the card that changed */
                clearCompressedInfo(changedCardName);

                /* Compress the whole decklist, or just the card that changed */
                for (MtgCard card : decklist) {
                    /* It's possible for empty cards to be saved, though I don't know how. Don't add them back */
                    if (!card.getName().isEmpty()) {
                        if (changedCardName == null || changedCardName.equals(card.getName())) {
                            CompressedDecklistInfo wrapped =
                                    new CompressedDecklistInfo(card, card.isSideboard());
                            if (mCompressedDecklist.contains(wrapped)) {
                                mCompressedDecklist.get(mCompressedDecklist.indexOf(wrapped))
                                        .add(card);
                            } else {
                                mCompressedDecklist.add(wrapped);
                            }
                            if (shouldShowPrice()) {
                                loadPrice(card);
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
                Collections.sort(mCompressedDecklist, mDecklistChain);
                setHeaderValues();
                updateDeckCounts(false);
            } catch (FamiliarDbException e) {
                handleFamiliarDbException(true);
            }
        }
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
        synchronized (mCompressedDecklist) {
            Collections.sort(mCompressedDecklist, mDecklistChain);
        }
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
        if (item.getItemId() == R.id.deck_menu_new) {
            showDialog(DecklistDialogFragment.DIALOG_NEW_DECK, null, false);
            return true;
        } else if (item.getItemId() == R.id.deck_menu_save_as) {
            showDialog(DecklistDialogFragment.DIALOG_SAVE_DECK_AS, null, false);
            return true;
        } else if (item.getItemId() == R.id.deck_menu_load) {
            showDialog(DecklistDialogFragment.DIALOG_LOAD_DECK, null, false);
            return true;
        } else if (item.getItemId() == R.id.deck_menu_delete) {
            showDialog(DecklistDialogFragment.DIALOG_DELETE_DECK, null, false);
            return true;
        } else if (item.getItemId() == R.id.deck_menu_clear) {
            showDialog(DecklistDialogFragment.DIALOG_CONFIRMATION, null, false);
            return true;
        } else if (item.getItemId() == R.id.deck_menu_import) {
            startNewFragment(new ImportFragment(), null);
            return true;
        } else if (item.getItemId() == R.id.deck_menu_share) {
            /* Share plaintext decklist */
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.decklist_share_title);
            synchronized (mCompressedDecklist) {
                sendIntent.putExtra(Intent.EXTRA_TEXT, DecklistHelpers
                        .getSharableDecklist(mCompressedDecklist, getActivity()));
            }
            sendIntent.setType("text/plain");
            try {
                startActivity(Intent.createChooser(sendIntent,
                        getString(R.string.decklist_share)));
            } catch (ActivityNotFoundException anfe) {
                SnackbarWrapper.makeAndShowText(getActivity(), R.string.error_no_email_client,
                        SnackbarWrapper.LENGTH_LONG);
            }
            return true;
        } else if (item.getItemId() == R.id.deck_menu_legality) {
            getFamiliarActivity().setLoading();

            if (null != mLegalityCheckerTask) {
                mLegalityCheckerTask.cancel(true);
            }
            mLegalityCheckerTask = new LegalityCheckerTask();
            mLegalityCheckerTask.execute(this);
            return true;
        } else if (item.getItemId() == R.id.deck_menu_settings) {
            showDialog(DecklistDialogFragment.DIALOG_PRICE_SETTING, null, false);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Save the current deck to the disk
     *
     * @param showSnackbar True to show a message that the deck was saved, false to not
     */
    public void saveCurrentDeck(boolean showSnackbar) {
        String currentDeckName = getCurrentDeckName();
        synchronized (mCompressedDecklist) {
            DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist,
                    currentDeckName);
        }
        if (showSnackbar) {
            SnackbarWrapper.makeAndShowText(getActivity(), getString(R.string.decklist_saved_toast,
                    currentDeckName), SnackbarWrapper.LENGTH_SHORT);
        }
    }

    /**
     * Remove the deck from RAM and clean up the fragment. Doesn't delete the deck
     *
     * @param preserveName true to leave the file name as-is, false to reset it to AUTOSAVE_NAME
     */
    public void clearDeck(boolean preserveName) {
        /* do some cleaning up */
        if (!preserveName) {
            mCurrentDeck = AUTOSAVE_NAME;
        }
        synchronized (mCompressedDecklist) {
            mCompressedDecklist.clear();
        }
        getCardDataAdapter(0).notifyDataSetChanged();
        if (!preserveName) {
            mDeckName.setText(
                    R.string.decklist_unnamed_deck
            );
        }
        updateDeckCounts(true);
        synchronized (mCompressedDecklist) {
            DecklistHelpers.WriteCompressedDecklist(
                    getActivity(),
                    mCompressedDecklist,
                    getCurrentDeckName()
            );
        }
        clearCardNameInput();
        clearCardNumberInput();
        uncheckFoilCheckbox();
    }

    /**
     * @return The name of the currently loaded deck, may be the autosave name
     */
    public String getCurrentDeckName() {
        final StringBuilder deckName = new StringBuilder(mCurrentDeck);
        if (deckName.toString().equals("")) {
            deckName.append(DecklistFragment.AUTOSAVE_NAME);
        }
        deckName.append(DecklistFragment.DECK_EXTENSION);
        return deckName.toString();
    }

    /**
     * Removes all of the headers.
     */
    private void clearHeaders() {
        synchronized (mCompressedDecklist) {
            for (int i = 0; i < mCompressedDecklist.size(); i++) {
                if (mCompressedDecklist.get(i).header != null) { /* We found our header */
                    /* Now remove it, and then back up a step */
                    mCompressedDecklist.remove(i);
                    i--;
                }
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
        synchronized (mCompressedDecklist) {
            final CompressedDecklistInfo header = new CompressedDecklistInfo(new MtgCard(), false);
            header.header = headerText;
            if (!mCompressedDecklist.contains(header)) {
                mCompressedDecklist.add(position, header);
                return true;
            }
            return false;
        }
    }

    /**
     * Inserts the headers for each type.
     */
    private void setHeaderValues() {

        final String[] cardTypes = getResources().getStringArray(R.array.card_types_extra);
        final String[] cardHeaders = getResources().getStringArray(R.array.decklist_card_headers);
        synchronized (mCompressedDecklist) {
            for (int i = 0; i < mCompressedDecklist.size(); i++) {
                for (int j = 0; j < cardTypes.length; j++) {
                    final CompressedDecklistInfo cdi = mCompressedDecklist.get(i);
                    if (!cdi.getName().equals("") /* We only want entries that have a card attached */
                            && (i == 0 || mCompressedDecklist.get(i - 1).header == null)
                            && ((DecklistDataAdapter) getCardDataAdapter(0)).getTotalNumberOfType(j) > 0) {
                        if (cdi.mIsSideboard /* it is in the sideboard */
                                /* The sideboard header isn't already there */
                                && !insertHeaderAt(i, cardHeaders[0])) {
                            break;
                        } else if (j < cardHeaders.length - 1 /* if j is in range */
                                /* the current card has the selected card type */
                                && cdi.getType().contains(cardTypes[j])
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
    }

    /**
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.decklist_menu, menu);

    }

    @Override
    protected void onCardPriceLookupFailure(MtgCard data, Throwable exception) {
        /* Find the compressed wishlist info for this card */
        synchronized (mCompressedDecklist) {
            for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                if (cdi.header == null && cdi.getName().equals(data.getName())) {
                    /* Find all foil and non foil compressed items with the same set code */
                    for (CardHelpers.IndividualSetInfo isi : cdi.mInfo) {
                        if (isi.mSetCode.equals(data.getExpansion())) {
                            /* Set the price as null and the message as the exception */
                            isi.mMessage = exception.getLocalizedMessage();
                            isi.mPrice = null;
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onCardPriceLookupSuccess(MtgCard data, MarketPriceInfo result) {
        /* Find the compressed wishlist info for this card */
        synchronized (mCompressedDecklist) {
            for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                if (cdi.header == null && cdi.getName().equals(data.getName())) {
                    /* Find all foil and non foil compressed items with the same set code */
                    for (CardHelpers.IndividualSetInfo isi : cdi.mInfo) {
                        if (isi.mSetCode.equals(data.getExpansion())) {
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
            }
        }
    }

    @Override
    protected void onAllPriceLookupsFinished() {
        updateTotalPrices(0);
        getCardDataAdapter(0).notifyDataSetChanged();
    }

    /**
     * Add together the price of all the cards in the wishlist and display it.
     */
    public void updateTotalPrices(int side) {

        /* default */
        float totalPrice = 0;
        synchronized (mCompressedDecklist) {
            for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                if (cdi.header == null) {
                    for (CardHelpers.IndividualSetInfo isi : cdi.mInfo) {
                        if (null != isi.mPrice) {
                            totalPrice += isi.mPrice.getPrice(isi.mIsFoil, getPriceSetting()).price * isi.mNumberOf;
                        }
                    }
                }
            }
        }
        setTotalPrice(String.format(Locale.US, PRICE_FORMAT, totalPrice), null, 0);
    }

    @Override
    public boolean shouldShowPrice() {
        return PreferenceAdapter.getShowTotalDecklistPrice(getContext());
    }

    @Override
    public MarketPriceInfo.PriceType getPriceSetting() {
        return PreferenceAdapter.getDeckPrice(getContext());
    }

    @Override
    public void setPriceSetting(MarketPriceInfo.PriceType priceSetting) {
        PreferenceAdapter.setDeckPrice(getContext(), priceSetting);
    }

    class DecklistViewHolder extends CardDataViewHolder {

        private final TextView mCardNumberOf;
        private final TextView mCardCost;
        private final TextView mCardPrice;

        DecklistViewHolder(ViewGroup view) {
            super(view, R.layout.decklist_card_row, DecklistFragment.this.getCardDataAdapter(0), DecklistFragment.this);

            mCardNumberOf = itemView.findViewById(R.id.decklistRowNumber);
            mCardCost = itemView.findViewById(R.id.decklistRowCost);
            mCardPrice = itemView.findViewById(R.id.decklistRowPrice);

        }

        @Override
        public void onClickNotSelectMode(View view, int position) {
            /* if we aren't in select mode, open a dialog to edit this card */
            synchronized (mCompressedDecklist) {
                final CompressedDecklistInfo item = mCompressedDecklist.get(position);
                showDialog(DecklistDialogFragment.DIALOG_UPDATE_CARD,
                        item.getName(), item.mIsSideboard);
            }
        }
    }

    /**
     * The adapter that drives the deck list.
     */
    public class DecklistDataAdapter
            extends CardDataAdapter<CompressedDecklistInfo, DecklistViewHolder> {

        /**
         * Create the adapter.
         *
         * @param values the data set
         */
        DecklistDataAdapter(List<CompressedDecklistInfo> values) {
            super(values, DecklistFragment.this);
        }

        @NonNull
        @Override
        public DecklistViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {
            return new DecklistViewHolder(parent);
        }

        @Override
        protected void onItemReadded() {
            synchronized (this.items) {
                // Resort the decklist
                Collections.sort(this.items, mDecklistChain);

                // Reset the headers
                updateDeckCounts(false);
                clearHeaders();
                Collections.sort(this.items, mDecklistChain);
                setHeaderValues();

                // Call super to notify the adapter, etc
                super.onItemReadded();
            }
        }

        @Override
        protected void onItemRemoved() {
            synchronized (this.items) {
                // Reset the headers
                updateDeckCounts(false);
                clearHeaders();
                Collections.sort(this.items, mDecklistChain);
                setHeaderValues();

                // Update the number of cards listed
                updateDeckCounts(false);

                // Call super to notify the adapter, etc
                super.onItemRemoved();
            }
        }

        @Override
        protected void onItemRemovedFinal() {
            // After the snackbar times out, write the decklist to the disk
            synchronized (this.items) {
                DecklistHelpers.WriteCompressedDecklist(getActivity(), this.items, getCurrentDeckName());
            }
        }

        /**
         * On binding the view holder.
         *
         * @param holder   the holder being bound
         * @param position where the holder is
         */
        @Override
        public void onBindViewHolder(@NonNull DecklistViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            final CompressedDecklistInfo info = getItem(position);

            holder.itemView.findViewById(R.id.card_row_full).setVisibility(View.VISIBLE);
            if (Objects.requireNonNull(info).header == null) {

                /* Enable the on click listener */
                holder.itemView.setOnClickListener(holder);
                holder.itemView.setOnLongClickListener(holder);

                /* Do the selection stuff */
                if (info.isSelected()) {
                    holder.mCardNumberOf.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_menu_done, 0, 0, 0);
                    holder.mCardNumberOf.setText("");
                } else {
                    holder.mCardNumberOf
                            .setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    holder.mCardNumberOf.setText(String.valueOf(info.getTotalNumber()));
                }

                /* set up the card's views */
                holder.itemView.findViewById(R.id.card_row).setVisibility(View.VISIBLE);
                View separator = holder.itemView.findViewById(R.id.decklistSeparator);
                separator.setVisibility(View.GONE);
                Html.ImageGetter imageGetter = ImageGetterHelper.GlyphGetter(getActivity());
                holder.setCardName(info.getName());
                holder.mCardCost.setText(ImageGetterHelper
                        .formatStringWithGlyphs(info.getManaCost(), imageGetter));

                if (shouldShowPrice()) {
                    // Add up the prices for the card
                    double totalPrice = 0;
                    for (CardHelpers.IndividualSetInfo isi : info.mInfo) {
                        if (null != isi.mPrice) {
                            totalPrice += isi.mPrice.getPrice(isi.mIsFoil, getPriceSetting()).price * isi.mNumberOf;
                        }
                    }

                    holder.mCardPrice.setVisibility(View.VISIBLE);
                    holder.mCardPrice.setText(String.format(Locale.US, PRICE_FORMAT, totalPrice));
                } else {
                    holder.mCardPrice.setVisibility(View.GONE);
                }

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

            /* Make sure the index is in-bounds, or a request for the sideboard */
            String targetType;
            if (-1 == typeIndex) {
                /* This value isn't used */
                targetType = "sb";
            } else if (0 <= typeIndex && typeIndex < types.length) {
                targetType = types[typeIndex];
            } else {
                return 0;
            }

            /* Iterate over the deck */
            for (int i = 0; i < getItemCount(); i++) {
                CompressedDecklistInfo cdi = getItem(i);

                /* Make sure nothing's null. Don't count header items */
                if (null == cdi || cdi.header != null) {
                    continue;
                }

                /* If the card is not in the sideboard and we're looking for non-sideboard cards */
                if (!cdi.mIsSideboard && 0 <= typeIndex) {
                    /* If the card matches the type */
                    if (cdi.getType().contains(targetType)) {
                        /* Check if this card was counted as a previous type */
                        boolean alreadyCounted = false;
                        for (int acIdx = 0; acIdx < typeIndex; acIdx++) {
                            if (cdi.getType().contains(types[acIdx])) {
                                /* The card was already counted, so don't count it again */
                                alreadyCounted = true;
                                break;
                            }
                        }
                        /* The card was not already counted, so add it to this count */
                        if (!alreadyCounted) {
                            totalCards += cdi.getTotalNumber();
                        }
                    }
                    /* If the card is in the sideboard and we're looking for sideboard cards */
                } else if (cdi.mIsSideboard && -1 == typeIndex) {
                    totalCards += cdi.getTotalNumber();
                }
            }
            /* Return the total count */
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
                totalCards += Objects.requireNonNull(getItem(i)).getTotalNumber();
            }
            return totalCards;

        }

        /**
         * Return the number of cards in the deck and sideboard separately
         *
         * @param counts The counts, [0] is the dec and [1] is the sideboard. Must be at least length two
         */
        void getDeckCardCounts(int[] counts) {
            counts[0] = 0;
            counts[1] = 0;
            for (int i = 0; i < getItemCount(); i++) {
                if (Objects.requireNonNull(getItem(i)).mIsSideboard) {
                    counts[1] += Objects.requireNonNull(getItem(i)).getTotalNumber();
                } else {
                    counts[0] += Objects.requireNonNull(getItem(i)).getTotalNumber();
                }
            }
        }
    }
}
