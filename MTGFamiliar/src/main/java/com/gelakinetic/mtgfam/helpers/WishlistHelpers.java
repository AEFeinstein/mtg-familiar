package com.gelakinetic.mtgfam.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class WishlistHelpers {
	private static final String wishlistName = "card.wishlist";

	public static final int DONE = 1;
	public static final int CANCEL = 2;

	/**
	 * Write the wishlist to a file
	 *
	 * @param mCtx
	 * @param lWishlist
	 */
	public static void WriteWishlist(Context mCtx, ArrayList<MtgCard> lWishlist) {
		try {
			FileOutputStream fos = mCtx.openFileOutput(wishlistName, Context.MODE_PRIVATE);

			for (MtgCard m : lWishlist) {
				fos.write(m.toString().getBytes());
			}

			fos.close();
		} catch (FileNotFoundException e) {
			Toast.makeText(mCtx, "FileNotFoundException", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Clear the wishlist, i.e. delete it
	 *
	 * @param mCtx
	 */
	public static void ResetCards(Context mCtx) {

		String[] files = mCtx.fileList();
		Boolean wishlistExists = false;
		for (String fileName : files) {
			if (fileName.equals(wishlistName)) {
				mCtx.deleteFile(fileName);
			}
		}
	}

	/**
	 * Read the wishlist from a file
	 *
	 * @param mCtx
	 * @throws FamiliarDbException
	 */
	public static ArrayList<MtgCard> ReadWishlist(Context mCtx) throws FamiliarDbException {

		CardDbAdapter mDbHelper = new CardDbAdapter(mCtx);
		ArrayList<MtgCard> lWishlist = new ArrayList<MtgCard>();

		/* Make sure the wishlist exists */
		String[] files = mCtx.fileList();
		Boolean wishlistExists = false;
		for (String fileName : files) {
			if (fileName.equals(wishlistName)) {
				wishlistExists = true;
			}
		}

		/* If it does, read it into the arrayList */
		if (wishlistExists) {
			try {
				String line;
				String[] parts;
				BufferedReader br = new BufferedReader(new InputStreamReader(mCtx.openFileInput(wishlistName)));

				while ((line = br.readLine()) != null) {
					parts = line.split(MtgCard.delimiter);

					MtgCard card = TradeListHelpers.FetchMtgCard(mCtx, parts[0], parts[1]);
					card.numberOf = Integer.parseInt(parts[2]);

					/* Parts [3] and [4] are collectors number and rarity, which are populated by the db call */
					/* "foil" didn't exist in earlier versions, so it may not be part of the string */
					boolean foil = false;
					if (parts.length > 5) {
						foil = Boolean.parseBoolean(parts[5]);
					}
					card.foil = foil;
					card.message = mCtx.getString(R.string.wishlist_loading);

					lWishlist.add(card);
				}
			} catch (NumberFormatException e) {
				Toast.makeText(mCtx, "NumberFormatException", Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
			}
		}
		mDbHelper.close();
		return lWishlist;
	}

	/**
	 * Return a dialog showing all the sets for a card, and which ones exist in the wishlist
	 *
	 * @param mCardName
	 * @param fragment
	 * @return
	 */
	public static Dialog getDialog(final String mCardName, final FamiliarFragment fragment) throws FamiliarDbException {

		final Context ctx = fragment.getActivity();

		CardDbAdapter adapter = new CardDbAdapter(ctx);
		Cursor cards = adapter.fetchCardByName(mCardName, new String[]{CardDbAdapter.KEY_ID, CardDbAdapter.KEY_SET});

		ScrollView scrollView = new ScrollView(ctx);
		final LinearLayout linearLayout = new LinearLayout(ctx);
		linearLayout.setOrientation(LinearLayout.VERTICAL);

		scrollView.addView(linearLayout);

		/* Read the wishlist */
		final ArrayList<MtgCard> wishlist = ReadWishlist(ctx);

		/* Get all potential sets for this card */
		final ArrayList<String> potentialSetCodes = new ArrayList<String>();
		cards.moveToFirst();
		while (!cards.isAfterLast()) {
			String setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
			String setName = adapter.getTCGname(setCode);

			View wishlistRow = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row, null);
			((TextView) wishlistRow.findViewById(R.id.cardset)).setText(setName);
			((EditText) wishlistRow.findViewById(R.id.numberInput)).setText("0");
			wishlistRow.findViewById(R.id.wishlistDialogFoil).setVisibility(View.GONE);
			linearLayout.addView(wishlistRow);
			potentialSetCodes.add(setCode);

			View wishlistRowFoil = null;
			if (TradeListHelpers.canBeFoil(setCode, adapter)) {
				wishlistRowFoil = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row, null);
				((TextView) wishlistRowFoil.findViewById(R.id.cardset)).setText(setName);
				((EditText) wishlistRowFoil.findViewById(R.id.numberInput)).setText("0");
				wishlistRowFoil.findViewById(R.id.wishlistDialogFoil).setVisibility(View.VISIBLE);
				linearLayout.addView(wishlistRowFoil);
				potentialSetCodes.add(setCode);
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

		cards.close();
		adapter.close();

		// make the dialog
		return new AlertDialog.Builder(ctx)
				.setTitle(mCardName + " " + fragment.getString(R.string.wishlist_edit_dialog_title_end))
				.setView(scrollView)
				.setPositiveButton(fragment.getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						/* Remove any instances of the current card from the wishlist */
						for (int i = 0; i < wishlist.size(); i++) {
							if (wishlist.get(i).name.equals(mCardName)) {
								wishlist.remove(i);
								i--;
							}
						}

						/* Add the cards listed in the dialog to the wishlist */
						for (int i = 0; i < linearLayout.getChildCount(); i++) {
							View view = linearLayout.getChildAt(i);
							int numberField;
							try {
								numberField = Integer.valueOf(((EditText) view.findViewById(R.id.numberInput)).getText().toString());
							} catch (NumberFormatException e) {
								numberField = 0;
							}

							if (numberField > 0) {
								int visibility = view.findViewById(R.id.wishlistDialogFoil).getVisibility();
								MtgCard card = new MtgCard();
								card.name = mCardName;
								card.setCode = potentialSetCodes.get(i);
								card.numberOf = numberField;
								card.foil = (visibility == View.VISIBLE);

								wishlist.add(card);
							}
						}

						/* Write the wishlist */
						WriteWishlist(fragment.getActivity(), wishlist);
						/* notify the fragment of a change in the wishlist */
						fragment.onWishlistChanged();
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
}
