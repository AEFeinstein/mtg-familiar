package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.helpers.CardHelpers;

/**
 * Class that creates dialogs for ResultListFragment
 */
public class ResultListDialogFragment extends FamiliarDialogFragment {

    public static final int DIALOG_SORT = 0;
    public static final int WISH_LIST_COUNTS = 1;
    public static final String NAME_KEY = "cardname";

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = getArguments().getInt(ID_KEY);
        switch (mDialogId) {
            case WISH_LIST_COUNTS: {
                Dialog dialog = CardHelpers.getDialog(getArguments().getString(NAME_KEY), getResultListFragment(), false, false);
                if (dialog == null) {
                    getResultListFragment().handleFamiliarDbException(false);
                    return DontShowDialog();
                }
                return dialog;
            }
            default: {
                return DontShowDialog();
            }
        }
    }

    private ResultListFragment getResultListFragment() {
        return (ResultListFragment) getFamiliarFragment();
    }
}
