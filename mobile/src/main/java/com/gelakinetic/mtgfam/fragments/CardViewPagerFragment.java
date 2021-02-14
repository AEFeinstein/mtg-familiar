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

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.gelakinetic.mtgfam.R;

import java.util.Objects;

/**
 * This class will nest the CardViewFragments found by a search in a ViewPager
 */
public class CardViewPagerFragment extends FamiliarFragment {

    /* Bundle keys */
    public static final String CARD_ID_ARRAY = "card_id_array";
    public static final String STARTING_CARD_POSITION = "starting_card_id";
    private ViewPager mViewPager;

    /**
     * @return The currently viewed CardViewFragment in the CardViewPagerFragment
     */
    public CardViewFragment getCurrentFragment() {
        return ((CardViewPagerAdapter) Objects.requireNonNull(mViewPager.getAdapter())).getCurrentFragment();
    }

    /**
     * Assume that every fragment has a menu
     * Assume that every fragment wants to retain it's instance state (onCreate/onDestroy called
     * once, onCreateView called on rotations etc)
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(false);

        /* add a fragment */
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Random rand = new Random(System.currentTimeMillis());
//                Bundle args = new Bundle();
//                args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, new long[]{1 + rand.nextInt(1000)});
//                args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
//                CardViewPagerFragment cvpFrag = new CardViewPagerFragment();
//                startNewFragment(cvpFrag, args);
//            }
//        }, 3000);
    }

    /**
     * Grab the array of card IDs and the current position, then create the view and attach the pager adapter
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The inflated view
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /* Instantiate a ViewPager and a PagerAdapter. */
        View v = inflater.inflate(R.layout.card_view_pager, container, false);
        assert v != null; /* Because Android Studio */
        mViewPager = v.findViewById(R.id.pager);

        /* Retain the instance */
        if (getParentFragment() == null) {
            this.setRetainInstance(true);
        }

        Bundle args = getArguments();
        long[] cardIds = Objects.requireNonNull(args).getLongArray(CARD_ID_ARRAY);
        int currentPosition = args.getInt(STARTING_CARD_POSITION);

        CardViewPagerAdapter pagerAdapter = new CardViewPagerAdapter(getChildFragmentManager(), cardIds);
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.setCurrentItem(currentPosition);
        mViewPager.setPageTransformer(true, new DepthPageTransformer());

        return v;
    }

    /**
     * Callback for when a permission is requested
     *
     * @param requestCode  The request code passed in requestPermissions(String[], int).
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either
     *                     android.content.pm.PackageManager.PERMISSION_GRANTED or
     *                     android.content.pm.PackageManager.PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (null != mViewPager) {
            CardViewPagerAdapter adapter = (CardViewPagerAdapter) mViewPager.getAdapter();
            if (null != adapter) {
                CardViewFragment frag = adapter.getCurrentFragment();
                if (null != frag) {
                    frag.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
            }
        }
    }

    /**
     * A simple pager adapter that holds CardViewFragments
     */
    private static class CardViewPagerAdapter extends FragmentPagerAdapter {
        final long[] mCardIds;
        private CardViewFragment mCurrentFragment;

        /**
         * Default Constructor
         *
         * @param fm      The FragmentManager which handles the fragments
         * @param cardIds The array of card IDs to make fragments with
         */
        CardViewPagerAdapter(FragmentManager fm, long[] cardIds) {
            super(fm);
            this.mCardIds = cardIds;
        }

        /**
         * Override this to do nothing and return nothing in order to fix a bug where orientation
         * changes will cause a vague NullPointerException
         *
         * @return Nothing, who cares?
         */
        @Override
        public Parcelable saveState() {
            return null;
        }

        /**
         * Make a fragment using the ID at this position in mCardIds, and return it
         *
         * @param position The index of the Fragment to make
         * @return The Fragment at that index
         */
        @Override
        public @NonNull Fragment getItem(int position) {
            CardViewFragment cvf = new CardViewFragment();
            Bundle args = new Bundle();
            args.putLong(CardViewFragment.CARD_ID, mCardIds[position]);
            cvf.setArguments(args);
            return cvf;
        }

        /**
         * Returns the count of all the items in the ViewPager
         *
         * @return the count of all the items in the ViewPager
         */
        @Override
        public int getCount() {
            return mCardIds.length;
        }

        /**
         * @return Returns the current fragment being displayed by this adapter
         */
        CardViewFragment getCurrentFragment() {
            return mCurrentFragment;
        }

        /**
         * Called to inform the adapter of which item is currently considered to be the "primary",
         * that is the one show to the user as the current page.
         * Also keeps track of the currently displayed fragment
         *
         * @param container The containing View from which the page will be removed.
         * @param position  The page position that is now the primary.
         * @param object    The same object that was returned by instantiateItem(View, int).
         */
        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            if (getCurrentFragment() != object) {
                mCurrentFragment = ((CardViewFragment) object);
            }
            super.setPrimaryItem(container, position, object);
        }
    }

    /**
     * Just to be fancy, lets spice up the transformation
     * http://developer.android.com/training/animation/screen-slide.html
     */
    static class DepthPageTransformer implements ViewPager.PageTransformer {

        /**
         * A custom transformer to get a sweet page effect
         *
         * @param view     The view being transformed
         * @param position Where the view currently is
         */
        public void transformPage(@NonNull View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) { /* [-Infinity,-1)
                This page is way off-screen to the left. */
                view.setAlpha(0);
            } else if (position <= 0) { /* [-1,0]
                Use the default slide transition when moving to the left page */
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);
                view.setAlpha(1);
            } else if (position <= 1) { /* (0,1]
                Fade the page out. */
                view.setAlpha(1 - position);

                /* Counteract the default slide transition */
                view.setTranslationX(pageWidth * -position);

                /* Scale the page down (between MIN_SCALE and 1) */
                float MIN_SCALE = 0.75f;
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { /* (1,+Infinity] */
                /* This page is way off-screen to the right. */
                view.setAlpha(0);
            }
        }
    }
}
