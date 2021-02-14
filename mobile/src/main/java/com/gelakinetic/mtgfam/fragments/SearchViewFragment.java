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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SearchViewDialogFragment;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.gelakinetic.mtgfam.helpers.model.Comparison;
import com.gelakinetic.mtgfam.helpers.view.ATokenTextView;
import com.gelakinetic.mtgfam.helpers.view.ComparisonSpinner;
import com.gelakinetic.mtgfam.helpers.view.CompletionView;
import com.gelakinetic.mtgfam.helpers.view.ManaCostTextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This fragment lets users configure search parameters, and then search for a card
 * The actual database query is handled in the ResultListFragment
 */
public class SearchViewFragment extends FamiliarFragment {

    /* String keys */
    public static final String CRITERIA_FLAG = "criteria_flag";

    /* Default search file */
    private static final String DEFAULT_CRITERIA_FILE = "defaultSearchCriteria.ser";

    /* Keys for persisting state */
    private static final String SAVED_SET_KEY = "SAVED_SET_KEY";
    private static final String SAVED_RARITY_KEY = "SAVED_RARITY_KEY";
    private static final String SAVED_FORMAT_KEY = "SAVED_FORMAT_KEY";


    /* Spinner Data Structures */
    private String[] mSetNames;
    private int[] mSetCheckedIndices;
    private String[] mSetSymbols;
    public String[] mFormatNames;
    private char[] mRarityCodes;
    public String[] mRarityNames;
    public int[] mRarityCheckedIndices;
    public int mSelectedFormat;

    /* Autocomplete data structures */
    private String[] mSupertypes = null;
    private String[] mSubtypes = null;
    private String[] mArtists = null;
    private String[] mWatermarks = null;
    private String[] mSetTypes = null;

    /* UI Elements */
    private AutoCompleteTextView mNameField;
    private EditText mTextField;
    private CompletionView mSupertypeField = null;
    private CompletionView mSubtypeField = null;
    private EditText mCollectorsNumberField;
    private CheckBox mCheckboxW;
    private CheckBox mCheckboxU;
    private CheckBox mCheckboxB;
    private CheckBox mCheckboxR;
    private CheckBox mCheckboxG;
    private CheckBox mCheckboxL;
    private Spinner mColorSpinner;
    private CheckBox mIsCommander;
    private CheckBox mCheckboxWIdentity;
    private CheckBox mCheckboxUIdentity;
    private CheckBox mCheckboxBIdentity;
    private CheckBox mCheckboxRIdentity;
    private CheckBox mCheckboxGIdentity;
    private CheckBox mCheckboxLIdentity;
    private Spinner mColorIdentitySpinner;
    private CompletionView mSetField;
    private CompletionView mSetTypeField = null;
    private Button mFormatButton;
    private Button mRarityButton;
    private Spinner mPowLogic;
    private Spinner mPowChoice;
    private Spinner mTouLogic;
    private Spinner mTouChoice;
    private Spinner mCmcLogic;
    private Spinner mCmcChoice;
    private ComparisonSpinner mManaComparisonSpinner;
    private ManaCostTextView mManaCostTextView;

    public Dialog mFormatDialog;
    public Dialog mRarityDialog;
    private EditText mFlavorField;
    private AutoCompleteTextView mArtistField = null;
    private AutoCompleteTextView mWatermarkField = null;
    private Spinner mTextSpinner;
    private Spinner mTypeSpinner;
    private Spinner mSetSpinner;

    /**
     * This will query the database to populate the set and format spinner dialogs.
     * The rarity dialog is pulled from resources
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Get the different rarities out of resources to populate the list of choices with */
        Resources res = getResources();
        TypedArray mRarityNamesTemp = res.obtainTypedArray(R.array.rarities);
        int i = mRarityNamesTemp.length();
        mRarityNames = new String[i];
        mRarityCodes = new char[i];

        if (savedInstanceState != null) {
            mSelectedFormat = savedInstanceState.getInt(SAVED_FORMAT_KEY);
            mRarityCheckedIndices = savedInstanceState.getIntArray(SAVED_RARITY_KEY);
            mSetCheckedIndices = savedInstanceState.getIntArray(SAVED_SET_KEY);
        } else {
            mRarityCheckedIndices = new int[0];
            mSelectedFormat = -1;
        }

