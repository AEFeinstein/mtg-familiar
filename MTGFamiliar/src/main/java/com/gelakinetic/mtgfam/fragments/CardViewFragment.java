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

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.Html.ImageGetter;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.ColorIndicatorView;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.lruCache.RecyclingBitmapDrawable;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * This class handles displaying card info
 * WARNING! Because this fragment is nested in a CardViewPagerFragment, always get the parent fragment's activity
 */
public class CardViewFragment extends FamiliarFragment {

    /* Bundle constant */
    public static final String CARD_ID = "card_id";
    /* Dialogs */
    private static final int GET_PRICE = 1;
    private static final int GET_IMAGE = 2;
    private static final int CHANGE_SET = 3;
    private static final int CARD_RULINGS = 4;
    private static final int WISH_LIST_COUNTS = 6;
    private static final int GET_LEGALITY = 7;
    /* Where the card image is loaded to */
    private static final int MAIN_PAGE = 1;
    private static final int DIALOG = 2;
    private int loadTo = DIALOG; /* where to load the image */
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
    private Button mTransformButton;
    private View mTransformButtonDivider;
    private ImageView mCardImageView;
    private ScrollView mTextScrollView;
    private ScrollView mImageScrollView;
    private LinearLayout mColorIndicatorLayout;

    /* the AsyncTask loads stuff off the UI thread, and stores whatever in these local variables */
    private AsyncTask<Void, Void, Void> mAsyncTask;
    private RecyclingBitmapDrawable mCardBitmap;
    private String[] mLegalities;
    private String[] mFormats;
    private ArrayList<Ruling> mRulingsArrayList;

    /* Loaded in a Spice Service */
    private PriceInfo mPriceInfo;

    /* Card info, used to build the URL to fetch the picture */
    private String mCardNumber;
    private String mSetCode;
    private String mCardName;
    private String mMagicCardsInfoSetCode;
    private int mMultiverseId;
    private String mCardType;

    /* Card info used to flip the card */
    private String mTransformCardNumber;
    private int mTransformId;

    /* To switch card between printings */
    private LinkedHashSet<String> mSets;
    private LinkedHashSet<Long> mCardIds;

    /* Easier than calling getActivity() all the time, and handles being nested */
    private FamiliarActivity mActivity;

    /* State for reporting page views */
    private boolean mHasReportedView = false;
    private boolean mShouldReportView = false;
    private String mDescription;
    private String mSetName;

