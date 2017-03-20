package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.fragments.TradeFragment;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Class that creates dialogs for TradeFragment
 */
public class TradeDialogFragment extends FamiliarDialogFragment {

    /* Dialog Constants */
    public static final int DIALOG_UPDATE_CARD = 1;
    public static final int DIALOG_PRICE_SETTING = 2;
    public static final int DIALOG_SAVE_TRADE = 3;
    public static final int DIALOG_LOAD_TRADE = 4;
    public static final int DIALOG_DELETE_TRADE = 5;
    public static final int DIALOG_CONFIRMATION = 6;
    private static final int DIALOG_CHANGE_SET = 7;
    public static final int DIALOG_SORT = 8;

    /* Extra argument keys */
    public static final String ID_POSITION = "Position";
    public static final String ID_SIDE = "Side";

    /**
     * @return The currently viewed TradeFragment
     */
    private TradeFragment getParentTradeFragment() {
        return (TradeFragment) getFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
                /* We're setting this to false if we return null, so we should reset it every time to be safe */
        setShowsDialog(true);
        mDialogId = getArguments().getInt(ID_KEY);
        final int sideForDialog = getArguments().getInt(ID_SIDE);
        final int positionForDialog = getArguments().getInt(ID_POSITION);
        switch (mDialogId) {
            case DIALOG_UPDATE_CARD: {
                        /* Get some final references */
                final ArrayList<MtgCard> lSide = (sideForDialog == TradeFragment.LEFT ? getParentTradeFragment().mLeftList : getParentTradeFragment().mRightList);
                final TradeFragment.TradeListAdapter aaSide = (sideForDialog == TradeFragment.LEFT ? getParentTradeFragment().mLeftAdapter : getParentTradeFragment().mRightAdapter);
                final boolean oldFoil = lSide.get(positionForDialog).foil;

                        /* Inflate the view and pull out UI elements */
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.trader_card_click_dialog,
                        null, false);
                assert view != null;
                final CheckBox foilCheckbox = (CheckBox) view.findViewById(R.id.traderDialogFoil);
                final EditText numberOf = (EditText) view.findViewById(R.id.traderDialogNumber);
                final EditText priceText = (EditText) view.findViewById(R.id.traderDialogPrice);

                        /* Set initial values */
                String numberOfStr = String.valueOf(lSide.get(positionForDialog).numberOf);
                numberOf.setText(numberOfStr);
                numberOf.setSelection(numberOfStr.length());
                foilCheckbox.setChecked(oldFoil);
                String priceNumberStr = lSide.get(positionForDialog).hasPrice() ?
                        lSide.get(positionForDialog).getPriceString().substring(1) : "";
                priceText.setText(priceNumberStr);
                priceText.setSelection(priceNumberStr.length());

                        /* Only show the foil checkbox if the card can be foil */
                SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                try {
                    if (CardDbAdapter.canBeFoil(lSide.get(positionForDialog).setCode, database)) {
                        view.findViewById(R.id.checkbox_layout).setVisibility(View.VISIBLE);
                    } else {
                        view.findViewById(R.id.checkbox_layout).setVisibility(View.GONE);
                    }
                } catch (FamiliarDbException e) {
                            /* Err on the side of foil */
                    foilCheckbox.setVisibility(View.VISIBLE);
                }
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

                        /* when the user checks or un-checks the foil box, if the price isn't custom, set it */
                foilCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        lSide.get(positionForDialog).foil = b;
                        if (!lSide.get(positionForDialog).customPrice) {
                            getParentTradeFragment().loadPrice(lSide.get(positionForDialog), aaSide);
                            priceText.setText(lSide.get(positionForDialog).hasPrice() ?
                                    lSide.get(positionForDialog).getPriceString().substring(1) : "");
                        }
                    }
                });

                        /* Set up the button to remove this card from the trade */
                view.findViewById(R.id.traderDialogRemove).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        lSide.remove(positionForDialog);
                        aaSide.notifyDataSetChanged();
                        getParentTradeFragment().UpdateTotalPrices(sideForDialog);
                        getParentTradeFragment().removeDialog(getFragmentManager());
                    }
                });

                        /* If this has a custom price, show the button to default the price */
                view.findViewById(R.id.traderDialogResetPrice).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        lSide.get(positionForDialog).customPrice = false;
                                /* This loads the price if necessary, or uses cached info */
                        getParentTradeFragment().loadPrice(lSide.get(positionForDialog), aaSide);
                        int price = lSide.get(positionForDialog).price;
                        priceText.setText(String.format(Locale.US, "%d.%02d", price / 100, price % 100));

                        aaSide.notifyDataSetChanged();
                        getParentTradeFragment().UpdateTotalPrices(sideForDialog);
                    }
                });


                        /* Set up the button to show info about this card */
                view.findViewById(R.id.traderDialogInfo).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                        try {
                                    /* Get the card ID, and send it to a new CardViewPagerFragment */
                            Cursor cursor = CardDbAdapter.fetchCardByNameAndSet(lSide.get(positionForDialog).name,
                                    lSide.get(positionForDialog).setCode, new String[]{
                                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID}, database);

                            Bundle args = new Bundle();
                            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, new long[]{cursor.getLong(
                                    cursor.getColumnIndex(CardDbAdapter.KEY_ID))});
                            args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);

                            cursor.close();
                            CardViewPagerFragment cvpFrag = new CardViewPagerFragment();
                            getParentTradeFragment().startNewFragment(cvpFrag, args);
                        } catch (FamiliarDbException e) {
                            getParentTradeFragment().handleFamiliarDbException(false);
                        }
                        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                    }
                });

                        /* Set up the button to change the set of this card */
                view.findViewById(R.id.traderDialogChangeSet).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        getParentTradeFragment().showDialog(DIALOG_CHANGE_SET, sideForDialog, positionForDialog);
                    }
                });

                return new MaterialDialog.Builder(this.getActivity())
                        .title(lSide.get(positionForDialog).name)
                        .customView(view, false)
                        .positiveText(R.string.dialog_done)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                /* Grab a reference to the card */
                                MtgCard data = lSide.get(positionForDialog);

                                /* Assume non-custom price */
                                data.customPrice = false;

                                /* Set this card's foil option */
                                data.foil = foilCheckbox.isChecked();

                                /* validate number of cards text */
                                if (numberOf.length() == 0) {
                                    data.numberOf = 1;
                                } else {
                                    /* Set the numberOf */
                                    assert numberOf.getEditableText() != null;
                                    try {
                                        data.numberOf =
                                                (Integer.parseInt(numberOf.getEditableText().toString()));
                                    } catch (NumberFormatException e) {
                                        data.numberOf = 1;
                                    }
                                }

                                /* validate the price text */
                                assert priceText.getText() != null;
                                String userInputPrice = priceText.getText().toString();

                                /* If the input price is blank, set it to zero */
                                if (userInputPrice.length() == 0) {
                                    data.customPrice = true;
                                    data.price = 0;
                                } else {
                                    /* Attempt to parse the price */
                                    try {
                                        data.price = (int) (Double.parseDouble(userInputPrice) * 100);
                                    } catch (NumberFormatException e) {
                                        data.customPrice = true;
                                        data.price = 0;
                                    }
                                }

                                /* Check if the user hand-modified the price by comparing the current price
                                 * to the cached price */
                                int oldPrice;
                                if (data.priceInfo != null) {
                                    if (data.foil) {
                                        oldPrice = (int) (data.priceInfo.mFoilAverage * 100);
                                    } else {
                                        switch (getParentTradeFragment().mPriceSetting) {
                                            case TradeFragment.LOW_PRICE: {
                                                oldPrice = (int) (data.priceInfo.mLow * 100);
                                                break;
                                            }
                                            default:
                                            case TradeFragment.AVG_PRICE: {
                                                oldPrice = (int) (data.priceInfo.mAverage * 100);
                                                break;
                                            }
                                            case TradeFragment.HIGH_PRICE: {
                                                oldPrice = (int) (data.priceInfo.mHigh * 100);
                                                break;
                                            }
                                            case TradeFragment.FOIL_PRICE: {
                                                oldPrice = (int) (data.priceInfo.mFoilAverage * 100);
                                                break;
                                            }
                                        }
                                    }
                                    if (oldPrice != data.price) {
                                        data.customPrice = true;
                                    }
                                } else {
                                    data.customPrice = true;
                                }

                                /* Notify things to update */
                                aaSide.notifyDataSetChanged();
                                getParentTradeFragment().UpdateTotalPrices(sideForDialog);
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
            }
            case DIALOG_CHANGE_SET: {
                /* Get the card */
                MtgCard data = (sideForDialog == TradeFragment.LEFT ?
                        getParentTradeFragment().mLeftList.get(positionForDialog) : getParentTradeFragment().mRightList.get(positionForDialog));

                SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                try {
                    /* Query the database for all versions of this card */
                    Cursor cards = CardDbAdapter.fetchCardByName(data.name, new String[]{
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                            CardDbAdapter.DATABASE_TABLE_SETS + "." + CardDbAdapter.KEY_NAME}, true, database);
                    /* Build set names and set codes */
                    Set<String> sets = new LinkedHashSet<>();
                    Set<String> setCodes = new LinkedHashSet<>();
                    while (!cards.isAfterLast()) {
                        if (sets.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NAME)))) {
                            setCodes.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET)));
                        }
                        cards.moveToNext();
                    }
                    /* clean up */
                    cards.close();
                    DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

                    /* Turn set names and set codes into arrays */
                    final String[] aSets = sets.toArray(new String[sets.size()]);
                    final String[] aSetCodes = setCodes.toArray(new String[setCodes.size()]);

                    /* Build and return the dialog */
                    return new MaterialDialog.Builder(getActivity())
                            .title(R.string.card_view_set_dialog_title)
                            .items(aSets)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                    /* Figure out what we're updating */
                                    MtgCard data;
                                    TradeFragment.TradeListAdapter adapter;
                                    if (sideForDialog == TradeFragment.LEFT) {
                                        data = getParentTradeFragment().mLeftList.get(positionForDialog);
                                        adapter = getParentTradeFragment().mLeftAdapter;
                                    } else {
                                        data = getParentTradeFragment().mRightList.get(positionForDialog);
                                        adapter = getParentTradeFragment().mRightAdapter;
                                    }

                                    /* Change the card's information, and reload the price */
                                    data.setCode = (aSetCodes[position]);
                                    data.setName = (aSets[position]);
                                    data.message = (getString(R.string.wishlist_loading));
                                    data.priceInfo = null;

                                    /* See if the new set can be foil */
                                    SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                                    try {
                                        if (!CardDbAdapter.canBeFoil(data.setCode, database)) {
                                            data.foil = false;
                                        }
                                    } catch (FamiliarDbException e) {
                                        data.foil = false;
                                    }
                                    DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

                                    /* Reload and notify the adapter */
                                    getParentTradeFragment().loadPrice(data, adapter);
                                    adapter.notifyDataSetChanged();
                                }
                            })
                            .build();
                } catch (FamiliarDbException e) {
                    /* Don't show the dialog, but pop a toast */
                    getParentTradeFragment().handleFamiliarDbException(true);
                    DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                    return DontShowDialog();
                }
            }
            case DIALOG_PRICE_SETTING: {
                /* Build the dialog with some choices */
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.trader_pricing_dialog_title)
                        .items(new String[]{getString(R.string.trader_Low),
                                getString(R.string.trader_Average),
                                getString(R.string.trader_High)})
                        .itemsCallbackSingleChoice(getParentTradeFragment().mPriceSetting, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                if (getParentTradeFragment().mPriceSetting != which) {
                                    getParentTradeFragment().mPriceSetting = which;

                                    /* Update ALL the prices! */
                                    for (MtgCard data : getParentTradeFragment().mLeftList) {
                                        if (!data.customPrice) {
                                            data.message = getString(R.string.wishlist_loading);
                                            getParentTradeFragment().loadPrice(data, getParentTradeFragment().mLeftAdapter);
                                        }
                                    }
                                    getParentTradeFragment().mLeftAdapter.notifyDataSetChanged();

                                    for (MtgCard data : getParentTradeFragment().mRightList) {
                                        if (!data.customPrice) {
                                            data.message = getString(R.string.wishlist_loading);
                                            getParentTradeFragment().loadPrice(data, getParentTradeFragment().mRightAdapter);
                                        }
                                    }
                                    getParentTradeFragment().mRightAdapter.notifyDataSetChanged();

                                    /* And also update the preference */
                                    getFamiliarActivity().mPreferenceAdapter.setTradePrice(
                                            String.valueOf(getParentTradeFragment().mPriceSetting));

                                    getParentTradeFragment().UpdateTotalPrices(TradeFragment.BOTH);
                                }
                                dialog.dismiss();
                                return true;
                            }
                        })
                        .build();
            }
            case DIALOG_SAVE_TRADE: {
                /* Inflate a view to type in the trade's name, and show it in an AlertDialog */
                View textEntryView = getActivity().getLayoutInflater()
                        .inflate(R.layout.alert_dialog_text_entry, null, false);
                assert textEntryView != null;
                final EditText nameInput = (EditText) textEntryView.findViewById(R.id.text_entry);
                nameInput.append(getParentTradeFragment().mCurrentTrade);
                /* Set the button to clear the text field */
                textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nameInput.setText("");
                    }
                });

                Dialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.trader_save_dialog_title)
                        .customView(textEntryView, false)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                if (nameInput.getText() == null) {
                                    return;
                                }
                                String tradeName = nameInput.getText().toString();

                                /* Don't bother saving if there is no name */
                                if (tradeName.length() == 0 || tradeName.equals("")) {
                                    return;
                                }

                                getParentTradeFragment().SaveTrade(tradeName + TradeFragment.TRADE_EXTENSION);
                                getParentTradeFragment().mCurrentTrade = tradeName;
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            case DIALOG_LOAD_TRADE: {
                /* Find all the trade files */
                String[] files = this.getActivity().fileList();
                ArrayList<String> validFiles = new ArrayList<>();
                for (String fileName : files) {
                    if (fileName.endsWith(TradeFragment.TRADE_EXTENSION)) {
                        validFiles.add(fileName.substring(0, fileName.indexOf(TradeFragment.TRADE_EXTENSION)));
                    }
                }

                /* If there are no files, don't show the dialog */
                if (validFiles.size() == 0) {
                    ToastWrapper.makeText(this.getActivity(), R.string.trader_toast_no_trades, ToastWrapper.LENGTH_LONG)
                            .show();
                    return DontShowDialog();
                }

                /* Make an array of the trade file names */
                final String[] tradeNames = new String[validFiles.size()];
                validFiles.toArray(tradeNames);

                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.trader_select_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items(tradeNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                /* Load the trade, set the current trade name */
                                getParentTradeFragment().LoadTrade(tradeNames[position] + TradeFragment.TRADE_EXTENSION);
                                getParentTradeFragment().mCurrentTrade = tradeNames[position];

                                /* Alert things to update */
                                getParentTradeFragment().mLeftAdapter.notifyDataSetChanged();
                                getParentTradeFragment().mRightAdapter.notifyDataSetChanged();
                                getParentTradeFragment().UpdateTotalPrices(TradeFragment.BOTH);
                            }
                        })
                        .build();
            }
            case DIALOG_DELETE_TRADE: {
                /* Find all the trade files */
                String[] files = this.getActivity().fileList();
                ArrayList<String> validFiles = new ArrayList<>();
                for (String fileName : files) {
                    if (fileName.endsWith(TradeFragment.TRADE_EXTENSION)) {
                        validFiles.add(fileName.substring(0, fileName.indexOf(TradeFragment.TRADE_EXTENSION)));
                    }
                }

                /* If there are no files, don't show the dialog */
                if (validFiles.size() == 0) {
                    ToastWrapper.makeText(this.getActivity(), R.string.trader_toast_no_trades, ToastWrapper.LENGTH_LONG)
                            .show();
                    return DontShowDialog();
                }

                /* Make an array of the trade file names */
                final String[] tradeNames = new String[validFiles.size()];
                validFiles.toArray(tradeNames);

                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.trader_delete_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items(tradeNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                File toDelete = new File(getActivity().getFilesDir(), tradeNames[position] +
                                        TradeFragment.TRADE_EXTENSION);
                                if (!toDelete.delete()) {
                                    ToastWrapper.makeText(getActivity(), toDelete.getName() + " " +
                                            getString(R.string.not_deleted), ToastWrapper.LENGTH_LONG).show();
                                }
                            }
                        })
                        .build();
            }
            case DIALOG_CONFIRMATION: {
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.trader_clear_dialog_title)
                        .content(R.string.trader_clear_dialog_text)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                /* Clear the arrays and tell everything to update */
                                getParentTradeFragment().mCurrentTrade = "";
                                getParentTradeFragment().mRightList.clear();
                                getParentTradeFragment().mLeftList.clear();
                                getParentTradeFragment().mRightAdapter.notifyDataSetChanged();
                                getParentTradeFragment().mLeftAdapter.notifyDataSetChanged();
                                getParentTradeFragment().mCheckboxFoil.setChecked(false);
                                getParentTradeFragment().UpdateTotalPrices(TradeFragment.BOTH);
                                dialog.dismiss();
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .cancelable(true)
                        .build();
            }
            default: {
                return DontShowDialog();
            }
        }
    }
}