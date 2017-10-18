package com.gelakinetic.mtgfam.helpers;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that holds whatever, swipes to delete with a undo timeout, and multi-select of items.
 *
 * @param <T> type of what is held in the ArrayList
 * @param <VH> the ViewHolder that is used for the adapter
 */
public abstract class SelectableItemAdapter<T, VH extends SelectableItemAdapter.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    protected final List<T> items;

    private boolean inSelectMode;
    protected final SparseBooleanArray selectedItems;

    protected final Handler handler;
    protected final SparseArray<Runnable> pendingRunnables;

    protected final int pendingTimeout;

    public SelectableItemAdapter(ArrayList<T> values, final int millisPending) {
        items = values;
        selectedItems = new SparseBooleanArray();
        handler = new Handler();
        inSelectMode = false;
        pendingRunnables = new SparseArray<>();
        pendingTimeout = millisPending;
    }

    /**
     * Properly go about removing an item from the list.
     * @param position where the item to remove is
     */
    public void remove(final int position) {

        try {
            final T item = items.get(position);
            if (pendingRunnables.indexOfKey(position) > -1) {
                pendingRunnables.remove(position);
            }
            if (items.contains(item)) {
                items.remove(position);
                notifyItemRemoved(position);
            }
        } catch (IndexOutOfBoundsException oob) {
            /* Happens from time to time, shouldn't worry about it */
        }

    }

    /**
     * Where things go before they get removed.
     * @param position where the item to be removed is
     */
    public boolean pendingRemoval(final int position) {
        if (pendingRunnables.indexOfKey(position) < 0) {
            notifyItemChanged(position);
            Runnable pendingRunnable = new Runnable() {
                @Override
                public void run() {
                    remove(position);
                }
            };
            handler.postDelayed(pendingRunnable, pendingTimeout);
            pendingRunnables.put(position, pendingRunnable);
            return true;
        }
        return false;
    }

    /**
     * Execute any pending Runnables NOW. This generally means we are moving away from this
     * fragment.
     */
    public void removePendingNow() {
        for (int i = 0; i < pendingRunnables.size(); i++) {
            Runnable runnable = pendingRunnables.valueAt(i);
            handler.removeCallbacks(runnable);
            runnable.run();
        }
        pendingRunnables.clear();
    }

    /**
     * If we are in select mode.
     * @return inSelectMode
     */
    public boolean isInSelectMode() {
        return inSelectMode;
    }

    /**
     * Set the select mode.
     * @param inSelectMode if we are in select mode or not
     */
    public void setInSelectMode(final boolean inSelectMode) {
        this.inSelectMode = inSelectMode;
    }

    public ArrayList<T> getSelectedItems() {

        ArrayList<T> selectedItemsLocal = new ArrayList<>();
        for (int i = 0; i < this.selectedItems.size(); i++) {
            if (this.selectedItems.valueAt(i)) {
                selectedItemsLocal.add(items.get(this.selectedItems.keyAt(i)));
            }
        }
        return selectedItemsLocal;

    }

    public void deleteSelectedItems() {
        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i)) {
                remove(selectedItems.keyAt(i));
            }
        }
    }

    public boolean isItemPendingRemoval(final int position) {
        return pendingRunnables.indexOfKey(position) > -1;
    }

    public void deselectAll() {

        selectedItems.clear();
        setInSelectMode(false);
        notifyDataSetChanged();

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void onItemDismissed(final int position) {
        pendingRemoval(position);
        notifyItemChanged(position);
    }

    /**
     * ViewHolder implementing the needed listeners and defining swipeable and its get/setters.
     */
    public abstract class ViewHolder
            extends RecyclerView.ViewHolder
            implements OnClickListener, OnLongClickListener {

        private boolean isSwipeable = true;

        public ViewHolder(View view) {
            super(view);
        }

        public boolean getIsSwipeable() {
            return isSwipeable;
        }

        public void setIsSwipeable(final boolean isSwipeable) {
            this.isSwipeable = isSwipeable;
        }

    }
}
