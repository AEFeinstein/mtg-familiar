package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SearchViewDialogFragment;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.SpaceTokenizer;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This fragment lets users configure search parameters, and then search for a card
 * The actual database query is handled in the ResultListFragment
 */
public class SearchViewFragment extends FamiliarFragment {

    /* String keys */
    public static final String CRITERIA = "criteria";

    /* Default search file */
    private static final String DEFAULT_CRITERIA_FILE = "defaultSearchCriteria.ser";

    /* Keys for persisting state */
    private static final String SAVED_SET_KEY = "SAVED_SET_KEY";
    private static final String SAVED_RARITY_KEY = "SAVED_RARITY_KEY";
    private static final String SAVED_FORMAT_KEY = "SAVED_FORMAT_KEY";

    /* Spinner Data Structures */
    public String[] mSetNames;
    public int[] mSetCheckedIndices;
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

    /* UI Elements */
    private AutoCompleteTextView mNameField;
    private EditText mTextField;
    private MultiAutoCompleteTextView mSupertypeField = null;
    private MultiAutoCompleteTextView mSubtypeField = null;
    private EditText mCollectorsNumberField;
    private CheckBox mCheckboxW;
    private CheckBox mCheckboxU;
    private CheckBox mCheckboxB;
    private CheckBox mCheckboxR;
    private CheckBox mCheckboxG;
    private CheckBox mCheckboxL;
    private Spinner mColorSpinner;
    private CheckBox mCheckboxWIdentity;
    private CheckBox mCheckboxUIdentity;
    private CheckBox mCheckboxBIdentity;
    private CheckBox mCheckboxRIdentity;
    private CheckBox mCheckboxGIdentity;
    private CheckBox mCheckboxLIdentity;
    private Spinner mColorIdentitySpinner;
    private Button mSetButton;
    private Button mFormatButton;
    private Button mRarityButton;
    private Spinner mPowLogic;
    private Spinner mPowChoice;
    private Spinner mTouLogic;
    private Spinner mTouChoice;
    private Spinner mCmcLogic;
    private Spinner mCmcChoice;
    public Dialog mSetDialog;
    public Dialog mFormatDialog;
    public Dialog mRarityDialog;
    private EditText mFlavorField;
    private AutoCompleteTextView mArtistField = null;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /* Inflate the view */
        View myFragmentView = inflater.inflate(R.layout.search_frag, container, false);
        assert myFragmentView != null;

