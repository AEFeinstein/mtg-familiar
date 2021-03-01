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

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.*;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment shows a simple editText, and allows you to import from it.
 */
public class ImportFragment extends FamiliarFragment {

    /* UI Elements */
    private EditText mDeckName;
    private EditText mDeckText;
    private boolean mImportStarted;
    private AsyncTask<Void, String[], DeckListImporter> mImportTask;

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

        /* Try to guess the deck name from the text field when it changes */
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
            ((Button) view).setText(R.string.import_start);
        });

        return myFragmentView;
    }

    /**
     * This function takes care of adding a card to the decklist from this fragment. It makes sure
     * that fields are not null or have bad information.
     */
    private void importDeck() {

        if (mImportStarted) {
            return;
        }
        mImportStarted = true;

        final FamiliarActivity activity = getFamiliarActivity();

        /* Don't allow the fields to be empty */
        if (getDeckNameInput() == null || getDeckNameInput().length() == 0) {
            SnackbarWrapper.makeAndShowText(getFamiliarActivity(), R.string.empty_deck_name, SnackbarWrapper.LENGTH_LONG);
            mImportStarted = false;
            return;
        }

        if (getDeckTextInput() == null || getDeckTextInput().length() == 0) {
            SnackbarWrapper.makeAndShowText(getFamiliarActivity(), R.string.empty_deck_text, SnackbarWrapper.LENGTH_LONG);
            mImportStarted = false;
            return;
        }

        /* TODO: check if deck already exists */

        activity.setLoading();

        final String name = String.valueOf(getDeckNameInput());
        final String lines = String.valueOf(getDeckTextInput());

        mImportTask = new ImportTask(name, lines).execute();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (mImportTask != null) {
            mImportTask.cancel(true);
            mImportTask = null;
        }
    }

    /**
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
    }

    /* TODO: make class static to prevent leaks? */
    private class ImportTask extends AsyncTask<Void, String[], DeckListImporter> {

        private final String mLines;
        private final String mName;
        private final ArrayList<MtgCard> unknownCards = new ArrayList<>();
        private final ArrayList<MtgCard> importedCards = new ArrayList<>();

        ImportTask(String name, String lines) {
            mName = name;
            mLines = lines;
        }

        @Override
        protected DeckListImporter doInBackground(Void... voids) {
            final DeckListImporter importer = new DeckListImporter();
            try (BufferedReader br = new BufferedReader(new StringReader(mLines))) {
                String line;
                while ((line = br.readLine()) != null) {
                    importer.parseLine(line);
                }
            } catch (IOException e) {
                /* TODO: show some kind of message here?
                 * I don't think StringReader actually throws unless it's closed... */
                return importer;
            }
            /* store local copy of cards so they can be updated */
            List<MtgCard> cardList = importer.getParsedCards();
            /* match cards with database */
            try {
                /* TODO: does error snackbar get shown properly? We're not on UI thread... */
                //Issues happen here if ImportTask is made static
                MtgCard.initCardListFromDb(getContext(), cardList);
            } catch (FamiliarDbException fde) {
                handleFamiliarDbException(false);
            }

            /* find out which cards are known */
            for (MtgCard card : cardList) {
                if (card.getMultiverseId() == 0) {
                    try {
                        MtgCard noSetCard = new MtgCard(getActivity(), card.getName(), null, card.mIsFoil, card.mNumberOf, card.isSideboard());
                        importedCards.add(noSetCard);
                    } catch (java.lang.InstantiationException e) {
                        unknownCards.add(card);
                    }
                } else {
                    importedCards.add(card);
                }
            }
            publishProgress(importer.getErrorLines().toArray(new String[0]));
            /* Save the decklist */
            if (!importedCards.isEmpty()) {
                DecklistHelpers.WriteDecklist(getFamiliarActivity(), importedCards, mName + ".deck");
            }

            return importer;
        }

        @Override
        protected void onProgressUpdate(String[]... errorLines) {
            getDeckTextInput().clear();
            if (errorLines[0].length > 0 || unknownCards.size() > 0) {
                for (String line : errorLines[0]) {
                    getDeckTextInput().append(line).append(System.getProperty("line.separator"));
                }
                for (MtgCard card : unknownCards) {
                    getDeckTextInput().append(String.valueOf(card.mNumberOf)).append(" ").append(card.getName()).append(System.getProperty("line.separator"));
                }
                SnackbarWrapper.makeAndShowText(getFamiliarActivity(),
                        getString(R.string.import_parse_error_toast),
                        SnackbarWrapper.LENGTH_LONG);
            }
        }

        @Override
        protected void onPostExecute(DeckListImporter importer) {
            getFamiliarActivity().clearLoading();
            mImportStarted = false;
            if (importedCards.size() > 0) {
                PreferenceAdapter.setLastLoadedDecklist(getContext(), mName);
            }
            int unknownCount = unknownCards.size();
            if (unknownCount > 0) {
                SnackbarWrapper.makeAndShowText(getFamiliarActivity(),
                        getResources().getQuantityString(R.plurals.import_card_unknown_toast, unknownCount, unknownCount),
                        SnackbarWrapper.LENGTH_LONG);
            } else if (importedCards.size() > 0) {
                SnackbarWrapper.makeAndShowText(getFamiliarActivity(),
                        getString(R.string.import_complete_toast),
                        SnackbarWrapper.LENGTH_LONG);
            }
        }
    }
}
