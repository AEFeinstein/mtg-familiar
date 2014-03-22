package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;

public class WishlistFragment extends FamiliarFragment {

	/* Dialog constants */
	private static final int DIALOG_UPDATE_CARD = 1;
	private static final int DIALOG_PRICE_SETTING = 2;
	private static final int DIALOG_CONFIRMATION = 3;
	private static final int DIALOG_SHARE = 4;

	/* State variables */
	private int mPriceSetting;

	/* UI Elements */
	private AutoCompleteTextView mNameField;
	private EditText mNumberField;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View myFragmentView = inflater.inflate(R.layout.wishlist_frag, container, false);

		/* set the autocomplete for card names */
		assert myFragmentView != null;
		mNameField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.name_search);
		mNameField.setAdapter(new AutocompleteCursorAdapter(this.getActivity()));

		mNumberField = (EditText)myFragmentView.findViewById(R.id.number_input);
		mNumberField.setText("1");

		myFragmentView.findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(getActivity(), mNumberField.getText() + "x " + mNameField.getText(),Toast.LENGTH_LONG).show();
				mNumberField.setText("1");
				mNameField.setText("");
			}
		});
		return myFragmentView;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		mPriceSetting = Integer.parseInt(((FamiliarActivity) getActivity()).mPreferenceAdapter.getTradePrice());
		super.onResume();
	}

	private void mailWishlist(boolean includeTcgName) {
		// TODO mail the wishlist
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
				showDialog(DIALOG_CONFIRMATION);
				return true;
			case R.id.wishlist_menu_settings:
				showDialog(DIALOG_PRICE_SETTING);
				return true;
			case R.id.wishlist_menu_share:
				showDialog(DIALOG_SHARE);
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
					case DIALOG_UPDATE_CARD: {
						try {
							return WishlistHelpers.getDialog(null, WishlistFragment.this);
						} catch (FamiliarDbException e) {
							handleFamiliarDbException(false);
							setShowsDialog(false);
							return null;
						}
					}
					case DIALOG_PRICE_SETTING: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

						builder
								.setTitle(R.string.trader_pricing_dialog_title)
								.setSingleChoiceItems(new String[]{getString(R.string.trader_Low),
												getString(R.string.trader_Average), getString(R.string.trader_High)},
										mPriceSetting,
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
												mPriceSetting = which;
												((FamiliarActivity) getActivity()).mPreferenceAdapter.setTradePrice(
														String.valueOf(mPriceSetting));
												// TODO refresh prices
												dialog.dismiss();
											}
										}
								);
						return builder.create();

					}
					case DIALOG_CONFIRMATION: {
						return new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.wishlist_empty_dialog_title)
								.setMessage(R.string.wishlist_empty_dialog_text)
								.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										// TODO clear wishlist
										dialog.dismiss();
									}
								})
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								})
								.setCancelable(true).create();

					}
					case DIALOG_SHARE: {
						return new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.wishlist_share)
								.setMessage(R.string.wishlist_share_include_set)
								.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										mailWishlist(true);
										dialog.dismiss();
									}
								})
								.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										mailWishlist(false);
										dialog.dismiss();
									}
								})
								.setCancelable(true).create();
					}
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
