package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
	private static final int FOIL_PRICE = 3;

	/* State variables */
	private int mPriceSetting;

	/* UI Elements */
	private AutoCompleteTextView mNameField;
	private EditText mNumberField;
	private ArrayList<CompressedWishlistInfo> mCompressedWishlist;
	private WishlistArrayAdapter mWishlistAdapter;
	private boolean mShowCardInfo;
	private boolean mShowIndividialPrices;
	private boolean mShowTotalWishlistPrice;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View myFragmentView = inflater.inflate(R.layout.wishlist_frag, container, false);

		/* set the autocomplete for card names */
		assert myFragmentView != null;
		mNameField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.name_search);
		mNameField.setAdapter(new AutocompleteCursorAdapter(this.getActivity()));

		mNumberField = (EditText) myFragmentView.findViewById(R.id.number_input);
		mNumberField.setText("1");

		ListView listView = (ListView) myFragmentView.findViewById(R.id.wishlist);

		myFragmentView.findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(getActivity(), mNumberField.getText() + "x " + mNameField.getText(), Toast.LENGTH_LONG).show();
				mNumberField.setText("1");
				mNameField.setText("");
			}
		});

		ArrayList<MtgCard> wishlist = WishlistHelpers.ReadWishlist(getActivity());
		mCompressedWishlist = new ArrayList<CompressedWishlistInfo>();

		try {
			CardDbAdapter adapter = new CardDbAdapter(getActivity());
			for (MtgCard card : wishlist) {
				card.tcgName = adapter.getTCGname(card.setCode);
			}
			adapter.close();
		} catch (FamiliarDbException e) {
			e.printStackTrace();
		}

		/* Compress the wishlist and load the prices */
		for (MtgCard card : wishlist) {
			if (!mCompressedWishlist.contains(card)) {
				mCompressedWishlist.add(new CompressedWishlistInfo(card));
			}
			else {
				mCompressedWishlist.get(mCompressedWishlist.indexOf(card)).add(card);
			}
			loadPrice(card.name, card.setCode, card.number, -1);
		}

		mWishlistAdapter = new WishlistArrayAdapter(getActivity(), mCompressedWishlist);
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
		mPriceSetting = Integer.parseInt(((FamiliarActivity) getActivity()).mPreferenceAdapter.getTradePrice());
		mShowIndividialPrices = ((FamiliarActivity) getActivity()).mPreferenceAdapter.getShowIndividualWishlistPrices();
		mShowTotalWishlistPrice = ((FamiliarActivity) getActivity()).mPreferenceAdapter.getShowTotalWishlistPrice();
		mShowCardInfo = ((FamiliarActivity) getActivity()).mPreferenceAdapter.getVerboseWishlist();
		super.onResume();
	}

	private void mailWishlist(boolean includeTcgName) {
		// TODO mail the wishlist
	}

	/**
	 * Override this to be notified when the wishlist changes
	 */
	@Override
	public void onWishlistChanged() {
		// TODO reload wishlist?
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
	 * @param cardName
	 */
	void showDialog(final int id, final String cardName) {
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
												((FamiliarActivity) getActivity()).mPreferenceAdapter.setTradePrice(
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

	public class CompressedWishlistInfo {
		public MtgCard mCard;
		public ArrayList<String> mSets;
		public ArrayList<String> mSetCodes;
		public ArrayList<Boolean> mIsFoil;
		public ArrayList<PriceInfo> mPrice;
		public ArrayList<String> mMessage;
		public ArrayList<Integer> mNumberOf;

		CompressedWishlistInfo(MtgCard card) {
			mSets = new ArrayList<String>();
			mSetCodes = new ArrayList<String>();
			mIsFoil = new ArrayList<Boolean>();
			mPrice = new ArrayList<PriceInfo>();
			mMessage = new ArrayList<String>();
			mNumberOf = new ArrayList<Integer>();

			mCard = card;
			add(mCard);
		}

		void add(MtgCard card) {
			mSets.add(card.tcgName);
			mSetCodes.add(card.setCode);
			mIsFoil.add(card.foil);
			mPrice.add(new PriceInfo());
			mMessage.add(card.message);
			mNumberOf.add(card.numberOf);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof CompressedWishlistInfo) {
				return mCard.name.equals(((CompressedWishlistInfo) o).mCard.name);
			}
			else if (o instanceof MtgCard) {
				return mCard.name.equals(((MtgCard) o).name);
			}
			return false;
		}
	}

	/**
	 * This nested class is the adapter which populates the listView in the drawer menu. It handles both entries and
	 * headers
	 */
	public class WishlistArrayAdapter extends ArrayAdapter<CompressedWishlistInfo> {
		private final Context context;
		private final ArrayList<CompressedWishlistInfo> values;

		/**
		 * Constructor. The context will be used to inflate views later. The array of values will be used to populate
		 * the views
		 *
		 * @param context The application's context, used to inflate views later.
		 * @param values  An array of DrawerEntries which will populate the list
		 */
		public WishlistArrayAdapter(Context context, ArrayList<CompressedWishlistInfo> values) {
			super(context, R.layout.drawer_list_item, values);
			this.context = context;
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
				convertView = getActivity().getLayoutInflater().inflate(R.layout.result_list_card_row, null);
				assert convertView != null;
				wishlistSets = ((LinearLayout) convertView.findViewById(R.id.wishlist_sets));
			}
			else {
				wishlistSets = ((LinearLayout) convertView.findViewById(R.id.wishlist_sets));
				wishlistSets.removeAllViews();
			}

			CompressedWishlistInfo info = values.get(position);

			((TextView) convertView.findViewById(R.id.cardname)).setText(info.mCard.name);

			for (int i = 0; i < info.mSets.size(); i++) {

				View setRow = getActivity().getLayoutInflater().inflate(R.layout.wishlist_cardset_row, null);
				assert setRow != null;
				if (info.mIsFoil.get(i)) {
					setRow.findViewById(R.id.wishlistSetRowFoil).setVisibility(View.VISIBLE);
					if (info.mPrice.get(i).mFoilAverage != 0) {
						((TextView) setRow.findViewById(R.id.wishlistRowPrice)).setText(info.mNumberOf.get(i) + "x $" + info.mPrice.get(i).mFoilAverage + "");
					}
				}
				else {
					setRow.findViewById(R.id.wishlistSetRowFoil).setVisibility(View.GONE);
					if (info.mPrice.get(i).mAverage != 0) {
						switch (mPriceSetting) {
							case LOW_PRICE:
								((TextView) setRow.findViewById(R.id.wishlistRowPrice)).setText(info.mNumberOf.get(i) + "x $" + info.mPrice.get(i).mLow + "");
								break;
							case AVG_PRICE:
								((TextView) setRow.findViewById(R.id.wishlistRowPrice)).setText(info.mNumberOf.get(i) + "x $" + info.mPrice.get(i).mAverage + "");
								break;
							case HIGH_PRICE:
								((TextView) setRow.findViewById(R.id.wishlistRowPrice)).setText(info.mNumberOf.get(i) + "x $" + info.mPrice.get(i).mHigh + "");
								break;
						}
					}
				}

				((TextView) setRow.findViewById(R.id.wishlistRowSet)).setText(info.mSets.get(i));

				wishlistSets.addView(setRow);
			}
			return convertView;
		}
	}

	public void loadPrice(final String mCardName, final String mSetCode, String mCardNumber, int mMultiverseId) {
		PriceFetchRequest priceRequest;
		priceRequest = new PriceFetchRequest(mCardName, mSetCode, mCardNumber, mMultiverseId, getActivity());
		((FamiliarActivity) getActivity()).mSpiceManager.execute(priceRequest, mCardName + "-" +
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
				}
				else {
					Toast.makeText(getActivity(), R.string.card_view_price_not_found, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
}
