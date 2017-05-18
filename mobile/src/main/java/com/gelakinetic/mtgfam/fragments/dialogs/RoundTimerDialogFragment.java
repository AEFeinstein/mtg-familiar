package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
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
                final CheckBox chkTwo = (CheckBox) v.findViewById(R.id.timer_pref_two);
                final CheckBox chkUseSound = (CheckBox) v.findViewById(R.id.timer_use_sound_instead_of_tts);

                boolean fifteen =
                        getFamiliarActivity().mPreferenceAdapter.getFifteenMinutePref();
                boolean ten = getFamiliarActivity().mPreferenceAdapter.getTenMinutePref();
                boolean five = getFamiliarActivity().mPreferenceAdapter.getFiveMinutePref();
                boolean two = getFamiliarActivity().mPreferenceAdapter.getTwoMinutePref();
                boolean useSound = getFamiliarActivity().mPreferenceAdapter.getUseSoundInsteadOfTTSPref();

                chkFifteen.setChecked(fifteen);
                chkTen.setChecked(ten);
                chkFive.setChecked(five);
                chkTwo.setChecked(two);
                chkUseSound.setChecked(useSound);

                return new MaterialDialog.Builder(getActivity())
                        .customView(v, false)
                        .title(R.string.timer_warning_dialog_title)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                getFamiliarActivity().mPreferenceAdapter
                                        .setFifteenMinutePref(chkFifteen.isChecked());
                                getFamiliarActivity().mPreferenceAdapter
                                        .setTenMinutePref(chkTen.isChecked());
                                getFamiliarActivity().mPreferenceAdapter
                                        .setFiveMinutePref(chkFive.isChecked());
                                getFamiliarActivity().mPreferenceAdapter
                                        .setTwoMinutePref(chkTwo.isChecked());
                                getFamiliarActivity().mPreferenceAdapter
                                        .setUseSoundInsteadOfTTSPref(chkUseSound.isChecked());
                            }
                        })
                        .build();
            }
            default: {
                return DontShowDialog();
            }
        }
    }
}