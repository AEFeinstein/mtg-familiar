package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.ProfileFragment;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Adam on 4/21/2016.
 */
public class ProfileDialogFragment extends FamiliarDialogFragment {

    /* Dialog constants */
    private static final int DIALOG_DCI_NUMBER = 1;

    /**
     * @return The currently viewed ProfileFragment
     */
    ProfileFragment getParentProfileFragment() {
        return (ProfileFragment) getFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
                /* We're setting this to false if we return null, so we should reset it every time to be safe */
        setShowsDialog(true);
        switch (DIALOG_DCI_NUMBER) {
            case DIALOG_DCI_NUMBER: {
                View view = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.alert_dialog_text_entry, null, false);

                final EditText dciEditText = (EditText) view.findViewById(R.id.text_entry);
                dciEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

                view.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dciEditText.setText("");
                    }
                });

                final String strDCI = getFamiliarActivity().mPreferenceAdapter.getDCINumber();

                dciEditText.setText(strDCI);

                return new AlertDialogWrapper.Builder(getActivity())
                        .setTitle(R.string.profile_update_dci_dialog_title)
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String strNumber = dciEditText.getText().toString();

                                if (strNumber.isEmpty()) {
                                    ToastWrapper.makeText(getActivity(),
                                            getString(R.string.profile_invalid_dci),
                                            ToastWrapper.LENGTH_SHORT).show();

                                    return;
                                }

                                getFamiliarActivity().mPreferenceAdapter.setDCINumber(strNumber);
                                getParentProfileFragment().mDCINumber = strNumber;
                                getParentProfileFragment().checkDCINumber();
                                dismiss();
                            }
                        })
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                getParentProfileFragment().checkDCINumber();
                                dismiss();
                            }
                        })
                        .setView(view)
                        .create();
            }
            default: {
                return DontShowDialog();
            }
        }
    }
}