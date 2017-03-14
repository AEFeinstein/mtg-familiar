package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that creates dialogs for ResultListFragment
 */
public class ResultListDialogFragment extends FamiliarDialogFragment {

    /**
     * @return The currently viewed ResultListFragment
     */
    private ResultListFragment getParentResultListFragment() {
        return (ResultListFragment) getFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setShowsDialog(true);

        /* Inflate the view */
        View view = getParentResultListFragment().getActivity().getLayoutInflater().inflate(R.layout.sort_dialog_frag, null, false);
        assert view != null;

        /* Create an arraylist of all the sorting options */
        final ArrayList<SortOption> options = new ArrayList<>(6);
        int idx = 0;
        options.add(new SortOption(getResources().getString(R.string.search_name), false, CardDbAdapter.KEY_NAME, idx++));
        options.add(new SortOption(getResources().getString(R.string.search_color_title), false, CardDbAdapter.KEY_COLOR, idx++));
        options.add(new SortOption(getResources().getString(R.string.search_supertype), false, CardDbAdapter.KEY_SUPERTYPE, idx++));
        options.add(new SortOption(getResources().getString(R.string.search_cmc), false, CardDbAdapter.KEY_CMC, idx++));
        options.add(new SortOption(getResources().getString(R.string.search_power), false, CardDbAdapter.KEY_POWER, idx));
        options.add(new SortOption(getResources().getString(R.string.search_toughness), false, CardDbAdapter.KEY_TOUGHNESS, idx));

        /* Get the sort view and set it up */
        DragListView sortView = (DragListView) view.findViewById(R.id.sort_list_view);
        sortView.setLayoutManager(new LinearLayoutManager(getParentResultListFragment().getActivity()));
        sortItemAdapter adapter = new sortItemAdapter(options);
        sortView.setAdapter(adapter, true);
        sortView.setCanDragHorizontally(false);

        /* Create the dialog */
        MaterialDialog.Builder adb = new MaterialDialog.Builder(getParentResultListFragment().getActivity());
        adb.customView(view, false);
        adb.title(getResources().getString(R.string.wishlist_sort_by));
        adb.positiveText(getParentResultListFragment().getActivity().getResources().getString(R.string.dialog_ok));
        adb.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                /* Reordering the entries reorders the pairs */
                for (SortOption p : options) {
                    Log.v("Pair", p.mName);
                }
                /* TODO pass the data back to the result list view and sort */
                dismiss();
            }
        });

        return adb.build();
    }


    private class sortItemAdapter extends DragItemAdapter<SortOption, sortItemAdapter.sortItemViewHolder> {

        sortItemAdapter(List<SortOption> options) {
            setHasStableIds(true);
            setItemList(options);
        }

        @Override
        public sortItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            /* This is where the individual views get inflated */
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sort_list_item, parent, false);
            return new sortItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(sortItemViewHolder holder, int position) {
            /* This is where the views get filled with data */
            /* TODO set up asc/desc spinner? */
            super.onBindViewHolder(holder, position);
            String text = mItemList.get(position).mName;
            holder.mText.setText(text);
            holder.itemView.setTag(mItemList.get(position));
        }

        @Override
        public long getItemId(int position) {
            /* This is needed for reordering */
            return mItemList.get(position).mId;
        }

        /**
         * This is a subclass for each view
         */
        class sortItemViewHolder extends DragItemAdapter.ViewHolder {

            TextView mText;

            sortItemViewHolder(final View itemView) {
                /* TODO prettify the handle */
                super(itemView, R.id.sort_list_handle, false);
                mText = (TextView) itemView.findViewById(R.id.sort_list_text);
            }

        }
    }

    class SortOption {
        String mName;
        boolean mAscending;
        String mDatabaseKey;
        int mId;

        SortOption(String name, boolean ascending, String databaseKey, int id) {
            mName = name;
            mAscending = ascending;
            mDatabaseKey = databaseKey;
            mId = id;
        }
    }
}