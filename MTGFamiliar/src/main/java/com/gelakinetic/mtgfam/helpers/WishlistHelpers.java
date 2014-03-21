package com.gelakinetic.mtgfam.helpers;

import android.app.Dialog;
import android.content.Context;
import android.widget.Toast;

import com.gelakinetic.mtgfam.helpers.TradeListHelpers.CardData;

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
	public static void WriteWishlist(Context mCtx, ArrayList<CardData> lWishlist) {
		try {
			FileOutputStream fos = mCtx.openFileOutput(wishlistName,
					Context.MODE_PRIVATE);

			for (int i = lWishlist.size() - 1; i >= 0; i--) {
				fos.write(lWishlist.get(i).toString().getBytes());
			}
			fos.close();
		} catch (FileNotFoundException e) {
			Toast.makeText(mCtx, "FileNotFoundException", Toast.LENGTH_LONG)
					.show();
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
	 * @param mDbHelper
	 * @param lWishlist
	 * @throws FamiliarDbException
	 */
	public static void ReadWishlist(Context mCtx, CardDbAdapter mDbHelper, ArrayList<CardData> lWishlist) throws FamiliarDbException {
		String[] files = mCtx.fileList();
		Boolean wishlistExists = false;
		for (String fileName : files) {
			if (fileName.equals(wishlistName)) {
				wishlistExists = true;
			}
		}
		if (wishlistExists) {
			lWishlist.clear();
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						mCtx.openFileInput(wishlistName)));

				String line;
				String[] parts;
				TradeListHelpers tradeListHelper = new TradeListHelpers();
				while ((line = br.readLine()) != null) {
					parts = line.split(CardData.delimiter);

					String cardName = parts[0];
					String cardSet = parts[1];
					String tcgName = "";
					try {
						tcgName = mDbHelper.getTCGname(cardSet);
					} catch (Exception e) {
					}
					int numberOf = Integer.parseInt(parts[2]);
					String number = parts.length < 4 ? null : parts[3];
					int rarity = parts.length < 5 ? '-' : Integer
							.parseInt(parts[4]);
					boolean foil = parts.length >= 6 && Boolean.parseBoolean(parts[5]);

					CardData cd = tradeListHelper.new CardData(cardName,
							tcgName, cardSet, numberOf, 0, "loading", number,
							rarity);
					cd.setIsFoil(foil);
					if (rarity == '-' || number == null)
						cd = TradeListHelpers.FetchCardData(cd, mDbHelper);
					lWishlist.add(0, cd);
				}
			} catch (NumberFormatException e) {
				Toast.makeText(mCtx, "NumberFormatException", Toast.LENGTH_LONG)
						.show();
			} catch (IOException e) {
				Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Return a dialog showing all the sets for a card, and which ones exist in the wishlist
	 * @return
	 */
	public Dialog getDialogForCard() {
		return null;
	}
}
