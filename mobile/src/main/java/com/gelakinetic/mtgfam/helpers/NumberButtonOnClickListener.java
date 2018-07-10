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

import android.view.View;

import com.codetroopers.betterpickers.numberpicker.NumberPickerBuilder;
import com.codetroopers.betterpickers.numberpicker.NumberPickerDialogFragment;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class NumberButtonOnClickListener implements View.OnClickListener,
        NumberPickerDialogFragment.NumberPickerDialogHandlerV2 {
    private final FamiliarFragment fragment;

    public NumberButtonOnClickListener(FamiliarFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onClick(View v) {
        NumberPickerBuilder npb = new NumberPickerBuilder()
                .addNumberPickerDialogHandler(this)
                .setDecimalVisibility(View.GONE)
                .setFragmentManager(fragment.getFragmentManager())
                .setLabelText(this.getLabelText())
                .setMaxNumber(BigDecimal.valueOf(this.getMaxNumber()))
                .setMinNumber(BigDecimal.valueOf(this.getMinNumber()))
                .setPlusMinusVisibility(View.GONE)
                .setStyleResId(fragment.getResourceIdFromAttr(R.attr.num_picker_style));
        if (this.getInitialValue() != null) {
            npb.setCurrentNumber(this.getInitialValue());
        }

        npb.show();
    }

    @Override
    public void onDialogNumberSet(int reference, BigInteger number, double decimal,
                                  boolean isNegative, BigDecimal fullNumber) {
        onDialogNumberSet(number.intValue());
    }

    protected Integer getInitialValue() {
        return null;
    }

    private String getLabelText() {
        return "";
    }

    protected Integer getMaxNumber() {
        return 99;
    }

    protected Integer getMinNumber() {
        return 0;
    }

    protected abstract void onDialogNumberSet(Integer number);
}
