package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;

import java.util.ArrayList;

/**
 * This is a superclass for all dialog fragments. It fixes some bugs and handles rotations nicely
 * <p/>
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
    FamiliarFragment getParentFamiliarFragment() {
        return (FamiliarFragment)
                getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container);
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
        String[] files = this.getActivity().fileList();
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
}
