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
                .setMinNumber(BigDecimal.valueOf(0))
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

    public Integer getInitialValue() {
        return null;
    }

    public String getLabelText() {
        return "";
    }

    public Integer getMaxNumber() {
        return 99;
    }

    public abstract void onDialogNumberSet(Integer number);
}