        /* Get references to UI elements. When a search is preformed, these values will be queried */
        mNameField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.name_search);
        mTextField = (EditText) myFragmentView.findViewById(R.id.textsearch);
        mSupertypeField = (MultiAutoCompleteTextView) myFragmentView.findViewById(R.id.supertypesearch);
        mSubtypeField = (MultiAutoCompleteTextView) myFragmentView.findViewById(R.id.subtypesearch);
        mFlavorField = (EditText) myFragmentView.findViewById(R.id.flavorsearch);
        mArtistField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.artistsearch);
        mCollectorsNumberField = (EditText) myFragmentView.findViewById(R.id.collectorsnumbersearch);

        Button searchButton = (Button) myFragmentView.findViewById(R.id.searchbutton);

        mCheckboxW = (CheckBox) myFragmentView.findViewById(R.id.checkBoxW);
        mCheckboxU = (CheckBox) myFragmentView.findViewById(R.id.checkBoxU);
        mCheckboxB = (CheckBox) myFragmentView.findViewById(R.id.checkBoxB);
        mCheckboxR = (CheckBox) myFragmentView.findViewById(R.id.checkBoxR);
        mCheckboxG = (CheckBox) myFragmentView.findViewById(R.id.checkBoxG);
        mCheckboxL = (CheckBox) myFragmentView.findViewById(R.id.checkBoxL);

        mCheckboxWIdentity = (CheckBox) myFragmentView.findViewById(R.id.checkBoxW_identity);
        mCheckboxUIdentity = (CheckBox) myFragmentView.findViewById(R.id.checkBoxU_identity);
        mCheckboxBIdentity = (CheckBox) myFragmentView.findViewById(R.id.checkBoxB_identity);
        mCheckboxRIdentity = (CheckBox) myFragmentView.findViewById(R.id.checkBoxR_identity);
        mCheckboxGIdentity = (CheckBox) myFragmentView.findViewById(R.id.checkBoxG_identity);
        mCheckboxLIdentity = (CheckBox) myFragmentView.findViewById(R.id.checkBoxL_identity);

        mColorSpinner = (Spinner) myFragmentView.findViewById(R.id.colorlogic);
        mColorIdentitySpinner = (Spinner) myFragmentView.findViewById(R.id.coloridentitylogic);
        mTextSpinner = (Spinner) myFragmentView.findViewById(R.id.textlogic);
        mTypeSpinner = (Spinner) myFragmentView.findViewById(R.id.typelogic);
        mSetSpinner = (Spinner) myFragmentView.findViewById(R.id.setlogic);

        mSetButton = (Button) myFragmentView.findViewById(R.id.setsearch);
        mFormatButton = (Button) myFragmentView.findViewById(R.id.formatsearch);
        mRarityButton = (Button) myFragmentView.findViewById(R.id.raritysearch);

        mPowLogic = (Spinner) myFragmentView.findViewById(R.id.powLogic);
        mPowChoice = (Spinner) myFragmentView.findViewById(R.id.powChoice);
        mTouLogic = (Spinner) myFragmentView.findViewById(R.id.touLogic);
        mTouChoice = (Spinner) myFragmentView.findViewById(R.id.touChoice);
        mCmcLogic = (Spinner) myFragmentView.findViewById(R.id.cmcLogic);
        mCmcChoice = (Spinner) myFragmentView.findViewById(R.id.cmcChoice);

        /* set the buttons to open the dialogs */
        mSetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(SearchViewDialogFragment.SET_LIST);
            }
        });
        mFormatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(SearchViewDialogFragment.FORMAT_LIST);
            }
        });
        mRarityButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(SearchViewDialogFragment.RARITY_LIST);
            }
        });

        /* This is a better default, might want to reorder the array */
        mColorSpinner.setSelection(2);

        /* The button colors change whether an option is selected or not */
        checkDialogButtonColors();

        /* This listener will do searches directly from the TextViews. Attach it to everything! */
        TextView.OnEditorActionListener doSearchListener = new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
                if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch();
                    return true;
                }
                return false;
            }
        };
        mNameField.setOnEditorActionListener(doSearchListener);
        mTextField.setOnEditorActionListener(doSearchListener);
        mSupertypeField.setOnEditorActionListener(doSearchListener);
        mSubtypeField.setOnEditorActionListener(doSearchListener);
        mFlavorField.setOnEditorActionListener(doSearchListener);
        mArtistField.setOnEditorActionListener(doSearchListener);
        mCollectorsNumberField.setOnEditorActionListener(doSearchListener);

        /* set the autocomplete for card names */
        mNameField.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME}, new int[]{R.id.text1}, mNameField, true));
        mNameField.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SearchCriteria searchCriteria = new SearchCriteria();
                searchCriteria.name = ((TextView) view.findViewById(R.id.text1)).getText().toString();
                Bundle args = new Bundle();
                args.putSerializable(CRITERIA, searchCriteria);
                ResultListFragment rlFrag = new ResultListFragment();
                startNewFragment(rlFrag, args);
            }
        });

        /* Get a bunch of database info in a background task */
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);

                /* Only actually get data if the arrays are null */
                if (mSetNames == null) {
                    try {
                        /* Query the database for all sets and fill the arrays to populate the list of choices with */
                        Cursor setCursor = CardDbAdapter.fetchAllSets(database);
                        setCursor.moveToFirst();

                        mSetNames = new String[setCursor.getCount()];
                        mSetSymbols = new String[setCursor.getCount()];

                        /* If this wasn't persisted, create it new */
                        if (mSetCheckedIndices == null) {
                            mSetCheckedIndices = new int[0];
                        }

                        for (int i = 0; i < setCursor.getCount(); i++) {
                            mSetSymbols[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_CODE));
                            mSetNames[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
                            setCursor.moveToNext();
                        }
                        setCursor.close();
                    } catch (FamiliarDbException e) {
                        handleFamiliarDbException(true);
                    }
                }

                if (mFormatNames == null) {
                    try {
                        /* Query the database for all formats and fill the arrays to populate the list of choices with */
                        Cursor formatCursor = CardDbAdapter.fetchAllFormats(database);
                        formatCursor.moveToFirst();

                        mFormatNames = new String[formatCursor.getCount()];

                        for (int i = 0; i < formatCursor.getCount(); i++) {
                            mFormatNames[i] = formatCursor.getString(formatCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
                            formatCursor.moveToNext();
                        }
                        formatCursor.close();
                    } catch (FamiliarDbException e) {
                        handleFamiliarDbException(true);
                    }
                }

                if (mSupertypes == null) {
                    try {
                        mSupertypes = CardDbAdapter.getUniqueColumnArray(CardDbAdapter.KEY_SUPERTYPE, true, database);
                    } catch (FamiliarDbException e) {
                        handleFamiliarDbException(true);
                    }
                }

                if (mSubtypes == null) {
                    try {
                        mSubtypes = CardDbAdapter.getUniqueColumnArray(CardDbAdapter.KEY_SUBTYPE, true, database);
                    } catch (FamiliarDbException e) {
                        handleFamiliarDbException(true);
                    }
                }

                if (mArtists == null) {
                    try {
                        mArtists = CardDbAdapter.getUniqueColumnArray(CardDbAdapter.KEY_ARTIST, false, database);
                    } catch (FamiliarDbException e) {
                        handleFamiliarDbException(true);
                    }
                }

                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            /* set the autocomplete for supertypes */
                            ArrayAdapter<String> supertypeAdapter = new ArrayAdapter<>(
                                    SearchViewFragment.this.getActivity(), R.layout.list_item_1, mSupertypes);
                            mSupertypeField.setThreshold(1);
                            mSupertypeField.setAdapter(supertypeAdapter);
                            mSupertypeField.setTokenizer(new SpaceTokenizer());

                            /* set the autocomplete for subtypes */
                            ArrayAdapter<String> subtypeAdapter = new ArrayAdapter<>(
                                    SearchViewFragment.this.getActivity(), R.layout.list_item_1, mSubtypes);
                            mSubtypeField.setThreshold(1);
                            mSubtypeField.setAdapter(subtypeAdapter);
                            mSubtypeField.setTokenizer(new SpaceTokenizer());

                            /* set the autocomplete for artists */
                            ArrayAdapter<String> artistAdapter = new ArrayAdapter<>(
                                    SearchViewFragment.this.getActivity(), R.layout.list_item_1, mArtists);
                            mArtistField.setThreshold(1);
                            mArtistField.setAdapter(artistAdapter);
                        }
                    });
                } catch (NullPointerException e) {
                    /* If the UI thread isn't here, eat it */
                }
            }
        }.execute();

        /* set the search button! */
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSearch();
            }
        });

        myFragmentView.findViewById(R.id.camera_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SearchViewFragment.this.getFamiliarActivity().startTutorCardsSearch();
                    }
                }
        );
        return myFragmentView;
    }

    /**
     * Generic onResume. Catches when consolidation is changed in preferences
     */
    @Override
    public void onResume() {
        super.onResume();

        /* Do we want to consolidate different printings of the same card in results, or not? */
        boolean consolidate = getFamiliarActivity().mPreferenceAdapter.getConsolidateSearch();
        mSetSpinner.setSelection(consolidate ? CardDbAdapter.MOST_RECENT_PRINTING : CardDbAdapter.ALL_PRINTINGS);
    }

    /**
     * Save the state of the dialog selections
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putInt(SAVED_FORMAT_KEY, mSelectedFormat);
            outState.putIntArray(SAVED_RARITY_KEY, mRarityCheckedIndices);
            outState.putIntArray(SAVED_SET_KEY, mSetCheckedIndices);
        }
    }

    /**
     * This function creates a results fragment, sends it the search criteria, and starts it
     */
    private void doSearch() {
        SearchCriteria searchCriteria = parseForm();
        Bundle args = new Bundle();
        args.putSerializable(CRITERIA, searchCriteria);
        ResultListFragment rlFrag = new ResultListFragment();
        startNewFragment(rlFrag, args);
    }

    /**
     * This function combs through all the UI elements and returns a SearchCriteria with the current search options
     *
     * @return a SearchCriteria with what the user wants to search for
     */
    private SearchCriteria parseForm() {
        SearchCriteria searchCriteria = new SearchCriteria();

        /* Because Android Studio whines */
        assert mNameField.getText() != null;
        assert mTextField.getText() != null;
        assert mSupertypeField.getText() != null;
        assert mSubtypeField.getText() != null;
        assert mFlavorField.getText() != null;
        assert mArtistField.getText() != null;
        assert mCollectorsNumberField.getText() != null;

        /* Read EditTexts */
        searchCriteria.name = mNameField.getText().toString().trim();
        searchCriteria.text = mTextField.getText().toString().trim();
        String supertype = mSupertypeField.getText().toString().trim();
        String subtype = mSubtypeField.getText().toString().trim();
        searchCriteria.type = supertype.trim() + " - " + subtype.trim();
        searchCriteria.flavor = mFlavorField.getText().toString().trim();
        searchCriteria.artist = mArtistField.getText().toString().trim();
        searchCriteria.collectorsNumber = mCollectorsNumberField.getText().toString().trim();

        if (searchCriteria.name.length() == 0) {
            searchCriteria.name = null;
        }
        if (searchCriteria.text.length() == 0) {
            searchCriteria.text = null;
        }
        if (searchCriteria.type.length() == 0) {
            searchCriteria.type = null;
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

        searchCriteria.set = null;

        if (mSetCheckedIndices != null) {
            for (int index : mSetCheckedIndices) {
                if (searchCriteria.set == null) {
                    searchCriteria.set = mSetSymbols[index];
                } else {
                    searchCriteria.set += "-" + mSetSymbols[index];
                }
            }
        }

        searchCriteria.format = null;
        if (mSelectedFormat != -1 && mFormatNames != null) {
            searchCriteria.format = mFormatNames[mSelectedFormat];
        }

        searchCriteria.rarity = null;
        for (int index : mRarityCheckedIndices) {
            if (searchCriteria.rarity == null) {
                searchCriteria.rarity = mRarityCodes[index] + "";
            } else {
                searchCriteria.rarity += mRarityCodes[index];
            }
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
        mSupertypeField.setText("");
        mSubtypeField.setText("");
        mTextField.setText("");
        mArtistField.setText("");
        mFlavorField.setText("");
        mCollectorsNumberField.setText("");

        mCheckboxW.setChecked(false);
        mCheckboxU.setChecked(false);
        mCheckboxB.setChecked(false);
        mCheckboxR.setChecked(false);
        mCheckboxG.setChecked(false);
        mCheckboxL.setChecked(false);
        mColorSpinner.setSelection(2);

        mCheckboxWIdentity.setChecked(false);
        mCheckboxUIdentity.setChecked(false);
        mCheckboxBIdentity.setChecked(false);
        mCheckboxRIdentity.setChecked(false);
        mCheckboxGIdentity.setChecked(false);
        mCheckboxLIdentity.setChecked(false);
        mColorIdentitySpinner.setSelection(0);

        mTextSpinner.setSelection(0);
        mTypeSpinner.setSelection(0);
        mSetSpinner.setSelection(0);

        mPowLogic.setSelection(0);
        mPowChoice.setSelection(0);
        mTouLogic.setSelection(0);
        mTouChoice.setSelection(0);
        mCmcLogic.setSelection(0);
        mCmcLogic.setSelection(1); /* CMC should default to < */
        mCmcChoice.setSelection(0);

        if (mSetCheckedIndices != null) {
            mSetCheckedIndices = new int[0];
        }
        mSelectedFormat = -1;
        mRarityCheckedIndices = new int[0];
        this.removeDialog(getFragmentManager());

        checkDialogButtonColors();
    }

    /**
     * This function saves the current search options into a file, so the user can have a default search
     */
    private void persistOptions() {
        try {
            SearchCriteria searchCriteria = parseForm();
            FileOutputStream fileStream = this.getActivity()
                    .openFileOutput(DEFAULT_CRITERIA_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fileStream);
            os.writeObject(searchCriteria);
            os.close();
        } catch (IOException e) {
            ToastWrapper.makeText(this.getActivity(), R.string.search_toast_cannot_save, ToastWrapper.LENGTH_LONG).show();
        }
    }

    /**
     * This function reads the saved default search options and populates the UI elements
     */
    private void fetchPersistedOptions() {
        try {
            FileInputStream fileInputStream = this.getActivity().openFileInput(DEFAULT_CRITERIA_FILE);
            ObjectInputStream oInputStream = new ObjectInputStream(fileInputStream);
            SearchCriteria criteria = (SearchCriteria) oInputStream.readObject();
            oInputStream.close();

            mNameField.setText(criteria.name);
            String delimiter = " - ";
            String[] type = criteria.type.split(delimiter);
            if (type.length > 0 && type[0] != null) {
                mSupertypeField.setText(type[0]);
            }
            if (type.length > 1 && type[1] != null) {
                /* Concatenate all strings after the first delimiter
                 * in case there's a hyphen in the subtype
                 */
                String subtype = "";
                boolean first = true;
                for (int i = 1; i < type.length; i++) {
                    if (!first) {
                        subtype += delimiter;
                    }
                    subtype += type[i];
                    first = false;
                }
                mSubtypeField.setText(subtype);
            }
            mTextField.setText(criteria.text);
            mArtistField.setText(criteria.artist);
            mFlavorField.setText(criteria.flavor);
            mCollectorsNumberField.setText(criteria.collectorsNumber);

            if (criteria.color != null) {
                mCheckboxW.setChecked(criteria.color.contains("W"));
                mCheckboxU.setChecked(criteria.color.contains("U"));
                mCheckboxB.setChecked(criteria.color.contains("B"));
                mCheckboxR.setChecked(criteria.color.contains("R"));
                mCheckboxG.setChecked(criteria.color.contains("G"));
                mCheckboxL.setChecked(criteria.color.contains("L"));
            }
            mColorSpinner.setSelection(criteria.colorLogic);

            if (criteria.colorIdentity != null) {
                mCheckboxWIdentity.setChecked(criteria.colorIdentity.contains("W"));
                mCheckboxUIdentity.setChecked(criteria.colorIdentity.contains("U"));
                mCheckboxBIdentity.setChecked(criteria.colorIdentity.contains("B"));
                mCheckboxRIdentity.setChecked(criteria.colorIdentity.contains("R"));
                mCheckboxGIdentity.setChecked(criteria.colorIdentity.contains("G"));
                mCheckboxLIdentity.setChecked(criteria.colorIdentity.contains("L"));
            }
            mColorIdentitySpinner.setSelection(criteria.colorIdentityLogic);

            mTextSpinner.setSelection(criteria.textLogic);
            mTypeSpinner.setSelection(criteria.typeLogic);
            mSetSpinner.setSelection(criteria.setLogic);

            List<String> logicChoices = Arrays.asList(getResources().getStringArray(R.array.logic_spinner));
            mPowLogic.setSelection(logicChoices.indexOf(criteria.powLogic));
            List<String> ptList = Arrays.asList(getResources().getStringArray(R.array.pt_spinner));
            float p = criteria.powChoice;
            if (p != CardDbAdapter.NO_ONE_CARES) {
                if (p == CardDbAdapter.STAR)
                    mPowChoice.setSelection(ptList.indexOf("*"));
                else if (p == CardDbAdapter.ONE_PLUS_STAR)
                    mPowChoice.setSelection(ptList.indexOf("1+*"));
                else if (p == CardDbAdapter.TWO_PLUS_STAR)
                    mPowChoice.setSelection(ptList.indexOf("2+*"));
                else if (p == CardDbAdapter.SEVEN_MINUS_STAR)
                    mPowChoice.setSelection(ptList.indexOf("7-*"));
                else if (p == CardDbAdapter.STAR_SQUARED)
                    mPowChoice.setSelection(ptList.indexOf("*^2"));
                else {
                    if (p == (int) p) {
                        mPowChoice.setSelection(ptList.indexOf(((int) p) + ""));
                    } else {
                        mPowChoice.setSelection(ptList.indexOf(p + ""));
                    }
                }
            }
            mTouLogic.setSelection(logicChoices.indexOf(criteria.touLogic));
            float t = criteria.touChoice;
            if (t != CardDbAdapter.NO_ONE_CARES) {
                if (t == CardDbAdapter.STAR)
                    mTouChoice.setSelection(ptList.indexOf("*"));
                else if (t == CardDbAdapter.ONE_PLUS_STAR)
                    mTouChoice.setSelection(ptList.indexOf("1+*"));
                else if (t == CardDbAdapter.TWO_PLUS_STAR)
                    mTouChoice.setSelection(ptList.indexOf("2+*"));
                else if (t == CardDbAdapter.SEVEN_MINUS_STAR)
                    mTouChoice.setSelection(ptList.indexOf("7-*"));
                else if (t == CardDbAdapter.STAR_SQUARED)
                    mTouChoice.setSelection(ptList.indexOf("*^2"));
                else {
                    if (t == (int) t) {
                        mTouChoice.setSelection(ptList.indexOf(((int) t) + ""));
                    } else {
                        mTouChoice.setSelection(ptList.indexOf(t + ""));
                    }
                }
            }
            mCmcLogic.setSelection(logicChoices.indexOf(criteria.cmcLogic));
            mCmcChoice.setSelection(Arrays.asList(getResources().getStringArray(R.array.cmc_spinner))
                    .indexOf(String.valueOf(criteria.cmc)));

            if (criteria.set != null) {
                /* Get a list of the persisted sets */
                List<String> sets = Arrays.asList(criteria.set.split("-"));
                ArrayList<Integer> setCheckedIndicesTmp = new ArrayList<>();
                /* For each set */
                for (String set : sets) {
                    for (int i = 0; i < mSetSymbols.length; i++) {
                        /* Find the index of that set */
                        if (mSetSymbols[i].equals(set)) {
                            /* And add it to the selected indices */
                            setCheckedIndicesTmp.add(i);
                            break;
                        }
                    }
                }
                /* Copy over the indices */
                mSetCheckedIndices = new int[sets.size()];
                for (int i = 0; i < setCheckedIndicesTmp.size(); i++) {
                    mSetCheckedIndices[i] = setCheckedIndicesTmp.get(i);
                }
            } else {
                mSetCheckedIndices = new int[0];
            }

            if (mFormatNames != null) {
                mSelectedFormat = Arrays.asList(mFormatNames).indexOf(criteria.format);
            }

            if (criteria.rarity != null) {
                ArrayList<Integer> rarityCheckedIndicesTmp = new ArrayList<>();
                /* For each rarity */
                for (int i = 0; i < mRarityNames.length; i++) {
                    /* If the persisted options contain that rarity */
                    if (criteria.rarity.contains(mRarityNames[i].charAt(0) + "")) {
                        /* Save that index */
                        rarityCheckedIndicesTmp.add(i);
                    }
                }
                mRarityCheckedIndices = new int[rarityCheckedIndicesTmp.size()];
                for (int i = 0; i < mRarityCheckedIndices.length; i++) {
                    mRarityCheckedIndices[i] = rarityCheckedIndicesTmp.get(i);
                }
            }

            this.removeDialog(getFragmentManager());
            checkDialogButtonColors();

        } catch (IOException | ClassNotFoundException e) {
            ToastWrapper.makeText(this.getActivity(), R.string.search_toast_cannot_load, ToastWrapper.LENGTH_LONG).show();
        }
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
        switch (item.getItemId()) {
            case R.id.search_menu_clear:
                clear();
                return true;
            case R.id.search_menu_save_defaults:
                persistOptions();
                return true;
            case R.id.search_menu_load_defaults:
                fetchPersistedOptions();
                return true;
            default:
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
        mSetButton.setTextColor(getResources().getColor(getResourceIdFromAttr(R.attr.color_text)));
        mFormatButton.setTextColor(getResources().getColor(getResourceIdFromAttr(R.attr.color_text)));
        mRarityButton.setTextColor(getResources().getColor(getResourceIdFromAttr(R.attr.color_text)));

        if (mSetCheckedIndices == null || mRarityCheckedIndices == null) {
            return;
        }

        /* Set the selected color, if necessary */
        if (mSetCheckedIndices.length > 0) {
            mSetButton.setTextColor(getResources().getColor(getResourceIdFromAttr(R.attr.colorPrimary_attr)));
        }
        if (mSelectedFormat != -1) {
            mFormatButton.setTextColor(getResources().getColor(getResourceIdFromAttr(R.attr.colorPrimary_attr)));
        }
        if (mRarityCheckedIndices.length > 0) {
            mRarityButton.setTextColor(getResources().getColor(getResourceIdFromAttr(R.attr.colorPrimary_attr)));
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);
    }

    /**
     * Called when there is a result from Tutor.cards visual search
     * It starts the ResultListFragment with the multiverseId to show
     * the card
     *
     * @param multiverseId The multiverse ID of the card the query returned
     */
    @Override
    public void receiveTutorCardsResult(long multiverseId) {

        Bundle args = new Bundle();
        args.putSerializable(ResultListFragment.CARD_ID, multiverseId);
        ResultListFragment rlFrag = new ResultListFragment();
        startNewFragment(rlFrag, args);
    }
}
