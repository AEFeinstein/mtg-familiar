package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;

public class ManaPoolFragment extends FamiliarFragment {

    private TextView mWhiteReadout, mBlueReadout, mBlackReadout, mRedReadout, mGreenReadout, mColorlessReadout,
            mSpellReadout;
    private int mWhite, mBlue, mBlack, mRed, mGreen, mColorless, mSpell;

    /**
     * Create the view and set up the buttons
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The view to be displayed
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myFragmentView = inflater.inflate(R.layout.mana_pool_frag, container, false);

        mWhite = 0;
        mBlue = 0;
        mBlack = 0;
        mRed = 0;
        mGreen = 0;

        assert myFragmentView != null;
        Button whiteMinus = myFragmentView.findViewById(R.id.white_minus);
        Button blueMinus = myFragmentView.findViewById(R.id.blue_minus);
        Button blackMinus = myFragmentView.findViewById(R.id.black_minus);
        Button redMinus = myFragmentView.findViewById(R.id.red_minus);
        Button greenMinus = myFragmentView.findViewById(R.id.green_minus);
        Button colorlessMinus = myFragmentView.findViewById(R.id.colorless_minus);
        Button spellMinus = myFragmentView.findViewById(R.id.spell_minus);

        Button whitePlus = myFragmentView.findViewById(R.id.white_plus);
        Button bluePlus = myFragmentView.findViewById(R.id.blue_plus);
        Button blackPlus = myFragmentView.findViewById(R.id.black_plus);
        Button redPlus = myFragmentView.findViewById(R.id.red_plus);
        Button greenPlus = myFragmentView.findViewById(R.id.green_plus);
        Button colorlessPlus = myFragmentView.findViewById(R.id.colorless_plus);
        Button spellPlus = myFragmentView.findViewById(R.id.spell_plus);

        mWhiteReadout = myFragmentView.findViewById(R.id.white_readout);
        mBlueReadout = myFragmentView.findViewById(R.id.blue_readout);
        mBlackReadout = myFragmentView.findViewById(R.id.black_readout);
        mRedReadout = myFragmentView.findViewById(R.id.red_readout);
        mGreenReadout = myFragmentView.findViewById(R.id.green_readout);
        mColorlessReadout = myFragmentView.findViewById(R.id.colorless_readout);
        mSpellReadout = myFragmentView.findViewById(R.id.spell_readout);

        boolean loadSuccessful = true;

        Button buttons[] = {whiteMinus, blueMinus, blackMinus, redMinus, greenMinus, colorlessMinus, spellMinus,
                whitePlus, bluePlus, blackPlus, redPlus, greenPlus, colorlessPlus, spellPlus};
        TextView readouts[] = {mWhiteReadout, mBlueReadout, mBlackReadout, mRedReadout, mGreenReadout, mColorlessReadout,
                mSpellReadout};

        for (Button e : buttons) {
            if (e == null) {
                loadSuccessful = false;
            }
        }
        for (TextView e : readouts) {
            if (e == null) {
                loadSuccessful = false;
            }
        }
        if (!loadSuccessful) {
            ToastWrapper.makeText(this.getActivity(), R.string.mana_pool_error_toast, ToastWrapper.LENGTH_LONG).show();
            this.getActivity().getSupportFragmentManager().popBackStack();
            return null;
        }

        whiteMinus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mWhite--;
                if (mWhite < 0) {
                    mWhite = 0;
                }
                update();
            }
        });
        blueMinus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mBlue--;
                if (mBlue < 0) {
                    mBlue = 0;
                }
                update();
            }
        });
        blackMinus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mBlack--;
                if (mBlack < 0) {
                    mBlack = 0;
                }
                update();
            }
        });
        redMinus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mRed--;
                if (mRed < 0) {
                    mRed = 0;
                }
                update();
            }
        });
        greenMinus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mGreen--;
                if (mGreen < 0) {
                    mGreen = 0;
                }
                update();
            }
        });
        colorlessMinus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mColorless--;
                if (mColorless < 0) {
                    mColorless = 0;
                }
                update();
            }
        });
        spellMinus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mSpell--;
                if (mSpell < 0) {
                    mSpell = 0;
                }
                update();
            }
        });
        whiteMinus.setOnLongClickListener(new Button.OnLongClickListener() {
            public boolean onLongClick(View view) {
                mWhite = 0;
                update();
                return true;
            }
        });
        blueMinus.setOnLongClickListener(new Button.OnLongClickListener() {
            public boolean onLongClick(View view) {
                mBlue = 0;
                update();
                return true;
            }
        });
        blackMinus.setOnLongClickListener(new Button.OnLongClickListener() {
            public boolean onLongClick(View view) {
                mBlack = 0;
                update();
                return true;
            }
        });
        redMinus.setOnLongClickListener(new Button.OnLongClickListener() {
            public boolean onLongClick(View view) {
                mRed = 0;
                update();
                return true;
            }
        });
        greenMinus.setOnLongClickListener(new Button.OnLongClickListener() {
            public boolean onLongClick(View view) {
                mGreen = 0;
                update();
                return true;
            }
        });
        colorlessMinus.setOnLongClickListener(new Button.OnLongClickListener() {
            public boolean onLongClick(View view) {
                mColorless = 0;
                update();
                return true;
            }
        });
        spellMinus.setOnLongClickListener(new Button.OnLongClickListener() {
            public boolean onLongClick(View view) {
                mSpell = 0;
                update();
                return true;
            }
        });

        whitePlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mWhite++;
                update();
            }
        });
        bluePlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mBlue++;
                update();
            }
        });
        blackPlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mBlack++;
                update();
            }
        });
        redPlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mRed++;
                update();
            }
        });
        greenPlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mGreen++;
                update();
            }
        });
        colorlessPlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mColorless++;
                update();
            }
        });
        spellPlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mSpell++;
                update();
            }
        });

        return myFragmentView;
    }

    /**
     * When the fragment pauses, save the mana values
     */
    @Override
    public void onPause() {
        super.onPause();
        store();
    }

