/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.fragments.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class that creates dialogs for ResultListFragment
 */
public class SortOrderDialogFragment extends FamiliarDialogFragment {

    public static final String SQL_ASC = "asc";
    public static final String SQL_DESC = "desc";
    public static final String SAVED_SORT_ORDER = "saved_sort_order";
    public static final String KEY_PRICE = "key_price";
    public static final String KEY_ORDER = "key_order";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        setShowsDialog(true);

        /* Inflate the view */
        @SuppressLint("InflateParams") View view = Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.sort_dialog_frag, null, false);
        assert view != null;

        /* Create an arraylist of all the sorting options */
        final ArrayList<SortOption> options = new ArrayList<>(6);
        String searchSortOrder = Objects.requireNonNull(getArguments()).getString(SAVED_SORT_ORDER);

        int idx = 0;

        if (searchSortOrder != null) {
            boolean orderAdded = false;
            boolean priceAdded = false;
            boolean rarityAdded = false;
            boolean setAdded = false;
            boolean colorIdentityAdded = false;
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
                        setAdded = true;
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
                    case CardDbAdapter.KEY_RARITY: {
                        name = getResources().getString(R.string.search_rarity);
                        rarityAdded = true;
                        break;
                    }
                    case CardDbAdapter.KEY_COLOR_IDENTITY: {
                        name = getResources().getString(R.string.search_color_identity_title);
                        colorIdentityAdded = true;
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
                        false, KEY_ORDER, idx++));
            }

            if (!rarityAdded) {
                options.add(new SortOption(getResources().getString(R.string.search_rarity),
                        false, CardDbAdapter.KEY_RARITY, idx++));
            }

            if (!setAdded) {
                options.add(new SortOption(getString(R.string.search_set),
                        false, CardDbAdapter.KEY_SET, idx++));
            }

            if (!colorIdentityAdded) {
                options.add(new SortOption(getString(R.string.search_color_identity_title),
                        false, CardDbAdapter.KEY_COLOR_IDENTITY, idx++));
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
        adb.onPositive((dialog, which) -> {
            /* Reordering the entries reorders the pairs */
            StringBuilder orderByStr = new StringBuilder();
            boolean first = true;
            for (SortOption p : options) {
                if (!first) {
                    orderByStr.append(",");
                }
                orderByStr.append(p.mDatabaseKey);
                if (p.mAscending) {
                    orderByStr.append(" ").append(SQL_ASC);
                } else {
                    orderByStr.append(" ").append(SQL_DESC);
                }
                first = false;
            }
            if (null != getParentFamiliarFragment()) {
                getParentFamiliarFragment().receiveSortOrder(orderByStr.toString());
            }
        });

        return adb.build();
    }

    private static class sortItemAdapter extends DragItemAdapter<SortOption, sortItemAdapter.sortItemViewHolder> {

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
        @NonNull
        @Override
        public sortItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        public void onBindViewHolder(@NonNull final sortItemViewHolder holder, int position) {
            /* Fill the view with data */
            super.onBindViewHolder(holder, position);
            holder.mText.setText(mItemList.get(position).mName);
            holder.mCheckbox.setChecked(mItemList.get(position).mAscending);
            holder.mCheckbox.setOnCheckedChangeListener((compoundButton, b) -> {
                int position1 = holder.getAdapterPosition();
                if (RecyclerView.NO_POSITION != position1) {
                    mItemList.get(position1).mAscending = b;
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
        static class sortItemViewHolder extends DragItemAdapter.ViewHolder {

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
        boolean mAscending;
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