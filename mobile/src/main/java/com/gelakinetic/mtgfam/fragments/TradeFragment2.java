package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.util.ArrayList;

/**
 * Created by xvicarious on 6/13/17.
 */

public class TradeFragment2 extends FamiliarListFragment {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int BOTH = 2;

    public static final String TRADE_EXTENSION = ".trade";
    public static final String AUTOSAVE_NAME = "autosave";

    private ArrayList<MtgCard> mListLeft;
    private RecyclerView mListViewLeft;
    private CardDataAdapter mListAdapterLeft;

    private ArrayList<MtgCard> mListRight;
    private RecyclerView mListViewRight;
    private CardDataAdapter mListAdapterRight;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View myFragmentView = inflater.inflate(R.layout.trader_frag_2, container, false);

        assert myFragmentView != null;
        mNameField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.name_search);
        mNumberOfField = (EditText) myFragmentView.findViewById(R.id.number_input);
        mCheckboxFoil = (CheckBox) myFragmentView.findViewById(R.id.list_foil);

        mNameField.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME}, new int[]{R.id.text1}, mNameField, false));

        mListLeft = new ArrayList<>();
        mListAdapterLeft = new CardDataAdapter(mListLeft);
        mListViewLeft = (RecyclerView) myFragmentView.findViewById(R.id.tradeListLeft);
        mListViewLeft.setAdapter(mListAdapterLeft);
        mListViewLeft.setLayoutManager(new LinearLayoutManager(getContext()));

        mListRight = new ArrayList<>();
        mListAdapterRight = new CardDataAdapter(mListRight);
        mListViewRight = (RecyclerView) myFragmentView.findViewById(R.id.tradeListRight);
        mListViewRight.setAdapter(mListAdapterRight);
        mListViewRight.setLayoutManager(new LinearLayoutManager(getContext()));

        /* Temporary? Just to appease FamiliarListFragment's onPause */
        mListAdapter = mListAdapterLeft;

        myFragmentView.findViewById(R.id.addCardLeft).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addCardToTrade(LEFT);
            }

        });

        myFragmentView.findViewById(R.id.addCardRight).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addCardToTrade(RIGHT);
            }

        });

        setUpCheckBoxClickListeners();

        // todo: swipe-to-delete

        mActionModeCallback = new ActionMode.Callback() {

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.decklist_select_menu, menu); // todo: make an actual menu
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return false; // todo: menu items
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

                mListAdapterLeft.unselectAll();
                mListAdapterRight.unselectAll();

            }

        };

        return myFragmentView;
    }

    private void addCardToTrade(final int side) {

        if (mNameField.getText() == null || mNumberOfField.getText() == null) {
            return;
        }

        final String cardName = mNameField.getText().toString();
        final int numberOf = Integer.parseInt(mNumberOfField.getText().toString());
        final boolean isFoil = mCheckboxFoil.isChecked();

        final MtgCard card = CardHelpers.makeMtgCard(getContext(), cardName, isFoil, numberOf);

        switch (side) {
            case LEFT: {
                mListLeft.add(0, card);
                mListAdapterLeft.notifyItemInserted(0);
                // todo: load price
                break;
            }
            case RIGHT: {
                mListRight.add(0, card);
                mListAdapterRight.notifyItemInserted(0);
                // todo: load price
                break;
            }
        }

        mNameField.setText("");
        mNumberOfField.setText("1");

        if (!mCheckboxFoilLocked) {
            mCheckboxFoil.setChecked(false);
        }

        // todo: sort trades

    }

    // todo: onOptionsItemSelected
    // todo: shareTrade
    // todo: onPause
    // todo: onResume
    // todo: saveTrade
    // todo: loadTrade

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.trader_menu, menu);

    }

    @Override
    public void onPause() {

        super.onPause();
        mListAdapterRight.removePendingNow();
        mListAdapterLeft.removePendingNow();

    }

    public class CardDataAdapter extends FamiliarListFragment.CardDataAdapter<MtgCard> {

        CardDataAdapter(ArrayList<MtgCard> values) {
            super(values);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new ViewHolder(viewGroup);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

            final MtgCard item = mItems.get(position);

            final ViewHolder holder = (ViewHolder) viewHolder;

            if (mItemsPendingRemoval.contains(item)) {
                holder.itemView.findViewById(R.id.trade_row).setVisibility(View.GONE);
            } else {
                holder.itemView.findViewById(R.id.trade_row).setVisibility(View.VISIBLE);
                holder.mCardName.setText(item.mName);
                holder.mCardSet.setText(item.setName);
                holder.mCardNumberOf.setText(item.hasPrice() ? item.numberOf + "x" : "");
                holder.mCardFoil.setVisibility(item.foil ? View.VISIBLE : View.GONE);
                if (item.hasPrice()) {
                    if (item.customPrice) {
                        holder.mCardPrice.setTextColor(ContextCompat.getColor(getContext(), R.color.material_green_500));
                    } else {
                        holder.mCardPrice.setTextColor(ContextCompat.getColor(getContext(), getResourceIdFromAttr(R.attr.color_text)));
                    }
                } else {
                    holder.mCardPrice.setTextColor(ContextCompat.getColor(getContext(), R.color.material_red_500));
                }
            }

        }

        class ViewHolder extends FamiliarListFragment.CardDataAdapter.ViewHolder {

            private TextView mCardSet;
            private TextView mCardNumberOf;
            private ImageView mCardFoil;
            private TextView mCardPrice;

            ViewHolder(ViewGroup view) {

                super(view, R.layout.trader_row);

                mCardSet = (TextView) itemView.findViewById(R.id.traderRowSet);
                mCardNumberOf = (TextView) itemView.findViewById(R.id.traderNumber);
                mCardFoil = (ImageView) itemView.findViewById(R.id.traderRowFoil);
                mCardPrice = (TextView) itemView.findViewById(R.id.traderRowPrice);

                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);

            }

        }

    }

}
