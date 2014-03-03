package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
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
import android.widget.Toast;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * This fragment lets users configure search parameters, and then search for a card
 * The actual database query is handled in the ResultListFragment
 */
public class SearchViewFragment extends FamiliarFragment {

	/* String keys */
	public static final String CRITERIA = "criteria";

	/* Dialog IDs */
	private static final int SET_LIST = 1;
	private static final int FORMAT_LIST = 2;
	private static final int RARITY_LIST = 3;

	/* Default search file */
	private static final String DEFAULT_CRITERIA_FILE = "defaultSearchCriteria.ser";

	/* Spinner Data Structures */
	private String[] mSetNames;
	private boolean[] mSetChecked;
	private String[] mSetSymbols;
	private String[] mFormatNames;
	private String[] mRarityNames;
	private boolean[] mRarityChecked;
	private int mSelectedFormat;

	/* UI Elements */
	private AutoCompleteTextView mNameField;
	private EditText mTextField;
	private AutoCompleteTextView mSupertypeField;
	private EditText mSubtypeField;
	private CheckBox mCheckboxW;
	private CheckBox mCheckboxU;
	private CheckBox mCheckboxB;
	private CheckBox mCheckboxR;
	private CheckBox mCheckboxG;
	private CheckBox mCheckboxL;
	private Spinner mColorSpinner;
	private Button mSetButton;
	private Button mFormatButton;
	private Button mRarityButton;
	private Spinner mPowLogic;
	private Spinner mPowChoice;
	private Spinner mTouLogic;
	private Spinner mTouChoice;
	private Spinner mCmcLogic;
	private Spinner mCmcChoice;
	private AlertDialog mSetDialog;
	private AlertDialog mFormatDialog;
	private AlertDialog mRarityDialog;
	private EditText mFlavorField;
	private EditText mArtistField;
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

