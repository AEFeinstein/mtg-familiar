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

package com.gelakinetic.mtgfam.fragments;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.WishlistDialogFragment;
import com.gelakinetic.mtgfam.helpers.CardDataAdapter;
import com.gelakinetic.mtgfam.helpers.CardDataViewHolder;
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;
import com.gelakinetic.mtgfam.helpers.ExpansionImageHelper;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * This class displays a wishlist of cards, details about the cards, their prices, and the sum of their prices
 */
public class WishlistFragment extends FamiliarListFragment {

    /* Preferences */
    private boolean mShowCardInfo;
    private boolean mShowIndividualPrices;

    /* The wishlist and adapter */
    public final List<CompressedWishlistInfo> mCompressedWishlist = Collections.synchronizedList(new ArrayList<>());
    private int mOrderAddedIdx = 0;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myFragmentView = inflater.inflate(R.layout.wishlist_frag, container, false);
        assert myFragmentView != null;

        TextView.OnEditorActionListener addCardListener = (arg0, arg1, arg2) -> {
            if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
                addCardToWishlist();
                return true;
            }
            return false;
        };

        /* Make sure to initialize shared members */
        synchronized (mCompressedWishlist) {
            initializeMembers(
                    myFragmentView,
                    new int[]{R.id.cardlist},
                    new CardDataAdapter[]{new WishlistDataAdapter(mCompressedWishlist)},
                    new int[]{R.id.priceText}, new int[]{R.id.divider_total_price},
                    R.menu.action_mode_menu, addCardListener);
        }
        myFragmentView.findViewById(R.id.add_card).setOnClickListener(view -> addCardToWishlist());

        return myFragmentView;
    }

    @Override
    public void onPause() {
        super.onPause();
        /* unsort, then save the wishlist */
        sortWishlist(SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_ASC);
        synchronized (mCompressedWishlist) {
            WishlistHelpers.WriteCompressedWishlist(getActivity(), mCompressedWishlist);
        }
    }

    /**
     * This function takes care of adding a card to the wishlist from this fragment. It makes sure that fields are
     * not null or have bad information.
     */
    private void addCardToWishlist() {
        /* Do not allow empty fields */
        String name = String.valueOf(getCardNameInput());
        if (name == null || name.equals("")) {
            return;
        }
        String numberOf = (String.valueOf(getCardNumberInput()));
        if (numberOf == null || numberOf.equals("")) {
            return;
        }

        ArrayList<String> nonFoilSets;
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            SQLiteDatabase database = DatabaseManager.openDatabase(getContext(), false, handle);
            nonFoilSets = CardDbAdapter.getNonFoilSets(database);
        } catch (SQLiteException | FamiliarDbException | IllegalStateException ignored) {
            nonFoilSets = new ArrayList<>();
        } finally {
            DatabaseManager.closeDatabase(getContext(), handle);
        }

        try {
            MtgCard card = new MtgCard(getActivity(), name, null, checkboxFoilIsChecked(), Integer.parseInt(numberOf));
            CompressedWishlistInfo wrapped = new CompressedWishlistInfo(card, 0);

            /* Add it to the wishlist, either as a new CompressedWishlistInfo, or to an existing one */
            synchronized (mCompressedWishlist) {
                if (mCompressedWishlist.contains(wrapped)) {
                    CompressedWishlistInfo cwi = mCompressedWishlist.get(mCompressedWishlist.indexOf(wrapped));
                    boolean added = false;
                    for (IndividualSetInfo isi : cwi.mInfo) {
                        if (isi.mSetCode.equals(card.getExpansion()) &&
                                (isi.mIsFoil.equals(card.mIsFoil) || nonFoilSets.contains(isi.mSetCode))) {
                            added = true;
                            isi.mNumberOf++;
                        }
                    }
                    if (!added) {
                        cwi.add(card);
                    }
                } else {
                    mCompressedWishlist.add(new CompressedWishlistInfo(card, mOrderAddedIdx++));
                }
            }

            /* load the price */
            loadPrice(card);

            /* Sort the wishlist */
            sortWishlist(PreferenceAdapter.getWishlistSortOrder(getContext()));

            /* Clean up for the next add */
            clearCardNameInput();
            uncheckFoilCheckbox();
            /* Don't reset the count after adding a card. This makes adding consecutive 4-ofs easier */
            /* clearCardNumberInput(); */

            /* Redraw the new wishlist with the new card */
            getCardDataAdapter(0).notifyDataSetChanged();
        } catch (java.lang.InstantiationException e) {
            /* Eat it */
        }
    }

    /**
     * Read the preferences, show or hide the total price, read and compress the wishlist, and load prices
     */
    @Override
    public void onResume() {
        super.onResume();

        /* Get the relevant preferences */
        mShowIndividualPrices = PreferenceAdapter.getShowIndividualWishlistPrices(getContext());
        mShowCardInfo = PreferenceAdapter.getVerboseWishlist(getContext());

        /* Clear, then read the wishlist. This is done in onResume() in case the user quick-searched for a card, and
         * added it to the wishlist from the CardViewFragment */
        readAndCompressWishlist(null);
        getCardDataAdapter(0).notifyDataSetChanged();
    }

    /**
     * Read in the wishlist from the file, and pack it into an ArrayList of CompressedWishlistInfo for display in the
     * ListView. This data structure stores one copy of the card itself, and a list of set-specific attributes like
     * set name, rarity, and price.
     *
     * @param changedCardName If the wishlist was changed by a dialog, this is the card which we should look at for
     *                        changes
     */
    private void readAndCompressWishlist(String changedCardName) {
        synchronized (mCompressedWishlist) {
            try {
                /* Read the wishlist */
                ArrayList<MtgCard> wishlist = WishlistHelpers.ReadWishlist(getActivity(), true);

                /* Clear the wishlist, or just the card that changed */
                if (changedCardName == null) {
                    mCompressedWishlist.clear();
                } else {
                    for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                        if (cwi.getName().equals(changedCardName)) {
                            cwi.clearCompressedInfo();
                        }
                    }
                }

                /* Compress the whole wishlist, or just the card that changed */
                for (MtgCard card : wishlist) {
                    if (changedCardName == null || changedCardName.equals(card.getName())) {
                        /* This works because both MtgCard's and CompressedWishlistInfo's .equals() can compare each
                         * other */
                        CompressedWishlistInfo wrapped = new CompressedWishlistInfo(card, 0);
                        if (mCompressedWishlist.contains(wrapped)) {
                            mCompressedWishlist.get(mCompressedWishlist.indexOf(wrapped)).add(card);
                        } else {
                            mCompressedWishlist.add(new CompressedWishlistInfo(card, mOrderAddedIdx++));
                        }
                        /* Look up the new price */
                        if (mShowIndividualPrices || shouldShowPrice()) {
                            loadPrice(card);
                        }
                    }
                }
                /* Check for wholly removed cards if one card was modified */
                if (changedCardName != null) {
                    for (int i = 0; i < mCompressedWishlist.size(); i++) {
                        if (mCompressedWishlist.get(i).mInfo.size() == 0) {
                            mCompressedWishlist.remove(i);
                            i--;
                        }
                    }
                }
            } catch (FamiliarDbException e) {
                handleFamiliarDbException(true);
            }
        }
    }

    /**
     * This notifies the fragment when a change has been made from a card's dialog
     */
    @Override
    public void onWishlistChanged(final String cardName) {
        readAndCompressWishlist(cardName);
        getCardDataAdapter(0).notifyDataSetChanged();
    }

    /**
     * Create the options menu
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.wishlist_menu, menu);
    }

    /**
     * Handle a click from the options menu
     *
     * @param item The item clicked
     * @return true if the click was handled, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.wishlist_menu_clear) {                /* Show a dialog to confirm clearing the wishlist */
            showDialog(WishlistDialogFragment.DIALOG_CONFIRMATION, null);
            return true;
        } else if (item.getItemId() == R.id.wishlist_menu_settings) {                /* Show a dialog to change which price (low/avg/high) is used */
            showDialog(WishlistDialogFragment.DIALOG_PRICE_SETTING, null);
            return true;
        } else if (item.getItemId() == R.id.wishlist_menu_sort) {                /* Show a dialog to change the sort criteria the list uses */
            showDialog(WishlistDialogFragment.DIALOG_SORT, null);
            return true;
        } else if (item.getItemId() == R.id.wishlist_menu_share) {                /* Share the plaintext wishlist */
            /* Use a more generic send text intent. It can also do emails */
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.wishlist_share_title);
            synchronized (mCompressedWishlist) {
                sendIntent.putExtra(Intent.EXTRA_TEXT, WishlistHelpers.GetSharableWishlist(mCompressedWishlist, getActivity(),
                        mShowCardInfo, mShowIndividualPrices, getPriceSetting()));
            }
            sendIntent.setType("text/plain");

            try {
                startActivity(Intent.createChooser(sendIntent, getString(R.string.wishlist_share)));
            } catch (android.content.ActivityNotFoundException ex) {
                SnackbarWrapper.makeAndShowText(getActivity(), R.string.error_no_email_client, SnackbarWrapper.LENGTH_SHORT);
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Remove any showing dialogs, and show the requested one
     *
     * @param id       the ID of the dialog to show
     * @param cardName The name of the card to use if this is a dialog to change wishlist counts
     */
    private void showDialog(final int id, final String cardName) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        /* If the fragment isn't visible (if desired being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        /* Create and show the dialog. */
        if (id == WishlistDialogFragment.DIALOG_SORT) {
            SortOrderDialogFragment newFragment = new SortOrderDialogFragment();
            Bundle args = new Bundle();
            args.putString(SortOrderDialogFragment.SAVED_SORT_ORDER,
                    PreferenceAdapter.getWishlistSortOrder(getContext()));
            newFragment.setArguments(args);
            newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
        } else {
            WishlistDialogFragment newFragment = new WishlistDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
            arguments.putString(WishlistDialogFragment.NAME_KEY, cardName);
            newFragment.setArguments(arguments);
            newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
        }
    }

    @Override
    protected void onCardPriceLookupFailure(MtgCard data, Throwable exception) {
        synchronized (mCompressedWishlist) {
            for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                if (cwi.getName().equals(data.getName())) {
                    /* Find all foil and non foil compressed items with the same set code */
                    for (IndividualSetInfo isi : cwi.mInfo) {
                        if (isi.mSetCode.equals(data.getExpansion())) {
                            /* Set the price as null and the message as the exception */
                            if (null != exception.getLocalizedMessage()) {
                                isi.mMessage = exception.getLocalizedMessage();
                            } else {
                                isi.mMessage = exception.getClass().getSimpleName();
                            }
                            isi.mPrice = null;
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onCardPriceLookupSuccess(MtgCard data, MarketPriceInfo result) {
        synchronized (mCompressedWishlist) {
            for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                if (cwi.getName().equals(data.getName())) {
                    /* Find all foil and non foil compressed items with the same set code */
                    for (IndividualSetInfo isi : cwi.mInfo) {
                        if (isi.mSetCode.equals(data.getExpansion())) {
                            /* Set the whole price info object */
                            if (result != null) {
                                isi.mPrice = result;
                            }
                            /* The message will never be shown with a valid price, so set it as DNE */
                            isi.mMessage = getString(R.string.card_view_price_not_found);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onAllPriceLookupsFinished() {
        updateTotalPrices(0);
        sortWishlist(PreferenceAdapter.getWishlistSortOrder(getContext()));
    }

    /**
     * Add together the price of all the cards in the wishlist and display it
     */
    public void updateTotalPrices(int side) {
        if (shouldShowPrice()) {
            float totalPrice = 0;
            synchronized (mCompressedWishlist) {
                for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                    for (IndividualSetInfo isi : cwi.mInfo) {
                        if (isi.mPrice != null) {
                            totalPrice += (isi.mPrice.getPrice(isi.mIsFoil, getPriceSetting()).price * isi.mNumberOf);
                        }
                    }
                }
            }
            setTotalPrice(String.format(Locale.US, PRICE_FORMAT, totalPrice), null, 0);
        }
    }

    @Override
    public boolean shouldShowPrice() {
        return PreferenceAdapter.getShowTotalWishlistPrice(getContext());
    }

    @Override
    public MarketPriceInfo.PriceType getPriceSetting() {
        return PreferenceAdapter.getWishlistPrice(getContext());
    }

    @Override
    public void setPriceSetting(MarketPriceInfo.PriceType priceSetting) {
        PreferenceAdapter.setWishlistPrice(getContext(), priceSetting);
    }

    /**
     * Called when the sorting dialog closes. Sort the wishlist with the new options
     *
     * @param orderByStr The sort order string
     */
    @Override
    public void receiveSortOrder(String orderByStr) {
        PreferenceAdapter.setWishlistSortOrder(getContext(), orderByStr);
        sortWishlist(orderByStr);
    }

    /**
     * Sorts the wishlist based on mWishlistSortType and mWishlistSortOrder
     */
    private void sortWishlist(String orderByStr) {
        synchronized (mCompressedWishlist) {
            Collections.sort(mCompressedWishlist,
                    new WishlistHelpers.WishlistComparator(orderByStr, getPriceSetting()));
            getCardDataAdapter(0).notifyDataSetChanged();
        }
    }

    class WishlistViewHolder extends CardDataViewHolder {

        /* Card Information */
        final TextView mCardType;
        final TextView mCardText;
        final TextView mCardPower;
        final TextView mCardSlash;
        final TextView mCardToughness;
        final TextView mCardCost;

        /* For adding individual wishlist sets */
        final LinearLayout mWishlistSets;

        WishlistViewHolder(ViewGroup view) {
            super(view, R.layout.result_list_card_row, WishlistFragment.this.getCardDataAdapter(0), WishlistFragment.this);

            mCardType = itemView.findViewById(R.id.cardtype);
            mCardText = itemView.findViewById(R.id.cardability);
            mCardPower = itemView.findViewById(R.id.cardp);
            mCardSlash = itemView.findViewById(R.id.cardslash);
            mCardToughness = itemView.findViewById(R.id.cardt);
            mCardCost = itemView.findViewById(R.id.cardcost);
            mWishlistSets = itemView.findViewById(R.id.wishlist_sets);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClickNotSelectMode(View view, int position) {
            // Make sure the wishlist is written first in the proper order
            sortWishlist(SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_ASC);
            synchronized (mCompressedWishlist) {
                WishlistHelpers.WriteCompressedWishlist(getActivity(), mCompressedWishlist);
            }
            sortWishlist(PreferenceAdapter.getWishlistSortOrder(getContext()));

            // Then show the dialog
            showDialog(WishlistDialogFragment.DIALOG_UPDATE_CARD, getCardName());
        }

    }

    /**
     * The adapter that drives the wish list
     */
    class WishlistDataAdapter
            extends CardDataAdapter<CompressedWishlistInfo, WishlistViewHolder> {

        WishlistDataAdapter(List<CompressedWishlistInfo> values) {
            super(values, WishlistFragment.this);
        }

        @NonNull
        @Override
        public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new WishlistViewHolder(parent);
        }

        @Override
        protected void onItemReadded() {
            // Sort the wishlist
            sortWishlist(PreferenceAdapter.getWishlistSortOrder(getContext()));

            // Call super to notify the adapter, etc
            super.onItemReadded();
        }

        @Override
        protected void onItemRemovedFinal() {
            // Unsort, save, then sort the wishlist
            sortWishlist(SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_ASC);
            synchronized (this.items) {
                WishlistHelpers.WriteCompressedWishlist(getActivity(), this.items);
            }
            sortWishlist(PreferenceAdapter.getWishlistSortOrder(getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            /* Get all the wishlist info for this entry */
            final CompressedWishlistInfo info = getItem(position);

            /* Make sure you can see the item */
            holder.itemView.findViewById(R.id.card_row_full).setVisibility(View.VISIBLE);

            /* Clear out the old items in the view */
            holder.mWishlistSets.removeAllViews();

            /* Set the card name, always */
            holder.setCardName(Objects.requireNonNull(info).getName());

            /* Show or hide full card information */
            holder.itemView.findViewById(R.id.cardsetcombo).setVisibility(View.GONE);
            if (mShowCardInfo) {
                Html.ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getActivity());
                /* make sure everything is showing */
                holder.mCardCost.setVisibility(View.VISIBLE);
                holder.mCardType.setVisibility(View.VISIBLE);
                holder.mCardText.setVisibility(View.VISIBLE);
                /* Show the power, toughness, or loyalty if the card has it */
                holder.mCardPower.setVisibility(View.GONE);
                holder.mCardSlash.setVisibility(View.GONE);
                holder.mCardToughness.setVisibility(View.GONE);
                /* Set the type, cost, and ability */
                holder.mCardType.setText(info.getType());
                holder.mCardCost.setText(ImageGetterHelper.formatStringWithGlyphs(info.getManaCost(), imgGetter));
                holder.mCardText.setText(ImageGetterHelper.formatStringWithGlyphs(info.getText(), imgGetter));
                try {
                    boolean shouldShowSign = false;
                    for (IndividualSetInfo isi : info.mInfo) {
                        if (isi.mSetCode.equals("UST")) {
                            shouldShowSign = info.getText().contains("Augment {");
                            break;
                        }
                    }
                    String power = CardDbAdapter.getPrintedPTL(info.getPower(), shouldShowSign);
                    String toughness = CardDbAdapter.getPrintedPTL(info.getToughness(), shouldShowSign);
                    holder.mCardPower.setText(power);
                    holder.mCardToughness.setText(toughness);
                    if (!power.isEmpty() && !toughness.isEmpty()) {
                        holder.mCardPower.setVisibility(View.VISIBLE);
                        holder.mCardSlash.setVisibility(View.VISIBLE);
                        holder.mCardToughness.setVisibility(View.VISIBLE);
                    }
                } catch (NumberFormatException nfe) {
                    /* eat it */
                }

                /* Show the loyalty, if the card has any (traitor...) */
                float loyalty = info.getLoyalty();
                if (loyalty != -1 && loyalty != CardDbAdapter.NO_ONE_CARES) {
                    holder.mCardPower.setVisibility(View.GONE);
                    holder.mCardSlash.setVisibility(View.GONE);
                    holder.mCardToughness.setText(CardDbAdapter.getPrintedPTL(loyalty, false));
                    holder.mCardToughness.setVisibility(View.VISIBLE);
                }
            } else {
                /* hide all the extra fields */
                holder.mCardCost.setVisibility(View.GONE);
                holder.mCardType.setVisibility(View.GONE);
                holder.mCardText.setVisibility(View.GONE);
                holder.mCardPower.setVisibility(View.GONE);
                holder.mCardSlash.setVisibility(View.GONE);
                holder.mCardToughness.setVisibility(View.GONE);
            }

            /* Rarity is displayed on the expansion lines */
            holder.itemView.findViewById(R.id.rarity).setVisibility(View.GONE);

            /* List all the sets and wishlist values for this card */
            for (IndividualSetInfo isi : info.mInfo) {
                /* inflate a new row */
                View setRow = Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.wishlist_cardset_row, (ViewGroup) holder.itemView.getParent(), false);
                ExpansionImageHelper.loadExpansionImage(getContext(), isi.mSetCode, isi.mRarity, setRow.findViewById(R.id.wishlistRowSetImage), null, ExpansionImageHelper.ExpansionImageSize.SMALL);
                /* Write the set name, color it with the rarity */
                int color;
                switch (isi.mRarity) {
                    case 'c':
                    case 'C':
                        color = R.attr.color_common;
                        break;
                    case 'u':
                    case 'U':
                        color = R.attr.color_uncommon;
                        break;
                    case 'r':
                    case 'R':
                        color = R.attr.color_rare;
                        break;
                    case 'm':
                    case 'M':
                        color = R.attr.color_mythic;
                        break;
                    case 't':
                    case 'T':
                        color = R.attr.color_timeshifted;
                        break;
                    default:
                        color = R.attr.color_text;
                        break;
                }
                String setAndRarity = isi.mSet + " (" + isi.mRarity + ")";
                ((TextView) setRow.findViewById(R.id.wishlistRowSet)).setText(setAndRarity);
                ((TextView) setRow.findViewById(R.id.wishlistRowSet)).setTextColor(
                        ContextCompat.getColor(Objects.requireNonNull(getContext()), getResourceIdFromAttr(color)));

                /* Show individual prices and number of each card, or message if price does not exist, if desired */
                TextView priceText = setRow.findViewById(R.id.wishlistRowPrice);
                if (mShowIndividualPrices) {
                    double price;
                    if (isi.mPrice == null || (price = isi.mPrice.getPrice(isi.mIsFoil, getPriceSetting()).price) == 0) {
                        priceText.setText(String.format(Locale.US, "%dx %s", isi.mNumberOf, isi.mMessage));
                        priceText.setTextColor(ContextCompat.getColor(getContext(), R.color.material_red_500));
                    } else {
                        priceText.setText(String.format(Locale.US, "%dx " + PRICE_FORMAT, isi.mNumberOf, price));
                        priceText.setTextColor(ContextCompat.getColor(getContext(), getResourceIdFromAttr(R.attr.color_text)));
                        isi.mIsFoil = isi.mPrice.getPrice(isi.mIsFoil, getPriceSetting()).isFoil;
                    }
                } else {
                    /* Just show the number of */
                    priceText.setText(String.format(Locale.getDefault(), "x%d", isi.mNumberOf));
                }

                /* Show or hide the foil indicator */
                if (isi.mIsFoil) {
                    setRow.findViewById(R.id.wishlistSetRowFoil).setVisibility(View.VISIBLE);
                } else {
                    setRow.findViewById(R.id.wishlistSetRowFoil).setVisibility(View.GONE);
                }

                /* Add the view to the linear layout */
                holder.mWishlistSets.addView(setRow);
            }
        }
    }
}
