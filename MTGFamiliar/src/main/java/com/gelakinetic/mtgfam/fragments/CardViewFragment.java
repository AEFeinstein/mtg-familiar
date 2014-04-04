/**
 Copyright 2011 Adam Feinstein

 This file is part of MTG Familiar.

 MTG Familiar is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 MTG Familiar is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class CardViewFragment extends FamiliarFragment {

	/* Dialogs */
	private static final int GET_PRICE = 1;
	private static final int GET_IMAGE = 2;
	private static final int CHANGE_SET = 3;
	private static final int CARD_RULINGS = 4;
	private static final int WISH_LIST_COUNTS = 6;
	private static final int GET_LEGALITY = 7;
	private static final int PROGRESS = 8;

	/* Where the card image is loaded to */
	private static final int MAIN_PAGE = 1;
	private static final int DIALOG = 2;

	/* Bundle constant */
	public static final String CARD_ID = "card_id";

	/* Used to store the String when copying to clipboard */
	private String mCopyString;

	/* UI elements, to be filled in */
	private TextView mNameTextView;
	private TextView mCostTextView;
	private TextView mTypeTextView;
	private TextView mSetTextView;
	private TextView mAbilityTextView;
	private TextView mPowTouTextView;
	private TextView mFlavorTextView;
	private TextView mArtistTextView;
	private Button mTransformButton;
	private View mTransformButtonDivider;
	private ImageView mCardImageView;
	private FrameLayout mFrameLayout;

	/* the AsyncTask loads stuff off the UI thread, and stores whatever in these local variables */
	private AsyncTask<Void, Void, Void> mAsyncTask;
	private BitmapDrawable mCardBitmap;
	private int loadTo = DIALOG; /* where to load the image */
	private String[] mLegalities;
	private String[] mFormats;
	private ArrayList<Ruling> mRulingsArrayList;

	/* Loaded in a Spice Service */
	private PriceInfo mPriceInfo;

	/* Card info, used to build the URL to fetch the picture */
	private String mCardNumber;
	private String mSetCode;
	private String mCardName;
	private String mMagicCardsInfoSetCode;
	private int mMultiverseId;

	/* Card info used to flip the card */
	private String mTransformCardNumber;
	private int mTransformId;

	/* To switch card between printings */
	private LinkedHashSet<String> mSets;
	private LinkedHashSet<Long> mCardIds;

	/**
	 * Kill any AsyncTask if it is still running
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();

		/* Pass a non-null bundle to the ResultListFragment so it knows to exit if there was a list of 1 card
		 * If this wasn't launched by a ResultListFragment, it'll get eaten */
		Bundle args = new Bundle();
		getFamiliarActivity().setFragmentResult(args);
		if (mAsyncTask != null) {
			mAsyncTask.cancel(true);
		}
	}

	/**
	 * Inflates the view and saves references to UI elements for later filling
	 *
	 * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
	 *                           here.
	 * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
	 * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
	 *                           fragment should not add the view itself, but this can be used to generate the
	 *                           LayoutParams of the view.
	 * @return The inflated view
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View myFragmentView = inflater.inflate(R.layout.card_view_frag, container, false);

		assert myFragmentView != null; /* Because Android Studio */
		mNameTextView = (TextView) myFragmentView.findViewById(R.id.name);
		mCostTextView = (TextView) myFragmentView.findViewById(R.id.cost);
		mTypeTextView = (TextView) myFragmentView.findViewById(R.id.type);
		mSetTextView = (TextView) myFragmentView.findViewById(R.id.set);
		mAbilityTextView = (TextView) myFragmentView.findViewById(R.id.ability);
		mFlavorTextView = (TextView) myFragmentView.findViewById(R.id.flavor);
		mArtistTextView = (TextView) myFragmentView.findViewById(R.id.artist);
		mPowTouTextView = (TextView) myFragmentView.findViewById(R.id.pt);
		mTransformButtonDivider = myFragmentView.findViewById(R.id.transform_button_divider);
		mTransformButton = (Button) myFragmentView.findViewById(R.id.transformbutton);
		mFrameLayout = (FrameLayout) myFragmentView.findViewById(R.id.frameLayout1);
		mCardImageView = (ImageView) myFragmentView.findViewById(R.id.cardpic);

		registerForContextMenu(mNameTextView);
		registerForContextMenu(mCostTextView);
		registerForContextMenu(mTypeTextView);
		registerForContextMenu(mSetTextView);
		registerForContextMenu(mAbilityTextView);
		registerForContextMenu(mPowTouTextView);
		registerForContextMenu(mFlavorTextView);
		registerForContextMenu(mArtistTextView);

		if (getFamiliarActivity().mPreferenceAdapter.getPicFirst()) {
			loadTo = MAIN_PAGE;
		}
		else {
			loadTo = DIALOG;
		}

		setInfoFromBundle(this.getArguments());

		return myFragmentView;
	}

	/**
	 * This will fill the UI elements with database information about the card specified in the given bundle
	 *
	 * @param extras The bundle passed to this fragment
	 */
	private void setInfoFromBundle(Bundle extras) {
		if (extras == null) {
			mNameTextView.setText("");
			mCostTextView.setText("");
			mTypeTextView.setText("");
			mSetTextView.setText("");
			mAbilityTextView.setText("");
			mFlavorTextView.setText("");
			mArtistTextView.setText("");
			mPowTouTextView.setText("");
			mTransformButton.setVisibility(View.GONE);
			mTransformButtonDivider.setVisibility(View.GONE);
			return;
		}
		long cardID = extras.getLong(CARD_ID);

		try {
			/* from onCreateView */
			setInfoFromID(cardID);
		} catch (FamiliarDbException e) {
			handleFamiliarDbException(true);
		}
	}

	/**
	 * This will fill the UI elements with information from the database
	 * It also saves information for AsyncTasks to use later and manages the transform/flip button
	 *
	 * @param id the ID of the the card to be displayed
	 * @throws FamiliarDbException
	 */
	private void setInfoFromID(final long id) throws FamiliarDbException {

		ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getResources());

		CardDbAdapter dbHelper = new CardDbAdapter(getActivity());
		Cursor cCardById = dbHelper.fetchCard(id, null);

		/* http://magiccards.info/scans/en/mt/55.jpg */
		mCardName = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NAME));
		mSetCode = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET));

		mMagicCardsInfoSetCode =
				dbHelper.getCodeMtgi(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET)));
		mCardNumber = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NUMBER));

		switch ((char) cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_RARITY))) {
			case 'C':
			case 'c':
				mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(R.color.common));
				break;
			case 'U':
			case 'u':
				mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(R.color.uncommon));
				break;
			case 'R':
			case 'r':
				mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(R.color.rare));
				break;
			case 'M':
			case 'm':
				mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(R.color.mythic));
				break;
			case 'T':
			case 't':
				mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(R.color.timeshifted));
				break;
		}

		String sCost = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_MANACOST));
		CharSequence csCost = ImageGetterHelper.formatStringWithGlyphs(sCost, imgGetter);
		mCostTextView.setText(csCost);

		String sAbility = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_ABILITY));
		CharSequence csAbility = ImageGetterHelper.formatStringWithGlyphs(sAbility, imgGetter);
		mAbilityTextView.setText(csAbility);

		String sFlavor = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
		CharSequence csFlavor = ImageGetterHelper.formatStringWithGlyphs(sFlavor, imgGetter);
		mFlavorTextView.setText(csFlavor);

		mNameTextView.setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NAME)));
		mTypeTextView.setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_TYPE)));
		mSetTextView.setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET)));
		mArtistTextView.setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_ARTIST)));

		int loyalty = cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
		float p = cCardById.getFloat(cCardById.getColumnIndex(CardDbAdapter.KEY_POWER));
		float t = cCardById.getFloat(cCardById.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
		if (loyalty != CardDbAdapter.NOONECARES) {
			mPowTouTextView.setText(Integer.valueOf(loyalty).toString());
		}
		else if (p != CardDbAdapter.NOONECARES && t != CardDbAdapter.NOONECARES) {

			String powTouStr = "";

			if (p == CardDbAdapter.STAR)
				powTouStr += "*";
			else if (p == CardDbAdapter.ONEPLUSSTAR)
				powTouStr += "1+*";
			else if (p == CardDbAdapter.TWOPLUSSTAR)
				powTouStr += "2+*";
			else if (p == CardDbAdapter.SEVENMINUSSTAR)
				powTouStr += "7-*";
			else if (p == CardDbAdapter.STARSQUARED)
				powTouStr += "*^2";
			else {
				if (p == (int) p) {
					powTouStr += (int) p;
				}
				else {
					powTouStr += p;
				}
			}

			powTouStr += "/";

			if (t == CardDbAdapter.STAR)
				powTouStr += "*";
			else if (t == CardDbAdapter.ONEPLUSSTAR)
				powTouStr += "1+*";
			else if (t == CardDbAdapter.TWOPLUSSTAR)
				powTouStr += "2+*";
			else if (t == CardDbAdapter.SEVENMINUSSTAR)
				powTouStr += "7-*";
			else if (t == CardDbAdapter.STARSQUARED)
				powTouStr += "*^2";
			else {
				if (t == (int) t) {
					powTouStr += (int) t;
				}
				else {
					powTouStr += t;
				}
			}

			mPowTouTextView.setText(powTouStr);
		}
		else {
			mPowTouTextView.setText("");
		}

		boolean isMultiCard = false;
		switch (CardDbAdapter.isMulticard(mCardNumber,
				cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET)))) {
			case CardDbAdapter.NOPE:
				isMultiCard = false;
				mTransformButton.setVisibility(View.GONE);
				mTransformButtonDivider.setVisibility(View.GONE);
				break;
			case CardDbAdapter.TRANSFORM:
				isMultiCard = true;
				mTransformButton.setVisibility(View.VISIBLE);
				mTransformButtonDivider.setVisibility(View.VISIBLE);
				mTransformButton.setText(R.string.card_view_transform);
				break;
			case CardDbAdapter.FUSE:
				isMultiCard = true;
				mTransformButton.setVisibility(View.VISIBLE);
				mTransformButtonDivider.setVisibility(View.VISIBLE);
				mTransformButton.setText(R.string.card_view_fuse);
				break;
			case CardDbAdapter.SPLIT:
				isMultiCard = true;
				mTransformButton.setVisibility(View.VISIBLE);
				mTransformButtonDivider.setVisibility(View.VISIBLE);
				mTransformButton.setText(R.string.card_view_other_half);
				break;
		}

		if (isMultiCard) {
			if (mCardNumber.contains("a")) {
				mTransformCardNumber = mCardNumber.replace("a", "b");
			}
			else if (mCardNumber.contains("b")) {
				mTransformCardNumber = mCardNumber.replace("b", "a");
			}
			mTransformId = dbHelper.getTransform(mSetCode, mTransformCardNumber);
			mTransformButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mCardBitmap = null;
					mCardNumber = mTransformCardNumber;
					try {
						setInfoFromID(mTransformId);
					} catch (FamiliarDbException e) {
						handleFamiliarDbException(true);
					}

				}
			});
		}

		/* Do we load the image immediately to the main page, or do it in a dialog later? */
		if (loadTo == MAIN_PAGE) {
			mCardImageView.setVisibility(View.VISIBLE);
			mNameTextView.setVisibility(View.GONE);
			mCostTextView.setVisibility(View.GONE);
			mTypeTextView.setVisibility(View.GONE);
			mSetTextView.setVisibility(View.GONE);
			mAbilityTextView.setVisibility(View.GONE);
			mPowTouTextView.setVisibility(View.GONE);
			mFlavorTextView.setVisibility(View.GONE);
			mArtistTextView.setVisibility(View.GONE);
			mFrameLayout.setVisibility(View.GONE);

			showDialog(PROGRESS);
			mAsyncTask = new FetchPictureTask();
			mAsyncTask.execute((Void[]) null);
		}
		else {
			mCardImageView.setVisibility(View.GONE);
			mNameTextView.setVisibility(View.VISIBLE);
			mCostTextView.setVisibility(View.VISIBLE);
			mTypeTextView.setVisibility(View.VISIBLE);
			mSetTextView.setVisibility(View.VISIBLE);
			mAbilityTextView.setVisibility(View.VISIBLE);
			mPowTouTextView.setVisibility(View.VISIBLE);
			mFlavorTextView.setVisibility(View.VISIBLE);
			mArtistTextView.setVisibility(View.VISIBLE);
			mFrameLayout.setVisibility(View.VISIBLE);
		}

		mMultiverseId = cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_MULTIVERSEID));

		cCardById.close();

		/* Find the other sets this card is in ahead of time, so that it can be remove from the menu if there is only
		   one set */
		Cursor cCardByName = dbHelper.fetchCardByName(mCardName,
				new String[]{
						CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
						CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID});
		mSets = new LinkedHashSet<String>();
		mCardIds = new LinkedHashSet<Long>();
		while (!cCardByName.isAfterLast()) {
			if (mSets.add(
					dbHelper.getTCGname(cCardByName.getString(cCardByName.getColumnIndex(CardDbAdapter.KEY_SET))))) {
				mCardIds.add(cCardByName.getLong(cCardByName.getColumnIndex(CardDbAdapter.KEY_ID)));
			}
			cCardByName.moveToNext();
		}
		cCardByName.close();
		/* If it exists in only one set, remove the button from the menu */
		if (mSets.size() == 1) {
			getActivity().invalidateOptionsMenu();
		}

		dbHelper.close();

	}

	/**
	 * This private class handles asking the database about the legality of a card, and will eventually show the
	 * information in a Dialog
	 */
	private class FetchLegalityTask extends AsyncTask<Void, Void, Void> {

		/**
		 * Queries the data in the database to see what sets this card is legal in
		 *
		 * @param params unused
		 * @return unused
		 */
		@Override
		protected Void doInBackground(Void... params) {

			try {
				CardDbAdapter dbHelper = new CardDbAdapter(getActivity());
				Cursor cFormats = dbHelper.fetchAllFormats();
				mFormats = new String[cFormats.getCount()];
				mLegalities = new String[cFormats.getCount()];

				cFormats.moveToFirst();
				for (int i = 0; i < cFormats.getCount(); i++) {
					mFormats[i] = cFormats.getString(cFormats.getColumnIndex(CardDbAdapter.KEY_NAME));
					switch (dbHelper.checkLegality(mCardName, mFormats[i])) {
						case CardDbAdapter.LEGAL:
							mLegalities[i] = getString(R.string.card_view_legal);
							break;
						case CardDbAdapter.RESTRICTED:
							mLegalities[i] = getString(R.string.card_view_restricted);
							break;
						case CardDbAdapter.BANNED:
							mLegalities[i] = getString(R.string.card_view_banned);
							break;
						default:
							mLegalities[i] = getString(R.string.error);
							break;
					}
					cFormats.moveToNext();
				}

				cFormats.close();
				dbHelper.close();
			} catch (FamiliarDbException e) {
				CardViewFragment.this.handleFamiliarDbException(false);
				mLegalities = null;
			}

			return null;
		}

		/**
		 * After the query, remove the progress dialog and show the legalities
		 *
		 * @param result unused
		 */
		@Override
		protected void onPostExecute(Void result) {
			showDialog(GET_LEGALITY);
		}
	}

	/**
	 * This private class retrieves a picture of the card from the internet
	 */
	private class FetchPictureTask extends AsyncTask<Void, Void, Void> {

		private String error;

		/**
		 * First check www.MagicCards.info for the card image in the user's preferred language
		 * If that fails, check www.MagicCards.info for the card image in English
		 * If that fails, check www.gatherer.wizards.com for the card image
		 * If that fails, give up
		 * There is non-standard URL building for planes and schemes
		 * It also re-sizes the image
		 *
		 * @param params unused
		 * @return unused
		 */
		@SuppressWarnings("SpellCheckingInspection")
		@Override
		protected Void doInBackground(Void... params) {
			error = null;
			String cardLanguage = getFamiliarActivity().mPreferenceAdapter.getCardLanguage();
			if (cardLanguage == null) {
				cardLanguage = "en";
			}

			boolean bRetry = true;
			boolean triedEn = false;

			while (bRetry) {

				bRetry = false;

				try {

					if (cardLanguage.equalsIgnoreCase("en")) {
						triedEn = true;
					}
					String picURL;
					if (mSetCode.equals("PP2")) {
						picURL = "http://magiccards.info/extras/plane/planechase-2012-edition/" + mCardName + ".jpg";
						picURL = picURL.replace(" ", "-").replace(Character.toChars(0xC6)[0] + "", "Ae")
								.replace("?", "").replace(",", "").replace("'", "").replace("!", "");
					}
					else if (mSetCode.equals("PCP")) {
						if (mCardName.equalsIgnoreCase("tazeem")) {
							mCardName = "tazeem-release-promo";
							picURL = "http://magiccards.info/extras/plane/planechase/" + mCardName + ".jpg";
						}
						else if (mCardName.equalsIgnoreCase("celestine reef")) {
							mCardName = "celestine-reef-pre-release-promo";
							picURL = "http://magiccards.info/extras/plane/planechase/" + mCardName + ".jpg";
						}
						else if (mCardName.equalsIgnoreCase("horizon boughs")) {
							mCardName = "horizon-boughs-gateway-promo";
							picURL = "http://magiccards.info/extras/plane/planechase/" + mCardName + ".jpg";
						}
						else {
							picURL = "http://magiccards.info/extras/plane/planechase/" + mCardName + ".jpg";
						}
						picURL = picURL.replace(" ", "-").replace(Character.toChars(0xC6)[0] + "", "Ae")
								.replace("?", "").replace(",", "").replace("'", "").replace("!", "");
					}
					else if (mSetCode.equals("ARS")) {
						picURL = "http://magiccards.info/extras/scheme/archenemy/" + mCardName + ".jpg";
						picURL = picURL.replace(" ", "-").replace(Character.toChars(0xC6)[0] + "", "Ae")
								.replace("?", "").replace(",", "").replace("'", "").replace("!", "");
					}
					else {
						picURL = "http://magiccards.info/scans/" + cardLanguage + "/" + mMagicCardsInfoSetCode + "/" +
								mCardNumber + ".jpg";
					}
					picURL = picURL.toLowerCase(Locale.ENGLISH);

					URL u = new URL(picURL);
					try {
						mCardBitmap = new BitmapDrawable(getActivity().getResources(), u.openStream());
					} catch (FileNotFoundException e) {
						if (!triedEn) {
							/* Let the catch block take care of it */
							throw new FileNotFoundException();
						}
						else {
							/* Ok, it doesn't exist on MagicCards.info in English. Fall back to Gatherer */
							URL u2 = new URL("http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid="
									+ mMultiverseId + "&type=card");
							mCardBitmap = new BitmapDrawable(getActivity().getResources(), u2.openStream());
						}
					}

					/* Resize the image */
					int height = 0, width = 0;
					float scale;
					/* 16dp */
					int border = (int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
					if (loadTo == MAIN_PAGE) {
						Rect rectangle = new Rect();
						Window window = getActivity().getWindow();
						window.getDecorView().getWindowVisibleDisplayFrame(rectangle);

						assert getActivity().getActionBar() != null; /* Because Android Studio */
						height = ((rectangle.bottom - rectangle.top) - getActivity().getActionBar().getHeight())
								- border;
						width = (rectangle.right - rectangle.left) - border;
					}
					else if (loadTo == DIALOG) {
						Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE))
								.getDefaultDisplay();
						Point p = new Point();
						display.getSize(p);
						height = p.y - border;
						width = p.x - border;
					}

					float screenAspectRatio = (float) height / (float) (width);
					float cardAspectRatio = (float) mCardBitmap.getIntrinsicHeight() /
							(float) mCardBitmap.getIntrinsicWidth();

					if (screenAspectRatio > cardAspectRatio) {
						scale = (width) / (float) mCardBitmap.getIntrinsicWidth();
					}
					else {
						scale = (height) / (float) mCardBitmap.getIntrinsicHeight();
					}

					int newWidth = Math.round(mCardBitmap.getIntrinsicWidth() * scale);
					int newHeight = Math.round(mCardBitmap.getIntrinsicHeight() * scale);

					Bitmap d = mCardBitmap.getBitmap();
					Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, newWidth, newHeight, true);
					mCardBitmap = new BitmapDrawable(getActivity().getResources(), bitmapOrig);
				} catch (FileNotFoundException e) {
					/* internet works, image not found */
					if (cardLanguage.equalsIgnoreCase("en")) {
						error = getString(R.string.card_view_image_not_found);
					}
					else {
						/* If image doesn't exist in the preferred language, let's retry with "en" */
						cardLanguage = "en";
						bRetry = true;
					}
				} catch (ConnectException e) {
					/* no internet */
					error = "ConnectException";
				} catch (UnknownHostException e) {
					/* no internet */
					error = "UnknownHostException";
				} catch (MalformedURLException e) {
					error = "MalformedURLException";
				} catch (IOException e) {
					error = "IOException";
				} catch (NullPointerException e) {
					error = "NullPointerException";
				}
			}
			return null;
		}

		/**
		 * When the task has finished, if there was no error, remove the progress dialog and show the image
		 * If the image was supposed to load to the main screen, and it failed to load, fall back to text view
		 *
		 * @param result unused
		 */
		@Override
		protected void onPostExecute(Void result) {
			if (error == null) {
				if (loadTo == DIALOG) {
					showDialog(GET_IMAGE);
				}
				else if (loadTo == MAIN_PAGE) {
					removeDialog(getFragmentManager());
					mCardImageView.setImageDrawable(mCardBitmap);
					getActivity().invalidateOptionsMenu(); /* remove the image load button if it is the main page */
				}
			}
			else {
				removeDialog(getFragmentManager());
				if (loadTo == MAIN_PAGE) {
					mCardImageView.setVisibility(View.GONE);
					mNameTextView.setVisibility(View.VISIBLE);
					mCostTextView.setVisibility(View.VISIBLE);
					mTypeTextView.setVisibility(View.VISIBLE);
					mSetTextView.setVisibility(View.VISIBLE);
					mAbilityTextView.setVisibility(View.VISIBLE);
					mPowTouTextView.setVisibility(View.VISIBLE);
					mFlavorTextView.setVisibility(View.VISIBLE);
					mArtistTextView.setVisibility(View.VISIBLE);
					mFrameLayout.setVisibility(View.VISIBLE);
				}
				Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
			}
		}

		/**
		 * If the task is canceled, fall back to text view
		 */
		@Override
		protected void onCancelled() {
			if (loadTo == MAIN_PAGE) {
				mCardImageView.setVisibility(View.GONE);
				mNameTextView.setVisibility(View.VISIBLE);
				mCostTextView.setVisibility(View.VISIBLE);
				mTypeTextView.setVisibility(View.VISIBLE);
				mSetTextView.setVisibility(View.VISIBLE);
				mAbilityTextView.setVisibility(View.VISIBLE);
				mPowTouTextView.setVisibility(View.VISIBLE);
				mFlavorTextView.setVisibility(View.VISIBLE);
				mArtistTextView.setVisibility(View.VISIBLE);
				mFrameLayout.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * This private class fetches rulings about this card from gatherer.wizards.com
	 */
	private class FetchRulingsTask extends AsyncTask<Void, Void, Void> {

		String mErrorMessage = null;

		/**
		 * This function downloads the source of the gatherer page, scans it for rulings, and stores them for display
		 *
		 * @param params unused
		 * @return unused
		 */
		@Override
		@SuppressWarnings("SpellCheckingInspection")
		protected Void doInBackground(Void... params) {

			URL url;
			InputStream is = null;
			BufferedReader br;
			String line;

			mRulingsArrayList = new ArrayList<Ruling>();

			try {
				url = new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + mMultiverseId);
				is = url.openStream(); /* throws an IOException */
				br = new BufferedReader(new InputStreamReader(new BufferedInputStream(is)));

				String date = null, ruling;
				while ((line = br.readLine()) != null) {
					if (line.contains("rulingDate") && line.contains("<td")) {
						date = (line.replace("<autocard>", "").replace("</autocard>", ""))
								.split(">")[1].split("<")[0];
					}
					if (line.contains("rulingText") && line.contains("<td")) {
						ruling = (line.replace("<autocard>", "").replace("</autocard>", ""))
								.split(">")[1].split("<")[0];
						Ruling r = new Ruling(date, ruling);
						mRulingsArrayList.add(r);
					}
				}
			} catch (MalformedURLException mue) {
				mErrorMessage = mue.getLocalizedMessage();
			} catch (IOException ioe) {
				mErrorMessage = ioe.getLocalizedMessage();
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException ioe) {
					mErrorMessage = ioe.getLocalizedMessage();
				}
			}

			return null;
		}

		/**
		 * Hide the progress dialog and show the rulings, if there are no errors
		 *
		 * @param result unused
		 */
		@Override
		protected void onPostExecute(Void result) {

			if (mErrorMessage == null) {
				showDialog(CARD_RULINGS);
			}
			else {
				removeDialog(getFragmentManager());
				Toast.makeText(getActivity(), mErrorMessage, Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * This inner class encapsulates a ruling and the date it was made
	 */
	private static class Ruling {
		public final String date;
		public final String ruling;

		public Ruling(String d, String r) {
			date = d;
			ruling = r;
		}

		public String toString() {
			return date + ": " + ruling;
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
			@SuppressWarnings("SpellCheckingInspection")
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				super.onCreateDialog(savedInstanceState);

				/* This will be set to false if we are returning a null dialog. It prevents a crash */
				setShowsDialog(true);

				switch (id) {
					case GET_IMAGE: {
						if (mCardBitmap == null) {
							setShowsDialog(false);
							return null;
						}

						Dialog dialog = new Dialog(this.getActivity());
						dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

						dialog.setContentView(R.layout.card_view_image_dialog);

						ImageView dialogImageView = (ImageView) dialog.findViewById(R.id.cardimage);
						dialogImageView.setImageDrawable(mCardBitmap);

						return dialog;
					}
					case GET_LEGALITY: {
						if (mFormats == null || mLegalities == null) {
							/* exception handled in AsyncTask */
							setShowsDialog(false);
							return null;
						}

						/* create the item mapping */
						String[] from = new String[]{"format", "status"};
						int[] to = new int[]{R.id.format, R.id.status};

						/* prepare the list of all records */
						List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
						for (int i = 0; i < mFormats.length; i++) {
							HashMap<String, String> map = new HashMap<String, String>();
							map.put(from[0], mFormats[i]);
							map.put(from[1], mLegalities[i]);
							fillMaps.add(map);
						}

						SimpleAdapter adapter = new SimpleAdapter(this.getActivity(), fillMaps, R.layout.card_view_legal_row,
								from, to);
						ListView lv = new ListView(this.getActivity());
						lv.setAdapter(adapter);

						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
						builder.setView(lv);
						builder.setTitle(R.string.card_view_legality);
						return builder.create();
					}
					case GET_PRICE: {
						if (mPriceInfo == null) {
							setShowsDialog(false);
							return null;
						}

						View v = getActivity().getLayoutInflater().inflate(R.layout.card_view_price_dialog,
								(ViewGroup) getActivity().findViewById(R.id.dialog_layout_root));

						assert v != null; /* Because Android Studio */
						TextView l = (TextView) v.findViewById(R.id.low);
						TextView m = (TextView) v.findViewById(R.id.med);
						TextView h = (TextView) v.findViewById(R.id.high);
						TextView f = (TextView) v.findViewById(R.id.foil);
						TextView priceLink = (TextView) v.findViewById(R.id.pricelink);

						l.setText(String.format("$%1$,.2f", mPriceInfo.mLow));
						m.setText(String.format("$%1$,.2f", mPriceInfo.mAverage));
						h.setText(String.format("$%1$,.2f", mPriceInfo.mHigh));

						if (mPriceInfo.mFoilAverage != 0) {
							f.setText(String.format("$%1$,.2f", mPriceInfo.mFoilAverage));
						}
						else {
							f.setVisibility(View.GONE);
							v.findViewById(R.id.foil_label).setVisibility(View.GONE);
						}
						priceLink.setMovementMethod(LinkMovementMethod.getInstance());
						priceLink.setText(ImageGetterHelper.formatHtmlString("<a href=\"" + mPriceInfo.mUrl + "\">" +
								getString(R.string.card_view_price_dialog_link) + "</a>"));

						AlertDialog.Builder adb = new AlertDialog.Builder(this.getActivity());
						adb.setView(v);
						adb.setTitle(R.string.card_view_price_dialog_title);
						return adb.create();
					}
					case CHANGE_SET: {
						final String[] aSets = mSets.toArray(new String[mSets.size()]);
						final Long[] aIds = mCardIds.toArray(new Long[mCardIds.size()]);
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
						builder.setTitle(R.string.card_view_set_dialog_title);
						builder.setItems(aSets, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialogInterface, int item) {
								try {
									setInfoFromID(aIds[item]);
								} catch (FamiliarDbException e) {
									handleFamiliarDbException(true);
								}
							}
						});
						return builder.create();
					}
					case CARD_RULINGS: {
						if (mRulingsArrayList == null) {
							setShowsDialog(false);
							return null;
						}
						ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getResources());

						View v = getActivity().getLayoutInflater().inflate(R.layout.card_view_rulings_dialog,
								(ViewGroup) getActivity().findViewById(R.id.dialog_layout_root));
						assert v != null; /* Because Android Studio */

						TextView textViewRules = (TextView) v.findViewById(R.id.rules);
						TextView textViewUrl = (TextView) v.findViewById(R.id.url);

						String message = "";
						if (mRulingsArrayList.size() == 0) {
							message = getString(R.string.card_view_no_rulings);
						}
						else {
							for (Ruling r : mRulingsArrayList) {
								message += (r.toString() + "<br><br>");
							}

							message = message.replace("{Tap}", "{T}");
						}
						CharSequence messageGlyph = ImageGetterHelper.formatStringWithGlyphs(message, imgGetter);

						textViewRules.setText(messageGlyph);

						textViewUrl.setMovementMethod(LinkMovementMethod.getInstance());
						textViewUrl.setText(Html.fromHtml(
								"<a href=http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" +
										mMultiverseId + ">" + getString(R.string.card_view_gatherer_page) + "</a>"));

						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
						builder.setTitle(R.string.card_view_rulings_dialog_title);
						builder.setView(v);
						return builder.create();
					}
					case WISH_LIST_COUNTS: {
						try {
							return WishlistHelpers.getDialog(mCardName, CardViewFragment.this, false);
						} catch (FamiliarDbException e) {
							handleFamiliarDbException(false);
							setShowsDialog(false);
							return null;
						}
					}
					case PROGRESS: {

						ProgressDialog progressDialog = new ProgressDialog(this.getActivity());
						progressDialog.setIndeterminate(true);
						progressDialog.setCancelable(true);
						progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
							public void onCancel(DialogInterface pd) {
								/* when the dialog is dismissed */
								if (mAsyncTask != null) {
									mAsyncTask.cancel(true);
								}
							}
						});

						return progressDialog;
					}
					default: {
						setShowsDialog(false);
						return null;
					}
				}
			}
		};
		newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
	}

	/**
	 * Called when a registered TextView is long-pressed. The menu inflated will give options to copy text
	 *
	 * @param menu     The context menu that is being built
	 * @param v        The view for which the context menu is being built
	 * @param menuInfo Extra information about the item for which the context menu should be shown. This information
	 *                 will vary depending on the class of v.
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);
		TextView tv = (TextView) v;

		assert tv.getText() != null;
		mCopyString = tv.getText().toString();

		android.view.MenuInflater inflater = this.getActivity().getMenuInflater();
		inflater.inflate(R.menu.copy_menu, menu);
	}

	/**
	 * Copies text to the clipboard
	 *
	 * @param item The context menu item that was selected.
	 * @return boolean Return false to allow normal context menu processing to proceed, true to consume it here.
	 */
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		String copyText;
		switch (item.getItemId()) {
			case R.id.copy: {
				copyText = mCopyString;
				break;
			}
			case R.id.copyall: {
				assert mNameTextView.getText() != null; /* Because Android Studio */
				assert mCostTextView.getText() != null;
				assert mTypeTextView.getText() != null;
				assert mSetTextView.getText() != null;
				assert mAbilityTextView.getText() != null;
				assert mFlavorTextView.getText() != null;
				assert mPowTouTextView.getText() != null;
				assert mArtistTextView.getText() != null;

				copyText = mNameTextView.getText().toString() + '\n' + mCostTextView.getText().toString() + '\n' +
						mTypeTextView.getText().toString() + '\n' + mSetTextView.getText().toString() + '\n' +
						mAbilityTextView.getText().toString() + '\n' + mFlavorTextView.getText().toString() + '\n' +
						mPowTouTextView.getText().toString() + '\n' + mArtistTextView.getText().toString();
				break;
			}
			default: {
				return super.onContextItemSelected(item);
			}
		}
		ClipboardManager clipboard = (ClipboardManager) (this.getActivity().
				getSystemService(android.content.Context.CLIPBOARD_SERVICE));
		String label = getResources().getString(R.string.app_name);
		String mimeTypes[] = {ClipDescription.MIMETYPE_TEXT_PLAIN};
		ClipData cd = new ClipData(label, mimeTypes, new ClipData.Item(copyText));
		clipboard.setPrimaryClip(cd);
		return true;
	}

	/**
	 * Handles clicks from the ActionBar
	 *
	 * @param item the item clicked
	 * @return true if acted upon, false if otherwise
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mCardName == null) {
			/*disable menu buttons if the card isn't initialized */
			return false;
		}
		/* Handle item selection */
		switch (item.getItemId()) {
			case R.id.image: {
				showDialog(PROGRESS);
				mAsyncTask = new FetchPictureTask();
				mAsyncTask.execute((Void[]) null);
				return true;
			}
			case R.id.price: {
				showDialog(PROGRESS);

				PriceFetchRequest priceRequest;
				priceRequest = new PriceFetchRequest(mCardName, mSetCode, mCardNumber, mMultiverseId,
						getActivity());
				getFamiliarActivity().mSpiceManager.execute(priceRequest, mCardName + "-" +
						mSetCode, DurationInMillis.ONE_DAY, new RequestListener<PriceInfo>() {

					@Override
					public void onRequestFailure(SpiceException spiceException) {
						CardViewFragment.this.removeDialog(getFragmentManager());
						Toast.makeText(getActivity(), spiceException.getMessage(), Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onRequestSuccess(final PriceInfo result) {
						if (result != null) {
							mPriceInfo = result;
							showDialog(GET_PRICE);
						}
						else {
							Toast.makeText(getActivity(), R.string.card_view_price_not_found, Toast.LENGTH_SHORT)
									.show();
						}
					}
				});

				return true;
			}
			case R.id.changeset: {
				showDialog(CHANGE_SET);
				return true;
			}
			case R.id.legality: {
				showDialog(PROGRESS);
				mAsyncTask = new FetchLegalityTask();
				mAsyncTask.execute((Void[]) null);
				return true;
			}
			case R.id.cardrulings: {
				showDialog(PROGRESS);
				mAsyncTask = new FetchRulingsTask();
				mAsyncTask.execute((Void[]) null);
				return true;
			}
			case R.id.addtowishlist: {
				showDialog(WISH_LIST_COUNTS);
				return true;
			}
			default: {
				return super.onOptionsItemSelected(item);
			}
		}
	}

	/**
	 * Inflate the ActionBar items
	 *
	 * @param menu     The options menu in which you place your items.
	 * @param inflater The inflater to use to inflate the menu
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.card_menu, menu);

		MenuItem mi;
		/* If the image has been loaded to the main page, remove the menu option for image */
		if (loadTo == MAIN_PAGE && mCardBitmap != null) {
			mi = menu.findItem(R.id.image);
			assert mi != null; /* Because Android Studio */
			menu.removeItem(mi.getItemId());
		}
		if (mSets != null && mSets.size() == 1) {
			mi = menu.findItem(R.id.changeset);
			assert mi != null; /* Because Android Studio */
			menu.removeItem(mi.getItemId());
		}
	}
}