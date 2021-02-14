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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import java.util.Objects;

/**
 * Class that creates dialogs for RoundTimerFragment
 */
public class RoundTimerDialogFragment extends FamiliarDialogFragment {

    private static final int DIALOG_SET_WARNINGS = 1;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        @SuppressLint("InflateParams") final View v = Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.round_timer_warning_dialog, null, false);
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
                .onPositive((dialog, which) -> {
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
                })
                .build();
    }
}