package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
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
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class that creates dialogs for DecklistFragment
 */
public class DecklistDialogFragment extends FamiliarDialogFragment {

    /* Dialog constants */
    public static final int DIALOG_UPDATE_CARD = 1;
    public static final int DIALOG_SAVE_DECK = 2;
    public static final int DIALOG_LOAD_DECK = 3;
    public static final int DIALOG_DELETE_DECK = 4;
    public static final int DIALOG_CONFIRMATION = 5;

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
                final FragmentActivity activity = getParentDecklistFragment().getActivity();
                /* Create the custom view */
                View customView = activity.getLayoutInflater().inflate(R.layout.wishlist_dialog, null, false);
                assert customView != null;
                /* Grab the linear layout. Make it final to be accessible from the button layer */
                final LinearLayout linearLayout = (LinearLayout) customView.findViewById(R.id.linear_layout);
                customView.findViewById(R.id.show_card_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle();
                        SQLiteDatabase db = DatabaseManager.getInstance(activity, false).openDatabase(false);
                        try {
                            /* Get the card ID, and send it to a new CardViewFragment */
                            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, new long[]{CardDbAdapter.fetchIdByName(cardName, db)});
                            args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
                            CardViewPagerFragment cvpFragment = new CardViewPagerFragment();
                            getParentDecklistFragment().startNewFragment(cvpFragment, args);
                        } catch (FamiliarDbException fde) {
                            getParentDecklistFragment().handleFamiliarDbException(false);
                        }
                        DatabaseManager.getInstance(activity, false).closeDatabase(false);
                    }
                });

                /* Read the decklist */
                String deckName = getParentDecklistFragment().mCurrentDeck;
                if (deckName.equals("")) {
                    deckName = DecklistFragment.AUTOSAVE_NAME;
                }
                ArrayList<Pair<MtgCard, Boolean>> decklist = DecklistHelpers.ReadDecklist(activity, deckName + DecklistFragment.DECK_EXTENSION);

                /* Find any counts currently in the decklist */
                final Map<String, String> targetCardNumberOfs = new HashMap<>();
                final Map<String, String> targetFoilCardNumberOfs = new HashMap<>();
                for (Pair<MtgCard, Boolean> card : decklist) {
                    if (card.first.name.equals(cardName)) {
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
                SQLiteDatabase db = DatabaseManager.getInstance(activity, false).openDatabase(false);

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
                    DatabaseManager.getInstance(activity, false).closeDatabase(false);
                    return null;
                }

                Set<String> foilSets;
                try {
                    foilSets = CardDbAdapter.getFoilSets(db);
                } catch (FamiliarDbException fde) {
                    DatabaseManager.getInstance(activity, false).closeDatabase(false);
                    return null;
                }

                /* For each card, add it to the decklist view */
                while (!cards.isAfterLast()) {
                    String setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
                    String setName = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME));
                    char rarity = (char) cards.getInt(cards.getColumnIndex(CardDbAdapter.KEY_RARITY));
                    String number = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));

                    /* Inflate a row and fill it with stuff */
                    View wishlistRow = activity.getLayoutInflater().inflate(R.layout.wishlist_dialog_row, null, false);
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
                        wishlistRowFoil = activity.getLayoutInflater().inflate(R.layout.wishlist_dialog_row,
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
                DatabaseManager.getInstance(activity, false).closeDatabase(false);

                /* make and return the actual dialog */
                return new MaterialDialog.Builder(activity)
                        .title(cardName + " " + getParentDecklistFragment().getString(R.string.decklist_edit_dialog_title_end))
                        .customView(customView, false)
                        .positiveText(getParentDecklistFragment().getString(R.string.dialog_ok))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                String deckName = getParentDecklistFragment().mCurrentDeck;
                                if (deckName.equals("")) {
                                    deckName = DecklistFragment.AUTOSAVE_NAME;
                                }
                                /* read the decklist */
                                ArrayList<Pair<MtgCard, Boolean>> decklist = DecklistHelpers.ReadDecklist(activity, deckName + DecklistFragment.DECK_EXTENSION);

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
                                DecklistHelpers.WriteDecklist(activity, decklist, deckName + DecklistFragment.DECK_EXTENSION);
                                getParentDecklistFragment().onWishlistChanged(cardName);
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
            }
            case DIALOG_SAVE_DECK: {
                /* Inflate a view to type in the deck's name and show it in an AlertDialog */
                View textEntryView = getActivity().getLayoutInflater().inflate(R.layout.alert_dialog_text_entry, null, false);
                assert textEntryView != null;
                final EditText nameInput = (EditText) textEntryView.findViewById(R.id.text_entry);
                nameInput.append(getParentDecklistFragment().mCurrentDeck);
                /* Set the button to clear the text field */
                textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nameInput.setText("");
                    }
                });
                Dialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.deck_save_dialog_title)
                        .customView(textEntryView, false)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                if (nameInput.getText() == null) {
                                    return;
                                }
                                String deckName = nameInput.getText().toString();
                                /* Don't save if there is not a name */
                                if (deckName.length() == 0 || deckName.equals("")) {
                                    return;
                                }
                                DecklistHelpers.WriteCompressedDecklist(getActivity(), getParentDecklistFragment().mCompressedDecklist, deckName + DecklistFragment.DECK_EXTENSION);
                                getParentDecklistFragment().mCurrentDeck = deckName;
                                getParentDecklistFragment().mDeckName.setText(deckName);
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            case DIALOG_LOAD_DECK: {
                /* Find all the decklist files */
                String[] files = this.getActivity().fileList();
                ArrayList<String> validFiles = new ArrayList<>();
                for (String fileName : files) {
                    if (fileName.endsWith(DecklistFragment.DECK_EXTENSION)) {
                        validFiles.add(fileName.substring(0, fileName.indexOf(DecklistFragment.DECK_EXTENSION)));
                    }
                }
                /* If there are no files, don't show the dialog */
                if (validFiles.size() == 0) {
                    ToastWrapper.makeText(this.getActivity(), R.string.decklist_toast_no_decks,
                            ToastWrapper.LENGTH_LONG).show();
                    return DontShowDialog();
                }
                /* Make an array of the trade file names */
                final String[] deckNames = new String[validFiles.size()];
                validFiles.toArray(deckNames);
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_select_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items(deckNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                getParentDecklistFragment().readAndCompressDecklist(null, deckNames[position]);
                                getParentDecklistFragment().mCurrentDeck = deckNames[position];
                                /* Alert things to update */
                                getParentDecklistFragment().mDecklistAdapter.notifyDataSetChanged();
                            }
                        })
                        .build();
            }
            case DIALOG_DELETE_DECK: {
                /* Find all the trade files */
                String[] files = this.getActivity().fileList();
                ArrayList<String> validFiles = new ArrayList<>();
                for (String fileName : files) {
                    if (fileName.endsWith(DecklistFragment.DECK_EXTENSION)) {
                        validFiles.add(fileName.substring(0, fileName.indexOf(DecklistFragment.DECK_EXTENSION)));
                    }
                }
                /* if there are no files, don't show the dialog */
                if (validFiles.size() == 0) {
                    ToastWrapper.makeText(this.getActivity(), R.string.decklist_toast_no_decks,
                            ToastWrapper.LENGTH_LONG).show();
                    return DontShowDialog();
                }
                /* make an array of the file names */
                final String[] deckNames = new String[validFiles.size()];
                validFiles.toArray(deckNames);
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_delete_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items(deckNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                File toDelete = new File(getActivity().getFilesDir(),
                                        deckNames[position] + DecklistFragment.DECK_EXTENSION);
                                if (!toDelete.delete()) {
                                    ToastWrapper.makeText(getActivity(), toDelete.getName() + " " +
                                            getString(R.string.not_deleted),
                                            ToastWrapper.LENGTH_LONG).show();
                                }
                            }
                        })
                        .build();
            }
            case DIALOG_CONFIRMATION: {
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_clear)
                        .content(R.string.decklist_clear_dialog_text)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                /* do some cleaning up */
                                getParentDecklistFragment().mCurrentDeck = "autosave";
                                getParentDecklistFragment().mCompressedDecklist.clear();
                                getParentDecklistFragment().mDecklistAdapter.notifyDataSetChanged();
                                dialog.dismiss();
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .cancelable(true)
                        .build();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }

}
