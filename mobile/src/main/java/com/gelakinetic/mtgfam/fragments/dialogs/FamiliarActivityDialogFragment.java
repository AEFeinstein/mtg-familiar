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
 * along with MTG Familiar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.fragments.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.ComponentDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.FamiliarLogger;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

/**
 * Class that creates dialogs for FamiliarActivity
 */
public class FamiliarActivityDialogFragment extends FamiliarDialogFragment {

    /* Constants used for displaying dialogs */
    public static final int DIALOG_ABOUT = 100;
    public static final int DIALOG_CHANGE_LOG = 101;
    //    public static final int DIALOG_DONATE = 102;
    public static final int DIALOG_TTS = 103;
    public static final int DIALOG_LOGGING = 104;
    public static final int DIALOG_UPDATE = 105;
    public static final int DIALOG_UPDATE_RESULT = 106;
    public static final int DIALOG_UPDATE_PROMPT = 107;

    /**
     * Overridden to create the specific dialogs
     *
     * @param savedInstanceState The last saved instance state of the Fragment, or null if this is a freshly
     *                           created Fragment.
     * @return The new dialog instance to be displayed. All dialogs are created with the AlertDialog builder, so
     * onCreateView() does not need to be implemented
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            return DontShowDialog();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this.requireActivity());

        assert getActivity().getPackageManager() != null;

        mDialogId = requireArguments().getInt(ID_KEY);
        switch (mDialogId) {
            case DIALOG_ABOUT: {

                /* Set the title with the package version if possible */
                try {
                    builder.setTitle(getString(R.string.main_about) + " " + getString(R.string.app_name) + " " +
                            getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    builder.setTitle(getString(R.string.main_about) + " " + getString(R.string.app_name));
                }

                /* Set the neutral button */
                builder.setNeutralButton(R.string.dialog_thanks, (dialog, which) -> dialog.dismiss());

                /* Set the custom view, with some images below the text */
                LayoutInflater inflater = this.getActivity().getLayoutInflater();
                @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);
                assert dialogLayout != null;
                TextView text = dialogLayout.findViewById(R.id.aboutfield);
                text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_about_text)));
                text.setMovementMethod(LinkMovementMethod.getInstance());
                builder.setView(dialogLayout);

                return builder.create();
            }
            case DIALOG_CHANGE_LOG: {
                try {
                    builder.setTitle(getString(R.string.main_whats_new_in_title) + " " +
                            getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    builder.setTitle(R.string.main_whats_new_title);
                }

                builder.setNeutralButton(R.string.dialog_enjoy, (dialog, which) -> dialog.dismiss());

                /* Set the custom view, with some images below the text */
                LayoutInflater inflater = this.getActivity().getLayoutInflater();
                @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);
                assert dialogLayout != null;
                TextView text = dialogLayout.findViewById(R.id.aboutfield);
                text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_whats_new_text)));
                text.setMovementMethod(LinkMovementMethod.getInstance());

                dialogLayout.findViewById(R.id.imageview1).setVisibility(View.GONE);
                dialogLayout.findViewById(R.id.imageview2).setVisibility(View.GONE);
                builder.setView(dialogLayout);

                return builder.create();
            }
//            case DIALOG_DONATE: {
//                /* Set the title */
//                builder.setTitle(R.string.main_donate_dialog_title);
//                /* Set the buttons button */
//                builder.setNegativeButton(R.string.dialog_thanks_anyway);
//
//                builder.setPositiveButton(R.string.main_donate_title);
//                builder.onPositive((dialog, which) -> {
//                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(FamiliarActivity.PAYPAL_URL));
//                    startActivity(myIntent);
//                });
//
//                /* Set the custom view */
//                LayoutInflater inflater = this.getActivity().getLayoutInflater();
//                @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);
//
//                /* Set the text */
//                assert dialogLayout != null;
//                TextView text = dialogLayout.findViewById(R.id.aboutfield);
//                text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_donate_text)));
//                text.setMovementMethod(LinkMovementMethod.getInstance());
//
//                /* Set the image view */
//                ImageView payPal = dialogLayout.findViewById(R.id.imageview1);
//                payPal.setImageResource(R.drawable.paypal_icon);
//                payPal.setOnClickListener(v -> {
//                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
//                            .parse(FamiliarActivity.PAYPAL_URL));
//
//                    startActivity(myIntent);
//                });
//                dialogLayout.findViewById(R.id.imageview2).setVisibility(View.GONE);
//
//                builder.setView(dialogLayout, false);
//                return builder.create();
//            }
            case DIALOG_TTS: {
                /* Then display a dialog informing them of TTS */

                builder.setTitle(R.string.main_tts_warning_title)
                        .setMessage(R.string.main_tts_warning_text)
                        .setPositiveButton(R.string.main_install_tts, (dialog, which) -> {
                            /* TTS couldn't init, try installing TTS data */
                            try {
                                Intent installIntent = new Intent();
                                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                                startActivity(installIntent);
                            } catch (ActivityNotFoundException e) {
                                /* TTS not even installed */
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse("market://details?id=com.google.android.tts"));
                                startActivity(intent);
                            }
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss());
                return builder.create();
            }
            case DIALOG_LOGGING: {
                return FamiliarLogger.createDialog(getFamiliarActivity(), builder);
            }
            case DIALOG_UPDATE_PROMPT: {
                // Show a dialog prompting for a database update
                return builder.setTitle(R.string.pref_cat_updates)
                        .setMessage(R.string.database_update_available_message)
                        .setPositiveButton(R.string.dialog_yes, (dialog1, which) -> getFamiliarActivity().startDatabaseUpdate())
                        .setNegativeButton(R.string.dialog_no, (dialog1, which) -> {
                            // Do nothing
                        })
                        .create();
            }
            case DIALOG_UPDATE: {
                // Show a dialog for database updates
                ComponentDialog dialog = builder.setTitle(R.string.update_notification)
                        .setView(R.layout.activity_dialog_update)
                        .setCancelable(false)
                        .create();
                dialog.getOnBackPressedDispatcher().addCallback(requireActivity(),
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                // Consume backs
                            }
                        });
                dialog.setCanceledOnTouchOutside(false);
                return dialog;
            }
            case DIALOG_UPDATE_RESULT: {
                return builder.setTitle(R.string.update_notification)
                        .setMessage(requireArguments().getString(ID_ARG_STR))
                        .setPositiveButton(R.string.dialog_ok, (dialog, which) -> dialog.dismiss())
                        .create();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }

    /**
     * When the change log dismisses, check to see if we should bounce the drawer. It will open in 100ms, then
     * close in 1000ms
     *
     * @param dialog A DialogInterface for the dismissed dialog
     */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        try {
            final FamiliarActivity activity = getFamiliarActivity();
            if (mDialogId == DIALOG_CHANGE_LOG) {
                if (PreferenceAdapter.getBounceDrawer(getContext())) {
                    new Handler().postDelayed(() -> {
                        if (null != activity &&
                                null != activity.mDrawerLayout &&
                                null != activity.mDrawerList) {
                            activity.mDrawerLayout.openDrawer(activity.mDrawerList);
                            new Handler().postDelayed(() -> {
                                PreferenceAdapter.setBounceDrawer(activity);
                                if (null != activity.mDrawerLayout &&
                                        null != activity.mDrawerList) {
                                    activity.mDrawerLayout.closeDrawer(activity.mDrawerList);
                                }
                            }, 2000);
                        }
                    }, 500);
                }
            }
        } catch (NullPointerException e) {
            /* If there's no activity, ignore it */
        }
    }
}