		CardDbAdapter dbHelper = null;
		try {
			dbHelper = new CardDbAdapter(this.getActivity());
			/* Query the database for all sets and fill the arrays to populate the list of choices with */
			Cursor setCursor = dbHelper.fetchAllSets();
			setCursor.moveToFirst();

			mSetNames = new String[setCursor.getCount()];
			mSetSymbols = new String[setCursor.getCount()];
			mSetChecked = new boolean[setCursor.getCount()];

			for (int i = 0; i < setCursor.getCount(); i++) {
				mSetSymbols[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_CODE));
				mSetNames[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
				mSetChecked[i] = false;
				setCursor.moveToNext();
			}
			setCursor.close();

            /* Query the database for all formats and fill the arrays to populate the list of choices with */
			Cursor formatCursor = dbHelper.fetchAllFormats();
			formatCursor.moveToFirst();

			mFormatNames = new String[formatCursor.getCount()];

			for (int i = 0; i < formatCursor.getCount(); i++) {
				mFormatNames[i] = formatCursor.getString(formatCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
				formatCursor.moveToNext();
			}
			formatCursor.close();
			mSelectedFormat = -1;
		} catch (FamiliarDbException e) {
			handleFamiliarDbException(true);
		} finally {
			if (dbHelper != null) {
				dbHelper.close();
			}
		}

        /* Get the different rarities out of resources to populate the list of choices with */
		Resources res = getResources();
		mRarityNames = res.getStringArray(R.array.rarities);
		mRarityChecked = new boolean[mRarityNames.length];
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
		mNameField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.namesearch);
		mTextField = (EditText) myFragmentView.findViewById(R.id.textsearch);
		mSupertypeField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.supertypesearch);
		mSubtypeField = (EditText) myFragmentView.findViewById(R.id.subtypesearch);
		mFlavorField = (EditText) myFragmentView.findViewById(R.id.flavorsearch);
		mArtistField = (EditText) myFragmentView.findViewById(R.id.artistsearch);

		Button searchButton = (Button) myFragmentView.findViewById(R.id.searchbutton);

		mCheckboxW = (CheckBox) myFragmentView.findViewById(R.id.checkBoxW);
		mCheckboxU = (CheckBox) myFragmentView.findViewById(R.id.checkBoxU);
		mCheckboxB = (CheckBox) myFragmentView.findViewById(R.id.checkBoxB);
		mCheckboxR = (CheckBox) myFragmentView.findViewById(R.id.checkBoxR);
		mCheckboxG = (CheckBox) myFragmentView.findViewById(R.id.checkBoxG);
		mCheckboxL = (CheckBox) myFragmentView.findViewById(R.id.checkBoxL);

		mColorSpinner = (Spinner) myFragmentView.findViewById(R.id.colorlogic);
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
				showDialog(SET_LIST);
			}
		});
		mFormatButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(FORMAT_LIST);
			}
		});
		mRarityButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(RARITY_LIST);
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

		/* set the autocomplete for card names */
		mNameField.setAdapter(new AutocompleteCursorAdapter(this.getActivity()));
		/* set the autocomplete for supertypes */
		String[] supertypes = getResources().getStringArray(R.array.supertypes);
		ArrayAdapter<String> supertypeAdapter = new ArrayAdapter<String>(this.getActivity(),
				R.layout.list_item_1, supertypes);
		mSupertypeField.setThreshold(1);
		mSupertypeField.setAdapter(supertypeAdapter);

		/* Do we want to consolidate different printings of the same card in results, or not? */
		boolean consolidate = ((FamiliarActivity) getActivity()).mPreferenceAdapter.getConsolidateSearch();
		mSetSpinner.setSelection(consolidate ? CardDbAdapter.MOSTRECENTPRINTING : CardDbAdapter.ALLPRINTINGS);

		/* set the search button! */
		searchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doSearch();
			}
		});

		return myFragmentView;
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

		/* Read EditTexts */
		searchCriteria.name = mNameField.getText().toString();
		searchCriteria.text = mTextField.getText().toString();
		String supertype = mSupertypeField.getText().toString();
		String subtype = mSubtypeField.getText().toString();
		searchCriteria.type = supertype + " - " + subtype;
		searchCriteria.flavor = mFlavorField.getText().toString();
		searchCriteria.artist = mArtistField.getText().toString();

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

		/* Build a color string. capital letters means the user is search for that color */
		searchCriteria.color = null;

		if (mCheckboxW.isChecked()) {
			searchCriteria.color = "W";
		}
		else {
			searchCriteria.color = "w";
		}

		if (mCheckboxU.isChecked()) {
			searchCriteria.color += "U";
		}
		else {
			searchCriteria.color += "u";
		}
		if (mCheckboxB.isChecked()) {
			searchCriteria.color += "B";
		}
		else {
			searchCriteria.color += "b";
		}
		if (mCheckboxR.isChecked()) {
			searchCriteria.color += "R";
		}
		else {
			searchCriteria.color += "r";
		}
		if (mCheckboxG.isChecked()) {
			searchCriteria.color += "G";
		}
		else {
			searchCriteria.color += "g";
		}
		if (mCheckboxL.isChecked()) {
			searchCriteria.color += "L";
		}
		else {
			searchCriteria.color += "l";
		}
		searchCriteria.colorLogic = mColorSpinner.getSelectedItemPosition();

		searchCriteria.set = null;

		for (int i = 0; i < mSetChecked.length; i++) {
			if (mSetChecked[i]) {
				if (searchCriteria.set == null) {
					searchCriteria.set = mSetSymbols[i];
				}
				else {
					searchCriteria.set += "-" + mSetSymbols[i];
				}
			}
		}

		searchCriteria.format = null;
		if (mSelectedFormat != -1) {
			searchCriteria.format = mFormatNames[mSelectedFormat];
		}

		searchCriteria.rarity = null;
		for (int i = 0; i < mRarityChecked.length; i++) {
			if (mRarityChecked[i]) {
				if (searchCriteria.rarity == null) {
					searchCriteria.rarity = mRarityNames[i].charAt(0) + "";
				}
				else {
					searchCriteria.rarity += mRarityNames[i].charAt(0);
				}
			}
		}

		String[] logicChoices = getResources().getStringArray(R.array.logic_spinner);
		String power = getResources().getStringArray(R.array.pt_spinner)[mPowChoice.getSelectedItemPosition()];
		String toughness = getResources().getStringArray(R.array.pt_spinner)[mTouChoice.getSelectedItemPosition()];

		float pow = CardDbAdapter.NOONECARES;
		try {
			pow = Float.parseFloat(power);
		} catch (NumberFormatException e) {
			if (power.equals("*")) {
				pow = CardDbAdapter.STAR;
			}
			else if (power.equals("1+*")) {
				pow = CardDbAdapter.ONEPLUSSTAR;
			}
			else if (power.equals("2+*")) {
				pow = CardDbAdapter.TWOPLUSSTAR;
			}
			else if (power.equals("7-*")) {
				pow = CardDbAdapter.SEVENMINUSSTAR;
			}
			else if (power.equals("*^2")) {
				pow = CardDbAdapter.STARSQUARED;
			}
		}
		searchCriteria.powChoice = pow;
		searchCriteria.powLogic = logicChoices[mPowLogic.getSelectedItemPosition()];

		float tou = CardDbAdapter.NOONECARES;
		try {
			tou = Float.parseFloat(toughness);
		} catch (NumberFormatException e) {
			if (toughness.equals("*")) {
				tou = CardDbAdapter.STAR;
			}
			else if (toughness.equals("1+*")) {
				tou = CardDbAdapter.ONEPLUSSTAR;
			}
			else if (toughness.equals("2+*")) {
				tou = CardDbAdapter.TWOPLUSSTAR;
			}
			else if (toughness.equals("7-*")) {
				tou = CardDbAdapter.SEVENMINUSSTAR;
			}
			else if (toughness.equals("*^2")) {
				tou = CardDbAdapter.STARSQUARED;
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
	void clear() {
		mNameField.setText("");
		mSupertypeField.setText("");
		mSubtypeField.setText("");
		mTextField.setText("");
		mArtistField.setText("");
		mFlavorField.setText("");

		mCheckboxW.setChecked(false);
		mCheckboxU.setChecked(false);
		mCheckboxB.setChecked(false);
		mCheckboxR.setChecked(false);
		mCheckboxG.setChecked(false);
		mCheckboxL.setChecked(false);
		mColorSpinner.setSelection(2);

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

		for (int i = 0; i < mSetChecked.length; i++) {
			mSetChecked[i] = false;
		}
		mSelectedFormat = -1;
		for (int i = 0; i < mRarityChecked.length; i++) {
			mRarityChecked[i] = false;
		}
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
			Toast.makeText(this.getActivity(), R.string.search_toast_cannot_save, Toast.LENGTH_LONG).show();
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
			String[] type = criteria.type.split(" - ");
			if (type.length > 0 && type[0] != null) {
				mSupertypeField.setText(type[0]);
			}
			if (type.length > 1 && type[1] != null) {
				mSubtypeField.setText(type[1]);
			}
			mTextField.setText(criteria.text);
			mArtistField.setText(criteria.artist);
			mFlavorField.setText(criteria.flavor);

			mCheckboxW.setChecked(criteria.color.contains("W"));
			mCheckboxU.setChecked(criteria.color.contains("U"));
			mCheckboxB.setChecked(criteria.color.contains("B"));
			mCheckboxR.setChecked(criteria.color.contains("R"));
			mCheckboxG.setChecked(criteria.color.contains("G"));
			mCheckboxL.setChecked(criteria.color.contains("L"));
			mColorSpinner.setSelection(criteria.colorLogic);

			mTextSpinner.setSelection(criteria.textLogic);
			mTypeSpinner.setSelection(criteria.typeLogic);
			mSetSpinner.setSelection(criteria.setLogic);

			List<String> logicChoices = Arrays.asList(getResources().getStringArray(R.array.logic_spinner));
			mPowLogic.setSelection(logicChoices.indexOf(criteria.powLogic));
			List<String> ptList = Arrays.asList(getResources().getStringArray(R.array.pt_spinner));
			float p = criteria.powChoice;
			if (p != CardDbAdapter.NOONECARES) {
				if (p == CardDbAdapter.STAR)
					mPowChoice.setSelection(ptList.indexOf("*"));
				else if (p == CardDbAdapter.ONEPLUSSTAR)
					mPowChoice.setSelection(ptList.indexOf("1+*"));
				else if (p == CardDbAdapter.TWOPLUSSTAR)
					mPowChoice.setSelection(ptList.indexOf("2+*"));
				else if (p == CardDbAdapter.SEVENMINUSSTAR)
					mPowChoice.setSelection(ptList.indexOf("7-*"));
				else if (p == CardDbAdapter.STARSQUARED)
					mPowChoice.setSelection(ptList.indexOf("*^2"));
				else {
					if (p == (int) p) {
						mPowChoice.setSelection(ptList.indexOf(((int) p) + ""));
					}
					else {
						mPowChoice.setSelection(ptList.indexOf(p + ""));
					}
				}
			}
			mTouLogic.setSelection(logicChoices.indexOf(criteria.touLogic));
			float t = criteria.touChoice;
			if (t != CardDbAdapter.NOONECARES) {
				if (t == CardDbAdapter.STAR)
					mTouChoice.setSelection(ptList.indexOf("*"));
				else if (t == CardDbAdapter.ONEPLUSSTAR)
					mTouChoice.setSelection(ptList.indexOf("1+*"));
				else if (t == CardDbAdapter.TWOPLUSSTAR)
					mTouChoice.setSelection(ptList.indexOf("2+*"));
				else if (t == CardDbAdapter.SEVENMINUSSTAR)
					mTouChoice.setSelection(ptList.indexOf("7-*"));
				else if (t == CardDbAdapter.STARSQUARED)
					mTouChoice.setSelection(ptList.indexOf("*^2"));
				else {
					if (t == (int) t) {
						mTouChoice.setSelection(ptList.indexOf(((int) t) + ""));
					}
					else {
						mTouChoice.setSelection(ptList.indexOf(t + ""));
					}
				}
			}
			mCmcLogic.setSelection(logicChoices.indexOf(criteria.cmcLogic));
			mCmcChoice.setSelection(Arrays.asList(getResources().getStringArray(R.array.cmc_spinner))
					.indexOf(String.valueOf(criteria.cmc)));

			if (criteria.set != null) {
				List<String> sets = Arrays.asList(criteria.set.split("-"));
				for (int i = 0; i < mSetChecked.length; i++)
					mSetChecked[i] = sets.contains(mSetSymbols[i]);
			}
			else
				for (int i = 0; i < mSetChecked.length; i++)
					mSetChecked[i] = false;

			mSelectedFormat = Arrays.asList(mFormatNames).indexOf(criteria.format);
			for (int i = 0; i < mRarityChecked.length; i++) {
				mRarityChecked[i] = (criteria.rarity != null && criteria.rarity
						.contains(mRarityNames[i].charAt(0) + ""));
			}

			this.removeDialog(getFragmentManager());
			checkDialogButtonColors();

		} catch (IOException e) {
			Toast.makeText(this.getActivity(), R.string.search_toast_cannot_load, Toast.LENGTH_LONG).show();
		} catch (ClassNotFoundException e) {
			Toast.makeText(this.getActivity(), R.string.search_toast_cannot_load, Toast.LENGTH_LONG).show();
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
	private void checkDialogButtonColors() {

		if (mSetNames == null || mSetChecked == null || mSetSymbols == null || mFormatNames == null ||
				mRarityNames == null || mRarityChecked == null || !isAdded()) {
			return;
		}

		mSetButton.setTextColor(getResources().getColor(R.color.black));
		for (boolean aSetChecked : mSetChecked) {
			if (aSetChecked) {
				mSetButton.setTextColor(getResources().getColor(R.color.holo_blue_dark));
			}
		}
		mFormatButton.setTextColor(getResources().getColor(R.color.black));
		if (mSelectedFormat != -1) {
			mFormatButton.setTextColor(getResources().getColor(R.color.holo_blue_dark));
		}
		mRarityButton.setTextColor(getResources().getColor(R.color.black));
		for (boolean aRarityChecked : mRarityChecked) {
			if (aRarityChecked) {
				mRarityButton.setTextColor(getResources().getColor(R.color.holo_blue_dark));
			}
		}
	}

	/**
	 * This will remove any currently showing dialogs and display the one given by id
	 * Usually the dialogs are created here, but in this case they were created in onCreate, because of the db calls,
	 * and that they are recreated in order to clear them
	 *
	 * @param id the id of the dialog to be shown
	 */
	void showDialog(final int id) {
		removeDialog(getFragmentManager());

		/* Create and show the dialog. */
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				super.onDismiss(dialog);
				checkDialogButtonColors();
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				super.onCreateDialog(savedInstanceState);

				/* This will be set to false if we are returning a null dialog. It prevents a crash */
				setShowsDialog(true);

				DialogInterface.OnMultiChoiceClickListener multiChoiceClickListener =
						new DialogInterface.OnMultiChoiceClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i, boolean b) {

							}
						};
				DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {

					}
				};
				try {
					/* Build the dialogs to display format, rarity, and set choices. The arrays were already filled in
					   onCreate() */
					switch (id) {
						case SET_LIST: {
							mSetDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_sets)
									.setMultiChoiceItems(mSetNames, mSetChecked, multiChoiceClickListener)
									.setPositiveButton(R.string.dialog_ok, clickListener).create();
							return mSetDialog;
						}
						case FORMAT_LIST: {
							mFormatDialog = new AlertDialog.Builder(this.getActivity()).
									setTitle(R.string.search_formats).setSingleChoiceItems(mFormatNames,
									mSelectedFormat, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									mSelectedFormat = which;
								}
							}).setPositiveButton(R.string.dialog_ok, clickListener).create();
							return mFormatDialog;
						}
						case RARITY_LIST: {
							mRarityDialog = new AlertDialog.Builder(this.getActivity())
									.setTitle(R.string.search_rarities).setMultiChoiceItems(mRarityNames,
											mRarityChecked, multiChoiceClickListener)
									.setPositiveButton(R.string.dialog_ok, clickListener).create();
							return mRarityDialog;
						}
						default: {
							setShowsDialog(false);
							return null;
						}
					}
				} catch (NullPointerException e) {
					/* if the db failed to open, these arrays will be null. */
					setShowsDialog(false);
					handleFamiliarDbException(false);
					return null;
				}
			}
		};
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
}
