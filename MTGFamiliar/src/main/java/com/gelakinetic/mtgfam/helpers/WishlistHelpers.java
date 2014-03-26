package com.gelakinetic.mtgfam.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class WishlistHelpers {
	private static final String wishlistName = "card.wishlist";

	private static void WriteWishlist(Context mCtx, ArrayList<MtgCard> lWishlist) {
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


	public static void WriteCompressedWishlist(Context mCtx, ArrayList<CompressedWishlistInfo> mCompressedWishlist) {
		try {
			FileOutputStream fos = mCtx.openFileOutput(wishlistName, Context.MODE_PRIVATE);

			for (CompressedWishlistInfo cwi : mCompressedWishlist) {
				MtgCard card = cwi.mCard;
				for (IndividualSetInfo isi : cwi.mInfo) {
					card.set = isi.mSets;
					card.setCode = isi.mSetCodes;
					card.number = isi.mNumber;
					card.foil = isi.mIsFoil;
					card.numberOf = isi.mNumberOf;
					fos.write(card.toString().getBytes());
				}
			}

			fos.close();
		} catch (FileNotFoundException e) {
			Toast.makeText(mCtx, "FileNotFoundException", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
		}
	}

	public static void ResetCards(Context mCtx) {

		String[] files = mCtx.fileList();
		for (String fileName : files) {
			if (fileName.equals(wishlistName)) {
				mCtx.deleteFile(fileName);
			}
		}
	}

	public static ArrayList<MtgCard> ReadWishlist(Context mCtx) {

		ArrayList<MtgCard> lWishlist = new ArrayList<MtgCard>();

		/* If it does, read it into the arrayList */
		try {
			String line;
			String[] parts;
			BufferedReader br = new BufferedReader(new InputStreamReader(mCtx.openFileInput(wishlistName)));

			while ((line = br.readLine()) != null) {
				parts = line.split(MtgCard.delimiter);

				MtgCard card = new MtgCard();
				card.name = parts[0];
				card.setCode = parts[1];
				card.numberOf = Integer.parseInt(parts[2]);

				/* "foil" didn't exist in earlier versions, so it may not be part of the string */
				if (parts.length > 3) {
					card.number = parts[3];
				}
				if (parts.length > 4) {
					card.rarity = (char) Integer.parseInt(parts[4]);
				}
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
			/* Catches file not found exception when wishlist doesn't exist */
		}
		return lWishlist;
	}

	public static Dialog getDialog(final String mCardName, final FamiliarFragment fragment, boolean showCardButton) throws FamiliarDbException {

		final Context ctx = fragment.getActivity();

		/* Create the custom view */
		CardDbAdapter adapter = new CardDbAdapter(ctx);
		Cursor cards = adapter.fetchCardByName(mCardName, new String[]{
				CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
				CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
				CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
				CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME_TCGPLAYER});

		View customView = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog, null);
		assert customView != null;

		final LinearLayout linearLayout = (LinearLayout) customView.findViewById(R.id.linear_layout);

		if (showCardButton) {
			customView.findViewById(R.id.show_card_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Bundle args = new Bundle();
					try {
						CardDbAdapter adapter = new CardDbAdapter(ctx);
						args.putLong(CardViewFragment.CARD_ID, adapter.fetchIdByName(mCardName));
						CardViewFragment cvFrag = new CardViewFragment();
						fragment.startNewFragment(cvFrag, args);
						adapter.close();
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

		/* Get all potential sets for this card */
		final ArrayList<String> potentialSetCodes = new ArrayList<String>();
		final ArrayList<Character> potentialRarities = new ArrayList<Character>();
		cards.moveToFirst();
		while (!cards.isAfterLast()) {
			String setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
			String setName = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME_TCGPLAYER));
			char rarity = (char) cards.getInt(cards.getColumnIndex(CardDbAdapter.KEY_RARITY));

			View wishlistRow = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row, null);
			assert wishlistRow != null;
			((TextView) wishlistRow.findViewById(R.id.cardset)).setText(setName);
			((EditText) wishlistRow.findViewById(R.id.numberInput)).setText("0");
			wishlistRow.findViewById(R.id.wishlistDialogFoil).setVisibility(View.GONE);
			linearLayout.addView(wishlistRow);
			potentialSetCodes.add(setCode);
			potentialRarities.add(rarity);

			View wishlistRowFoil = null;
			if (TradeListHelpers.canBeFoil(setCode, adapter)) {
				wishlistRowFoil = fragment.getActivity().getLayoutInflater().inflate(R.layout.wishlist_dialog_row, null);
				assert wishlistRowFoil != null;
				((TextView) wishlistRowFoil.findViewById(R.id.cardset)).setText(setName);
				((EditText) wishlistRowFoil.findViewById(R.id.numberInput)).setText("0");
				wishlistRowFoil.findViewById(R.id.wishlistDialogFoil).setVisibility(View.VISIBLE);
				linearLayout.addView(wishlistRowFoil);
				potentialSetCodes.add(setCode);
				potentialRarities.add(rarity);
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

	public static class IndividualSetInfo {
		public String mSets;
		public String mSetCodes;
		public String mNumber;

		public Boolean mIsFoil;
		public PriceInfo mPrice;
		public String mMessage;
		public Integer mNumberOf;
		public Character mRarity;
	}

	public static class CompressedWishlistInfo {
		public final MtgCard mCard;
		public final ArrayList<IndividualSetInfo> mInfo;

		public CompressedWishlistInfo(MtgCard card) {
			mInfo = new ArrayList<IndividualSetInfo>();
			mCard = card;
			add(mCard);
		}

		public void add(MtgCard card) {
			IndividualSetInfo isi = new IndividualSetInfo();

			isi.mSets = card.tcgName;
			isi.mSetCodes = card.setCode;
			isi.mNumber = card.number;
			isi.mIsFoil = card.foil;
			isi.mPrice = new PriceInfo();
			isi.mMessage = card.message;
			isi.mNumberOf = card.numberOf;
			isi.mRarity = card.rarity;

			mInfo.add(isi);
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

		public void clearCompressedInfo() {
			mInfo.clear();
		}
	}

	public static String GetSharableWishlist(ArrayList<CompressedWishlistInfo> mCompressedWishlist, Context ctx) {
		StringBuilder readableWishlist = new StringBuilder();

		for (CompressedWishlistInfo cwi : mCompressedWishlist) {
			for (IndividualSetInfo isi : cwi.mInfo) {
				readableWishlist
						.append(isi.mNumberOf)
						.append("x ")
						.append(cwi.mCard.name)
						.append(", ")
						.append(isi.mSets);
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