        while (i-- > 0) {
            int resID = mRarityNamesTemp.peekValue(i).resourceId;
            String resEntryName = res.getResourceEntryName(resID);
            int p = resEntryName.lastIndexOf("_");
            if (-1 != p && p + 1 < resEntryName.length())
                mRarityCodes[i] = resEntryName.charAt(p + 1);
            else mRarityCodes[i] = ' ';
            mRarityNames[i] = res.getString(resID);
        }
        mRarityNamesTemp.recycle();
    }

    /**
     * Find all the UI elements. set actions for buttons. Attach array adapters for autocomplete
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return the inflated view
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /* Inflate the view */
        View myFragmentView = inflater.inflate(R.layout.search_frag, container, false);
        assert myFragmentView != null;

        /* Get references to UI elements. When a search is preformed, these values will be queried */
        mNameField = myFragmentView.findViewById(R.id.name_search);
        mTextField = myFragmentView.findViewById(R.id.textsearch);
        mSupertypeField = myFragmentView.findViewById(R.id.supertypesearch);
        mSubtypeField = myFragmentView.findViewById(R.id.subtypesearch);
        mFlavorField = myFragmentView.findViewById(R.id.flavorsearch);
        mArtistField = myFragmentView.findViewById(R.id.artistsearch);
        mWatermarkField = myFragmentView.findViewById(R.id.watermarksearch);
        mCollectorsNumberField = myFragmentView.findViewById(R.id.collectorsnumbersearch);

        Button searchButton = myFragmentView.findViewById(R.id.searchbutton);

        mCheckboxW = myFragmentView.findViewById(R.id.checkBoxW);
        mCheckboxU = myFragmentView.findViewById(R.id.checkBoxU);
        mCheckboxB = myFragmentView.findViewById(R.id.checkBoxB);
        mCheckboxR = myFragmentView.findViewById(R.id.checkBoxR);
        mCheckboxG = myFragmentView.findViewById(R.id.checkBoxG);
        mCheckboxL = myFragmentView.findViewById(R.id.checkBoxL);

        mCheckboxWIdentity = myFragmentView.findViewById(R.id.checkBoxW_identity);
        mCheckboxUIdentity = myFragmentView.findViewById(R.id.checkBoxU_identity);
        mCheckboxBIdentity = myFragmentView.findViewById(R.id.checkBoxB_identity);
        mCheckboxRIdentity = myFragmentView.findViewById(R.id.checkBoxR_identity);
        mCheckboxGIdentity = myFragmentView.findViewById(R.id.checkBoxG_identity);
        mCheckboxLIdentity = myFragmentView.findViewById(R.id.checkBoxL_identity);
        mIsCommander = myFragmentView.findViewById(R.id.isCommander);

        mColorSpinner = myFragmentView.findViewById(R.id.colorlogic);
        mColorIdentitySpinner = myFragmentView.findViewById(R.id.coloridentitylogic);
        mTextSpinner = myFragmentView.findViewById(R.id.textlogic);
        mTypeSpinner = myFragmentView.findViewById(R.id.typelogic);
        mSetSpinner = myFragmentView.findViewById(R.id.setlogic);

        mSetField = myFragmentView.findViewById(R.id.setsearch);
        mSetTypeField = myFragmentView.findViewById(R.id.settypesearch);
        mFormatButton = myFragmentView.findViewById(R.id.formatsearch);
        mRarityButton = myFragmentView.findViewById(R.id.raritysearch);

        mPowLogic = myFragmentView.findViewById(R.id.powLogic);
        mPowChoice = myFragmentView.findViewById(R.id.powChoice);
        mTouLogic = myFragmentView.findViewById(R.id.touLogic);
        mTouChoice = myFragmentView.findViewById(R.id.touChoice);
        mCmcLogic = myFragmentView.findViewById(R.id.cmcLogic);
        mCmcChoice = myFragmentView.findViewById(R.id.cmcChoice);
        mManaCostTextView = myFragmentView.findViewById(R.id.manaCostTextView);
        mManaComparisonSpinner = myFragmentView.findViewById(R.id.comparisonSpinner);

        /* set the buttons to open the dialogs */
        mFormatButton.setOnClickListener(v -> showDialog(SearchViewDialogFragment.FORMAT_LIST));
        mRarityButton.setOnClickListener(v -> showDialog(SearchViewDialogFragment.RARITY_LIST));

        /* This is a better default, might want to reorder the array */
        safeSetSelection(mColorSpinner, 2);

        /* The button colors change whether an option is selected or not */
        checkDialogButtonColors();

        /* This listener will do searches directly from the TextViews. Attach it to everything! */
        TextView.OnEditorActionListener doSearchListener = (arg0, arg1, arg2) -> {
            if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        };
        mNameField.setOnEditorActionListener(doSearchListener);
        mTextField.setOnEditorActionListener(doSearchListener);
        mSupertypeField.setOnEditorActionListener(doSearchListener);
        mSubtypeField.setOnEditorActionListener(doSearchListener);
        mSetField.setOnEditorActionListener(doSearchListener);
        mSetTypeField.setOnEditorActionListener(doSearchListener);
        mManaCostTextView.setOnEditorActionListener(doSearchListener);
        mFlavorField.setOnEditorActionListener(doSearchListener);
        mArtistField.setOnEditorActionListener(doSearchListener);
        mWatermarkField.setOnEditorActionListener(doSearchListener);
        mCollectorsNumberField.setOnEditorActionListener(doSearchListener);

        /* set the autocomplete for card names */
        mNameField.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME}, new int[]{R.id.text1}, mNameField, true));
        mNameField.setOnItemClickListener((adapterView, view, i, l) -> {
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.name = ((TextView) view.findViewById(R.id.text1)).getText().toString();
            Bundle args = new Bundle();
            args.putBoolean(CRITERIA_FLAG, true);
            PreferenceAdapter.setSearchCriteria(getContext(), searchCriteria);
            ResultListFragment rlFrag = new ResultListFragment();
            startNewFragment(rlFrag, args);
        });

        /* Disable system level autocomplete for fields which do it already */
        mNameField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mArtistField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mWatermarkField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mSupertypeField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mSubtypeField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mSetField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mSetTypeField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mManaCostTextView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        /* Get a bunch of database info in a background task */
        new BuildAutocompleteTask().execute(this);

        /* set the search button! */
        searchButton.setOnClickListener(v -> doSearch());

        myFragmentView.findViewById(R.id.camera_button).setVisibility(View.GONE);
        return myFragmentView;
    }

    /**
     * Safely set a spinner's selection by defaulting to if out of bounds
     *
     * @param spinner   The spinner to set a selection
     * @param selection The index to set
     */
    private void safeSetSelection(Spinner spinner, int selection) {
        int spinnerCount = spinner.getAdapter().getCount();
        // Make sure the spinner has items
        if (0 != spinnerCount) {
            if (selection >= spinnerCount) {
                // The selection would be out of bounds, just select 0
                spinner.setSelection(0);
            } else {
                // All good, make the selection
                spinner.setSelection(selection);
            }
        }
    }

    private static class BuildAutocompleteTask extends AsyncTask<SearchViewFragment, Void, SearchViewFragment> {
        @Override
        protected SearchViewFragment doInBackground(SearchViewFragment... frags) {

            SearchViewFragment frag = frags[0];
            /* Only actually get data if the arrays are null */
            Cursor formatCursor = null;
            Cursor setCursor = null;
            FamiliarDbHandle handle = new FamiliarDbHandle();
            try {
                SQLiteDatabase database = DatabaseManager.openDatabase(frag.getActivity(), false, handle);
                if (frag.mSetNames == null) {
                    /* Query the database for all sets and fill the arrays to populate the list of choices with */
                    setCursor = CardDbAdapter.fetchAllSets(database);
                    setCursor.moveToFirst();

                    frag.mSetNames = new String[setCursor.getCount()];
                    frag.mSetSymbols = new String[setCursor.getCount()];

                    /* If this wasn't persisted, create it new */
                    if (frag.mSetCheckedIndices == null) {
                        frag.mSetCheckedIndices = new int[0];
                    }

                    for (int i = 0; i < setCursor.getCount(); i++) {
                        frag.mSetSymbols[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_CODE));
                        frag.mSetNames[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
                        setCursor.moveToNext();
                    }
                }

                if (frag.mFormatNames == null) {
                    /* Query the database for all formats and fill the arrays to populate the list of choices with */
                    formatCursor = CardDbAdapter.fetchAllFormats(database);
                    formatCursor.moveToFirst();

                    frag.mFormatNames = new String[formatCursor.getCount()];

                    for (int i = 0; i < formatCursor.getCount(); i++) {
                        frag.mFormatNames[i] = formatCursor.getString(formatCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
                        formatCursor.moveToNext();
                    }
                }

                if (frag.mSupertypes == null) {
                    String[] supertypes = CardDbAdapter.getUniqueColumnArray(CardDbAdapter.DATABASE_TABLE_CARDS, CardDbAdapter.KEY_SUPERTYPE, true, database);
                    frag.mSupertypes = tokenStringsFromTypes(supertypes);
                }

                if (frag.mSubtypes == null) {
                    String[] subtypes = CardDbAdapter.getUniqueColumnArray(CardDbAdapter.DATABASE_TABLE_CARDS, CardDbAdapter.KEY_SUBTYPE, true, database);
                    frag.mSubtypes = tokenStringsFromTypes(subtypes);
                }

                if (frag.mArtists == null) {
                    frag.mArtists = CardDbAdapter.getUniqueColumnArray(CardDbAdapter.DATABASE_TABLE_CARDS, CardDbAdapter.KEY_ARTIST, false, database);
                }

                if (frag.mWatermarks == null) {
                    frag.mWatermarks = CardDbAdapter.getUniqueColumnArray(CardDbAdapter.DATABASE_TABLE_CARDS, CardDbAdapter.KEY_WATERMARK, false, database);
                }

                if (frag.mSetTypes == null) {
                    frag.mSetTypes = CardDbAdapter.getUniqueColumnArray(CardDbAdapter.DATABASE_TABLE_SETS, CardDbAdapter.KEY_SET_TYPE, false, database);
                }
            } catch (SQLiteException | FamiliarDbException e) {
                frag.handleFamiliarDbException(false);
            } finally {
                if (null != setCursor) {
                    setCursor.close();
                }
                if (null != formatCursor) {
                    formatCursor.close();
                }
                DatabaseManager.closeDatabase(frag.getActivity(), handle);
            }

            return frag;
        }

        private String[] tokenStringsFromTypes(String[] types) {
            String[] tokenStrings = new String[types.length * 2];
            System.arraycopy(types, 0, tokenStrings, 0, types.length);
            for (int i = 0; i < types.length; i++) {
                tokenStrings[types.length + i] = CardDbAdapter.EXCLUDE_TOKEN + types[i];
            }
            return tokenStrings;
        }

        @Override
        protected void onPostExecute(SearchViewFragment frag) {
            super.onPostExecute(frag);
            Activity activity = frag.getActivity();
            if (null == activity) {
                return;
            }
            try {
                /* set the autocomplete for supertypes */
                if (null != frag.mSupertypes) {
                    ArrayAdapter<String> supertypeAdapter = new ArrayAdapter<>(
                            frag.getActivity(), R.layout.list_item_1, frag.mSupertypes);
                    frag.mSupertypeField.setAdapter(supertypeAdapter);
                }

                if (null != frag.mSubtypes) {
                    /* set the autocomplete for subtypes */
                    ArrayAdapter<String> subtypeAdapter = new ArrayAdapter<>(
                            frag.getActivity(), R.layout.list_item_1, frag.mSubtypes);
                    frag.mSubtypeField.setAdapter(subtypeAdapter);
                }

                /* set the autocomplete for sets */
                final SetAdapter setAdapter = new SetAdapter(frag);
                frag.mSetField.setAdapter(setAdapter);

                if (null != frag.mSetTypes) {
                    /* set the autocomplete for set types */
                    ArrayAdapter<String> setTypeAdapter = new ArrayAdapter<>(
                            frag.getActivity(), R.layout.list_item_1, frag.mSetTypes);
                    frag.mSetTypeField.setAdapter(setTypeAdapter);
                }

                if (null != frag.mArtists) {
                    /* set the autocomplete for artists */
                    ArrayAdapter<String> artistAdapter = new ArrayAdapter<>(
                            frag.getActivity(), R.layout.list_item_1, frag.mArtists);
                    frag.mArtistField.setThreshold(1);
                    frag.mArtistField.setAdapter(artistAdapter);
                }

                if (null != frag.mWatermarks) {
                    /* set the autocomplete for watermarks */
                    ArrayAdapter<String> watermarkAdapter = new ArrayAdapter<>(
                            frag.getActivity(), R.layout.list_item_1, frag.mWatermarks);
                    frag.mWatermarkField.setThreshold(1);
                    frag.mWatermarkField.setAdapter(watermarkAdapter);
                }
            } catch (NullPointerException e) {
                /* If the UI thread isn't here, eat it */
            }
        }
    }

    private static class SetAdapter extends ArrayAdapter<String> {
        final Map<String, String> symbolsByAutocomplete = new LinkedHashMap<>();

        SetAdapter(SearchViewFragment frag) {
            super(Objects.requireNonNull(frag.getActivity()), R.layout.list_item_1);
            for (int index = 0; index < frag.mSetSymbols.length; index++) {
                String autocomplete = "[" + frag.mSetSymbols[index] + "] " + frag.mSetNames[index];
                String set = frag.mSetSymbols[index];
                symbolsByAutocomplete.put(autocomplete, set);
                this.add(autocomplete);
            }
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ((TextView) view.findViewById(R.id.text1)).setText(super.getItem(position));
            return view;
        }

        @Nullable
        @Override
        public String getItem(int position) {
            String key = super.getItem(position);
            return symbolsByAutocomplete.get(key);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save the search criteria
        if (PreferenceAdapter.getPersistSearchOptions(getContext())) {
            try {
                PreferenceAdapter.setSearchViewCriteria(getContext(), parseForm());
                clear();
            } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
                /* Eat it */
            }
        }
    }

    /**
     * Generic onResume. Catches when consolidation is changed in preferences
     */
    @Override
    public void onResume() {
        super.onResume();

        /* Do we want to consolidate different printings of the same card in results, or not? */
        boolean consolidate = PreferenceAdapter.getConsolidateSearch(getContext());
        safeSetSelection(mSetSpinner, consolidate ? CardDbAdapter.MOST_RECENT_PRINTING : CardDbAdapter.ALL_PRINTINGS);

        // Load the saved criteria
        if (PreferenceAdapter.getPersistSearchOptions(getContext())) {
            setFieldsFromCriteria(PreferenceAdapter.getSearchViewCriteria(getContext()));
        }
    }

    /**
     * Save the state of the dialog selections
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(SAVED_FORMAT_KEY, mSelectedFormat);
        outState.putIntArray(SAVED_RARITY_KEY, mRarityCheckedIndices);
        outState.putIntArray(SAVED_SET_KEY, mSetCheckedIndices);
        super.onSaveInstanceState(outState);
    }

    /**
     * This function creates a results fragment, sends it the search criteria, and starts it
     */
    private void doSearch() {
        Bundle args = new Bundle();
        args.putBoolean(CRITERIA_FLAG, true);
        try {
            PreferenceAdapter.setSearchCriteria(getContext(), parseForm());
            ResultListFragment rlFrag = new ResultListFragment();
            startNewFragment(rlFrag, args);
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            SnackbarWrapper.makeAndShowText(getActivity(), R.string.judges_corner_error, SnackbarWrapper.LENGTH_SHORT);
        }
    }

    /**
     * This function combs through all the UI elements and returns a SearchCriteria with the current search options
     *
     * @return a SearchCriteria with what the user wants to search for
     */
    private SearchCriteria parseForm() throws ArrayIndexOutOfBoundsException, NullPointerException {
        SearchCriteria searchCriteria = new SearchCriteria();

        /* Because Android Studio whines */
        assert mNameField.getText() != null;
        assert mTextField.getText() != null;
        assert mSupertypeField.getText() != null;
        assert mSubtypeField.getText() != null;
        assert mFlavorField.getText() != null;
        assert mArtistField.getText() != null;
        assert mWatermarkField.getText() != null;
        assert mSetField.getText() != null;
        assert mSetTypeField.getText() != null;
        assert mCollectorsNumberField.getText() != null;
        assert mManaCostTextView.getText() != null;

        /* Read EditTexts */
        searchCriteria.name = mNameField.getText().toString().trim();
        searchCriteria.text = mTextField.getText().toString().trim();

        searchCriteria.superTypes = mSupertypeField.getObjects();
        searchCriteria.subTypes = mSubtypeField.getObjects();
        searchCriteria.flavor = mFlavorField.getText().toString().trim();
        searchCriteria.artist = mArtistField.getText().toString().trim();
        searchCriteria.watermark = mWatermarkField.getText().toString().trim();
        searchCriteria.collectorsNumber = mCollectorsNumberField.getText().toString().trim();
        searchCriteria.sets = mSetField.getObjects();
        searchCriteria.setTypes = mSetTypeField.getObjects();
        searchCriteria.manaCost = mManaCostTextView.getObjects();
        searchCriteria.manaCostLogic = (Comparison) mManaComparisonSpinner.getSelectedItem();

        if (searchCriteria.name.length() == 0) {
            searchCriteria.name = null;
        }
        if (searchCriteria.text.length() == 0) {
            searchCriteria.text = null;
        }
        if (searchCriteria.superTypes.size() == 0) {
            searchCriteria.superTypes = null;
        }
        if (searchCriteria.subTypes.size() == 0) {
            searchCriteria.subTypes = null;
        }
        if (searchCriteria.flavor.length() == 0) {
            searchCriteria.flavor = null;
        }
        if (searchCriteria.artist.length() == 0) {
            searchCriteria.artist = null;
        }
        if (searchCriteria.collectorsNumber.length() == 0) {
            searchCriteria.collectorsNumber = null;
        }
        if (searchCriteria.manaCost.size() == 0) {
            searchCriteria.manaCost = null;
        }
        if (searchCriteria.sets.size() == 0) {
            searchCriteria.sets = null;
        }
        if (searchCriteria.watermark.length() == 0) {
            searchCriteria.watermark = null;
        }

        /* Build a color string. capital letters means the user is search for that color */
        searchCriteria.color = null;

        if (mCheckboxW.isChecked()) {
            searchCriteria.color = "W";
        } else {
            searchCriteria.color = "w";
        }

        if (mCheckboxU.isChecked()) {
            searchCriteria.color += "U";
        } else {
            searchCriteria.color += "u";
        }
        if (mCheckboxB.isChecked()) {
            searchCriteria.color += "B";
        } else {
            searchCriteria.color += "b";
        }
        if (mCheckboxR.isChecked()) {
            searchCriteria.color += "R";
        } else {
            searchCriteria.color += "r";
        }
        if (mCheckboxG.isChecked()) {
            searchCriteria.color += "G";
        } else {
            searchCriteria.color += "g";
        }
        if (mCheckboxL.isChecked()) {
            searchCriteria.color += "L";
        } else {
            searchCriteria.color += "l";
        }
        searchCriteria.colorLogic = mColorSpinner.getSelectedItemPosition();

        searchCriteria.isCommander = mIsCommander.isChecked();

        /* Build a color identity string */
        searchCriteria.colorIdentity = "";

        if (mCheckboxWIdentity.isChecked()) {
            searchCriteria.colorIdentity += "W";
        } else {
            searchCriteria.colorIdentity += "w";
        }
        if (mCheckboxUIdentity.isChecked()) {
            searchCriteria.colorIdentity += "U";
        } else {
            searchCriteria.colorIdentity += "u";
        }
        if (mCheckboxBIdentity.isChecked()) {
            searchCriteria.colorIdentity += "B";
        } else {
            searchCriteria.colorIdentity += "b";
        }
        if (mCheckboxRIdentity.isChecked()) {
            searchCriteria.colorIdentity += "R";
        } else {
            searchCriteria.colorIdentity += "r";
        }
        if (mCheckboxGIdentity.isChecked()) {
            searchCriteria.colorIdentity += "G";
        } else {
            searchCriteria.colorIdentity += "g";
        }
        if (mCheckboxLIdentity.isChecked()) {
            searchCriteria.colorIdentity += "L";
        } else {
            searchCriteria.colorIdentity += "l";
        }
        searchCriteria.colorIdentityLogic = mColorIdentitySpinner.getSelectedItemPosition();

        searchCriteria.format = null;
        if (mSelectedFormat != -1 && mFormatNames != null) {
            searchCriteria.format = mFormatNames[mSelectedFormat];
        }

        StringBuilder rarityBuilder = new StringBuilder();
        for (int index : mRarityCheckedIndices) {
            rarityBuilder.append(mRarityCodes[index]);
        }
        if (rarityBuilder.length() > 0) {
            searchCriteria.rarity = rarityBuilder.toString();
        } else {
            searchCriteria.rarity = null;
        }

        String[] logicChoices = getResources().getStringArray(R.array.logic_spinner);
        String power = getResources().getStringArray(R.array.pt_spinner)[mPowChoice.getSelectedItemPosition()];
        String toughness = getResources().getStringArray(R.array.pt_spinner)[mTouChoice.getSelectedItemPosition()];

        float pow = CardDbAdapter.NO_ONE_CARES;
        try {
            pow = Float.parseFloat(power);
        } catch (NumberFormatException e) {
            switch (power) {
                case "*":
                    pow = CardDbAdapter.STAR;
                    break;
                case "1+*":
                    pow = CardDbAdapter.ONE_PLUS_STAR;
                    break;
                case "2+*":
                    pow = CardDbAdapter.TWO_PLUS_STAR;
                    break;
                case "7-*":
                    pow = CardDbAdapter.SEVEN_MINUS_STAR;
                    break;
                case "*^2":
                    pow = CardDbAdapter.STAR_SQUARED;
                    break;
                case "X":
                    pow = CardDbAdapter.X;
                    break;
                case "?":
                    pow = CardDbAdapter.QUESTION_MARK;
                    break;
                case "∞":
                    pow = CardDbAdapter.INFINITY;
                    break;
            }
        }
        searchCriteria.powChoice = pow;
        searchCriteria.powLogic = logicChoices[mPowLogic.getSelectedItemPosition()];

        float tou = CardDbAdapter.NO_ONE_CARES;
        try {
            tou = Float.parseFloat(toughness);
        } catch (NumberFormatException e) {
            switch (toughness) {
                case "*":
                    tou = CardDbAdapter.STAR;
                    break;
                case "1+*":
                    tou = CardDbAdapter.ONE_PLUS_STAR;
                    break;
                case "2+*":
                    tou = CardDbAdapter.TWO_PLUS_STAR;
                    break;
                case "7-*":
                    tou = CardDbAdapter.SEVEN_MINUS_STAR;
                    break;
                case "*^2":
                    tou = CardDbAdapter.STAR_SQUARED;
                    break;
                case "X":
                    tou = CardDbAdapter.X;
                    break;
                case "?":
                    tou = CardDbAdapter.QUESTION_MARK;
                    break;
                case "∞":
                    tou = CardDbAdapter.INFINITY;
                    break;
            }
        }
        searchCriteria.touChoice = tou;
        searchCriteria.touLogic = logicChoices[mTouLogic.getSelectedItemPosition()];

        String[] cmcChoices = getResources().getStringArray(R.array.cmc_spinner);
        int cmc;
        try {
            cmc = Integer.parseInt(cmcChoices[mCmcChoice.getSelectedItemPosition()]);
        } catch (NumberFormatException e) {
            cmc = -1;
        }
        searchCriteria.cmc = cmc;
        searchCriteria.cmcLogic = logicChoices[mCmcLogic.getSelectedItemPosition()];

        searchCriteria.typeLogic = mTypeSpinner.getSelectedItemPosition();
        searchCriteria.textLogic = mTextSpinner.getSelectedItemPosition();
        searchCriteria.setLogic = mSetSpinner.getSelectedItemPosition();

        return searchCriteria;
    }

    /**
     * This function clears all the search options, it's called from the ActionBar
     */
    private void clear() {
        mNameField.setText("");
        clearTextAndTokens(mSupertypeField);
        clearTextAndTokens(mSubtypeField);
        mTextField.setText("");
        mArtistField.setText("");
        mWatermarkField.setText("");
        mFlavorField.setText("");
        mCollectorsNumberField.setText("");
        clearTextAndTokens(mSetField);
        clearTextAndTokens(mSetTypeField);

        mCheckboxW.setChecked(false);
        mCheckboxU.setChecked(false);
        mCheckboxB.setChecked(false);
        mCheckboxR.setChecked(false);
        mCheckboxG.setChecked(false);
        mCheckboxL.setChecked(false);
        safeSetSelection(mColorSpinner, 2);

        mIsCommander.setChecked(false);
        mCheckboxWIdentity.setChecked(false);
        mCheckboxUIdentity.setChecked(false);
        mCheckboxBIdentity.setChecked(false);
        mCheckboxRIdentity.setChecked(false);
        mCheckboxGIdentity.setChecked(false);
        mCheckboxLIdentity.setChecked(false);
        safeSetSelection(mColorIdentitySpinner, 0);

        safeSetSelection(mTextSpinner, 0);
        safeSetSelection(mTypeSpinner, 0);
        safeSetSelection(mSetSpinner, 0);

        safeSetSelection(mPowLogic, 0);
        safeSetSelection(mPowChoice, 0);
        safeSetSelection(mTouLogic, 0);
        safeSetSelection(mTouChoice, 0);
        safeSetSelection(mCmcLogic, 0);
        safeSetSelection(mCmcLogic, 1); /* CMC should default to < */
        safeSetSelection(mCmcChoice, 0);
        clearTextAndTokens(mManaCostTextView);
        safeSetSelection(mManaComparisonSpinner, Comparison.EMPTY.ordinal());

        if (mSetCheckedIndices != null) {
            mSetCheckedIndices = new int[0];
        }
        mSelectedFormat = -1;
        mRarityCheckedIndices = new int[0];
        this.removeDialog(getFragmentManager());

        checkDialogButtonColors();
    }

    /**
     * This helper function clears all tokens and text from an ATokenTextView
     *
     * @param field The ATokenTextView to clear everything from
     */
    private void clearTextAndTokens(ATokenTextView field) {
        while (!field.getObjects().isEmpty()) {
            field.removeObjectSync(field.getObjects().get(0));
        }
        field.clearCompletionText();
        field.clearComposingText();
        field.clearListSelection();
    }

    /**
     * This function saves the current search options into a file, so the user can have a default search
     */
    private void persistOptions() {
        try {
            FileOutputStream fileStream = Objects.requireNonNull(this.getActivity())
                    .openFileOutput(DEFAULT_CRITERIA_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fileStream);
            os.writeObject(parseForm());
            os.close();
        } catch (IOException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            SnackbarWrapper.makeAndShowText(this.getActivity(), R.string.search_toast_cannot_save, SnackbarWrapper.LENGTH_LONG);
        }
    }

    /**
     * This function reads the saved default search options and populates the UI elements
     */
    private void fetchPersistedOptions() {
        try {
            FileInputStream fileInputStream = Objects.requireNonNull(this.getActivity()).openFileInput(DEFAULT_CRITERIA_FILE);
            ObjectInputStream oInputStream = new ObjectInputStream(fileInputStream);
            SearchCriteria criteria = (SearchCriteria) oInputStream.readObject();
            oInputStream.close();

            setFieldsFromCriteria(criteria);
        } catch (IOException | ClassNotFoundException e) {
            SnackbarWrapper.makeAndShowText(this.getActivity(), R.string.search_toast_cannot_load, SnackbarWrapper.LENGTH_LONG);
        }
    }

    private void setFieldsFromCriteria(SearchCriteria criteria) {

        /* Set name */
        if (null != criteria.name) {
            mNameField.setText(criteria.name);
        }

        /* Set type fields */
        if (null != criteria.superTypes && criteria.superTypes.size() > 0) {
            for (String supertype : criteria.superTypes) {
                mSupertypeField.addObjectSync(supertype);
            }
        } else {
            clearTextAndTokens(mSupertypeField);
        }
        if (null != criteria.subTypes && criteria.subTypes.size() > 0) {
            for (String subtype : criteria.subTypes) {
                mSubtypeField.addObjectSync(subtype);
            }
        } else {
            clearTextAndTokens(mSubtypeField);
        }
        safeSetSelection(mTypeSpinner, criteria.typeLogic);

        /* Set text fields */
        mTextField.setText(criteria.text);
        safeSetSelection(mTextSpinner, criteria.textLogic);

        /* Set color fields */
        if (criteria.color != null) {
            mCheckboxW.setChecked(criteria.color.contains("W"));
            mCheckboxU.setChecked(criteria.color.contains("U"));
            mCheckboxB.setChecked(criteria.color.contains("B"));
            mCheckboxR.setChecked(criteria.color.contains("R"));
            mCheckboxG.setChecked(criteria.color.contains("G"));
            mCheckboxL.setChecked(criteria.color.contains("L"));
        }
        safeSetSelection(mColorSpinner, criteria.colorLogic);

        mIsCommander.setChecked(criteria.isCommander);

        if (criteria.colorIdentity != null) {
            mCheckboxWIdentity.setChecked(criteria.colorIdentity.contains("W"));
            mCheckboxUIdentity.setChecked(criteria.colorIdentity.contains("U"));
            mCheckboxBIdentity.setChecked(criteria.colorIdentity.contains("B"));
            mCheckboxRIdentity.setChecked(criteria.colorIdentity.contains("R"));
            mCheckboxGIdentity.setChecked(criteria.colorIdentity.contains("G"));
            mCheckboxLIdentity.setChecked(criteria.colorIdentity.contains("L"));
        }
        safeSetSelection(mColorIdentitySpinner, criteria.colorIdentityLogic);

        /* Set power and toughness fields */
        List<String> logicChoices = Arrays.asList(getResources().getStringArray(R.array.logic_spinner));
        safeSetSelection(mPowLogic, logicChoices.indexOf(criteria.powLogic));
        List<String> ptList = Arrays.asList(getResources().getStringArray(R.array.pt_spinner));
        float p = criteria.powChoice;
        if (p != CardDbAdapter.NO_ONE_CARES) {
            if (p == CardDbAdapter.STAR)
                safeSetSelection(mPowChoice, ptList.indexOf("*"));
            else if (p == CardDbAdapter.ONE_PLUS_STAR)
                safeSetSelection(mPowChoice, ptList.indexOf("1+*"));
            else if (p == CardDbAdapter.TWO_PLUS_STAR)
                safeSetSelection(mPowChoice, ptList.indexOf("2+*"));
            else if (p == CardDbAdapter.SEVEN_MINUS_STAR)
                safeSetSelection(mPowChoice, ptList.indexOf("7-*"));
            else if (p == CardDbAdapter.STAR_SQUARED)
                safeSetSelection(mPowChoice, ptList.indexOf("*^2"));
            else if (p == CardDbAdapter.X)
                safeSetSelection(mPowChoice, ptList.indexOf("X"));
            else if (p == CardDbAdapter.QUESTION_MARK)
                safeSetSelection(mPowChoice, ptList.indexOf("?"));
            else if (p == CardDbAdapter.INFINITY)
                safeSetSelection(mPowChoice, ptList.indexOf("∞"));
            else {
                if (p == (int) p) {
                    safeSetSelection(mPowChoice, ptList.indexOf(((int) p) + ""));
                } else {
                    safeSetSelection(mPowChoice, ptList.indexOf(p + ""));
                }
            }
        }
        safeSetSelection(mTouLogic, logicChoices.indexOf(criteria.touLogic));
        float t = criteria.touChoice;
        if (t != CardDbAdapter.NO_ONE_CARES) {
            if (t == CardDbAdapter.STAR)
                safeSetSelection(mTouChoice, ptList.indexOf("*"));
            else if (t == CardDbAdapter.ONE_PLUS_STAR)
                safeSetSelection(mTouChoice, ptList.indexOf("1+*"));
            else if (t == CardDbAdapter.TWO_PLUS_STAR)
                safeSetSelection(mTouChoice, ptList.indexOf("2+*"));
            else if (t == CardDbAdapter.SEVEN_MINUS_STAR)
                safeSetSelection(mTouChoice, ptList.indexOf("7-*"));
            else if (t == CardDbAdapter.STAR_SQUARED)
                safeSetSelection(mTouChoice, ptList.indexOf("*^2"));
            else if (t == CardDbAdapter.X)
                safeSetSelection(mTouChoice, ptList.indexOf("X"));
            else if (t == CardDbAdapter.QUESTION_MARK)
                safeSetSelection(mTouChoice, ptList.indexOf("?"));
            else if (t == CardDbAdapter.INFINITY)
                safeSetSelection(mTouChoice, ptList.indexOf("∞"));
            else {
                if (t == (int) t) {
                    safeSetSelection(mTouChoice, ptList.indexOf(((int) t) + ""));
                } else {
                    safeSetSelection(mTouChoice, ptList.indexOf(t + ""));
                }
            }
        }

        /* Set CMC fields */
        safeSetSelection(mCmcLogic, logicChoices.indexOf(criteria.cmcLogic));
        safeSetSelection(mCmcChoice, Arrays.asList(getResources().getStringArray(R.array.cmc_spinner))
                .indexOf(String.valueOf(criteria.cmc)));

        /* Set mana fields */
        if (criteria.manaCost != null && criteria.manaCost.size() > 0) {
            for (String mana : criteria.manaCost) {
                mManaCostTextView.addObjectSync(mana);
            }
        } else {
            clearTextAndTokens(mManaCostTextView);
        }
        if (null != criteria.manaCostLogic) {
            safeSetSelection(mManaComparisonSpinner, criteria.manaCostLogic.ordinal());
        }

        /* set format */
        if (mFormatNames != null) {
            mSelectedFormat = Arrays.asList(mFormatNames).indexOf(criteria.format);
        }

        /* Set rarity */
        if (criteria.rarity != null) {
            ArrayList<Integer> rarityCheckedIndicesTmp = new ArrayList<>();
            /* For each rarity */
            for (int i = 0; i < mRarityCodes.length; i++) {
                /* If the persisted options contain that rarity */
                if (criteria.rarity.contains(mRarityCodes[i] + "")) {
                    /* Save that index */
                    rarityCheckedIndicesTmp.add(i);
                }
            }
            mRarityCheckedIndices = new int[rarityCheckedIndicesTmp.size()];
            for (int i = 0; i < mRarityCheckedIndices.length; i++) {
                mRarityCheckedIndices[i] = rarityCheckedIndicesTmp.get(i);
            }
        }

        /* Set expansions */
        if (criteria.sets != null && criteria.sets.size() > 0) {
            /* Get a list of the persisted sets */
            for (String set : criteria.sets) {
                mSetField.addObjectSync(set);
            }
        } else {
            clearTextAndTokens(mSetField);
        }
        safeSetSelection(mSetSpinner, criteria.setLogic);

        if (null != criteria.setTypes && criteria.setTypes.size() > 0) {
            for (String setType : criteria.setTypes) {
                mSetTypeField.addObjectSync(setType);
            }
        } else {
            clearTextAndTokens(mSetTypeField);
        }

        /* Set text fields at the end */
        mWatermarkField.setText(criteria.watermark);
        mFlavorField.setText(criteria.flavor);
        mArtistField.setText(criteria.artist);
        mCollectorsNumberField.setText(criteria.collectorsNumber);

        checkDialogButtonColors();
    }

    /**
     * This function is checked when building the menu.
     * Since it returns true, the menu button will call onInterceptSearchKey() instead of being a quick search
     *
     * @return True
     */
    @Override
    boolean canInterceptSearchKey() {
        return true;
    }

    /**
     * This is called when the hardware search key is pressed.
     *
     * @return the fragment did something, so true
     */
    @Override
    public boolean onInterceptSearchKey() {
        doSearch();
        return true;
    }

    /**
     * Process a button press on the ActionBar
     *
     * @param item The item pressed
     * @return true if the action was taken, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle item selection */
        if (item.getItemId() == R.id.search_menu_clear) {
            clear();
            return true;
        } else if (item.getItemId() == R.id.search_menu_save_defaults) {
            persistOptions();
            return true;
        } else if (item.getItemId() == R.id.search_menu_load_defaults) {
            fetchPersistedOptions();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This is a convenience function to set the colors for the buttons which pop dialogs
     */
    public void checkDialogButtonColors() {

        if (!isAdded()) {
            return;
        }

        /* Set the default color */
        mFormatButton.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), getResourceIdFromAttr(R.attr.color_text)));
        mRarityButton.setTextColor(ContextCompat.getColor(getContext(), getResourceIdFromAttr(R.attr.color_text)));

        if (mSetCheckedIndices == null || mRarityCheckedIndices == null) {
            return;
        }

        /* Set the selected color, if necessary */
        if (mSelectedFormat != -1) {
            mFormatButton.setTextColor(ContextCompat.getColor(getContext(), getResourceIdFromAttr(R.attr.colorPrimary_attr)));
        }
        if (mRarityCheckedIndices.length > 0) {
            mRarityButton.setTextColor(ContextCompat.getColor(getContext(), getResourceIdFromAttr(R.attr.colorPrimary_attr)));
        }
    }

    /**
     * This will remove any currently showing dialogs and display the one given by id
     * Usually the dialogs are created here, but in this case they were created in onCreate, because of the db calls,
     * and that they are recreated in order to clear them
     *
     * @param id the id of the dialog to be shown
     */
    private void showDialog(final int id) throws IllegalStateException {
        /* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        /* Create and show the dialog. */
        SearchViewDialogFragment newFragment = new SearchViewDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        newFragment.setArguments(arguments);
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Inflate the menu
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);
    }

}
