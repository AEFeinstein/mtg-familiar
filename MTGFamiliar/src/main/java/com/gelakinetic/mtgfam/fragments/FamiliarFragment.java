package com.gelakinetic.mtgfam.fragments;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;

import java.util.concurrent.RejectedExecutionException;

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
    public abstract View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

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
        }
    }

    /**
     * first saving my state, so the bundle wont be empty.
     * http://code.google.com/p/android/issues/detail?id=19917
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link FamiliarActivity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     * <p/>
     * In this case, always remove the dialog, since it can contain stale references to the pre-rotated activity and
     * fragment after rotation. The one exception is the change log dialog, which would get removed by TTS checking
     * intents on the first install. It also cleans up any pending spice requests (price loading)
     */
    @Override
    public void onPause() {
        super.onPause();
        removeDialog(getFragmentManager());
        try {
            getFamiliarActivity().mSpiceManager.cancelAllRequests();
        } catch (RejectedExecutionException e) {
            /* eat it */
        }
    }

    /**
     * This will inflate the menu as usual. It also adds a SearchView to the ActionBar if the
     * Fragment does not override the search key, or a search button if it does override the key
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();

        if (getActivity() != null) {
            if (canInterceptSearchKey()) {
                menu.add(R.string.search_search)
                        .setIcon(getResourceIdFromAttr(R.attr.ic_menu_search))
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                onInterceptSearchKey();
                                return true;
                            }
                        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else {

                SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
                SearchView sv = new SearchView(getActivity());
                try {
                    sv.setSearchableInfo(searchManager.getSearchableInfo(
                            new ComponentName("com.gelakinetic.mtgfam", "com.gelakinetic.mtgfam.FamiliarActivity")));

                    MenuItem mi = menu.add(R.string.name_search_hint)
                            .setIcon(getResourceIdFromAttr(R.attr.ic_menu_search));
                    MenuItemCompat.setActionView(mi, sv);
                    MenuItemCompat.setOnActionExpandListener(mi, new MenuItemCompat.OnActionExpandListener() {
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
                    MenuItemCompat.setShowAsAction(mi, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

                } catch (Resources.NotFoundException e) {
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
    public void startNewFragment(Fragment frag, Bundle args) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        if (fm != null) {
            frag.setArguments(args);
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.fragment_container, frag, FamiliarActivity.FRAGMENT_TAG);
            ft.addToBackStack(null);
            ft.commit();
            if (getActivity() != null) {
                getFamiliarActivity().hideKeyboard();
            }
        }
    }

    /**
     * This removes any currently open dialog
     */
    void removeDialog(FragmentManager fm) {
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastWrapper.makeText(getActivity(), getString(R.string.error_database), ToastWrapper.LENGTH_LONG).show();
            }
        });
		/* Finish the fragment if requested */
        if (shouldFinish) {
			/* will be correct for nested ViewPager fragments too */
            FragmentManager fm = getActivity().getSupportFragmentManager();
            if (fm != null) {
				/* If there is only one fragment, finish the activity
				 * Otherwise pop the offending fragment */
                if (fm.getFragments().size() == 1) {
                    getActivity().finish();
                } else {
                    fm.popBackStack();
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
        return ((FamiliarActivity) getActivity()).getResourceIdFromAttr(attr);
    }
}
