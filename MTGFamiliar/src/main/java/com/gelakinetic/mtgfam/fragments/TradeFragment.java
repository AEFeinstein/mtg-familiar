package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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

	public static final int LEFT = 0;
	public static final int RIGHT = 1;

	private final static int DIALOG_UPDATE_CARD = 1;
	private final static int DIALOG_PRICE_SETTING = 2;
	private final static int DIALOG_SAVE_TRADE = 3;
	private final static int DIALOG_LOAD_TRADE = 4;
	private final static int DIALOG_DELETE_TRADE = 5;
	private final static int DIALOG_CONFIRMATION = 6;
	private String sideForDialog;
	private int positionForDialog;

	/* Trade information */
	private TextView tradePriceLeft;
	private TradeListAdapter aaTradeLeft;
	private ArrayList<MtgCard> lTradeLeft;
	private TextView tradePriceRight;
	private TradeListAdapter aaTradeRight;
	private ArrayList<MtgCard> lTradeRight;

	/* UI Elements */
	private AutoCompleteTextView namefield;
	private EditText numberfield;
	private CheckBox checkboxFoil;

	private int priceSetting;

	private String currentTrade;

	public static final String card_not_found = "Card Not Found";
	public static final String mangled_url = "Mangled URL";
	public static final String database_busy = "Database Busy";
	public static final String fetch_failed = "Fetch Failed";
	public static final String number_of_invalid = "Number of Cards Invalid";
	public static final String card_corrupted = "Card Data corrupted, discarding.";
	public static final String cannot_be_foil = "Removing Foil - Card set does not match those printed for foil.";

	private static final String autosaveName = "autosave";
	private static final String TRADE_EXTENSION = ".trade";
	private boolean doneLoading = false;

	private static final int LOW_PRICE = 0;
	private static final int AVG_PRICE = 1;
	private static final int HIGH_PRICE = 2;
	private static final int FOIL_PRICE = 3;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View myFragmentView = inflater.inflate(R.layout.trader_frag, container, false);

		assert myFragmentView != null;
		namefield = (AutoCompleteTextView) myFragmentView.findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME}, new int[]{R.id.text1}, namefield));

		numberfield = (EditText) myFragmentView.findViewById(R.id.numberInput);
		numberfield.setText("1");

		checkboxFoil = (CheckBox) myFragmentView.findViewById(R.id.trader_foil);

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
				if (namefield.getText().toString().length() > 0) {
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					int numberOf = Integer.parseInt(numberOfFromField);
					boolean foil = checkboxFoil.isChecked();

					try {
						String cardName, setCode, tcgName, cardNumber;
						CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
						cardName = namefield.getText().toString();
						Cursor cards = mDbHelper.fetchCardByName(cardName, new String[]{CardDbAdapter.KEY_SET, CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_RARITY});
						setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
						tcgName = mDbHelper.getTCGname(setCode);
						cardNumber = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));
						if (foil && !TradeListHelpers.canBeFoil(setCode, mDbHelper)) {
							Toast.makeText(getActivity(), cannot_be_foil, Toast.LENGTH_LONG).show();
							foil = false;
						}

						cards.close();
						mDbHelper.close();

						final MtgCard data = new MtgCard(cardName, tcgName, setCode, numberOf, 0, "loading", cardNumber, '-', false, foil);

						lTradeLeft.add(0, data);
						aaTradeLeft.notifyDataSetChanged();
						loadPrice(data, aaTradeLeft);
						namefield.setText("");
						numberfield.setText("1");
						checkboxFoil.setChecked(false);

					} catch (Exception e) {
						Toast.makeText(getActivity(), card_not_found, Toast.LENGTH_SHORT).show();
						namefield.setText("");
						numberfield.setText("1");
					}
				}
				else {
					Toast.makeText(getActivity(), getString(R.string.trader_toast_select_card), Toast.LENGTH_SHORT).show();
				}
			}
		});

		bAddTradeRight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().toString().length() > 0) {
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					int numberOf = Integer.parseInt(numberOfFromField);
					boolean foil = checkboxFoil.isChecked();

					String cardName = "",
							setCode = "",
							tcgName = "",
							cardNumber = "";
					try {
						CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
						cardName = namefield.getText().toString();
						Cursor cards = mDbHelper.fetchCardByName(cardName, new String[]{CardDbAdapter.KEY_SET, CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_RARITY});
						setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
						tcgName = mDbHelper.getTCGname(setCode);
						cardNumber = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));

						if (foil && !TradeListHelpers.canBeFoil(setCode, mDbHelper)) {
							Toast.makeText(getActivity(), cannot_be_foil, Toast.LENGTH_LONG).show();
							foil = false;
						}

						mDbHelper.close();
						cards.close();

						MtgCard data = new MtgCard(cardName, tcgName, setCode, numberOf, 0, "loading", cardNumber, '-', false, foil);

						lTradeRight.add(0, data);
						aaTradeRight.notifyDataSetChanged();
						loadPrice(data, aaTradeRight);
						namefield.setText("");
						numberfield.setText("1");
						checkboxFoil.setChecked(false);

					} catch (Exception e) {
						Toast.makeText(getActivity(), card_not_found, Toast.LENGTH_SHORT).show();
						namefield.setText("");
						numberfield.setText("1");
					}
				}
				else {
					Toast.makeText(getActivity(), getString(R.string.trader_toast_select_card), Toast.LENGTH_SHORT).show();
				}
			}
		});

		lvTradeLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				sideForDialog = "left";
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});
		lvTradeRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				sideForDialog = "right";
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});

		// Give this a default value so we don't get the null pointer-induced FC. It
		// shouldn't matter what we set it to, as long as we set it, since we
		// dismiss the dialog if it's showing in onResume().
		sideForDialog = "left";

		// Default this to an empty string so we never get NPEs from it
		currentTrade = "";

		return myFragmentView;
	}

	@Override
	public void onResume() {
		super.onResume();

		priceSetting = Integer.parseInt(getFamiliarActivity().mPreferenceAdapter.getTradePrice());

		try {
			// Test to see if the autosave file exist, then load the trade it if does.
			this.getActivity().openFileInput(autosaveName + TRADE_EXTENSION);
			LoadTrade(autosaveName + TRADE_EXTENSION);
		} catch (FileNotFoundException e) {
			// Do nothing if the file doesn't exist, but mark it as loaded otherwise prices won't update
			doneLoading = true;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		SaveTrade(autosaveName + TRADE_EXTENSION);
		getFamiliarActivity().mSpiceManager.cancelAllRequests();
	}

	private void showDialog(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}

		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				setShowsDialog(true); // We're setting this to false if we return null, so we should reset it every time to be safe
				switch (id) {
					case DIALOG_UPDATE_CARD: {
						final int position = positionForDialog;
						final String side = (sideForDialog.equals("left") ? "left" : "right");
						final ArrayList<MtgCard> lSide = (sideForDialog.equals("left") ? lTradeLeft : lTradeRight);
						final TradeListAdapter aaSide = (sideForDialog.equals("left") ? aaTradeLeft : aaTradeRight);
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
									ChangeSet(side, position);
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
											Toast.makeText(getActivity(), number_of_invalid, Toast.LENGTH_LONG).show();
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
												Toast.makeText(getActivity(), cannot_be_foil, Toast.LENGTH_LONG).show();
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
										UpdateTotalPrices("both");
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
		newFragment.show(ft, "dialog");
	}


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

	protected void LoadTrade(String _tradeName) {
		try {
			doneLoading = false;
			BufferedReader br = new BufferedReader(new InputStreamReader(this.getActivity().openFileInput(_tradeName)));

			lTradeLeft.clear();
			lTradeRight.clear();

			String line;
			String[] parts;
			while ((line = br.readLine()) != null) {
				parts = line.split(MtgCard.DELIMITER);

				String cardName = "";

				try {
					cardName = parts[1];
					String cardSet = parts[2];
					CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
					String tcgName = mDbHelper.getTCGname(cardSet);
					mDbHelper.close();
					int side = Integer.parseInt(parts[0]);
					int numberOf = Integer.parseInt(parts[3]);
					boolean customPrice = false;
					int price = 0;
					String message = "loading";
					boolean foil = false;

					MtgCard cd;

					try {
						customPrice = Boolean.parseBoolean(parts[4]);
						price = Integer.parseInt(parts[5]);
						foil = Boolean.parseBoolean(parts[6]);
						message = "";
					} catch (Exception e) {
						customPrice = false;
						price = 0;
						message = "loading";
						foil = false;
					} finally {
						cd = new MtgCard(cardName, tcgName, cardSet, numberOf, price, message, null, '-', customPrice, foil);
					}

					if (side == LEFT) {
						lTradeLeft.add(cd);
						if (!customPrice)
							loadPrice(cd, aaTradeLeft);
					}
					else if (side == RIGHT) {
						lTradeRight.add(cd);
						if (!customPrice)
							loadPrice(cd, aaTradeRight);
					}
				} catch (Exception e) {
					if (cardName != null && cardName.length() != 0) {
						Toast.makeText(this.getActivity(), cardName + ": " + card_corrupted, Toast.LENGTH_LONG).show();
					}
					else {
						Toast.makeText(this.getActivity(), card_corrupted, Toast.LENGTH_SHORT).show();
					}
				}
			}
		} catch (NumberFormatException e) {
			Toast.makeText(this.getActivity(), "NumberFormatException", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(this.getActivity(), "IOException", Toast.LENGTH_LONG).show();
		}
		doneLoading = true;
		UpdateTotalPrices(); // this is for custom prices
	}

	protected void ChangeSet(final String _side, final int _position) throws FamiliarDbException {
		MtgCard data = (_side.equals("left") ? lTradeLeft.get(_position) : lTradeRight.get(_position));
		String name = data.name;

		CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
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
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setTitle(R.string.card_view_set_dialog_title);
		builder.setItems(aSets, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int item) {
				if (_side.equals("left")) {
					lTradeLeft.get(_position).setCode = (aSetCodes[item]);
					lTradeLeft.get(_position).tcgName = (aSets[item]);
					lTradeLeft.get(_position).message = ("loading");
					aaTradeLeft.notifyDataSetChanged();
					loadPrice(lTradeLeft.get(_position), aaTradeLeft);
				}
				else if (_side.equals("right")) {
					lTradeRight.get(_position).setCode = (aSetCodes[item]);
					lTradeRight.get(_position).tcgName = (aSets[item]);
					lTradeRight.get(_position).message = ("loading");
					aaTradeRight.notifyDataSetChanged();
					loadPrice(lTradeRight.get(_position), aaTradeRight);
				}
			}
		});
		builder.create().show();
	}

	public void UpdateTotalPrices() {
		UpdateTotalPrices("both");
	}

	private void UpdateTotalPrices(String _side) {
		if (doneLoading) {
			if (_side.equals("left") || _side.equals("both")) {
				int totalPriceLeft = GetPricesFromTradeList(lTradeLeft);
				int color = PriceListHasBadValues(lTradeLeft) ? this.getActivity().getResources().getColor(R.color.holo_red_dark) : this.getActivity().getResources().getColor(R.color.black);
				String sTotalLeft = "$" + (totalPriceLeft / 100) + "." + String.format("%02d", (totalPriceLeft % 100));
				tradePriceLeft.setText(sTotalLeft);
				tradePriceLeft.setTextColor(color);
			}
			if (_side.equals("right") || _side.equals("both")) {
				int totalPriceRight = GetPricesFromTradeList(lTradeRight);
				int color = PriceListHasBadValues(lTradeRight) ? this.getActivity().getResources().getColor(R.color.holo_red_dark) : this.getActivity().getResources().getColor(R.color.black);
				String sTotalRight = "$" + (totalPriceRight / 100) + "." + String.format("%02d", (totalPriceRight % 100));
				tradePriceRight.setText(sTotalRight);
				tradePriceRight.setTextColor(color);
			}
		}
	}

	private boolean PriceListHasBadValues(ArrayList<MtgCard> trade) {
		for (MtgCard data : trade) {
			if (!data.hasPrice()) {
				return true;
			}
		}
		return false;
	}

	private int GetPricesFromTradeList(ArrayList<MtgCard> _trade) {
		int totalPrice = 0;

		for (int i = 0; i < _trade.size(); i++) {// MtgCard data : _trade) {
			MtgCard data = _trade.get(i);
			if (data.hasPrice()) {
				totalPrice += data.numberOf * data.price;
			}
			else {
				String message = data.message;

				// Remove the card from the list, unless it was just a fetch failed.
				// Otherwise, the card does not exist, or there is a database problem

				if (message.compareTo(card_not_found) == 0) {
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(this.getActivity(), data.name + ": " + card_not_found, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(mangled_url) == 0) {
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(this.getActivity(), data.name + ": " + mangled_url, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(database_busy) == 0) {
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(this.getActivity(), data.name + ": " + database_busy, Toast.LENGTH_LONG).show();
				}
			}
		}
		return totalPrice;
	}

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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.trader_menu, menu);
	}

	private class TradeListAdapter extends ArrayAdapter<MtgCard> {

		private int layoutResourceId;
		private ArrayList<MtgCard> items;

		public TradeListAdapter(Context context, int textViewResourceId, ArrayList<MtgCard> items) {
			super(context, textViewResourceId, items);

			this.layoutResourceId = textViewResourceId;
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				v = getActivity().getLayoutInflater().inflate(layoutResourceId, null);
			}
			MtgCard data = items.get(position);
			if (data != null) {
				assert v != null;
				TextView nameField = (TextView) v.findViewById(R.id.traderRowName);
				TextView setField = (TextView) v.findViewById(R.id.traderRowSet);
				TextView numberField = (TextView) v.findViewById(R.id.traderNumber);
				TextView priceField = (TextView) v.findViewById(R.id.traderRowPrice);
				ImageView foilField = (ImageView) v.findViewById(R.id.traderRowFoil);

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
						priceField.setTextColor(getActivity().getResources().getColor(R.color.drawer_divider_item));
					}
				}
				else {
					priceField.setTextColor(getActivity().getResources().getColor(R.color.holo_red_dark));
				}
			}
			return v;
		}
	}

	private void loadPrice(final MtgCard data, final TradeListAdapter adapter) {
		PriceFetchRequest priceRequest = new PriceFetchRequest(data.name, data.setCode, data.number, -1, getActivity());
		final boolean foilOverride = data.foil;
		getFamiliarActivity().mSpiceManager.execute(priceRequest, data.name + "-" + data.setCode, DurationInMillis.ONE_DAY, new RequestListener<PriceInfo>() {
			@Override
			public void onRequestFailure(SpiceException spiceException) {
				data.message = spiceException.getMessage();
			}

			@Override
			public void onRequestSuccess(final PriceInfo result) {
				if (result != null) {
					int cardPrice = (foilOverride ? FOIL_PRICE : priceSetting);

					switch (cardPrice) {
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
					data.message = null;
				}
				else {
					data.message = getString(R.string.trader_no_price);
				}

				UpdateTotalPrices();
				adapter.notifyDataSetChanged();
			}
		});
	}
}