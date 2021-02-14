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

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.HtmlDialogFragment;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

/**
 * This is a simple fragment which displays an HTML document as specified in the bundle of arguments passed to it.
 * Primarily it is used to display the MTR and IPG
 */
public class HtmlDocFragment extends FamiliarFragment {

    private static final String SCROLL_PCT = "scroll_pct";
    private String mName;
    private String mLastSearchTerm;
    private WebView mWebView;
    private boolean findAllCalled = false;

    /**
     * Get the document from the bundle. Spin a progress bar while it loads, then hide it when it's done. Set up
     * a button to jump to the top of the document
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The view to display
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /* Inflate the view, pull out UI elements */
        View view = inflater.inflate(R.layout.html_frag, container, false);
        assert view != null;
        mWebView = view.findViewById(R.id.webview);
        final ProgressBar progressBar = view.findViewById(R.id.progress_bar);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                /* The progress bar will spin until the web view is loaded */
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
                if (savedInstanceState != null && savedInstanceState.containsKey(SCROLL_PCT)) {
                    // Delay the scrollTo to make it work
                    view.postDelayed(() -> {
                        float webViewSize = mWebView.getContentHeight() - mWebView.getTop();
                        float positionInWV = webViewSize * savedInstanceState.getFloat(SCROLL_PCT);
                        int positionY = Math.round(mWebView.getTop() + positionInWV);
                        mWebView.scrollTo(0, positionY);
                    }, 300);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if (Objects.requireNonNull(uri.getAuthority()).toLowerCase().equals("gatherer.wizards.com")) {
                    /* Display card links internally */
                    startCardViewFrag(uri.getQueryParameter("name"));
                } else {
                    /* Otherwise launch links externally */
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    Objects.requireNonNull(getContext()).startActivity(i);
                }
                return true;
            }
        });

        /* Enable zoom */
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);

        assert getArguments() != null;
        mName = getArguments().getString(JudgesCornerFragment.PAGE_NAME);

        /* Get the document from the bundle, load it */
        File file = new File(Objects.requireNonNull(getActivity()).getFilesDir(), Objects.requireNonNull(getArguments().getString(JudgesCornerFragment.HTML_DOC)));
        StringBuilder html = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line);
            }
        } catch (IOException e) {
            html.setLength(0);
            html.append(getString(R.string.judges_corner_error));
        }
        /* eat it */
        mWebView.loadDataWithBaseURL(null, html.toString(), "text/html", "utf-8", null);

        /* Set up the button to jump to the top of the document */
        view.findViewById(R.id.mtr_ipg_jump_to_top).setOnClickListener(v -> mWebView.scrollTo(0, 0));

        return view;
    }

    /**
     * Convenience method to start a card view fragment.
     *
     * @param name The name of the card to launch
     */
    private void startCardViewFrag(String name) {
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            Bundle args = new Bundle();
            SQLiteDatabase database = DatabaseManager.openDatabase(getActivity(), false, handle);
            long[] cardIds = {CardDbAdapter.getIdFromName(name, database)};

            /* Load the array of ids and position into the bundle, start the fragment */
            args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, cardIds);
            CardViewPagerFragment cardViewPagerFragment = new CardViewPagerFragment();
            startNewFragment(cardViewPagerFragment, args);
        } catch (SQLiteException | FamiliarDbException | IllegalStateException ignored) {
            /* Eh */
        } finally {
            DatabaseManager.closeDatabase(getActivity(), handle);
        }
    }

    /**
     * Calculate the percentage of scroll progress in the actual web page content
     *
     * @return The percentage of scroll progress
     */
    private float calculateProgression() {
        if (null != mWebView) {
            float positionTopView = mWebView.getTop();
            float contentHeight = mWebView.getContentHeight();
            float currentScrollPosition = mWebView.getScrollY();
            return (currentScrollPosition - positionTopView) / contentHeight;
        }
        return 0;
    }

    /**
     * Save the scroll location before rotating or whatever
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(JudgesCornerFragment.PAGE_NAME, mName);
        try {
            outState.putFloat(SCROLL_PCT, calculateProgression());
        } catch (NullPointerException e) {
            outState.putFloat(SCROLL_PCT, 0);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * @return true, because the Html Doc Fragment overrides the search key
     */
    @Override
    boolean canInterceptSearchKey() {
        return true;
    }

    /**
     * Show the dialog to search when the search key is pressed
     *
     * @return true, because the dialog was shown
     */
    @Override
    public boolean onInterceptSearchKey() {
        showDialog();
        return true;
    }

    /**
     * Remove any showing dialogs, and show the requested one
     */
    private void showDialog() throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        /* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        /* Create and show the dialog. */
        HtmlDialogFragment newFragment = new HtmlDialogFragment();
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * @return Get the name of the web page being displayed
     */
    public String getName() {
        return mName;
    }

    /**
     * @return Get the last string that was searched on this web page
     */
    public String getLastSearchTerm() {
        return mLastSearchTerm;
    }

    /**
     * Search the displayed web page for the given term. If a search isn't in progress, start it.
     * Otherwise, find the next term
     *
     * @param searchTerm The term to search for, saved for later searches
     */
    public void doSearch(String searchTerm) {
        mLastSearchTerm = searchTerm;

        if (null != mWebView) {
            if (!findAllCalled) {
                mWebView.findAllAsync(searchTerm);
                findAllCalled = true;
                getFamiliarActivity().invalidateOptionsMenu();
            } else {
                mWebView.findNext(true);
            }
        }
    }

    /**
     * Cancel the current search for a term
     */
    public void cancelSearch() {
        findAllCalled = false;
        if (null != mWebView) {
            mWebView.clearMatches();
            getFamiliarActivity().invalidateOptionsMenu();
        }
    }

    /**
     * Create the menu and show the search controls if search is active
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (findAllCalled) {
            inflater.inflate(R.menu.htmldoc_menu, menu);
        }
    }

    /**
     * Called when a search action button is clicked, either search up, down, or cancel
     *
     * @param item The item that was tapped
     * @return true if the button was handled, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        /* Handle item selection */
        if (null != mWebView) {
            if (item.getItemId() == R.id.arrow_down) {
                mWebView.findNext(true);
                return true;
            } else if (item.getItemId() == R.id.arrow_up) {
                mWebView.findNext(false);
                return true;
            } else if (item.getItemId() == R.id.cancel) {
                mWebView.clearMatches();
                findAllCalled = false;
                getFamiliarActivity().invalidateOptionsMenu();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