    /**
     * Kill any AsyncTask if it is still running
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

		/* Pass a non-null bundle to the ResultListFragment so it knows to exit if there was a list of 1 card
         * If this wasn't launched by a ResultListFragment, it'll get eaten */
        Bundle args = new Bundle();
        mActivity.setFragmentResult(args);
    }

    /**
     * Called when the Fragment is no longer resumed. Clear the loading bar just in case
     */
    @Override
    public void onPause() {
        super.onPause();
        mActivity.clearLoading();
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }
    }

    /**
     * Called when the fragment stops, attempt to report the close
     */
    @Override
    public void onStop() {
        reportAppIndexEndIfAble();
        super.onStop();
    }

    /**
     * Creates and returns the action describing this page view
     *
     * @return An action describing this page view
     */
    private Action getAppIndexAction() {

        Thing object = new Thing.Builder()
                .setType("http://schema.org/Thing")         /* Optional, any valid schema.org type */
                .setName(mCardName + " (" + mSetName + ")") /* Required, title field */
                .setDescription(mDescription)               /* Required, description field */
                /* Required, deep link in the android-app:// format */
                .setUrl(Uri.parse("android-app://com.gelakinetic.mtgfam/card/multiverseid/" + mMultiverseId))
                .build();

        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .build();
    }

    /**
     * Reports this view to the Google app indexing API, once, when the fragment is viewed
     */
    private void reportAppIndexViewIfAble() {
        /* If this view hasn't been reported yet, and the name exists */
        if (!mHasReportedView) {
            if (mCardName != null) {
                /* Connect your client */
                getFamiliarActivity().mGoogleApiClient.connect();
                AppIndex.AppIndexApi.start(getFamiliarActivity().mGoogleApiClient, getAppIndexAction());

                /* Manage state */
                mHasReportedView = true;
                mShouldReportView = false;
            } else {
                mShouldReportView = true;
            }
        }
    }

    /**
     * Ends the report to the Google app indexing API, once, when the fragment is no longer viewed
     */
    private void reportAppIndexEndIfAble() {
        /* If the view was previously reported, and the name exists */
        if (mHasReportedView && mCardName != null) {
            /* Call end() and disconnect the client */
            AppIndex.AppIndexApi.end(getFamiliarActivity().mGoogleApiClient, getAppIndexAction());
            getFamiliarActivity().mGoogleApiClient.disconnect();

            /* manage state */
            mHasReportedView = false;
        }
    }

    /**
     * Set a hint to the system about whether this fragment's UI is currently visible to the user.
     * This hint defaults to true and is persistent across fragment instance state save and restore.
     * <p/>
     * An app may set this to false to indicate that the fragment's UI is scrolled out of visibility
     * or is otherwise not directly visible to the user. This may be used by the system to
     * prioritize operations such as fragment lifecycle updates or loader ordering behavior.
     * <p/>
     * In this case, it's used to report fragment views to Google app indexing
     *
     * @param isVisibleToUser true if this fragment's UI is currently visible to the user (default),
     *                        false if it is not.
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            /* If the fragment is visible to the user, attempt to report the view */
            reportAppIndexViewIfAble();
        } else {
            /* The view isn't visible anymore, attempt to report it */
            reportAppIndexEndIfAble();
        }
    }

    /**
     * Inflates the view and saves references to UI elements for later filling
     *
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @return The inflated view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        try {
            mActivity = ((FamiliarFragment) getParentFragment()).getFamiliarActivity();
        } catch (NullPointerException e) {
            mActivity = getFamiliarActivity();
        }

        View myFragmentView = inflater.inflate(R.layout.card_view_frag, container, false);

        assert myFragmentView != null; /* Because Android Studio */
        mNameTextView = (TextView) myFragmentView.findViewById(R.id.name);
        mCostTextView = (TextView) myFragmentView.findViewById(R.id.cost);
        mTypeTextView = (TextView) myFragmentView.findViewById(R.id.type);
        mSetTextView = (TextView) myFragmentView.findViewById(R.id.set);
        mAbilityTextView = (TextView) myFragmentView.findViewById(R.id.ability);
        mFlavorTextView = (TextView) myFragmentView.findViewById(R.id.flavor);
        mArtistTextView = (TextView) myFragmentView.findViewById(R.id.artist);
        mPowTouTextView = (TextView) myFragmentView.findViewById(R.id.pt);
        mTransformButtonDivider = myFragmentView.findViewById(R.id.transform_button_divider);
        mTransformButton = (Button) myFragmentView.findViewById(R.id.transformbutton);
        mTextScrollView = (ScrollView) myFragmentView.findViewById(R.id.cardTextScrollView);
        mImageScrollView = (ScrollView) myFragmentView.findViewById(R.id.cardImageScrollView);
        mCardImageView = (ImageView) myFragmentView.findViewById(R.id.cardpic);
        mColorIndicatorLayout = (LinearLayout) myFragmentView.findViewById(R.id.color_indicator_view);

        registerForContextMenu(mNameTextView);
        registerForContextMenu(mCostTextView);
        registerForContextMenu(mTypeTextView);
        registerForContextMenu(mSetTextView);
        registerForContextMenu(mAbilityTextView);
        registerForContextMenu(mPowTouTextView);
        registerForContextMenu(mFlavorTextView);
        registerForContextMenu(mArtistTextView);

        mCardImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mAsyncTask != null) {
                    mAsyncTask.cancel(true);
                }
                mAsyncTask = new saveCardImageTask();
                mAsyncTask.execute((Void[]) null);
                return true;
            }
        });

        if (mActivity.mPreferenceAdapter.getPicFirst()) {
            loadTo = MAIN_PAGE;
        } else {
            loadTo = DIALOG;
        }

        setInfoFromBundle(this.getArguments());

        return myFragmentView;
    }

    /**
     * This will fill the UI elements with database information about the card specified in the given bundle
     *
     * @param extras The bundle passed to this fragment
     */
    private void setInfoFromBundle(Bundle extras) {
        if (extras == null) {
            mNameTextView.setText("");
            mCostTextView.setText("");
            mTypeTextView.setText("");
            mSetTextView.setText("");
            mAbilityTextView.setText("");
            mFlavorTextView.setText("");
            mArtistTextView.setText("");
            mPowTouTextView.setText("");
            mTransformButton.setVisibility(View.GONE);
            mTransformButtonDivider.setVisibility(View.GONE);
            return;
        }
        long cardID = extras.getLong(CARD_ID);

        /* from onCreateView */
        setInfoFromID(cardID);
    }

    /**
     * This will fill the UI elements with information from the database
     * It also saves information for AsyncTasks to use later and manages the transform/flip button
     *
     * @param id the ID of the the card to be displayed
     */
    private void setInfoFromID(final long id) {

        ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getActivity());

        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
        Cursor cCardById;
        try {
            cCardById = CardDbAdapter.fetchCard(id, database);
        } catch (FamiliarDbException e) {
            handleFamiliarDbException(true);
            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
            return;
        }

		/* http://magiccards.info/scans/en/mt/55.jpg */
        mCardName = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NAME));
        mSetCode = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET));

        /* Start building a description */
        addToDescription(getString(R.string.search_name), mCardName);
        try {
            mSetName = CardDbAdapter.getSetNameFromCode(mSetCode, database);
            addToDescription(getString(R.string.search_set), mSetName);
        } catch (FamiliarDbException e) {
            /* no set for you */
        }

        try {
            mMagicCardsInfoSetCode =
                    CardDbAdapter.getCodeMtgi(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET)),
                            database);
        } catch (FamiliarDbException e) {
            handleFamiliarDbException(true);
            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
            return;
        }
        mCardNumber = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NUMBER));

        switch ((char) cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_RARITY))) {
            case 'C':
            case 'c':
                mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(getResourceIdFromAttr(R.attr.color_common)));
                addToDescription(getString(R.string.search_rarity), getString(R.string.search_Common));
                break;
            case 'U':
            case 'u':
                mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(getResourceIdFromAttr(R.attr.color_uncommon)));
                addToDescription(getString(R.string.search_rarity), getString(R.string.search_Uncommon));
                break;
            case 'R':
            case 'r':
                mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(getResourceIdFromAttr(R.attr.color_rare)));
                addToDescription(getString(R.string.search_rarity), getString(R.string.search_Rare));
                break;
            case 'M':
            case 'm':
                mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(getResourceIdFromAttr(R.attr.color_mythic)));
                addToDescription(getString(R.string.search_rarity), getString(R.string.search_Mythic));
                break;
            case 'T':
            case 't':
                mSetTextView.setTextColor(CardViewFragment.this.getResources().getColor(getResourceIdFromAttr(R.attr.color_timeshifted)));
                addToDescription(getString(R.string.search_rarity), getString(R.string.search_Timeshifted));
                break;
        }

        String sCost = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_MANACOST));
        addToDescription(getString(R.string.search_mana_cost), sCost);
        CharSequence csCost = ImageGetterHelper.formatStringWithGlyphs(sCost, imgGetter);
        mCostTextView.setText(csCost);

        String sAbility = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_ABILITY));
        addToDescription(getString(R.string.search_text), sAbility);
        CharSequence csAbility = ImageGetterHelper.formatStringWithGlyphs(sAbility, imgGetter);
        mAbilityTextView.setText(csAbility);

        String sFlavor = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
        addToDescription(getString(R.string.search_flavor_text), sFlavor);
        CharSequence csFlavor = ImageGetterHelper.formatStringWithGlyphs(sFlavor, imgGetter);
        mFlavorTextView.setText(csFlavor);

        mNameTextView.setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_NAME)));
        mCardType = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_TYPE));
        mTypeTextView.setText(mCardType);
        mSetTextView.setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET)));
        mArtistTextView.setText(cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_ARTIST)));

        addToDescription(getString(R.string.search_type), cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_TYPE)));
        addToDescription(getString(R.string.search_artist), cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_ARTIST)));

        int loyalty = cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
        float p = cCardById.getFloat(cCardById.getColumnIndex(CardDbAdapter.KEY_POWER));
        float t = cCardById.getFloat(cCardById.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
        if (loyalty != CardDbAdapter.NO_ONE_CARES) {
            mPowTouTextView.setText(Integer.valueOf(loyalty).toString());
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
            else {
                if (t == (int) t) {
                    powTouStr += (int) t;
                } else {
                    powTouStr += t;
                }
            }

            addToDescription(getString(R.string.search_power), powTouStr);

            mPowTouTextView.setText(powTouStr);
        } else {
            mPowTouTextView.setText("");
        }

        boolean isMultiCard = false;
        switch (CardDbAdapter.isMultiCard(mCardNumber,
                cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SET)))) {
            case CardDbAdapter.NOPE:
                isMultiCard = false;
                mTransformButton.setVisibility(View.GONE);
                mTransformButtonDivider.setVisibility(View.GONE);
                break;
            case CardDbAdapter.TRANSFORM:
                isMultiCard = true;
                mTransformButton.setVisibility(View.VISIBLE);
                mTransformButtonDivider.setVisibility(View.VISIBLE);
                mTransformButton.setText(R.string.card_view_transform);
                break;
            case CardDbAdapter.FUSE:
                isMultiCard = true;
                mTransformButton.setVisibility(View.VISIBLE);
                mTransformButtonDivider.setVisibility(View.VISIBLE);
                mTransformButton.setText(R.string.card_view_fuse);
                break;
            case CardDbAdapter.SPLIT:
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
            try {
                mTransformId = CardDbAdapter.getTransform(mSetCode, mTransformCardNumber, database);
            } catch (FamiliarDbException e) {
                handleFamiliarDbException(true);
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }
            if (mTransformId == -1) {
                mTransformButton.setVisibility(View.GONE);
                mTransformButtonDivider.setVisibility(View.GONE);
            } else {
                mTransformButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mCardBitmap = null;
                        mCardNumber = mTransformCardNumber;
                        setInfoFromID(mTransformId);
                    }
                });
            }
        }

        mMultiverseId = cCardById.getInt(cCardById.getColumnIndex(CardDbAdapter.KEY_MULTIVERSEID));

		/* Do we load the image immediately to the main page, or do it in a dialog later? */
        if (loadTo == MAIN_PAGE) {
            mImageScrollView.setVisibility(View.VISIBLE);
            mTextScrollView.setVisibility(View.GONE);

            mActivity.setLoading();
            if (mAsyncTask != null) {
                mAsyncTask.cancel(true);
            }
            mAsyncTask = new FetchPictureTask();
            mAsyncTask.execute((Void[]) null);
        } else {
            mImageScrollView.setVisibility(View.GONE);
            mTextScrollView.setVisibility(View.VISIBLE);
        }

        /* Figure out how large the color indicator should be. Medium text is 18sp, with a border its 22sp */
        int dimension = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 22, getResources().getDisplayMetrics());

        mColorIndicatorLayout.removeAllViews();
        ColorIndicatorView civ = new ColorIndicatorView(this.getActivity(), dimension, dimension/15,
                cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_COLOR)), sCost);
        if(civ.shouldInidcatorBeShown()) {
            mColorIndicatorLayout.setVisibility(View.VISIBLE);
            mColorIndicatorLayout.addView(civ);
        }
        else {
            mColorIndicatorLayout.setVisibility(View.GONE);
        }

        cCardById.close();

		/* Find the other sets this card is in ahead of time, so that it can be remove from the menu if there is only
           one set */
        Cursor cCardByName;
        try {
            cCardByName = CardDbAdapter.fetchCardByName(mCardName,
                    new String[]{
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID}, database
            );
        } catch (FamiliarDbException e) {
            handleFamiliarDbException(true);
            DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
            return;
        }
        mSets = new LinkedHashSet<>();
        mCardIds = new LinkedHashSet<>();
        while (!cCardByName.isAfterLast()) {
            try {
                if (mSets.add(CardDbAdapter
                        .getSetNameFromCode(cCardByName.getString(cCardByName.getColumnIndex(CardDbAdapter.KEY_SET)), database))) {
                    mCardIds.add(cCardByName.getLong(cCardByName.getColumnIndex(CardDbAdapter.KEY_ID)));
                }
            } catch (FamiliarDbException e) {
                handleFamiliarDbException(true);
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }
            cCardByName.moveToNext();
        }
        cCardByName.close();
        /* If it exists in only one set, remove the button from the menu */
        if (mSets.size() == 1) {
            mActivity.supportInvalidateOptionsMenu();
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);

        if (mShouldReportView) {
            reportAppIndexViewIfAble();
        }
    }

    /**
     * Used to build a meta description of this card, for app indexing
     *
     * @param tag  A tag for this data
     * @param data The data to add to the description
     */
    private void addToDescription(String tag, String data) {
        if (mDescription == null) {
            mDescription = tag + ": \"" + data + "\"";
        } else {
            mDescription += "\n" + tag + ": \"" + data + "\"";
        }
    }

    /**
     * Remove any showing dialogs, and show the requested one
     *
     * @param id the ID of the dialog to show
     */
    private void showDialog(final int id) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

		/* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

		/* Create and show the dialog. */
        final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

            @NotNull
            @Override
            @SuppressWarnings("SpellCheckingInspection")
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                super.onCreateDialog(savedInstanceState);

				/* This will be set to false if we are returning a null dialog. It prevents a crash */
                setShowsDialog(true);

                switch (id) {
                    case GET_IMAGE: {
                        if (mCardBitmap == null) {
                            return DontShowDialog();
                        }

                        Dialog dialog = new Dialog(mActivity);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                        dialog.setContentView(R.layout.card_view_image_dialog);

                        ImageView dialogImageView = (ImageView) dialog.findViewById(R.id.cardimage);
                        dialogImageView.setImageDrawable(mCardBitmap);

                        dialogImageView.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                if (mAsyncTask != null) {
                                    mAsyncTask.cancel(true);
                                }
                                mAsyncTask = new saveCardImageTask();
                                mAsyncTask.execute((Void[]) null);
                                return true;
                            }
                        });

                        return dialog;
                    }
                    case GET_LEGALITY: {
                        if (mFormats == null || mLegalities == null) {
                            /* exception handled in AsyncTask */
                            return DontShowDialog();
                        }

						/* create the item mapping */
                        String[] from = new String[]{"format", "status"};
                        int[] to = new int[]{R.id.format, R.id.status};

						/* prepare the list of all records */
                        List<HashMap<String, String>> fillMaps = new ArrayList<>();
                        for (int i = 0; i < mFormats.length; i++) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put(from[0], mFormats[i]);
                            map.put(from[1], mLegalities[i]);
                            fillMaps.add(map);
                        }

                        SimpleAdapter adapter = new SimpleAdapter(mActivity, fillMaps, R.layout.card_view_legal_row,
                                from, to);
                        ListView lv = new ListView(mActivity);
                        lv.setAdapter(adapter);

                        AlertDialogPro.Builder builder = new AlertDialogPro.Builder(mActivity);
                        builder.setView(lv);
                        builder.setTitle(R.string.card_view_legality);
                        return builder.create();
                    }
                    case GET_PRICE: {
                        if (mPriceInfo == null) {
                            return DontShowDialog();
                        }

                        View v = mActivity.getLayoutInflater().inflate(R.layout.card_view_price_dialog, null, false);

                        assert v != null; /* Because Android Studio */
                        TextView l = (TextView) v.findViewById(R.id.low);
                        TextView m = (TextView) v.findViewById(R.id.med);
                        TextView h = (TextView) v.findViewById(R.id.high);
                        TextView f = (TextView) v.findViewById(R.id.foil);
                        TextView priceLink = (TextView) v.findViewById(R.id.pricelink);

                        l.setText(String.format("$%1$,.2f", mPriceInfo.mLow));
                        m.setText(String.format("$%1$,.2f", mPriceInfo.mAverage));
                        h.setText(String.format("$%1$,.2f", mPriceInfo.mHigh));

                        if (mPriceInfo.mFoilAverage != 0) {
                            f.setText(String.format("$%1$,.2f", mPriceInfo.mFoilAverage));
                        } else {
                            f.setVisibility(View.GONE);
                            v.findViewById(R.id.foil_label).setVisibility(View.GONE);
                        }
                        priceLink.setMovementMethod(LinkMovementMethod.getInstance());
                        priceLink.setText(ImageGetterHelper.formatHtmlString("<a href=\"" + mPriceInfo.mUrl + "\">" +
                                getString(R.string.card_view_price_dialog_link) + "</a>"));

                        AlertDialogPro.Builder adb = new AlertDialogPro.Builder(mActivity);
                        adb.setView(v);
                        adb.setTitle(R.string.card_view_price_dialog_title);
                        return adb.create();
                    }
                    case CHANGE_SET: {
                        final String[] aSets = mSets.toArray(new String[mSets.size()]);
                        final Long[] aIds = mCardIds.toArray(new Long[mCardIds.size()]);

                        /* Sanity check */
                        for (String set : aSets) {
                            if (set == null) {
                                return DontShowDialog();
                            }
                        }
                        AlertDialogPro.Builder builder = new AlertDialogPro.Builder(mActivity);
                        builder.setTitle(R.string.card_view_set_dialog_title);
                        builder.setItems(aSets, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int item) {
                                setInfoFromID(aIds[item]);
                            }
                        });
                        return builder.create();
                    }
                    case CARD_RULINGS: {
                        if (mRulingsArrayList == null) {
                            return DontShowDialog();
                        }
                        ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getActivity());

                        View v = mActivity.getLayoutInflater().inflate(R.layout.card_view_rulings_dialog, null, false);
                        assert v != null; /* Because Android Studio */

                        TextView textViewRules = (TextView) v.findViewById(R.id.rules);
                        TextView textViewUrl = (TextView) v.findViewById(R.id.url);

                        String message = "";
                        if (mRulingsArrayList.size() == 0) {
                            message = getString(R.string.card_view_no_rulings);
                        } else {
                            for (Ruling r : mRulingsArrayList) {
                                message += (r.toString() + "<br><br>");
                            }

                            message = message.replace("{Tap}", "{T}");
                        }
                        CharSequence messageGlyph = ImageGetterHelper.formatStringWithGlyphs(message, imgGetter);

                        textViewRules.setText(messageGlyph);

                        textViewUrl.setMovementMethod(LinkMovementMethod.getInstance());
                        textViewUrl.setText(Html.fromHtml(
                                "<a href=http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" +
                                        mMultiverseId + ">" + getString(R.string.card_view_gatherer_page) + "</a>"
                        ));

                        AlertDialogPro.Builder builder = new AlertDialogPro.Builder(mActivity);
                        builder.setTitle(R.string.card_view_rulings_dialog_title);
                        builder.setView(v);
                        return builder.create();
                    }
                    case WISH_LIST_COUNTS: {
                        Dialog dialog = WishlistHelpers.getDialog(mCardName, CardViewFragment.this, false);
                        if (dialog == null) {
                            handleFamiliarDbException(false);
                            return DontShowDialog();
                        }
                        return dialog;
                    }
                    default: {
                        return DontShowDialog();
                    }
                }
            }
        };
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Called when a registered view is long-pressed. The menu inflated will give different options based on the view class
     *
     * @param menu     The context menu that is being built
     * @param v        The view for which the context menu is being built
     * @param menuInfo Extra information about the item for which the context menu should be shown. This information
     *                 will vary depending on the class of v.
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
     * Copies text to the clipboard
     *
     * @param item The context menu item that was selected.
     * @return boolean Return false to allow normal context menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (getUserVisibleHint()) {
            String copyText;
            switch (item.getItemId()) {
                case R.id.copy: {
                    copyText = mCopyString;
                    break;
                }
                case R.id.copyall: {
                    assert mNameTextView.getText() != null; /* Because Android Studio */
                    assert mCostTextView.getText() != null;
                    assert mTypeTextView.getText() != null;
                    assert mSetTextView.getText() != null;
                    assert mAbilityTextView.getText() != null;
                    assert mFlavorTextView.getText() != null;
                    assert mPowTouTextView.getText() != null;
                    assert mArtistTextView.getText() != null;

                    copyText = mNameTextView.getText().toString() + '\n' + mCostTextView.getText().toString() + '\n' +
                            mTypeTextView.getText().toString() + '\n' + mSetTextView.getText().toString() + '\n' +
                            mAbilityTextView.getText().toString() + '\n' + mFlavorTextView.getText().toString() + '\n' +
                            mPowTouTextView.getText().toString() + '\n' + mArtistTextView.getText().toString();
                    break;
                }
                default: {
                    return super.onContextItemSelected(item);
                }
            }

            ClipboardManager clipboard = (ClipboardManager) (this.mActivity.
                    getSystemService(android.content.Context.CLIPBOARD_SERVICE));
            String label = getResources().getString(R.string.app_name);
            String mimeTypes[] = {ClipDescription.MIMETYPE_TEXT_PLAIN};
            ClipData cd = new ClipData(label, mimeTypes, new ClipData.Item(copyText));
            clipboard.setPrimaryClip(cd);
            return true;
        }
        return false;
    }

    /**
     * Handles clicks from the ActionBar
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
                if (getFamiliarActivity().getNetworkState(true) == -1) {
                    return true;
                }

                mActivity.setLoading();
                if (mAsyncTask != null) {
                    mAsyncTask.cancel(true);
                }
                mAsyncTask = new FetchPictureTask();
                mAsyncTask.execute((Void[]) null);
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
                                    ToastWrapper.makeText(mActivity, spiceException.getMessage(), ToastWrapper.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onRequestSuccess(final PriceInfo result) {
                                if (CardViewFragment.this.isAdded()) {
                                    mActivity.clearLoading();

                                    if (result != null) {
                                        mPriceInfo = result;
                                        showDialog(GET_PRICE);
                                    } else {
                                        ToastWrapper.makeText(mActivity, R.string.card_view_price_not_found,
                                                ToastWrapper.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                );

                return true;
            }
            case R.id.changeset: {
                showDialog(CHANGE_SET);
                return true;
            }
            case R.id.legality: {
                mActivity.setLoading();
                if (mAsyncTask != null) {
                    mAsyncTask.cancel(true);
                }
                mAsyncTask = new FetchLegalityTask();
                mAsyncTask.execute((Void[]) null);
                return true;
            }
            case R.id.cardrulings: {
                if (getFamiliarActivity().getNetworkState(true) == -1) {
                    return true;
                }

                mActivity.setLoading();
                if (mAsyncTask != null) {
                    mAsyncTask.cancel(true);
                }
                mAsyncTask = new FetchRulingsTask();
                mAsyncTask.execute((Void[]) null);
                return true;
            }
            case R.id.addtowishlist: {
                showDialog(WISH_LIST_COUNTS);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Inflate the ActionBar items
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
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     * @see #setHasOptionsMenu
     * @see #onCreateOptionsMenu
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem mi;
		/* If the image has been loaded to the main page, remove the menu option for image */
        if (loadTo == MAIN_PAGE && mCardBitmap != null) {
            mi = menu.findItem(R.id.image);
            if (mi != null) {
                menu.removeItem(mi.getItemId());
            }
        }
		/* This code removes the "change set" button if there is only one set.
		 * Turns out some users use it to view the full set name when there is only one set/
		 * I'm leaving it here, but commented, for posterity */
		/*
 		if (mSets != null && mSets.size() == 1) {
			mi = menu.findItem(R.id.changeset);
			if (mi != null) {
				menu.removeItem(mi.getItemId());
			}
		}
		*/
    }

    /**
     * This inner class encapsulates a ruling and the date it was made
     */
    private static class Ruling {
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

    class saveCardImageTask extends AsyncTask<Void, Void, Void> {
        String mToastString;

        @Override
        protected Void doInBackground(Void... voids) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                mToastString = getString(R.string.card_view_no_external_storage);
                return null;
            }

            String strPath;

            try {
                strPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        .getCanonicalPath() + "/MTGFamiliar";
            } catch (IOException ex) {
                mToastString = getString(R.string.card_view_no_pictures_folder);
                return null;
            }

            File fPath = new File(strPath);

            if (!fPath.exists()) {
                fPath.mkdir();

                if (!fPath.isDirectory()) {
                    mToastString = getString(R.string.card_view_unable_to_create_dir);
                    return null;
                }
            }

            fPath = new File(strPath, mCardName + "_" + mSetCode + ".jpg");

            if (fPath.exists()) {
                fPath.delete();
            }

            try {
                if (!fPath.createNewFile()) {
                    mToastString = getString(R.string.card_view_unable_to_create_file);
                    return null;
                }

                FileOutputStream fStream = new FileOutputStream(fPath);

				/* If the card is displayed, there's a real good chance it's cached */
                String cardLanguage = mActivity.mPreferenceAdapter.getCardLanguage();
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
                    mToastString = getString(R.string.card_view_no_image);
                    return null;
                }

                boolean bCompressed = bmpImage.compress(Bitmap.CompressFormat.JPEG, 90, fStream);

                if (!bCompressed) {
                    mToastString = getString(R.string.card_view_unable_to_save_image);
                    return null;
                }

                strPath = fPath.getCanonicalPath();
            } catch (IOException ex) {
                mToastString = getString(R.string.card_view_save_failure);
                return null;
            }

            mToastString = getString(R.string.card_view_image_saved) + strPath;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mToastString != null) {
                ToastWrapper.makeText(mActivity, mToastString, ToastWrapper.LENGTH_LONG).show();
            }
        }
    }

    /**
     * This private class handles asking the database about the legality of a card, and will eventually show the
     * information in a Dialog
     */
    private class FetchLegalityTask extends AsyncTask<Void, Void, Void> {

        /**
         * Queries the data in the database to see what sets this card is legal in
         *
         * @param params unused
         * @return unused
         */
        @Override
        protected Void doInBackground(Void... params) {

            SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
            try {
                Cursor cFormats = CardDbAdapter.fetchAllFormats(database);
                mFormats = new String[cFormats.getCount()];
                mLegalities = new String[cFormats.getCount()];

                cFormats.moveToFirst();
                for (int i = 0; i < cFormats.getCount(); i++) {
                    mFormats[i] = cFormats.getString(cFormats.getColumnIndex(CardDbAdapter.KEY_NAME));
                    switch (CardDbAdapter.checkLegality(mCardName, mFormats[i], database)) {
                        case CardDbAdapter.LEGAL:
                            mLegalities[i] = getString(R.string.card_view_legal);
                            break;
                        case CardDbAdapter.RESTRICTED:
							/* For backwards compatibility, we list cards that are legal
							 * in commander, but can't be the commander as Restricted in
							 * the legality file.  This prevents older version of the app
							 * from throwing an IllegalStateException if we try including
							 * a new legality. */
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
         * After the query, remove the progress dialog and show the legalities
         *
         * @param result unused
         */
        @Override
        protected void onPostExecute(Void result) {
            try {
                showDialog(GET_LEGALITY);
            } catch (IllegalStateException e) {
				/* eat it */
            }
            mActivity.clearLoading();
        }
    }

    /**
     * This private class retrieves a picture of the card from the internet
     */
    private class FetchPictureTask extends AsyncTask<Void, Void, Void> {

        private String error;

        int mHeight;
        int mWidth;
        int mBorder;

        /* Get the size of the window on the UI thread, not the worker thread */
        final Runnable getWindowSize = new Runnable() {
            @Override
            public void run() {
                Rect rectangle = new Rect();
                mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);

                assert mActivity.getSupportActionBar() != null; /* Because Android Studio */
                mHeight = ((rectangle.bottom - rectangle.top) - mActivity.getSupportActionBar().getHeight()) - mBorder;
                mWidth = (rectangle.right - rectangle.left) - mBorder;

                synchronized (this) {
                    this.notify();
                }
            }
        };

        /**
         * First check www.MagicCards.info for the card image in the user's preferred language
         * If that fails, check www.MagicCards.info for the card image in English
         * If that fails, check www.gatherer.wizards.com for the card image
         * If that fails, give up
         * There is non-standard URL building for planes and schemes
         * It also re-sizes the image
         *
         * @param params unused
         * @return unused
         */
        @SuppressWarnings("SpellCheckingInspection")
        @Override
        protected Void doInBackground(Void... params) {

            String cardLanguage = mActivity.mPreferenceAdapter.getCardLanguage();
            if (cardLanguage == null) {
                cardLanguage = "en";
            }

            final String imageKey = Integer.toString(mMultiverseId) + cardLanguage;

            /* Check disk cache in background thread */
            Bitmap bitmap;
            try {
                bitmap = getFamiliarActivity().mImageCache.getBitmapFromDiskCache(imageKey);
            } catch (NullPointerException e) {
                bitmap = null;
            }

            if (bitmap == null) { /* Not found in disk cache */

                boolean bRetry = true;

                boolean triedMtgi = false;
                boolean triedGatherer = false;

                while (bRetry) {

                    bRetry = false;
                    error = null;

                    try {

                        URL u;
                        if (!cardLanguage.equalsIgnoreCase("en")) {
                            u = new URL(getMtgiPicUrl(mCardName, mMagicCardsInfoSetCode, mCardNumber, cardLanguage));
                            cardLanguage = "en";
                        } else {
                            if (!triedMtgi) {
                                u = new URL(getMtgiPicUrl(mCardName, mMagicCardsInfoSetCode, mCardNumber, cardLanguage));
                                triedMtgi = true;
                            } else {
                                u = new URL("http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid="
                                        + mMultiverseId + "&type=card");
                                triedGatherer = true;
                            }
                        }

                        mCardBitmap = new RecyclingBitmapDrawable(mActivity.getResources(), BitmapFactory.decodeStream(u.openStream()));
                        bitmap = mCardBitmap.getBitmap();
                        getFamiliarActivity().mImageCache.addBitmapToCache(imageKey, mCardBitmap);

                    } catch (Exception e) {
						/* Something went wrong */
                        try {
                            error = getString(R.string.card_view_image_not_found);
                        } catch (RuntimeException re) {
                            /* in case the fragment isn't attached to an activity */
                            error = e.toString();
                        }
                        if (!triedGatherer) {
                            bRetry = true;
                        }
                    }
                }
            }

            if (bitmap == null) {
                return null;
            }

            try {
				/* 16dp */
                mBorder = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
                if (loadTo == MAIN_PAGE) {
                    /* Block the worker thread until the size is figured out */
                    synchronized (getWindowSize) {
                        getActivity().runOnUiThread(getWindowSize);
                        getWindowSize.wait();
                    }
                } else if (loadTo == DIALOG) {
                    Display display = ((WindowManager) mActivity
                            .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    Point p = new Point();
                    display.getSize(p);
                    mHeight = p.y - mBorder;
                    mWidth = p.x - mBorder;
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

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true); /* todo this is leaky? */
                mCardBitmap = new RecyclingBitmapDrawable(mActivity.getResources(), scaledBitmap);
                bitmap.recycle();
            } catch (Exception e) {
				/* Some error resizing. Out of memory? */
            }
            return null;
        }

        /**
         * Jumps through hoops and returns a correctly formatted URL for magiccards.info's image
         *
         * @param cardName              The name of the card
         * @param magicCardsInfoSetCode The set of the card
         * @param cardNumber            The number of the card
         * @param cardLanguage          The language of the card
         * @return a URL to the card's image
         */
        private String getMtgiPicUrl(String cardName, String magicCardsInfoSetCode, String cardNumber,
                                     String cardLanguage) {
            String picURL;
            if (mCardType.toLowerCase().contains(getString(R.string.search_Ongoing).toLowerCase()) ||
                    /* extra space to not confuse with planeswalker */
                    mCardType.toLowerCase().contains(getString(R.string.search_Plane).toLowerCase() + " ") ||
                    mCardType.toLowerCase().contains(getString(R.string.search_Phenomenon).toLowerCase()) ||
                    mCardType.toLowerCase().contains(getString(R.string.search_Scheme).toLowerCase())) {
                switch (mSetCode) {
                    case "PC2":
                        picURL = "http://magiccards.info/extras/plane/planechase-2012-edition/" + cardName + ".jpg";
                        picURL = picURL.replace(" ", "-").replace(Character.toChars(0xC6)[0] + "", "Ae")
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
                        picURL = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
                        picURL = picURL.replace(" ", "-").replace(Character.toChars(0xC6)[0] + "", "Ae")
                                .replace("?", "").replace(",", "").replace("'", "").replace("!", "");
                        break;
                    case "ARC":
                        picURL = "http://magiccards.info/extras/scheme/archenemy/" + cardName + ".jpg";
                        picURL = picURL.replace(" ", "-").replace(Character.toChars(0xC6)[0] + "", "Ae")
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
         * When the task has finished, if there was no error, remove the progress dialog and show the image
         * If the image was supposed to load to the main screen, and it failed to load, fall back to text view
         *
         * @param result unused
         */
        @Override
        protected void onPostExecute(Void result) {
            if (error == null) {
                if (loadTo == DIALOG) {
                    try {
                        showDialog(GET_IMAGE);
                    } catch (IllegalStateException e) {
						/* eat it */
                    }
                } else if (loadTo == MAIN_PAGE) {
                    removeDialog(getFragmentManager());
                    mCardImageView.setImageDrawable(mCardBitmap);
					/* remove the image load button if it is the main page */
                    mActivity.supportInvalidateOptionsMenu();
                }
            } else {
                removeDialog(getFragmentManager());
                if (loadTo == MAIN_PAGE) {
                    mImageScrollView.setVisibility(View.GONE);
                    mTextScrollView.setVisibility(View.VISIBLE);
                }
                ToastWrapper.makeText(mActivity, error, ToastWrapper.LENGTH_LONG).show();
            }
            mActivity.clearLoading();
        }

        /**
         * If the task is canceled, fall back to text view
         */
        @Override
        protected void onCancelled() {
            if (loadTo == MAIN_PAGE) {
                mImageScrollView.setVisibility(View.GONE);
                mTextScrollView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * This private class fetches rulings about this card from gatherer.wizards.com
     */
    private class FetchRulingsTask extends AsyncTask<Void, Void, Void> {

        String mErrorMessage = null;

        /**
         * This function downloads the source of the gatherer page, scans it for rulings, and stores them for display
         *
         * @param params unused
         * @return unused
         */
        @Override
        @SuppressWarnings("SpellCheckingInspection")
        protected Void doInBackground(Void... params) {

            URL url;
            InputStream is = null;
            BufferedReader br;
            String line;

            mRulingsArrayList = new ArrayList<>();

            try {
                url = new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + mMultiverseId);
                is = url.openStream(); /* throws an IOException */
                br = new BufferedReader(new InputStreamReader(new BufferedInputStream(is)));

                String date = null, ruling;
                while ((line = br.readLine()) != null) {
                    if (line.contains("rulingDate") && line.contains("<td")) {
                        date = (line.replace("<autocard>", "").replace("</autocard>", ""))
                                .split(">")[1].split("<")[0];
                    }
                    if (line.contains("rulingText") && line.contains("<td")) {
                        ruling = (line.replace("<autocard>", "").replace("</autocard>", ""))
                                .split(">")[1].split("<")[0];
                        Ruling r = new Ruling(date, ruling);
                        mRulingsArrayList.add(r);
                    }
                }
            } catch (IOException ioe) {
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
         * Hide the progress dialog and show the rulings, if there are no errors
         *
         * @param result unused
         */
        @Override
        protected void onPostExecute(Void result) {

            if (mErrorMessage == null) {
                try {
                    showDialog(CARD_RULINGS);
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
}