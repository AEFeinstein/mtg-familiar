package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * This is a superclass for all dialog fragments. It fixes some bugs and handles rotations nicely
 * <p/>
 * You must override onCreateDialog(); and then call
 * newFragment.show(getActivity().getSupportFragmentManager(), FamiliarActivity.DIALOG_TAG);
 */
public class FamiliarDialogFragment extends DialogFragment {

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
    protected Dialog DontShowDialog() {
        setShowsDialog(false);
        return null;
    }
}
