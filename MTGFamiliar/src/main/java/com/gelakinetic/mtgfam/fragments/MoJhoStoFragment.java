package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * This fragment helps user play Momir Stonehewer Jhoira basic, a variant format usually found online. It contains
 * the rules, as well as ways to pick random cards
 */
public class MoJhoStoFragment extends FamiliarFragment {

    /* Dialog Constants */
    private static final int DIALOG_RULES = 1;
    private static final int DIALOG_MOMIR = 2;
    private static final int DIALOG_STONEHEWER = 3;
    private static final int DIALOG_JHOIRA = 4;

    /* Type constants */
    private static final String EQUIPMENT = "equipment";
    private static final String CREATURE = "creature";
    private static final String INSTANT = "instant";
    private static final String SORCERY = "sorcery";

    /* UI Elements */
    private Spinner mMomirCmcChoice;
    private Spinner mStonehewerCmcChoice;

    /* It has to be random somehow, right? */
    private Random mRandom;

    /**
     * Create the view, set up the ImageView clicks to see the full Vanguards, set up the buttons to display random
     * cards, save the spinners to figure out CMCs later.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The created view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRandom = new Random(System.currentTimeMillis());

        View myFragmentView = inflater.inflate(R.layout.mojhosto_frag, container, false);

        assert myFragmentView != null;

		/* Add listeners to the portraits to show the full Vanguards */
        myFragmentView.findViewById(R.id.imageViewMo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_MOMIR);
            }
        });

        myFragmentView.findViewById(R.id.imageViewSto).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_STONEHEWER);
            }
        });

        myFragmentView.findViewById(R.id.imageViewJho).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_JHOIRA);
            }
        });

		/* Add the listeners to the buttons to display random cards */
        myFragmentView.findViewById(R.id.momir_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    int cmc = Integer.parseInt((String) mMomirCmcChoice.getSelectedItem());
                    getOneSpell(CREATURE, cmc);
                } catch (NumberFormatException e) {
                    /* eat it */
                }
            }
        });

        myFragmentView.findViewById(R.id.stonehewer_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    int cmc = Integer.parseInt((String) mStonehewerCmcChoice.getSelectedItem());
                    getOneSpell(EQUIPMENT, cmc);
                } catch (NumberFormatException e) {
                    /* eat it */
                }
            }
        });

        myFragmentView.findViewById(R.id.jhorira_instant_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getThreeSpells(INSTANT);
            }
        });

        myFragmentView.findViewById(R.id.jhorira_sorcery_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getThreeSpells(SORCERY);
            }
        });

		/* Save the spinners to pull out the CMCs later */
        mMomirCmcChoice = (Spinner) myFragmentView.findViewById(R.id.momir_spinner);
        mStonehewerCmcChoice = (Spinner) myFragmentView.findViewById(R.id.stonehewer_spinner);

		/* Return the view */
        return myFragmentView;
    }

    /**
     * Check if the rules should be automatically displayed. Must be done in onResume(), otherwise the dialog won't show
     * because the fragment is not yet visible
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getFamiliarActivity().mPreferenceAdapter.getMojhostoFirstTime()) {
            showDialog(DIALOG_RULES);
            getFamiliarActivity().mPreferenceAdapter.setMojhostoFirstTime();
        }
    }

    /**
     * Inflate the menu, has an option to display the rules
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mojhosto_menu, menu);
    }

    /**
     * Handle a menu item click, in this case, it's only to show the rules dialog
     *
     * @param item The item selected
     * @return True if the click was acted upon, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle item selection */
        switch (item.getItemId()) {
            case R.id.random_rules:
                showDialog(DIALOG_RULES);
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
            public Dialog onCreateDialog(Bundle savedInstanceState) {
				/* This will be set to false if we are returning a null dialog. It prevents a crash */
                setShowsDialog(true);

                switch (id) {
                    case DIALOG_RULES: {
						/* Use a generic AlertDialog to display the rules text */
                        AlertDialogPro.Builder builder = new AlertDialogPro.Builder(this.getActivity());
                        builder.setNeutralButton(R.string.mojhosto_dialog_play, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                        builder.setMessage(ImageGetterHelper.formatHtmlString(getString(R.string.mojhosto_rules_text)));
                        builder.setTitle(R.string.mojhosto_rules_title);
                        return builder.create();
                    }
                    case DIALOG_MOMIR:
                    case DIALOG_STONEHEWER:
                    case DIALOG_JHOIRA: {
						/* Use a raw dialog with a custom view (ImageView inside LinearLayout) to display the Vanguard*/
                        Dialog dialog = new Dialog(this.getActivity());
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.setContentView(R.layout.card_view_image_dialog);
                        ImageView image = (ImageView) dialog.findViewById(R.id.cardimage);

						/* These drawables are re-sized on-the-fly, so only a single hi-res version exists in a resource
						   folder without density */
                        switch (id) {
                            case DIALOG_MOMIR:
                                image.setImageResource(R.drawable.mjs_momir);
                                break;
                            case DIALOG_STONEHEWER:
                                image.setImageResource(R.drawable.mjs_stonehewer);
                                break;
                            case DIALOG_JHOIRA:
                                image.setImageResource(R.drawable.mjs_jhoira);
                                break;
                        }

						/* Make a DP border */
                        int border = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());

						/* Get the screen size in px */
                        Rect rectangle = new Rect();
                        Window window = getActivity().getWindow();
                        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
                        int windowHeight = rectangle.height();
                        int windowWidth = rectangle.width();

						/* Get the drawable size in px */
                        assert image.getDrawable() != null;
                        int imageHeight = image.getDrawable().getIntrinsicHeight();
                        int imageWidth = image.getDrawable().getIntrinsicWidth();

						/* Figure out how much to scale the drawable */
                        float scaleFactor;
                        if ((imageHeight / (float) imageWidth) > (windowHeight / (float) windowWidth)) {
							/* Limiting factor is height */
                            scaleFactor = (windowHeight - border) / (float) imageHeight;
                        } else {
							/* Limiting factor is width */
                            scaleFactor = (windowWidth - border) / (float) imageWidth;
                        }

						/* Scale the drawable */
                        image.setLayoutParams(new LinearLayout.LayoutParams((int) (imageWidth * scaleFactor),
                                (int) (imageHeight * scaleFactor)));

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
     * Convenience method to fetch a random card of the given supertype and cmc, and start a CardViewPagerFragment to
     * display said card
     *
     * @param type The supertype of the card to randomly fetch
     * @param cmc  The converted mana cost of the card to randomly fetch
     */
    private void getOneSpell(String type, int cmc) {
        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
        try {
            String logic = "=";
            if (type.equals(EQUIPMENT)) {
                logic = "<=";
            }
            String[] returnTypes = new String[]{CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME};
            Cursor permanents = CardDbAdapter.Search(null, null, type, "wubrgl", 0, null,
                    CardDbAdapter.NO_ONE_CARES, null, CardDbAdapter.NO_ONE_CARES, null, cmc, logic, null, null, null,
                    null, 0, 0, CardDbAdapter.MOST_RECENT_PRINTING, null, false, returnTypes, true, database);

            if(permanents == null) {
                throw new FamiliarDbException(new Exception("permanents failure"));
            }

            if (permanents.getCount() == 0) {
                permanents.close();
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }
            int pos = mRandom.nextInt(permanents.getCount());
            permanents.moveToPosition(pos);

			/* add a fragment */
            Bundle args = new Bundle();
            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, new long[]{permanents.getLong(permanents.getColumnIndex(CardDbAdapter.KEY_ID))});
            args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
            CardViewPagerFragment cvpFrag = new CardViewPagerFragment();
            startNewFragment(cvpFrag, args);

            permanents.close();
        } catch (FamiliarDbException | SQLiteDatabaseCorruptException e) {
            handleFamiliarDbException(true);
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * Convenience method to fetch three random cards of the given supertype, and start a ResultListFragment to
     * display said cards
     *
     * @param type The supertype of the card to randomly fetch
     */
    private void getThreeSpells(String type) {
        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
        try {
            String[] returnTypes = new String[]{CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME};
            Cursor spells = CardDbAdapter.Search(null, null, type, "wubrgl", 0, null,
                    CardDbAdapter.NO_ONE_CARES, null, CardDbAdapter.NO_ONE_CARES, null, -1, null, null, null, null,
                    null, 0, 0, CardDbAdapter.MOST_RECENT_PRINTING, null, false, returnTypes, true, database);

            if(spells == null) {
                throw new FamiliarDbException(new Exception("three spell failure"));
            }
			/* Get 3 random, distinct numbers */
            int pos[] = new int[3];
            pos[0] = mRandom.nextInt(spells.getCount());
            pos[1] = mRandom.nextInt(spells.getCount());
            while (pos[0] == pos[1]) {
                pos[1] = mRandom.nextInt(spells.getCount());
            }
            pos[2] = mRandom.nextInt(spells.getCount());
            while (pos[0] == pos[2] || pos[1] == pos[2]) {
                pos[2] = mRandom.nextInt(spells.getCount());
            }

            Bundle args = new Bundle();

            spells.moveToPosition(pos[0]);
            args.putLong(ResultListFragment.CARD_ID_0, spells.getLong(spells.getColumnIndex(CardDbAdapter.KEY_ID)));
            spells.moveToPosition(pos[1]);
            args.putLong(ResultListFragment.CARD_ID_1, spells.getLong(spells.getColumnIndex(CardDbAdapter.KEY_ID)));
            spells.moveToPosition(pos[2]);
            args.putLong(ResultListFragment.CARD_ID_2, spells.getLong(spells.getColumnIndex(CardDbAdapter.KEY_ID)));

			/* add a fragment */
            ResultListFragment rlFrag = new ResultListFragment();
            startNewFragment(rlFrag, args);

            spells.close();
        } catch (FamiliarDbException | SQLiteDatabaseCorruptException e) {
            handleFamiliarDbException(true);
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }
}