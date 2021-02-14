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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.astuetz.PagerSlidingTabStrip;
import com.gelakinetic.mtgfam.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This fragment nests a few other fragments which are useful for judges
 */
public class JudgesCornerFragment extends FamiliarFragment {

    /* Key and constants for displaying the MTR and IPG */
    public static final String HTML_DOC = "html";
    public static final String PAGE_NAME = "page_name";
    public static final String MTR_LOCAL_FILE = "MTR.html";
    public static final String IPG_LOCAL_FILE = "IPG.html";
    public static final String JAR_LOCAL_FILE = "JAR.html";
    public static final String DMTR_LOCAL_FILE = "DMTR.html";
    public static final String DIPG_LOCAL_FILE = "DIPG.html";
    /* Constants to keep track of tabs */
    private static final String TAG_MTR = "MTR";
    private static final String TAG_IPG = "IPG";
    private static final String TAG_JAR = "JAR";
    private static final String TAG_DMTR = "DMTR";
    private static final String TAG_DIPG = "DIPG";
    private static final String TAG_COUNTER = "COUNTER";

    /**
     * List of {@link com.gelakinetic.mtgfam.fragments.JudgesCornerFragment.PagerItem} which represent this sample's tabs.
     */
    private final List<PagerItem> mTabs = new ArrayList<>();
    private ViewPager mViewPager = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Populate our tab list with tabs. Each item contains a title, indicator color and divider
         * color, which are used by {@link SlidingTabLayout}.
         */
        mTabs.add(new PagerItem(getString(R.string.judges_corner_MTR), TAG_MTR));
        mTabs.add(new PagerItem(getString(R.string.judges_corner_IPG), TAG_IPG));
        mTabs.add(new PagerItem(getString(R.string.judges_corner_JAR), TAG_JAR));
        mTabs.add(new PagerItem(getString(R.string.judges_corner_DMTR), TAG_DMTR));
        mTabs.add(new PagerItem(getString(R.string.judges_corner_DIPG), TAG_DIPG));
        mTabs.add(new PagerItem(getString(R.string.judges_corner_counter), TAG_COUNTER));
    }

    /**
     * Inflates the {@link View} which will be displayed by this {@link Fragment}, from the app's
     * resources.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.judges_corner_frag, container, false);
    }

    /**
     * This is called after the {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has finished.
     * Here we can pick out the {@link View}s we need to configure from the content view.
     * <p>
     * We set the {@link ViewPager}'s adapter to be an instance of
     * {@link JudgeFragmentPagerAdapter}. The {@link PagerSlidingTabStrip} is then given the
     * {@link ViewPager} so that it can populate itself.
     *
     * @param view View created in {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        /* A {@link ViewPager} which will be used in conjunction with the {@link SlidingTabLayout} above. */
        mViewPager = view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new JudgeFragmentPagerAdapter(getChildFragmentManager()));
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
                // Don't care
            }

            @Override
            public void onPageSelected(int i) {
                Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                // Don't care
            }
        });

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = view.findViewById(R.id.sliding_tabs);
        tabs.setViewPager(mViewPager);
    }

    public FamiliarFragment getCurrentFragment() {
        return ((JudgeFragmentPagerAdapter) Objects.requireNonNull(mViewPager.getAdapter())).getCurrentFragment();
    }

    /**
     * This class represents a tab to be displayed by {@link ViewPager}
     */
    class PagerItem {
        private final CharSequence mTitle;
        private final String mFragmentType;
        FamiliarFragment mFragment;

        PagerItem(CharSequence title, String fragmentType) {
            mTitle = title;
            mFragmentType = fragmentType;
            mFragment = null;
        }

        /**
         * @return A new {@link Fragment} to be displayed by a {@link ViewPager}
         */
        FamiliarFragment createFragment() {

            if (null == mFragment) {
                switch (mFragmentType) {
                    case TAG_MTR: {
                        Bundle MtrBundle = new Bundle();
                        MtrBundle.putString(HTML_DOC, MTR_LOCAL_FILE);
                        MtrBundle.putString(PAGE_NAME, getString(R.string.judges_corner_MTR));
                        mFragment = new HtmlDocFragment();
                        mFragment.setArguments(MtrBundle);
                        break;
                    }
                    case TAG_IPG: {
                        Bundle IpgBundle = new Bundle();
                        IpgBundle.putString(HTML_DOC, IPG_LOCAL_FILE);
                        IpgBundle.putString(PAGE_NAME, getString(R.string.judges_corner_IPG));
                        mFragment = new HtmlDocFragment();
                        mFragment.setArguments(IpgBundle);
                        break;
                    }
                    case TAG_JAR: {
                        Bundle JarBundle = new Bundle();
                        JarBundle.putString(HTML_DOC, JAR_LOCAL_FILE);
                        JarBundle.putString(PAGE_NAME, getString(R.string.judges_corner_JAR));
                        mFragment = new HtmlDocFragment();
                        mFragment.setArguments(JarBundle);
                        break;
                    }
                    case TAG_DMTR: {
                        Bundle dMtrBundle = new Bundle();
                        dMtrBundle.putString(HTML_DOC, DMTR_LOCAL_FILE);
                        dMtrBundle.putString(PAGE_NAME, getString(R.string.judges_corner_DMTR));
                        mFragment = new HtmlDocFragment();
                        mFragment.setArguments(dMtrBundle);
                        break;
                    }
                    case TAG_DIPG: {
                        Bundle dIpgBundle = new Bundle();
                        dIpgBundle.putString(HTML_DOC, DIPG_LOCAL_FILE);
                        dIpgBundle.putString(PAGE_NAME, getString(R.string.judges_corner_DIPG));
                        mFragment = new HtmlDocFragment();
                        mFragment.setArguments(dIpgBundle);
                        break;
                    }
                    default:
                    case TAG_COUNTER: {
                        mFragment = new DeckCounterFragment();
                        break;
                    }
                }
            }
            return mFragment;
        }

        /**
         * @return the title which represents this tab. In this sample this is used directly by
         * {@link PagerAdapter#getPageTitle(int)}
         */
        CharSequence getTitle() {
            return mTitle;
        }
    }

    /**
     * The {@link FragmentPagerAdapter} used to display pages in this sample. The individual pages
     * are instances of ContentFragment which just display three lines of text. Each page is
     * created by the relevant {@link com.gelakinetic.mtgfam.fragments.JudgesCornerFragment.PagerItem} for the requested position.
     * <p>
     * The important section of this class is the {@link #getPageTitle(int)} method which controls
     * what is displayed in the {@link PagerSlidingTabStrip}.
     */
    class JudgeFragmentPagerAdapter extends FragmentPagerAdapter {

        private FamiliarFragment mCurrentFragment;

        JudgeFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        /**
         * Return the {@link Fragment} to be displayed at {@code position}.
         * <p>
         * Here we return the value returned from {@link com.gelakinetic.mtgfam.fragments.JudgesCornerFragment.PagerItem#createFragment()}.
         */
        @Override
        public @NonNull FamiliarFragment getItem(int i) {
            return mTabs.get(i).createFragment();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        /**
         * @return Returns the current fragment being displayed by this adapter
         */
        FamiliarFragment getCurrentFragment() {
            return mCurrentFragment;
        }

        /**
         * Return the title of the item at {@code position}. This is important as what this method
         * returns is what is displayed in the {@link PagerSlidingTabStrip}.
         * <p>
         * Here we return the value returned from {@link com.gelakinetic.mtgfam.fragments.JudgesCornerFragment.PagerItem#getTitle()}.
         */
        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs.get(position).getTitle();
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
                mCurrentFragment = (FamiliarFragment) object;
            }
            super.setPrimaryItem(container, position, object);
        }
    }

    /**
     * Ask the current fragment whether it can intercept the search key
     *
     * @return true if it can, false otherwise
     */
    @Override
    boolean canInterceptSearchKey() {
        return ((JudgeFragmentPagerAdapter) Objects.requireNonNull(mViewPager.getAdapter())).getItem(mViewPager.getCurrentItem()).canInterceptSearchKey();
    }

    /**
     * Intercept the search key, if the current fragment supports it
     *
     * @return true if the button was handled, false otherwise
     */
    @Override
    public boolean onInterceptSearchKey() {
        return ((JudgeFragmentPagerAdapter) Objects.requireNonNull(mViewPager.getAdapter())).getItem(mViewPager.getCurrentItem()).onInterceptSearchKey();
    }
}
