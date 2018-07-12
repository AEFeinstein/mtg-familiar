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

import android.Manifest;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.Language;
import com.gelakinetic.mtgfam.BuildConfig;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.CardViewDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.helpers.ColorIndicatorView;
import com.gelakinetic.mtgfam.helpers.FamiliarGlideTarget;
import com.gelakinetic.mtgfam.helpers.GlideApp;
import com.gelakinetic.mtgfam.helpers.GlideRequest;
import com.gelakinetic.mtgfam.helpers.GlideRequests;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

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
import java.util.Collections;
import java.util.LinkedHashSet;

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
    public static final int SHARE = 3;
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
    private LinearLayout mCardTextLinearLayout;
    private LinearLayout mColorIndicatorLayout;

    /* the AsyncTask loads stuff off the UI thread, and stores whatever in these local variables */
    private AsyncTask mAsyncTask;
    public String[] mLegalities;
    public String[] mFormats;
    public ArrayList<Ruling> mRulingsArrayList;

    /* Loaded in a Spice Service */
    public MarketPriceInfo mPriceInfo;
    public String mErrorMessage;

    /* Card info, used to build the URL to fetch the picture */
    public MtgCard mCard;

    /* Card info used to flip the card */
    private String mTransformCardNumber;
    private int mTransformId;

    /* To switch card between printings */
    public LinkedHashSet<String> mPrintings;
    public LinkedHashSet<Long> mCardIds;

    /* Easier than calling getActivity() all the time, and handles being nested */
    public FamiliarActivity mActivity;

    /* When requesting a permission, save what to do after the permission is granted */
    private int mSaveImageWhereTo = MAIN_PAGE;

    /* Objects dealing with loading images so they can be released later */
    private GlideRequests mGlideRequestManager = null;
    private Target mGlideTarget = null;
    private Drawable mDrawableForDialog = null;
    private boolean mIsOnlineOnly = false;

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
            @NonNull LayoutInflater inflater,
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
        mCardTextLinearLayout = myFragmentView.findViewById(R.id.CardTextLinearLayout);
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

        mSetTextView.setOnClickListener(v -> {
            SearchCriteria setSearch = new SearchCriteria();
            assert mSetTextView.getText() != null;
            setSearch.sets = Collections.singletonList(mSetTextView.getText().toString());
            Bundle arguments = new Bundle();
            arguments.putBoolean(SearchViewFragment.CRITERIA_FLAG, true);
            PreferenceAdapter.setSearchCriteria(getContext(), setSearch);
            ResultListFragment rlFrag = new ResultListFragment();
            startNewFragment(rlFrag, arguments);
        });

        mCardImageView.setOnLongClickListener(view -> {
            saveImageWithGlide(MAIN_PAGE);
            return true;
        });

        setInfoFromBundle(this.getArguments());

        /* Uncomment this to test memory issues due to loading images
        final long cardId = this.getArguments().getLong(CARD_ID);
        Log.e("LOAD", cardId + "");
        Handler myHandler = new Handler();
        myHandler.postDelayed(() -> {
            // search another card
            Bundle args = new Bundle();
            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, new long[]{cardId + 1});
            CardViewPagerFragment cardViewPagerFragment = new CardViewPagerFragment();
            startNewFragment(cardViewPagerFragment, args);
        }, 2500);
        */

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
    private void releaseImageResources(boolean isSplit) {

        // Have Glide release any image resources
        if (null != mGlideRequestManager && null != mGlideTarget) {
            mGlideRequestManager.clear(mGlideTarget);
        }

        // Clear the image view too
        if (mCardImageView != null) {
            mCardImageView.setImageDrawable(null);
            mCardImageView.setImageBitmap(null);
        }

        // For non-split cards, null out all UI elements
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
            mCardTextLinearLayout = null;
            mCardImageView = null;
            mColorIndicatorLayout = null;
        }

        mDrawableForDialog = null;
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

        ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(mActivity);

        Cursor cCardById = null;
        Cursor cCardByName = null;
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            SQLiteDatabase database = DatabaseManager.openDatabase(mActivity, false, handle);
            cCardById = CardDbAdapter.fetchCards(new long[]{id}, null, database);

            /* http://magiccards.info/scans/en/mt/55.jpg */
            mCard = new MtgCard(database, cCardById);

            switch (mCard.getRarity()) {
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

            mCostTextView.setText(ImageGetterHelper.formatStringWithGlyphs(mCard.getManaCost(), imgGetter));

            mAbilityTextView.setText(ImageGetterHelper.formatStringWithGlyphs(mCard.getText(), imgGetter));
            mAbilityTextView.setMovementMethod(LinkMovementMethod.getInstance());

            mFlavorTextView.setText(ImageGetterHelper.formatStringWithGlyphs(mCard.getFlavor(), imgGetter));

            mNameTextView.setText(mCard.getName());
            mTypeTextView.setText(mCard.getType());
            mSetTextView.setText(mCard.getExpansion());
            mArtistTextView.setText(mCard.getArtist());
            String numberAndRarity = mCard.getNumber() + " (" + mCard.getRarity() + ")";
            mNumberTextView.setText(numberAndRarity);

            int loyalty = mCard.getLoyalty();
            float p = mCard.getPower();
            float t = mCard.getToughness();
            if (loyalty != CardDbAdapter.NO_ONE_CARES) {
                mPowTouTextView.setText(CardDbAdapter.getPrintedPTL(loyalty, false));
            } else if (p != CardDbAdapter.NO_ONE_CARES && t != CardDbAdapter.NO_ONE_CARES) {
                boolean shouldShowSign = mCard.getText().contains("Augment {") && mSetTextView.getText().equals("UST");
                mPowTouTextView.setText(CardDbAdapter.getPrintedPTL(p, shouldShowSign) + "/" + CardDbAdapter.getPrintedPTL(t, shouldShowSign));
            } else {
                mPowTouTextView.setText("");
            }

            boolean isMultiCard = false;
            switch (CardDbAdapter.isMultiCard(mCard.getNumber(), mCard.getExpansion())) {
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
                if (mCard.getNumber().contains("a")) {
                    mTransformCardNumber = mCard.getNumber().replace("a", "b");
                } else if (mCard.getNumber().contains("b")) {
                    mTransformCardNumber = mCard.getNumber().replace("b", "a");
                }
                mTransformId = CardDbAdapter.getIdFromSetAndNumber(mCard.getExpansion(), mTransformCardNumber, database);
                if (mTransformId == -1) {
                    mTransformButton.setVisibility(View.GONE);
                    mTransformButtonDivider.setVisibility(View.GONE);
                } else {
                    mTransformButton.setOnClickListener(v -> {
                        releaseImageResources(true);
                        setInfoFromID(mTransformId);
                    });
                }
            }

            /* Do we load the image immediately to the main page, or do it in a dialog later? */
            if (PreferenceAdapter.getPicFirst(getContext())) {
                mCardImageView.setVisibility(View.VISIBLE);
                mCardTextLinearLayout.setVisibility(View.GONE);

                // Load the image with Glide
                loadImageWithGlide(mCardImageView, false);
            } else {
                mCardImageView.setVisibility(View.GONE);
                mCardTextLinearLayout.setVisibility(View.VISIBLE);
            }

            /* Figure out how large the color indicator should be. Medium text is 18sp, with a border
             * its 22sp */
            int dimension = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 22, getResources().getDisplayMetrics());

            mColorIndicatorLayout.removeAllViews();
            ColorIndicatorView civ =
                    new ColorIndicatorView(this.mActivity, dimension, dimension / 15,
                            mCard.getColor(), mCard.getManaCost());
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
            mCard.getForeignPrintings().clear();

            // Add English
            Card.ForeignPrinting englishPrinting = new Card.ForeignPrinting(mCard.getName(), Language.English, mCard.getMultiverseId());
            mCard.getForeignPrintings().add(englishPrinting);

            // Add all the others
            for (String lang[] : allLanguageKeys) {
                Card.ForeignPrinting fp = new Card.ForeignPrinting(
                        cCardById.getString(cCardById.getColumnIndex(lang[1])), lang[0],
                        cCardById.getInt(cCardById.getColumnIndex(lang[2])));
                if (fp.getName() != null && !fp.getName().isEmpty()) {
                    mCard.getForeignPrintings().add(fp);
                }
            }

            mIsOnlineOnly = CardDbAdapter.isOnlineOnly(mCard.getExpansion(), database);

            /* Find the other sets this card is in ahead of time, so that it can be remove from the menu
             * if there is only one set */
            cCardByName = CardDbAdapter.fetchCardByName(mCard.getName(),
                    Arrays.asList(
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_SET,
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID,
                            CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_NUMBER), false, false, database
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
        } catch (SQLiteException | FamiliarDbException | CursorIndexOutOfBoundsException e) {
            handleFamiliarDbException(true);
        } finally {
            if (null != cCardById) {
                cCardById.close();
            }
            if (null != cCardByName) {
                cCardByName.close();
            }
            DatabaseManager.closeDatabase(mActivity, handle);
        }
        mActivity.invalidateOptionsMenu();
    }

    /**
     * Load and resize an image of this card using Glide
     *
     * @param cardImageView The ImageView to load the image into
     */
    private void loadImageWithGlide(ImageView cardImageView, boolean shouldScale) {

        int width = 0;
        int height = 0;
        // Get screen dimensions
        if (shouldScale) {
            int mBorder = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics());
            View toMeasure = getFamiliarActivity().findViewById(R.id.drawer_layout);
            width = toMeasure.getWidth() - mBorder;
            height = toMeasure.getHeight() - mBorder;
        }
        // Load the image
        runGlideTarget(new FamiliarGlideTarget(this, cardImageView), width, height);
    }

    /**
     * Load and save or share an image of this card using Glide
     *
     * @param whereTo What to do with this image. Either SHARE to share it, or MAIN_PAGE to save it
     *                to the disk
     */
    public void saveImageWithGlide(int whereTo) {

        // Check that there's memory to save the image to
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            SnackbarWrapper.makeAndShowText(getActivity(), R.string.card_view_no_external_storage, SnackbarWrapper.LENGTH_SHORT);
            return;
        }

        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(CardViewFragment.this.mActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(CardViewFragment.this.mActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    FamiliarActivity.REQUEST_WRITE_EXTERNAL_STORAGE_IMAGE);
            // Wait for the permission to be granted
            mSaveImageWhereTo = whereTo;
            return;
        }

        // Get a File where the image should be saved
        File imageFile;
        try {
            imageFile = getSavedImageFile();
        } catch (Exception e) {
            SnackbarWrapper.makeAndShowText(getActivity(), e.getMessage(), SnackbarWrapper.LENGTH_SHORT);
            return;
        }

        // Check if the saved image already exists
        if (imageFile.exists()) {
            if (SHARE == whereTo) {
                // Image is already saved, just share it
                shareImage();
            } else {
                // Or display the path where it's saved
                String strPath = imageFile.getAbsolutePath();
                SnackbarWrapper.makeAndShowText(getActivity(), getString(R.string.card_view_image_saved) + strPath, SnackbarWrapper.LENGTH_LONG);
            }
            return;
        }

        runGlideTarget(new FamiliarGlideTarget(this, new FamiliarGlideTarget.DrawableLoadedCallback() {
            /**
             * When Glide loads the resource either from cache or the network, save it
             * to a file then optionally launch the intent to share it
             *
             * @param resource The Drawable Glide loaded, hopefully a BitmapDrawable
             */
            @Override
            protected void onDrawableLoaded(Drawable resource) {
                if (resource instanceof BitmapDrawable) {
                    // Save the image
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) resource;

                    try {
                        // Create the file
                        if (!imageFile.createNewFile()) {
                            // Couldn't create the file
                            SnackbarWrapper.makeAndShowText(getActivity(), R.string.card_view_unable_to_create_file, SnackbarWrapper.LENGTH_SHORT);
                            return;
                        }

                        // Now that the file is created, write to it
                        FileOutputStream fStream = new FileOutputStream(imageFile);
                        boolean bCompressed = bitmapDrawable.getBitmap().compress(Bitmap.CompressFormat.JPEG, 90, fStream);
                        fStream.flush();
                        fStream.close();

                        // Couldn't save the image for some reason
                        if (!bCompressed) {
                            SnackbarWrapper.makeAndShowText(getActivity(), R.string.card_view_save_failure, SnackbarWrapper.LENGTH_SHORT);
                            return;
                        }
                    } catch (IOException e) {
                        // Couldn't save it for some reason
                        SnackbarWrapper.makeAndShowText(getActivity(), R.string.card_view_save_failure, SnackbarWrapper.LENGTH_SHORT);
                        return;
                    }

                    // Notify the system that a new image was saved
                    getFamiliarActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(imageFile)));

                    // Now that the image is saved, launch the intent
                    if (SHARE == whereTo) {
                        // Image is already saved, just share it
                        shareImage();
                    } else {
                        // Or display the path where it's saved
                        String strPath = imageFile.getAbsolutePath();
                        SnackbarWrapper.makeAndShowText(getActivity(), getString(R.string.card_view_image_saved) + strPath, SnackbarWrapper.LENGTH_LONG);
                    }
                }
            }
        }), 0, 0);
    }

    /**
     * Helper function to create and run a glide target. Handles creating the request manager, if
     * it doesn't exist
     *
     * @param familiarGlideTarget The target to load the image into
     * @param width               0 to do nothing or a positive number to resize the image
     * @param height              0 to do nothing or a positive number to resize the image
     */
    private void runGlideTarget(FamiliarGlideTarget familiarGlideTarget, int width, int height) {
        // Get the language this card should be in
        String cardLanguage = PreferenceAdapter.getCardLanguage(getContext());
        if (cardLanguage == null) {
            cardLanguage = "en";
        }

        // Try downloading the image
        if (null == mGlideRequestManager) {
            mGlideRequestManager = GlideApp.with(this);
        } else {
            if (null != mGlideTarget) {
                mGlideRequestManager.clear(mGlideTarget);
                mGlideTarget = null;
            }
        }

        // Run the first request.
        mGlideTarget = runGlideRequest(0, cardLanguage, width, height, true, familiarGlideTarget);
    }

    /**
     * Helper function to run a glide request
     *
     * @param attempt        The number of this attempt
     * @param cardLanguage   The language of the card to load
     * @param width          0 to do nothing or a positive number to resize the image
     * @param height         0 to do nothing or a positive number to resize the image
     * @param onlyCheckCache true to only check the cache, false to check the network too
     * @param target         The target to load the image into
     * @return The built glide request
     */
    private Target<Drawable> runGlideRequest(int attempt, String cardLanguage, int width, int height,
                                             boolean onlyCheckCache, Target<Drawable> target) {

        // Build the initial request
        GlideRequest<Drawable> request = mGlideRequestManager
                .load(mCard.getImageUrlString(attempt, cardLanguage, getContext()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        // Peek at the next URL
                        if (null == mCard.getImageUrlString(attempt + 1, cardLanguage, getContext())) {
                            // All lookups failed
                            if (onlyCheckCache) {
                                // It's only checking the cache. This comes first
                                if (FamiliarActivity.getNetworkState(getActivity(), true) == -1) {
                                    // Done checking the cache, and there's no network, return false
                                    return false;
                                } else {
                                    // Done checking the cache, but there is network, try to look there starting with the 0th attempt
                                    (new Handler()).post(() -> runGlideRequest(0, cardLanguage, width, height, false, target));
                                    return true;
                                }
                            } else {
                                // Not only checking the cache, and all lookups failed.
                                // Return false to let FamiliarGlideTarget take care of it
                                return false;
                            }
                        } else {
                            // Otherwise post a runnable to try the next load
                            (new Handler()).post(() -> runGlideRequest(attempt + 1, cardLanguage, width, height, onlyCheckCache, target));
                            // Return true so FamiliarGlideTarget doesn't call onLoadFailed()
                            return true;
                        }
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        // Don't do anything
                        return false;
                    }
                });

        // Only check the cache if requested
        if (onlyCheckCache) {
            request = request.onlyRetrieveFromCache(true);
        }

        // Resize the request if given
        if (0 != width && 0 != height) {
            request = request
                    .override(width, height)
                    .fitCenter();
        }

        // Return the request
        return request.into(target);
    }

    /**
     * Set a temporary drawable from a Glide loader to be shown in a Dialog which
     * hasn't been created yet
     *
     * @param drawable The drawable to save
     */
    public void setImageDrawableForDialog(Drawable drawable) {
        mDrawableForDialog = drawable;
    }

    /**
     * @return A temporary drawable loaded by Glide to be shown in a Dialog
     */
    public Drawable getImageDrawable() {
        return mDrawableForDialog;
    }

    /**
     * Display the text if the image fails to load
     */
    public void showText() {
        mCardImageView.setVisibility(View.GONE);
        mCardTextLinearLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Launch the intent to share a saved image
     */
    private void shareImage() {
        /* Start the intent to share the image */
        try {
            Uri uri = FileProvider.getUriForFile(mActivity,
                    BuildConfig.APPLICATION_ID + ".FileProvider", getSavedImageFile());
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("image/jpeg");
            startActivity(Intent.createChooser(shareIntent,
                    getResources().getText(R.string.card_view_send_to)));
        } catch (Exception e) {
            SnackbarWrapper.makeAndShowText(mActivity, e.getMessage(), SnackbarWrapper.LENGTH_SHORT);
        }
    }

    /**
     * Returns the File used to save this card's image.
     *
     * @return A File, either with the image already or blank
     * @throws Exception If something goes wrong
     */
    private File getSavedImageFile() throws Exception {

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

        fPath = new File(strPath, mCard.getName() + "_" + mCard.getExpansion() + ".jpg");

        return fPath;
    }

    /**
     * Remove any showing dialogs, and show the requested one.
     *
     * @param id the ID of the dialog to show
     */
    public void showDialog(final int id) throws IllegalStateException {
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
     * consume it here.
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
                if (null != clipboard) {
                    String label = getResources().getString(R.string.app_name);
                    String mimeTypes[] = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                    ClipData cd = new ClipData(label, mimeTypes, new ClipData.Item(copyText));
                    clipboard.setPrimaryClip(cd);
                }
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
        if (mCard.getName() == null) {
            /*disable menu buttons if the card isn't initialized */
            return false;
        }
        /* Handle item selection */
        switch (item.getItemId()) {
            case R.id.image: {
                loadImageWithGlide(null, true);
                return true;
            }
            case R.id.price: {
                try {
                    mActivity.mMarketPriceStore.fetchMarketPrice(mCard,
                            marketPriceInfo -> {
                                if (CardViewFragment.this.isAdded()) {
                                    if (marketPriceInfo != null) {
                                        mPriceInfo = marketPriceInfo;
                                    } else {
                                        mPriceInfo = null;
                                        mErrorMessage = getString(R.string.card_view_price_not_found);
                                    }
                                }
                            },
                            throwable -> {
                                if (CardViewFragment.this.isAdded()) {
                                    mPriceInfo = null;
                                    mErrorMessage = throwable.getMessage();
                                }
                            },
                            () -> {
                                if (mPriceInfo == null) {
                                    // This was a failure
                                    CardViewFragment.this.removeDialog(getFragmentManager());
                                    if (null != mErrorMessage) {
                                        SnackbarWrapper.makeAndShowText(mActivity, mErrorMessage, SnackbarWrapper.LENGTH_SHORT);
                                    }
                                } else {
                                    // This was a success
                                    showDialog(CardViewDialogFragment.GET_PRICE);
                                }
                            });

                } catch (java.lang.InstantiationException e) {
                    mErrorMessage = getString(R.string.card_view_price_not_found);
                }

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
                if (FamiliarActivity.getNetworkState(getActivity(), true) == -1) {
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
            case R.id.addtodecklist: {
                showDialog(CardViewDialogFragment.ADD_TO_DECKLIST);
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

        /* If the image has been loaded to the main page, remove the menu option for image */
        if (PreferenceAdapter.getPicFirst(getContext())) {
            menu.findItem(R.id.image).setVisible(false);
        } else {
            menu.findItem(R.id.image).setVisible(true);
        }

        /* If this is an online-only card, hide the price lookup button */
        if (mIsOnlineOnly) {
            menu.findItem(R.id.price).setVisible(false);
        } else {
            menu.findItem(R.id.price).setVisible(true);
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
     * This inner class encapsulates a ruling and the date it was made.
     */
    public static class Ruling {
        final String date;
        final String ruling;

        Ruling(String d, String r) {
            date = d;
            ruling = r;
        }

        public String toString() {
            return date + ": " + ruling;
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

            Cursor cFormats = null;
            FamiliarDbHandle handle = new FamiliarDbHandle();
            try {
                SQLiteDatabase database = DatabaseManager.openDatabase(mActivity, false, handle);
                cFormats = CardDbAdapter.fetchAllFormats(database);
                mFormats = new String[cFormats.getCount()];
                mLegalities = new String[cFormats.getCount()];

                cFormats.moveToFirst();
                for (int i = 0; i < cFormats.getCount(); i++) {
                    mFormats[i] =
                            cFormats.getString(cFormats.getColumnIndex(CardDbAdapter.KEY_NAME));
                    switch (CardDbAdapter.checkLegality(mCard.getName(), mFormats[i], database)) {
                        case CardDbAdapter.LEGAL:
                            mLegalities[i] = getString(R.string.card_view_legal);
                            break;
                        case CardDbAdapter.RESTRICTED:
                            /* For backwards compatibility, we list cards that are legal in
                             * commander, but can't be the commander as Restricted in the legality
                             * file.  This prevents older version of the app from throwing an
                             * IllegalStateException if we try including a new legality. */
                            if (mFormats[i].equalsIgnoreCase("Commander") ||
                                    mFormats[i].equalsIgnoreCase("Brawl")) {
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
            } catch (SQLiteException | FamiliarDbException e) {
                CardViewFragment.this.handleFamiliarDbException(false);
                mLegalities = null;
            } finally {
                if (null != cFormats) {
                    cFormats.close();
                }
                DatabaseManager.closeDatabase(mActivity, handle);
            }

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
                url = new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + mCard.getMultiverseId());
                is = FamiliarActivity.getHttpInputStream(url, null, getContext());
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
                SnackbarWrapper.makeAndShowText(mActivity, mErrorMessage, SnackbarWrapper.LENGTH_SHORT);
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
                    saveImageWithGlide(mSaveImageWhereTo);
                } else {
                    /* Permission denied */
                    SnackbarWrapper.makeAndShowText(getActivity(), R.string.card_view_unable_to_save_image,
                            SnackbarWrapper.LENGTH_LONG);
                }
            }
        }
    }
}
