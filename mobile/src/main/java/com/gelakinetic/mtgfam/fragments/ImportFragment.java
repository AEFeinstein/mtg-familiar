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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.DecklistDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.helpers.CardDataAdapter;
import com.gelakinetic.mtgfam.helpers.CardDataViewHolder;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.DeckListImporter;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

import static com.gelakinetic.mtgfam.FamiliarActivity.FRAGMENT_TAG;

/**
 * This fragment shows a simple editText, and allows you to import from it.
 */
public class ImportFragment extends FamiliarFragment {

    /* UI Elements */
    private EditText mDeckName;
    private EditText mDeckText;

    /**
     * @return The current text in mDeckName
     */
    Editable getDeckNameInput() {
        return mDeckName.getText();
    }

    /**
     * @return The current text in mDeckText
     */
    Editable getDeckTextInput() {
        return mDeckText.getText();
    }


    /**
     * Create the view, pull out UI elements, and set up the listener for the "add cards" button.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in
     *                           the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should
     *                           be attached to. The fragment should not add the view itself, but
     *                           this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return The view to be displayed.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final View myFragmentView =
                inflater.inflate(R.layout.import_frag, container, false);
        assert myFragmentView != null;

        mDeckName = myFragmentView.findViewById(R.id.importName);
        mDeckText = myFragmentView.findViewById(R.id.import_editText);

        mDeckText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int countBefore, int countAfter) {
                /* the `100` below is a bit arbitrary,
                   we know that the name will be somewhere at the start of the file */
                if (mDeckName.length() == 0 && start < 100) {
                    mDeckName.setText(DeckListImporter.tryGuessDeckName(s.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int countBefore, int countAfter) {
            }
        });

        Button startButton = myFragmentView.findViewById(R.id.import_start);
        startButton.setOnClickListener(view -> {
            ((Button) view).setText(R.string.import_loading);
            importDeck();
        });

        return myFragmentView;
    }

    /**
     * This function takes care of adding a card to the decklist from this fragment. It makes sure
     * that fields are not null or have bad information.
     */
    private void importDeck() {

        /* Don't allow the fields to be empty */
        if (getDeckNameInput() == null || getDeckNameInput().length() == 0 ||
                getDeckTextInput() == null || getDeckTextInput().length() == 0) {
            return;
        }

        final String name = String.valueOf(getDeckNameInput());
        final String lines = String.valueOf(getDeckTextInput());

        final DeckListImporter importer = new DeckListImporter();
        try (BufferedReader br = new BufferedReader(new StringReader(lines))) {
            String line;
            while ((line = br.readLine()) != null) {
                importer.parseLine(line);
            }
        } catch (IOException e) {
            /* TODO: show some kind of message here?
             * I don't think StringReader actually throws unless it's closed... */
            return;
        }

        /* TODO: handle errors in parsing */
        /* TODO: match cards with database */
        /* TODO: handle errors in card matching */

        /*
        ArrayList<String> nonFoilSets;
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            SQLiteDatabase database = DatabaseManager.openDatabase(getContext(), false, handle);
            nonFoilSets = CardDbAdapter.getNonFoilSets(database);
        } catch (SQLiteException | FamiliarDbException | IllegalStateException ignored) {
            nonFoilSets = new ArrayList<>();
        } finally {
            DatabaseManager.closeDatabase(getContext(), handle);
        }
        */

        ArrayList<CompressedDecklistInfo> compressedDecklist = null;

        /* Save the decklist */
        DecklistHelpers.WriteCompressedDecklist(getActivity(), compressedDecklist, getDeckNameInput().toString());
    }

    /**
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.decklist_menu, menu);

    }
}
