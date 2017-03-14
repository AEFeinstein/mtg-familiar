package com.gelakinetic.mtgfam.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

import static com.gelakinetic.mtgfam.R.id.decklist;

public class DecklistFragment extends FamiliarFragment {

    /* UI Elements */
    public AutoCompleteTextView mNameField;
    public EditText mNumberField;

    /* Decklist and adapters */
    public ListView decklistView;
    public ArrayList<CompressedDecklistInfo> mCompressedDecklist;
    public DecklistArrayAdapter mDecklistAdapter;
    public ComparatorChain mDecklistChain;

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
        mNumberField = (EditText) myFragmentView.findViewById(R.id.number_input);
        mNumberField.setText("1");
        mNumberField.setOnEditorActionListener(addCardListener);

        decklistView = (ListView) myFragmentView.findViewById(decklist);

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
        mDecklistAdapter = new DecklistArrayAdapter(mCompressedDecklist);
        decklistView.setAdapter(mDecklistAdapter);

        mDecklistChain = new ComparatorChain<>();
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSideboard());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSupertype(getResources().getStringArray(R.array.card_types_extra)));
        mDecklistChain.addComparator(new CardHelpers.CardComparatorCMC());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorColor());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorName());

        decklistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CompressedDecklistInfo item = mCompressedDecklist.get(position);
                /* Show the dialog for this particular card */
                showDialog(DecklistDialogFragment.DIALOG_UPDATE_CARD, item.mCard.name, item.mIsSideboard);
            }
        });
        decklistView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                /* Remove the card */
                mCompressedDecklist.remove(position);
                /* Save the decklist */
                DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist);
                /* Redraw the new decklist */
                mDecklistAdapter.notifyDataSetChanged();
                return true;
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
        String numberOf = String.valueOf(mNumberField.getText());
        /* Don't allow the fields to be empty */
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
            card.foil = false;
            card.numberOf = Integer.parseInt(numberOf);
            card.message = getString(R.string.wishlist_loading);

            /* Get some extra information from the database */
            Cursor cardCursor = CardDbAdapter.fetchCardByName(card.name, CardDbAdapter.allCardDataKeys, true, database);
            if (cardCursor.getCount() == 0) {
                ToastWrapper.makeText(DecklistFragment.this.getActivity(), getString(R.string.toast_no_card),
                        ToastWrapper.LENGTH_LONG).show();
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }
            card.name = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
            card.type = CardDbAdapter.getTypeLine(cardCursor);
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
            if (mCompressedDecklist.contains(card)) {
                boolean added = false;
                int firstIndex = mCompressedDecklist.indexOf(card);
                int lastIndex = mCompressedDecklist.lastIndexOf(card);
                if (firstIndex == lastIndex) {
                    CompressedDecklistInfo existingCard = mCompressedDecklist.get(firstIndex);
                    if (existingCard.mIsSideboard == isSideboard) {
                        for (IndividualSetInfo isi : existingCard.mInfo) {
                            if (isi.mSetCode.equals(card.setCode) && isi.mIsFoil.equals(card.foil)) {
                                added = true;
                                isi.mNumberOf++;
                            }
                        }
                        if (!added) {
                            existingCard.add(card);
                        }
                    } else {
                        mCompressedDecklist.add(new CompressedDecklistInfo(card, isSideboard));
                    }
                } else {
                    CompressedDecklistInfo firstCard = mCompressedDecklist.get(firstIndex);
                    CompressedDecklistInfo secondCard = mCompressedDecklist.get(lastIndex);
                    if (firstCard.mIsSideboard == isSideboard) {
                        for (IndividualSetInfo isi : firstCard.mInfo) {
                            if (isi.mSetCode.equals(card.setCode) && isi.mIsFoil.equals(card.foil)) {
                                added = true;
                                isi.mNumberOf++;
                            }
                        }
                        if (!added) {
                            firstCard.add(card);
                        }
                    } else {
                        for (IndividualSetInfo isi : firstCard.mInfo) {
                            if (isi.mSetCode.equals(card.setCode) && isi.mIsFoil.equals(card.foil)) {
                                added = true;
                                isi.mNumberOf++;
                            }
                        }
                        if (!added) {
                            secondCard.add(card);
                        }
                    }
                }
            } else {
                mCompressedDecklist.add(new CompressedDecklistInfo(card, isSideboard));
            }

            /* Sort the decklist */;
            Collections.sort(mCompressedDecklist, mDecklistChain);


            /* Save the decklist */
            DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist);

            /* Clean up for the next add */
            mNumberField.setText("1");
            mNameField.setText("");

            /* Update the headers */
            setHeaderValues();

            /* Redraw the new decklist with the new card */
            mDecklistAdapter.notifyDataSetChanged();

        } catch (FamiliarDbException e) {
            handleFamiliarDbException(false);
        } catch (NumberFormatException e) {
            /* eat it */
        }
    }

    /**
     * read and compress the wishlist
     */
    @Override
    public void onResume() {
        super.onResume();
        mCompressedDecklist.clear();
        readAndCompressDecklist(null, mCurrentDeck);
        mDecklistAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist);
    }

    /**
     * Read in the decklist from the file, and pack it into an ArrayList of CompressedDecklistInfo
     * for display in a ListView. This data structure stores one copy of the card itself, and a list
     * of set-specific attributes like the set name and rarity.
     * @param changedCardName
     */
    public void readAndCompressDecklist(String changedCardName, String deckName) {
        if (deckName == null || deckName.equals("")) {
            deckName = AUTOSAVE_NAME;
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
                    if (cdi.mCard.name.equals(changedCardName)) {
                        cdi.clearCompressedInfo();
                    }
                }
            }
            /* Compress the whole decklist, or just the card that changed */
            for (Pair<MtgCard, Boolean> card : decklist) {
                if (changedCardName == null || changedCardName.equals(card.first.name)) {
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
        Collections.sort(mCompressedDecklist, mDecklistChain);
        mDecklistAdapter.notifyDataSetChanged();
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
     * Sets the header values for all the cards in the list
     */
    private void setHeaderValues() {
        final String[] cardTypes = getResources().getStringArray(R.array.card_types_extra);
        final String[] cardHeaders = getResources().getStringArray(R.array.decklist_card_headers);
            /* We need to tell each card what it should be classified as, that is the order as
             * defined by R.array.card_types_extra */
        for (CompressedDecklistInfo cdi : mCompressedDecklist) {
            for (int i = 0; i < cardTypes.length; i++) {
                if (cdi.mCard.type.contains(cardTypes[i])) {
                    cdi.header = cardHeaders[i + 1];
                    break;
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
     * This nested class is the adapter which populates the ListView in the drawer menu. It handles
     * both entries and headers.
     */
    public class DecklistArrayAdapter extends ArrayAdapter<CompressedDecklistInfo> {

        private final ArrayList<CompressedDecklistInfo> values;

        /**
         * Constructor. The context will be used to inflate views later. The array of values will be
         * used to populate the views
         * @param values an array of DrawerEntries which will populate the list.
         */
        public DecklistArrayAdapter(ArrayList<CompressedDecklistInfo> values) {
            super(getActivity(), R.layout.drawer_list_item, values);
            this.values = values;
        }

        /**
         * Called to get a view for an entry in the ListView
         * @param position The position of the ListView to populate
         * @param convertView The old view to reuse if possible. Since the layouts for enteries and
         *                    headers are different, this will be ignored
         * @param parent The parent this view will eventually be attached to
         * @return The view for the data at this position
         */
        @NotNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.decklist_card_row, parent, false);
                assert convertView != null;
            }
            /* Get the decklist information for this entry */
            CompressedDecklistInfo info = values.get(position);
            /* Get the information for the previous, if it exists */
            CompressedDecklistInfo previousInfo = null;
            try {
                previousInfo = values.get(position - 1);
            } catch (ArrayIndexOutOfBoundsException aie) { /* this is fine, we check for a null value later */ }
            String[] cardTypes = getResources().getStringArray(R.array.card_types);
            /* Note "Creature" is index 1 */
            String[] cardHeaders = getResources().getStringArray(R.array.decklist_card_headers);
            View separator = convertView.findViewById(R.id.decklistSeparator);
            TextView separatorText = (TextView) separator.findViewById(R.id.decklistHeaderType);
            TextView separatorNumber = (TextView) separator.findViewById(R.id.decklistHeaderNumber);
            separator.setVisibility(View.GONE);
            if (info.mIsSideboard) { // Card is in the sideboard
                if (previousInfo == null || !previousInfo.mIsSideboard) { // The card before either does not exist, or is not in the sideboard
                    separator.setVisibility(View.VISIBLE);
                    separatorText.setText(cardHeaders[0]);
                    String number = "(" + String.valueOf(getTotalNumberOfType(getString(R.string.decklist_sideboard))) + ")";
                    separatorNumber.setText(number);
                }
            } else { // Card is in the mainboard
                for (int i = 0; i < cardTypes.length + 1; i++) {
                    if (i == cardTypes.length || info.mCard.type.contains(cardTypes[i])) { // if the card is of type cardTypes[i]
                        if (previousInfo == null || !previousInfo.header.equals(info.header)) { // if the previous card is null, or the previous header is not equal to the current header
                            separator.setVisibility(View.VISIBLE);
                            separatorText.setText(cardHeaders[i + 1]);
                            String number = "(" + String.valueOf(getTotalNumberOfType(info.header)) + ")";
                            separatorNumber.setText(number);
                        }
                        break; // We don't need to continue checking, since we found the header
                    }
                }
            }
            Html.ImageGetter imageGetter = ImageGetterHelper.GlyphGetter(getActivity());
            /* Get the numberOf of the card, the name, and the mana cost to display */
            ((TextView) convertView.findViewById(R.id.decklistRowNumber)).setText(String.valueOf(info.getTotalNumber()));
            ((TextView) convertView.findViewById(R.id.decklistRowName)).setText(info.mCard.name);
            ((TextView) convertView.findViewById(R.id.decklistRowCost)).setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.manaCost, imageGetter));
            return convertView;
        }

        /**
         * Get the number of cards of the type to display in the header
         * @param headerValue the card type we are counting
         * @return the number of cards of the given type
         */
        private int getTotalNumberOfType(final String headerValue) {
            int totalCards = 0;
            for (CompressedDecklistInfo cdi : values) {
                /* check if one of two things is correct
                 * 1. the card is a sideboard card
                 * 2. the card's header is the headerValue, and isn't in the sideboard */
                if ((headerValue.equals(getString(R.string.decklist_sideboard)) && cdi.mIsSideboard)
                        || (cdi.header.equals(headerValue) && !cdi.mIsSideboard)) {
                    totalCards += cdi.getTotalNumber();
                }
            }
            return totalCards;
        }

    }
}
