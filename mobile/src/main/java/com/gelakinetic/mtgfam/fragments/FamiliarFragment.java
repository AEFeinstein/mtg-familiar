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

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;

import java.util.Objects;

/**
 * This is the superclass for all fragments. It has a bunch of convenient methods
 */
public abstract class FamiliarFragment extends Fragment {

    boolean mIsSearchViewOpen = false;
    Runnable mAfterSearchClosedRunnable = null;

    /**
     * http://developer.android.com/reference/android/app/Fragment.html
     * All subclasses of Fragment must include a public empty constructor. The framework will often re-instantiate a
     * fragment class when needed, in particular during state restore, and needs to be able to find this constructor
     * to instantiate it. If the empty constructor is not available, a runtime exception will occur in some cases during
     * state restore.
     */
    FamiliarFragment() {
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
        this.setHasOptionsMenu(true);
    }

    /**
     * Called when the fragment is no longer attached to its activity.  This
     * is called after {@link #onDestroy()}.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        getFamiliarActivity().clearLoading();
        getFamiliarActivity().mMarketPriceStore.stopAllRequests();
    }

    /**
     * Force the child fragments to override onCreateView
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
    public abstract View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    /**
     * Clear any results from the prior fragment. We don't want them persisting past this fragment,
     * and they should have been looked at by now anyway
     */
    @Override
    public void onResume() {
        super.onResume();
        if ((getActivity()) != null) {
            getFamiliarActivity().getFragmentResults();
            getFamiliarActivity().mDrawerLayout.closeDrawer(getFamiliarActivity().mDrawerList);
            getFamiliarActivity().selectDrawerEntry(this.getClass());
        }
    }

    /**
     * first saving my state, so the bundle wont be empty.
     * http://code.google.com/p/android/issues/detail?id=19917
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (outState.isEmpty()) {
            outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        }
        super.onSaveInstanceState(outState);
        FamiliarActivity.logBundleSize("OSSI " + this.getClass().getName(), outState);
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to Activity.onPause of the containing Activity's lifecycle.
     * <p>
     * In this case, always remove the dialog, since it can contain stale references to the pre-rotated activity and
     * fragment after rotation. The one exception is the change log dialog, which would get removed by TTS checking
     * intents on the first install. It also cleans up any pending spice requests (price loading)
     */
    @Override
    public void onPause() {
        super.onPause();
        removeDialog(getFragmentManager());
    }

