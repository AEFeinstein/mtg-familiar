package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.gelakinetic.mtgfam.R;

import org.jetbrains.annotations.NotNull;

/**
 * Class that creates dialogs for RoundTimerFragment
 */
public class RoundTimerDialogFragment extends FamiliarDialogFragment {

    private static final int DIALOG_SET_WARNINGS = 1;

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

                /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        switch (DIALOG_SET_WARNINGS) {
            case DIALOG_SET_WARNINGS: {
                final View v = View.inflate(this.getActivity(), R.layout.round_timer_warning_dialog, null);
                final CheckBox chkFifteen = (CheckBox) v.findViewById(R.id.timer_pref_fifteen);
                final CheckBox chkTen = (CheckBox) v.findViewById(R.id.timer_pref_ten);
                final CheckBox chkFive = (CheckBox) v.findViewById(R.id.timer_pref_five);

                boolean fifteen =
                        getFamiliarActivity().mPreferenceAdapter.getFifteenMinutePref();
                boolean ten = getFamiliarActivity().mPreferenceAdapter.getTenMinutePref();
                boolean five = getFamiliarActivity().mPreferenceAdapter.getFiveMinutePref();

                chkFifteen.setChecked(fifteen);
                chkTen.setChecked(ten);
                chkFive.setChecked(five);

                return new AlertDialogWrapper.Builder(getActivity())
                        .setView(v).setTitle(R.string.timer_warning_dialog_title)
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                getFamiliarActivity().mPreferenceAdapter
                                        .setFifteenMinutePref(chkFifteen.isChecked());
                                getFamiliarActivity().mPreferenceAdapter
                                        .setTenMinutePref(chkTen.isChecked());
                                getFamiliarActivity().mPreferenceAdapter
                                        .setFiveMinutePref(chkFive.isChecked());
                            }
                        })
                        .create();
            }
            default: {
                return DontShowDialog();
            }
        }
    }
}