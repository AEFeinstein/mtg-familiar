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

package com.gelakinetic.mtgfam.fragments;

import android.animation.Animator;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

public class ManaPoolFragment extends FamiliarFragment {

    private class ManaPoolItem {

        private final ImageView mPlus;
        private final ImageView mMinus;
        private final TextView mReadout;
        private int mCount;
        @StringRes
        private final int mKeyResId;

        /**
         * Create a mana pool item
         *
         * @param parent       The view this item exists in
         * @param plusResId    The resource ID for the [+] button
         * @param minusResId   The resource ID for the [-] button
         * @param readoutResId The resource ID for the text readout
         * @param keyResId     The string resource ID for the key to save this value
         */
        ManaPoolItem(View parent, @IdRes int plusResId, @IdRes int minusResId,
                     @IdRes int readoutResId, @StringRes int keyResId) {
            mCount = 0;
            mReadout = parent.findViewById(readoutResId);
            mKeyResId = keyResId;
            mPlus = parent.findViewById(plusResId);
            mMinus = parent.findViewById(minusResId);
            mPlus.setOnClickListener((View v) -> {
                mCount++;
                updateReadout();
                updateVisibility();
                showMovingManaAnimation(mPlus, mMinus, mPlus.getDrawable(), true);
            });
            mMinus.setOnClickListener((View v) -> {
                mCount--;
                if (mCount < 0) {
                    mCount = 0;
                    return;
                }
                updateReadout();
                updateVisibility();
                showMovingManaAnimation(mMinus, mPlus, mPlus.getDrawable(), false);
            });
        }

        /**
         * Clear the count and update the readout
         */
        void clearCount() {
            mCount = 0;
            updateReadout();
            updateVisibility();
        }

        /**
         * Update the readout with the current count
         */
        void updateReadout() {
            if (mCount == 0 || mCount == 1) {
                mReadout.setText("");
            }
            else {
                mReadout.setText(String.format(Locale.getDefault(), "%d", mCount));
            }
        }

        private void updateVisibility() {
            mMinus.animate().alpha(mCount == 0 ? 0.1f : 1.0f);
        }

        /**
         * Use the given String key to save this count from shared prefrences and display it
         */
        void loadCount() {
            mCount = PreferenceAdapter.getMana(getContext(), mKeyResId);
            updateReadout();
            updateVisibility();
        }

        /**
         * Use the given String key to load this count from shared prefrences
         */
        void saveCount() {
            PreferenceAdapter.setMana(getContext(), mKeyResId, mCount);
        }
    }

    private final ArrayList<ManaPoolItem> mManaPoolItems = new ArrayList<>();
    private final Stack<ImageView> mMovingMana = new Stack<>();
    private ViewGroup parentView;

    /**
     * Create the view and set up the buttons
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The view to be displayed
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        parentView = (ViewGroup) inflater.inflate(R.layout.mana_pool_frag2, container, false);

        /* Clear out the mana pool items, just in case */
        mManaPoolItems.clear();

        /* Create and save all the mana pool items */
        mManaPoolItems.add(new ManaPoolItem(parentView, R.id.white_plus, R.id.white_minus,
                R.id.white_readout, R.string.key_whiteMana));
        mManaPoolItems.add(new ManaPoolItem(parentView, R.id.blue_plus, R.id.blue_minus,
                R.id.blue_readout, R.string.key_blueMana));
        mManaPoolItems.add(new ManaPoolItem(parentView, R.id.black_plus, R.id.black_minus,
                R.id.black_readout, R.string.key_blackMana));
        mManaPoolItems.add(new ManaPoolItem(parentView, R.id.red_plus, R.id.red_minus,
                R.id.red_readout, R.string.key_redMana));
        mManaPoolItems.add(new ManaPoolItem(parentView, R.id.green_plus, R.id.green_minus,
                R.id.green_readout, R.string.key_greenMana));
        mManaPoolItems.add(new ManaPoolItem(parentView, R.id.colorless_plus, R.id.colorless_minus,
                R.id.colorless_readout, R.string.key_colorlessMana));
        mManaPoolItems.add(new ManaPoolItem(parentView, R.id.energy_plus, R.id.energy_minus,
                R.id.energy_readout, R.string.key_energy));
        mManaPoolItems.add(new ManaPoolItem(parentView, R.id.spell_plus, R.id.spell_minus,
                R.id.spell_readout, R.string.key_spellCount));

        return parentView;
    }

    void showMovingManaAnimation(ImageView from, ImageView to, Drawable drawable, boolean grow) {

        ImageView imageView;
        if (mMovingMana.isEmpty()) {
            imageView = new ImageView(getContext());
        }
        else {
            imageView = mMovingMana.pop();
        }

        parentView.addView(imageView);
        imageView.setImageDrawable(drawable);

        int fromCoords[] = new int[2];
        from.getLocationOnScreen(fromCoords);
        int toCoords[] = new int[2];
        to.getLocationOnScreen(toCoords);

        Rect fromRect = new Rect();
        from.getLocalVisibleRect(fromRect);

        float startingScale = grow ? 1.0f : 2.0f;
        float endingScale = grow ? 2.0f : 0.75f;

        imageView.setX(fromCoords[0]);
        imageView.setY(fromCoords[1]);
        imageView.setScaleX(startingScale);
        imageView.setScaleY(startingScale);
        imageView.setAlpha(1.0f);
        imageView.animate()
                .x(toCoords[0])
                .y(toCoords[1])
                .scaleX(endingScale)
                .scaleY(endingScale)
                .alpha(0.5f)
                .setDuration(450)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(final Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(final Animator animation) {
                        parentView.removeView(imageView);
                        mMovingMana.push(imageView);
                    }

                    @Override
                    public void onAnimationCancel(final Animator animation) {
                        onAnimationEnd(animation);
                    }

                    @Override
                    public void onAnimationRepeat(final Animator animation) {

                    }
                });
    }

    /**
     * When the fragment pauses, save the mana values
     */
    @Override
    public void onPause() {
        super.onPause();
        for (ManaPoolItem item : mManaPoolItems) {
            item.saveCount();
        }
    }

    /**
     * When the fragment resumes, load the mana values and display them
     */
    @Override
    public void onResume() {
        super.onResume();
        for (ManaPoolItem item : mManaPoolItems) {
            item.loadCount();
        }
    }

    /**
     * Handle menu clicks, in this case, just clear all
     *
     * @param item The MenuItem which was selected
     * @return True if the event was acted upon, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_all:
                for (ManaPoolItem manaPoolItem : mManaPoolItems) {
                    manaPoolItem.clearCount();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Inflate the options menu
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.manapool_menu, menu);
    }
}