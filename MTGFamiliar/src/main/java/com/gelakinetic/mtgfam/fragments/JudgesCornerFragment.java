/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.gelakinetic.mtgfam.R;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment nests a few other fragments which are useful for judges
 */
public class JudgesCornerFragment extends FamiliarFragment {

    /* Key and constants for displaying the MTR and IPG */
    public static final String HTML_DOC = "html";
    public static final String MTR_LOCAL_FILE = "MTR.html";
    public static final String IPG_LOCAL_FILE = "IPG.html";
    public static final String JAR_LOCAL_FILE = "JAR.html";
    /* Constants to keep track of tabs */
    private static final String TAG_MTR = "MTR";
    private static final String TAG_IPG = "IPG";
    private static final String TAG_JAR = "JAR";
    private static final String TAG_COUNTER = "COUNTER";

    /**
     * List of {@link com.gelakinetic.mtgfam.fragments.JudgesCornerFragment.PagerItem} which represent this sample's tabs.
     */
    private final List<PagerItem> mTabs = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Populate our tab list with tabs. Each item contains a title, indicator color and divider
         * color, which are used by {@link SlidingTabLayout}.
         */
        mTabs.add(new PagerItem(getString(R.string.judges_corner_MTR), TAG_MTR));
        mTabs.add(new PagerItem(getString(R.string.judges_corner_IPG), TAG_IPG));
        mTabs.add(new PagerItem(getString(R.string.judges_corner_JAR), TAG_JAR));
        mTabs.add(new PagerItem(getString(R.string.judges_corner_counter), TAG_COUNTER));
    }

    /**
     * Inflates the {@link View} which will be displayed by this {@link Fragment}, from the app's
     * resources.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.judges_corner_frag, container, false);
    }

    /**
     * This is called after the {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has finished.
     * Here we can pick out the {@link View}s we need to configure from the content view.
     * <p/>
     * We set the {@link ViewPager}'s adapter to be an instance of
     * {@link SampleFragmentPagerAdapter}. The {@link PagerSlidingTabStrip} is then given the
     * {@link ViewPager} so that it can populate itself.
     *
     * @param view View created in {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        /* A {@link ViewPager} which will be used in conjunction with the {@link SlidingTabLayout} above. */
        ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        viewPager.setAdapter(new SampleFragmentPagerAdapter(getChildFragmentManager()));
        viewPager.setOffscreenPageLimit(2);

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) view.findViewById(R.id.sliding_tabs);
        tabs.setViewPager(viewPager);
    }

    /**
     * This class represents a tab to be displayed by {@link ViewPager}
     */
    class PagerItem {
        private final CharSequence mTitle;
        private final String mFragmentType;

        PagerItem(CharSequence title, String fragmentType) {
            mTitle = title;
            mFragmentType = fragmentType;
        }

        /**
         * @return A new {@link Fragment} to be displayed by a {@link ViewPager}
         */
        Fragment createFragment() {

            if (mFragmentType.equals(TAG_MTR)) {
                Bundle MtrBundle = new Bundle();
                MtrBundle.putString(HTML_DOC, MTR_LOCAL_FILE);
                FamiliarFragment frag = new HtmlDocFragment();
                frag.setArguments(MtrBundle);
                return frag;
            }
            if (mFragmentType.equals(TAG_IPG)) {
                Bundle IpgBundle = new Bundle();
                IpgBundle.putString(HTML_DOC, IPG_LOCAL_FILE);
                FamiliarFragment frag = new HtmlDocFragment();
                frag.setArguments(IpgBundle);
                return frag;
            }
            if (mFragmentType.equals(TAG_JAR)) {
                Bundle JarBundle = new Bundle();
                JarBundle.putString(HTML_DOC, JAR_LOCAL_FILE);
                FamiliarFragment frag = new HtmlDocFragment();
                frag.setArguments(JarBundle);
                return frag;
            }
            if (mFragmentType.equals(TAG_COUNTER)) {
                return new DeckCounterFragment();
            }
            return new DeckCounterFragment();
        }

        /**
         * @return the title which represents this tab. In this sample this is used directly by
         * {@link android.support.v4.view.PagerAdapter#getPageTitle(int)}
         */
        CharSequence getTitle() {
            return mTitle;
        }
    }

    /**
     * The {@link FragmentPagerAdapter} used to display pages in this sample. The individual pages
     * are instances of ContentFragment which just display three lines of text. Each page is
     * created by the relevant {@link com.gelakinetic.mtgfam.fragments.JudgesCornerFragment.PagerItem} for the requested position.
     * <p/>
     * The important section of this class is the {@link #getPageTitle(int)} method which controls
     * what is displayed in the {@link PagerSlidingTabStrip}.
     */
    class SampleFragmentPagerAdapter extends FragmentPagerAdapter {

        SampleFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        /**
         * Return the {@link android.support.v4.app.Fragment} to be displayed at {@code position}.
         * <p/>
         * Here we return the value returned from {@link com.gelakinetic.mtgfam.fragments.JudgesCornerFragment.PagerItem#createFragment()}.
         */
        @Override
        public Fragment getItem(int i) {
            return mTabs.get(i).createFragment();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        /**
         * Return the title of the item at {@code position}. This is important as what this method
         * returns is what is displayed in the {@link PagerSlidingTabStrip}.
         * <p/>
         * Here we return the value returned from {@link com.gelakinetic.mtgfam.fragments.JudgesCornerFragment.PagerItem#getTitle()}.
         */
        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs.get(position).getTitle();
        }
    }
}
