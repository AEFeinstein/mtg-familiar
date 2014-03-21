package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

public class WishlistFragment extends FamiliarFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View myFragmentView = inflater.inflate(R.layout.wishlist_frag, container, false);
		return myFragmentView;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	private void mailWishlist(boolean includeTcgName) {
		// TODO
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.wishlist_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.wishlist_menu_clear:
				return true;
			case R.id.wishlist_menu_settings:
				return true;
			case R.id.wishlist_menu_share:
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Remove any showing dialogs, and show the requested one
	 *
	 * @param id the ID of the dialog to show
	 */
	void showDialog(final int id) {
		/* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
		currently showing dialog, so make our own transaction and take care of that here. */

		/* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
		if (!this.isVisible()) {
			return;
		}

		removeDialog(getFragmentManager());

		/* Create and show the dialog. */
		final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				super.onDismiss(dialog);
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				setShowsDialog(true);
				switch (id) {
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
	}
}
