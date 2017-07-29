package com.gelakinetic.mtgfam.helpers.util;

import android.content.Context;
import android.support.v4.app.Fragment;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

/**
 * Created by bmaurer on 5/25/2017.
 */

public class FragmentHelpers {

    public static boolean isInstanceOf(final Context context, final Class<?> pClass) {
        final Fragment fragment = ((FamiliarActivity) context).getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        return pClass.isInstance(fragment);
    }

}
