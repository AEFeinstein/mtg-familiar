package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import java.util.ArrayList;

public class WishlistFragment extends FamiliarFragment {

	/* Dialog constants */
	private static final int DIALOG_UPDATE_CARD = 1;
	private static final int DIALOG_PRICE_SETTING = 2;
	private static final int DIALOG_CONFIRMATION = 3;
	private static final int DIALOG_SHARE = 4;

	private static final int LOW_PRICE = 0;
	private static final int AVG_PRICE = 1;
	private static final int HIGH_PRICE = 2;

	/* Preferences */
	private int mPriceSetting;
	private boolean mShowCardInfo;
	private boolean mShowIndividualPrices;

	/* UI Elements */
	private AutoCompleteTextView mNameField;
	private EditText mNumberField;
	private TextView mTotalPriceField;
	private CheckBox mFoilCheckBox;

	/* The wishlist and adapter */
	private ArrayList<CompressedWishlistInfo> mCompressedWishlist;
	private WishlistArrayAdapter mWishlistAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View myFragmentView = inflater.inflate(R.layout.wishlist_frag, container, false);

		/* set the autocomplete for card names */
		assert myFragmentView != null;
		mNameField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.name_search);
		mNameField.setAdapter(new AutocompleteCursorAdapter(this.getActivity()));

		mNumberField = (EditText) myFragmentView.findViewById(R.id.number_input);
		mNumberField.setText("1");

		mTotalPriceField = (TextView) myFragmentView.findViewById(R.id.priceText);
		mFoilCheckBox = (CheckBox) myFragmentView.findViewById(R.id.wishlistFoil);
		ListView listView = (ListView) myFragmentView.findViewById(R.id.wishlist);

		myFragmentView.findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO actually add the card to the wishlist
				Toast.makeText(getActivity(), mNumberField.getText() + "x " + mNameField.getText(), Toast.LENGTH_LONG).show();
				mNumberField.setText("1");
				mNameField.setText("");
				mFoilCheckBox.setChecked(false);
			}
		});

		mCompressedWishlist = new ArrayList<CompressedWishlistInfo>();
		mWishlistAdapter = new WishlistArrayAdapter(mCompressedWishlist);
		listView.setAdapter(mWishlistAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				showDialog(DIALOG_UPDATE_CARD, mCompressedWishlist.get(position).mCard.name);
			}
		});
		return myFragmentView;
	}

	@Override
	public void onResume() {
		super.onResume();

		/* Get the relevant prices */
		mPriceSetting = Integer.parseInt(getFamiliarActivity().mPreferenceAdapter.getTradePrice());
		mShowIndividualPrices = getFamiliarActivity().mPreferenceAdapter.getShowIndividualWishlistPrices();
		boolean showTotalWishlistPrice = getFamiliarActivity().mPreferenceAdapter.getShowTotalWishlistPrice();
		mShowCardInfo = getFamiliarActivity().mPreferenceAdapter.getVerboseWishlist();

		/* Read the wishlist, populate set names */
		ArrayList<MtgCard> wishlist = WishlistHelpers.ReadWishlist(getActivity());
		try {
			CardDbAdapter adapter = new CardDbAdapter(getActivity());
			for (MtgCard card : wishlist) {
				card.tcgName = adapter.getTCGname(card.setCode);
			}
			adapter.close();
		} catch (FamiliarDbException e) {
			e.printStackTrace();
		}

		/* Compress the wishlist */
		for (MtgCard card : wishlist) {
			/* This works because both MtgCard's and CompressedWishlistInfo's .equals() can compare each other */
			if (!mCompressedWishlist.contains(card)) {
				mCompressedWishlist.add(new CompressedWishlistInfo(card));
			}
			else {
				mCompressedWishlist.get(mCompressedWishlist.indexOf(card)).add(card);
			}
		}

		/* Populate the rest of the card info, if desired
		if (mShowCardInfo) {
			// TODO a bunch of db calls :(
		}
		*/

		/* Show the total price, if desired */
		if (showTotalWishlistPrice) {
			mTotalPriceField.setVisibility(View.VISIBLE);
		}
		else {
			mTotalPriceField.setVisibility(View.GONE);
		}

		/* Show individual prices, if desired */
		if (mShowIndividualPrices) {
			for (CompressedWishlistInfo cwi : mCompressedWishlist) {
				for (int i = 0; i < cwi.mSetCodes.size(); i++) {
					loadPrice(cwi.mCard.name, cwi.mSetCodes.get(i), cwi.mNumber.get(i), -1);
				}
			}
		}

		/* Tell the adapter to redraw */
		mWishlistAdapter.notifyDataSetChanged();
	}

	private void mailWishlist(boolean includeTcgName) {
		// Use a more generic send text intent. It can also do emails
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.wishlist_share_title);
		sendIntent.putExtra(Intent.EXTRA_TEXT, WishlistHelpers.GetReadableWishlist(mCompressedWishlist, includeTcgName));
		sendIntent.setType("text/plain");

		try {
			startActivity(Intent.createChooser(sendIntent, getString(R.string.wishlist_chooser_title)));
		}
		catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(getActivity(), getString(R.string.error_no_email_client), Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * Override this to be notified when the wishlist changes
	 */
	@Override
	public void onWishlistChanged() {
		// TODO reload wishlist
		mWishlistAdapter.notifyDataSetChanged();
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
				showDialog(DIALOG_CONFIRMATION, null);
				return true;
			case R.id.wishlist_menu_settings:
				showDialog(DIALOG_PRICE_SETTING, null);
				return true;
			case R.id.wishlist_menu_share:
				showDialog(DIALOG_SHARE, null);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Remove any showing dialogs, and show the requested one
	 *
	 * @param id       the ID of the dialog to show
	 * @param cardName The name of the card to use if this is a dialog to change wishlist counts
	 */
	void showDialog(final int id, final String cardName) {
		/* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
		currently showing dialog, so make our own transaction and take care of that here. */

		/* If the fragment isn't visible (if desired being loaded by the pager), don't show dialogs */
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
							return WishlistHelpers.getDialog(cardName, WishlistFragment.this);
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
												getFamiliarActivity().mPreferenceAdapter.setTradePrice(
														String.valueOf(mPriceSetting));
												mWishlistAdapter.notifyDataSetChanged();
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
										WishlistHelpers.ResetCards(WishlistFragment.this.getActivity());
										mCompressedWishlist.clear();
										mWishlistAdapter.notifyDataSetChanged();
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

	/**
	 * This nested class is the adapter which populates the listView in the drawer menu. It handles both entries and
	 * headers
	 */
	public class WishlistArrayAdapter extends ArrayAdapter<CompressedWishlistInfo> {
		private final ArrayList<CompressedWishlistInfo> values;

		/**
		 * Constructor. The context will be used to inflate views later. The array of values will be used to populate
		 * the views
		 *
		 * @param values An array of DrawerEntries which will populate the list
		 */
		public WishlistArrayAdapter(ArrayList<CompressedWishlistInfo> values) {
			super(getActivity(), R.layout.drawer_list_item, values);
			this.values = values;
		}

		/**
		 * Called to get a view for an entry in the listView
		 *
		 * @param position    The position of the listView to populate
		 * @param convertView The old view to reuse, if possible. Since the layouts for entries and headers are
		 *                    different, this will be ignored
		 * @param parent      The parent this view will eventually be attached to
		 * @return The view for the data at this position
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			LinearLayout wishlistSets;
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(R.layout.result_list_card_row, parent, false);
				assert convertView != null;
				wishlistSets = ((LinearLayout) convertView.findViewById(R.id.wishlist_sets));
			}
			else {
				wishlistSets = ((LinearLayout) convertView.findViewById(R.id.wishlist_sets));
				wishlistSets.removeAllViews();
			}

			CompressedWishlistInfo info = values.get(position);

			((TextView) convertView.findViewById(R.id.cardname)).setText(info.mCard.name);

			/* Show or hide full card information */
			convertView.findViewById(R.id.cardset).setVisibility(View.GONE);
			if (mShowCardInfo) {
				/* TODO populate stuff */
				convertView.findViewById(R.id.cardcost).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardtype).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardability).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardp).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardslash).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardp).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardslash).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardt).setVisibility(View.VISIBLE);
			}
			else {
				convertView.findViewById(R.id.cardcost).setVisibility(View.GONE);
				convertView.findViewById(R.id.cardtype).setVisibility(View.GONE);
				convertView.findViewById(R.id.cardability).setVisibility(View.GONE);
				convertView.findViewById(R.id.cardp).setVisibility(View.GONE);
				convertView.findViewById(R.id.cardslash).setVisibility(View.GONE);
				convertView.findViewById(R.id.cardp).setVisibility(View.GONE);
				convertView.findViewById(R.id.cardslash).setVisibility(View.GONE);
				convertView.findViewById(R.id.cardt).setVisibility(View.GONE);
			}

			for (int i = 0; i < info.mSets.size(); i++) {

				View setRow = getActivity().getLayoutInflater().inflate(R.layout.wishlist_cardset_row, parent, false);
				assert setRow != null;

				/* Write the set name */
				((TextView) setRow.findViewById(R.id.wishlistRowSet)).setText(info.mSets.get(i));

				/* Show or hide the foil indicator */
				if (info.mIsFoil.get(i)) {
					setRow.findViewById(R.id.wishlistSetRowFoil).setVisibility(View.VISIBLE);
				}
				else {
					setRow.findViewById(R.id.wishlistSetRowFoil).setVisibility(View.GONE);
				}

				/* Show individual prices, or message if price does not exist, if desired */
				TextView priceText = ((TextView) setRow.findViewById(R.id.wishlistRowPrice));
				if (mShowIndividualPrices) {
					if (info.mIsFoil.get(i)) {
						if (info.mPrice.get(i).mFoilAverage != 0) {
							priceText.setText(info.mNumberOf.get(i) + "x $" + info.mPrice.get(i).mFoilAverage + "");
						}
						else {
							priceText.setText(info.mMessage.get(i));
						}
					}
					else {
						if (info.mPrice.get(i).mAverage != 0) {
							switch (mPriceSetting) {
								case LOW_PRICE:
									priceText.setText(info.mNumberOf.get(i) + "x $" + info.mPrice.get(i).mLow + "");
									break;
								case AVG_PRICE:
									priceText.setText(info.mNumberOf.get(i) + "x $" + info.mPrice.get(i).mAverage + "");
									break;
								case HIGH_PRICE:
									priceText.setText(info.mNumberOf.get(i) + "x $" + info.mPrice.get(i).mHigh + "");
									break;
							}
						}
						else {
							priceText.setText(info.mMessage.get(i));
						}
					}
				}
				else {
					priceText.setText("x" + info.mNumberOf.get(i));
				}

				/* Add the view to the linear layout */
				wishlistSets.addView(setRow);
			}
			return convertView;
		}
	}

	public void loadPrice(final String mCardName, final String mSetCode, String mCardNumber, int mMultiverseId) {
		PriceFetchRequest priceRequest;
		priceRequest = new PriceFetchRequest(mCardName, mSetCode, mCardNumber, mMultiverseId, getActivity());
		getFamiliarActivity().mSpiceManager.execute(priceRequest, mCardName + "-" +
				mSetCode, DurationInMillis.ONE_DAY, new RequestListener<PriceInfo>() {

			@Override
			public void onRequestFailure(SpiceException spiceException) {
				Toast.makeText(getActivity(), spiceException.getMessage(), Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onRequestSuccess(final PriceInfo result) {
				if (result != null) {
					/* Find the compressed wishlist info for this card */
					for (CompressedWishlistInfo cwi : mCompressedWishlist) {
						if (cwi.mCard.name.equals(mCardName)) {
							/* Find all foil and non foil compressed items with the same set code */
							for (int i = 0; i < cwi.mSetCodes.size(); i++) {
								if (cwi.mSetCodes.get(i).equals(mSetCode)) {
									/* Set the whole price info object */
									cwi.mPrice.set(i, result);
									mWishlistAdapter.notifyDataSetChanged();
								}
							}
						}
					}
					// TODO sum the total price
				}
				else {
					Toast.makeText(getActivity(), R.string.card_view_price_not_found, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
}
