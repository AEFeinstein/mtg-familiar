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
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import org.jetbrains.annotations.NotNull;

/**
 * Class that creates dialogs for FamiliarActivity
 */
public class FamiliarActivityDialogFragment extends FamiliarDialogFragment {

    /* Constants used for displaying dialogs */
    public static final int DIALOG_ABOUT = 100;
    public static final int DIALOG_CHANGE_LOG = 101;
    public static final int DIALOG_DONATE = 102;
    public static final int DIALOG_TTS = 103;

    /**
     * Overridden to create the specific dialogs
     *
     * @param savedInstanceState The last saved instance state of the Fragment, or null if this is a freshly
     *                           created Fragment.
     * @return The new dialog instance to be displayed. All dialogs are created with the AlertDialog builder, so
     * onCreateView() does not need to be implemented
     */
    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this.getActivity());

        assert getActivity().getPackageManager() != null;

        mDialogId = getArguments().getInt(ID_KEY);
        switch (mDialogId) {
            case DIALOG_ABOUT: {

                /* Set the title with the package version if possible */
                try {
                    builder.title(getString(R.string.main_about) + " " + getString(R.string.app_name) + " " +
                            getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    builder.title(getString(R.string.main_about) + " " + getString(R.string.app_name));
                }

                /* Set the neutral button */
                builder.neutralText(R.string.dialog_thanks);

                /* Set the custom view, with some images below the text */
                LayoutInflater inflater = this.getActivity().getLayoutInflater();
                @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);
                assert dialogLayout != null;
                TextView text = dialogLayout.findViewById(R.id.aboutfield);
                text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_about_text)));
                text.setMovementMethod(LinkMovementMethod.getInstance());
                builder.customView(dialogLayout, false);

                return builder.build();
            }
            case DIALOG_CHANGE_LOG: {
                try {
                    builder.title(getString(R.string.main_whats_new_in_title) + " " +
                            getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    builder.title(R.string.main_whats_new_title);
                }

                builder.neutralText(R.string.dialog_enjoy);

                /* Set the custom view, with some images below the text */
                LayoutInflater inflater = this.getActivity().getLayoutInflater();
                @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);
                assert dialogLayout != null;
                TextView text = dialogLayout.findViewById(R.id.aboutfield);
                text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_whats_new_text)));
                text.setMovementMethod(LinkMovementMethod.getInstance());

                dialogLayout.findViewById(R.id.imageview1).setVisibility(View.GONE);
                dialogLayout.findViewById(R.id.imageview2).setVisibility(View.GONE);
                builder.customView(dialogLayout, false);

                return builder.build();
            }
            case DIALOG_DONATE: {
                /* Set the title */
                builder.title(R.string.main_donate_dialog_title);
                /* Set the buttons button */
                builder.negativeText(R.string.dialog_thanks_anyway);

                builder.positiveText(R.string.main_donate_title);
                builder.onPositive((dialog, which) -> {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(FamiliarActivity.PAYPAL_URL));
                    startActivity(myIntent);
                });

                /* Set the custom view */
                LayoutInflater inflater = this.getActivity().getLayoutInflater();
                @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);

                /* Set the text */
                assert dialogLayout != null;
                TextView text = dialogLayout.findViewById(R.id.aboutfield);
                text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_donate_text)));
                text.setMovementMethod(LinkMovementMethod.getInstance());

                /* Set the image view */
                ImageView payPal = dialogLayout.findViewById(R.id.imageview1);
                payPal.setImageResource(R.drawable.paypal_icon);
                payPal.setOnClickListener(v -> {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
                            .parse(FamiliarActivity.PAYPAL_URL));

                    startActivity(myIntent);
                });
                dialogLayout.findViewById(R.id.imageview2).setVisibility(View.GONE);

                builder.customView(dialogLayout, false);
                return builder.build();
            }
            case DIALOG_TTS: {
                /* Then display a dialog informing them of TTS */

                builder.title(R.string.main_tts_warning_title)
                        .content(R.string.main_tts_warning_text)
                        .positiveText(R.string.main_install_tts)
                        .onPositive((dialog, which) -> {
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
                        .negativeText(R.string.dialog_cancel);
                return builder.build();
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
    public void onDismiss(DialogInterface dialog) {
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
                                if (null != activity) {
                                    PreferenceAdapter.setBounceDrawer(activity);
                                    if (null != activity.mDrawerLayout &&
                                            null != activity.mDrawerList) {
                                        activity.mDrawerLayout.closeDrawer(activity.mDrawerList);
                                    }
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