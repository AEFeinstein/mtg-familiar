package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that creates dialogs for ResultListFragment
 */
public class SortOrderDialogFragment extends FamiliarDialogFragment {

    public static final String SQL_ASC = "asc";
    public static final String SQL_DESC = "desc";
    public static final String SAVED_SORT_ORDER = "saved_sort_order";
    public static final String KEY_PRICE = "key_price";
    public static final String KEY_ORDER = "key_order";

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setShowsDialog(true);

        /* Inflate the view */
        View view = getActivity().getLayoutInflater().inflate(R.layout.sort_dialog_frag, null, false);
        assert view != null;

        /* Create an arraylist of all the sorting options */
        final ArrayList<SortOption> options = new ArrayList<>(6);
        String searchSortOrder = getArguments().getString(SAVED_SORT_ORDER);

        int idx = 0;

        if (searchSortOrder != null) {
            boolean orderAdded = false;
            boolean priceAdded = false;
            for (String option : searchSortOrder.split(",")) {
                String key = option.split(" ")[0];
                boolean ascending = option.split(" ")[1].equalsIgnoreCase(SQL_ASC);
                String name = null;

                switch (key) {
                    case CardDbAdapter.KEY_NAME: {
                        name = getResources().getString(R.string.search_name);
                        break;
                    }
                    case CardDbAdapter.KEY_COLOR: {
                        name = getResources().getString(R.string.search_color_title);
                        break;
                    }
                    case CardDbAdapter.KEY_SUPERTYPE: {
                        name = getResources().getString(R.string.search_supertype);
                        break;
                    }
                    case CardDbAdapter.KEY_CMC: {
                        name = getResources().getString(R.string.search_cmc);
                        break;
                    }
                    case CardDbAdapter.KEY_POWER: {
                        name = getResources().getString(R.string.search_power);
                        break;
                    }
                    case CardDbAdapter.KEY_TOUGHNESS: {
                        name = getResources().getString(R.string.search_toughness);
                        break;
                    }
                    case CardDbAdapter.KEY_SET: {
                        name = getResources().getString(R.string.search_set);
                        break;
                    }
                    case KEY_PRICE: {
                        name = getResources().getString(R.string.wishlist_type_price);
                        priceAdded = true;
                        break;
                    }
                    case KEY_ORDER: {
                        name = getResources().getString(R.string.wishlist_type_order);
                        orderAdded = true;
                        break;
                    }
                }
                options.add(new SortOption(name, ascending, key, idx++));
            }

            /* Sorting by order was added later, so if it's not in the given string and price is,
             * which it is for wishlist and trade list, add order too.
             */
            if (priceAdded && !orderAdded) {
                options.add(new SortOption(getResources().getString(R.string.wishlist_type_order),
                        false, KEY_ORDER, idx));
            }
        }

        /* Get the sort view and set it up */
        DragListView sortView = view.findViewById(R.id.sort_list_view);
        sortView.setLayoutManager(new LinearLayoutManager(getActivity()));
        sortItemAdapter adapter = new sortItemAdapter(options);
        sortView.setAdapter(adapter, true);
        sortView.setCanDragHorizontally(false);

        /* Create the dialog */
        MaterialDialog.Builder adb = new MaterialDialog.Builder(getActivity());
        adb.customView(view, false);
        adb.title(getResources().getString(R.string.wishlist_sort_by));
        adb.negativeText(R.string.dialog_cancel);
        adb.positiveText(getActivity().getResources().getString(R.string.dialog_ok));
        adb.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                /* Reordering the entries reorders the pairs */
                String orderByStr = "";
                boolean first = true;
                for (SortOption p : options) {
                    if (!first) {
                        orderByStr += ",";
                    }
                    orderByStr += (p.mDatabaseKey);
                    if (p.mAscending) {
                        orderByStr += " " + SQL_ASC;
                    } else {
                        orderByStr += " " + SQL_DESC;
                    }
                    first = false;
                }
                if (null != getParentFamiliarFragment()) {
                    getParentFamiliarFragment().receiveSortOrder(orderByStr);
                }
                dismiss();
            }
        });

        return adb.build();
    }

    private class sortItemAdapter extends DragItemAdapter<SortOption, sortItemAdapter.sortItemViewHolder> {

        /**
         * Constructor. It sets the item list
         *
         * @param options A list of SortOptions to display
         */
        sortItemAdapter(List<SortOption> options) {
            setHasStableIds(true);
            setItemList(options);
        }

        /**
         * Called when RecyclerView needs a new RecyclerView.ViewHolder of the given type to
         * represent an item. This new ViewHolder is constructed with a new View that can represent
         * the items of the given type from R.layout.sort_list_item.
         *
         * @param parent   The parent ViewGroup
         * @param viewType Unused
         * @return The newly inflated sortItemViewHolder
         */
        @Override
        public sortItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            /* This is where the individual views get inflated */
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sort_list_item, parent, false);
            return new sortItemViewHolder(view);
        }

        /**
         * Called by RecyclerView to display the data at the specified position. This method updates
         * the contents of the itemView to reflect the item at the given position.
         *
         * @param holder   The itemView to update
         * @param position The item's position
         */
        @Override
        public void onBindViewHolder(final sortItemViewHolder holder, int position) {
            /* Fill the view with data */
            super.onBindViewHolder(holder, position);
            holder.mText.setText(mItemList.get(position).mName);
            holder.mCheckbox.setChecked(mItemList.get(position).mAscending);
            holder.mCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mItemList.get(holder.getAdapterPosition()).mAscending = b;
                }
            });
            holder.itemView.setTag(mItemList.get(position));
        }

        /**
         * Given a position, get that item's stable ID.
         * This is needed for reordering
         *
         * @param position The position to get an ID for
         * @return This position's ID
         */
        @Override
        public long getUniqueItemId(int position) {
            return mItemList.get(position).mId;
        }

        /**
         * This is a subclass for each view
         */
        class sortItemViewHolder extends DragItemAdapter.ViewHolder {

            final CheckBox mCheckbox;
            final TextView mText;

            /**
             * This constructor pulls out UI elements from the given View
             *
             * @param itemView The View for this holder
             */
            sortItemViewHolder(final View itemView) {
                super(itemView, R.id.sort_list_handle, false);
                mText = itemView.findViewById(R.id.sort_list_text);
                mCheckbox = itemView.findViewById(R.id.asc_desc_checkbox);
            }

        }
    }

    public static class SortOption implements Serializable {
        final String mName;
        boolean mAscending = true;
        final String mDatabaseKey;
        final int mId;

        /**
         * Constructs a SortOption
         *
         * @param name        The name to display in the dialog
         * @param ascending   Whether or not this sorts in ascending or descending order by default
         * @param databaseKey The SQL key used to sort the data
         * @param id          A unique ID to enable drag sorting
         */
        public SortOption(String name, boolean ascending, String databaseKey, int id) {
            mName = name;
            mAscending = ascending;
            mDatabaseKey = databaseKey;
            mId = id;
        }

        /**
         * @return the SQL key used to sort the data
         */
        public String getKey() {
            return mDatabaseKey;
        }

        /**
         * @return true if the data should be sorted in ascending order, false otherwise
         */
        public boolean getAscending() {
            return mAscending;
        }
    }
}