    /**
     * This will inflate the menu as usual. It also adds a SearchView to the ActionBar if the
     * Fragment does not override the search key, or a search button if it does override the key
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();

        if (getActivity() != null) {
            if (canInterceptSearchKey()) {
                menu.add(R.string.search_search)
                        .setIcon(R.drawable.ic_menu_search)
                        .setOnMenuItemClickListener(item -> {
                            onInterceptSearchKey();
                            return true;
                        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else {

                SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
                if (null == searchManager) {
                    return;
                }
                SearchView sv = new SearchView(getActivity());
                try {
                    // Try to get the package name, important for debug builds
                    String packageName = null;
                    if (null != getContext()) {
                        packageName = getContext().getPackageName();
                    }
                    // Default to the production package name
                    if (null == packageName) {
                        packageName = "com.gelakinetic.mtgfam";
                    }
                    sv.setSearchableInfo(searchManager.getSearchableInfo(
                            new ComponentName(packageName, "com.gelakinetic.mtgfam.FamiliarActivity")));

                    MenuItem mi = menu.add(R.string.name_search_hint)
                            .setIcon(R.drawable.ic_menu_search);
                    mi.setActionView(sv);
                    mi.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                        @Override
                        public boolean onMenuItemActionExpand(MenuItem item) {
                            mIsSearchViewOpen = true;
                            return true;
                        }

                        @Override
                        public boolean onMenuItemActionCollapse(MenuItem item) {
                            mIsSearchViewOpen = false;
                            if (null != mAfterSearchClosedRunnable) {
                                mAfterSearchClosedRunnable.run();
                                mAfterSearchClosedRunnable = null;
                            }
                            return true;
                        }
                    });
                    mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

                } catch (RuntimeException e) {
                    /* One user threw this once. I think the typed ComponentName fixes it, but just in case */
                }
            }

        }
    }

    /**
     * This is called when the hardware search key is pressed. If it is overridden, it should return true
     *
     * @return true if this fragment did anything with the key, false if otherwise
     */
    public boolean onInterceptSearchKey() {
        return false;
    }

    /**
     * This function is checked when building the menu. If the fragment can intercept the search key, an ActionSearch
     * won't be created
     *
     * @return True if this fragment overrides the hardware search key, false if otherwise
     */
    boolean canInterceptSearchKey() {
        return false;
    }

    /**
     * This starts a new fragment in the main fragment container
     *
     * @param frag The fragment to start
     * @param args Any arguments which the fragment takes
     */
    public void startNewFragment(FamiliarFragment frag, Bundle args) {
        try {
            FragmentManager fm = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
            if (fm != null) {
                frag.setArguments(args);
                FragmentTransaction ft = fm.beginTransaction();
                ft.replace(R.id.fragment_container, frag, FamiliarActivity.FRAGMENT_TAG);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                if (getActivity() != null) {
                    getFamiliarActivity().hideKeyboard();
                }
            }
        } catch (IllegalStateException | NullPointerException e) {
            // If the fragment can't be shown, fail quietly
        }
    }

    /**
     * This removes any currently open dialog
     */
    public void removeDialog(FragmentManager fm) {
        try {
            getFamiliarActivity().removeDialogFragment(fm);
        } catch (NullPointerException e) {
            /* no dialog to close */
        }
    }

    /**
     * Called when someone catches a FamiliarDbException. It can remove the current fragment, or close the app if there
     * is nothing on the back stack.
     *
     * @param shouldFinish if the current fragment should exit
     */
    public void handleFamiliarDbException(boolean shouldFinish) {
        /* Show a toast on the UI thread */
        FragmentActivity activity = getActivity();
        if (null != activity) {
            activity.runOnUiThread(() -> SnackbarWrapper.makeAndShowText(getActivity(), R.string.error_database, SnackbarWrapper.LENGTH_LONG));
            /* Finish the fragment if requested */
            if (shouldFinish) {
                try {
                    /* will be correct for nested ViewPager fragments too */
                    FragmentManager fm = activity.getSupportFragmentManager();
                    if (fm != null) {
                        /* If there is only one fragment, finish the activity
                         * Otherwise pop the offending fragment */
                        if (fm.getFragments().size() == 1) {
                            activity.finish();
                        } else if (!fm.isStateSaved()) {
                            fm.popBackStack();
                        }
                    }
                } catch (IllegalStateException e) {
                    /* Just give up, hopefully the toast was shown */
                    activity.finish();
                }
            }
        }
    }

    /**
     * Override this to be notified when the user is inactive
     */
    public void onUserInactive() {

    }

    /**
     * Override this to be notified when the user is active again
     */
    public void onUserActive() {
    }

    /**
     * Override this to be notified when the wishlist changes
     */
    public void onWishlistChanged(String cardName) {

    }

    /**
     * Convenience method for getting the FamiliarActivity parent for this fragment
     *
     * @return The FamiliarActivity
     */
    public FamiliarActivity getFamiliarActivity() {
        if (getActivity() instanceof FamiliarActivity) {
            return (FamiliarActivity) getActivity();
        }
        return null;
    }

    /**
     * Convenience method for getting a resource ID given an attr
     *
     * @param attr The attr to get an ID for
     * @return The resource ID
     */
    public int getResourceIdFromAttr(int attr) {
        return ((FamiliarActivity) Objects.requireNonNull(getActivity())).getResourceIdFromAttr(attr);
    }

    /**
     * Override this to receive results from ResultListDialogFragments
     *
     * @param orderByStr The sort order string
     */
    public void receiveSortOrder(String orderByStr) {
    }

    /**
     * Override setArguments to also log the size of the arguments being set
     *
     * @param args Arguments to set
     */
    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        FamiliarActivity.logBundleSize("SA " + this.getClass().getName(), args);
    }
}
