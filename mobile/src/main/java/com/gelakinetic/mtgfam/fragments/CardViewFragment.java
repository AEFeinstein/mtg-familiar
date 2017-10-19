/**
 * Copyright 2011 Adam Feinstein
 * <p/>
 * This file is part of MTG Familiar.
 * <p/>
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.fragments;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.Language;
import com.gelakinetic.mtgfam.BuildConfig;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.CardViewDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.helpers.ColorIndicatorView;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.lruCache.RecyclingBitmapDrawable;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class handles displaying card info.
 * WARNING! Because this fragment is nested in a CardViewPagerFragment, always get the parent
 * fragment's activity.
 */
public class CardViewFragment extends FamiliarFragment {

    /* Bundle constant */
    public static final String CARD_ID = "card_id";

    /* Where the card image is loaded to */
    public static final int MAIN_PAGE = 1;
    private static final int DIALOG = 2;
    private static final int SHARE = 3;
    /* Used to store the String when copying to clipboard */
    private String mCopyString;
    /* UI elements, to be filled in */
    private TextView mNameTextView;
    private TextView mCostTextView;
    private TextView mTypeTextView;
    private TextView mSetTextView;
    private TextView mAbilityTextView;
    private TextView mPowTouTextView;
    private TextView mFlavorTextView;
    private TextView mArtistTextView;
    private TextView mNumberTextView;
    private Button mTransformButton;
    private View mTransformButtonDivider;
    private ImageView mCardImageView;
    private ScrollView mTextScrollView;
    private ScrollView mImageScrollView;
    private LinearLayout mColorIndicatorLayout;

    /* the AsyncTask loads stuff off the UI thread, and stores whatever in these local variables */
    public AsyncTask mAsyncTask;
    public RecyclingBitmapDrawable mCardBitmap;
    public String[] mLegalities;
    public String[] mFormats;
    public ArrayList<Ruling> mRulingsArrayList;

    /* Loaded in a Spice Service */
    public PriceInfo mPriceInfo;

    /* Card info, used to build the URL to fetch the picture */
    private String mCardNumber;
    private String mSetCode;
    public String mCardName;
    private int mCardCMC;
    private String mMagicCardsInfoSetCode;
    public int mMultiverseId;
    private String mCardType;
    private String mSetName;

    /* Card info used to flip the card */
    private String mTransformCardNumber;
    private int mTransformId;

    /* To switch card between printings */
    public LinkedHashSet<String> mPrintings;
    public LinkedHashSet<Long> mCardIds;

    /* Easier than calling getActivity() all the time, and handles being nested */
    private FamiliarActivity mActivity;

    /* Foreign name translations */
    public final ArrayList<Card.ForeignPrinting> mTranslatedNames = new ArrayList<>();

