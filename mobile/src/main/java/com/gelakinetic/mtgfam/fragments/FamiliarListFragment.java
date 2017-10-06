package com.gelakinetic.mtgfam.fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.LayoutRes;
import android.support.design.widget.Snackbar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SelectableItemAdapter;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import java.util.ArrayList;

/**
 * This class is for extension by any Fragment that has a custom list of cards at it's base that
 * allows modification.
 */
public abstract class FamiliarListFragment extends FamiliarFragment {

    /* Preferences */
    public int mPriceSetting;

    /* Pricing Constants */
    public static final int LOW_PRICE = 0;
    public static final int AVG_PRICE = 1;
    public static final int HIGH_PRICE = 2;
    public static final int FOIL_PRICE = 3;

    /* UI Elements */
    public AutoCompleteTextView mNameField;
    public EditText mNumberOfField;
    public CheckBox mCheckboxFoil;
    TextView mTotalPriceField;

    int mPriceFetchRequests = 0;

    RecyclerView mListView;

    public CardDataAdapter mListAdapter;

    boolean mCheckboxFoilLocked = false;

    ActionMode mActionMode;
    ActionMode.Callback mActionModeCallback;

    ItemTouchHelper itemTouchHelper;

    /**
     * Initializes common members. Generally called in onCreate
     * @param fragmentView the fragment calling this method
     */
    void initializeMembers(View fragmentView) {

        mNameField = fragmentView.findViewById(R.id.name_search);
        mNumberOfField = fragmentView.findViewById(R.id.number_input);
        mCheckboxFoil = fragmentView.findViewById(R.id.list_foil);
        mListView = fragmentView.findViewById(R.id.cardlist);

    }

    void setUpCheckBoxClickListeners() {

        mCheckboxFoil.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {

                /* Lock the checkbox on long click */
                mCheckboxFoilLocked = true;
                mCheckboxFoil.setChecked(true);
                return true;

            }

        });

        mCheckboxFoil.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (!isChecked) {
                    /* Unlock the checkbox when the user unchecks it */
                    mCheckboxFoilLocked = false;
                }

            }

        });

    }

    @Override
    public void onPause() {

        super.onPause();
        mListAdapter.removePendingNow();

    }

    /**
     * Receive the result from the card image search, then fill in the name edit text on the
     * UI thread.
     *
     * @param multiverseId The multiverseId of the card the query returned
     */
    @Override
    public void receiveTutorCardsResult(long multiverseId) {

        try {
            SQLiteDatabase database =
                    DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
            Cursor card = CardDbAdapter.fetchCardByMultiverseId(multiverseId, new String[]{
                    CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NAME}, database);
            final String name = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NAME));
            getFamiliarActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mNameField.setText(name);
                }
            });
            card.close();
        } catch (FamiliarDbException e) {
            e.printStackTrace();
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * Specific implementation for list-based Familiar Fragments.
     *
     * @param <T> type that is stored in the ArrayList
     * @param <VH> ViewHolder that is used by the adapter
     */
    public abstract class CardDataAdapter<T extends MtgCard, VH extends CardDataAdapter.ViewHolder>
            extends SelectableItemAdapter<T, VH> {

        public CardDataAdapter(ArrayList<T> values) {
            super(values, PreferenceAdapter.getUndoTimeout(getContext()));
        }

        @Override
        public boolean pendingRemoval(final int position) {
            if (super.pendingRemoval(position)) {
                Snackbar undoBar = Snackbar.make(
                        getFamiliarActivity().findViewById(R.id.fragment_container),
                        getString(R.string.cardlist_item_deleted) + " " + getItemName(position),
                        pendingTimeout
                );
                undoBar.setAction(R.string.cardlist_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Runnable pendingRemovalRunnable = pendingRunnables.get(position);
                        pendingRunnables.remove(position);
                        onUndoDelete(position);
                        if (pendingRemovalRunnable != null) {
                            handler.removeCallbacks(pendingRemovalRunnable);
                        }
                        notifyItemChanged(position);
                    }
                });
                undoBar.show();
                return true;
            }
            return false;
        }

        public void onUndoDelete(final int position) {
            // Do nothing by default.
        }

        public abstract String getItemName(final int position);

        abstract class ViewHolder extends SelectableItemAdapter.ViewHolder {

            final TextView mCardName;

            ViewHolder(ViewGroup view, @LayoutRes final int layoutRowId) {
                super(LayoutInflater.from(view.getContext()).inflate(layoutRowId, view, false));
                mCardName = itemView.findViewById(R.id.card_name);
            }

            @Override
            public void onClick(View view) {
                if (isInSelectMode()) {
                    if (selectedItems.get(getAdapterPosition(), false)) {
                        selectedItems.delete(getAdapterPosition());
                        itemView.setSelected(false);
                        if (selectedItems.size() < 1) {
                            mActionMode.finish();
                            setInSelectMode(false);
                        }
                        notifyItemChanged(getAdapterPosition());
                        return;
                    }
                    itemView.setSelected(true);
                    selectedItems.put(getAdapterPosition(), true);
                    notifyItemChanged(getAdapterPosition());
                }
            }

            @Override
            public boolean onLongClick(View view) {
                if (!isInSelectMode()) {
                    mActionMode = getFamiliarActivity().startSupportActionMode(mActionModeCallback);
                    itemView.setSelected(true);
                    selectedItems.put(getAdapterPosition(), true);
                    setInSelectMode(true);
                    notifyItemChanged(getAdapterPosition());
                    return true;
                }
                return false;
            }
        }

    }

}
