package com.gelakinetic.mtgfam.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.DecklistDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import org.apache.commons.collections4.comparators.ComparatorChain;

import java.util.ArrayList;
import java.util.Collections;

public class DecklistFragment extends FamiliarListFragment {

    /* UI Elements */
    public TextView mDeckName;
    public TextView mDeckCards;
    private TextView mDeckPrice;

    /* Decklist and adapters */
    public ArrayList<CompressedDecklistInfo> mCompressedDecklist;
    private ComparatorChain<CompressedDecklistInfo> mDecklistChain;

    public static final String AUTOSAVE_NAME = "autosave";
    public String mCurrentDeck = "";
    public static final String DECK_EXTENSION = ".deck";

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
        View myFragmentView = inflater.inflate(R.layout.decklist_frag, container, false);
        assert myFragmentView != null;

        final TextView.OnEditorActionListener addCardListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    addCardToDeck(false);
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
        mNumberOfField = (EditText) myFragmentView.findViewById(R.id.number_input);
        mNumberOfField.setText("1");
        mNumberOfField.setOnEditorActionListener(addCardListener);

        mCheckboxFoil = (CheckBox) myFragmentView.findViewById(R.id.list_foil);

        mListView = (RecyclerView) myFragmentView.findViewById(R.id.decklist);
        mListView.setLayoutManager(new LinearLayoutManager(getContext()));

