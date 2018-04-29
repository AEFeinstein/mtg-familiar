/*
 * Copyright 2014 Devin Collins
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

package com.gelakinetic.mtgfam.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.ProfileDialogFragment;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

/**
 * This fragment contains a players profile information such as their DCI number and anything else
 * we can think of to go here
 */
public class ProfileFragment extends FamiliarFragment {

    /* UI Elements */
    private TextView mBarcodeTextView;
    private TextView mDCINumberTextView;
    private TextView mNoDCINumberTextView;

    /* String variables */
    public String mDCINumber;

    /**
     * Initialize the view and set up the button actions
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The inflated view
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View myFragmentView = inflater.inflate(R.layout.profile_frag, container, false);

        assert myFragmentView != null;
        mBarcodeTextView = myFragmentView.findViewById(R.id.barcode);
        mDCINumberTextView = myFragmentView.findViewById(R.id.dci_number);
        mNoDCINumberTextView = myFragmentView.findViewById(R.id.no_dci_number);

        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "free3of9.ttf");
        mBarcodeTextView.setTypeface(tf);

        mDCINumber = PreferenceAdapter.getDCINumber(getContext());

        checkDCINumber();

        return myFragmentView;
    }

    /**
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.profile_menu, menu);
    }

    /**
     * Handle an ActionBar item click
     *
     * @param item the item clicked
     * @return true if the click was acted on
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.profile_menu_update_dci:
                showDialog();
                return true;
            case R.id.profile_menu_remove_dci:
                PreferenceAdapter.setDCINumber(getContext(), "");
                mDCINumber = "";
                checkDCINumber();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Set up menu buttons depending on whether a DCI number has been entered
     *
     * @param menu The options menu in which you place your items.
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem updateDCI = menu.findItem(R.id.profile_menu_update_dci);
        MenuItem removeDCI = menu.findItem(R.id.profile_menu_remove_dci);
        assert updateDCI != null;
        assert removeDCI != null;

        if (getFamiliarActivity() == null || !getFamiliarActivity().mIsMenuVisible) {
            updateDCI.setVisible(false);
            removeDCI.setVisible(false);
        } else if (mDCINumber != null && !mDCINumber.isEmpty()) {
            updateDCI.setVisible(false);
            removeDCI.setVisible(true);
        } else {
            updateDCI.setVisible(true);
            removeDCI.setVisible(false);
        }
    }

    private void showDialog() throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        /* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        /* Create and show the dialog. */
        ProfileDialogFragment newFragment = new ProfileDialogFragment();
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    public void checkDCINumber() {
        if (mDCINumber.isEmpty()) {
            hideDCINumber();
        } else {
            showDCINumber();
        }
        getActivity().invalidateOptionsMenu();
    }

    private void hideDCINumber() {
        mDCINumberTextView.setVisibility(View.GONE);
        mBarcodeTextView.setVisibility(View.GONE);
        mNoDCINumberTextView.setText(R.string.profile_no_dci);
        mNoDCINumberTextView.setClickable(false);
    }

    private void showDCINumber() {
        mDCINumberTextView.setText(mDCINumber);
        mBarcodeTextView.setText(mDCINumber);
        mDCINumberTextView.setVisibility(View.VISIBLE);
        mBarcodeTextView.setVisibility(View.VISIBLE);

        mNoDCINumberTextView.setText(Html.fromHtml("<a href=\"http://www.wizards.com/Magic/PlaneswalkerPoints/" + mDCINumber + "\">" + getString(R.string.profile_planeswalker_points) + "</a>"));
        mNoDCINumberTextView.setClickable(true);
        mNoDCINumberTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
