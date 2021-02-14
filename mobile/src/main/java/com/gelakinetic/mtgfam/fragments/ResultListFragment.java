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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.ResultListDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.helpers.FamiliarLogger;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.ResultListAdapter;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

/**
 * This fragment displays a list of cards. It can be from a search result, some MoJhoSto basic, or whatever
 */
public class ResultListFragment extends FamiliarFragment {

    /* Constants for bundled arguments */
    public static final String CARD_ID_0 = "id0";
    public static final String CARD_ID_1 = "id1";
    public static final String CARD_ID_2 = "id2";
    /* Saved instance state bundle keys */
    private static final String CURSOR_POSITION_OFFSET = "cur_pos";
    private static final String CURSOR_POSITION = "pos_off";
    /* Static integers preserve list position during the fragment's lifecycle */
    private int mCursorPosition;
    private int mCursorPositionOffset;
    /* The cursor with the data and the list view to display it */
    private Cursor mCursor;
    private ListView mListView;
    private SQLiteDatabase mDatabase;
    private final FamiliarDbHandle mDbHandle = new FamiliarDbHandle();

    /**
     * When the fragment is created, open the database and search for whatever.
     * This should likely be done off the UI thread, but it's usually a quick operation,
     * and the user wouldn't be doing anything else anyway
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* After a search, make sure the position is on top */
        mCursorPosition = 0;
        mCursorPositionOffset = 0;

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(CURSOR_POSITION)) {
                mCursorPosition = savedInstanceState.getInt(CURSOR_POSITION);
            }
            if (savedInstanceState.containsKey(CURSOR_POSITION_OFFSET)) {
                mCursorPositionOffset = savedInstanceState.getInt(CURSOR_POSITION_OFFSET);
            }
        }
    }

    /**
     * Save the position of the list
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        try {
            outState.putInt(CURSOR_POSITION, mListView.getFirstVisiblePosition());
            View tmp = mListView.getChildAt(0);
            outState.putInt(CURSOR_POSITION_OFFSET, (tmp == null) ? 0 : tmp.getTop());
        } catch (NullPointerException e) {
            outState.putInt(CURSOR_POSITION, 0);
            outState.putInt(CURSOR_POSITION_OFFSET, 0);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Called when the fragment is paused, save the list location
     */
    @Override
    public void onPause() {
        super.onPause();
        mCursorPosition = mListView.getFirstVisiblePosition();
        View tmp = mListView.getChildAt(0);
        mCursorPositionOffset = (tmp == null) ? 0 : tmp.getTop();
    }

    /**
     * When the fragment resumes, fill mListView with mCursor, and move the selection to its prior state, so that the
     * list doesn't appear to jump around when opening new fragments
     */
    @Override
    public void onResume() {
        super.onResume();
        fillData();
        mListView.setSelectionFromTop(mCursorPosition, mCursorPositionOffset);

//        mCursor.moveToFirst();
//        while (!mCursor.isAfterLast()) {
//            try {
//                MtgCard toAdd = new MtgCard(
//                        mCursor.getString(mCursor.getColumnIndex(CardDbAdapter.KEY_NAME)),
//                        mCursor.getString(mCursor.getColumnIndex(CardDbAdapter.KEY_SET)),
//                        false, 1, false);
//
//                WishlistHelpers.addItemToWishlist(getActivity(),
//                        new WishlistHelpers.CompressedWishlistInfo(toAdd, 0));
//
//                // Read the decklist
//                String deckFileName = "Slivers" + DecklistFragment.DECK_EXTENSION;
//                ArrayList<MtgCard> decklist =
//                        DecklistHelpers.ReadDecklist(getActivity(), deckFileName, false);
//
//                // Look through the decklist for any existing matches
//                boolean entryIncremented = false;
//                for (MtgCard deckEntry : decklist) {
//                    if (!deckEntry.isSideboard() && // not in the sideboard
//                            deckEntry.getName().equals(toAdd.getName()) &&
//                            deckEntry.getExpansion().equals(toAdd.getExpansion())) {
//                        // Increment the card already in the deck
//                        deckEntry.mNumberOf++;
//                        entryIncremented = true;
//                        break;
//                    }
//                }
//                if (!entryIncremented) {
//                    // Add a new card to the deck
//                    decklist.add(toAdd);
//                }
//
//                // Write the decklist back
//                DecklistHelpers.WriteDecklist(getActivity(), decklist, deckFileName);
//
//            } catch (java.lang.InstantiationException e) {
//                /* Eat it */
//            }
//            mCursor.moveToNext();
//        }
    }

    /**
     * When the view is created, set up the ListView. The data will be filled in onResume
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return the view if the fragment is showing, null if otherwise
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (container == null) {
            /* Something is happening when the fragment is on the back stack */
            return null;
        }

        /* Inflate the view */
        View myFragmentView = inflater.inflate(R.layout.result_list_frag, container, false);
        assert myFragmentView != null; /* Because Android Studio */
        mListView = myFragmentView.findViewById(R.id.result_list);

        /* Open up the database, search for stuff */
        try {
            mDatabase = DatabaseManager.openDatabase(getActivity(), false, mDbHandle);
            doSearch(Objects.requireNonNull(this.getArguments()), mDatabase);
        } catch (SQLiteException | FamiliarDbException e) {
            handleFamiliarDbException(true);
            return myFragmentView;
        }

        /* Sub-optimal, but KitKat is silly */
        mListView.setOnScrollListener(new ListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                switch (scrollState) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                        absListView.setFastScrollAlwaysVisible(false);
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                        absListView.setFastScrollAlwaysVisible(true);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {

            }
        });

        if (mCursor == null) {
            FamiliarLogger.appendToLogFile(new StringBuilder("Null cursor"), "onCreateView");
        } else {
            FamiliarLogger.appendToLogFile(new StringBuilder("Cursor Count: ").append(mCursor.getCount()), "onCreateView");
        }
        FragmentManager fm = Objects.requireNonNull(getFragmentManager());
        Bundle res = getFamiliarActivity().getFragmentResults();
        if (res != null) {
            if (null == mCursor || mCursor.getCount() == 1) {
                /* Jump back past the result list (it wasn't displayed because this card is a singleton)
                 * or maybe the cursor was null for no good reason */
                if (!Objects.requireNonNull(getActivity()).isTaskRoot()) {
                    getActivity().finish();
                } else if (!fm.isStateSaved()) {
                    fm.popBackStack();
                }
            }
        } else if (this.isAdded()) {
            if (mCursor == null || mCursor.getCount() == 0) {
                SnackbarWrapper.makeAndShowText(this.getActivity(), R.string.search_toast_no_results, SnackbarWrapper.LENGTH_SHORT);
                if (!Objects.requireNonNull(getActivity()).isTaskRoot()) {
                    getActivity().finish();
                } else if (!fm.isStateSaved()) {
                    fm.popBackStack();
                }
            } else if (mCursor.getCount() == 1) {
                mCursor.moveToFirst();
                long id = mCursor.getLong(mCursor.getColumnIndex(CardDbAdapter.KEY_ID));
                try {
                    startCardViewFrag(id);
                } catch (SQLiteException | FamiliarDbException e) {
                    handleFamiliarDbException(true);
                }
            } else {
                if (savedInstanceState == null) {
                    SnackbarWrapper.makeAndShowText(this.getActivity(), String.format(getResources().getQuantityString(R.plurals.search_toast_results, mCursor.getCount()),
                            mCursor.getCount()), SnackbarWrapper.LENGTH_LONG);
                }
            }
        }

        mListView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                startCardViewFrag(id);
            } catch (SQLiteException | FamiliarDbException e) {
                handleFamiliarDbException(true);
            }
        });

        mListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            String cardName = ((TextView) view.findViewById(R.id.card_name)).getText().toString();
            String cardSet = ((TextView) view.findViewById(R.id.cardset)).getText().toString();
            showDialog(ResultListDialogFragment.QUICK_ADD, cardName, cardSet);
            return true;
        });

        return myFragmentView;
    }

    /**
     * Search the database for cards and store the result in mCursor, a global variable
     *
     * @param args     A bundle which may contain card IDs. If it does not, then use
     *                 PreferenceAdapter.getSearchCriteria() to get parameters to search with
     * @param database The database to search
     * @throws FamiliarDbException If there is a database error
     */
    private void doSearch(Bundle args, SQLiteDatabase database) throws FamiliarDbException {
        long id;
        /* If "id0" exists, then it's three cards and they should be merged
         * Otherwise, do a search with the given criteria
         */
        if ((id = args.getLong(CARD_ID_0)) != 0L) {
            long id1 = args.getLong(CARD_ID_1);
            long id2 = args.getLong(CARD_ID_2);
            mCursor = CardDbAdapter.fetchCards(new long[]{id, id1, id2},
                    PreferenceAdapter.getSearchSortOrder(getContext()), database);
        } else {

            /* All the things we may want to display */
            String[] returnTypes = new String[]{CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_SET,
                    CardDbAdapter.KEY_RARITY, CardDbAdapter.KEY_MANACOST, CardDbAdapter.KEY_SUPERTYPE, CardDbAdapter.KEY_SUBTYPE,
                    CardDbAdapter.KEY_ABILITY, CardDbAdapter.KEY_POWER, CardDbAdapter.KEY_TOUGHNESS, CardDbAdapter.KEY_LOYALTY,
                    CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_CMC, CardDbAdapter.KEY_COLOR, CardDbAdapter.KEY_COLOR_IDENTITY};

            SearchCriteria criteria = PreferenceAdapter.getSearchCriteria(getContext());
            assert criteria != null; /* Because Android Studio */
            boolean consolidate = (criteria.setLogic == CardDbAdapter.MOST_RECENT_PRINTING ||
                    criteria.setLogic == CardDbAdapter.FIRST_PRINTING);

            mCursor = CardDbAdapter.Search(criteria, true, returnTypes, consolidate,
                    PreferenceAdapter.getSearchSortOrder(getContext()), database);
            if (mCursor == null) {
                FamiliarLogger.appendToLogFile(new StringBuilder("Null cursor"), "doSearch");
            } else {
                FamiliarLogger.appendToLogFile(new StringBuilder("Cursor Count: ").append(mCursor.getCount()), "doSearch");
            }
        }
    }

    /**
     * Be clean with the cursor!
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mCursor != null) {
            mCursor.close();
        }
        DatabaseManager.closeDatabase(getActivity(), mDbHandle);
    }

    /**
     * This function fills mListView with the info in mCursor using a ResultListAdapter
     */
    private void fillData() {
        if (mCursor != null) {
            ArrayList<String> fromList = new ArrayList<>();
            ArrayList<Integer> toList = new ArrayList<>();
            // Always get name, set, and rarity. This is for the wishlist quick add
            fromList.add(CardDbAdapter.KEY_NAME);
            toList.add(R.id.card_name);
            fromList.add(CardDbAdapter.KEY_SET);
            toList.add(R.id.cardset);
            fromList.add(CardDbAdapter.KEY_RARITY);
            toList.add(R.id.rarity);
            if (PreferenceAdapter.getManaCostPref(getContext())) {
                fromList.add(CardDbAdapter.KEY_MANACOST);
                toList.add(R.id.cardcost);
            }
            if (PreferenceAdapter.getTypePref(getContext())) {
                /* This will handle both sub and super type */
                fromList.add(CardDbAdapter.KEY_SUPERTYPE);
                toList.add(R.id.cardtype);
            }
            if (PreferenceAdapter.getAbilityPref(getContext())) {
                fromList.add(CardDbAdapter.KEY_ABILITY);
                toList.add(R.id.cardability);
            }
            if (PreferenceAdapter.getPTPref(getContext())) {
                fromList.add(CardDbAdapter.KEY_POWER);
                toList.add(R.id.cardp);
                fromList.add(CardDbAdapter.KEY_TOUGHNESS);
                toList.add(R.id.cardt);
                fromList.add(CardDbAdapter.KEY_LOYALTY);
                toList.add(R.id.cardt);
            }

            String[] from = new String[fromList.size()];
            fromList.toArray(from);

            int[] to = new int[toList.size()];
            for (int i = 0; i < to.length; i++) {
                to[i] = toList.get(i);
            }

            ResultListAdapter rla = new ResultListAdapter(getActivity(), mCursor, from, to);
            mListView.setAdapter(rla);
        }
    }

    /**
     * Convenience method to start a card view fragment.
     *
     * @param id The id of the card to display, or -1 for a random card
     */
    private void startCardViewFrag(long id) throws FamiliarDbException {
        try {
            // TODO don't use bundle?
            Bundle args = new Bundle();
            int cardPosition = 0;

            /* Build the array of ids sequentially, make note of the chosen card's position */
            long[] cardIds = new long[mCursor.getCount()];
            mCursor.moveToFirst();
            for (int i = 0; i < mCursor.getCount(); i++, mCursor.moveToNext()) {
                cardIds[i] = mCursor.getLong(mCursor.getColumnIndex(CardDbAdapter.KEY_ID));
                if (cardIds[i] == id) {
                    cardPosition = i;
                }
            }

            if (id == -1) {
                Random rand = new Random(System.currentTimeMillis());

                /* Shuffle the array of ids */
                /* implements http://en.wikipedia.org/wiki/Fisher-Yates_shuffle */
                long temp;
                int k, j;
                for (k = cardIds.length - 1; k > 0; k--) {
                    j = rand.nextInt(k + 1);/* j = random integer with 0 <= j <= i */
                    temp = cardIds[j];
                    cardIds[j] = cardIds[k];
                    cardIds[k] = temp;
                }

                /* Start at the beginning of the random sequence */
                cardPosition = 0;
            }

            /* Load the array of ids and position into the bundle, start the fragment */
            args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, cardPosition);
            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, cardIds);
            CardViewPagerFragment cardViewPagerFragment = new CardViewPagerFragment();
            startNewFragment(cardViewPagerFragment, args);
        } catch (IllegalStateException | NullPointerException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Create an options menu. Super will handle adding a SearchView
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.result_list_menu, menu);
    }

    /**
     * Handle an ActionBar item click
     *
     * @param item the item clicked
     * @return true if the click was acted on
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle item selection */
        if (item.getItemId() == R.id.search_menu_random_search) {
            try {
                startCardViewFrag(-1);
            } catch (SQLiteException | FamiliarDbException e) {
                handleFamiliarDbException(true);
            }
            return true;
        } else if (item.getItemId() == R.id.search_menu_sort) {
            showDialog(ResultListDialogFragment.DIALOG_SORT, null, null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Remove any showing dialogs, and show the requested one
     */
    public void showDialog(int dialogId, String cardName, String cardSet) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        /* If the fragment isn't visible (if desired being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        if (dialogId == ResultListDialogFragment.DIALOG_SORT) {
            SortOrderDialogFragment newFragment = new SortOrderDialogFragment();
            Bundle args = new Bundle();
            args.putString(SortOrderDialogFragment.SAVED_SORT_ORDER,
                    PreferenceAdapter.getSearchSortOrder(getContext()));
            newFragment.setArguments(args);
            newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
        } else {
            ResultListDialogFragment newFragment = new ResultListDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt(FamiliarDialogFragment.ID_KEY, dialogId);
            arguments.putString(ResultListDialogFragment.NAME_KEY, cardName);
            arguments.putString(ResultListDialogFragment.NAME_SET, cardSet);
            newFragment.setArguments(arguments);
            newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
        }
    }

    /**
     * Called when the sort dialog closes. Sort the cards according to the new options.
     *
     * @param orderByStr The sort order string
     */
    public void receiveSortOrder(String orderByStr) {

        PreferenceAdapter.setSearchSortOrder(getContext(), orderByStr);

        try {
            /* Close the old cursor */
            mCursor.close();
            /* Do the search again with the new "order by" options */
            doSearch(Objects.requireNonNull(getArguments()), mDatabase);
            /* Display the newly sorted data */
            fillData();
        } catch (SQLiteException | FamiliarDbException e) {
            handleFamiliarDbException(true);
        }
    }
}