        myFragmentView.findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCardToDeck(false);
            }
        });
        myFragmentView.findViewById(R.id.add_card).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                addCardToDeck(true);
                return true;
            }
        });

        /* Set up the decklist and adapter, it will be read in onResume() */
        mCompressedDecklist = new ArrayList<>();
        mListAdapter = new CardDataAdapter(mCompressedDecklist);
        mListView.setAdapter(mListAdapter);

        /* Decklist information */
        mDeckName = (TextView) myFragmentView.findViewById(R.id.decklistName);
        mDeckName.setText(R.string.decklist_unnamed_deck);
        mDeckCards = (TextView) myFragmentView.findViewById(R.id.decklistCards);
        mDeckCards.setText("0 ");
        mDeckPrice = (TextView) myFragmentView.findViewById(R.id.decklistPrice);
        mDeckPrice.setVisibility(View.GONE);

        mDecklistChain = new ComparatorChain<>();
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSideboard());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSupertype(getResources().getStringArray(R.array.card_types_extra)));
        mDecklistChain.addComparator(new CardHelpers.CardComparatorCMC());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorColor());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorName());

        getTouchHelper().attachToRecyclerView(mListView);

        /* Hopefully things look fancy */
        mListView.addItemDecoration(getItemDecorator());

        myFragmentView.findViewById(R.id.camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFamiliarActivity().startTutorCardsSearch();
            }
        });

        setUpCheckBoxClickListeners();

        /* on the click */
        mListAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* get the position so we can work with it */
                int position = mListView.getChildAdapterPosition(view);
                CompressedDecklistInfo item = mCompressedDecklist.get(position);
                /* Show the dialog for this particular card */
                showDialog(DecklistDialogFragment.DIALOG_UPDATE_CARD, item.mCard.mName, item.mIsSideboard);
            }
        });

        return myFragmentView;
    }

    /**
     * This function takes care of adding a card to the decklist from this fragment. It makes sure
     * that fields are not null or have bad information.
     * @param isSideboard if the card is in the sideboard
     */
    private void addCardToDeck(boolean isSideboard) {
        String name = String.valueOf(mNameField.getText());
        String numberOf = String.valueOf(mNumberOfField.getText());
        /* Don't allow the fields to be empty */
        if ((name == null || name.equals(""))
                && (numberOf == null || numberOf.equals(""))) {
            return;
        }

        MtgCard card = CardHelpers.makeMtgCard(getContext(), name, mCheckboxFoil.isChecked(), Integer.valueOf(numberOf));

        if (card == null) {
            return;
        }

        /* Add it to the decklist, either as a new CompressedDecklistInfo, or to an existing one */
        if (mCompressedDecklist.contains(card)) {
            boolean added = false;
            int firstIndex = mCompressedDecklist.indexOf(card);
            int lastIndex = mCompressedDecklist.lastIndexOf(card);
            CompressedDecklistInfo firstCard = mCompressedDecklist.get(firstIndex);
            CompressedDecklistInfo secondCard = mCompressedDecklist.get(lastIndex);
            int isiSize = Math.max(firstCard.mInfo.size(), secondCard.mInfo.size());
            for (int i = 0; i < isiSize; i++) {
                IndividualSetInfo firstIsi = firstCard.mInfo.get(i);
                IndividualSetInfo secondIsi = secondCard.mInfo.get(i);
                if (firstCard.mIsSideboard == isSideboard
                        && firstIsi.mSetCode.equals(card.setCode)
                        && firstIsi.mIsFoil.equals(card.foil)) {
                    firstIsi.mNumberOf++;
                    added = true;
                    break;
                } else if (secondCard.mIsSideboard == isSideboard
                        && secondIsi.mSetCode.equals(card.setCode)
                        && secondIsi.mIsFoil.equals(card.foil)) {
                    secondIsi.mNumberOf++;
                    added = true;
                    break;
                }
            }
            if (!added) { // todo: account for reprints since last added
                mCompressedDecklist.add(new CompressedDecklistInfo(card, isSideboard));
            }
        } else {
            mCompressedDecklist.add(new CompressedDecklistInfo(card, isSideboard));
        }

        /* The headers shouldn't (and can't) be sorted */
        clearHeaders();

        /* Sort the decklist */
        Collections.sort(mCompressedDecklist, mDecklistChain);

        /* Save the decklist */
        DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist);

        /* Clean up for the next add */
        mNumberOfField.setText("1");
        mNameField.setText("");

        /* Uncheck the foil box if it isn't locked */
        if (!mCheckboxFoilLocked) {
            mCheckboxFoil.setChecked(false);
        }

        /* Redraw the new decklist with the new card */
        ((CardDataAdapter) mListAdapter).notifyDataSetChanged2();

    }

    /**
     * read and compress the wishlist
     */
    @Override
    public void onResume() {
        super.onResume();
        mCompressedDecklist.clear();
        readAndCompressDecklist(null, mCurrentDeck);
        ((CardDataAdapter) mListAdapter).notifyDataSetChanged2();
    }

    /**
     * Read in the decklist from the file, and pack it into an ArrayList of CompressedDecklistInfo
     * for display in a ListView. This data structure stores one copy of the card itself, and a list
     * of set-specific attributes like the set name and rarity.
     * @param changedCardName
     */
    public void readAndCompressDecklist(String changedCardName, String deckName) {
        if (deckName == null || deckName.equals("") || deckName.equals(AUTOSAVE_NAME)) {
            deckName = AUTOSAVE_NAME;
            mDeckName.setText(R.string.decklist_unnamed_deck);
        } else {
            mDeckName.setText(deckName);
        }
        deckName += DECK_EXTENSION;
        /* Read the decklist */
        ArrayList<Pair<MtgCard, Boolean>> decklist = DecklistHelpers.ReadDecklist(getActivity(), deckName);
        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
        try {
            /* Translate the set code to TCG name of course it's not saved */
            for (Pair<MtgCard, Boolean> card : decklist) {
                card.first.setName = CardDbAdapter.getSetNameFromCode(card.first.setCode, database);
            }
            /* Clear the decklist, or just the card that changed */
            if (changedCardName == null) {
                mCompressedDecklist.clear();
            } else {
                for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                    if (cdi.mCard != null && cdi.mCard.mName.equals(changedCardName)) {
                        cdi.clearCompressedInfo();
                    }
                }
            }
            /* Compress the whole decklist, or just the card that changed */
            for (Pair<MtgCard, Boolean> card : decklist) {
                if (changedCardName == null || changedCardName.equals(card.first.mName)) {
                    if (!mCompressedDecklist.contains(card.first)) {
                        mCompressedDecklist.add(new CompressedDecklistInfo(card.first, card.second));
                    } else {
                        CompressedDecklistInfo existingCard = mCompressedDecklist.get(mCompressedDecklist.indexOf(card.first));
                        if (existingCard.mIsSideboard == card.second) {
                            mCompressedDecklist.get(mCompressedDecklist.indexOf(card.first)).add(card.first);
                        } else {
                            mCompressedDecklist.add(new CompressedDecklistInfo(card.first, card.second));
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
            setHeaderValues();
        } catch (FamiliarDbException fde) {
            handleFamiliarDbException(false);
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * This notifies the fragment when a change has been made from a card's dialog
     * @param cardName the card that was changed
     */
    @Override
    public void onWishlistChanged(String cardName) {
        readAndCompressDecklist(cardName, mCurrentDeck);
        clearHeaders();
        Collections.sort(mCompressedDecklist, mDecklistChain);
        ((CardDataAdapter) mListAdapter).notifyDataSetChanged2();
    }

    /**
     * Remove any showing dialogs, and show the requested one
     * @param id the ID of the dialog to show
     * @param cardName the name of the card to use if this is a dialog to change decklist counts
     * @param isSideboard if the card is in the sideboard
     * @throws IllegalStateException
     */
    private void showDialog(final int id, final String cardName, final boolean isSideboard) throws IllegalStateException {
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
     * Handle an ActionBar item click
     * @param item the item clicked
     * @return     true if the click was acted on
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* handle item selection */
        switch (item.getItemId()) {
            case R.id.deck_menu_save: {
                showDialog(DecklistDialogFragment.DIALOG_SAVE_DECK, null, false);
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
                sendIntent.putExtra(Intent.EXTRA_TEXT, DecklistHelpers.GetSharableDecklist(mCompressedDecklist, getActivity()));
                sendIntent.setType("text/plain");
                try {
                    startActivity(Intent.createChooser(sendIntent, getString(R.string.decklist_share)));
                } catch (ActivityNotFoundException anfe) {
                    ToastWrapper.makeText(getActivity(), R.string.error_no_email_client, ToastWrapper.LENGTH_LONG).show();
                }
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Removes all of the headers
     */
    private void clearHeaders() {
        for (int i = 0; i < mCompressedDecklist.size(); i++) {
            if (mCompressedDecklist.get(i).mCard == null) { /* We found our header */
                /* Now remove it, and then back up a step */
                mCompressedDecklist.remove(i);
                i--;
            }
        }
    }

    /**
     * Inserts the headers for each type
     */
    private void setHeaderValues() {
        final String[] cardTypes = getResources().getStringArray(R.array.card_types_extra);
        final String[] cardHeaders = getResources().getStringArray(R.array.decklist_card_headers);
        ArrayList<String> insertedHeaders = new ArrayList<>();
        for (int i = 0; i < mCompressedDecklist.size(); i++) {
            for (int j = 0; j < cardTypes.length; j++) {
                CompressedDecklistInfo cdi = mCompressedDecklist.get(i);
                if (cdi.mCard != null && /* We only want entries that have a card attached */
                    (i == 0 || mCompressedDecklist.get(i - 1).header == null)) {
                    if (!cdi.mIsSideboard) {
                        if (j < cardHeaders.length - 1 && /* if j is in range */
                                cdi.mCard.mType.contains(cardTypes[j])) { /* the current card has the selected card type */
                            if (!insertedHeaders.contains(cardHeaders[j + 1])) {
                                mCompressedDecklist.add(i, new CompressedDecklistInfo(null, false)); /* Add a new entry that will be our header */
                                mCompressedDecklist.get(i).header = cardHeaders[j + 1]; /* Use the header for the card type */
                                insertedHeaders.add(cardHeaders[j + 1]);
                            }
                            break;
                        } else if (j >= cardHeaders.length - 1) { /* j is out of bounds */
                            if (!insertedHeaders.contains(cardHeaders[cardHeaders.length - 1])) {
                                mCompressedDecklist.add(i, new CompressedDecklistInfo(null, false)); /* Add a new entry that will be our header */
                                mCompressedDecklist.get(i).header = cardHeaders[cardHeaders.length - 1]; /* Use the last card header, "Other" */
                                insertedHeaders.add(cardHeaders[cardHeaders.length - 1]);
                            }
                            break;
                        }
                    } else if (!insertedHeaders.contains(cardHeaders[0])) { /* it is sideboard, if sideboard header hasn't already been added */
                        mCompressedDecklist.add(i, new CompressedDecklistInfo(null, false));
                        mCompressedDecklist.get(i).header = cardHeaders[0];
                        insertedHeaders.add(cardHeaders[0]);
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
     * The adapter that drives the deck list
     */
    public class CardDataAdapter extends FamiliarListFragment.CardDataAdapter<CompressedDecklistInfo> {

        /**
         * Create the adapter
         * @param values the data set
         */
        CardDataAdapter(ArrayList<CompressedDecklistInfo> values) {
            super(values);
        }

        /**
         * Creating the view holder
         * @param parent parent of the to be created
         * @param viewType see super
         * @return the created view holder
         */
        @Override
        public CardDataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        /**
         * On binding the view holder
         * @param holder the holder being bound
         * @param position where the holder is
         */
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            onBindViewHolder((ViewHolder) holder, position);
        }

        /**
         * On binding the view holder
         * @param holder the holder being bound
         * @param position where the holder is
         */
        private void onBindViewHolder(ViewHolder holder, int position) {
            final CompressedDecklistInfo info = mItems.get(position);
            if (mItemsPendingRemoval.contains(info)) {
                holder.itemView.findViewById(R.id.card_row_full).setVisibility(View.GONE);
            } else { /* if the item IS NOT pending removal */
                holder.itemView.findViewById(R.id.card_row_full).setVisibility(View.VISIBLE);
                if (info.header != null) {
                    /* The header uses the same layout, just set it up */
                    holder.itemView.setOnClickListener(null);
                    holder.itemView.findViewById(R.id.decklistSeparator).setVisibility(View.VISIBLE);
                    holder.itemView.findViewById(R.id.card_row).setVisibility(View.GONE);
                    ((TextView) holder.itemView.findViewById(R.id.decklistHeaderType)).setText(info.header);
                    holder.swipeable = false;
                } else {
                    /* Enable the on click listener */
                    holder.enableClickListener();
                    /* set up the card's views */
                    holder.itemView.findViewById(R.id.card_row).setVisibility(View.VISIBLE);
                    holder.itemView.findViewById(R.id.decklistSeparator).setVisibility(View.GONE);
                    View separator = holder.itemView.findViewById(R.id.decklistSeparator);
                    separator.setVisibility(View.GONE);
                    Html.ImageGetter imageGetter = ImageGetterHelper.GlyphGetter(getActivity());
                    holder.mCardName.setText(info.mCard.mName);
                    holder.mCardNumberOf.setText(String.valueOf(info.getTotalNumber()));
                    holder.mCardCost.setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.mManaCost, imageGetter));
                }
            }
        }

        /**
         * Get the number of cards of the type to display in the header
         * @param headerValue the card type we are counting
         * @return the number of cards of the given type
         */
        int getTotalNumberOfType(final String headerValue) {
            int totalCards = 0;
            String currentHeader = "";
            for (CompressedDecklistInfo cdi : mItems) {
                /* check if one of two things is correct
                 * 1. the card is a sideboard card
                 * 2. the card's header is the headerValue, and isn't in the sideboard */
                if (cdi.header != null) {
                    currentHeader = cdi.header;
                    continue;
                }
                if ((headerValue.equals(getString(R.string.decklist_sideboard)) && cdi.mIsSideboard)
                        || (currentHeader.equals(headerValue) && !cdi.mIsSideboard)) {
                    totalCards += cdi.getTotalNumber();
                }
            }
            return totalCards;
        }

        /**
         * Get the total number of cards in this adapter
         * @return the total number of cards
         */
        int getTotalCards() {
            int totalCards = 0;
            for (CompressedDecklistInfo cdi : mItems) {
                totalCards += cdi.getTotalNumber();
            }
            return totalCards;
        }

        @Override
        public void remove(int position) {
            super.remove(position);
            mDeckCards.setText(String.valueOf(getTotalCards()) + " ");
            clearHeaders();
            notifyDataSetChanged();
            setHeaderValues();
            DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist);
        }

        /**
         * just a simple extension of notifyDataSetChanged() because it is final for this adapter
         */
        private void notifyDataSetChanged2() {
            String totalCards = String.valueOf(getTotalCards()) + " ";
            mDeckCards.setText(totalCards);
            clearHeaders();
            setHeaderValues();
            notifyDataSetChanged();
        }

        class ViewHolder extends FamiliarListFragment.CardDataAdapter.ViewHolder {

            private TextView mCardNumberOf;
            private TextView mCardCost;

            ViewHolder(ViewGroup view) {
                super(view, R.layout.decklist_card_row);
                mCardNumberOf = (TextView) itemView.findViewById(R.id.decklistRowNumber);
                mCardCost = (TextView) itemView.findViewById(R.id.decklistRowCost);
            }

        }

    }

}
