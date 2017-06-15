package com.gelakinetic.mtgfam.helpers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment.SortOption;
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.gelakinetic.mtgfam.fragments.WishlistFragment.AVG_PRICE;
import static com.gelakinetic.mtgfam.fragments.WishlistFragment.HIGH_PRICE;
import static com.gelakinetic.mtgfam.fragments.WishlistFragment.LOW_PRICE;

/**
 * This class has helpers used for reading, writing, and modifying the wishlist from different fragments
 */
public class WishlistHelpers {

    /* The name of the wishlist file */
    private static final String WISHLIST_NAME = "card.wishlist";

    /**
     * Write the wishlist passed as a parameter to the wishlist file
     *
     * @param mCtx      A context to open the file and pop toasts with
     * @param lWishlist The wishlist to write to the file
     */
    private static void WriteWishlist(Context mCtx, ArrayList<MtgCard> lWishlist) {
        try {
            FileOutputStream fos = mCtx.openFileOutput(WISHLIST_NAME, Context.MODE_PRIVATE);

            for (MtgCard m : lWishlist) {
                fos.write(m.toWishlistString().getBytes());
            }

            fos.close();
        } catch (IOException e) {
            ToastWrapper.makeText(mCtx, e.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        }
    }

    /**
     * Write the wishlist passed as a parameter to the wishlist file
     *
     * @param mCtx                A context to open the file and pop toasts with
     * @param mCompressedWishlist The wishlist to write to the file
     */
    public static void WriteCompressedWishlist(Context mCtx, ArrayList<CompressedWishlistInfo> mCompressedWishlist) {
        try {
            FileOutputStream fos = mCtx.openFileOutput(WISHLIST_NAME, Context.MODE_PRIVATE);

            /* For each compressed card, make an MtgCard and write it to the wishlist */
            for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                MtgCard card = cwi.mCard;
                for (IndividualSetInfo isi : cwi.mInfo) {
                    card.mExpansion = isi.mSet;
                    card.setCode = isi.mSetCode;
                    card.mNumber = isi.mNumber;
                    card.foil = isi.mIsFoil;
                    card.numberOf = isi.mNumberOf;
                    fos.write(card.toWishlistString().getBytes());
                }
            }

            fos.close();
        } catch (IOException e) {
            ToastWrapper.makeText(mCtx, e.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        }
    }

    /**
     * Delete the wishlist
     *
     * @param mCtx A context to open the file wish
     */
    public static void ResetCards(Context mCtx) {

        String[] files = mCtx.fileList();
        for (String fileName : files) {
            if (fileName.equals(WISHLIST_NAME)) {
                mCtx.deleteFile(fileName);
            }
        }
    }

    /**
     * Read the wishlist from a file and return it as an ArrayList<MtgCard>
     *
     * @param mCtx A context to open the file and pop toasts with
     * @return The wishlist in ArrayList form
     */
    public static ArrayList<MtgCard> ReadWishlist(Context mCtx) {

        ArrayList<MtgCard> lWishlist = new ArrayList<>();

        try {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(mCtx.openFileInput(WISHLIST_NAME)));
            /* Read each line as a card, and add them to the ArrayList */
            while ((line = br.readLine()) != null) {
                lWishlist.add(MtgCard.fromWishlistString(line, mCtx));
            }
        } catch (NumberFormatException e) {
            ToastWrapper.makeText(mCtx, e.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        } catch (IOException e) {
            /* Catches file not found exception when wishlist doesn't exist */
        }
        return lWishlist;
    }

    /**
     * Return a dialog in which a user can specify how many of what set of a card are in the wishlist
     *
     * @param mCardName      The name of the card
     * @param fragment       The fragment which hosts the dialog and receives onWishlistChanged()
     * @param showCardButton Whether the button to launch the CardViewFragment should be shown
     * @return A dialog which edits the wishlist
     */
    public static Dialog getDialog(final String mCardName, final FamiliarFragment fragment, boolean showCardButton) {

        final Context ctx = fragment.getActivity();

        /* Create the custom view */
        View customView = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog,
                null, false);
        assert customView != null;

        /* Grab the linear layout. Make it final to be accessible from the button later */
        final LinearLayout linearLayout = (LinearLayout) customView.findViewById(R.id.linear_layout);

        /* If the button should be shown, show it and attach a listener */
        if (showCardButton) {
            customView.findViewById(R.id.show_card_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle args = new Bundle();
                        /* Open the database */
                    SQLiteDatabase db = DatabaseManager.getInstance(fragment.getActivity(), false).openDatabase(false);
                    try {
                        /* Get the card ID, and send it to a new CardViewFragment */
                        args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, new long[]{CardDbAdapter.fetchIdByName(mCardName, db)});
                        args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
                        CardViewPagerFragment cvpFrag = new CardViewPagerFragment();
                        fragment.startNewFragment(cvpFrag, args);
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

        /* Read the wishlist */
        ArrayList<MtgCard> wishlist = ReadWishlist(ctx);

        /* Find any counts currently in the wishlist */
        final Map<String, String> targetCardNumberOfs = new HashMap<>();
        final Map<String, String> targetFoilCardNumberOfs = new HashMap<>();
        for (MtgCard card : wishlist) {
            if (card.mName.equals(mCardName)) {
                if (card.foil) {
                    targetFoilCardNumberOfs.put(card.setCode, card.numberOf + "");
                } else {
                    targetCardNumberOfs.put(card.setCode, card.numberOf + "");
                }
            }
        }

        /* Get all potential sets and rarities for this card */
        final ArrayList<String> potentialSetCodes = new ArrayList<>();
        final ArrayList<Character> potentialRarities = new ArrayList<>();
        final ArrayList<String> potentialNumbers = new ArrayList<>();

        /* Open the database */
        SQLiteDatabase db = DatabaseManager.getInstance(fragment.getActivity(), false).openDatabase(false);

        /* Get all the cards with relevant info from the database */
        Cursor cards;
        try {
            cards = CardDbAdapter.fetchCardByName(mCardName, new String[]{
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
                    CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME}, true, db);
        } catch (FamiliarDbException e) {
            DatabaseManager.getInstance(fragment.getActivity(), false).closeDatabase(false);
            return null;
        }

        Set<String> foilSets;
        try {
            foilSets = CardDbAdapter.getFoilSets(db);
        } catch (FamiliarDbException e) {
            DatabaseManager.getInstance(fragment.getActivity(), false).closeDatabase(false);
            return null;
        }

        /* For each card, add it to the wishlist view */
        while (!cards.isAfterLast()) {
            String setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
            String setName = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME));
            char rarity = (char) cards.getInt(cards.getColumnIndex(CardDbAdapter.KEY_RARITY));
            String number = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));

