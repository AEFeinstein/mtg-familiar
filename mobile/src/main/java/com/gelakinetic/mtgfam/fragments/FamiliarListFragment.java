package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by xvicarious on 5/24/17.
 */

public abstract class FamiliarListFragment extends FamiliarFragment {

    /* UI Elements */
    AutoCompleteTextView mNameField;
    EditText mNumberOfField;
    public CheckBox mCheckboxFoil;

    boolean mCheckboxFoilLocked = false;

    public abstract class CardDataAdapter<E> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        ArrayList<E> mItems;
        ArrayList<E> mItemsPendingRemoval;

        Handler mHandler;
        HashMap<E, Runnable> mPendingRunnables;

        View.OnClickListener mClickListener;

        CardDataAdapter(ArrayList<E> values) {
            mItems = values;
            mItemsPendingRemoval = new ArrayList<>();
            mHandler = new Handler();
            mPendingRunnables = new HashMap<>();
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        /**
         * Where things go before they get removed
         * @param position where the item to be removed is
         */
        void pendingRemoval(int position) {
            final E item = mItems.get(position);
            if (!mItemsPendingRemoval.contains(item)) {
                mItemsPendingRemoval.add(item);
                notifyItemChanged(position);
                Runnable pendingRemovalRunnable = new Runnable() {
                    @Override
                    public void run() {
                        remove(mItems.indexOf(item));
                    }
                };
                PreferenceAdapter pa = new PreferenceAdapter(getContext());
                mHandler.postDelayed(pendingRemovalRunnable, pa.getUndoTimeout());
                mPendingRunnables.put(item, pendingRemovalRunnable);
            }
        }

        /**
         * Properly go about removing an item from the list
         * @param position where the item to remove is
         */
        public void remove(int position) {
            final E item = mItems.get(position);
            if (mItemsPendingRemoval.contains(item)) {
                mItemsPendingRemoval.remove(item);
            }
            if (mItems.contains(item)) {
                mItems.remove(item);
                /* The items that change are including and after position */
                notifyItemRangeChanged(position, mItems.size() - position);
            }
        };

        public void setOnClickListener(View.OnClickListener clickListener) {
            mClickListener = clickListener;
        }

        abstract class ViewHolder extends RecyclerView.ViewHolder {

            TextView mCardName;
            TextView mUndoButton;

            boolean swipeable = true;

            ViewHolder(ViewGroup view, @LayoutRes int listRowLayout) {
                super(LayoutInflater.from(view.getContext()).inflate(listRowLayout, view, false));
                mCardName = (TextView) itemView.findViewById(R.id.card_name);
                mUndoButton = (TextView) itemView.findViewById(R.id.undo_button);
            }

        }

    }

}
