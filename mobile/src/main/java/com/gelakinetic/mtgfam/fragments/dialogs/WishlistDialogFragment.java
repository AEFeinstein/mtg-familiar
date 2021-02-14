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

package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import java.util.Objects;

/**
 * Class that creates dialogs for WishlistFragment
 */
public class WishlistDialogFragment extends FamiliarDialogFragment {

    /* Dialog constants */
    public static final int DIALOG_UPDATE_CARD = 1;
    public static final int DIALOG_PRICE_SETTING = 2;
    public static final int DIALOG_CONFIRMATION = 3;
    public static final int DIALOG_SORT = 4;

    public static final String NAME_KEY = "name";

    /**
     * @return The currently viewed DiceFragment
     */
    @Nullable
    private WishlistFragment getParentWishlistFragment() {
        try {
            return (WishlistFragment) getParentFamiliarFragment();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        setShowsDialog(true);
        mDialogId = Objects.requireNonNull(getArguments()).getInt(ID_KEY);
        String cardName = getArguments().getString(NAME_KEY);

        if (null == getParentWishlistFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case DIALOG_UPDATE_CARD: {
                Dialog dialog = CardHelpers.getDialog(cardName, Objects.requireNonNull(getParentWishlistFragment()), true, false);
                if (dialog == null) {
                    if (null != getParentWishlistFragment()) {
                        getParentWishlistFragment().handleFamiliarDbException(false);
                    }
                    return DontShowDialog();
                }
                return dialog;
            }
            case DIALOG_PRICE_SETTING: {
                return new MaterialDialog.Builder(Objects.requireNonNull(this.getActivity()))
                        .title(R.string.pref_trade_price_title)
                        .items(getResources().getStringArray(R.array.trade_option_entries))
                        .itemsCallbackSingleChoice(getParentWishlistFragment().getPriceSetting().ordinal(), (dialog, itemView, which, text) -> {
                            if (getParentWishlistFragment().getPriceSetting().ordinal() != which) {
                                getParentWishlistFragment().setPriceSetting(MarketPriceInfo.PriceType.fromOrdinal(which));
                                PreferenceAdapter.setWishlistPrice(getContext(), getParentWishlistFragment().getPriceSetting());
                                getParentWishlistFragment().getCardDataAdapter(0).notifyDataSetChanged();
                                getParentWishlistFragment().updateTotalPrices(0);
                            }
                            dialog.dismiss();
                            return true;
                        })
                        .build();
            }
            case DIALOG_CONFIRMATION: {
                return new MaterialDialog.Builder(Objects.requireNonNull(this.getActivity()))
                        .title(R.string.wishlist_empty_dialog_title)
                        .content(R.string.wishlist_empty_dialog_text)
                        .positiveText(R.string.dialog_ok)
                        .onPositive((dialog, which) -> {
                            WishlistHelpers.ResetCards(getActivity());
                            synchronized (getParentWishlistFragment().mCompressedWishlist) {
                                getParentWishlistFragment().mCompressedWishlist.clear();
                            }
                            getParentWishlistFragment().getCardDataAdapter(0).notifyDataSetChanged();
                            getParentWishlistFragment().updateTotalPrices(0);
                            /* Clear input too */
                            getParentWishlistFragment().clearCardNameInput();
                            getParentWishlistFragment().clearCardNumberInput();
                            getParentWishlistFragment().uncheckFoilCheckbox();
                            dialog.dismiss();
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