    /**
     * When the fragment resumes, load the mana values and display them
     */
    @Override
    public void onResume() {
        super.onResume();
        load();
        update();
    }

    /**
     * Load the mana values from the shared preferences
     */
    private void load() {
        mWhite = PreferenceAdapter.getWhiteMana(getContext());
        mBlue = PreferenceAdapter.getBlueMana(getContext());
        mBlack = PreferenceAdapter.getBlackMana(getContext());
        mRed = PreferenceAdapter.getRedMana(getContext());
        mGreen = PreferenceAdapter.getGreenMana(getContext());
        mColorless = PreferenceAdapter.getColorlessMana(getContext());
        mSpell = PreferenceAdapter.getSpellCount(getContext());
    }

    /**
     * Save the mana values in the shared preferences
     */
    private void store() {
        PreferenceAdapter.setWhiteMana(getContext(), mWhite);
        PreferenceAdapter.setBlueMana(getContext(), mBlue);
        PreferenceAdapter.setBlackMana(getContext(), mBlack);
        PreferenceAdapter.setRedMana(getContext(), mRed);
        PreferenceAdapter.setGreenMana(getContext(), mGreen);
        PreferenceAdapter.setColorlessMana(getContext(), mColorless);
        PreferenceAdapter.setSpellCount(getContext(), mSpell);
    }

    /**
     * Display the loaded mana values in the TextViews
     */
    private void update() {
        TextView readouts[] = {mWhiteReadout, mBlueReadout, mBlackReadout, mRedReadout, mGreenReadout,
                mColorlessReadout, mSpellReadout};
        int values[] = {mWhite, mBlue, mBlack, mRed, mGreen, mColorless, mSpell};

        for (int ii = 0; ii < readouts.length; ii++) {
            readouts[ii].setText("" + values[ii]);
        }
    }

    /**
     * Handle menu clicks, in this case, just clear all
     *
     * @param item The MenuItem which was selected
     * @return True if the event was acted upon, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_all:
                PreferenceAdapter.setWhiteMana(getContext(), 0);
                PreferenceAdapter.setBlueMana(getContext(), 0);
                PreferenceAdapter.setBlackMana(getContext(), 0);
                PreferenceAdapter.setRedMana(getContext(), 0);
                PreferenceAdapter.setGreenMana(getContext(), 0);
                PreferenceAdapter.setColorlessMana(getContext(), 0);
                PreferenceAdapter.setSpellCount(getContext(), 0);
                load();
                update();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Inflate the options menu
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.manapool_menu, menu);
    }
}