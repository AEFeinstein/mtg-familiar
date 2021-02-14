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
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.helpers.LcPlayer;

import java.util.Locale;
import java.util.Objects;

/**
 * Class that creates dialogs for LcPlayer
 */
public class LcPlayerDialogFragment extends FamiliarDialogFragment {

    public static final String POSITION_KEY = "position";
    /* Dialog Constants */
    public final static int DIALOG_SET_NAME = 0;
    public final static int DIALOG_COMMANDER_DAMAGE = 1;
    public final static int DIALOG_CHANGE_LIFE = 2;
    private LcPlayer mLcPlayer;

    /**
     * @return The currently viewed LifeCounterFragment
     */
    @Nullable
    private LifeCounterFragment getParentLifeCounterFragment() {
        try {
            return (LifeCounterFragment) getParentFamiliarFragment();
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Set the LcPlayer object associated with this dialog
     *
     * @param player The LcPlayer for this dialog
     */
    public void setLcPlayer(LcPlayer player) {
        mLcPlayer = player;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = Objects.requireNonNull(getArguments()).getInt(ID_KEY);
        final int position = getArguments().getInt(POSITION_KEY);

        if (null == getParentLifeCounterFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case DIALOG_SET_NAME: {
                /* Inflate a view to type in the player's name, and show it in an AlertDialog */
                @SuppressLint("InflateParams") View textEntryView = Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(
                        R.layout.alert_dialog_text_entry, null, false);
                assert textEntryView != null;
                final EditText nameInput = textEntryView.findViewById(R.id.text_entry);
                nameInput.append(mLcPlayer.mName);
                textEntryView.findViewById(R.id.clear_button).setOnClickListener(view -> nameInput.setText(""));

                Dialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.life_counter_edit_name_dialog_title)
                        .customView(textEntryView, false)
                        .positiveText(R.string.dialog_ok)
                        .onPositive((dialog12, which) -> {
                            assert nameInput.getText() != null;
                            String newName = nameInput.getText().toString();
                            if (newName.equals("")) {
                                return;
                            }
                            mLcPlayer.mName = newName;
                            mLcPlayer.mNameTextView.setText(newName);
                            if (mLcPlayer.mCommanderNameTextView != null) {
                                mLcPlayer.mCommanderNameTextView.setText(newName);
                            }
                            getParentLifeCounterFragment().setCommanderInfo(-1);
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            case DIALOG_COMMANDER_DAMAGE: {
                /* inflate a view to add or subtract commander damage, and show it in an AlertDialog */
                @SuppressLint("InflateParams") View view = LayoutInflater.from(getActivity()).inflate(R.layout.life_counter_edh_dialog,
                        null, false);
                assert view != null;
                final TextView deltaText = view.findViewById(R.id.delta);
                final TextView absoluteText = view.findViewById(R.id.absolute);

                /* These are strange arrays of length one to have modifiable, yet final, variables */
                final int[] delta = {0};
                final int[] absolute = {mLcPlayer.mCommanderDamage.get(position).mLife};

                deltaText.setText(String.format(Locale.getDefault(), "+%d", delta[0]));
                absoluteText.setText(String.format(Locale.getDefault(), "%d", absolute[0]));

                view.findViewById(R.id.commander_plus1).setOnClickListener(v -> {
                    delta[0]++;
                    absolute[0]++;
                    deltaText.setText(String.format(Locale.getDefault(), "%s%d", ((delta[0] >= 0) ? "+" : ""), delta[0]));
                    absoluteText.setText(String.format(Locale.getDefault(), "%d", absolute[0]));
                });

                view.findViewById(R.id.commander_minus1).setOnClickListener(v -> {
                    delta[0]--;
                    absolute[0]--;
                    deltaText.setText(String.format(Locale.getDefault(), "%s%d", ((delta[0] >= 0) ? "+" : ""), delta[0]));
                    absoluteText.setText(String.format(Locale.getDefault(), "%d", absolute[0]));
                });

                MaterialDialog.Builder builder = new MaterialDialog.Builder(Objects.requireNonNull(this.getActivity()));
                builder.title(String.format(getResources().getString(R.string.life_counter_edh_dialog_title),
                        mLcPlayer.mCommanderDamage.get(position).mName))
                        .customView(view, false)
                        .negativeText(R.string.dialog_cancel)
                        .positiveText(R.string.dialog_ok)
                        .onPositive((dialog, which) -> {
                            mLcPlayer.mCommanderDamage.get(position).mLife = absolute[0];
                            mLcPlayer.mCommanderDamageAdapter.notifyDataSetChanged();
                            mLcPlayer.changeValue(-delta[0], true);
                        });

                return builder.build();
            }
            case DIALOG_CHANGE_LIFE: {
                /* Inflate a view to type in a new life, then show it in an AlertDialog */
                @SuppressLint("InflateParams") View textEntryView2 = Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(
                        R.layout.alert_dialog_text_entry, null, false);
                assert textEntryView2 != null;
                final EditText lifeInput = textEntryView2.findViewById(R.id.text_entry);
                lifeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                if (mLcPlayer.mReadoutTextView.getText() != null) {
                    lifeInput.append(mLcPlayer.mReadoutTextView.getText());
                }
                textEntryView2.findViewById(R.id.clear_button).setOnClickListener(view -> lifeInput.setText(""));

                String title;
                if (mLcPlayer.mMode == LifeCounterFragment.STAT_POISON) {
                    title = getResources().getString(R.string.life_counter_edit_poison_dialog_title);
                } else {
                    title = getResources().getString(R.string.life_counter_edit_life_dialog_title);
                }

                Dialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(title)
                        .customView(textEntryView2, false)
                        .positiveText(R.string.dialog_ok)
                        .onPositive((dialog1, which) -> {
                            assert lifeInput.getText() != null;
                            try {
                                /* make sure the life is valid, not empty */
                                int newLife = Integer.parseInt(lifeInput.getText().toString());
                                if (mLcPlayer.mMode == LifeCounterFragment.STAT_POISON) {
                                    mLcPlayer.changeValue(newLife - mLcPlayer.mPoison, true);
                                } else {
                                    mLcPlayer.changeValue(newLife - mLcPlayer.mLife, true);
                                }
                            } catch (NumberFormatException e) {
                                /* eat it */
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}