            /* Inflate a row and fill it with stuff */
            View wishlistRow = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row, null, false);
            assert wishlistRow != null;
            ((TextView) wishlistRow.findViewById(R.id.cardset)).setText(setName);
            String numberOf = targetCardNumberOfs.get(setCode);
            numberOf = numberOf == null ? "0" : numberOf;
            final Button numberButton = (Button) wishlistRow.findViewById(R.id.number_button);
            numberButton.setText(numberOf);
            numberButton.setOnClickListener(new NumberButtonOnClickListener(fragment) {
                @SuppressLint("SetTextI18n")
                @Override
                public void onDialogNumberSet(Integer number) {
                    numberButton.setText(number.toString());
                }
            });
            wishlistRow.findViewById(R.id.wishlistDialogFoil).setVisibility(View.GONE);
            linearLayout.addView(wishlistRow);
            potentialSetCodes.add(setCode);
            potentialRarities.add(rarity);
            potentialNumbers.add(number);

            /* If this card has a foil version, add that too */
            View wishlistRowFoil;
            if (foilSets.contains(setCode)) {
                wishlistRowFoil = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row,
                        null, false);
                assert wishlistRowFoil != null;
                ((TextView) wishlistRowFoil.findViewById(R.id.cardset)).setText(setName);
                String foilNumberOf = targetFoilCardNumberOfs.get(setCode);
                foilNumberOf = foilNumberOf == null ? "0" : foilNumberOf;
                final Button numberButtonFoil = (Button) wishlistRowFoil.findViewById(R.id.number_button);
                numberButtonFoil.setText(foilNumberOf);
                numberButtonFoil.setOnClickListener(new NumberButtonOnClickListener(fragment) {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDialogNumberSet(Integer number) {
                        numberButtonFoil.setText(number.toString());
                    }
                });
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
        DatabaseManager.getInstance(fragment.getActivity(), false).closeDatabase(false);

        /* make and return the actual dialog */
        return new MaterialDialog.Builder(ctx)
                .title(mCardName + " " + fragment.getString(R.string.wishlist_edit_dialog_title_end))
                .customView(customView, false)
                .positiveText(fragment.getString(R.string.dialog_ok))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                        /* Read the wishlist */
                        ArrayList<MtgCard> wishlist = ReadWishlist(ctx);

                        /* Add the cards listed in the dialog to the wishlist */
                        for (int i = 0; i < linearLayout.getChildCount(); i++) {
                            View view = linearLayout.getChildAt(i);
                            assert view != null;

                            /* build the card object */
                            MtgCard card = new MtgCard();
                            card.mName = mCardName;
                            card.setCode = potentialSetCodes.get(i);
                            try {
                                Button numberInput = ((Button) view.findViewById(R.id.number_button));
                                assert numberInput.getText() != null;
                                card.numberOf = Integer.valueOf(numberInput.getText().toString());
                            } catch (NumberFormatException e) {
                                card.numberOf = 0;
                            }
                            card.foil = (view.findViewById(R.id.wishlistDialogFoil).getVisibility() == View.VISIBLE);
                            card.mRarity = potentialRarities.get(i);
                            card.mNumber = potentialNumbers.get(i);

                            /* Look through the wishlist for each card, set the numberOf or remove it if it exists, or
                             * add the card if it doesn't */
                            boolean added = false;
                            for (int j = 0; j < wishlist.size(); j++) {
                                if (card.mName.equals(wishlist.get(j).mName)
                                        && card.setCode.equals(wishlist.get(j).setCode)
                                        && card.foil == wishlist.get(j).foil) {
                                    if (card.numberOf == 0) {
                                        wishlist.remove(j);
                                        j--;
                                    } else {
                                        wishlist.get(j).numberOf = card.numberOf;
                                    }
                                    added = true;
                                }
                            }
                            if (!added && card.numberOf > 0) {
                                wishlist.add(card);
                            }

                        }

                        /* Write the wishlist */
                        WriteWishlist(fragment.getActivity(), wishlist);
                        /* notify the fragment of a change in the wishlist */
                        fragment.onWishlistChanged(mCardName);
                    }
                })
                .negativeText(fragment.getString(R.string.dialog_cancel))
                .build();
    }

    /**
     * Take a wishlist and turn it into plaintext so that it can be shared via email or whatever,
     * with the choice of including the set in the wishlist export
     *
     * @param mCompressedWishlist The wishlist to share
     * @param ctx                 The context to get localized strings with
     * @param shareText           Whether or not the full card text should be exported
     * @param sharePrice          Whether or not the card price should be exported
     * @return A string containing all the wishlist data
     */
    public static String GetSharableWishlist(ArrayList<CompressedWishlistInfo> mCompressedWishlist,
                                             Context ctx, boolean shareText, boolean sharePrice,
                                             int priceOption) {
        StringBuilder readableWishlist = new StringBuilder();

        /* For each wishlist entry */
        for (CompressedWishlistInfo cwi : mCompressedWishlist) {
            /* Append the card name, always */
            readableWishlist.append(cwi.mCard.mName);
            readableWishlist.append("\r\n");

            /* Append the full text, if the user wants it */
            if (shareText) {
                cwi.mCard.appendCardText(readableWishlist);
            }

            /* For each set info in the wishlist */
            for (IndividualSetInfo isi : cwi.mInfo) {
                /* Append the number of the card, per-set */
                readableWishlist
                        .append(isi.mNumberOf)
                        .append(' ')
                        .append(isi.mSet);
                /* Append whether it is foil or not */
                if (isi.mIsFoil) {
                    readableWishlist
                            .append(" (")
                            .append(ctx.getString(R.string.wishlist_foil))
                            .append(")");
                }
                /* Attempt to append the price */
                if (sharePrice && isi.mPrice != null) {
                    double price = 0;
                    if (isi.mIsFoil) {
                        price = isi.mPrice.mFoilAverage;
                    } else {
                        switch (priceOption) {
                            case LOW_PRICE: {
                                price = isi.mPrice.mLow;
                                break;
                            }
                            case AVG_PRICE: {
                                price = isi.mPrice.mAverage;
                                break;
                            }
                            case HIGH_PRICE: {
                                price = isi.mPrice.mHigh;
                                break;
                            }
                        }
                    }
                    if (price != 0) {
                        readableWishlist
                                .append(", $")
                                .append(String.format(Locale.US, "%d.%02d", (int) price, (int) ((price - ((int) price)) * 100)));
                    }
                }
                readableWishlist.append("\r\n");
            }
            readableWishlist.append("\r\n");
        }
        return readableWishlist.toString();
    }

    /**
     * This class encapsulates a single MtgCard and an ArrayList of non-duplicated information for different printings
     * of that card
     */
    public static class CompressedWishlistInfo extends CardHelpers.CompressedCardInfo {

        int mIndex;

        /**
         * Constructor
         *
         * @param card The MtgCard which will be the base for this object
         */
        public CompressedWishlistInfo(MtgCard card, int index) {
            super(card);
            mIndex = index;
        }

        /**
         * Check to see if two CompressedWishlistInfo objects are equivalent, or if this is equivalent to a MtgCard
         * object. The comparison is done on the MtgCard's name
         *
         * @param o The object to compare to this one
         * @return true if the specified object is equal to this string, false otherwise.
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof CompressedWishlistInfo) {
                return mCard.mName.equals(((CompressedWishlistInfo) o).mCard.mName);
            } else if (o instanceof MtgCard) {
                return mCard.mName.equals(((MtgCard) o).mName);
            }
            return false;
        }

        /**
         * Clear all the different printings for this object
         */
        public void clearCompressedInfo() {
            mInfo.clear();
        }

        /**
         * Return the total price of all cards in this object
         *
         * @param priceSetting LOW_PRICE, AVG_PRICE, or HIGH_PRICE
         * @return The sum price of all cards in this object
         */
        public double getTotalPrice(int priceSetting) {
            double sumWish = 0;

            for (IndividualSetInfo isi : mInfo) {
                try {
                    if (isi.mIsFoil) {
                        sumWish += (isi.mPrice.mFoilAverage * isi.mNumberOf);
                    } else {
                        switch (priceSetting) {
                            case LOW_PRICE:
                                sumWish += (isi.mPrice.mLow * isi.mNumberOf);
                                break;
                            case AVG_PRICE:
                                sumWish += (isi.mPrice.mAverage * isi.mNumberOf);
                                break;
                            case HIGH_PRICE:
                                sumWish += (isi.mPrice.mHigh * isi.mNumberOf);
                                break;
                        }
                    }
                } catch (NullPointerException e) {
                    /* eat it, no price is loaded */
                }
            }
            return sumWish;
        }

        public int getIndex() {
            return mIndex;
        }
    }

    public static class WishlistComparator implements Comparator<CompressedWishlistInfo> {

        final ArrayList<SortOption> options = new ArrayList<>();
        int mPriceSetting = 0;

        /**
         * Constructor. It parses an "order by" string into search options. The first options have
         * higher priority
         *
         * @param orderByStr   The string to parse. It uses SQLite syntax: "KEY asc,KEY2 desc" etc
         * @param priceSetting The current price setting (LO/AVG/HIGH) used to sort by prices
         * @param orderByIndex true to order by index, false to order by values. This overrides orderByStr
         */
        public WishlistComparator(String orderByStr, int priceSetting, boolean orderByIndex) {
            if (orderByIndex) {
                options.clear();
            } else {
                int idx = 0;
                for (String option : orderByStr.split(",")) {
                    String key = option.split(" ")[0];
                    boolean ascending = option.split(" ")[1].equalsIgnoreCase(SortOrderDialogFragment.SQL_ASC);
                    options.add(new SortOption(null, ascending, key, idx++));
                }
                mPriceSetting = priceSetting;
            }
        }

        /**
         * Compare two CompressedWishlistInfo objects based on all the search options in descending priority
         *
         * @param wish1 One card to compare
         * @param wish2 The other card to compare
         * @return an integer < 0 if wish1 is less than wish2, 0 if they are equal, and > 0 if wish1 is greater than wish2.
         */
        @Override
        public int compare(CompressedWishlistInfo wish1, CompressedWishlistInfo wish2) {

            if (options.isEmpty()) {
                if (wish1.getIndex() < wish2.getIndex()) {
                    return -1;
                } else if (wish1.getIndex() == wish2.getIndex()) {
                    return 0;
                }
                return 1;
            }

            int retVal = 0;
            /* Iterate over all the sort options, starting with the high priority ones */
            for (SortOption option : options) {
                /* Compare the entries based on the key */
                try {
                    switch (option.getKey()) {
                        case CardDbAdapter.KEY_NAME: {
                            retVal = wish1.mCard.mName.compareTo(wish2.mCard.mName);
                            break;
                        }
                        case CardDbAdapter.KEY_COLOR: {
                            retVal = wish1.mCard.mColor.compareTo(wish2.mCard.mColor);
                            break;
                        }
                        case CardDbAdapter.KEY_SUPERTYPE: {
                            retVal = wish1.mCard.mType.compareTo(wish2.mCard.mType);
                            break;
                        }
                        case CardDbAdapter.KEY_CMC: {
                            retVal = wish1.mCard.mCmc - wish2.mCard.mCmc;
                            break;
                        }
                        case CardDbAdapter.KEY_POWER: {
                            retVal = Float.compare(wish1.mCard.mPower, wish2.mCard.mPower);
                            break;
                        }
                        case CardDbAdapter.KEY_TOUGHNESS: {
                            retVal = Float.compare(wish1.mCard.mToughness, wish2.mCard.mToughness);
                            break;
                        }
                        case CardDbAdapter.KEY_SET: {
                            retVal = wish1.mCard.mExpansion.compareTo(wish2.mCard.mExpansion);
                            break;
                        }
                        case SortOrderDialogFragment.KEY_PRICE: {
                            retVal = Double.compare(wish1.getTotalPrice(mPriceSetting), wish2.getTotalPrice(mPriceSetting));
                            break;
                        }
                    }
                } catch (NullPointerException e) {
                    retVal = 0;
                }

                /* Adjust for ascending / descending */
                if (!option.getAscending()) {
                    retVal = -retVal;
                }

                /* If these two entries aren't equal, return. Otherwise continue and compare the
                 * next value
                 */
                if (retVal != 0) {
                    return retVal;
                }
            }

            /* Guess they're totally equal */
            return retVal;
        }
    }

}
