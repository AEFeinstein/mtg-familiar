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

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;

import java.util.ArrayList;
import java.util.Objects;

/**
 * This is a superclass for all dialog fragments. It fixes some bugs and handles rotations nicely
 * <p>
 * You must override onCreateDialog(); and then call
 * newFragment.show(getActivity().getSupportFragmentManager(), FamiliarActivity.DIALOG_TAG);
 */
public class FamiliarDialogFragment extends DialogFragment {

    public static final String ID_KEY = "DIALOG_ID";
    int mDialogId;

    /**
     * All subclasses of Fragment must include a public empty constructor.
     * The framework will often re-instantiate a fragment class when needed,
     * in particular during state restore, and needs to be able to find this constructor
     * to instantiate it. If the empty constructor is not available, a runtime exception
     * will occur in some cases during state restore.
     */
    public FamiliarDialogFragment() {
    }

    /**
     * Display the dialog, adding the fragment to the given FragmentManager. This is a convenience
     * for explicitly creating a transaction, adding the fragment to it with the given tag, and
     * committing it. This does not add the transaction to the back stack. When the fragment is
     * dismissed, a new transaction will be executed to remove it from the activity.
     *
     * @param manager The FragmentManager this fragment will be added to.
     * @param tag     The tag for this fragment, as per FragmentTransaction.add.
     */
    @Override
    public void show(FragmentManager manager, String tag) {
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    /**
     * first saving my state, so the bundle wont be empty.
     * http://code.google.com/p/android/issues/detail?id=19917
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (outState.isEmpty()) {
            outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        }
        super.onSaveInstanceState(outState);
        FamiliarActivity.logBundleSize("OSSI " + this.getClass().getName(), outState);
    }

    /**
     * Default constructor, except we always retain the instance state, as long as it isn't nested
     * (fragment isn't destroyed/created across activity recreation)
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() == null) {
            this.setRetainInstance(true);
        }
    }

    /**
     * Fixes a bug on rotation
     * http://stackoverflow.com/questions/8235080/fragments-dialogFragment-and-screen-rotation
     */
    @Override
    public void onDestroyView() {
        if (getDialog() != null) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    /**
     * Lint complains if onCreateDialog returns null, but it's fine if the dialog won't be shown
     *
     * @return null, since the dialog won't be shown
     */
    @SuppressWarnings("SameReturnValue")
    Dialog DontShowDialog() {
        setShowsDialog(false);
        return null;
    }

    /**
     * @return The current fragment being displayed by the app
     */
    @Nullable
    private Fragment getDialogParentFragment() {
        return Objects.requireNonNull(getActivity()).getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    /**
     * @return The current fragment being displayed by the app
     */
    @Nullable
    FamiliarFragment getParentFamiliarFragment() {
        try {
            return (FamiliarFragment) getDialogParentFragment();
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * @return The current FamiliarActivity
     */
    FamiliarActivity getFamiliarActivity() {
        return (FamiliarActivity) getActivity();
    }

    /**
     * Gets all files of the given extension
     *
     * @param fileExtension kind of files to get
     * @return array of string file names, without the extension
     */
    String[] getFiles(String fileExtension) {
        String[] files = Objects.requireNonNull(this.getActivity()).fileList();
        ArrayList<String> validFiles = new ArrayList<>();
        for (String fileName : files) {
            if (fileName.endsWith(fileExtension)) {
                validFiles.add(fileName.substring(0, fileName.indexOf(fileExtension)));
            }
        }
        if (validFiles.size() == 0) {
            return new String[]{};
        }
        final String[] tradeNames = new String[validFiles.size()];
        validFiles.toArray(tradeNames);
        return tradeNames;
    }

    /**
     * @return true if the dialog should be created, false otherwise
     */
    boolean canCreateDialog() {
        return (null != getDialogParentFragment()) &&
                (!Objects.requireNonNull(getDialogParentFragment().getActivity()).isFinishing());
    }

    /**
     * Override setArguments to also log the size of the arguments being set
     *
     * @param args Arguments to set
     */
    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        FamiliarActivity.logBundleSize("SA " + this.getClass().getName(), args);
    }
}
