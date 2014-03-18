package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RulesFragment extends FamiliarFragment {

	/* Keys for information in the bundle */
	private static final String CATEGORY_KEY = "category";
	private static final String SUBCATEGORY_KEY = "subcategory";
	private static final String POSITION_KEY = "position";
	private static final String KEYWORD_KEY = "keyword";
	private static final String GLOSSARY_KEY = "glossary";

	/* Dialog constant */
	private static final int DIALOG_SEARCH = 1;

	/* Result code, to close multiple fragments */
	private static final int RESULT_QUIT_TO_MAIN = 2;

	/* Current rules information */
	private ArrayList<DisplayItem> mRules;
	private int mCategory;
	private int mSubcategory;

	/* Regular expression patterns */
	private Pattern mUnderscorePattern;
	private Pattern mExamplePattern;
	private Pattern mGlyphPattern;
	private Pattern mKeywordPattern;
	private Pattern mHyperlinkPattern;
	private Pattern mLinkPattern;

	/**
	 * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
	 * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
	 *                           fragment should not add the view itself, but this can be used to generate the
	 *                           LayoutParams of the view.
	 * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
	 *                           here.
	 * @return The view to be shown
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		String keyword;

		/* Open a database connection */
		CardDbAdapter mDbHelper;
		try {
			mDbHelper = new CardDbAdapter(getActivity());
		} catch (FamiliarDbException e) {
			handleFamiliarDbException(true);
			return null;
		}

		/* Inflate the view */
		View myFragmentView = inflater.inflate(R.layout.result_list_frag, container, false);
		assert myFragmentView != null;

		/* Check if we are returning to the root */
		Bundle res = ((FamiliarActivity) getActivity()).getFragmentResults();
		if (res != null) {
			int resultCode = res.getInt("resultCode");
			if (resultCode == RESULT_QUIT_TO_MAIN) {
				Bundle result = new Bundle();
				result.putInt("resultCode", RESULT_QUIT_TO_MAIN);
				((FamiliarActivity) getActivity()).setFragmentResult(result);
				getFragmentManager().popBackStack();
			}
		}

		/* Get arguments to display a rules section, or use defaults */
		Bundle extras = getArguments();
		int position;
		boolean isGlossary;
		if (extras == null) {
			mCategory = -1;
			mSubcategory = -1;
			position = 0;
			keyword = null;
			isGlossary = false;
		}
		else {
			mCategory = extras.getInt(CATEGORY_KEY, -1);
			mSubcategory = extras.getInt(SUBCATEGORY_KEY, -1);
			position = extras.getInt(POSITION_KEY, 0);
			keyword = extras.getString(KEYWORD_KEY);
			isGlossary = extras.getBoolean(GLOSSARY_KEY, false);
		}

		ListView list = (ListView) myFragmentView.findViewById(R.id.resultList);
		mRules = new ArrayList<DisplayItem>();
		boolean isClickable;
		Cursor cursor;

		/* Populate the cursor with information from the database */
		try {
			if (isGlossary) {
				cursor = mDbHelper.getGlossaryTerms();
				isClickable = false;
			}
			else if (keyword == null) {
				cursor = mDbHelper.getRules(mCategory, mSubcategory);
				isClickable = mSubcategory == -1;
			}
			else {
				cursor = mDbHelper.getRulesByKeyword(keyword, mCategory, mSubcategory);
				isClickable = false;
			}
		} catch (FamiliarDbException e) {
			handleFamiliarDbException(true);
			return myFragmentView;
		}

		/* Add DisplayItems to mRules */
		if (cursor != null) {
			try {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					while (!cursor.isAfterLast()) {
						if (isGlossary) {
							mRules.add(new GlossaryItem(
									cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_TERM)),
									cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_DEFINITION))));
						}
						else {
							mRules.add(new RuleItem(
									cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_CATEGORY)),
									cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_SUBCATEGORY)),
									cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_ENTRY)),
									cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_RULE_TEXT))));
						}
						cursor.moveToNext();
					}
					cursor.close();
					if (!isGlossary && mCategory == -1 && keyword == null) {
						/* If it's the initial rules page, add a Glossary link to the end*/
						mRules.add(new GlossaryItem(getString(R.string.rules_glossary)));
					}
					int listItemResource = R.layout.rules_list_item;
					/* These cases can't be exclusive; otherwise keyword search from anything but a subcategory will use
					 * the wrong layout*/
					if (isGlossary || mSubcategory >= 0 || keyword != null) {
						listItemResource = R.layout.rules_list_detail_item;
					}
					RulesListAdapter adapter = new RulesListAdapter(getActivity(), listItemResource, mRules);
					list.setAdapter(adapter);

					if (isClickable) {
						/* This only happens for rule items with no subcategory, so the cast, should be safe */
						list.setOnItemClickListener(new OnItemClickListener() {
							public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
								DisplayItem item = mRules.get(position);
								Bundle args = new Bundle();
								if (item instanceof RuleItem) {
									args.putInt(CATEGORY_KEY, ((RuleItem) item).getCategory());
									args.putInt(SUBCATEGORY_KEY, ((RuleItem) item).getSubcategory());
								}
								else if (item instanceof GlossaryItem) {
									args.putBoolean(GLOSSARY_KEY, true);
								}
								RulesFragment frag = new RulesFragment();
								startNewFragment(frag, args);
							}
						});
					}
				}
				else {
					/* Cursor had a size of 0, boring */
					cursor.close();
					Toast.makeText(getActivity(), R.string.rules_no_results_toast, Toast.LENGTH_SHORT).show();
					getFragmentManager().popBackStack();
				}
			} catch (SQLiteDatabaseCorruptException e) {
				handleFamiliarDbException(true);
				return null;
			}
		}
		else {
			/* Cursor is null, weird */
			Toast.makeText(getActivity(), R.string.rules_no_results_toast, Toast.LENGTH_SHORT).show();
			getFragmentManager().popBackStack();
		}

		list.setSelection(position);

		/* Explanations for these regular expressions are available upon request. - Alex */
		mUnderscorePattern = Pattern.compile("_(.+?)_");
		mExamplePattern = Pattern.compile("(Example:.+)$");
		mGlyphPattern = Pattern.compile("\\{([a-zA-Z0-9/]{1,3})\\}");
		if (keyword != null && !keyword.contains("{") && !keyword.contains("}")) {
			mKeywordPattern = Pattern.compile("(" + Pattern.quote(keyword) + ")", Pattern.CASE_INSENSITIVE);
		}
		else {
			mKeywordPattern = null;
		}
		mHyperlinkPattern = Pattern.compile("<(http://)?(www|gatherer|mtgcommander)(.+?)>");

		/*
		 * Regex breakdown for Adam:
		 * [1-9]: first character is between 1 and 9
		 * [0-9]{2}: followed by two characters between 0 and 9 (i.e. a 3-digit number)
		 * (...)?: maybe followed by the group:
		 * \\.: period
		 * ([a-z0-9]{1,3}(-[a-z]{1})?)?: maybe followed by one to three alphanumeric
		 * characters, which are maybe followed by a hyphen and an alphabetical
		 * character \\.?: maybe followed by another period
		 *
		 * I realize this isn't completely easy to read, but it might at least help
		 * make some sense of the regex so I'm not just waving my hands and shouting
		 * "WIZARDS!". I still reserve the right to do that, though. - Alex
		 */
		mLinkPattern = Pattern.compile("([1-9][0-9]{2}(\\.([a-z0-9]{1,4}(-[a-z])?)?\\.?)?)");

		if (cursor != null) {
			cursor.close();
		}
		mDbHelper.close();

		return myFragmentView;
	}

	/**
	 * Remove any showing dialogs, and show the requested one
	 *
	 * @param id the ID of the dialog to show
	 */
	@SuppressWarnings("SameParameterValue")
	void showDialog(final int id) {
		/* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
		currently showing dialog, so make our own transaction and take care of that here. */

		/* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
		if (!this.isVisible()) {
			return;
		}

		removeDialog(getFragmentManager());

		/* Create and show the dialog. */
		final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			private Bundle searchArgs = null;

			@Override
			public void onDestroy() {
				super.onDestroy();
				if (searchArgs != null) {
					RulesFragment frag = new RulesFragment();
					startNewFragment(frag, searchArgs);
				}
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				searchArgs = null;
				switch (id) {
					case DIALOG_SEARCH: {
						/* Inflate a view to type in the player's name, and show it in an AlertDialog */
						View textEntryView = getActivity().getLayoutInflater()
								.inflate(R.layout.alert_dialog_text_entry, null);
						assert textEntryView != null;
						final EditText nameInput = (EditText) textEntryView.findViewById(R.id.text_entry);
						textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								nameInput.setText("");
							}
						});

						String title;
						if (mCategory == -1) {
							title = getString(R.string.rules_search_all);
						}
						else {
							try {
								CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
								title = String.format(getString(R.string.rules_search_cat),
										mDbHelper.getCategoryName(mCategory, mSubcategory));
								mDbHelper.close();
							} catch (FamiliarDbException e) {
								title = String.format(getString(R.string.rules_search_cat),
										getString(R.string.rules_this_cat));
							}
						}

						Dialog dialog = new AlertDialog.Builder(getActivity())
								.setTitle(title)
								.setView(textEntryView)
								.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										if (nameInput.getText() == null) {
											dialog.dismiss();
											return;
										}
										String keyword = nameInput.getText().toString().trim();
										if (keyword.length() < 3) {
											Toast.makeText(getActivity(),
													R.string.rules_short_key_toast, Toast.LENGTH_LONG).show();
										}
										else {
											searchArgs = new Bundle();
											searchArgs.putString(KEYWORD_KEY, keyword);
											searchArgs.putInt(CATEGORY_KEY, mCategory);
											searchArgs.putInt(SUBCATEGORY_KEY, mSubcategory);
										}
									}
								})
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										dialog.dismiss();
									}
								})
								.create();
						dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
						return dialog;
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
	}

	/**
	 * Handle a click in the menu
	 *
	 * @param item The item clicked
	 * @return true if the click was handled, false otherwise
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.rules_menu_exit:
				Bundle result = new Bundle();
				result.putInt("resultCode", RESULT_QUIT_TO_MAIN);
				((FamiliarActivity) getActivity()).setFragmentResult(result);
				getFragmentManager().popBackStack();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Called when the user clicks the search key in the menu
	 *
	 * @return true, since the click was acted upon
	 */
	@Override
	public boolean onInterceptSearchKey() {
		showDialog(DIALOG_SEARCH);
		return true;
	}

	/**
	 * @return true, since this fragment can intercept the search key
	 */
	@Override
	public boolean canInterceptSearchKey() {
		return true;
	}

	/**
	 * Format text for an entry with glyphs and links to other entries, found using regular expressions
	 *
	 * @param input      The entry to format
	 * @param shouldLink true if links should be added, false otherwise
	 * @return a SpannableString with glyphs and links
	 */
	private SpannableString formatText(String input, boolean shouldLink) {
		String encodedInput = input;
		encodedInput = mUnderscorePattern.matcher(encodedInput).replaceAll("\\<i\\>$1\\</i\\>");
		encodedInput = mExamplePattern.matcher(encodedInput).replaceAll("\\<i\\>$1\\</i\\>");
		encodedInput = mGlyphPattern.matcher(encodedInput).replaceAll("\\<img src=\"$1\"/\\>");
		if (mKeywordPattern != null) {
			encodedInput = mKeywordPattern.matcher(encodedInput).replaceAll("\\<font color=\"yellow\"\\>$1\\</font\\>");
		}
		encodedInput = mHyperlinkPattern.matcher(encodedInput).replaceAll("\\<a href=\"http://$2$3\"\\>$2$3\\</a\\>");
		encodedInput = encodedInput.replace("{", "").replace("}", "");

		CharSequence cs = ImageGetterHelper
				.formatStringWithGlyphs(encodedInput, ImageGetterHelper.GlyphGetter(getResources()));
		SpannableString result = new SpannableString(cs);

		if (shouldLink) {
			Matcher m = mLinkPattern.matcher(cs);
			while (m.find()) {
				try {
					String[] tokens = cs.subSequence(m.start(), m.end()).toString().split("(\\.)");
					int firstInt = Integer.parseInt(tokens[0]);
					final int linkCat = firstInt / 100;
					final int linkSub = firstInt % 100;
					int position = 0;
					if (tokens.length > 1) {
						String entry = tokens[1];
						int dashIndex = entry.indexOf("-");
						if (dashIndex >= 0) {
							entry = entry.substring(0, dashIndex);
						}
						CardDbAdapter mDbHelper = new CardDbAdapter(getActivity());
						position = mDbHelper.getRulePosition(linkCat, linkSub, entry);
						mDbHelper.close();
					}
					final int linkPosition = position;
					result.setSpan(new ClickableSpan() {
						@Override
						public void onClick(View widget) {
							/* Open a new activity instance*/
							Bundle args = new Bundle();
							args.putInt(CATEGORY_KEY, linkCat);
							args.putInt(SUBCATEGORY_KEY, linkSub);
							args.putInt(POSITION_KEY, linkPosition);
							RulesFragment frag = new RulesFragment();
							startNewFragment(frag, args);
						}
					}, m.start(), m.end(), 0);
				} catch (Exception e) {
					/* Eat any exceptions; they'll just cause the link to not appear*/
				}
			}
		}
		return result;
	}

	/**
	 *
	 */
	private abstract class DisplayItem {
		/**
		 * @return The string text associated with this entry
		 */
		public abstract String getText();

		/**
		 * @return The string header associated with this entry
		 */
		public abstract String getHeader();

		/**
		 * @return True if clicking this entry opens a sub-fragment, false otherwise
		 */
		public abstract boolean isClickable();
	}

	/**
	 *
	 */
	private class RuleItem extends DisplayItem {
		private final int category;
		private final int subcategory;
		private final String entry;
		private final String rulesText;

		/**
		 * @param category
		 * @param subcategory
		 * @param entry
		 * @param rulesText
		 */
		public RuleItem(int category, int subcategory, String entry, String rulesText) {
			this.category = category;
			this.subcategory = subcategory;
			this.entry = entry;
			this.rulesText = rulesText;
		}

		/**
		 * @return
		 */
		public int getCategory() {
			return this.category;
		}

		/**
		 * @return
		 */
		public int getSubcategory() {
			return this.subcategory;
		}

		/**
		 * @return
		 */
		public String getText() {
			return this.rulesText;
		}

		/**
		 * @return
		 */
		public String getHeader() {
			if (this.subcategory == -1) {
				return String.valueOf(this.category) + ".";
			}
			else if (this.entry == null) {
				return String.valueOf((this.category * 100) + this.subcategory) + ".";
			}
			else {
				return String.valueOf((this.category * 100 + this.subcategory)) + "." + this.entry;
			}
		}

		/**
		 * @return
		 */
		public boolean isClickable() {
			return this.entry == null || this.entry.length() == 0;
		}
	}

	/**
	 *
	 */
	private class GlossaryItem extends DisplayItem {
		private final String term;
		private final String definition;
		private final boolean clickable;

		/**
		 * @param term
		 * @param definition
		 */
		public GlossaryItem(String term, String definition) {
			this.term = term;
			this.definition = definition;
			this.clickable = false;
		}

		/**
		 * @param term
		 */
		public GlossaryItem(String term) {
			this.term = term;
			this.definition = "";
			this.clickable = true;
		}

		/**
		 * @return
		 */
		public String getText() {
			return this.definition;
		}

		/**
		 * @return
		 */
		public String getHeader() {
			return this.term;
		}

		/**
		 * @return
		 */
		public boolean isClickable() {
			return this.clickable;
		}
	}

	/**
	 *
	 */
	private class RulesListAdapter extends ArrayAdapter<DisplayItem> implements SectionIndexer {
		private final int layoutResourceId;
		private final ArrayList<DisplayItem> items;
		private final HashMap<String, Integer> alphaIndex;
		private final String[] sections;

		/**
		 * @param context
		 * @param textViewResourceId
		 * @param items
		 */
		public RulesListAdapter(Context context, int textViewResourceId, ArrayList<DisplayItem> items) {
			super(context, textViewResourceId, items);

			this.layoutResourceId = textViewResourceId;
			this.items = items;

			boolean isGlossary = true;
			for (DisplayItem item : items) {
				if (RuleItem.class.isInstance(item)) {
					isGlossary = false;
					break;
				}
			}

			if (isGlossary) {
				this.alphaIndex = new HashMap<String, Integer>();
				for (int i = 0; i < items.size(); i++) {
					String first = items.get(i).getHeader().substring(0, 1).toUpperCase();
					if (!this.alphaIndex.containsKey(first)) {
						this.alphaIndex.put(first, i);
					}
				}

				ArrayList<String> letters = new ArrayList<String>(this.alphaIndex.keySet());
				Collections.sort(letters); /* This should do nothing in practice, but*/
				/* just to be safe*/

				sections = new String[letters.size()];
				letters.toArray(sections);
			}
			else {
				this.alphaIndex = null;
				this.sections = null;
			}
		}

		/**
		 * @param position
		 * @param convertView
		 * @param parent
		 * @return
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inf = getActivity().getLayoutInflater();
				v = inf.inflate(layoutResourceId, null);
			}
			assert v != null;
			DisplayItem data = items.get(position);
			if (data != null) {
				TextView rulesHeader = (TextView) v.findViewById(R.id.rules_item_header);
				TextView rulesText = (TextView) v.findViewById(R.id.rules_item_text);

				String header = data.getHeader();
				String text = data.getText();

				rulesHeader.setText(formatText(header, false), BufferType.SPANNABLE);
				if (text.equals("")) {
					rulesText.setVisibility(View.GONE);
				}
				else {
					rulesText.setVisibility(View.VISIBLE);
					rulesText.setText(formatText(text, true), BufferType.SPANNABLE);
				}
				if (!data.isClickable()) {
					rulesText.setMovementMethod(LinkMovementMethod.getInstance());
					rulesText.setClickable(false);
					rulesText.setLongClickable(false);
				}
			}
			return v;
		}

		/**
		 * @param section
		 * @return
		 */
		public int getPositionForSection(int section) {
			if (this.alphaIndex == null) {
				return 0;
			}
			else {
				return this.alphaIndex.get(this.sections[section]);
			}
		}

		/**
		 * @param position
		 * @return
		 */
		public int getSectionForPosition(int position) {
			if (this.alphaIndex == null) {
				return 0;
			}
			else {
				return 1;
			}
		}

		/**
		 * @return
		 */
		public Object[] getSections() {
			if (this.alphaIndex == null) {
				return null;
			}
			else {
				return sections;
			}
		}
	}

	/**
	 * @param menu     The options menu in which you place your items.
	 * @param inflater The inflater to use to inflate the menu
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.rules_menu, menu);
	}
}
