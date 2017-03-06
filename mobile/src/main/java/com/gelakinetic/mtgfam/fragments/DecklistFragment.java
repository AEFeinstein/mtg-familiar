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
import android.widget.ListView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.DecklistDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;
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
    public ArrayList<CompressedDecklistInfo> mCompressedDecklist;
    public DecklistArrayAdapter mDecklistAdapter;
    public ComparatorChain mDecklistChain;

    public String mCurrentDeck = "autosave";
    public static final String DECK_EXTENSION = ".fDeck";

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
        View myFragmentView = inflater.inflate(R.layout.deck_view_frag, container, false);
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

        ListView listView = (ListView) myFragmentView.findViewById(decklist);

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
        listView.setAdapter(mDecklistAdapter);

        mDecklistChain = new ComparatorChain();
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSideboard());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSupertype(false));
        mDecklistChain.addComparator(new CardHelpers.CardComparatorCMC());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorColor());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorName());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CompressedDecklistInfo item = mCompressedDecklist.get(position);
                /* Show the dialog for this particular card */
                showDialog(DecklistDialogFragment.DIALOG_UPDATE_CARD, item.mCard.name, item.mIsSideboard);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
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
        readAndCompressDecklist(null, mCurrentDeck + DECK_EXTENSION);
        mDecklistAdapter.notifyDataSetChanged();
    }

    /**
     * Read in the decklist from the file, and pack it into an ArrayList of CompressedDecklistInfo
     * for display in a ListView. This data structure stores one copy of the card itself, and a list
     * of set-specific attributes like the set name and rarity.
     * @param changedCardName
     */
    public void readAndCompressDecklist(String changedCardName, String deckName) {
        if (deckName == null) {
            deckName = DecklistHelpers.DECKLIST_NAME;
        }
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
            // todo: is this needed HERE? Answer, probably not.
            CardDbAdapter.fillExtraWishlistData(mCompressedDecklist, database);
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
        readAndCompressDecklist(cardName, mCurrentDeck + DECK_EXTENSION);
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
            Html.ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getActivity());
            /* Get all the decklist information for this entry */
            CompressedDecklistInfo info = values.get(position);
            CompressedDecklistInfo previousInfo = null;
            /* Try to get the info above this one, if not we are good with it being null */
            try {
                previousInfo = values.get(position - 1);
            } catch (ArrayIndexOutOfBoundsException aie) {}
            /* Card type seperators. These check if the card is the first of that type in the view,
             * and if it is, show the seperator with the correct type text */
            TextView separator = (TextView) convertView.findViewById(R.id.decklistSeparator);
            separator.setVisibility(View.VISIBLE);
            if (info.mIsSideboard && !(previousInfo != null && previousInfo.mIsSideboard)) {
                separator.setText(R.string.decklist_sideboard);
            } else if (!info.mIsSideboard) {
                if (info.mCard.type.contains("Creature") && !(previousInfo != null && previousInfo.mCard.type.contains("Creature"))) {
                    separator.setText(R.string.decklist_creatures);
                } else if (info.mCard.type.contains("Planeswalker") && !(previousInfo != null && previousInfo.mCard.type.contains("Planeswalker"))) {
                    separator.setText(R.string.decklist_planeswalker);
                } else if ((info.mCard.type.contains("Instant") || info.mCard.type.contains("Sorcery")) && !(previousInfo != null && (previousInfo.mCard.type.contains("Instant") || previousInfo.mCard.type.contains("Sorcery")))) {
                    separator.setText(R.string.decklist_spells);
                } else if (info.mCard.type.contains("Artifact") && !(previousInfo != null && previousInfo.mCard.type.contains("Artifact"))) {
                    /* Exclude Artifact Creatures and Artifact Enchantments */
                    if (!info.mCard.type.contains("Creature") && !info.mCard.type.contains("Enchantment")) {
                        separator.setText(R.string.decklist_artifacts);
                    } else {
                        separator.setVisibility(View.GONE);
                    }
                } else if (info.mCard.type.contains("Enchantment") && !(previousInfo != null && previousInfo.mCard.type.contains("Enchantment"))) {
                    /* Exclude Enchantment Creatures and Artifact Enchantments */
                    if (!info.mCard.type.contains("Creature") && !info.mCard.type.contains("Artifact")) {
                        separator.setText(R.string.decklist_enchantments);
                    } else {
                        separator.setVisibility(View.GONE);
                    }
                } else if (info.mCard.type.contains("Land") && !(previousInfo != null && previousInfo.mCard.type.contains("Land"))) {
                    /* Exclude land creatures */
                    if (!info.mCard.type.contains("Creature")) {
                        separator.setText(R.string.decklist_lands);
                    } else {
                        separator.setVisibility(View.GONE);
                    }
                } else {
                    separator.setVisibility(View.GONE);
                }
            } else {
                separator.setVisibility(View.GONE);
            }
            /* Get the numberOf of the card, the name, and the mana cost to display */
            ((TextView) convertView.findViewById(R.id.decklistRowNumber)).setText(String.valueOf(info.getTotalNumber()));
            ((TextView) convertView.findViewById(R.id.decklistRowName)).setText(info.mCard.name);
            ((TextView) convertView.findViewById(R.id.decklistRowCost)).setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.manaCost, imgGetter));

            return convertView;
        }
    }
}
