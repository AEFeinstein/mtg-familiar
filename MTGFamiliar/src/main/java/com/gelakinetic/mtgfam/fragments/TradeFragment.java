package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class TradeFragment extends FamiliarFragment {

	/* Price Constants */
	private static final int LOW_PRICE = 0;
	private static final int AVG_PRICE = 1;
	private static final int HIGH_PRICE = 2;
	private static final int FOIL_PRICE = 3;

	/* Side Constants */
	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int BOTH = 2;

	/* Dialog Constants */
	private static final int DIALOG_UPDATE_CARD = 1;
	private static final int DIALOG_PRICE_SETTING = 2;
	private static final int DIALOG_SAVE_TRADE = 3;
	private static final int DIALOG_LOAD_TRADE = 4;
	private static final int DIALOG_DELETE_TRADE = 5;
	private static final int DIALOG_CONFIRMATION = 6;
	private static final int DIALOG_CHANGE_SET = 7;

	/* Save file constants */
	private static final String AUTOSAVE_NAME = "autosave";
	private static final String TRADE_EXTENSION = ".trade";

	/* Trade information */
	private TextView mTotalPriceLeft;
	private TradeListAdapter mLeftAdapter;
	private ArrayList<MtgCard> mLeftList;
	private TextView mTotalPriceRight;
	private TradeListAdapter mRightAdapter;
	private ArrayList<MtgCard> mRightList;

	/* UI Elements */
	private AutoCompleteTextView mNameEditText;
	private EditText mNumberEditText;
	private CheckBox mCheckboxFoil;

	/* Settings */
	private int mPriceSetting;
	private String mCurrentTrade = "";

	/**
	 * Initialize the view and set up the button actions
	 *
	 * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
	 * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
	 *                           fragment should not add the view itself, but this can be used to generate the
	 *                           LayoutParams of the view.
	 * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
	 *                           here.
	 * @return The inflated view
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		/* Inflate the view, pull out UI elements */
		View myFragmentView = inflater.inflate(R.layout.trader_frag, container, false);
		assert myFragmentView != null;
		mNameEditText = (AutoCompleteTextView) myFragmentView.findViewById(R.id.namesearch);
		mNumberEditText = (EditText) myFragmentView.findViewById(R.id.numberInput);
		mCheckboxFoil = (CheckBox) myFragmentView.findViewById(R.id.trader_foil);
		mTotalPriceRight = (TextView) myFragmentView.findViewById(R.id.priceTextRight);
		mTotalPriceLeft = (TextView) myFragmentView.findViewById(R.id.priceTextLeft);

		/* Set the autocomplete adapter, default number */
		mNameEditText.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME},
				new int[]{R.id.text1}, mNameEditText));
		mNumberEditText.setText("1");

		/* Initialize the left list */
		mLeftList = new ArrayList<MtgCard>();
		mLeftAdapter = new TradeListAdapter(this.getActivity(), R.layout.trader_row, mLeftList);
		ListView lvTradeLeft = (ListView) myFragmentView.findViewById(R.id.tradeListLeft);
		lvTradeLeft.setAdapter(mLeftAdapter);
		lvTradeLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				showDialog(DIALOG_UPDATE_CARD, LEFT, arg2);
			}
		});

		/* Initialize the right list */
		mRightList = new ArrayList<MtgCard>();
		mRightAdapter = new TradeListAdapter(this.getActivity(), R.layout.trader_row, mRightList);
		ListView lvTradeRight = (ListView) myFragmentView.findViewById(R.id.tradeListRight);
		lvTradeRight.setAdapter(mRightAdapter);
		lvTradeRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				showDialog(DIALOG_UPDATE_CARD, RIGHT, arg2);
			}
		});

		/* Set the buttons to add cards to the left or right */
		myFragmentView.findViewById(R.id.addCardLeft).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				addCardToTrade(LEFT);
			}
		});
		myFragmentView.findViewById(R.id.addCardRight).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				addCardToTrade(RIGHT);
			}
		});

		/* Return the view */
		return myFragmentView;
	}

	/**
	 * This helper method adds a card to a side of the wishlist from the user input
	 *
	 * @param side RIGHT or LEFT, depending on which side to add the card to
	 */
	private void addCardToTrade(int side) {
		/* Make sure there is something to add */
		if (mNameEditText.getText() == null || mNumberEditText.getText() == null) {
			return;
		}

		/* Get the card info from the UI */
		String cardName, setCode, tcgName, cardNumber;
		cardName = mNameEditText.getText().toString();
		String numberOfFromField = mNumberEditText.getText().toString();
		boolean foil = mCheckboxFoil.isChecked();

		/* Make sure it isn't the empty string */
		if (cardName.equals("") || numberOfFromField.equals("")) {
			return;
		}

		/* Parse the int after the "" check */
		int numberOf = Integer.parseInt(numberOfFromField);

		try {
			/* Get the rest of the relevant card info from the database */
			CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
			Cursor cardCursor = mDbHelper.fetchCardByName(cardName, new String[]{
					CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
					CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
					CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
					CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME_TCGPLAYER});

			/* Make sure there was a database hit */
			if (cardCursor.getCount() == 0) {
				Toast.makeText(TradeFragment.this.getActivity(), getString(R.string.toast_no_card), Toast.LENGTH_LONG)
						.show();
				return;
			}

			/* Read the information from the cursor, check if the card can be foil */
			setCode = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
			tcgName = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NAME_TCGPLAYER));
			cardNumber = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
			if (foil && !mDbHelper.canBeFoil(setCode)) {
				foil = false;
			}

			/* Clean up */
			cardCursor.close();
			mDbHelper.close();

			/* Create the card, add it to a list, start a price fetch */
			MtgCard data = new MtgCard(cardName, tcgName, setCode, numberOf, 0, getString(R.string.wishlist_loading),
					cardNumber, '-', false, foil);
			switch (side) {
				case LEFT: {
					mLeftList.add(0, data);
					mLeftAdapter.notifyDataSetChanged();
					loadPrice(data, mLeftAdapter);
					break;
				}
				case RIGHT: {
					mRightList.add(0, data);
					mRightAdapter.notifyDataSetChanged();
					loadPrice(data, mRightAdapter);
					break;
				}
			}

			/* Return the input fields to defaults */
			mNameEditText.setText("");
			mNumberEditText.setText("1");
			mCheckboxFoil.setChecked(false);

		} catch (FamiliarDbException e) {
			/* Something went wrong, but it's not worth quitting */
			handleFamiliarDbException(false);
		}
	}

	/**
	 * When the fragment resumes, get the price preference again and attempt to load the autosave trade
	 */
	@Override
	public void onResume() {
		super.onResume();
		mPriceSetting = Integer.parseInt(getFamiliarActivity().mPreferenceAdapter.getTradePrice());
		/* Try to load the autosave trade, the function will handle FileNotFoundException */
		LoadTrade(AUTOSAVE_NAME + TRADE_EXTENSION);
	}

	/**
	 * WHen the fragment pauses, save the trade and cancel all pending price requests
	 */
	@Override
	public void onPause() {
		super.onPause();
		SaveTrade(AUTOSAVE_NAME + TRADE_EXTENSION);
	}

	/**
	 * Remove any showing dialogs, and show the requested one
	 *
	 * @param id                The ID of the dialog to show
	 * @param sideForDialog     If this is for a specific card, this is the side of the trade the card lives in.
	 * @param positionForDialog If this is for a specific card, this is the position of the card in the list.
	 */
	void showDialog(final int id, final int sideForDialog, final int positionForDialog) {
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
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				/* We're setting this to false if we return null, so we should reset it every time to be safe */
				setShowsDialog(true);
				switch (id) {
					case DIALOG_UPDATE_CARD: {
						/* Get some final references */
						final ArrayList<MtgCard> lSide = (sideForDialog == LEFT ? mLeftList : mRightList);
						final TradeListAdapter aaSide = (sideForDialog == LEFT ? mLeftAdapter : mRightAdapter);
						final boolean foil = lSide.get(positionForDialog).foil;

						/* Inflate the view and pull out UI elements */
						View view = LayoutInflater.from(getActivity()).inflate(R.layout.trader_card_click_dialog, null);
						assert view != null;
						final CheckBox foilCheckbox = (CheckBox) view.findViewById(R.id.traderDialogFoil);
						final EditText numberOf = (EditText) view.findViewById(R.id.traderDialogNumber);
						final EditText priceText = (EditText) view.findViewById(R.id.traderDialogPrice);

						/* Set initial values */
						String numberOfStr = String.valueOf(lSide.get(positionForDialog).numberOf);
						numberOf.setText(numberOfStr);
						numberOf.setSelection(numberOfStr.length());
						foilCheckbox.setChecked(foil);
						final String priceNumberStr = lSide.get(positionForDialog).hasPrice() ?
								lSide.get(positionForDialog).getPriceString().substring(1) : "";
						priceText.setText(priceNumberStr);
						priceText.setSelection(priceNumberStr.length());

						/* Set up the button to remove this card from the trade */
						view.findViewById(R.id.traderDialogRemove).setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								lSide.remove(positionForDialog);
								aaSide.notifyDataSetChanged();
								UpdateTotalPrices(sideForDialog);
								removeDialog(getFragmentManager());
							}
						});

						/* If this has a custom price, show the button to default the price */
						if (!lSide.get(positionForDialog).customPrice) {
							view.findViewById(R.id.traderDialogResetPrice).setVisibility(View.GONE);
						}
						else {
							view.findViewById(R.id.traderDialogResetPrice).setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View v) {
									lSide.get(positionForDialog).customPrice = false;
									priceText.setText("TODO"); // TODO avoid query, set correct price string
									loadPrice(lSide.get(positionForDialog), aaSide);

									aaSide.notifyDataSetChanged();
									UpdateTotalPrices(sideForDialog);
								}
							});
						}

						/* Set up the button to show info about this card */
						view.findViewById(R.id.traderDialogInfo).setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								try {
									Bundle args = new Bundle();
									/* Get the card ID, and send it to a new CardViewFragment */
									CardDbAdapter adapter = new CardDbAdapter(getActivity());
									Cursor cursor = adapter.fetchCardByNameAndSet(lSide.get(positionForDialog).name,
											lSide.get(positionForDialog).setCode, new String[]{
													CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID}
									);
									args.putLong(CardViewFragment.CARD_ID, cursor.getLong(
											cursor.getColumnIndex(CardDbAdapter.KEY_ID)));
									cursor.close();
									adapter.close();
									CardViewFragment cvFrag = new CardViewFragment();
									TradeFragment.this.startNewFragment(cvFrag, args);
								} catch (FamiliarDbException e) {
									TradeFragment.this.handleFamiliarDbException(false);
								}
							}
						});

						/* Set up the button to change the set of this card */
						view.findViewById(R.id.traderDialogChangeSet).setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								showDialog(DIALOG_CHANGE_SET, sideForDialog, positionForDialog);
							}
						});

						return new AlertDialog.Builder(this.getActivity())
								.setTitle(lSide.get(positionForDialog).name)
								.setView(view)
								.setPositiveButton(R.string.dialog_done, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										/* validate number of cards text */
										if (numberOf.length() == 0) {
											return;
										}

										/* validate the price text */
										assert priceText.getText() != null;
										String userInputPrice = priceText.getText().toString();

										/* Check if the user hand-modified the price */
										if (!userInputPrice.equals(priceNumberStr)) {
											lSide.get(positionForDialog).customPrice = true;
										}

										/* If the input price is blank, reset it */
										if (userInputPrice.length() == 0) {
											lSide.get(positionForDialog).customPrice = false;
											loadPrice(lSide.get(positionForDialog), aaSide); // TODO avoid a query?
										}

										/* Set this card's foil option, then make sure it can be foil */
										lSide.get(positionForDialog).foil = foilCheckbox.isChecked();
										try {
											CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
											if (lSide.get(positionForDialog).foil && !mDbHelper.canBeFoil(
													lSide.get(positionForDialog).setCode)) {
												lSide.get(positionForDialog).foil = false;
											}
											mDbHelper.close();
										} catch (FamiliarDbException e) {
											/* I guess it is foil after all */
										}

										/* If this card changed foil value, and it doesn't have a custom price,
										 * reload the price. */
										if (!lSide.get(positionForDialog).customPrice &&
												foil != foilCheckbox.isChecked()) {
											loadPrice(lSide.get(positionForDialog), aaSide);
										}

										/* Attempt to parse the price */
										double uIP;
										try {
											uIP = Double.parseDouble(userInputPrice);
											/* Clear the message so the user's specified price will display */
											lSide.get(positionForDialog).message = "";
										} catch (NumberFormatException e) {
											uIP = 0;
										}

										/* Set the price */
										lSide.get(positionForDialog).price = ((int) Math.round(uIP * 100));

										/* Set the numberOf */
										assert numberOf.getEditableText() != null;
										String numberOfString = numberOf.getEditableText().toString();
										try {
											lSide.get(positionForDialog).numberOf = (Integer.parseInt(numberOfString));
										} catch (NumberFormatException e) {
											lSide.get(positionForDialog).numberOf = 1;
										}

										/* Notify things to update */
										aaSide.notifyDataSetChanged();
										UpdateTotalPrices(sideForDialog);
									}
								})
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										dialogInterface.dismiss();
									}
								})
								.create();
					}
					case DIALOG_CHANGE_SET: {
						/* Get the card */
						MtgCard data = (sideForDialog == LEFT ?
								mLeftList.get(positionForDialog) : mRightList.get(positionForDialog));

						try {
							/* Query the database for all versions of this card */
							CardDbAdapter mDbHelper = new CardDbAdapter(TradeFragment.this.getActivity());
							Cursor cards = mDbHelper.fetchCardByName(data.name, new String[]{
									CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
									CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
									CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME_TCGPLAYER});
							/* Build set names and set codes */
							Set<String> sets = new LinkedHashSet<String>();
							Set<String> setCodes = new LinkedHashSet<String>();
							while (!cards.isAfterLast()) {
								if (sets.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME_TCGPLAYER)))) {
									setCodes.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET)));
								}
								cards.moveToNext();
							}
							/* clean up */
							cards.close();
							mDbHelper.close();

							/* Turn set names and set codes into arrays */
							final String[] aSets = sets.toArray(new String[sets.size()]);
							final String[] aSetCodes = setCodes.toArray(new String[setCodes.size()]);

							/* Build and return the dialog */
							return new AlertDialog.Builder(getActivity())
									.setTitle(R.string.card_view_set_dialog_title)
									.setItems(aSets, new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialogInterface, int item) {
											/* Change the card's information, and reload the price */
											if (sideForDialog == LEFT) {
												mLeftList.get(positionForDialog).setCode = (aSetCodes[item]);
												mLeftList.get(positionForDialog).tcgName = (aSets[item]);
												mLeftList.get(positionForDialog).message = (getString(R.string.wishlist_loading));
												mLeftAdapter.notifyDataSetChanged();
												loadPrice(mLeftList.get(positionForDialog), mLeftAdapter);
											}
											else if (sideForDialog == RIGHT) {
												mRightList.get(positionForDialog).setCode = (aSetCodes[item]);
												mRightList.get(positionForDialog).tcgName = (aSets[item]);
												mRightList.get(positionForDialog).message = (getString(R.string.wishlist_loading));
												mRightAdapter.notifyDataSetChanged();
												loadPrice(mRightList.get(positionForDialog), mRightAdapter);
											}
										}
									})
									.create();
						} catch (FamiliarDbException e) {
							/* Don't show the dialog, but pop a toast */
							setShowsDialog(false);
							handleFamiliarDbException(true);
							return null;
						}
					}
					case DIALOG_PRICE_SETTING: {
						/* Build the dialog with some choices */
						return new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.trader_pricing_dialog_title)
								.setSingleChoiceItems(new String[]{getString(R.string.trader_Low),
												getString(R.string.trader_Average),
												getString(R.string.trader_High)}, mPriceSetting,
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
												if (mPriceSetting != which) {
													mPriceSetting = which;

													/* Update ALL the prices! */
													for (MtgCard data : mLeftList) {
														if (!data.customPrice) {
															data.message = getString(R.string.wishlist_loading);
															loadPrice(data, mLeftAdapter); // TODO avoid requery
														}
													}
													mLeftAdapter.notifyDataSetChanged();

													for (MtgCard data : mRightList) {
														if (!data.customPrice) {
															data.message = getString(R.string.wishlist_loading);
															loadPrice(data, mRightAdapter); // TODO avoid requery
														}
													}
													mRightAdapter.notifyDataSetChanged();

													/* And also update the preference */
													getFamiliarActivity().mPreferenceAdapter.setTradePrice(String.valueOf(mPriceSetting));
												}
												dialog.dismiss();
											}
										}
								).create();
					}
					case DIALOG_SAVE_TRADE: {
						/* Inflate a view to type in the trade's name, and show it in an AlertDialog */
						View textEntryView = getActivity().getLayoutInflater()
								.inflate(R.layout.alert_dialog_text_entry, null);
						assert textEntryView != null;
						final EditText nameInput = (EditText) textEntryView.findViewById(R.id.text_entry);
						nameInput.append(mCurrentTrade);
						/* Set the button to clear the text field */
						textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								nameInput.setText("");
							}
						});

						Dialog dialog = new AlertDialog.Builder(getActivity())
								.setTitle(R.string.trader_save_dialog_title)
								.setView(textEntryView)
								.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										if (nameInput.getText() == null) {
											return;
										}
										String tradeName = nameInput.getText().toString();

										/* Don't bother saving if there is no name */
										if (tradeName.length() == 0 || tradeName.equals("")) {
											return;
										}

										SaveTrade(tradeName + TRADE_EXTENSION);
										mCurrentTrade = tradeName;
									}
								})
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										dialog.dismiss();
									}
								})
								.create();
						dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
						return dialog;
					}
					case DIALOG_LOAD_TRADE: {
						/* Find all the trade files */
						String[] files = this.getActivity().fileList();
						ArrayList<String> validFiles = new ArrayList<String>();
						for (String fileName : files) {
							if (fileName.endsWith(TRADE_EXTENSION)) {
								validFiles.add(fileName.substring(0, fileName.indexOf(TRADE_EXTENSION)));
							}
						}

						/* If there are no files, don't show the dialog */
						if (validFiles.size() == 0) {
							Toast.makeText(this.getActivity(), R.string.trader_toast_no_trades, Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}

						/* Make an array of the trade file names */
						final String[] tradeNames = new String[validFiles.size()];
						validFiles.toArray(tradeNames);

						return new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.trader_select_dialog_title)
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										/* Canceled. */
										dialog.dismiss();
									}
								})
								.setItems(tradeNames, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface di, int which) {
										/* Load the trade, set the current trade name */
										LoadTrade(tradeNames[which] + TRADE_EXTENSION);
										mCurrentTrade = tradeNames[which];

										/* Alert things to update */
										mLeftAdapter.notifyDataSetChanged();
										mRightAdapter.notifyDataSetChanged();
										UpdateTotalPrices(BOTH);
									}
								})
								.create();
					}
					case DIALOG_DELETE_TRADE: {
						/* Find all the trade files */
						String[] files = this.getActivity().fileList();
						ArrayList<String> validFiles = new ArrayList<String>();
						for (String fileName : files) {
							if (fileName.endsWith(TRADE_EXTENSION)) {
								validFiles.add(fileName.substring(0, fileName.indexOf(TRADE_EXTENSION)));
							}
						}

						/* If there are no files, don't show the dialog */
						if (validFiles.size() == 0) {
							Toast.makeText(this.getActivity(), R.string.trader_toast_no_trades, Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}

						/* Make an array of the trade file names */
						final String[] tradeNames = new String[validFiles.size()];
						validFiles.toArray(tradeNames);

						return new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.trader_delete_dialog_title)
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										/* Canceled. */
										dialog.dismiss();
									}
								})
								.setItems(tradeNames, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface di, int which) {
										File toDelete = new File(getActivity().getFilesDir(), tradeNames[which] + TRADE_EXTENSION);
										toDelete.delete();
									}
								})
								.create();
					}
					case DIALOG_CONFIRMATION: {
						return new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.trader_clear_dialog_title)
								.setMessage(R.string.trader_clear_dialog_text)
								.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										/* Clear the arrays and tell everything to update */
										mRightList.clear();
										mLeftList.clear();
										mRightAdapter.notifyDataSetChanged();
										mLeftAdapter.notifyDataSetChanged();
										UpdateTotalPrices(BOTH);
										dialog.dismiss();
									}
								})
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										/* Canceled */
										dialog.dismiss();
									}
								})
								.setCancelable(true)
								.create();
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

	protected void SaveTrade(String _tradeName) { // TODO clean from here
		FileOutputStream fos;

		try {
			/* MODE_PRIVATE will create the file (or replace a file of the */
			/* same name) */
			fos = this.getActivity().openFileOutput(_tradeName, Context.MODE_PRIVATE);

			for (MtgCard cd : mLeftList) {
				fos.write(cd.toString(LEFT).getBytes());
			}
			for (MtgCard cd : mRightList) {
				fos.write(cd.toString(RIGHT).getBytes());
			}

			fos.close();
		} catch (FileNotFoundException e) {
			Toast.makeText(this.getActivity(), R.string.trader_toast_save_error, Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(this.getActivity(), R.string.trader_toast_save_error, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(this.getActivity(), R.string.trader_toast_invalid_chars, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * @param _tradeName
	 */
	protected void LoadTrade(String _tradeName) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.getActivity().openFileInput(_tradeName)));

			mLeftList.clear();
			mRightList.clear();

			String line;
			while ((line = br.readLine()) != null) {
				try {
					MtgCard card = MtgCard.MtgCardFromTradeString(line, getActivity());

					if (card.mSide == LEFT) {
						mLeftList.add(card);
						if (!card.customPrice)
							loadPrice(card, mLeftAdapter);
					}
					else if (card.mSide == RIGHT) {
						mRightList.add(card);
						if (!card.customPrice)
							loadPrice(card, mRightAdapter);
					}
				} catch (FamiliarDbException e) {
					handleFamiliarDbException(false);
				}
			}
		} catch (FileNotFoundException e) {
			/* Do nothing, the autosave doesn't exist */
		} catch (IOException e) {
			Toast.makeText(this.getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
		UpdateTotalPrices(BOTH);
	}

	/**
	 * @param side
	 */
	private void UpdateTotalPrices(int side) {
		if (side == LEFT || side == BOTH) {
			int totalPriceLeft = GetPricesFromTradeList(mLeftList);
			int color = PriceListHasBadValues(mLeftList) ?
					this.getActivity().getResources().getColor(R.color.holo_red_dark) :
					this.getActivity().getResources().getColor(R.color.black);
			mTotalPriceLeft.setText(String.format("$%d.%02d", totalPriceLeft / 100, totalPriceLeft % 100));
			mTotalPriceLeft.setTextColor(color);
		}
		if (side == RIGHT || side == BOTH) {
			int totalPriceRight = GetPricesFromTradeList(mRightList);
			int color = PriceListHasBadValues(mRightList) ?
					this.getActivity().getResources().getColor(R.color.holo_red_dark) :
					this.getActivity().getResources().getColor(R.color.black);
			mTotalPriceRight.setText(String.format("$%d.%02d", totalPriceRight / 100, totalPriceRight % 100));
			mTotalPriceRight.setTextColor(color);
		}

	}

	/**
	 * @param trade
	 * @return
	 */
	private boolean PriceListHasBadValues(ArrayList<MtgCard> trade) {
		for (MtgCard data : trade) {
			if (!data.hasPrice()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param _trade
	 * @return
	 */
	private int GetPricesFromTradeList(ArrayList<MtgCard> _trade) {
		int totalPrice = 0;
		for (MtgCard data : _trade) {
			if (data.hasPrice()) {
				totalPrice += data.numberOf * data.price;
			}
		}
		return totalPrice;
	}

	/**
	 * @param item
	 * @return
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* Handle item selection */
		switch (item.getItemId()) {
			case R.id.trader_menu_clear:
				showDialog(DIALOG_CONFIRMATION, 0, 0);
				return true;
			case R.id.trader_menu_settings:
				showDialog(DIALOG_PRICE_SETTING, 0, 0);
				return true;
			case R.id.trader_menu_save:
				showDialog(DIALOG_SAVE_TRADE, 0, 0);
				return true;
			case R.id.trader_menu_load:
				showDialog(DIALOG_LOAD_TRADE, 0, 0);
				return true;
			case R.id.trader_menu_delete:
				showDialog(DIALOG_DELETE_TRADE, 0, 0);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * @param menu     The options menu in which you place your items.
	 * @param inflater The inflater to use to inflate the menu
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.trader_menu, menu);
	}

	/**
	 * TODO
	 */
	private class TradeListAdapter extends ArrayAdapter<MtgCard> {

		private int layoutResourceId;
		private ArrayList<MtgCard> items;

		/**
		 * @param context
		 * @param textViewResourceId
		 * @param items
		 */
		public TradeListAdapter(Context context, int textViewResourceId, ArrayList<MtgCard> items) {
			super(context, textViewResourceId, items);

			this.layoutResourceId = textViewResourceId;
			this.items = items;
		}

		/**
		 * @param position
		 * @param convertView
		 * @param parent
		 * @return
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(layoutResourceId, null);
			}
			MtgCard data = items.get(position);
			if (data != null) {
				assert convertView != null;
				TextView nameField = (TextView) convertView.findViewById(R.id.traderRowName);
				TextView setField = (TextView) convertView.findViewById(R.id.traderRowSet);
				TextView numberField = (TextView) convertView.findViewById(R.id.traderNumber);
				TextView priceField = (TextView) convertView.findViewById(R.id.traderRowPrice);
				ImageView foilField = (ImageView) convertView.findViewById(R.id.traderRowFoil);

				nameField.setText(data.name);
				setField.setText(data.tcgName);
				numberField.setText(data.hasPrice() ? data.numberOf + "x" : "");
				priceField.setText(data.hasPrice() ? data.getPriceString() : data.message);
				foilField.setVisibility((data.foil ? View.VISIBLE : View.GONE));

				if (data.hasPrice()) {
					if (data.customPrice) {
						priceField.setTextColor(getActivity().getResources().getColor(R.color.holo_green_dark));
					}
					else {
						priceField.setTextColor(getActivity().getResources().getColor(R.color.black));
					}
				}
				else {
					priceField.setTextColor(getActivity().getResources().getColor(R.color.holo_red_dark));
				}
			}
			return convertView;
		}
	}

	/**
	 * @param data
	 * @param adapter
	 */
	private void loadPrice(final MtgCard data, final TradeListAdapter adapter) {
		PriceFetchRequest priceRequest = new PriceFetchRequest(data.name, data.setCode, data.number, -1, getActivity());
		getFamiliarActivity().mSpiceManager.execute(priceRequest, data.name + "-" + data.setCode, DurationInMillis.ONE_DAY, new RequestListener<PriceInfo>() {
			/**
			 *
			 * @param spiceException
			 */
			@Override
			public void onRequestFailure(SpiceException spiceException) {
				data.message = spiceException.getLocalizedMessage();
				data.priceInfo = null;
			}

			/**
			 *
			 * @param result
			 */
			@Override
			public void onRequestSuccess(final PriceInfo result) {
				if (result == null) {
					data.priceInfo = null;
				}
				else {
					data.priceInfo = result;

					if (data.foil) {
						data.price = (int) (result.mFoilAverage * 100);
					}
					else {
						switch (mPriceSetting) {
							case LOW_PRICE: {
								data.price = (int) (result.mLow * 100);
								break;
							}
							default:
							case AVG_PRICE: {
								data.price = (int) (result.mAverage * 100);
								break;
							}
							case HIGH_PRICE: {
								data.price = (int) (result.mHigh * 100);
								break;
							}
							case FOIL_PRICE: {
								data.price = (int) (result.mFoilAverage * 100);
								break;
							}
						}
					}
					data.message = null;
				}
				UpdateTotalPrices(BOTH);
				adapter.notifyDataSetChanged();
			}
		});
	}
}