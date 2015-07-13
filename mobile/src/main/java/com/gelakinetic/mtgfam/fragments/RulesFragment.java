package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Build;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RulesFragment extends FamiliarFragment {

    /* Keys for information in the bundle */
    private static final String CATEGORY_KEY = "category";
    private static final String SUBCATEGORY_KEY = "subcategory";
    private static final String POSITION_KEY = "position";
    private static final String KEYWORD_KEY = "keyword";
    private static final String GLOSSARY_KEY = "glossary";
    private static final String BANNED_KEY = "banned";
    private static final String FORMAT_KEY = "format";

    /* Keys for BannedItems */
    private static final int BANNED = 1;
    private static final int RESTRICTED = 2;
    private static final int NONE = -1;
    private static final int SETS = -2;

    /* Dialog constant */
    private static final int DIALOG_SEARCH = 1;

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
        final String format;

		/* Open a database connection */
        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);

		/* Inflate the view */
        View myFragmentView = inflater.inflate(R.layout.result_list_frag, container, false);
        assert myFragmentView != null;

		/* Get arguments to display a rules section, or use defaults */
        Bundle extras = getArguments();
        int position;
        boolean isGlossary;
        final boolean isBanned;
        if (extras == null) {
            mCategory = -1;
            mSubcategory = -1;
            position = 0;
            keyword = null;
            format = null;
            isGlossary = false;
            isBanned = false;
        } else {
            mCategory = extras.getInt(CATEGORY_KEY, -1);
            mSubcategory = extras.getInt(SUBCATEGORY_KEY, -1);
            position = extras.getInt(POSITION_KEY, 0);
            keyword = extras.getString(KEYWORD_KEY);
            format = extras.getString(FORMAT_KEY);
            isGlossary = extras.getBoolean(GLOSSARY_KEY, false);
            isBanned = extras.getBoolean(BANNED_KEY, false);
        }

        ListView list = (ListView) myFragmentView.findViewById(R.id.result_list);
        mRules = new ArrayList<>();
        boolean isClickable;
        Cursor cursor;

        Cursor setsCursor;
        setsCursor = null;

		/* Sub-optimal, but KitKat is silly */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            list.setOnScrollListener(new ListView.OnScrollListener() {

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
        }

		/* Populate the cursor with information from the database */
        try {
            if (isGlossary) {
                cursor = CardDbAdapter.getGlossaryTerms(database);
                isClickable = false;
            } else if (isBanned && format != null) {
                cursor = CardDbAdapter.getBannedCards(database, format);
                setsCursor = CardDbAdapter.getLegalSets(database, format);
                isClickable = false;
            } else if (isBanned && format == null) {
                cursor = CardDbAdapter.fetchAllFormats(database);
                isClickable = true;
            } else if (keyword == null) {
                cursor = CardDbAdapter.getRules(mCategory, mSubcategory, database);
                isClickable = mSubcategory == -1;
            } else {
                cursor = CardDbAdapter.getRulesByKeyword(keyword, mCategory, mSubcategory, database);
                isClickable = false;
            }
        } catch (FamiliarDbException e) {
            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
            handleFamiliarDbException(true);
            return myFragmentView;
        }

		/* Add DisplayItems to mRules */
        if (setsCursor != null) {
            try {
                if (setsCursor.getCount() > 0) {
                    setsCursor.moveToFirst();
                    mRules.add(new BannedItem(
                            format,
                            SETS,
                            setsCursor.getString(setsCursor.getColumnIndex(CardDbAdapter.KEY_LEGAL_SETS)), false));
                }
                setsCursor.close();
            } catch (SQLiteDatabaseCorruptException e) {
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                handleFamiliarDbException(true);
                return null;
            }
            if (cursor.getCount() == 0) { // Adapter will not be set when cursor has count 0
                int listItemResource = R.layout.rules_list_detail_item;
                RulesListAdapter adapter = new RulesListAdapter(getActivity(), listItemResource, mRules);
                list.setAdapter(adapter);
            }
        }
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        if (isGlossary) {
                            mRules.add(new GlossaryItem(
                                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_TERM)),
                                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_DEFINITION)), false));
                        } else if (isBanned && format != null) {
                            mRules.add(new BannedItem(
                                    format,
                                    cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_LEGALITY)),
                                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_BANNED_LIST)), false));
                        } else if (isBanned && format == null) {
                            mRules.add(new BannedItem(
                                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME)),
                                    NONE, "", true));
                        } else {
                            mRules.add(new RuleItem(
                                    cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_CATEGORY)),
                                    cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_SUBCATEGORY)),
                                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_ENTRY)),
                                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_RULE_TEXT))));
                        }
                        cursor.moveToNext();
                    }
                    cursor.close();
                    if (!isGlossary && !isBanned && mCategory == -1 && keyword == null) {
                        /* If it's the initial rules page, add a Glossary link to the end*/
                        mRules.add(new GlossaryItem(getString(R.string.rules_glossary), "", true));
                        mRules.add(new BannedItem(getString(R.string.rules_banned_and_restricted), NONE, "", true));
                    }
                    int listItemResource = R.layout.rules_list_item;
                    /* These cases can't be exclusive; otherwise keyword search from anything but a subcategory will use
                     * the wrong layout*/
                    if (isGlossary || isBanned || mSubcategory >= 0 || keyword != null) {
                        listItemResource = R.layout.rules_list_detail_item;
                    }
                    RulesListAdapter adapter = new RulesListAdapter(getActivity(), listItemResource, mRules);
                    list.setAdapter(adapter);

                    if (isClickable) {
                        /* This only happens for rule mItems with no subcategory, so the cast, should be safe */
                        list.setOnItemClickListener(new OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                DisplayItem item = mRules.get(position);
                                Bundle args = new Bundle();
                                if (item instanceof RuleItem) {
                                    args.putInt(CATEGORY_KEY, ((RuleItem) item).mCategory);
                                    args.putInt(SUBCATEGORY_KEY, ((RuleItem) item).mSubcategory);
                                } else if (item instanceof GlossaryItem) {
                                    args.putBoolean(GLOSSARY_KEY, true);
                                } else if (item instanceof BannedItem) {
                                    args.putBoolean(BANNED_KEY, true);
                                    if (isBanned && format == null) {
                                        args.putString(FORMAT_KEY, item.getHeader());
                                    }
                                }
                                RulesFragment frag = new RulesFragment();
                                startNewFragment(frag, args);
                            }
                        });
                    }
                    list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                            DisplayItem item = mRules.get(position);
                            if (item instanceof RuleItem) {
                                // Gets a handle to the clipboard service.
                                ClipboardManager clipboard = (ClipboardManager)
                                        getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                // Creates a new text clip to put on the clipboard
                                ClipData clip = ClipData.newPlainText(getString(R.string.rules_copy_tag), item.getHeader() + ": " + item.getText());
                                // Set the clipboard's primary clip.
                                clipboard.setPrimaryClip(clip);
                                // Alert the user
                                ToastWrapper.makeText(getActivity(), R.string.rules_coppied, ToastWrapper.LENGTH_SHORT).show();
                            }
                            return true;
                        }
                    });
                } else {
					/* Cursor had a size of 0, boring */
                    cursor.close();
                    if (!isBanned) {
                        ToastWrapper.makeText(getActivity(), R.string.rules_no_results_toast, ToastWrapper.LENGTH_SHORT).show();
                        getFragmentManager().popBackStack();
                    }
                }
            } catch (SQLiteDatabaseCorruptException e) {
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                handleFamiliarDbException(true);
                return null;
            }
        } else {
            if (!isBanned) { /* Cursor is null. weird. */
                ToastWrapper.makeText(getActivity(), R.string.rules_no_results_toast, ToastWrapper.LENGTH_SHORT).show();
                getFragmentManager().popBackStack();
            }
        }

        list.setSelection(position);

		/* Explanations for these regular expressions are available upon request. - Alex */
        mUnderscorePattern = Pattern.compile("_(.+?)_");
        mExamplePattern = Pattern.compile("(Example:.+)$");
        mGlyphPattern = Pattern.compile("\\{([a-zA-Z0-9/]{1,3})\\}");
        if (keyword != null && !keyword.contains("{") && !keyword.contains("}")) {
            mKeywordPattern = Pattern.compile("(" + Pattern.quote(keyword) + ")", Pattern.CASE_INSENSITIVE);
        } else {
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
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

        return myFragmentView;
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

            @NotNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                searchArgs = null;
                switch (RulesFragment.DIALOG_SEARCH) {
                    case DIALOG_SEARCH: {
						/* Inflate a view to type in the player's name, and show it in an AlertDialog */
                        View textEntryView = getActivity().getLayoutInflater().inflate(R.layout.alert_dialog_text_entry,
                                null, false);
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
                        } else {
                            SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                            try {
                                title = String.format(getString(R.string.rules_search_cat),
                                        CardDbAdapter.getCategoryName(mCategory, mSubcategory, database));
                            } catch (FamiliarDbException e) {
                                title = String.format(getString(R.string.rules_search_cat),
                                        getString(R.string.rules_this_cat));
                            }
                            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                        }

                        Dialog dialog = new AlertDialogPro.Builder(getActivity())
                                .setTitle(title)
                                .setView(textEntryView)
                                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        if (nameInput.getText() == null) {
                                            dialog.dismiss();
                                            return;
                                        }
                                        String keyword = nameInput.getText().toString();
                                        if (keyword.length() < 3) {
                                            ToastWrapper.makeText(getActivity(),
                                                    R.string.rules_short_key_toast, ToastWrapper.LENGTH_LONG).show();
                                        } else {
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
                        savedInstanceState.putInt("id", RulesFragment.DIALOG_SEARCH);
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
                for (int i = 0; i < getFragmentManager().getBackStackEntryCount(); ++i) {
                    getFragmentManager().popBackStack();
                }
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
        showDialog();
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
     * TODO is input.length() == 0? That makes SpannableString unhappy
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
            encodedInput = mKeywordPattern.matcher(encodedInput)
                    .replaceAll("\\<font color=\"" +
                            String.format("0x%06X", 0xFFFFFF & getResources().getColor(R.color.colorPrimaryDark_light)) +
                            "\"\\>$1\\</font\\>");
        }
        encodedInput = mHyperlinkPattern.matcher(encodedInput).replaceAll("\\<a href=\"http://$2$3\"\\>$2$3\\</a\\>");
        encodedInput = encodedInput.replace("{", "").replace("}", "");

        CharSequence cs = ImageGetterHelper
                .formatStringWithGlyphs(encodedInput, ImageGetterHelper.GlyphGetter(getActivity()));
        SpannableString result = new SpannableString(cs);

        if (shouldLink) {
            Matcher m = mLinkPattern.matcher(cs);
            while (m.find()) {
                SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
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
                        position = CardDbAdapter.getRulePosition(linkCat, linkSub, entry, database);
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
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
            }
        }
        return result;
    }

    /**
     * @param menu     The options menu in which you place your mItems.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.rules_menu, menu);
    }

    /**
     * This is an abstract class which can be displayed with a RulesListAdapter in the fragment
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
     * This is a rule to be displayed in the list view
     */
    private class RuleItem extends DisplayItem {
        private final int mCategory;
        private final int mSubcategory;
        private final String mEntry;
        private final String mRulesText;

        /**
         * Constructor
         *
         * @param category    The integer category of the rule
         * @param subcategory The integer subcategory of the rule
         * @param entry       The letter entry of the rule.
         * @param rulesText   The rule. Follow it!
         */
        public RuleItem(int category, int subcategory, String entry, String rulesText) {
            this.mCategory = category;
            this.mSubcategory = subcategory;
            this.mEntry = entry;
            this.mRulesText = rulesText;
        }

        /**
         * Returns the rules text to be shown in the list
         *
         * @return The rules text
         */
        public String getText() {
            return this.mRulesText;
        }

        /**
         * Returns the header to be shown in the list
         *
         * @return Some concatenation of the category, subcategory, and entry, depending on what is valid
         */
        public String getHeader() {
            if (this.mSubcategory == -1) {
                return String.valueOf(this.mCategory) + ".";
            } else if (this.mEntry == null) {
                return String.valueOf((this.mCategory * 100) + this.mSubcategory) + ".";
            } else {
                return String.valueOf((this.mCategory * 100 + this.mSubcategory)) + "." + this.mEntry;
            }
        }

        /**
         * This entry is clickable if it does not have rules text, i.e. it is just a header which will open a
         * sub-fragment
         *
         * @return True if it launches a sub-fragment, false otherwise
         */
        public boolean isClickable() {
            return this.mEntry == null || this.mEntry.length() == 0;
        }
    }

    /**
     * This is a glossary item to be displayed in the list view
     */
    private class GlossaryItem extends DisplayItem {
        private final String mTerm;
        private final String mDefinition;
        private final boolean mClickable;

        /**
         * Constructor
         *
         * @param term       The term to be defined
         * @param definition The definition of the term
         * @param clickable  Whether clicking this entry will start a sub-fragment. In practice, just the main glossary
         *                   entry point
         */
        public GlossaryItem(String term, String definition, boolean clickable) {
            this.mTerm = term;
            this.mDefinition = definition;
            this.mClickable = clickable;
        }

        /**
         * @return the definition for this glossary entry
         */
        public String getText() {
            return this.mDefinition;
        }

        /**
         * @return the header for this glossary entry
         */
        public String getHeader() {
            return this.mTerm;
        }

        /**
         * @return whether clicking this entry launches a sub fragment
         */
        public boolean isClickable() {
            return this.mClickable;
        }
    }

    private class BannedItem extends DisplayItem {
        private final String mFormat;
        private final String mLegality;
        private final String mCards;
        private final boolean mClickable;

        /**
         * Constructor
         *
         * @param format    The format
         * @param legality  The legality of the cards
         * @param cards     Banned and restricted cards in the format
         * @param clickable Whether clicking on this entry will start a sub-fragment. Main Banned entry point
         */
        public BannedItem(String format, int legality, String cards, boolean clickable) {

            this.mFormat = format;
            if (format.equalsIgnoreCase("Commander")) {
                if (legality == RESTRICTED) {
                    mLegality = getString(R.string.rules_banned_as_commander);
                } else if (legality == BANNED) {
                    mLegality = getString(R.string.rules_banned);
                } else if (legality == NONE) {
                    mLegality = "";
                } else if (legality == SETS) {
                    mLegality = getString(R.string.rules_legal_sets);
                } else {
                    mLegality = "";
                }
            } else {
                if (legality == BANNED) {
                    mLegality = getString(R.string.rules_banned);
                } else if (legality == RESTRICTED) {
                    mLegality = getString(R.string.rules_restricted);

                } else if (legality == NONE) {
                    mLegality = "";
                } else if (legality == SETS) {
                    mLegality = getString(R.string.rules_legal_sets);
                } else {
                    mLegality = "";
                }
            }
            if (cards == null) {
                if (legality == SETS) {
                    this.mCards = getString(R.string.rules_bb_wb_sets);
                } else {
                    this.mCards = getString(R.string.rules_no_cards);
                }
            } else {
                this.mCards = cards;
            }
            this.mClickable = clickable;
        }

        /**
         * @return the list of banned and restricted cards
         */
        public String getText() {
            return this.mCards;
        }

        /**
         * @return the format and legality
         */
        public String getHeader() {
            if (mClickable) {
                // it is the initial rules fragment
                return this.mFormat;
            } else {
                return this.mLegality;
            }
        }

        /**
         * @return whether clicking this entry launches a sub-fragment
         */
        public boolean isClickable() {
            return this.mClickable;
        }
    }

    /**
     * This class displays rules items in the list view. It also enables the fast scrolling with the alphabet
     */
    private class RulesListAdapter extends ArrayAdapter<DisplayItem> implements SectionIndexer {
        private final int mLayoutResourceId;
        private final ArrayList<DisplayItem> mItems;

        private Integer mIndices[];
        private String mAlphabet[];

        /**
         * Constructor
         *
         * @param context            A context to inflate views with
         * @param textViewResourceId The layout to inflate, either R.layout.rules_list_detail_item or
         *                           R.layout.rules_list_item
         * @param items              The DisplayItems to show
         */
        public RulesListAdapter(Context context, int textViewResourceId, ArrayList<DisplayItem> items) {
            super(context, textViewResourceId, items);

            this.mLayoutResourceId = textViewResourceId;
            this.mItems = items;

            boolean isGlossary = true;
            for (DisplayItem item : items) {
                if (RuleItem.class.isInstance(item) || BannedItem.class.isInstance(item)) {
                    isGlossary = false;
                    break;
                }
            }

			/* Enable fast scrolling for the glossary. Add all the first letters of the entries */
            if (isGlossary) {
                LinkedHashSet<Integer> indicesLHS = new LinkedHashSet<>();
                LinkedHashSet<String> alphabetLHS = new LinkedHashSet<>();

				/* Find the first index for each letter in the alphabet by looking at all the items */
                for (int index = 0; index < items.size(); index++) {
                    String letter = items.get(index).getHeader().substring(0, 1).toUpperCase();
                    if (!alphabetLHS.contains(letter)) {
                        alphabetLHS.add(letter);
                        indicesLHS.add(index);
                    }
                }
                mAlphabet = new String[alphabetLHS.size()];
                mAlphabet = alphabetLHS.toArray(mAlphabet);
                mIndices = new Integer[indicesLHS.size()];
                mIndices = indicesLHS.toArray(mIndices);
            } else {
                this.mIndices = null;
                this.mAlphabet = null;
            }
        }

        /**
         * Get a view for a given item's position
         *
         * @param position    The position of the item to display
         * @param convertView A view. If not null, it should be updated with this position's info
         * @param parent      The parent this view will eventually be attached to
         * @return The view to display
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inf = getActivity().getLayoutInflater();
                v = inf.inflate(mLayoutResourceId, null);
            }
            assert v != null;
            DisplayItem data = mItems.get(position);
            if (data != null) {
                TextView rulesHeader = (TextView) v.findViewById(R.id.rules_item_header);
                TextView rulesText = (TextView) v.findViewById(R.id.rules_item_text);

                String header = data.getHeader();
                String text = data.getText();

                rulesHeader.setText(formatText(header, false), BufferType.SPANNABLE);
                if (text.equals("")) {
                    rulesText.setVisibility(View.GONE);
                } else {
                    boolean shouldLink = true;
                    if (data instanceof BannedItem) {
                        shouldLink = false;
                    }
                    rulesText.setVisibility(View.VISIBLE);
                    rulesText.setText(formatText(text, shouldLink), BufferType.SPANNABLE);
                }
                if (!data.isClickable()) {
                    rulesText.setMovementMethod(LinkMovementMethod.getInstance());
                    rulesText.setClickable(false);
                }
            }
            return v;
        }

        /**
         * Given the index of a section within the array of section objects, returns the starting position of that
         * section within the adapter. If the section's starting position is outside of the adapter bounds, the position
         * must be clipped to fall within the size of the adapter.
         *
         * @param section the index of the section within the array of section objects
         * @return the starting position of that section within the adapter, constrained to fall within the adapter
         * bounds
         */
        public int getPositionForSection(int section) {
            if (this.mIndices == null) {
                return 0;
            } else {
                return mIndices[section];
            }
        }

        /**
         * Given a position within the adapter, returns the index of the corresponding section within the array of
         * section objects. If the section index is outside of the section array bounds, the index must be clipped to
         * fall within the size of the section array. For example, consider an indexer where the section at array index
         * 0 starts at adapter position 100. Calling this method with position 10, which is before the first section,
         * must return index 0.
         *
         * @param position the position within the adapter for which to return the corresponding section index
         * @return the index of the corresponding section within the array of section objects, constrained to fall
         * within the array bounds
         */
        public int getSectionForPosition(int position) {
            if (this.mIndices == null) {
                return 0;
            } else {
                return 1;
            }
        }

        /**
         * Returns an array of objects representing sections of the list. The returned array and its contents should be
         * non-null. The list view will call toString() on the objects to get the preview text to display while
         * scrolling. For example, an adapter may return an array of Strings representing letters of the alphabet. Or,
         * it may return an array of objects whose toString() methods return their section titles.
         *
         * @return the array of section objects
         */
        public Object[] getSections() {
            if (this.mIndices == null) {
                return null;
            } else {
                return mAlphabet;
            }
        }
    }
}