    /**
     * Kill any AsyncTask if it is still running.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        /* Pass a non-null bundle to the ResultListFragment so it knows to exit if there was a list
         * of 1 card. If this wasn't launched by a ResultListFragment, it'll get eaten */
        Bundle args = new Bundle();
        if (mActivity != null) {
            mActivity.setFragmentResult(args);
        }
    }

    /**
     * Called when the Fragment is no longer resumed. Clear the loading bar just in case.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mActivity != null) {
            mActivity.clearLoading();
        }
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }
    }

    /**
     * Inflates the view and saves references to UI elements for later filling.
     *
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @param inflater           The LayoutInflater object that can be used to inflate any views in
     *                           the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should
     *                           be attached to. The fragment should not add the view itself, but
     *                           this can be used to generate the LayoutParams of the view.
     * @return The inflated view
     */
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        try {
            mActivity = ((FamiliarFragment) getParentFragment()).getFamiliarActivity();
        } catch (NullPointerException e) {
            mActivity = getFamiliarActivity();
        }

        View myFragmentView = inflater.inflate(R.layout.card_view_frag, container, false);

        assert myFragmentView != null; /* Because Android Studio */
        mNameTextView = myFragmentView.findViewById(R.id.name);
        mCostTextView = myFragmentView.findViewById(R.id.cost);
        mTypeTextView = myFragmentView.findViewById(R.id.type);
        mSetTextView = myFragmentView.findViewById(R.id.set);
        mAbilityTextView = myFragmentView.findViewById(R.id.ability);
        mFlavorTextView = myFragmentView.findViewById(R.id.flavor);
        mArtistTextView = myFragmentView.findViewById(R.id.artist);
        mNumberTextView = myFragmentView.findViewById(R.id.number);
        mPowTouTextView = myFragmentView.findViewById(R.id.pt);
        mTransformButtonDivider = myFragmentView.findViewById(R.id.transform_button_divider);
        mTransformButton = myFragmentView.findViewById(R.id.transformbutton);
        mTextScrollView = myFragmentView.findViewById(R.id.cardTextScrollView);
        mImageScrollView = myFragmentView.findViewById(R.id.cardImageScrollView);
        mCardImageView = myFragmentView.findViewById(R.id.cardpic);
        mColorIndicatorLayout =
                myFragmentView.findViewById(R.id.color_indicator_view);

        registerForContextMenu(mNameTextView);
        registerForContextMenu(mCostTextView);
        registerForContextMenu(mTypeTextView);
        registerForContextMenu(mSetTextView);
        registerForContextMenu(mAbilityTextView);
        registerForContextMenu(mPowTouTextView);
        registerForContextMenu(mFlavorTextView);
        registerForContextMenu(mArtistTextView);
        registerForContextMenu(mNumberTextView);

        mSetTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchCriteria setSearch = new SearchCriteria();
                assert mSetTextView.getText() != null;
                setSearch.set = mSetTextView.getText().toString();
                Bundle arguments = new Bundle();
                arguments.putSerializable(SearchViewFragment.CRITERIA, setSearch);
                ResultListFragment rlFrag = new ResultListFragment();
                startNewFragment(rlFrag, arguments);
            }
        });

        mCardImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mAsyncTask != null) {
                    mAsyncTask.cancel(true);
                }
                mAsyncTask = new saveCardImageTask();
                ((saveCardImageTask) mAsyncTask).execute(MAIN_PAGE);
                return true;
            }
        });

        setInfoFromBundle(this.getArguments());

        return myFragmentView;
    }

    /**
     * When the view is destroyed, release any memory used to display card images.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseImageResources(false);
    }

    /**
     * Release all image resources and invoke the garbage collector.
     */
    @SuppressFBWarnings(value = "DM_GC", justification = "Memory Leak without this")
    private void releaseImageResources(boolean isSplit) {

        if (mCardImageView != null) {

            /* Release the drawable from the ImageView */
            Drawable drawable = mCardImageView.getDrawable();
            if (drawable != null) {
                drawable.setCallback(null);
                Bitmap drawableBitmap = ((BitmapDrawable) drawable).getBitmap();
                if (drawableBitmap != null) {
                    drawableBitmap.recycle();
                }
            }

            /* Release the ImageView */
            mCardImageView.setImageDrawable(null);
            mCardImageView.setImageBitmap(null);

            if (!isSplit) {
                mCardImageView = null;
            }
        }
        if (mCardBitmap != null) {
            /* Release the drawable */
            mCardBitmap.getBitmap().recycle();
            mCardBitmap = null;
        }

        if (!isSplit) {
            mNameTextView = null;
            mCostTextView = null;
            mTypeTextView = null;
            mSetTextView = null;
            mAbilityTextView = null;
            mFlavorTextView = null;
            mArtistTextView = null;
            mNumberTextView = null;
            mPowTouTextView = null;
            mTransformButtonDivider = null;
            mTransformButton = null;
            mTextScrollView = null;
            mImageScrollView = null;
            mCardImageView = null;
            mColorIndicatorLayout = null;
        }

        /* Invoke the garbage collector */
        java.lang.System.gc();
    }

    /**
     * This will fill the UI elements with database information about the card specified in the
     * given bundle.
     *
     * @param extras The bundle passed to this fragment
     */
    private void setInfoFromBundle(Bundle extras) {
        if (extras == null && mNameTextView != null) {
            mNameTextView.setText("");
            mCostTextView.setText("");
            mTypeTextView.setText("");
            mSetTextView.setText("");
            mAbilityTextView.setText("");
            mFlavorTextView.setText("");
            mArtistTextView.setText("");
            mNumberTextView.setText("");
            mPowTouTextView.setText("");
            mTransformButton.setVisibility(View.GONE);
            mTransformButtonDivider.setVisibility(View.GONE);
        } else if (extras != null) {
            long cardID = extras.getLong(CARD_ID);

            /* from onCreateView */
            setInfoFromID(cardID);
        }
    }

    /**
     * This will fill the UI elements with information from the database.
     * It also saves information for AsyncTasks to use later and manages the transform/flip button.
     *
     * @param id the ID of the the card to be displayed
     */
    public void setInfoFromID(final long id) {

        /* If the views are null, don't attempt to fill them in */
        if (mSetTextView == null) {
            return;
        }

        ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getActivity());

        try {
            SQLiteDatabase database =
                    DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
            Cursor cCardById;
            cCardById = CardDbAdapter.fetchCards(new long[]{id}, null, database);

            /* http://magiccards.info/scans/en/mt/55.jpg */
            mCardName = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NAME));
            mCardCMC = cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_CMC));
            mSetCode = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET));

            mSetName = CardDbAdapter.getSetNameFromCode(mSetCode, database);

            mMagicCardsInfoSetCode =
                    CardDbAdapter.getCodeMtgi(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET)),
                            database);
            mCardNumber = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NUMBER));

            switch ((char) cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_RARITY))) {
                case 'C':
                case 'c':
                    mSetTextView.setTextColor(ContextCompat.getColor(getContext(),
                            getResourceIdFromAttr(R.attr.color_common)));
                    break;
                case 'U':
                case 'u':
                    mSetTextView.setTextColor(ContextCompat.getColor(getContext(),
                            getResourceIdFromAttr(R.attr.color_uncommon)));
                    break;
                case 'R':
                case 'r':
                    mSetTextView.setTextColor(ContextCompat.getColor(getContext(),
                            getResourceIdFromAttr(R.attr.color_rare)));
                    break;
                case 'M':
                case 'm':
                    mSetTextView.setTextColor(ContextCompat.getColor(getContext(),
                            getResourceIdFromAttr(R.attr.color_mythic)));
                    break;
                case 'T':
                case 't':
                    mSetTextView.setTextColor(ContextCompat.getColor(getContext(),
                            getResourceIdFromAttr(R.attr.color_timeshifted)));
                    break;
            }

            String sCost = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_MANACOST));
            CharSequence csCost = ImageGetterHelper.formatStringWithGlyphs(sCost, imgGetter);
            mCostTextView.setText(csCost);

            String sAbility = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_ABILITY));
            CharSequence csAbility = ImageGetterHelper.formatStringWithGlyphs(sAbility, imgGetter);
            mAbilityTextView.setText(csAbility);
            mAbilityTextView.setMovementMethod(LinkMovementMethod.getInstance());

            String sFlavor = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
            CharSequence csFlavor = ImageGetterHelper.formatStringWithGlyphs(sFlavor, imgGetter);
            mFlavorTextView.setText(csFlavor);

            mNameTextView
                    .setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NAME)));
            mCardType = CardDbAdapter.getTypeLine(cCardById);
            mTypeTextView.setText(mCardType);
            mSetTextView.setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET)));
            mArtistTextView
                    .setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_ARTIST)));
            String numberAndRarity =
                    cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NUMBER)) + " (" +
                            (char) cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_RARITY))
                            + ")";
            mNumberTextView.setText(numberAndRarity);

            int loyalty = cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
            float p = cCardById.getFloat(cCardById.getColumnIndex(CardDbAdapter.KEY_POWER));
            float t = cCardById.getFloat(cCardById.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
            if (loyalty != CardDbAdapter.NO_ONE_CARES) {
                if (loyalty == CardDbAdapter.X) {
                    mPowTouTextView.setText("X");
                } else {
                    mPowTouTextView.setText(Integer.toString(loyalty));
                }
            } else if (p != CardDbAdapter.NO_ONE_CARES && t != CardDbAdapter.NO_ONE_CARES) {

                String powTouStr = "";

                if (p == CardDbAdapter.STAR)
                    powTouStr += "*";
                else if (p == CardDbAdapter.ONE_PLUS_STAR)
                    powTouStr += "1+*";
                else if (p == CardDbAdapter.TWO_PLUS_STAR)
                    powTouStr += "2+*";
                else if (p == CardDbAdapter.SEVEN_MINUS_STAR)
                    powTouStr += "7-*";
                else if (p == CardDbAdapter.STAR_SQUARED)
                    powTouStr += "*^2";
                else if (p == CardDbAdapter.X)
                    powTouStr += "X";
                else {
                    if (p == (int) p) {
                        powTouStr += (int) p;
                    } else {
                        powTouStr += p;
                    }
                }

                powTouStr += "/";

                if (t == CardDbAdapter.STAR)
                    powTouStr += "*";
                else if (t == CardDbAdapter.ONE_PLUS_STAR)
                    powTouStr += "1+*";
                else if (t == CardDbAdapter.TWO_PLUS_STAR)
                    powTouStr += "2+*";
                else if (t == CardDbAdapter.SEVEN_MINUS_STAR)
                    powTouStr += "7-*";
                else if (t == CardDbAdapter.STAR_SQUARED)
                    powTouStr += "*^2";
                else if (t == CardDbAdapter.X)
                    powTouStr += "X";
                else {
                    if (t == (int) t) {
                        powTouStr += (int) t;
                    } else {
                        powTouStr += t;
                    }
                }

                mPowTouTextView.setText(powTouStr);
            } else {
                mPowTouTextView.setText("");
            }

            boolean isMultiCard = false;
            switch (CardDbAdapter.isMultiCard(mCardNumber,
                    cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET)))) {
                case NOPE:
                    isMultiCard = false;
                    mTransformButton.setVisibility(View.GONE);
                    mTransformButtonDivider.setVisibility(View.GONE);
                    break;
                case TRANSFORM:
                    isMultiCard = true;
                    mTransformButton.setVisibility(View.VISIBLE);
                    mTransformButtonDivider.setVisibility(View.VISIBLE);
                    mTransformButton.setText(R.string.card_view_transform);
                    break;
                case FUSE:
                    isMultiCard = true;
                    mTransformButton.setVisibility(View.VISIBLE);
                    mTransformButtonDivider.setVisibility(View.VISIBLE);
                    mTransformButton.setText(R.string.card_view_fuse);
                    break;
                case SPLIT:
                    isMultiCard = true;
                    mTransformButton.setVisibility(View.VISIBLE);
                    mTransformButtonDivider.setVisibility(View.VISIBLE);
                    mTransformButton.setText(R.string.card_view_other_half);
                    break;
            }

            if (isMultiCard) {
                if (mCardNumber.contains("a")) {
                    mTransformCardNumber = mCardNumber.replace("a", "b");
                } else if (mCardNumber.contains("b")) {
                    mTransformCardNumber = mCardNumber.replace("b", "a");
                }
                mTransformId = CardDbAdapter.getIdFromSetAndNumber(mSetCode, mTransformCardNumber, database);
                if (mTransformId == -1) {
                    mTransformButton.setVisibility(View.GONE);
                    mTransformButtonDivider.setVisibility(View.GONE);
                } else {
                    mTransformButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            releaseImageResources(true);
                            mCardNumber = mTransformCardNumber;
                            setInfoFromID(mTransformId);
                        }
                    });
                }
            }

            mMultiverseId = cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_MULTIVERSEID));

            /* Do we load the image immediately to the main page, or do it in a dialog later? */
            if (PreferenceAdapter.getPicFirst(getContext())) {
                mImageScrollView.setVisibility(View.VISIBLE);
                mTextScrollView.setVisibility(View.GONE);

                mActivity.setLoading();
                if (mAsyncTask != null) {
                    mAsyncTask.cancel(true);
                }
                mAsyncTask = new FetchPictureTask();
                ((FetchPictureTask) mAsyncTask).execute(MAIN_PAGE);
            } else {
                mImageScrollView.setVisibility(View.GONE);
                mTextScrollView.setVisibility(View.VISIBLE);
            }

            /* Figure out how large the color indicator should be. Medium text is 18sp, with a border
             * its 22sp */
            int dimension = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 22, getResources().getDisplayMetrics());

            mColorIndicatorLayout.removeAllViews();
            ColorIndicatorView civ =
                    new ColorIndicatorView(this.getActivity(), dimension, dimension / 15,
                            cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_COLOR)),
                            sCost);
            if (civ.shouldInidcatorBeShown()) {
                mColorIndicatorLayout.setVisibility(View.VISIBLE);
                mColorIndicatorLayout.addView(civ);
            } else {
                mColorIndicatorLayout.setVisibility(View.GONE);
            }

            String allLanguageKeys[][] = {
                    {Language.Chinese_Traditional, CardDbAdapter.KEY_NAME_CHINESE_TRADITIONAL, CardDbAdapter.KEY_MULTIVERSEID_CHINESE_TRADITIONAL},
                    {Language.Chinese_Simplified, CardDbAdapter.KEY_NAME_CHINESE_SIMPLIFIED, CardDbAdapter.KEY_MULTIVERSEID_CHINESE_SIMPLIFIED},
                    {Language.French, CardDbAdapter.KEY_NAME_FRENCH, CardDbAdapter.KEY_MULTIVERSEID_FRENCH},
                    {Language.German, CardDbAdapter.KEY_NAME_GERMAN, CardDbAdapter.KEY_MULTIVERSEID_GERMAN},
                    {Language.Italian, CardDbAdapter.KEY_NAME_ITALIAN, CardDbAdapter.KEY_MULTIVERSEID_ITALIAN},
                    {Language.Japanese, CardDbAdapter.KEY_NAME_JAPANESE, CardDbAdapter.KEY_MULTIVERSEID_JAPANESE},
                    {Language.Portuguese_Brazil, CardDbAdapter.KEY_NAME_PORTUGUESE_BRAZIL, CardDbAdapter.KEY_MULTIVERSEID_PORTUGUESE_BRAZIL},
                    {Language.Russian, CardDbAdapter.KEY_NAME_RUSSIAN, CardDbAdapter.KEY_MULTIVERSEID_RUSSIAN},
                    {Language.Spanish, CardDbAdapter.KEY_NAME_SPANISH, CardDbAdapter.KEY_MULTIVERSEID_SPANISH},
                    {Language.Korean, CardDbAdapter.KEY_NAME_KOREAN, CardDbAdapter.KEY_MULTIVERSEID_KOREAN}};

            // Clear the translations first
            mTranslatedNames.clear();

            // Add English
            Card.ForeignPrinting englishPrinting = new Card.ForeignPrinting();
            englishPrinting.mLanguageCode = Language.English;
            englishPrinting.mName = mCardName;
            englishPrinting.mMultiverseId = mMultiverseId;
            mTranslatedNames.add(englishPrinting);

            // Add all the others
            for (String lang[] : allLanguageKeys) {
                Card.ForeignPrinting fp = new Card.ForeignPrinting();
                fp.mLanguageCode = lang[0];
                fp.mName = cCardById.getString(cCardById.getColumnIndex(lang[1]));
                fp.mMultiverseId = cCardById.getInt(cCardById.getColumnIndex(lang[2]));
                if (fp.mName != null && !fp.mName.isEmpty()) {
                    mTranslatedNames.add(fp);
                }
            }

            cCardById.close();

            /* Find the other sets this card is in ahead of time, so that it can be remove from the menu
             * if there is only one set */
            Cursor cCardByName;
            cCardByName = CardDbAdapter.fetchCardByName(mCardName,
                    Arrays.asList(
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER), false, database
            );
            mPrintings = new LinkedHashSet<>();
            mCardIds = new LinkedHashSet<>();
            while (!cCardByName.isAfterLast()) {
                String number =
                        cCardByName.getString(cCardByName.getColumnIndex(CardDbAdapter.KEY_NUMBER));
                if (!(number == null || number.length() == 0)) {
                    number = " (" + number + ")";
                } else {
                    number = "";
                }
                if (mPrintings.add(CardDbAdapter
                        .getSetNameFromCode(cCardByName.getString(cCardByName.getColumnIndex(CardDbAdapter.KEY_SET)), database) + number)) {
                    mCardIds.add(cCardByName.getLong(cCardByName.getColumnIndex(CardDbAdapter.KEY_ID)));
                }
                cCardByName.moveToNext();
            }
            cCardByName.close();
            /* If it exists in only one set, remove the button from the menu */
            if (mPrintings.size() == 1) {
                mActivity.supportInvalidateOptionsMenu();
            }
        } catch (FamiliarDbException | CursorIndexOutOfBoundsException e) {
            handleFamiliarDbException(true);
            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
            return;
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * Remove any showing dialogs, and show the requested one.
     *
     * @param id the ID of the dialog to show
     */
    private void showDialog(final int id) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also
         * want to remove any currently showing dialog, so make our own transaction and take care of
         * that here. */

        /* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        /* Create and show the dialog. */
        CardViewDialogFragment newFragment = new CardViewDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        newFragment.setArguments(arguments);
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Called when a registered view is long-pressed. The menu inflated will give different options
     * based on the view class.
     *
     * @param menu     The context menu that is being built
     * @param v        The view for which the context menu is being built
     * @param menuInfo Extra information about the item for which the context menu should be shown.
     *                 This information will vary depending on the class of v.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);


        TextView tv = (TextView) v;

        assert tv.getText() != null;
        mCopyString = tv.getText().toString();

        android.view.MenuInflater inflater = this.mActivity.getMenuInflater();
        inflater.inflate(R.menu.copy_menu, menu);
    }

    /**
     * Copies text to the clipboard.
     *
     * @param item The context menu item that was selected.
     * @return boolean Return false to allow normal context menu processing to proceed, true to
     *         consume it here.
     */
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (getUserVisibleHint()) {
            String copyText = null;
            switch (item.getItemId()) {
                case R.id.copy: {
                    copyText = mCopyString;
                    break;
                }
                case R.id.copyall: {
                    if (mNameTextView.getText() != null &&
                            mCostTextView.getText() != null &&
                            mTypeTextView.getText() != null &&
                            mSetTextView.getText() != null &&
                            mAbilityTextView.getText() != null &&
                            mFlavorTextView.getText() != null &&
                            mPowTouTextView.getText() != null &&
                            mArtistTextView.getText() != null &&
                            mNumberTextView.getText() != null) {
                        // Hacky, but it works
                        String costText =
                                convertHtmlToPlainText(Html.toHtml(
                                        new SpannableString(mCostTextView.getText())));
                        String abilityText =
                                convertHtmlToPlainText(Html.toHtml(
                                        new SpannableString(mAbilityTextView.getText())));
                        copyText = mNameTextView.getText().toString() + '\n' +
                                costText + '\n' +
                                mTypeTextView.getText().toString() + '\n' +
                                mSetTextView.getText().toString() + '\n' +
                                abilityText + '\n' +
                                mFlavorTextView.getText().toString() + '\n' +
                                mPowTouTextView.getText().toString() + '\n' +
                                mArtistTextView.getText().toString() + '\n' +
                                mNumberTextView.getText().toString();
                    }
                    break;
                }
                default: {
                    return super.onContextItemSelected(item);
                }
            }

            if (copyText != null) {
                ClipboardManager clipboard = (ClipboardManager) (this.mActivity.
                        getSystemService(android.content.Context.CLIPBOARD_SERVICE));
                String label = getResources().getString(R.string.app_name);
                String mimeTypes[] = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                ClipData cd = new ClipData(label, mimeTypes, new ClipData.Item(copyText));
                clipboard.setPrimaryClip(cd);
            }
            return true;
        }
        return false;
    }

    /**
     * Converts some html to plain text, replacing images with their textual counterparts.
     *
     * @param html html to be converted
     * @return plain text representation of the input
     */
    public String convertHtmlToPlainText(String html) {
        Document document = Jsoup.parse(html);
        Elements images = document.select("img");
        for (Element image : images) {
            image.html("{" + image.attr("src") + "}");
        }
        return document.text();
    }

    /**
     * Handles clicks from the ActionBar.
     *
     * @param item the item clicked
     * @return true if acted upon, false if otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mCardName == null) {
            /*disable menu buttons if the card isn't initialized */
            return false;
        }
        /* Handle item selection */
        switch (item.getItemId()) {
            case R.id.image: {
                if (FamiliarActivity.getNetworkState(getContext(), true) == -1) {
                    return true;
                }

                mActivity.setLoading();
                if (mAsyncTask != null) {
                    mAsyncTask.cancel(true);
                }
                mAsyncTask = new FetchPictureTask();
                ((FetchPictureTask) mAsyncTask).execute(DIALOG);
                return true;
            }
            case R.id.price: {
                mActivity.setLoading();

                PriceFetchRequest priceRequest;
                priceRequest = new PriceFetchRequest(mCardName, mSetCode, mCardNumber, mMultiverseId, getActivity());
                mActivity.mSpiceManager.execute(priceRequest,
                        mCardName + "-" + mSetCode, DurationInMillis.ONE_DAY, new RequestListener<PriceInfo>() {

                            @Override
                            public void onRequestFailure(SpiceException spiceException) {
                                if (CardViewFragment.this.isAdded()) {
                                    mActivity.clearLoading();

                                    CardViewFragment.this.removeDialog(getFragmentManager());
                                    ToastWrapper.makeText(mActivity, spiceException.getMessage(),
                                            ToastWrapper.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onRequestSuccess(final PriceInfo result) {
                                if (CardViewFragment.this.isAdded()) {
                                    mActivity.clearLoading();

                                    if (result != null) {
                                        mPriceInfo = result;
                                        showDialog(CardViewDialogFragment.GET_PRICE);
                                    } else {
                                        ToastWrapper.makeText(mActivity,
                                                R.string.card_view_price_not_found,
                                                ToastWrapper.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                );

                return true;
            }
            case R.id.changeset: {
                showDialog(CardViewDialogFragment.CHANGE_SET);
                return true;
            }
            case R.id.legality: {
                mActivity.setLoading();
                if (mAsyncTask != null) {
                    mAsyncTask.cancel(true);
                }
                mAsyncTask = new FetchLegalityTask();
                ((FetchLegalityTask) mAsyncTask).execute((Void[]) null);
                return true;
            }
            case R.id.cardrulings: {
                if (FamiliarActivity.getNetworkState(getContext(), true) == -1) {
                    return true;
                }

                mActivity.setLoading();
                if (mAsyncTask != null) {
                    mAsyncTask.cancel(true);
                }
                mAsyncTask = new FetchRulingsTask();
                ((FetchRulingsTask) mAsyncTask).execute((Void[]) null);
                return true;
            }
            case R.id.addtowishlist: {
                showDialog(CardViewDialogFragment.WISH_LIST_COUNTS);
                return true;
            }
            case R.id.sharecard: {
                showDialog(CardViewDialogFragment.SHARE_CARD);
                return true;
            }
            case R.id.translatecard: {
                showDialog(CardViewDialogFragment.TRANSLATE_CARD);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Inflate the ActionBar items.
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.card_menu, menu);
    }

    /**
     * Prepare the Screen's standard options menu to be displayed.  This is called right before the
     * menu is shown, every time it is shown.  You can use this method to efficiently enable/disable
     * items or otherwise dynamically modify the contents.
     *
     * @param menu The options menu as last shown or first initialized by onCreateOptionsMenu().
     * @see #setHasOptionsMenu
     * @see #onCreateOptionsMenu
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem mi;
        /* If the image has been loaded to the main page, remove the menu option for image */
        if (PreferenceAdapter.getPicFirst(getContext()) && mCardBitmap != null) {
            mi = menu.findItem(R.id.image);
            if (mi != null) {
                menu.removeItem(mi.getItemId());
            }
        }
        /* This code removes the "change set" button if there is only one set.
         * Turns out some users use it to view the full set name when there is only one set/
         * I'm leaving it here, but commented, for posterity */
        /*
         if (mPrintings != null && mPrintings.size() == 1) {
            mi = menu.findItem(R.id.changeset);
            if (mi != null) {
                menu.removeItem(mi.getItemId());
            }
        }
        */
    }

    /**
     * Called from the share dialog to load and share this card's image.
     */
    public void runShareImageTask() {
        mActivity.setLoading();
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }
        mAsyncTask = new FetchPictureTask();
        ((FetchPictureTask) mAsyncTask).execute(SHARE);
    }

    /**
     * This inner class encapsulates a ruling and the date it was made.
     */
    public static class Ruling {
        public final String date;
        public final String ruling;

        public Ruling(String d, String r) {
            date = d;
            ruling = r;
        }

        public String toString() {
            return date + ": " + ruling;
        }
    }

    public class saveCardImageTask extends AsyncTask<Integer, Void, Void> {

        String mToastString = null;
        private Integer mWhereTo;

        @Override
        protected Void doInBackground(Integer... params) {

            if (params != null && params.length > 0) {
                mWhereTo = params[0];
            } else {
                mWhereTo = MAIN_PAGE;
            }

            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                mToastString = getString(R.string.card_view_no_external_storage);
                return null;
            }

            /* Check if permission is granted */
            if (ContextCompat.checkSelfPermission(CardViewFragment.this.mActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                /* Request the permission */
                ActivityCompat.requestPermissions(CardViewFragment.this.mActivity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        FamiliarActivity.REQUEST_WRITE_EXTERNAL_STORAGE_IMAGE);
            } else {
                /* Permission already granted */
                mToastString = saveImage();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mWhereTo == SHARE) {
                try {

                    /* Start the intent to share the image */
                    Uri uri = FileProvider.getUriForFile(mActivity,
                            BuildConfig.APPLICATION_ID + ".FileProvider", getSavedImageFile(false));
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.setType("image/jpeg");
                    startActivity(Intent.createChooser(shareIntent,
                            getResources().getText(R.string.card_view_send_to)));

                } catch (Exception e) {
                    ToastWrapper.makeText(mActivity, e.getMessage(), ToastWrapper.LENGTH_LONG)
                            .show();
                }
            } else if (mToastString != null) {
                ToastWrapper.makeText(mActivity, mToastString, ToastWrapper.LENGTH_LONG).show();
            }
        }
    }

    /**
     * This private class handles asking the database about the legality of a card, and will
     * eventually show the information in a Dialog.
     */
    private class FetchLegalityTask extends AsyncTask<Void, Void, Void> {

        /**
         * Queries the data in the database to see what sets this card is legal in.
         *
         * @param params unused
         * @return unused
         */
        @Override
        protected Void doInBackground(Void... params) {

            try {
                SQLiteDatabase database =
                        DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                Cursor cFormats = CardDbAdapter.fetchAllFormats(database);
                mFormats = new String[cFormats.getCount()];
                mLegalities = new String[cFormats.getCount()];

                cFormats.moveToFirst();
                for (int i = 0; i < cFormats.getCount(); i++) {
                    mFormats[i] =
                            cFormats.getString(cFormats.getColumnIndex(CardDbAdapter.KEY_NAME));
                    switch (CardDbAdapter.checkLegality(mCardName, mFormats[i], database)) {
                        case CardDbAdapter.LEGAL:
                            mLegalities[i] = getString(R.string.card_view_legal);
                            break;
                        case CardDbAdapter.RESTRICTED:
                            /* For backwards compatibility, we list cards that are legal in
                             * commander, but can't be the commander as Restricted in the legality
                             * file.  This prevents older version of the app from throwing an
                             * IllegalStateException if we try including a new legality. */
                            if (mFormats[i].equalsIgnoreCase("Commander")) {
                                mLegalities[i] = getString(R.string.card_view_no_commander);
                            } else {
                                mLegalities[i] = getString(R.string.card_view_restricted);
                            }
                            break;
                        case CardDbAdapter.BANNED:
                            mLegalities[i] = getString(R.string.card_view_banned);
                            break;
                        default:
                            mLegalities[i] = getString(R.string.error);
                            break;
                    }
                    cFormats.moveToNext();
                }
                cFormats.close();
            } catch (FamiliarDbException e) {
                CardViewFragment.this.handleFamiliarDbException(false);
                mLegalities = null;
            }

            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
            return null;
        }

        /**
         * After the query, remove the progress dialog and show the legalities.
         *
         * @param result unused
         */
        @Override
        protected void onPostExecute(Void result) {
            try {
                showDialog(CardViewDialogFragment.GET_LEGALITY);
            } catch (IllegalStateException e) {
                /* eat it */
            }
            mActivity.clearLoading();
        }
    }

    /**
     * This private class retrieves a picture of the card from the internet.
     */
    private class FetchPictureTask extends AsyncTask<Integer, Void, Void> {

        int mHeight;
        int mWidth;
        int mBorder;

        private String mError;
        private int mLoadTo;
        private String mImageKey;

        /* Get the size of the window on the UI thread, not the worker thread */
        final Runnable getWindowSize = new Runnable() {
            @Override
            public void run() {
                Rect rectangle = new Rect();
                mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);

                assert mActivity.getSupportActionBar() != null; /* Because Android Studio */
                mHeight = ((rectangle.bottom - rectangle.top) -
                        mActivity.getSupportActionBar().getHeight()) - mBorder;
                mWidth = (rectangle.right - rectangle.left) - mBorder;

                synchronized (this) {
                    this.notify();
                }
            }
        };

        /**
         * If the preferred langauge is English, get the card image from Scryfall.
         * If that fails, check www.MagicCards.info for the card image in the user's preferred
         * language.
         * If that fails, try Scryfall again in English.
         * If that fails, check www.MagicCards.info for the card image in English.
         * If that fails, check www.gatherer.wizards.com for the card image.
         * If that fails, give up.
         * There is a non-standard URL building for planes and schemes for www.MagicCards.info.
         * It also re-sizes the image.
         *
         * @param params unused
         * @return unused
         */
        @SuppressWarnings("SpellCheckingInspection")
        @SuppressFBWarnings(value = "DM_GC", justification = "Memory leak without the GC")
        @Override
        protected Void doInBackground(Integer... params) {

            if (params != null && params.length > 0) {
                mLoadTo = params[0];
            } else {
                mLoadTo = MAIN_PAGE;
            }

            String cardLanguage = PreferenceAdapter.getCardLanguage(getContext());
            if (cardLanguage == null) {
                cardLanguage = "en";
            }

            mImageKey = Integer.toString(mMultiverseId) + cardLanguage;

            /* Check disk cache in background thread */
            Bitmap bitmap;
            try {
                bitmap = getFamiliarActivity().mImageCache.getBitmapFromDiskCache(mImageKey);
            } catch (NullPointerException e) {
                bitmap = null;
            }

            if (bitmap == null) { /* Not found in disk cache */

                /* Some trickery to figure out if we have a token */
                boolean isToken = false;
                if (mCardType.contains("Token") || /* try to take the easy way out */
                    (mCardCMC == 0 && /* Tokens have a CMC of 0 */
                    /* The only tokens in Gatherer are from Duel Decks */
                     mSetName.contains("Duel Decks") &&
                     /* The only tokens in Gatherer are creatures */
                     mCardType.contains("Creature"))) {
                    isToken = true;
                }

                boolean bRetry = true;

                boolean triedMtgi = false;
                boolean triedGatherer = false;
                boolean triedScryfall = false;

                while (bRetry) {

                    bRetry = false;
                    mError = null;

                    try {
                        URL u;
                        if (!cardLanguage.equalsIgnoreCase("en") && !isToken) {
                            /* Non-English have to come from magiccards.info. Try there first */
                            u = new URL(getMtgiPicUrl(mCardName, mMagicCardsInfoSetCode, mCardNumber, cardLanguage));
                            /* If this fails, try next time with the English version */
                            cardLanguage = "en";
                        } else if (!triedScryfall && !isToken) {
                            /* Try downloading the image from Scryfall next */
                            u = new URL(getScryfallImageUri(mMultiverseId));
                            /* If this fails, try next time with the Magiccards.info version */
                            triedScryfall = true;
                        } else if (!triedMtgi && !isToken) {
                            /* Try downloading the image from magiccards.info next */
                            u = new URL(getMtgiPicUrl(mCardName, mMagicCardsInfoSetCode, mCardNumber, cardLanguage));
                            /* If this fails, try next time with the gatherer version */
                            triedMtgi = true;
                        } else {
                            /* Try downloading the image from gatherer */
                            u = new URL("http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=" + mMultiverseId + "&type=card");
                            /* If this fails, give up */
                            triedGatherer = true;
                        }

                        /* Download the bitmap */
                        bitmap = BitmapFactory.decodeStream(FamiliarActivity.getHttpInputStream(u, null));
                        /* Cache it */
                        getFamiliarActivity().mImageCache.addBitmapToCache(mImageKey, new BitmapDrawable(mActivity.getResources(), bitmap));
                    } catch (Exception e) {
                        /* Something went wrong */
                        try {
                            mError = getString(R.string.card_view_image_not_found);
                        } catch (RuntimeException re) {
                            /* in case the fragment isn't attached to an activity */
                            mError = e.toString();
                        }

                        /* Gatherer is always tried last. If that fails, give up */
                        if (!triedGatherer) {
                            bRetry = true;
                        }
                    }
                }
            }

            /* Image download failed, just return null */
            if (bitmap == null) {
                return null;
            }

            try {
                /* 16dp */
                mBorder = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
                if (mLoadTo == MAIN_PAGE) {
                    /* Block the worker thread until the size is figured out */
                    synchronized (getWindowSize) {
                        getActivity().runOnUiThread(getWindowSize);
                        getWindowSize.wait();
                    }
                } else if (mLoadTo == DIALOG) {
                    Display display = ((WindowManager) mActivity
                            .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    Point p = new Point();
                    display.getSize(p);
                    mHeight = p.y - mBorder;
                    mWidth = p.x - mBorder;
                } else if (mLoadTo == SHARE) {
                    /* Don't scale shared images */
                    mWidth = bitmap.getWidth();
                    mHeight = bitmap.getHeight();
                }

                float screenAspectRatio = (float) mHeight / (float) (mWidth);
                float cardAspectRatio = (float) bitmap.getHeight() / (float) bitmap.getWidth();

                float scale;
                if (screenAspectRatio > cardAspectRatio) {
                    scale = (mWidth) / (float) bitmap.getWidth();
                } else {
                    scale = (mHeight) / (float) bitmap.getHeight();
                }

                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                mCardBitmap = new RecyclingBitmapDrawable(mActivity.getResources(), scaledBitmap);
            } catch (InterruptedException e) {
                /* Some error resizing. Out of memory? */
            }

            /* Recycle the non-scaled bitmap to avoid memory leaks */
            bitmap.recycle();
            java.lang.System.gc();
            return null;
        }

        /**
         * Jumps through hoops and returns a correctly formatted URL for magiccards.info's image.
         *
         * @param cardName              The name of the card
         * @param magicCardsInfoSetCode The set of the card
         * @param cardNumber            The number of the card
         * @param cardLanguage          The language of the card
         * @return a URL to the card's image
         */
        private String getMtgiPicUrl(
                String cardName,
                String magicCardsInfoSetCode,
                String cardNumber,
                String cardLanguage) {

            final String mtgiExtras = "http://magiccards.info/extras/";
            String picURL;
            if (mCardType.toLowerCase().contains(getString(R.string.search_Ongoing).toLowerCase()) ||
                    /* extra space to not confuse with planeswalker */
                    mCardType.toLowerCase().contains(getString(R.string.search_Plane).toLowerCase() + " ") ||
                    mCardType.toLowerCase().contains(getString(R.string.search_Phenomenon).toLowerCase()) ||
                    mCardType.toLowerCase().contains(getString(R.string.search_Scheme).toLowerCase())) {
                switch (mSetCode) {
                    case "PC2":
                        picURL = mtgiExtras + "plane/planechase-2012-edition/" + cardName + ".jpg";
                        picURL = picURL.replace(" ", "-")
                                .replace("?", "").replace(",", "").replace("'", "").replace("!", "");
                        break;
                    case "PCH":
                        if (cardName.equalsIgnoreCase("tazeem")) {
                            cardName = "tazeem-release-promo";
                        } else if (cardName.equalsIgnoreCase("celestine reef")) {
                            cardName = "celestine-reef-pre-release-promo";
                        } else if (cardName.equalsIgnoreCase("horizon boughs")) {
                            cardName = "horizon-boughs-gateway-promo";
                        }
                        picURL = mtgiExtras + "plane/planechase/" + cardName + ".jpg";
                        picURL = picURL.replace(" ", "-")
                                .replace("?", "").replace(",", "").replace("'", "").replace("!", "");
                        break;
                    case "ARC":
                        picURL = mtgiExtras + "scheme/archenemy/" + cardName + ".jpg";
                        picURL = picURL.replace(" ", "-")
                                .replace("?", "").replace(",", "").replace("'", "").replace("!", "");
                        break;
                    default:
                        picURL = "http://magiccards.info/scans/" + cardLanguage + "/" + magicCardsInfoSetCode + "/" +
                                cardNumber + ".jpg";
                        break;
                }
            } else {
                picURL = "http://magiccards.info/scans/" + cardLanguage + "/" + magicCardsInfoSetCode + "/" +
                        cardNumber + ".jpg";
            }
            return picURL.toLowerCase(Locale.ENGLISH);
        }

        /**
         * Easily gets the uri for the image for a card by multiverseid.
         *
         * @param multiverseId the multiverse id of the card
         * @return uri of the card image
         */
        private String getScryfallImageUri(int multiverseId) {
            return "https://api.scryfall.com/cards/multiverse/" + multiverseId + "?format=image";
        }

        /**
         * When the task has finished, if there was no error, remove the progress dialog and show
         * the image. If the image was supposed to load to the main screen, and it failed to load,
         * fall back to text view
         *
         * @param result unused
         */
        @Override
        protected void onPostExecute(Void result) {
            if (mError == null) {
                if (mLoadTo == DIALOG) {
                    try {
                        showDialog(CardViewDialogFragment.GET_IMAGE);
                    } catch (IllegalStateException e) {
                        /* eat it */
                    }
                } else if (mLoadTo == MAIN_PAGE) {
                    removeDialog(getFragmentManager());
                    if (mCardImageView != null) {
                        mCardImageView.setImageDrawable(mCardBitmap);
                    }
                    /* remove the image load button if it is the main page */
                    mActivity.supportInvalidateOptionsMenu();
                } else if (mLoadTo == SHARE) {

                    /* Images must be saved before sharing */
                    if (mAsyncTask != null) {
                        mAsyncTask.cancel(true);
                    }
                    mAsyncTask = new saveCardImageTask();
                    ((saveCardImageTask) mAsyncTask).execute(SHARE);
                }
            } else {
                removeDialog(getFragmentManager());
                if (mLoadTo == MAIN_PAGE && mImageScrollView != null) {
                    mImageScrollView.setVisibility(View.GONE);
                    mTextScrollView.setVisibility(View.VISIBLE);
                }
                ToastWrapper.makeText(mActivity, mError, ToastWrapper.LENGTH_LONG).show();
            }
            mActivity.clearLoading();
        }

        /**
         * If the task is canceled, fall back to text view.
         */
        @Override
        protected void onCancelled() {
            if (mLoadTo == MAIN_PAGE && mImageScrollView != null) {
                mImageScrollView.setVisibility(View.GONE);
                mTextScrollView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * This private class fetches rulings about this card from gatherer.wizards.com.
     */
    private class FetchRulingsTask extends AsyncTask<Void, Void, Void> {

        String mErrorMessage = null;

        /**
         * This function downloads the source of the gatherer page, scans it for rulings, and stores
         * them for display.
         *
         * @param params unused
         * @return unused
         */
        @Override
        @SuppressWarnings("SpellCheckingInspection")
        protected Void doInBackground(Void... params) {

            URL url;
            InputStream is = null;

            mRulingsArrayList = new ArrayList<>();
            try {
                url = new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + mMultiverseId);
                is = FamiliarActivity.getHttpInputStream(url, null);
                if (is == null) {
                    throw new IOException("null stream");
                }

                String gathererPage = IOUtils.toString(is);
                String date;

                Document document = Jsoup.parse(gathererPage);
                Elements rulingTable = document.select("table.rulingsTable > tbody > tr");

                for (Element ruling : rulingTable) {
                    date = ruling.children().get(0).text();
                    Element rulingText = ruling.children().get(1);
                    Elements imageTags = rulingText.getElementsByTag("img");
                    /* For each symbol in the rulings text */
                    for (Element symbol : imageTags) {
                        /* Build the glyph with {, the text between "name=" and "&" and } */
                        String symbolString = "{" + symbol.attr("src").split("name=")[1].split("&")[0] + "}";
                        /* The new "HTML" for the symbols will be {n}, instead of the img tags they were before */
                        symbol.html(symbolString);
                    }
                    Ruling r = new Ruling(date, rulingText.text());
                    mRulingsArrayList.add(r);
                }
            } catch (Exception ioe) {
                mErrorMessage = ioe.getLocalizedMessage();
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException ioe) {
                    mErrorMessage = ioe.getLocalizedMessage();
                }
            }

            return null;
        }

        /**
         * Hide the progress dialog and show the rulings, if there are no errors.
         *
         * @param result unused
         */
        @Override
        protected void onPostExecute(Void result) {

            if (mErrorMessage == null) {
                try {
                    showDialog(CardViewDialogFragment.CARD_RULINGS);
                } catch (IllegalStateException e) {
                    /* eat it */
                }
            } else {
                removeDialog(getFragmentManager());
                ToastWrapper.makeText(mActivity, mErrorMessage, ToastWrapper.LENGTH_SHORT).show();
            }
            mActivity.clearLoading();
        }
    }

    /**
     * Callback for when a permission is requested.
     *
     * @param requestCode  The request code passed in requestPermissions(String[], int).
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either
     *                     android.content.pm.PackageManager.PERMISSION_GRANTED or
     *                     android.content.pm.PackageManager.PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case FamiliarActivity.REQUEST_WRITE_EXTERNAL_STORAGE_IMAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    /* Permission granted, run the task again */
                    if (mAsyncTask instanceof saveCardImageTask) {
                        int whereTo = ((saveCardImageTask) mAsyncTask).mWhereTo;
                        mAsyncTask.cancel(true);
                        mAsyncTask = new saveCardImageTask();
                        ((saveCardImageTask) mAsyncTask).execute(whereTo);
                    }
                } else {
                    /* Permission denied */
                    ToastWrapper.makeText(this.getContext(), getString(R.string.card_view_unable_to_save_image),
                            ToastWrapper.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Returns the File used to save this card's image.
     *
     * @param shouldDelete true if the file should be deleted before returned, false otherwise
     * @return A File, either with the image already or blank
     * @throws Exception If something goes wrong
     */
    private File getSavedImageFile(boolean shouldDelete) throws Exception {

        String strPath;
        try {
            strPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .getCanonicalPath() + "/MTGFamiliar";
        } catch (IOException ex) {
            throw new Exception(getString(R.string.card_view_no_pictures_folder));
        }

        File fPath = new File(strPath);

        if (!fPath.exists()) {
            if (!fPath.mkdir()) {
                throw new Exception(getString(R.string.card_view_unable_to_create_dir));
            }

            if (!fPath.isDirectory()) {
                throw new Exception(getString(R.string.card_view_unable_to_create_dir));
            }
        }

        fPath = new File(strPath, mCardName + "_" + mSetCode + ".jpg");

        if (shouldDelete) {
            if (fPath.exists()) {
                if (!fPath.delete()) {
                    throw new Exception(getString(R.string.card_view_unable_to_create_file));
                }
            }
        }

        return fPath;
    }

    /**
     * Saves the current card image to external storage.
     *
     * @return A status string, to be displayed in a toast on the UI thread
     */
    private String saveImage() {
        File fPath;

        try {
            fPath = getSavedImageFile(true);
        } catch (Exception e) {
            return e.getMessage();
        }

        String strPath = fPath.getAbsolutePath();

        if (fPath.exists()) {
            return getString(R.string.card_view_image_saved) + strPath;
        }
        try {
            if (!fPath.createNewFile()) {
                return getString(R.string.card_view_unable_to_create_file);
            }

            /* If the card is displayed, there's a real good chance it's cached */
            String cardLanguage = PreferenceAdapter.getCardLanguage(getContext());
            if (cardLanguage == null) {
                cardLanguage = "en";
            }
            String imageKey = Integer.toString(mMultiverseId) + cardLanguage;
            Bitmap bmpImage;
            try {
                bmpImage = getFamiliarActivity().mImageCache.getBitmapFromDiskCache(imageKey);
            } catch (NullPointerException e) {
                bmpImage = null;
            }

            /* Check if this is an english only image */
            if (bmpImage == null && !cardLanguage.equalsIgnoreCase("en")) {
                imageKey = Integer.toString(mMultiverseId) + "en";
                try {
                    bmpImage = getFamiliarActivity().mImageCache.getBitmapFromDiskCache(imageKey);
                } catch (NullPointerException e) {
                    bmpImage = null;
                }
            }

            /* nope, not here */
            if (bmpImage == null) {
                return getString(R.string.card_view_no_image);
            }

            FileOutputStream fStream = new FileOutputStream(fPath);

            boolean bCompressed = bmpImage.compress(Bitmap.CompressFormat.JPEG, 90, fStream);
            fStream.flush();
            fStream.close();

            if (!bCompressed) {
                return getString(R.string.card_view_unable_to_save_image);
            }

        } catch (IOException ex) {
            return getString(R.string.card_view_save_failure);
        }

        /* Notify the system that a new image was saved */
        getFamiliarActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(fPath)));

        return getString(R.string.card_view_image_saved) + strPath;
    }
}
