package com.gelakinetic.mtgfam.helpers;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;
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
                    card.set = isi.mSet;
                    card.setCode = isi.mSetCode;
                    card.number = isi.mNumber;
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
            if (card.name.equals(mCardName)) {
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
            ((EditText) wishlistRow.findViewById(R.id.numberInput)).setText(numberOf);
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
                            card.name = mCardName;
                            card.setCode = potentialSetCodes.get(i);
                            try {
                                EditText numberInput = ((EditText) view.findViewById(R.id.numberInput));
                                assert numberInput.getText() != null;
                                card.numberOf = Integer.valueOf(numberInput.getText().toString());
                            } catch (NumberFormatException e) {
                                card.numberOf = 0;
                            }
                            card.foil = (view.findViewById(R.id.wishlistDialogFoil).getVisibility() == View.VISIBLE);
                            card.rarity = potentialRarities.get(i);
                            card.number = potentialNumbers.get(i);

                            /* Look through the wishlist for each card, set the numberOf or remove it if it exists, or
                             * add the card if it doesn't */
                            boolean added = false;
                            for (int j = 0; j < wishlist.size(); j++) {
                                if (card.name.equals(wishlist.get(j).name)
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
            readableWishlist.append(cwi.mCard.name);
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
                            case WishlistFragment.LOW_PRICE: {
                                price = isi.mPrice.mLow;
                                break;
                            }
                            case WishlistFragment.AVG_PRICE: {
                                price = isi.mPrice.mAverage;
                                break;
                            }
                            case WishlistFragment.HIGH_PRICE: {
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
    public static class CompressedWishlistInfo implements CompressedCardInfo {

        public final MtgCard mCard;
        public final ArrayList<IndividualSetInfo> mInfo;

        /**
         * Constructor
         *
         * @param card The MtgCard which will be the base for this object
         */
        public CompressedWishlistInfo(MtgCard card) {
            mInfo = new ArrayList<>();
            mCard = card;
            add(mCard);
        }

        /**
         * Add a new printing of a MtgCard to this object
         *
         * @param card The new printing to add to this object
         */
        public void add(MtgCard card) {
            IndividualSetInfo isi = new IndividualSetInfo();

            isi.mSet = card.setName;
            isi.mSetCode = card.setCode;
            isi.mNumber = card.number;
            isi.mIsFoil = card.foil;
            isi.mPrice = null;
            isi.mMessage = card.message;
            isi.mNumberOf = card.numberOf;
            isi.mRarity = card.rarity;

            mInfo.add(isi);
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
                return mCard.name.equals(((CompressedWishlistInfo) o).mCard.name);
            } else if (o instanceof MtgCard) {
                return mCard.name.equals(((MtgCard) o).name);
            }
            return false;
        }

        /**
         * Clear all the different printings for this object
         */
        public void clearCompressedInfo() {
            mInfo.clear();
        }

        public MtgCard getCard() {
            return mCard;
        }

        public ArrayList<IndividualSetInfo> getSetInfo() {
            return mInfo;
        }

    }

    /* Comparator based on converted mana cost */
    public static class WishlistComparatorCmc implements Comparator<CompressedWishlistInfo> {
        @Override
        public int compare(CompressedWishlistInfo wish1, CompressedWishlistInfo wish2) {
            if (wish1.mCard.cmc == wish2.mCard.cmc) {
                return wish1.mCard.name.compareTo(wish2.mCard.name);
            } else if (wish1.mCard.cmc > wish2.mCard.cmc) {
                return 1;
            }
            return -1;
        }
    }

    /* Comparator based on color */
    public static class WishlistComparatorColor implements Comparator<CompressedWishlistInfo> {
        private static final String colors = "WUBRG";
        private static final String nonColors = "LAC";

        /* Filters a color string to only include chars representing colors (e.g. "LG" (Dryad Arbor) will return "G"). */
        public String getColors(String c) {
            String validColors = "";
            //1. Catch null/empty string
            if (c == null || c.isEmpty()) {
                return "";
            }
            //2. For each char, if a valid color, add to return String
            for (int i = 0; i < c.length(); i++) {
                if (colors.indexOf(c.charAt(i)) > -1) {
                    validColors += c.charAt(i);
                }
            }
            return validColors;
        }

        @Override
        public int compare(CompressedWishlistInfo wish1, CompressedWishlistInfo wish2) {
            String colors1 = getColors(wish1.mCard.color);
            String colors2 = getColors(wish2.mCard.color);
            int priority1;
            int priority2;
            //1. If colorless, perform colorless comparison
            if (colors1.length() + colors2.length() == 0) {
                colors1 = wish1.mCard.color;
                colors2 = wish2.mCard.color;
                for (int i = 0; i < Math.min(colors1.length(), colors2.length()); i++) {
                    priority1 = nonColors.indexOf(colors1.charAt(i));
                    priority2 = nonColors.indexOf(colors2.charAt(i));
                    if (priority1 != priority2) {
                        return priority1 < priority2 ? -1 : 1;
                    }
                }
                return wish1.mCard.name.compareTo(wish2.mCard.name);
            }
            //2. Else compare based on number of colors
            if (colors1.length() < colors2.length()) {
                return -1;
            } else if (colors1.length() > colors2.length()) {
                return 1;
            }
            //3. Else if same number of colors exist, compare based on WUBRG-ness
            else {
                for (int i = 0; i < Math.min(colors1.length(), colors2.length()); i++) {
                    priority1 = colors.indexOf(colors1.charAt(i));
                    priority2 = colors.indexOf(colors2.charAt(i));
                    if (priority1 != priority2) {
                        return priority1 < priority2 ? -1 : 1;
                    }
                }
                return wish1.mCard.name.compareTo(wish2.mCard.name);
            }
        }
    }

    /* Comparator based on name */
    public static class WishlistComparatorName implements Comparator<CompressedWishlistInfo> {
        @Override
        public int compare(CompressedWishlistInfo wish1, CompressedWishlistInfo wish2) {
            return wish1.mCard.name.compareTo(wish2.mCard.name);
        }
    }

    /* Comparator based on first set of a card */
    public static class WishlistComparatorSet implements Comparator<CompressedWishlistInfo> {
        @Override
        public int compare(CompressedWishlistInfo wish1, CompressedWishlistInfo wish2) {
            return wish1.mInfo.get(0).mSet.compareTo(wish2.mInfo.get(0).mSet);
        }
    }

    /* Comparator based on price */
    public static class WishlistComparatorPrice implements Comparator<CompressedWishlistInfo> {
        /* Price setting constants */
        private static final int LOW_PRICE = 0;
        private static final int AVG_PRICE = 1;
        private static final int HIGH_PRICE = 2;

        private final int mPriceSetting;

        public WishlistComparatorPrice(int mPriceSetting) {
            this.mPriceSetting = mPriceSetting;
        }

        @Override
        public int compare(CompressedWishlistInfo wish1, CompressedWishlistInfo wish2) {
            double sumWish1 = 0;
            double sumWish2 = 0;

            for (IndividualSetInfo isi : wish1.mInfo) {
                try {
                    if (isi.mIsFoil) {
                        sumWish1 += (isi.mPrice.mFoilAverage * isi.mNumberOf);
                    } else {
                        switch (mPriceSetting) {
                            case LOW_PRICE:
                                sumWish1 += (isi.mPrice.mLow * isi.mNumberOf);
                                break;
                            case AVG_PRICE:
                                sumWish1 += (isi.mPrice.mAverage * isi.mNumberOf);
                                break;
                            case HIGH_PRICE:
                                sumWish1 += (isi.mPrice.mHigh * isi.mNumberOf);
                                break;
                        }
                    }
                } catch (NullPointerException e) {
                    /* eat it, no price is loaded */
                }
            }

            for (IndividualSetInfo isi : wish2.mInfo) {
                try {
                    if (isi.mIsFoil) {
                        sumWish2 += (isi.mPrice.mFoilAverage * isi.mNumberOf);
                    } else {
                        switch (mPriceSetting) {
                            case LOW_PRICE:
                                sumWish2 += (isi.mPrice.mLow * isi.mNumberOf);
                                break;
                            case AVG_PRICE:
                                sumWish2 += (isi.mPrice.mAverage * isi.mNumberOf);
                                break;
                            case HIGH_PRICE:
                                sumWish2 += (isi.mPrice.mHigh * isi.mNumberOf);
                                break;
                        }
                    }
                } catch (NullPointerException e) {
                    /* eat it, no price is loaded */
                }
            }

            if (sumWish1 == sumWish2) {
                return wish1.mCard.name.compareTo(wish2.mCard.name);
            } else if (sumWish1 > sumWish2) {
                return 1;
            }
            return -1;
        }
    }
}
