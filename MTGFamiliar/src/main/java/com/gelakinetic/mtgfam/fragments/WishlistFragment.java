package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
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
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
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
	private boolean mShowTotalWishlistPrice;
	private View mTotalPriceDivider;

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
		mTotalPriceDivider = myFragmentView.findViewById(R.id.divider_total_price);
		mFoilCheckBox = (CheckBox) myFragmentView.findViewById(R.id.wishlistFoil);
		ListView listView = (ListView) myFragmentView.findViewById(R.id.wishlist);

		myFragmentView.findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				try {
					CardDbAdapter adapter = new CardDbAdapter(getActivity());
					/* Make the new card */
					MtgCard card = new MtgCard();
					card.name = String.valueOf(mNameField.getText());
					card.foil = mFoilCheckBox.isChecked();
					card.numberOf = Integer.parseInt(String.valueOf(mNumberField.getText()));
					card.message = getString(R.string.wishlist_loading);

					/* Get some extra information */
					Cursor cardCursor = adapter.fetchCardByName(card.name, CardDbAdapter.allData);

					card.type = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_TYPE));
					card.rarity = (char) cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_RARITY));
					card.manaCost = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_MANACOST));
					card.power = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_POWER));
					card.toughness = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
					card.loyalty = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
					card.ability = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_ABILITY));
					card.flavor = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
					card.number = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
					card.setCode = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
					card.tcgName = adapter.getTCGname(card.setCode);

					/* Clean up */
					cardCursor.close();
					adapter.close();

					/* Add it to the wishlist */
					if (mCompressedWishlist.contains(card)) {
						CompressedWishlistInfo cwi = mCompressedWishlist.get(mCompressedWishlist.indexOf(card));
						boolean added = false;
						for (int i = 0; i < cwi.mSetCodes.size(); i++) {
							if (cwi.mSetCodes.get(i).equals(card.setCode) && cwi.mIsFoil.get(i).equals(card.foil)) {
								added = true;
								cwi.mNumberOf.set(i, cwi.mNumberOf.get(i) + 1);
							}
						}
						if (!added) {
							cwi.add(card);
						}
					}
					else {
						mCompressedWishlist.add(new CompressedWishlistInfo(card));
					}

					loadPrice(card.name, card.setCode, card.number, -1);

					/* Write the wishlist */
					WishlistHelpers.WriteCompressedWishlist(getActivity(), mCompressedWishlist);

					/* Clean up */
					mNumberField.setText("1");
					mNameField.setText("");
					mFoilCheckBox.setChecked(false);

					mWishlistAdapter.notifyDataSetChanged();

				} catch (FamiliarDbException e) {
					handleFamiliarDbException(false);
				}
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

		if(!isAdded()) {
			return;
		}

		mCompressedWishlist.clear();

		/* Get the relevant prices */
		mPriceSetting = Integer.parseInt(getFamiliarActivity().mPreferenceAdapter.getTradePrice());
		mShowIndividualPrices = getFamiliarActivity().mPreferenceAdapter.getShowIndividualWishlistPrices();
		mShowTotalWishlistPrice = getFamiliarActivity().mPreferenceAdapter.getShowTotalWishlistPrice();
		mShowCardInfo = getFamiliarActivity().mPreferenceAdapter.getVerboseWishlist();

		/* Read the wishlist, populate set names */
		readAndCompressWishlist(null);

		/* Show the total price, if desired */
		if (mShowTotalWishlistPrice) {
			mTotalPriceField.setVisibility(View.VISIBLE);
			mTotalPriceDivider.setVisibility(View.VISIBLE);
		}
		else {
			mTotalPriceField.setVisibility(View.GONE);
			mTotalPriceDivider.setVisibility(View.GONE);
		}

		/* Load prices, if desired */
		if (mShowIndividualPrices || mShowTotalWishlistPrice) {
			for (CompressedWishlistInfo cwi : mCompressedWishlist) {
				for (int i = 0; i < cwi.mSetCodes.size(); i++) {
					loadPrice(cwi.mCard.name, cwi.mSetCodes.get(i), cwi.mNumber.get(i), -1);
				}
			}
		}

		/* Tell the adapter to redraw */
		mWishlistAdapter.notifyDataSetChanged();
	}

	private void readAndCompressWishlist(String changedCardName) {
		ArrayList<MtgCard> wishlist = WishlistHelpers.ReadWishlist(getActivity());
		try {
			CardDbAdapter adapter = new CardDbAdapter(getActivity());

			for (MtgCard card : wishlist) {
				card.tcgName = adapter.getTCGname(card.setCode);
			}

			/* Clear the wishlist, or just the card that changed */
			if (changedCardName == null) {
				mCompressedWishlist.clear();
			}
			else {
				for (CompressedWishlistInfo cwi : mCompressedWishlist) {
					if (cwi.mCard.name.equals(changedCardName)) {
						cwi.clearCompressedInfo();
					}
				}
			}

			/* Compress the whole wishlist, or just the card that changed */
			for (MtgCard card : wishlist) {
				if (changedCardName == null || changedCardName.equals(card.name)) {
					/* This works because both MtgCard's and CompressedWishlistInfo's .equals() can compare each other */
					if (!mCompressedWishlist.contains(card)) {
						mCompressedWishlist.add(new CompressedWishlistInfo(card));
					}
					else {
						mCompressedWishlist.get(mCompressedWishlist.indexOf(card)).add(card);
					}
					/* If we are changing a card, look up the new price */
					if (changedCardName != null) {
						loadPrice(card.name, card.setCode, card.number, -1);
					}
				}
			}

			/* Check for wholly removed cards if one card was modified */
			if (changedCardName != null) {
				for (int i = 0; i < mCompressedWishlist.size(); i++) {
					if (mCompressedWishlist.get(i).mSetCodes.size() == 0) {
						mCompressedWishlist.remove(i);
						i--;
					}
				}
			}

			adapter.fillExtraWishlistData(mCompressedWishlist, CardDbAdapter.allData);

			adapter.close();
		} catch (FamiliarDbException e) {
			handleFamiliarDbException(false);
		}

	}

	private void mailWishlist(boolean includeTcgName) {
		// Use a more generic send text intent. It can also do emails
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.wishlist_share_title);
		sendIntent.putExtra(Intent.EXTRA_TEXT, WishlistHelpers.GetSharableWishlist(mCompressedWishlist, includeTcgName));
		sendIntent.setType("text/plain");

		try {
			startActivity(Intent.createChooser(sendIntent, getString(R.string.wishlist_chooser_title)));
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(getActivity(), getString(R.string.error_no_email_client), Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * Override this to be notified when the wishlist changes
	 */
	@Override
	public void onWishlistChanged(String cardName) {
		readAndCompressWishlist(cardName);
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
							return WishlistHelpers.getDialog(cardName, WishlistFragment.this, true);
						} catch (FamiliarDbException e) {
							handleFamiliarDbException(false);
							setShowsDialog(false);
							return null;
						}
					}
					case DIALOG_PRICE_SETTING: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

						builder.setTitle(R.string.trader_pricing_dialog_title)
								.setSingleChoiceItems(new String[]{getString(R.string.trader_Low),
												getString(R.string.trader_Average), getString(R.string.trader_High)},
										mPriceSetting,
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
												if (mPriceSetting != which) {
													mPriceSetting = which;
													getFamiliarActivity().mPreferenceAdapter.setTradePrice(
															String.valueOf(mPriceSetting));
													mWishlistAdapter.notifyDataSetChanged();
													sumTotalPrice();
												}
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
										sumTotalPrice();
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

				convertView.findViewById(R.id.cardcost).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardtype).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardability).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardp).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardslash).setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.cardt).setVisibility(View.VISIBLE);

				Html.ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getResources());

				((TextView) convertView.findViewById(R.id.cardtype)).setText(info.mCard.type);
				((TextView) convertView.findViewById(R.id.cardcost)).setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.manaCost, imgGetter));
				((TextView) convertView.findViewById(R.id.cardability)).setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.ability, imgGetter));

				convertView.findViewById(R.id.cardt).setVisibility(View.GONE);
				convertView.findViewById(R.id.cardp).setVisibility(View.GONE);
				convertView.findViewById(R.id.cardslash).setVisibility(View.GONE);

				try {
					float p = info.mCard.power;
					if (p != CardDbAdapter.NOONECARES) {
						String pow;
						if (p == CardDbAdapter.STAR)
							pow = "*";
						else if (p == CardDbAdapter.ONEPLUSSTAR)
							pow = "1+*";
						else if (p == CardDbAdapter.TWOPLUSSTAR)
							pow = "2+*";
						else if (p == CardDbAdapter.SEVENMINUSSTAR)
							pow = "7-*";
						else if (p == CardDbAdapter.STARSQUARED)
							pow = "*^2";
						else {
							if (p == (int) p) {
								pow = Integer.valueOf((int) p).toString();
							}
							else {
								pow = Float.valueOf(p).toString();
							}
						}
						((TextView) convertView.findViewById(R.id.cardp)).setText(pow);

						convertView.findViewById(R.id.cardt).setVisibility(View.VISIBLE);
						convertView.findViewById(R.id.cardp).setVisibility(View.VISIBLE);
						convertView.findViewById(R.id.cardslash).setVisibility(View.VISIBLE);

					}
				} catch (NumberFormatException e) {
					/* eat it ? */
				}
				try {
					float t = info.mCard.toughness;
					if (t != CardDbAdapter.NOONECARES) {
						String tou;
						if (t == CardDbAdapter.STAR)
							tou = "*";
						else if (t == CardDbAdapter.ONEPLUSSTAR)
							tou = "1+*";
						else if (t == CardDbAdapter.TWOPLUSSTAR)
							tou = "2+*";
						else if (t == CardDbAdapter.SEVENMINUSSTAR)
							tou = "7-*";
						else if (t == CardDbAdapter.STARSQUARED)
							tou = "*^2";
						else {
							if (t == (int) t) {
								tou = Integer.valueOf((int) t).toString();
							}
							else {
								tou = Float.valueOf(t).toString();
							}
						}
						((TextView) convertView.findViewById(R.id.cardt)).setText(tou);
					}
				} catch (NumberFormatException e) {
					/* eat it? */
				}

				float loyalty = info.mCard.loyalty;
				if (loyalty != -1 && loyalty != CardDbAdapter.NOONECARES) {
					if (loyalty == (int) loyalty) {
						((TextView) convertView.findViewById(R.id.cardt)).setText(Integer.toString((int) loyalty));
					}
					else {
						((TextView) convertView.findViewById(R.id.cardt)).setText(Float.toString(loyalty));
					}
					convertView.findViewById(R.id.cardt).setVisibility(View.VISIBLE);
					convertView.findViewById(R.id.cardp).setVisibility(View.GONE);
					convertView.findViewById(R.id.cardslash).setVisibility(View.GONE);
				}
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
				int color;
				switch (info.mRarity.get(i)) {
					case 'c':
					case 'C':
						color = R.color.common;
						break;
					case 'u':
					case 'U':
						color = R.color.uncommon;
						break;
					case 'r':
					case 'R':
						color = R.color.rare;
						break;
					case 'm':
					case 'M':
						color = R.color.mythic;
						break;
					case 't':
					case 'T':
						color = R.color.timeshifted;
						break;
					default:
						color = R.color.black;
						break;
				}
				((TextView) setRow.findViewById(R.id.wishlistRowSet)).setText(info.mSets.get(i));
				((TextView) setRow.findViewById(R.id.wishlistRowSet)).setTextColor(getResources().getColor(color));

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
							priceText.setText(String.format("%dx $%.02f", info.mNumberOf.get(i), info.mPrice.get(i).mFoilAverage));
							priceText.setTextColor(getResources().getColor(R.color.black));
						}
						else {
							priceText.setText(info.mMessage.get(i));
							priceText.setTextColor(getResources().getColor(R.color.holo_red_dark));
						}
					}
					else {
						if (info.mPrice.get(i).mAverage != 0) {
							switch (mPriceSetting) {
								case LOW_PRICE:
									priceText.setText(String.format("%dx $%.02f", info.mNumberOf.get(i), info.mPrice.get(i).mLow));
									break;
								case AVG_PRICE:
									priceText.setText(String.format("%dx $%.02f", info.mNumberOf.get(i), info.mPrice.get(i).mAverage));
									break;
								case HIGH_PRICE:
									priceText.setText(String.format("%dx $%.02f", info.mNumberOf.get(i), info.mPrice.get(i).mHigh));
									break;
							}
							priceText.setTextColor(getResources().getColor(R.color.black));
						}
						else {
							priceText.setText(info.mMessage.get(i));
							priceText.setTextColor(getResources().getColor(R.color.holo_red_dark));
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
				/* because this can return when the fragment is in the background */
				if(!WishlistFragment.this.isAdded()) {
					return;
				}
				/* Find the compressed wishlist info for this card */
				for (CompressedWishlistInfo cwi : mCompressedWishlist) {
					if (cwi.mCard.name.equals(mCardName)) {
						/* Find all foil and non foil compressed items with the same set code */
						for (int i = 0; i < cwi.mSetCodes.size(); i++) {
							if (cwi.mSetCodes.get(i).equals(mSetCode)) {
								/* The message will never be shown with a valid price, so set it as DNE */
								cwi.mMessage.set(i, spiceException.getLocalizedMessage());
							}
						}
					}
				}
				mWishlistAdapter.notifyDataSetChanged();
			}

			@Override
			public void onRequestSuccess(final PriceInfo result) {
				/* because this can return when the fragment is in the background */
				if(!WishlistFragment.this.isAdded()) {
					return;
				}
				/* Find the compressed wishlist info for this card */
				for (CompressedWishlistInfo cwi : mCompressedWishlist) {
					if (cwi.mCard.name.equals(mCardName)) {
						/* Find all foil and non foil compressed items with the same set code */
						for (int i = 0; i < cwi.mSetCodes.size(); i++) {
							if (cwi.mSetCodes.get(i).equals(mSetCode)) {
								/* Set the whole price info object */
								if (result != null) {
									cwi.mPrice.set(i, result);
								}
								/* The message will never be shown with a valid price, so set it as DNE */
								cwi.mMessage.set(i, getString(R.string.card_view_price_not_found));
							}
						}
					}
					sumTotalPrice();
				}
				mWishlistAdapter.notifyDataSetChanged();
			}
		});
	}

	void sumTotalPrice() {
		if (mShowTotalWishlistPrice) {
			float totalPrice = 0;

			for (CompressedWishlistInfo cwi : mCompressedWishlist) {
				for (int i = 0; i < cwi.mSets.size(); i++) {
					if (cwi.mIsFoil.get(i)) {
						totalPrice += (cwi.mPrice.get(i).mFoilAverage * cwi.mNumberOf.get(i));
					}
					else {
						switch (mPriceSetting) {
							case LOW_PRICE:
								totalPrice += (cwi.mPrice.get(i).mLow * cwi.mNumberOf.get(i));
								break;
							case AVG_PRICE:
								totalPrice += (cwi.mPrice.get(i).mAverage * cwi.mNumberOf.get(i));
								break;
							case HIGH_PRICE:
								totalPrice += (cwi.mPrice.get(i).mHigh * cwi.mNumberOf.get(i));
								break;
						}
					}
				}
			}
			mTotalPriceField.setText(String.format("$%.02f", totalPrice));
		}
	}
}
