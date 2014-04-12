package com.gelakinetic.mtgfam.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This class has helpers used for reading, writing, and modifying the wishlist from different fragments
 */
public class WishlistHelpers {

	/* The name of the wishlist file */
	private static final String WISHLIST_NAME = "card.wishlist";

	/**
	 * Write the wishlist passed as a parameter to the wishlist file
	 *
	 * @param mCtx      A context to open the file and pop toasts with
	 * @param lWishlist The wishlist to write to the file
	 */
	private static void WriteWishlist(Context mCtx, ArrayList<MtgCard> lWishlist) {
		try {
			FileOutputStream fos = mCtx.openFileOutput(WISHLIST_NAME, Context.MODE_PRIVATE);

			for (MtgCard m : lWishlist) {
				fos.write(m.toWishlistString().getBytes());
			}

			fos.close();
		} catch (FileNotFoundException e) {
			Toast.makeText(mCtx, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(mCtx, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Write the wishlist passed as a parameter to the wishlist file
	 *
	 * @param mCtx                A context to open the file and pop toasts with
	 * @param mCompressedWishlist The wishlist to write to the file
	 */
	public static void WriteCompressedWishlist(Context mCtx, ArrayList<CompressedWishlistInfo> mCompressedWishlist) {
		try {
			FileOutputStream fos = mCtx.openFileOutput(WISHLIST_NAME, Context.MODE_PRIVATE);

			/* For each compressed card, make an MtgCard and write it to the wishlist */
			for (CompressedWishlistInfo cwi : mCompressedWishlist) {
				MtgCard card = cwi.mCard;
				for (IndividualSetInfo isi : cwi.mInfo) {
					card.set = isi.mSet;
					card.setCode = isi.mSetCode;
					card.number = isi.mNumber;
					card.foil = isi.mIsFoil;
					card.numberOf = isi.mNumberOf;
					fos.write(card.toWishlistString().getBytes());
				}
			}

			fos.close();
		} catch (FileNotFoundException e) {
			Toast.makeText(mCtx, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(mCtx, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Delete the wishlist
	 *
	 * @param mCtx A context to open the file wish
	 */
	public static void ResetCards(Context mCtx) {

		String[] files = mCtx.fileList();
		for (String fileName : files) {
			if (fileName.equals(WISHLIST_NAME)) {
				mCtx.deleteFile(fileName);
			}
		}
	}

	/**
	 * Read the wishlist from a file and return it as an ArrayList<MtgCard>
	 *
	 * @param mCtx A context to open the file and pop toasts with
	 * @return The wishlist in ArrayList form
	 */
	public static ArrayList<MtgCard> ReadWishlist(Context mCtx) {

		ArrayList<MtgCard> lWishlist = new ArrayList<MtgCard>();

		try {
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(mCtx.openFileInput(WISHLIST_NAME)));
			/* Read each line as a card, and add them to the ArrayList */
			while ((line = br.readLine()) != null) {
				lWishlist.add(new MtgCard(line, mCtx));
			}
		} catch (NumberFormatException e) {
			Toast.makeText(mCtx, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			/* Catches file not found exception when wishlist doesn't exist */
		}
		return lWishlist;
	}

	/**
	 * Return a dialog in which a user can specify how many of what set of a card are in the wishlist
	 *
	 * @param mCardName      The name of the card
	 * @param fragment       The fragment which hosts the dialog and receives onWishlistChanged()
	 * @param showCardButton Whether the button to launch the CardViewFragment should be shown
	 * @return A dialog which edits the wishlist
	 * @throws com.gelakinetic.mtgfam.helpers.database.FamiliarDbException
	 */
	public static Dialog getDialog(final String mCardName, final FamiliarFragment fragment, boolean showCardButton)
			throws FamiliarDbException {

		final Context ctx = fragment.getActivity();

		/* Create the custom view */
		View customView = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog,
				null, false);
		assert customView != null;

		/* Grab the linear layout. Make it final to be accessible from the button later */
		final LinearLayout linearLayout = (LinearLayout) customView.findViewById(R.id.linear_layout);

		/* If the button should be shown, show it and attach a listener */
		if (showCardButton) {
			customView.findViewById(R.id.show_card_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Bundle args = new Bundle();
					try {
						/* Open the database */
						SQLiteDatabase db = DatabaseManager.getInstance().openDatabase(false);
						/* Get the card ID, and send it to a new CardViewFragment */
						args.putLong(CardViewFragment.CARD_ID, CardDbAdapter.fetchIdByName(mCardName, db));
						DatabaseManager.getInstance().closeDatabase();
						CardViewFragment cvFrag = new CardViewFragment();
						fragment.startNewFragment(cvFrag, args);
					} catch (FamiliarDbException e) {
						fragment.handleFamiliarDbException(false);
					}
				}
			});
		}
		else {
			customView.findViewById(R.id.show_card_button).setVisibility(View.GONE);
			customView.findViewById(R.id.divider).setVisibility(View.GONE);
		}

		/* Read the wishlist */
		ArrayList<MtgCard> wishlist = ReadWishlist(ctx);

		/* Get all potential sets and rarities for this card */
		final ArrayList<String> potentialSetCodes = new ArrayList<String>();
		final ArrayList<Character> potentialRarities = new ArrayList<Character>();
		final ArrayList<String> potentialNumbers = new ArrayList<String>();

		/* Open the database */
		SQLiteDatabase db = DatabaseManager.getInstance().openDatabase(false);

		/* Get all the cards with relevant info from the database */
		Cursor cards = CardDbAdapter.fetchCardByName(mCardName, new String[]{
				CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
				CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
				CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
				CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
				CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME_TCGPLAYER}, db);

		/* For each card, add it to the wishlist view */
		while (!cards.isAfterLast()) {
			String setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
			String setName = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME_TCGPLAYER));
			char rarity = (char) cards.getInt(cards.getColumnIndex(CardDbAdapter.KEY_RARITY));
			String number = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));

			/* Inflate a row and fill it with stuff */
			View wishlistRow = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row, null, false);
			assert wishlistRow != null;
			((TextView) wishlistRow.findViewById(R.id.cardset)).setText(setName);
			((EditText) wishlistRow.findViewById(R.id.numberInput)).setText("0");
			wishlistRow.findViewById(R.id.wishlistDialogFoil).setVisibility(View.GONE);
			linearLayout.addView(wishlistRow);
			potentialSetCodes.add(setCode);
			potentialRarities.add(rarity);
			potentialNumbers.add(number);

			/* If this card has a foil version, add that too */
			View wishlistRowFoil = null;
			if (CardDbAdapter.canBeFoil(setCode, db)) {
				wishlistRowFoil = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row,
						null, false);
				assert wishlistRowFoil != null;
				((TextView) wishlistRowFoil.findViewById(R.id.cardset)).setText(setName);
				((EditText) wishlistRowFoil.findViewById(R.id.numberInput)).setText("0");
				wishlistRowFoil.findViewById(R.id.wishlistDialogFoil).setVisibility(View.VISIBLE);
				linearLayout.addView(wishlistRowFoil);
				potentialSetCodes.add(setCode);
				potentialRarities.add(rarity);
				potentialNumbers.add(number);
			}

			/* Set any counts currently in the wishlist */
			for (MtgCard card : wishlist) {
				if (card.name.equals(mCardName) && card.setCode.equals(setCode)) {
					if (card.foil && wishlistRowFoil != null) {
						((EditText) wishlistRowFoil.findViewById(R.id.numberInput)).setText(card.numberOf + "");
					}
					else {
						((EditText) wishlistRow.findViewById(R.id.numberInput)).setText(card.numberOf + "");
					}
				}
			}

			cards.moveToNext();
		}

