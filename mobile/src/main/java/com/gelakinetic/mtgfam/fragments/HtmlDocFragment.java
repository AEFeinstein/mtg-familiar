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
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This is a simple fragment which displays an HTML document as specified in the bundle of arguments passed to it.
 * Primarily it is used to display the MTR and IPG
 */
public class HtmlDocFragment extends FamiliarFragment {

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
        final WebView webView = view.findViewById(R.id.webview);
        final ProgressBar progressBar = view.findViewById(R.id.progress_bar);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                /* The progress bar will spin until the web view is loaded */
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if (uri.getAuthority().toLowerCase().equals("gatherer.wizards.com")) {
                    /* Display card links internally */
                    startCardViewFrag(uri.getQueryParameter("name"));
                } else {
                    /* Otherwise launch links externally */
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    getContext().startActivity(i);
                }
                return true;
            }
        });

        /* Enable zoom */
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        /* Get the document from the bundle, load it */
        File file = new File(getActivity().getFilesDir(), getArguments().getString(JudgesCornerFragment.HTML_DOC));
        StringBuilder html = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line);
            }
        } catch (IOException e) {
            html.setLength(0);
            html.append(getString(R.string.judges_corner_error));
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                /* eat it */
            }
        }
        webView.loadDataWithBaseURL(null, html.toString(), "text/html", "utf-8", null);

        /* Set up the button to jump to the top of the document */
        view.findViewById(R.id.mtr_ipg_jump_to_top).setOnClickListener(v -> webView.scrollTo(0, 0));

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
            long cardIds[] = {CardDbAdapter.getIdFromName(name, database)};

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
}
