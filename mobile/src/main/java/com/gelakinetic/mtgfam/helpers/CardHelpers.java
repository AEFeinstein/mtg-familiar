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

package com.gelakinetic.mtgfam.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;
import com.gelakinetic.mtgfam.helpers.util.FragmentHelpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

/**
 * This class has helpers for anything that utilizes card things, and has classes that
 * DecklistHelpers and WishlistHelpers override.
 */
public class CardHelpers {

    /**
     * Return a dialog in which a user can specify how many of what set of a card are in the
     * wishlist.
     *
     * @param mCardName      The name of the card
     * @param fragment       The fragment which hosts the dialog and receives onWishlistChanged()
     * @param showCardButton Whether the button to launch the CardViewFragment should be shown
     * @param isSideboard    true if this card goes in the sidebard, false for the maindeck
     * @return A dialog which edits the wishlist
     */
    public static Dialog getDialog(
            final String mCardName,
            final FamiliarFragment fragment,
            boolean showCardButton,
            final boolean isSideboard) {

        final Activity activity = fragment.getActivity();

        /* Create the custom view */
        @SuppressLint("InflateParams") View customView = fragment.requireActivity().getLayoutInflater()
                .inflate(R.layout.wishlist_dialog, null, false);
        assert customView != null;

        /* Grab the linear layout. Make it final to be accessible from the button later */
        final LinearLayout linearLayout =
                customView.findViewById(R.id.linear_layout);

        // The wishlist dialog is shown both in the card view and the wishlist fragments
        final boolean isWishlistDialog = FragmentHelpers.isInstanceOf(activity, WishlistFragment.class);
        final boolean isCardViewDialog = FragmentHelpers.isInstanceOf(activity, CardViewPagerFragment.class);
        final boolean isResultListDialog = FragmentHelpers.isInstanceOf(activity, ResultListFragment.class);

        final String deckName;
        final String dialogText;

        ArrayList<MtgCard> cardList;
        try {
            if (isWishlistDialog || isCardViewDialog || isResultListDialog) {
                /* Read the wishlist */
                cardList = WishlistHelpers.ReadWishlist(activity, false);
                deckName = "";
                dialogText = Objects.requireNonNull(activity).getString(R.string.wishlist_edit_dialog_title_end);
            } else {
                /* Right now only WishlistDialogFragment and DecklistDialogFragment call this, so
                 * obviously now it is the decklist */
                String tempDeckName = ((DecklistFragment) fragment).mCurrentDeck;
                if (tempDeckName.equals("")) {
                    deckName = DecklistFragment.AUTOSAVE_NAME;
                } else {
                    deckName = ((DecklistFragment) fragment).mCurrentDeck;
                }
                cardList =
                        DecklistHelpers.ReadDecklist(activity, deckName + DecklistFragment.DECK_EXTENSION, false);
                dialogText = Objects.requireNonNull(activity).getString(R.string.decklist_edit_dialog_title_end);
            }
        } catch (FamiliarDbException e) {
            return null;
        }

        /* Get all potential sets and rarities for this card */
        final ArrayList<String> potentialSetCodes = new ArrayList<>();

        Cursor cards = null;
        FamiliarDbHandle fetchCardHandle = new FamiliarDbHandle();
        try {
            /* Open the database */
            SQLiteDatabase db = DatabaseManager.openDatabase(fragment.getActivity(), false, fetchCardHandle);

            /* Get all the cards with relevant info from the database */
            cards = CardDbAdapter.fetchCardByName(mCardName, Arrays.asList(
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
                    CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME), false, false, false, db);

            Set<String> foilSets = CardDbAdapter.getFoilSets(db);

            /* For each card, add it to the wishlist view */
            HashMap<Boolean, ArrayList<String>> emptyNumberSetCodes = new HashMap<>();
            emptyNumberSetCodes.put(true, new ArrayList<>());
            emptyNumberSetCodes.put(false, new ArrayList<>());

            while (!cards.isAfterLast()) {
                String setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
                String setName = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME));
                char rarity = (char) cards.getInt(cards.getColumnIndex(CardDbAdapter.KEY_RARITY));
                String number = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));

                int[] numberOfCards = getNumberOfCardsInList(cardList, mCardName, setCode, number, isSideboard, emptyNumberSetCodes);

                if (numberOfCards[1] > 0 && !foilSets.contains(setCode)) {
                    // The card is foil, but the set isn't. This happens for foil-only sets like Masterpieces
                    // Add a non-foil row
                    View wishlistRow = createDialogRow(
                            fragment,
                            setName,
                            setCode,
                            rarity,
                            number,
                            Integer.toString(numberOfCards[1]),
                            false,
                            linearLayout);
                    linearLayout.addView(wishlistRow);
                    potentialSetCodes.add(setCode);
                } else {
                    /* Inflate a row and fill it with stuff */
                    View listDialogRow = createDialogRow(
                            fragment,
                            setName,
                            setCode,
                            rarity,
                            number,
                            Integer.toString(numberOfCards[0]),
                            false,
                            linearLayout);
                    linearLayout.addView(listDialogRow);
                    potentialSetCodes.add(setCode);

                    /* If this card has a foil version, add that too */
                    if (foilSets.contains(setCode)) {
                        View wishlistRowFoil = createDialogRow(
                                fragment,
                                setName,
                                setCode,
                                rarity,
                                number,
                                Integer.toString(numberOfCards[1]),
                                true,
                                linearLayout);
                        linearLayout.addView(wishlistRowFoil);
                        potentialSetCodes.add(setCode);
                    }
                }

                cards.moveToNext();
            }
        } catch (SQLiteException | FamiliarDbException | CursorIndexOutOfBoundsException e) {
            return null;
        } finally {
            if (null != cards) {
                cards.close();
            }
            DatabaseManager.closeDatabase(fragment.getActivity(), fetchCardHandle);
        }

        MaterialDialog.SingleButtonCallback onPositiveCallback = (dialog, which) -> {

            ArrayList<MtgCard> list;
            ArrayList<String> nonFoilSets;

            try {
                if (isWishlistDialog || isCardViewDialog || isResultListDialog) {
                    /* Read the wishlist */
                    list = WishlistHelpers.ReadWishlist(activity, false);
                } else {
                    list = DecklistHelpers.ReadDecklist(
                            activity,
                            deckName + DecklistFragment.DECK_EXTENSION,
                            false
                    );
                }
            } catch (FamiliarDbException e) {
                return;
            }

            FamiliarDbHandle handle = new FamiliarDbHandle();
            try {
                SQLiteDatabase database = DatabaseManager.openDatabase(activity, false, handle);
                nonFoilSets = CardDbAdapter.getNonFoilSets(database);
            } catch (SQLiteException | FamiliarDbException | IllegalStateException ignored) {
                nonFoilSets = new ArrayList<>();
            } finally {
                DatabaseManager.closeDatabase(activity, handle);
            }

            /* Remove all number-less cards with this card name from the list */
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j).getName().equals(mCardName) && list.get(j).getNumber().isEmpty()) {
                    list.remove(j);
                    j--;
                }
            }

            /* Add the cards listed in the dialog to the wishlist */
            for (int i = 0; i < linearLayout.getChildCount(); i++) {
                View view = linearLayout.getChildAt(i);
                assert view != null;

                /* build the card object */
                boolean isFoil = view.findViewById(R.id.wishlistDialogFoil).getVisibility() == View.VISIBLE;
                int numberOf;
                try {
                    Button numberInput = view.findViewById(R.id.number_button);
                    assert numberInput.getText() != null;
                    numberOf = Integer.parseInt(numberInput.getText().toString());
                } catch (NumberFormatException e) {
                    numberOf = 0;
                }
                String cardNumber = ((TextView) view.findViewById(R.id.card_number)).getText().toString();
                MtgCard card = new MtgCard(mCardName, potentialSetCodes.get(i), cardNumber, isFoil, numberOf, isSideboard);

                /* Look through the wishlist for each card, set the numberOf or remove
                 * it if it exists, or add the card if it doesn't */
                boolean added = false;
                for (int j = 0; j < list.size(); j++) {
                    if (card.getName().equals(list.get(j).getName())
                            && card.isSideboard() == list.get(j).isSideboard()
                            && card.getExpansion().equals(list.get(j).getExpansion())
                            && card.getNumber().equals(list.get(j).getNumber())) {
                        if (card.mIsFoil == list.get(j).mIsFoil ||
                                nonFoilSets.contains(card.getExpansion())) {
                            if (card.mNumberOf == 0) {
                                list.remove(j);
                                j--;
                            } else {
                                list.get(j).mNumberOf = card.mNumberOf;
                            }
                            added = true;
                        }
                    }
                }
                if (!added && card.mNumberOf > 0) {
                    list.add(card);
                }
            }

            if (isWishlistDialog || isCardViewDialog || isResultListDialog) {
                /* Write the wishlist */
                WishlistHelpers.WriteWishlist(fragment.getActivity(), list);
                /* notify the fragment of a change in the wishlist */
            } else {
                DecklistHelpers.WriteDecklist(
                        activity,
                        list,
                        deckName + DecklistFragment.DECK_EXTENSION);
            }
            fragment.onWishlistChanged(mCardName);
        };

        /* If the button should be shown, show it and attach a listener */
        if (showCardButton) {
            customView.findViewById(R.id.show_card_button).setOnClickListener(
                    view -> {

                        onPositiveCallback.onClick(null, null);

                        Bundle args = new Bundle();
                        /* Open the database */
                        FamiliarDbHandle showCardHandle = new FamiliarDbHandle();
                        try {
                            SQLiteDatabase db = DatabaseManager.openDatabase(fragment.getActivity(), false, showCardHandle);
                            /* Get the card ID, and send it to a new CardViewFragment */
                            long cardId = CardDbAdapter.fetchIdByName(mCardName, db);
                            if (cardId > 0) {
                                args.putLongArray(
                                        CardViewPagerFragment.CARD_ID_ARRAY,
                                        new long[]{cardId}
                                );
                                args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
                                CardViewPagerFragment cvpFrag = new CardViewPagerFragment();
                                fragment.startNewFragment(cvpFrag, args);
                            }
                        } catch (SQLiteException | FamiliarDbException e) {
                            fragment.handleFamiliarDbException(false);
                        } finally {
                            DatabaseManager.closeDatabase(fragment.getActivity(), showCardHandle);
                        }
                        fragment.removeDialog(fragment.getParentFragmentManager());
                    });
        } else {
            customView.findViewById(R.id.show_card_button).setVisibility(View.GONE);
            customView.findViewById(R.id.divider1).setVisibility(View.GONE);
            customView.findViewById(R.id.divider2).setVisibility(View.GONE);
        }

        /* make and return the actual dialog */
        return new MaterialDialog.Builder(activity)
                .title(mCardName + " " + dialogText)
                .customView(customView, false)
                .positiveText(fragment.getString(R.string.dialog_ok))
                .onPositive(onPositiveCallback)
                .negativeText(fragment.getString(R.string.dialog_cancel))
                .build();
    }

    /**
     * Get the number of a specific card in a list of cards
     *
     * @param cards               The list of cards to check
     * @param cardName            The name of the card
     * @param setCode             The set code for the card
     * @param cardNumber          The card's number (may be empty)
     * @param isSideboard         Whether or not this card is a sideboard or not
     * @param emptyNumberSetCodes A list of set codes where a card with an empty number was counted
     * @return The number of specific cards in the list, non-foil then foil
     */
    static int[] getNumberOfCardsInList(ArrayList<MtgCard> cards, String cardName, String setCode,
                                        String cardNumber, boolean isSideboard,
                                        HashMap<Boolean, ArrayList<String>> emptyNumberSetCodes) {
        int[] numberOf = new int[]{0, 0};
        for (MtgCard card : cards) {
            if (card.getName().equals(cardName) &&
                    card.getExpansion().equals(setCode) &&
                    card.isSideboard() == isSideboard) {

                // If the number matches, or the number is empty and this set's empty number entry hasn't been counted yet
                if (card.getNumber().equals(cardNumber) ||
                        (!emptyNumberSetCodes.get(card.mIsFoil).contains(setCode) && card.getNumber().isEmpty())) {
                    if (card.getNumber().isEmpty()) {
                        emptyNumberSetCodes.get(card.mIsFoil).add(setCode);
                    }

                    if (!card.mIsFoil) {
                        numberOf[0] += card.mNumberOf;
                    } else {
                        numberOf[1] += card.mNumberOf;
                    }
                }
            }
        }
        return numberOf;
    }

    /**
     * Creates the row of each card in the edit dialog.
     *
     * @param fragment           the fragment we are from
     * @param setName            the set of the card
     * @param setCode            The card's set code
     * @param rarity             The card's rarity
     * @param number             The card's number
     * @param targetCardNumberOf the number of the card
     * @param isFoil             if the card is foil or not
     * @param viewGroup          the viewGroup to inflate the row into
     * @return a View that displays an idividual printing of a card
     */
    private static View createDialogRow(
            FamiliarFragment fragment,
            String setName,
            String setCode,
            char rarity,
            String number,
            String targetCardNumberOf,
            boolean isFoil,
            ViewGroup viewGroup) {

        View dialogRow = fragment.requireActivity().getLayoutInflater()
                .inflate(R.layout.wishlist_dialog_row, viewGroup, false);
        assert dialogRow != null;
        ((TextView) dialogRow.findViewById(R.id.cardset)).setText(setName);
        ((TextView) dialogRow.findViewById(R.id.card_number)).setText(number);
        ExpansionImageHelper.loadExpansionImage(fragment.getContext(), setCode, rarity, dialogRow.findViewById(R.id.cardsetimage), null, ExpansionImageHelper.ExpansionImageSize.LARGE);
        String numberOf = targetCardNumberOf;
        numberOf = numberOf == null ? "0" : numberOf;
        final Button numberButton = dialogRow.findViewById(R.id.number_button);
        numberButton.setText(numberOf);
        numberButton.setOnClickListener(new NumberButtonOnClickListener(fragment) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDialogNumberSet(Integer number) {
                numberButton.setText(number.toString());
            }
        });
        if (isFoil) {
            dialogRow.findViewById(R.id.wishlistDialogFoil).setVisibility(View.VISIBLE);
        } else {
            dialogRow.findViewById(R.id.wishlistDialogFoil).setVisibility(View.GONE);
        }
        return dialogRow;

    }

    /**
     * This class encapsulates all non-duplicated information for two cards in different sets.
     */
    public static class IndividualSetInfo {

        public String mSet;
        public String mSetCode;
        public String mNumber;

        public Boolean mIsFoil;
        public MarketPriceInfo mPrice;
        public String mMessage;
        public Integer mNumberOf;
        public Character mRarity;

    }

    /**
     * Parent class of CompressedDecklistInfo and CompressedWishlistInfo.
     * It contains the common code shared between the two.
     */
    public static class CompressedCardInfo extends MtgCard {

        public final ArrayList<IndividualSetInfo> mInfo;

        /**
         * Constructor.
         *
         * @param card The MtgCard which will be the base for this object
         */
        CompressedCardInfo(MtgCard card) {

            super(card);
            mInfo = new ArrayList<>();
            if (mName != null) {
                add(card);
            }

        }

        /**
         * Add a new printing of a MtgCard to this object.
         *
         * @param card The new printing to add to this object
         */
        public final void add(MtgCard card) {

            IndividualSetInfo isi = new IndividualSetInfo();

            if (card != null) {
                isi.mSet = card.getSetName();
                isi.mSetCode = card.getExpansion();
                isi.mNumber = card.getNumber();
                isi.mIsFoil = card.mIsFoil;
                isi.mPrice = null;
                isi.mMessage = card.mMessage;
                isi.mNumberOf = card.mNumberOf;
                isi.mRarity = card.getRarity();
            }

            mInfo.add(isi);

        }

        /**
         * Clear all the different printings for this object.
         */
        public void clearCompressedInfo() {
            mInfo.clear();
        }

        /**
         * @return The total number cards this object contains
         */
        public int getTotalNumber() {

            int totalCopies = 0;
            for (IndividualSetInfo isi : mInfo) {
                totalCopies += isi.mNumberOf;
            }
            return totalCopies;

        }

        /**
         * Apply IndividualSetInfo to this CompressedCardInfo. This is useful for modifying card
         * data before writing it to a file
         *
         * @param isi The IndividualSetInfo to apply
         */
        void applyIndividualInfo(IndividualSetInfo isi) {
            this.mSetName = isi.mSet;
            this.mExpansion = isi.mSetCode;
            this.mNumber = isi.mNumber;
            this.mIsFoil = isi.mIsFoil;
            this.mNumberOf = isi.mNumberOf;
            this.mRarity = isi.mRarity;
        }

        /**
         * @return The alphabetically first expansion for this CompressedCardInfo
         */
        String getFirstExpansion() {
            String firstExpansion = this.mInfo.get(0).mSet;
            for (IndividualSetInfo info : this.mInfo) {
                if (firstExpansion.compareTo(info.mSet) < 1) {
                    firstExpansion = info.mSet;
                }
            }
            return firstExpansion;
        }

        /**
         * @return The rarity of the rarest card in this CompressedCardInfo
         */
        char getHighestRarity() {
            char rarity = '\0';
            for (IndividualSetInfo info : this.mInfo) {
                switch (info.mRarity) {
                    case 'c':
                    case 'C':
                        if ('\0' == rarity) {
                            rarity = 'C';
                        }
                        break;
                    case 'u':
                    case 'U':
                        if ('\0' == rarity ||
                                'C' == rarity) {
                            rarity = 'U';
                        }
                        break;
                    case 'r':
                    case 'R':
                        if ('\0' == rarity ||
                                'C' == rarity ||
                                'U' == rarity) {
                            rarity = 'R';
                        }
                        break;
                    case 't':
                    case 'T':
                        if ('\0' == rarity ||
                                'C' == rarity ||
                                'U' == rarity ||
                                'R' == rarity) {
                            rarity = 'T';
                        }
                        break;
                    case 'm':
                    case 'M':
                        if ('\0' == rarity ||
                                'C' == rarity ||
                                'U' == rarity ||
                                'R' == rarity ||
                                'T' == rarity) {
                            rarity = 'M';
                        }
                        break;
                }
            }
            return rarity;
        }
    }

    /**
     * Comparator based on name.
     */
    public static class CardComparatorName
            implements Comparator<MtgCard>, Serializable {

        @Override
        public int compare(MtgCard card1, MtgCard card2) {
            return card1.getName().compareTo(card2.getName());
        }

    }

    /**
     * Comparator based on CMC.
     */
    public static class CardComparatorCMC
            implements Comparator<MtgCard>, Serializable {

        @Override
        public int compare(MtgCard card1, MtgCard card2) {

            return Integer.compare(card1.getCmc(), card2.getCmc());

        }

    }

    /**
     * Comparator based on color.
     */
    public static class CardComparatorColor
            implements Comparator<MtgCard>, Serializable {

        private static final String COLORS = "WUBRG";

        /**
         * Gets what COLORS are in the given string.
         *
         * @param c the string of COLORS
         * @return valid COLORS from the string
         */
        private String getColors(String c) {

            StringBuilder validColors = new StringBuilder();
            //1. catch null/empty string
            if (c == null || c.isEmpty()) {
                return "";
            }
            //2. For each char, if a valid color, add to return String
            for (int i = 0; i < c.length(); i++) {
                if (COLORS.indexOf(c.charAt(i)) > -1) {
                    validColors.append(c.charAt(i));
                }
            }
            return validColors.toString();

        }

        @Override
        public int compare(MtgCard card1, MtgCard card2) {

            String cardColors1 = getColors(card1.getColor());
            String cardColors2 = getColors(card2.getColor());
            if (cardColors1.length() < cardColors2.length()) {
                return -1;
            } else if (cardColors1.length() > cardColors2.length()) {
                return 1;
            }
            // Else if the same number of COLORS exist, compare based on WUBRG-ness
            for (int i = 0; i < Math.min(cardColors1.length(), cardColors2.length()); i++) {
                int priority1 = COLORS.indexOf(cardColors1.charAt(i));
                int priority2 = COLORS.indexOf(cardColors2.charAt(i));
                if (priority1 != priority2) {
                    return priority1 < priority2 ? -1 : 1;
                }
            }
            return 0;

        }

    }

    /**
     * Comparator based on sideboard.
     */
    public static class CardComparatorSideboard
            implements Comparator<MtgCard>, Serializable {

        @Override
        public int compare(MtgCard card1, MtgCard card2) {

            return Boolean.compare(card1.isSideboard(), card2.isSideboard());

        }

    }

    /**
     * Comparator based on card supertype, an array of types must be passed in the order to sort.
     */
    public static class CardComparatorSupertype
            implements Comparator<MtgCard>, Serializable {

        final String[] mTypes;

        public CardComparatorSupertype(String[] superTypes) {
            mTypes = superTypes.clone();
        }

        @Override
        public int compare(MtgCard card1, MtgCard card2) {

            String card1Type = card1.getType();
            String card2Type = card2.getType();
            for (String type : mTypes) {
                if (card1Type.contains(type) && card2Type.contains(type)) {
                    return 0;
                } else if (card1Type.contains(type) && !card2Type.contains(type)) {
                    return -1;
                } else if (!card1Type.contains(type) && card2Type.contains(type)) {
                    return 1;
                }
            }
            return 0;

        }

    }
}