		/* Clean up */
		cards.close();
		DatabaseManager.getInstance().closeDatabase();

		/* make and return the actual dialog */
		return new AlertDialog.Builder(ctx)
				.setTitle(mCardName + " " + fragment.getString(R.string.wishlist_edit_dialog_title_end))
				.setView(customView)
				.setPositiveButton(fragment.getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {

						/* Read the wishlist */
						ArrayList<MtgCard> wishlist = ReadWishlist(ctx);

						/* Add the cards listed in the dialog to the wishlist */
						for (int i = 0; i < linearLayout.getChildCount(); i++) {
							View view = linearLayout.getChildAt(i);
							assert view != null;

							/* build the card object */
							MtgCard card = new MtgCard();
							card.name = mCardName;
							card.setCode = potentialSetCodes.get(i);
							try {
								EditText numberInput = ((EditText) view.findViewById(R.id.numberInput));
								assert numberInput.getText() != null;
								card.numberOf = Integer.valueOf(numberInput.getText().toString());
							} catch (NumberFormatException e) {
								card.numberOf = 0;
							}
							card.foil = (view.findViewById(R.id.wishlistDialogFoil).getVisibility() == View.VISIBLE);
							card.rarity = potentialRarities.get(i);
							card.number = potentialNumbers.get(i);

							/* Look through the wishlist for each card, set the numberOf or remove it if it exists, or
							 * add the card if it doesn't */
							boolean added = false;
							for (int j = 0; j < wishlist.size(); j++) {
								if (card.name.equals(wishlist.get(j).name)
										&& card.setCode.equals(wishlist.get(j).setCode)
										&& card.foil == wishlist.get(j).foil) {
									if (card.numberOf == 0) {
										wishlist.remove(j);
										j--;
									}
									else {
										wishlist.get(j).numberOf = card.numberOf;
									}
									added = true;
								}
							}
							if (!added && card.numberOf > 0) {
								wishlist.add(card);
							}

						}

						/* Write the wishlist */
						WriteWishlist(fragment.getActivity(), wishlist);
						/* notify the fragment of a change in the wishlist */
						fragment.onWishlistChanged(mCardName);
					}
				})
				.setNegativeButton(fragment.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						/* Abort! */
						dialogInterface.dismiss();
					}
				})
				.create();
	}

	/**
	 * This class encapsulates all non-duplicated information for two cards in different sets
	 */
	public static class IndividualSetInfo {
		public String mSet;
		public String mSetCode;
		public String mNumber;

		public Boolean mIsFoil;
		public PriceInfo mPrice;
		public String mMessage;
		public Integer mNumberOf;
		public Character mRarity;
	}

	/**
	 * This class encapsulates a single MtgCard and an ArrayList of non-duplicated information for different printings
	 * of that card
	 */
	public static class CompressedWishlistInfo {
		public final MtgCard mCard;
		public final ArrayList<IndividualSetInfo> mInfo;

		/**
		 * Constructor
		 *
		 * @param card The MtgCard which will be the base for this object
		 */
		public CompressedWishlistInfo(MtgCard card) {
			mInfo = new ArrayList<IndividualSetInfo>();
			mCard = card;
			add(mCard);
		}

		/**
		 * Add a new printing of a MtgCard to this object
		 *
		 * @param card The new printing to add to this object
		 */
		public void add(MtgCard card) {
			IndividualSetInfo isi = new IndividualSetInfo();

			isi.mSet = card.tcgName;
			isi.mSetCode = card.setCode;
			isi.mNumber = card.number;
			isi.mIsFoil = card.foil;
			isi.mPrice = null;
			isi.mMessage = card.message;
			isi.mNumberOf = card.numberOf;
			isi.mRarity = card.rarity;

			mInfo.add(isi);
		}

		/**
		 * Check to see if two CompressedWishlistInfo objects are equivalent, or if this is equivalent to a MtgCard
		 * object. The comparison is done on the MtgCard's name
		 *
		 * @param o The object to compare to this one
		 * @return true if the specified object is equal to this string, false otherwise.
		 */
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

		/**
		 * Clear all the different printings for this object
		 */
		public void clearCompressedInfo() {
			mInfo.clear();
		}
	}

	/**
	 * Take a wishlist and turn it into plaintext so that it can be shared via email or whatever
	 *
	 * @param mCompressedWishlist The wishlist to share
	 * @param ctx                 The context to get localized strings with
	 * @return A string containing all the wishlist data
	 */
	public static String GetSharableWishlist(ArrayList<CompressedWishlistInfo> mCompressedWishlist, Context ctx) {
		StringBuilder readableWishlist = new StringBuilder();

		for (CompressedWishlistInfo cwi : mCompressedWishlist) {
			for (IndividualSetInfo isi : cwi.mInfo) {
				readableWishlist
						.append(isi.mNumberOf)
						.append("x ")
						.append(cwi.mCard.name)
						.append(", ")
						.append(isi.mSet);
				if (isi.mIsFoil) {
					readableWishlist
							.append(" (")
							.append(ctx.getString(R.string.wishlist_foil))
							.append(")");
				}
				readableWishlist.append("\r\n");
			}
		}
		return readableWishlist.toString();
	}
}
