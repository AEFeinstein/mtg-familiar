package com.gelakinetic.mtgfam.fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import java.util.ArrayList;

public class DeckBuilderFragment extends FamiliarFragment {

    /* UI Elements */
    public AutoCompleteTextView mNameField;
    public EditText mNumberField;

    /* Decklist and adapters */
    public ArrayList<CompressedDecklistInfo> mCompressedDecklist;
    public DecklistArrayAdapter mDecklistAdapter;

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

        ListView listView = (ListView) myFragmentView.findViewById(R.id.decklist);

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

        mCompressedDecklist = new ArrayList<>();
        mDecklistAdapter = new DecklistArrayAdapter(mCompressedDecklist);
        listView.setAdapter(mDecklistAdapter);

        return myFragmentView;
    }

    private void addCardToDeck(boolean isSideboard) {
        String name = String.valueOf(mNameField.getText());
        String numberOf = String.valueOf(mNumberField.getText());
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
                ToastWrapper.makeText(DeckBuilderFragment.this.getActivity(), getString(R.string.toast_no_card),
                        ToastWrapper.LENGTH_LONG).show();
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }
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
                CompressedDecklistInfo cwi = mCompressedDecklist.get(mCompressedDecklist.indexOf(card));
                boolean added = false;
                for (WishlistHelpers.IndividualSetInfo isi : cwi.mInfo) {
                    if (isi.mSetCode.equals(card.setCode) && isi.mIsFoil.equals(card.foil)) {
                        added = true;
                        isi.mNumberOf++;
                    }
                }
                if (!added) {
                    cwi.add(card);
                }
            } else {
                mCompressedDecklist.add(new CompressedDecklistInfo(card, isSideboard));
            }

            /* Sort the wishlist */
            //sortWishlist();

            /* Save the wishlist */
            //WishlistHelpers.WriteCompressedWishlist(getActivity(), mCompressedWishlist);

            /* Clean up for the next add */
            mNumberField.setText("1");
            mNameField.setText("");

            /* Redraw the new wishlist with the new card */
            mDecklistAdapter.notifyDataSetChanged();

        } catch (FamiliarDbException e) {
            handleFamiliarDbException(false);
        } catch (NumberFormatException e) {
            /* eat it */
        }
    }

    public class DecklistArrayAdapter extends ArrayAdapter<CompressedDecklistInfo> {

        private final ArrayList<CompressedDecklistInfo> values;

        public DecklistArrayAdapter(ArrayList<CompressedDecklistInfo> values) {
            super(getActivity(), R.layout.drawer_list_item, values);
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            convertView = getActivity().getLayoutInflater().inflate(R.layout.decklist_card_row, parent, false);
            assert convertView != null;
            Html.ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getActivity());
            CompressedDecklistInfo info = values.get(position);
            ((TextView) convertView.findViewById(R.id.decklistRowNumber)).setText(String.valueOf(info.mCard.numberOf));
            ((TextView) convertView.findViewById(R.id.decklistRowName)).setText(info.mCard.name);
            ((TextView) convertView.findViewById(R.id.decklistRowCost)).setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.manaCost, imgGetter));
            return convertView;
        }

    }
}
