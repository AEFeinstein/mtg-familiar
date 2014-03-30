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
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
import com.gelakinetic.mtgfam.helpers.TradeListHelpers;
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

	/* CONSTANTS */
	private static final int LOW_PRICE = 0;
	private static final int AVG_PRICE = 1;
	private static final int HIGH_PRICE = 2;
	private static final int FOIL_PRICE = 3;

	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int BOTH = 2;

	private final static int DIALOG_UPDATE_CARD = 1;
	private final static int DIALOG_PRICE_SETTING = 2;
	private final static int DIALOG_SAVE_TRADE = 3;
	private final static int DIALOG_LOAD_TRADE = 4;
	private final static int DIALOG_DELETE_TRADE = 5;
	private final static int DIALOG_CONFIRMATION = 6;

	private static final String AUTOSAVE_NAME = "autosave";
	private static final String TRADE_EXTENSION = ".trade";

	/* Trade information */
	private TextView tradePriceLeft;
	private TradeListAdapter aaTradeLeft;
	private ArrayList<MtgCard> lTradeLeft;
	private TextView tradePriceRight;
	private TradeListAdapter aaTradeRight;
	private ArrayList<MtgCard> lTradeRight;

	/* UI Elements */
	private AutoCompleteTextView mNameEditText;
	private EditText mNumberEditText;
	private CheckBox mCheckboxFoil;

	/* TODO sort */
	private int sideForDialog = LEFT;
	private int positionForDialog;
	private int priceSetting;
	private String currentTrade = "";
	private boolean doneLoading = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View myFragmentView = inflater.inflate(R.layout.trader_frag, container, false);

		assert myFragmentView != null;
		mNameEditText = (AutoCompleteTextView) myFragmentView.findViewById(R.id.namesearch);
		mNameEditText.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME}, new int[]{R.id.text1}, mNameEditText));

		mNumberEditText = (EditText) myFragmentView.findViewById(R.id.numberInput);
		mNumberEditText.setText("1");

		mCheckboxFoil = (CheckBox) myFragmentView.findViewById(R.id.trader_foil);

		lTradeLeft = new ArrayList<MtgCard>();
		Button bAddTradeLeft = (Button) myFragmentView.findViewById(R.id.addCardLeft);
		tradePriceLeft = (TextView) myFragmentView.findViewById(R.id.priceTextLeft);
		ListView lvTradeLeft = (ListView) myFragmentView.findViewById(R.id.tradeListLeft);
		aaTradeLeft = new TradeListAdapter(this.getActivity(), R.layout.trader_row, lTradeLeft);
		lvTradeLeft.setAdapter(aaTradeLeft);

		lTradeRight = new ArrayList<MtgCard>();
		Button bAddTradeRight = (Button) myFragmentView.findViewById(R.id.addCardRight);
		tradePriceRight = (TextView) myFragmentView.findViewById(R.id.priceTextRight);
		ListView lvTradeRight = (ListView) myFragmentView.findViewById(R.id.tradeListRight);
		aaTradeRight = new TradeListAdapter(this.getActivity(), R.layout.trader_row, lTradeRight);
		lvTradeRight.setAdapter(aaTradeRight);

		bAddTradeLeft.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				addCardToTrade(LEFT);
			}
		});

		bAddTradeRight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				addCardToTrade(RIGHT);
			}
		});

		lvTradeLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				sideForDialog = LEFT;
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});
		lvTradeRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				sideForDialog = RIGHT;
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});

		return myFragmentView;
	}

	/**
	 * @param side
	 */
	private void addCardToTrade(int side) {
		String cardName, setCode, tcgName, cardNumber;
		cardName = mNameEditText.getText().toString();
		String numberOfFromField = mNumberEditText.getText().toString();
		boolean foil = mCheckboxFoil.isChecked();

		if (cardName == null || cardName.equals("") || numberOfFromField == null || numberOfFromField.equals("")) {
			return;
		}

		int numberOf = Integer.parseInt(numberOfFromField);

		try {
			CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
			Cursor cardCursor = mDbHelper.fetchCardByName(cardName, new String[]{
					CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
					CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER,
					CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_RARITY,
					CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME_TCGPLAYER});

			if (cardCursor.getCount() == 0) {
				Toast.makeText(TradeFragment.this.getActivity(), getString(R.string.toast_no_card),
						Toast.LENGTH_LONG).show();
				return;
			}
			setCode = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
			tcgName = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NAME_TCGPLAYER));
			cardNumber = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
			if (foil && !TradeListHelpers.canBeFoil(setCode, mDbHelper)) {
				foil = false;
			}

			cardCursor.close();
			mDbHelper.close();

			final MtgCard data = new MtgCard(cardName, tcgName, setCode, numberOf, 0, "loading", cardNumber, '-', false, foil);

			switch (side) {
				case LEFT: {
					lTradeLeft.add(0, data);
					aaTradeLeft.notifyDataSetChanged();
					loadPrice(data, aaTradeLeft);
					break;
				}
				case RIGHT: {
					lTradeRight.add(0, data);
					aaTradeRight.notifyDataSetChanged();
					loadPrice(data, aaTradeRight);
					break;
				}
			}

			mNameEditText.setText("");
			mNumberEditText.setText("1");
			mCheckboxFoil.setChecked(false);

		} catch (FamiliarDbException e) {
			handleFamiliarDbException(false);
		}
	}

	/**
	 * TODO
	 */
	@Override
	public void onResume() {
		super.onResume();

		priceSetting = Integer.parseInt(getFamiliarActivity().mPreferenceAdapter.getTradePrice());

		try {
			// Test to see if the autosave file exist, then load the trade it if does.
			this.getActivity().openFileInput(AUTOSAVE_NAME + TRADE_EXTENSION);
			LoadTrade(AUTOSAVE_NAME + TRADE_EXTENSION);
		} catch (FileNotFoundException e) {
			// Do nothing if the file doesn't exist, but mark it as loaded otherwise prices won't update
			doneLoading = true;
		}
	}

	/**
	 * TODO
	 */
	@Override
	public void onPause() {
		super.onPause();
		SaveTrade(AUTOSAVE_NAME + TRADE_EXTENSION);
		getFamiliarActivity().mSpiceManager.cancelAllRequests();
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
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				setShowsDialog(true); // We're setting this to false if we return null, so we should reset it every time to be safe
				switch (id) {
					case DIALOG_UPDATE_CARD: {
						final int position = positionForDialog;
						final int side = sideForDialog;
						final ArrayList<MtgCard> lSide = (sideForDialog == LEFT ? lTradeLeft : lTradeRight);
						final TradeListAdapter aaSide = (sideForDialog == LEFT ? aaTradeLeft : aaTradeRight);
						final int numberOfCards = lSide.get(position).numberOf;
						final String priceOfCard = lSide.get(position).getPriceString();
						final boolean foil = lSide.get(position).foil;

						View view = LayoutInflater.from(getActivity()).inflate(R.layout.trader_card_click_dialog, null);
						Button removeAll = (Button) view.findViewById(R.id.traderDialogRemove);
						Button resetPrice = (Button) view.findViewById(R.id.traderDialogResetPrice);
						Button info = (Button) view.findViewById(R.id.traderDialogInfo);
						Button changeSet = (Button) view.findViewById(R.id.traderDialogChangeSet);
						final CheckBox foilbtn = (CheckBox) view.findViewById(R.id.traderDialogFoil);
						final EditText numberOf = (EditText) view.findViewById(R.id.traderDialogNumber);
						final EditText priceText = (EditText) view.findViewById(R.id.traderDialogPrice);

						String numberOfStr = String.valueOf(numberOfCards);
						numberOf.setText(numberOfStr);
						numberOf.setSelection(numberOfStr.length());
						foilbtn.setChecked(foil);

						final String priceNumberStr = lSide.get(position).hasPrice() ? priceOfCard.substring(1) : "";
						priceText.setText(priceNumberStr);
						priceText.setSelection(priceNumberStr.length());

						removeAll.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								lSide.remove(position);
								aaSide.notifyDataSetChanged();
								UpdateTotalPrices(side);
								removeDialog(getFragmentManager());
							}
						});

						if (!lSide.get(position).customPrice) {
							resetPrice.setVisibility(View.GONE);
						}
						else {
							resetPrice.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View v) {
									lSide.get(position).customPrice = false;
									loadPrice(lSide.get(position), aaSide);
									aaSide.notifyDataSetChanged();
									UpdateTotalPrices(side);
									removeDialog(getFragmentManager());
								}
							});
						}

						info.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								try {
									Bundle args = new Bundle();
									/* Get the card ID, and send it to a new CardViewFragment */
									CardDbAdapter adapter = new CardDbAdapter(getActivity());
									Cursor cursor = adapter.fetchCardByNameAndSet(lSide.get(position).name, lSide.get(position).setCode, new String[]{CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID});
									args.putLong(CardViewFragment.CARD_ID, cursor.getLong(cursor.getColumnIndex(CardDbAdapter.KEY_ID)));
									CardViewFragment cvFrag = new CardViewFragment();
									TradeFragment.this.startNewFragment(cvFrag, args);
									cursor.close();
									adapter.close();
								} catch (FamiliarDbException e) {
									TradeFragment.this.handleFamiliarDbException(false);
								}
							}
						});

						changeSet.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								removeDialog(getFragmentManager());
								try {
									MtgCard data = (side == LEFT ? lTradeLeft.get(position) : lTradeRight.get(position));
									String name = data.name;

									CardDbAdapter mDbHelper = new CardDbAdapter(TradeFragment.this.getActivity());
									Cursor cards = mDbHelper.fetchCardByName(name, new String[]{CardDbAdapter.KEY_SET});
									Set<String> sets = new LinkedHashSet<String>();
									Set<String> setCodes = new LinkedHashSet<String>();
									while (!cards.isAfterLast()) {
										if (sets.add(mDbHelper.getTCGname(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET))))) {
											setCodes.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET)));
										}
										cards.moveToNext();
									}
									cards.close();
									mDbHelper.close();

									final String[] aSets = sets.toArray(new String[sets.size()]);
									final String[] aSetCodes = setCodes.toArray(new String[setCodes.size()]);
									AlertDialog.Builder builder = new AlertDialog.Builder(TradeFragment.this.getActivity());
									builder.setTitle(R.string.card_view_set_dialog_title);
									builder.setItems(aSets, new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialogInterface, int item) {
											if (side == LEFT) {
												lTradeLeft.get(position).setCode = (aSetCodes[item]);
												lTradeLeft.get(position).tcgName = (aSets[item]);
												lTradeLeft.get(position).message = ("loading");
												aaTradeLeft.notifyDataSetChanged();
												loadPrice(lTradeLeft.get(position), aaTradeLeft);
											}
											else if (side == RIGHT) {
												lTradeRight.get(position).setCode = (aSetCodes[item]);
												lTradeRight.get(position).tcgName = (aSets[item]);
												lTradeRight.get(position).message = ("loading");
												aaTradeRight.notifyDataSetChanged();
												loadPrice(lTradeRight.get(position), aaTradeRight);
											}
										}
									});
									builder.create().show();
								} catch (FamiliarDbException e) {
									handleFamiliarDbException(true);
								}
							}
						});

						return new AlertDialog.Builder(this.getActivity())
								.setTitle(lSide.get(position).name)
								.setView(view)
								.setPositiveButton(R.string.dialog_done, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										// validate number of cards text
										if (numberOf.length() == 0) {
											return;
										}

										// validate the price text
										String userInputPrice = priceText.getText().toString();

										if (!userInputPrice.equals(priceNumberStr)) {
											lSide.get(position).customPrice = true;
										}

										//Hack to regrab price, if the set price is blank.
										if (userInputPrice.length() == 0) {
											lSide.get(position).customPrice = false;
											loadPrice(lSide.get(position), aaSide);
										}

										lSide.get(position).foil = foilbtn.isChecked();
										try {
											CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
											if (lSide.get(position).foil && !TradeListHelpers.canBeFoil(lSide.get(position).setCode, mDbHelper)) {
												lSide.get(position).foil = false;
											}
											mDbHelper.close();
										} catch (FamiliarDbException e) {
											/* eat it */
										}
										if (foil != foilbtn.isChecked()) {
											loadPrice(lSide.get(position), aaSide);
										}

										double uIP;
										try {
											uIP = Double.parseDouble(userInputPrice);
											// Clear the message so the user's specified price will display
											lSide.get(position).message = "";
										} catch (NumberFormatException e) {
											uIP = 0;
										}

										lSide.get(position).numberOf = (Integer.parseInt(numberOf.getEditableText().toString()));
										lSide.get(position).price = ((int) Math.round(uIP * 100));
										aaSide.notifyDataSetChanged();
										UpdateTotalPrices(side);

										removeDialog(getFragmentManager());
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
					case DIALOG_PRICE_SETTING: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

						return builder
								.setTitle(R.string.trader_pricing_dialog_title)
								.setSingleChoiceItems(new String[]{getString(R.string.trader_Low), getString(R.string.trader_Average), getString(R.string.trader_High)}, priceSetting,
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
												priceSetting = which;
												dialog.dismiss();

												// Update ALL the prices!
												for (MtgCard data : lTradeLeft) {
													if (!data.customPrice) {
														data.message = "loading";
														loadPrice(data, aaTradeLeft);
													}
												}
												aaTradeLeft.notifyDataSetChanged();

												for (MtgCard data : lTradeRight) {
													if (!data.customPrice) {
														data.message = "loading";
														loadPrice(data, aaTradeRight);
													}
												}
												aaTradeRight.notifyDataSetChanged();

												// And also update the preference
												getFamiliarActivity().mPreferenceAdapter.setTradePrice(String.valueOf(priceSetting));

												removeDialog(getFragmentManager());
											}
										}
								).create();
					}
					case DIALOG_SAVE_TRADE: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

						builder.setTitle(R.string.trader_save_dialog_title);
						builder.setMessage(R.string.trader_save_dialog_text);

						// Set an EditText view to get user input
						final EditText input = new EditText(this.getActivity());
						input.setText(currentTrade);
						input.setSingleLine(true);
						builder.setView(input);

						builder.setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String tradeName = input.getText().toString();

								String FILENAME = tradeName + TRADE_EXTENSION;
								SaveTrade(FILENAME);

								currentTrade = tradeName;
							}
						});

						builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Canceled.
							}
						});

						Dialog dialog = builder.create();
						dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
						return dialog;
					}
					case DIALOG_LOAD_TRADE: {
						String[] files = this.getActivity().fileList();
						ArrayList<String> validFiles = new ArrayList<String>();
						for (String fileName : files) {
							if (fileName.endsWith(TRADE_EXTENSION)) {
								validFiles.add(fileName.substring(0, fileName.indexOf(TRADE_EXTENSION)));
							}
						}

						Dialog dialog;
						if (validFiles.size() == 0) {
							Toast.makeText(this.getActivity(), R.string.trader_toast_no_trades, Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}

						final String[] tradeNames = new String[validFiles.size()];
						validFiles.toArray(tradeNames);

						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
						builder.setTitle(R.string.trader_select_dialog_title);
						builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Canceled.
							}
						});
						builder.setItems(tradeNames, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface di, int which) {

								LoadTrade(tradeNames[which] + TRADE_EXTENSION);

								currentTrade = tradeNames[which];

								aaTradeLeft.notifyDataSetChanged();
								aaTradeRight.notifyDataSetChanged();
							}
						});

						dialog = builder.create();
						return dialog;
					}
					case DIALOG_DELETE_TRADE: {
						String[] files = getActivity().fileList();
						ArrayList<String> validFiles = new ArrayList<String>();
						for (String fileName : files) {
							if (fileName.endsWith(TRADE_EXTENSION)) {
								validFiles.add(fileName.substring(0, fileName.indexOf(TRADE_EXTENSION)));
							}
						}

						if (validFiles.size() == 0) {
							Toast.makeText(this.getActivity(), R.string.trader_toast_no_trades, Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}

						final String[] tradeNamesD = new String[validFiles.size()];
						validFiles.toArray(tradeNamesD);

						return new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.trader_delete_dialog_title)
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										// Canceled.
									}
								})
								.setItems(tradeNamesD, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface di, int which) {
										File toDelete = new File(getActivity().getFilesDir(), tradeNamesD[which] + TRADE_EXTENSION);
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
										lTradeRight.clear();
										aaTradeRight.notifyDataSetChanged();
										lTradeLeft.clear();
										aaTradeLeft.notifyDataSetChanged();
										UpdateTotalPrices(BOTH);
										dialog.dismiss();
									}
								}).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								}).setCancelable(true).create();
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
	 *
	 * @param _tradeName
	 */
	protected void SaveTrade(String _tradeName) {
		FileOutputStream fos;

		try {
			// MODE_PRIVATE will create the file (or replace a file of the
			// same name)
			fos = this.getActivity().openFileOutput(_tradeName, Context.MODE_PRIVATE);

			for (MtgCard cd : lTradeLeft) {
				fos.write(cd.toString(LEFT).getBytes());
			}
			for (MtgCard cd : lTradeRight) {
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
	 *
	 * @param _tradeName
	 */
	protected void LoadTrade(String _tradeName) {
		try {
			doneLoading = false;
			BufferedReader br = new BufferedReader(new InputStreamReader(this.getActivity().openFileInput(_tradeName)));

			lTradeLeft.clear();
			lTradeRight.clear();

			String line;
			while ((line = br.readLine()) != null) {
				try {
					MtgCard card = MtgCard.MtgCardFromTradeString(line, getActivity());

					if (card.mSide == LEFT) {
						lTradeLeft.add(card);
						if (!card.customPrice)
							loadPrice(card, aaTradeLeft);
					}
					else if (card.mSide == RIGHT) {
						lTradeRight.add(card);
						if (!card.customPrice)
							loadPrice(card, aaTradeRight);
					}
				} catch (FamiliarDbException e) {
					handleFamiliarDbException(false);
				}
			}
		} catch (IOException e) {
			Toast.makeText(this.getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
		doneLoading = true;
		UpdateTotalPrices(BOTH);
	}

	/**
	 * @param side
	 */
	private void UpdateTotalPrices(int side) {
		if (doneLoading) {
			if (side == LEFT || side == BOTH) {
				int totalPriceLeft = GetPricesFromTradeList(lTradeLeft);
				int color = PriceListHasBadValues(lTradeLeft) ?
						this.getActivity().getResources().getColor(R.color.holo_red_dark) :
						this.getActivity().getResources().getColor(R.color.black);
				tradePriceLeft.setText(String.format("$%d.%02d", totalPriceLeft / 100, totalPriceLeft % 100));
				tradePriceLeft.setTextColor(color);
			}
			if (side == RIGHT || side == BOTH) {
				int totalPriceRight = GetPricesFromTradeList(lTradeRight);
				int color = PriceListHasBadValues(lTradeRight) ?
						this.getActivity().getResources().getColor(R.color.holo_red_dark) :
						this.getActivity().getResources().getColor(R.color.black);
				tradePriceRight.setText(String.format("$%d.%02d", totalPriceRight / 100, totalPriceRight % 100));
				tradePriceRight.setTextColor(color);
			}
		}
	}

	/**
	 *
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
	 *
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
	 *
	 * @param item
	 * @return
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.trader_menu_clear:
				showDialog(DIALOG_CONFIRMATION);
				return true;
			case R.id.trader_menu_settings:
				showDialog(DIALOG_PRICE_SETTING);
				return true;
			case R.id.trader_menu_save:
				showDialog(DIALOG_SAVE_TRADE);
				return true;
			case R.id.trader_menu_load:
				showDialog(DIALOG_LOAD_TRADE);
				return true;
			case R.id.trader_menu_delete:
				showDialog(DIALOG_DELETE_TRADE);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 *
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
		 *
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
		 *
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
	 *
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
						switch (priceSetting) {
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