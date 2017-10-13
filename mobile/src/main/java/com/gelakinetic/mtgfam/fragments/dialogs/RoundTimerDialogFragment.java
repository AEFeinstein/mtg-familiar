package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

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
                final CheckBox chkFifteen = v.findViewById(R.id.timer_pref_fifteen);
                final CheckBox chkTen = v.findViewById(R.id.timer_pref_ten);
                final CheckBox chkFive = v.findViewById(R.id.timer_pref_five);
                final CheckBox chkTwo = v.findViewById(R.id.timer_pref_two);
                final CheckBox chkUseSound = v.findViewById(R.id.timer_use_sound_instead_of_tts);

                boolean fifteen = PreferenceAdapter.getFifteenMinutePref(getContext());
                boolean ten = PreferenceAdapter.getTenMinutePref(getContext());
                boolean five = PreferenceAdapter.getFiveMinutePref(getContext());
                boolean two = PreferenceAdapter.getTwoMinutePref(getContext());
                boolean useSound = PreferenceAdapter.getUseSoundInsteadOfTTSPref(getContext());

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
                                PreferenceAdapter
                                        .setFifteenMinutePref(getContext(), chkFifteen.isChecked());
                                PreferenceAdapter
                                        .setTenMinutePref(getContext(), chkTen.isChecked());
                                PreferenceAdapter
                                        .setFiveMinutePref(getContext(), chkFive.isChecked());
                                PreferenceAdapter
                                        .setTwoMinutePref(getContext(), chkTwo.isChecked());
                                PreferenceAdapter
                                        .setUseSoundInsteadOfTTSPref(getContext(), chkUseSound.isChecked());
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