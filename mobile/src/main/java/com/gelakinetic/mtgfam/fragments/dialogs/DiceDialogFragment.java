package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.DiceFragment;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Adam on 4/21/2016.
 */
public class DiceDialogFragment extends FamiliarDialogFragment {

    /**
     * @return The currently viewed DiceFragment
     */
    DiceFragment getParentDiceFragment(){
        return (DiceFragment) getFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setShowsDialog(true);

        View v = getParentDiceFragment().mActivity.getLayoutInflater().inflate(R.layout.number_picker_frag, null, false);

        assert v != null;

        final EditText txtNumber = (EditText) v.findViewById(R.id.numberInput);

        if (getParentDiceFragment().mLastNumber > 0) {
            txtNumber.setText(String.valueOf(getParentDiceFragment().mLastNumber));
        }

        AlertDialogWrapper.Builder adb = new AlertDialogWrapper.Builder(getParentDiceFragment().mActivity);
        adb.setView(v);
        adb.setTitle(getResources().getString(R.string.dice_choose_sides));
        adb.setPositiveButton(getParentDiceFragment().mActivity.getResources().getString(R.string.dialog_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (txtNumber.getText() == null || txtNumber.getText().toString().isEmpty()) {
                            return;
                        }

                        int num;

                        try {
                            num = Integer.parseInt(txtNumber.getText().toString());
                        } catch (NumberFormatException e) {
                            ToastWrapper.makeText(getParentDiceFragment().mActivity, getResources().getString(R.string.dice_num_too_large),
                                    ToastWrapper.LENGTH_SHORT).show();
                            return;
                        }

                        if (num < 1) {
                            ToastWrapper.makeText(getParentDiceFragment().mActivity, getResources().getString(R.string.dice_postive),
                                    ToastWrapper.LENGTH_SHORT).show();
                        } else {
                            getParentDiceFragment().mLastNumber = num;
                            getParentDiceFragment().rollDie(num);
                        }

                        dismiss();
                    }
                }
        );
        return adb.create();
    }
}