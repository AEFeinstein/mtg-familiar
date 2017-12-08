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
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.fragments.TradeFragment;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.util.FragmentHelpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

        final Context ctx = fragment.getActivity();

        /* Create the custom view */
        @SuppressLint("InflateParams") View customView = fragment.getActivity().getLayoutInflater()
                .inflate(R.layout.wishlist_dialog, null, false);
        assert customView != null;

        /* Grab the linear layout. Make it final to be accessible from the button later */
        final LinearLayout linearLayout =
                customView.findViewById(R.id.linear_layout);

        /* If the button should be shown, show it and attach a listener */
        if (showCardButton) {
            customView.findViewById(R.id.show_card_button).setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            Bundle args = new Bundle();
                            /* Open the database */
                            try {
                                SQLiteDatabase db = DatabaseManager.getInstance(fragment.getActivity(), false)
                                        .openDatabase(false);
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
                            } catch (FamiliarDbException e) {
                                fragment.handleFamiliarDbException(false);
                            }
                            DatabaseManager.getInstance(fragment.getActivity(), false).closeDatabase(false);

                        }

                    });
        } else {
            customView.findViewById(R.id.show_card_button).setVisibility(View.GONE);
            customView.findViewById(R.id.divider1).setVisibility(View.GONE);
            customView.findViewById(R.id.divider2).setVisibility(View.GONE);
        }

        final Pair<Map<String, String>, Map<String, String>> targetNumberOfs;
        final Map<String, String> targetCardNumberOfs;
        final Map<String, String> targetFoilCardNumberOfs;

        // The wishlist dialog is shown both in the card view and the wishlist fragments
        final boolean isWishlistDialog = FragmentHelpers.isInstanceOf(ctx, WishlistFragment.class);
        final boolean isCardViewDialog = FragmentHelpers.isInstanceOf(ctx, CardViewPagerFragment.class);
        final boolean isResultListDialog = FragmentHelpers.isInstanceOf(ctx, ResultListFragment.class);

        final String deckName;
        final String dialogText;

        if (isWishlistDialog || isCardViewDialog || isResultListDialog) {
            /* Read the wishlist */
            ArrayList<MtgCard> wishlist = WishlistHelpers.ReadWishlist(ctx);
            targetNumberOfs = WishlistHelpers.getTargetNumberOfs(mCardName, wishlist);
            deckName = "";
            dialogText = ctx.getString(R.string.wishlist_edit_dialog_title_end);
        } else {
            /* Right now only WishlistDialogFragment and DecklistDialogFragment call this, so
             * obviously now it is the decklist */
            String tempDeckName = ((DecklistFragment) fragment).mCurrentDeck;
            if (tempDeckName.equals("")) {
                deckName = DecklistFragment.AUTOSAVE_NAME;
            } else {
                deckName = ((DecklistFragment) fragment).mCurrentDeck;
            }
            ArrayList<Pair<MtgCard, Boolean>> decklist =
                    DecklistHelpers.ReadDecklist(ctx, deckName + DecklistFragment.DECK_EXTENSION);
            targetNumberOfs = DecklistHelpers.getTargetNumberOfs(mCardName, decklist, isSideboard);
            dialogText = ctx.getString(R.string.decklist_edit_dialog_title_end);
        }
        targetCardNumberOfs = targetNumberOfs.first;
        targetFoilCardNumberOfs = targetNumberOfs.second;


        /* Get all potential sets and rarities for this card */
        final ArrayList<String> potentialSetCodes = new ArrayList<>();
        final ArrayList<Character> potentialRarities = new ArrayList<>();
        final ArrayList<String> potentialNumbers = new ArrayList<>();

        try {
            /* Open the database */
            SQLiteDatabase db =
                    DatabaseManager.getInstance(fragment.getActivity(), false).openDatabase(false);

            /* Get all the cards with relevant info from the database */
            Cursor cards;
            cards = CardDbAdapter.fetchCardByName(mCardName, Arrays.asList(
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
                    CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME), true, db);

            Set<String> foilSets;
            foilSets = CardDbAdapter.getFoilSets(db);

            /* For each card, add it to the wishlist view */
            while (!cards.isAfterLast()) {
                String setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
                String setName = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME));
                char rarity = (char) cards.getInt(cards.getColumnIndex(CardDbAdapter.KEY_RARITY));
                String number = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));

                /* Inflate a row and fill it with stuff */
                View listDialogRow = createDialogRow(
                        fragment,
                        setName,
                        targetCardNumberOfs.get(setCode),
                        false,
                        linearLayout);
                linearLayout.addView(listDialogRow);
                potentialSetCodes.add(setCode);
                potentialRarities.add(rarity);
                potentialNumbers.add(number);

                /* If this card has a foil version, add that too */
                if (foilSets.contains(setCode)) {
                    View wishlistRowFoil = createDialogRow(
                            fragment,
                            setName,
                            targetFoilCardNumberOfs.get(setCode),
                            true,
                            linearLayout);
                    linearLayout.addView(wishlistRowFoil);
                    potentialSetCodes.add(setCode);
                    potentialRarities.add(rarity);
                    potentialNumbers.add(number);
                }

                cards.moveToNext();
            }

            /* Clean up */
            cards.close();
        } catch (FamiliarDbException e) {
            DatabaseManager.getInstance(fragment.getActivity(), false).closeDatabase(false);
            return null;
        }

        DatabaseManager.getInstance(fragment.getActivity(), false).closeDatabase(false);

        /* make and return the actual dialog */
        return new MaterialDialog.Builder(ctx)
                .title(mCardName + " " + dialogText)
                .customView(customView, false)
                .positiveText(fragment.getString(R.string.dialog_ok))
                .onPositive(new MaterialDialog.SingleButtonCallback() {

                    @Override
                    public void onClick(
                            @NonNull MaterialDialog dialog,
                            @NonNull DialogAction which) {

                        ArrayList<Pair<MtgCard, Boolean>> list;

                        if (isWishlistDialog || isCardViewDialog || isResultListDialog) {
                            /* Read the wishlist */
                            list = new ArrayList<>();
                            ArrayList<MtgCard> wishlist = WishlistHelpers.ReadWishlist(ctx);
                            for (MtgCard card : wishlist) {
                                list.add(new Pair<>(card, false));
                            }
                        } else {
                            list = DecklistHelpers.ReadDecklist(
                                    ctx,
                                    deckName + DecklistFragment.DECK_EXTENSION
                            );
                        }

                        /* Add the cards listed in the dialog to the wishlist */
                        for (int i = 0; i < linearLayout.getChildCount(); i++) {
                            View view = linearLayout.getChildAt(i);
                            assert view != null;

                            /* build the card object */
                            MtgCard card = new MtgCard();
                            card.mName = mCardName;
                            card.setCode = potentialSetCodes.get(i);
                            try {
                                Button numberInput = view.findViewById(R.id.number_button);
                                assert numberInput.getText() != null;
                                card.numberOf = Integer.parseInt(numberInput.getText().toString());
                            } catch (NumberFormatException e) {
                                card.numberOf = 0;
                            }
                            card.foil = view.findViewById(R.id.wishlistDialogFoil)
                                    .getVisibility() == View.VISIBLE;
                            card.mRarity = potentialRarities.get(i);
                            card.mNumber = potentialNumbers.get(i);

                            /* Look through the wishlist for each card, set the numberOf or remove
                             * it if it exists, or add the card if it doesn't */
                            boolean added = false;
                            for (int j = 0; j < list.size(); j++) {
                                if (card.mName.equals(list.get(j).first.mName)
                                        && isSideboard == list.get(j).second
                                        && card.setCode.equals(list.get(j).first.setCode)
                                        && card.foil == list.get(j).first.foil) {
                                    if (card.numberOf == 0) {
                                        list.remove(j);
                                        j--;
                                    } else {
                                        list.get(j).first.numberOf = card.numberOf;
                                    }
                                    added = true;
                                }
                            }
                            if (!added && card.numberOf > 0) {
                                list.add(new Pair<>(card, false));
                            }

                        }

                        if (isWishlistDialog || isCardViewDialog || isResultListDialog) {
                            ArrayList<MtgCard> wishlist = new ArrayList<>();
                            /* Turn it back in to a plain ArrayList */
                            for (Pair<MtgCard, Boolean> card : list) {
                                wishlist.add(card.first);
                            }
                            /* Write the wishlist */
                            WishlistHelpers.WriteWishlist(fragment.getActivity(), wishlist);
                            /* notify the fragment of a change in the wishlist */
                            fragment.onWishlistChanged(mCardName); //
                        } else {
                            DecklistHelpers.WriteDecklist(
                                    ctx,
                                    list,
                                    deckName + DecklistFragment.DECK_EXTENSION
                            );
                            fragment.onWishlistChanged(mCardName);

                        }
                    }

                })
                .negativeText(fragment.getString(R.string.dialog_cancel))
                .build();
    }

    /**
     * Creates the row of each card in the edit dialog.
     *
     * @param fragment           the fragment we are from
     * @param setName            the set of the card
     * @param targetCardNumberOf the number of the card
     * @param isFoil             if the card is foil or not
     * @param viewGroup          the viewGroup to inflate the row into
     * @return a View that displays an idividual printing of a card
     */
    private static View createDialogRow(
            FamiliarFragment fragment,
            String setName,
            String targetCardNumberOf,
            boolean isFoil,
            ViewGroup viewGroup) {

        View dialogRow = fragment.getActivity().getLayoutInflater()
                .inflate(R.layout.wishlist_dialog_row, viewGroup, false);
        assert dialogRow != null;
        ((TextView) dialogRow.findViewById(R.id.cardset)).setText(setName);
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
        public PriceInfo mPrice;
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
        public CompressedCardInfo(MtgCard card) {

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
                isi.mSet = card.setName;
                isi.mSetCode = card.setCode;
                isi.mNumber = card.mNumber;
                isi.mIsFoil = card.foil;
                isi.mPrice = null;
                isi.mMessage = card.message;
                isi.mNumberOf = card.numberOf;
                isi.mRarity = card.mRarity;
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
            this.mExpansion = isi.mSet;
            this.setCode = isi.mSetCode;
            this.mNumber = isi.mNumber;
            this.foil = isi.mIsFoil;
            this.numberOf = isi.mNumberOf;
            this.mRarity = isi.mRarity;
        }

    }

    /**
     * Comparator based on name.
     */
    public static class CardComparatorName
            implements Comparator<CompressedDecklistInfo>, Serializable {

        @Override
        public int compare(CompressedDecklistInfo card1, CompressedDecklistInfo card2) {
            return card1.mName.compareTo(card2.mName);
        }

    }

    /**
     * Comparator based on CMC.
     */
    public static class CardComparatorCMC
            implements Comparator<CompressedDecklistInfo>, Serializable {

        @Override
        public int compare(CompressedDecklistInfo card1, CompressedDecklistInfo card2) {

            if (card1.mCmc == card2.mCmc) {
                return 0;
            } else if (card1.mCmc > card2.mCmc) {
                return 1;
            }
            return -1;

        }

    }

    /**
     * Comparator based on color.
     */
    public static class CardComparatorColor
            implements Comparator<CompressedDecklistInfo>, Serializable {

        private static final String COLORS = "WUBRG";
        private static final String NON_COLORS = "LAC";

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
        public int compare(CompressedDecklistInfo card1, CompressedDecklistInfo card2) {

            String cardColors1 = getColors(card1.mColor);
            String cardColors2 = getColors(card2.mColor);
            int priority1;
            int priority2;
            //1. If colorless, perform colorless comparison
            if (cardColors1.length() + cardColors2.length() == 0) {
                cardColors1 = card1.mColor;
                cardColors2 = card2.mColor;
                for (int i = 0; i < Math.min(cardColors1.length(), cardColors2.length()); i++) {
                    priority1 = NON_COLORS.indexOf(cardColors1.charAt(i));
                    priority2 = NON_COLORS.indexOf(cardColors2.charAt(i));
                    if (priority1 != priority2) {
                        return priority1 < priority2 ? -1 : 1;
                    }
                }
                return 0;
            }
            if (cardColors1.length() < cardColors2.length()) {
                return -1;
            } else if (cardColors1.length() > cardColors2.length()) {
                return 1;
            }
            // Else if the same number of COLORS exist, compare based on WUBRG-ness
            for (int i = 0; i < Math.min(cardColors1.length(), cardColors2.length()); i++) {
                priority1 = COLORS.indexOf(cardColors1.charAt(i));
                priority2 = COLORS.indexOf(cardColors2.charAt(i));
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
            implements Comparator<CompressedDecklistInfo>, Serializable {

        @Override
        public int compare(CompressedDecklistInfo card1, CompressedDecklistInfo card2) {

            if (card1.mIsSideboard == card2.mIsSideboard) {
                return 0;
            } else if (card1.mIsSideboard) {
                return 1;
            }
            return -1;

        }

    }

    /**
     * Comparator based on card supertype, an array of types must be passed in the order to sort.
     */
    public static class CardComparatorSupertype
            implements Comparator<CompressedDecklistInfo>, Serializable {

        final String[] mTypes;

        public CardComparatorSupertype(String[] superTypes) {
            mTypes = superTypes.clone();
        }

        @Override
        public int compare(CompressedDecklistInfo card1, CompressedDecklistInfo card2) {

            String card1Type = card1.mType;
            String card2Type = card2.mType;
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

    /**
     * Construct a MtgCard based on the given parameters.
     *
     * @param context  context the method is being called from
     * @param cardName name of the card to make
     * @param cardSet  set code of the card to make
     * @param isFoil   if the card is foil or not
     * @param numberOf how many copies of the card are needed
     * @return an MtgCard made based on the given parameters
     */
    public static MtgCard makeMtgCard(
            Context context,
            String cardName,
            String cardSet,
            boolean isFoil,
            int numberOf) {

        FamiliarActivity activity = (FamiliarActivity) context;
        try {
            SQLiteDatabase database = DatabaseManager.getInstance(activity, false).openDatabase(false);
            /* Make the new MTGCard */
            MtgCard card = new MtgCard();
            card.foil = isFoil;
            card.numberOf = numberOf;
            /* Find out what kind of fragment we are in, so we can pull less stuff if we can */
            /* trade, wishlist, and decklist all use "name_search" */
            List<String> fields;
            boolean isTradeFragment = FragmentHelpers.isInstanceOf(context, TradeFragment.class);
            if (isTradeFragment) {
                fields = Arrays.asList(
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
                        /* CMC and Color For sorting */
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_CMC,
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_COLOR,
                        /* Don't trust the user */
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NAME);
            } else {
                fields = CardDbAdapter.ALL_CARD_DATA_KEYS;
                card.message = activity.getString(R.string.wishlist_loading);
            }
            /* Get extra information from the database */
            Cursor cardCursor;
            if (cardSet == null) {
                cardCursor = CardDbAdapter.fetchCardByName(cardName, fields, true, database);

                /* Make sure at least one card was found */
                if (cardCursor.getCount() == 0) {
                    ToastWrapper.makeAndShowText(activity, R.string.toast_no_card,
                            ToastWrapper.LENGTH_LONG);
                    DatabaseManager.getInstance(activity, false).closeDatabase(false);
                    return null;
                }
                /* If we don't specify the set, and we are trying to find a foil card, choose the
                 * latest foil printing. If there are no eligible printings, select the latest */
                if (isFoil) {
                    while (!CardDbAdapter.canBeFoil(
                            cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET))
                            , database)) {
                        if (cardCursor.isLast()) {
                            cardCursor.moveToFirst();
                            break;
                        }
                        cardCursor.moveToNext();
                    }
                }
            } else {
                cardCursor = CardDbAdapter.fetchCardByNameAndSet(cardName, cardSet, fields, database);
            }

            /* Make sure at least one card was found */
            if (cardCursor.getCount() == 0) {
                ToastWrapper.makeAndShowText(activity, R.string.toast_no_card,
                        ToastWrapper.LENGTH_LONG);
                DatabaseManager.getInstance(activity, false).closeDatabase(false);
                return null;
            }

            /* Don't rely on the user's given name, get it from the DB just to be sure */
            card.mName = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
            card.setCode = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
            card.setName = CardDbAdapter.getSetNameFromCode(card.setCode, database);
            card.mNumber = cardCursor.getString(cardCursor
                    .getColumnIndex(CardDbAdapter.KEY_NUMBER));
            card.mCmc = cardCursor.getInt((cardCursor
                    .getColumnIndex(CardDbAdapter.KEY_CMC)));
            card.mColor = cardCursor.getString(cardCursor
                    .getColumnIndex(CardDbAdapter.KEY_COLOR));
            if (!isTradeFragment) {
                card.mType = CardDbAdapter.getTypeLine(cardCursor);
                card.mRarity = (char) cardCursor.getInt(cardCursor
                        .getColumnIndex(CardDbAdapter.KEY_RARITY));
                card.mManaCost = cardCursor.getString(cardCursor
                        .getColumnIndex(CardDbAdapter.KEY_MANACOST));
                card.mPower = cardCursor.getInt(cardCursor
                        .getColumnIndex(CardDbAdapter.KEY_POWER));
                card.mToughness = cardCursor.getInt(cardCursor
                        .getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
                card.mLoyalty = cardCursor.getInt(cardCursor
                        .getColumnIndex(CardDbAdapter.KEY_LOYALTY));
                card.mText = cardCursor.getString(cardCursor
                        .getColumnIndex(CardDbAdapter.KEY_ABILITY));
                card.mFlavor = cardCursor.getString(cardCursor
                        .getColumnIndex(CardDbAdapter.KEY_FLAVOR));
            }

            /* Override choice is the card can't be foil */
            if (!CardDbAdapter.canBeFoil(card.setCode, database)) {
                card.foil = false;
            }
            /* clean up */
            cardCursor.close();
            /* return our made card! */
            return card;
        } catch (FamiliarDbException | NumberFormatException fde) {
            /* todo: handle this */
        }
        DatabaseManager.getInstance(activity, false).closeDatabase(false);
        return null;

    }
}
