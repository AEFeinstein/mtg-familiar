package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by brian on 3/1/2017.
 */
public class DecklistDialogFragment extends FamiliarDialogFragment {

    /* Dialog constants */
    public static final int DIALOG_UPDATE_CARD = 1;

    public static final String NAME_KEY = "name";
    public static final String SIDE_KEY = "side";

    /**
     * @return The currently viewed DecklistFragment
     */
    private DecklistFragment getParentDecklistFragment() {
        return (DecklistFragment) getFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setShowsDialog(true);
        mDialogId = getArguments().getInt(ID_KEY);
        final String cardName = getArguments().getString(NAME_KEY);
        final boolean isSideboard = getArguments().getBoolean(SIDE_KEY);
        switch (mDialogId) {
            case DIALOG_UPDATE_CARD: {
                /* Create the custom view */
                View customView = getParentDecklistFragment().getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog, null, false);
                assert customView != null;
                /* Grab the linear layout. Make it final to be accessible from the button layer */
                final LinearLayout linearLayout = (LinearLayout) customView.findViewById(R.id.linear_layout);
                customView.findViewById(R.id.show_card_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle();
                        SQLiteDatabase db = DatabaseManager.getInstance(getParentDecklistFragment().getActivity(), false).openDatabase(false);
                        try {
                            /* Get the card ID, and send it to a new CardViewFragment */
                            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, new long[]{CardDbAdapter.fetchIdByName(cardName, db)});
                            args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
                            CardViewPagerFragment cvpFragment = new CardViewPagerFragment();
                            getParentDecklistFragment().startNewFragment(cvpFragment, args);
                        } catch (FamiliarDbException fde) {
                            getParentDecklistFragment().handleFamiliarDbException(false);
                        }
                        DatabaseManager.getInstance(getParentDecklistFragment().getActivity(), false).closeDatabase(false);
                    }
                });

                /* Read the decklist */
                ArrayList<Pair<MtgCard, Boolean>> decklist = DecklistHelpers.ReadDecklist(getParentDecklistFragment().getActivity());

                /* Find any counts currently in the decklist */
                final Map<String, String> targetCardNumberOfs = new HashMap<>();
                final Map<String, String> targetFoilCardNumberOfs = new HashMap<>();
                for (Pair<MtgCard, Boolean> card : decklist) {
                    if (card.first.name.equals(cardName) && card.second.equals(isSideboard)) {
                        if (card.first.foil) {
                            targetFoilCardNumberOfs.put(card.first.setCode, card.first.numberOf + "");
                        } else {
                            targetCardNumberOfs.put(card.first.setCode, card.first.numberOf + "");
                        }
                    }
                }

                /* Get all potential sets and rarities for this card */
                final ArrayList<String> potentialSetCodes = new ArrayList<>();
                final ArrayList<Character> potentialRarities = new ArrayList<>();
                final ArrayList<String> potentialNumbers = new ArrayList<>();

                /* Open the database! */
                SQLiteDatabase db = DatabaseManager.getInstance(getParentDecklistFragment().getActivity(), false).openDatabase(false);

                /* Get all the cards with relevant info from the database */
                Cursor cards;
                try {
                    cards = CardDbAdapter.fetchCardByName(cardName, new String[]{
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
                            CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME}, true, db);
                } catch (FamiliarDbException e) {
                    DatabaseManager.getInstance(getParentDecklistFragment().getActivity(), false).closeDatabase(false);
                    return null;
                }

                Set<String> foilSets;
                try {
                    foilSets = CardDbAdapter.getFoilSets(db);
                } catch (FamiliarDbException fde) {
                    DatabaseManager.getInstance(getParentDecklistFragment().getActivity(), false).closeDatabase(false);
                    return null;
                }

                /* For each card, add it to the decklist view */
                while (!cards.isAfterLast()) {
                    String setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
                    String setName = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME));
                    char rarity = (char) cards.getInt(cards.getColumnIndex(CardDbAdapter.KEY_RARITY));
                    String number = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));

                    /* Inflate a row and fill it with stuff */
                    View wishlistRow = getParentDecklistFragment().getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row, null, false);
                    assert wishlistRow != null;
                    ((TextView) wishlistRow.findViewById(R.id.cardset)).setText(setName);
                    String numberOf = targetCardNumberOfs.get(setCode);
                    numberOf = numberOf == null ? "0" : numberOf;
                    ((EditText) wishlistRow.findViewById(R.id.numberInput)).setText(numberOf);
                    wishlistRow.findViewById(R.id.wishlistDialogFoil).setVisibility(View.GONE);
                    linearLayout.addView(wishlistRow);
                    potentialSetCodes.add(setCode);
                    potentialRarities.add(rarity);
                    potentialNumbers.add(number);

                    /* If this card has a foil version, add that too */
                    View wishlistRowFoil;
                    if (foilSets.contains(setCode)) {
                        wishlistRowFoil = getParentDecklistFragment().getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row,
                                null, false);
                        assert wishlistRowFoil != null;
                        ((TextView) wishlistRowFoil.findViewById(R.id.cardset)).setText(setName);
                        String foilNumberOf = targetFoilCardNumberOfs.get(setCode);
                        foilNumberOf = foilNumberOf == null ? "0" : foilNumberOf;
                        ((EditText) wishlistRowFoil.findViewById(R.id.numberInput)).setText(foilNumberOf);
                        wishlistRowFoil.findViewById(R.id.wishlistDialogFoil).setVisibility(View.VISIBLE);
                        linearLayout.addView(wishlistRowFoil);
                        potentialSetCodes.add(setCode);
                        potentialRarities.add(rarity);
                        potentialNumbers.add(number);
                    }

                    cards.moveToNext();
                }

                /* Clean up */
                cards.close();
                DatabaseManager.getInstance(getParentDecklistFragment().getActivity(), false).closeDatabase(false);

                /* make and return the actual dialog */
                return new MaterialDialog.Builder(getParentDecklistFragment().getActivity())
                        .title(cardName + " " + getParentDecklistFragment().getString(R.string.decklist_edit_dialog_title_end))
                        .customView(customView, false)
                        .positiveText(getParentDecklistFragment().getString(R.string.dialog_ok))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                /* read the decklist */
                                ArrayList<Pair<MtgCard, Boolean>> decklist = DecklistHelpers.ReadDecklist(getParentDecklistFragment().getActivity());

                                /* Add the cards listed in the dialog to the wishlist */
                                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                                    View view = linearLayout.getChildAt(i);
                                    assert view != null;

                                    /* build the card object */
                                    MtgCard card = new MtgCard();
                                    card.name = cardName;
                                    card.setCode = potentialSetCodes.get(i);
                                    try {
                                        EditText numberInput = ((EditText) view.findViewById(R.id.numberInput));
                                        assert numberInput.getText() != null;
                                        card.numberOf = Integer.valueOf(numberInput.getText().toString());
                                    } catch (NumberFormatException nfe) {
                                        card.numberOf = 0;
                                    }
                                    card.foil = (view.findViewById(R.id.wishlistDialogFoil).getVisibility() == View.VISIBLE);
                                    card.rarity = potentialRarities.get(i);
                                    card.number = potentialNumbers.get(i);

                                    /* Look through the decklist for each card, set the numberOf or
                                     * remove it if it exists, or add the card if it doesn't */
                                    boolean added = false;
                                    for (int j = 0; j < decklist.size(); j++) {
                                        if (card.name.equals(decklist.get(j).first.name)
                                                && isSideboard == decklist.get(j).second
                                                && card.setCode.equals(decklist.get(j).first.setCode)
                                                && card.foil == decklist.get(j).first.foil) {
                                            if (card.numberOf == 0) {
                                                decklist.remove(j);
                                                j--;
                                            } else {
                                                decklist.get(j).first.numberOf = card.numberOf;
                                            }
                                            added = true;
                                        }
                                    }
                                    if (!added && card.numberOf > 0) {
                                        decklist.add(new Pair<>(card, isSideboard));
                                    }
                                }
                                DecklistHelpers.WriteDecklist(getParentDecklistFragment().getActivity(), decklist);
                                getParentDecklistFragment().onWishlistChanged(cardName);
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }

}
