package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * This fragment lets a user touch die and get a random result
 */
public class DiceFragment extends FamiliarFragment implements ViewSwitcher.ViewFactory {

    private Random mRandom;
    private TextSwitcher mDieOutput;

    private FamiliarActivity mActivity;
    private int mLastNumber = -1;

    /**
     * Set up the TextSwitcher animations, button handlers
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return A view with a TextSwitcher and a bunch of die buttons
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        try {
            mActivity = ((FamiliarFragment) getParentFragment()).getFamiliarActivity();
        } catch (NullPointerException e) {
            mActivity = getFamiliarActivity();
        }

        View myFragmentView = inflater.inflate(R.layout.dice_frag, container, false);

        mRandom = new Random();

        assert myFragmentView != null;
        mDieOutput = (TextSwitcher) myFragmentView.findViewById(R.id.die_output);
        mDieOutput.setInAnimation(AnimationUtils.loadAnimation(this.getActivity(), android.R.anim.slide_in_left));
        mDieOutput.setOutAnimation(AnimationUtils.loadAnimation(this.getActivity(), android.R.anim.slide_out_right));
        mDieOutput.setFactory(this);

        ImageView d2 = (ImageView) myFragmentView.findViewById(R.id.d2);
        ImageView d4 = (ImageView) myFragmentView.findViewById(R.id.d4);
        ImageView d6 = (ImageView) myFragmentView.findViewById(R.id.d6);
        ImageView d8 = (ImageView) myFragmentView.findViewById(R.id.d8);
        ImageView d10 = (ImageView) myFragmentView.findViewById(R.id.d10);
        ImageView d12 = (ImageView) myFragmentView.findViewById(R.id.d12);
        ImageView d20 = (ImageView) myFragmentView.findViewById(R.id.d20);
        ImageView d100 = (ImageView) myFragmentView.findViewById(R.id.d100);
        ImageView dN = (ImageView) myFragmentView.findViewById(R.id.dN);

        if (d2 != null) {
            d2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    flipCoin();
                }
            });
        }
        if (d4 != null) {
            d4.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    rollDie(4);
                }
            });
        }
        if (d6 != null) {
            d6.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    rollDie(6);
                }
            });
        }
        if (d8 != null) {
            d8.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    rollDie(8);
                }
            });
        }
        if (d10 != null) {
            d10.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    rollDie(10);
                }
            });
        }
        if (d12 != null) {
            d12.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    rollDie(12);
                }
            });
        }
        if (d20 != null) {
            d20.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    rollDie(20);
                }
            });
        }
        if (d100 != null) {
            d100.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    rollDie(100);
                }
            });
        }
        if (dN != null) {
            dN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    chooseDie();
                }
            });
        }

        /* Color the die faces */
        int color = getResources().getColor(R.color.colorPrimary_light);
        d2.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        d4.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        d6.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        d8.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        d10.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        d12.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        d20.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        d100.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        dN.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        return myFragmentView;
    }

    /**
     * Present a fragment that lets the user choose the number of sides on the die
     */
    private void chooseDie() {
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {
            @NotNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                super.onCreateDialog(savedInstanceState);

                setShowsDialog(true);

                View v = mActivity.getLayoutInflater().inflate(R.layout.number_picker_frag, null, false);

                assert v != null;

                final EditText txtNumber = (EditText) v.findViewById(R.id.numberInput);

                if (mLastNumber > 0) {
                    txtNumber.setText(String.valueOf(mLastNumber));
                }

                AlertDialogPro.Builder adb = new AlertDialogPro.Builder(mActivity);
                adb.setView(v);
                adb.setTitle(getResources().getString(R.string.dice_choose_sides));
                adb.setPositiveButton(mActivity.getResources().getString(R.string.dialog_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (txtNumber.getText() == null || txtNumber.getText().toString().isEmpty()) {
                                    return;
                                }

                                int num;

                                try {
                                    num = Integer.parseInt(txtNumber.getText().toString());
                                } catch (NumberFormatException e) {
                                    ToastWrapper.makeText(mActivity, getResources().getString(R.string.dice_num_too_large),
                                            ToastWrapper.LENGTH_SHORT).show();
                                    return;
                                }

                                if (num < 1) {
                                    ToastWrapper.makeText(mActivity, getResources().getString(R.string.dice_postive),
                                            ToastWrapper.LENGTH_SHORT).show();
                                } else {
                                    mLastNumber = num;
                                    rollDie(num);
                                }

                                dismiss();
                            }
                        }
                );
                return adb.create();
            }
        };

        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Get a random number between [0, d) and display it as [1,d]
     *
     * @param dieFaces the number of "die faces" for the die being "rolled"
     */
    private void rollDie(int dieFaces) {
        if (mDieOutput != null) {
            mDieOutput.setText("" + (mRandom.nextInt(dieFaces) + 1));
        }
    }

    /**
     * "Flip" a "coin" and display the result as a Heads or Tails string
     */
    private void flipCoin() {
        if (mDieOutput != null) {
            String output;
            switch (mRandom.nextInt(2)) {
                case 0:
                    output = getString(R.string.dice_heads);
                    break;
                default:
                    output = getString(R.string.dice_tails);
                    break;
            }
            mDieOutput.setText(output);
        }
    }

    /**
     * When the TextSwitcher requests a new view, this is where it gets one. Usually I don't like doing UI stuff
     * programmatically, but it was easier than having a separate file just for a single TextView
     *
     * @return a TextView with 80dp text size and center gravity
     */
    @Override
    public View makeView() {
        TextView view = new TextView(getActivity());
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 80);
        view.setGravity(Gravity.CENTER);
        return view;
    }
}