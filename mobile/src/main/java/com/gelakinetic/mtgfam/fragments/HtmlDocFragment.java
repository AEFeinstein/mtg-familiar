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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.gelakinetic.mtgfam.R;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /* Inflate the view, pull out UI elements */
        View view = inflater.inflate(R.layout.html_frag, container, false);
        assert view != null;
        final WebView webView = view.findViewById(R.id.webview);
        final ProgressBar progressBar = view.findViewById(R.id.progress_bar);

        /* The progress bar will spin until the web view is loaded */
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });

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
        view.findViewById(R.id.mtr_ipg_jump_to_top).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                webView.scrollTo(0, 0);
            }
        });

        return view;
    }
}
