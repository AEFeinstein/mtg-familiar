package com.gelakinetic.mtgfam.helpers;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback;

/**
 * Created by bmaurer on 6/21/2017.
 */

public class SelectableItemTouchHelper extends SimpleCallback {

    private final SelectableItemAdapter adapter;

    public SelectableItemTouchHelper(final SelectableItemAdapter adapter, final int swipeDirs) {
        super(0, swipeDirs);
        this.adapter = adapter;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        adapter.onItemDismissed(viewHolder.getAdapterPosition());
    }

    @Override
    public int getSwipeDirs(RecyclerView parent, RecyclerView.ViewHolder holder) {
        if (holder instanceof SelectableItemAdapter.ViewHolder) {
            if (!((SelectableItemAdapter.ViewHolder) holder).getIsSwipeable()) {
                return 0;
            }
        }
        return super.getSwipeDirs(parent, holder);
    }

    @Override
    public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            final float alpha = 1.0f - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
            viewHolder.itemView.setAlpha(alpha);
            viewHolder.itemView.setTranslationX(dX);
        } else {
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

}
