package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.helpers.LcPlayer;

import org.jetbrains.annotations.NotNull;

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
        return (LifeCounterFragment) getParentFamiliarFragment();
    }

    /**
     * Set the LcPlayer object associated with this dialog
     *
     * @param player The LcPlayer for this dialog
     */
    public void setLcPlayer(LcPlayer player) {
        mLcPlayer = player;
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
                /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = getArguments().getInt(ID_KEY);
        final int position = getArguments().getInt(POSITION_KEY);

        if (null == getParentLifeCounterFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case DIALOG_SET_NAME: {
                        /* Inflate a view to type in the player's name, and show it in an AlertDialog */
                View textEntryView = getActivity().getLayoutInflater().inflate(
                        R.layout.alert_dialog_text_entry, null, false);
                assert textEntryView != null;
                final EditText nameInput = textEntryView.findViewById(R.id.text_entry);
                nameInput.append(mLcPlayer.mName);
                textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nameInput.setText("");
                    }
                });

                Dialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.life_counter_edit_name_dialog_title)
                        .customView(textEntryView, false)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
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
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            case DIALOG_COMMANDER_DAMAGE: {
                        /* inflate a view to add or subtract commander damage, and show it in an AlertDialog */
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.life_counter_edh_dialog,
                        null, false);
                assert view != null;
                final TextView deltaText = view.findViewById(R.id.delta);
                final TextView absoluteText = view.findViewById(R.id.absolute);

                        /* These are strange arrays of length one to have modifiable, yet final, variables */
                final int[] delta = {0};
                final int[] absolute = {mLcPlayer.mCommanderDamage.get(position).mLife};

                deltaText.setText(((delta[0] >= 0) ? "+" : "") + delta[0]);
                absoluteText.setText("" + absolute[0]);

                view.findViewById(R.id.commander_plus1).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        delta[0]++;
                        absolute[0]++;
                        deltaText.setText(((delta[0] >= 0) ? "+" : "") + delta[0]);
                        absoluteText.setText("" + absolute[0]);
                    }
                });

                view.findViewById(R.id.commander_minus1).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        delta[0]--;
                        absolute[0]--;
                        deltaText.setText(((delta[0] >= 0) ? "+" : "") + delta[0]);
                        absoluteText.setText("" + absolute[0]);
                    }
                });

                MaterialDialog.Builder builder = new MaterialDialog.Builder(this.getActivity());
                builder.title(String.format(getResources().getString(R.string.life_counter_edh_dialog_title),
                        mLcPlayer.mCommanderDamage.get(position).mName))
                        .customView(view, false)
                        .negativeText(R.string.dialog_cancel)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                mLcPlayer.mCommanderDamage.get(position).mLife = absolute[0];
                                mLcPlayer.mCommanderDamageAdapter.notifyDataSetChanged();
                                mLcPlayer.changeValue(-delta[0], true);
                            }
                        });

                return builder.build();
            }
            case DIALOG_CHANGE_LIFE: {
                        /* Inflate a view to type in a new life, then show it in an AlertDialog */
                View textEntryView2 = getActivity().getLayoutInflater().inflate(
                        R.layout.alert_dialog_text_entry, null, false);
                assert textEntryView2 != null;
                final EditText lifeInput = textEntryView2.findViewById(R.id.text_entry);
                lifeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                if (mLcPlayer.mReadoutTextView.getText() != null) {
                    lifeInput.append(mLcPlayer.mReadoutTextView.getText());
                }
                textEntryView2.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        lifeInput.setText("");
                    }
                });

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
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
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
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}